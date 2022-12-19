package moe.sdl.ipdb

import inet.ipaddr.IPAddress
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import moe.sdl.ipdb.exceptions.NoSuchLanguageException
import moe.sdl.ipdb.parser.OrderParser
import moe.sdl.ipdb.parser.PairParser
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption

public suspend fun Reader(
    file: File,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
): Reader = withContext(Dispatchers.IO) {
    val channel = Files.newByteChannel(file.toPath(), StandardOpenOption.READ) as FileChannel
    channel.use {
        val mapped = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())
        Reader(mapped, dispatcher)
    }
}

public suspend inline fun Reader(
    data: ByteArray,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
): Reader = Reader(ByteBuffer.wrap(data), dispatcher)

public suspend fun Reader(
    data: ByteBuffer,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
): Reader = withContext(dispatcher) {
    val metaLen = data.int
    val meta = run {
        val meta = ByteArray(metaLen)
        data.get(meta, 0, metaLen)
        Json.decodeFromString(Metadata.serializer(), meta.decodeToString())
    }
    var node = 0L
    for (i in 0 until 96) {
        if (node >= meta.nodeCount) {
            break
        }
        val offset = if (i >= 80) node * 8 + 1 * 4 else node * 8
        node = data.slice().getInt(offset.toInt()).toUInt().toLong()
    }
    Reader(meta, data, node)
}

public class Reader internal constructor(
    public val metadata: Metadata,
    private val data: ByteBuffer,
    private val v4offset: Long
) {
    public val isIpv4: Boolean
        get() = metadata.ipVersion and 0x01 == 0x01

    public val isIpv6: Boolean
        get() = metadata.ipVersion and 0x02 == 0x02

    /**
     * The IP version of this IPDB [Reader], return null if unknown
     *
     * @see IPAddress.IPVersion
     */
    public val ipVersion: IPAddress.IPVersion?
        get() = when {
            isIpv4 -> IPAddress.IPVersion.IPV4
            isIpv6 -> IPAddress.IPVersion.IPV6
            else -> null
        }

    private fun resolve(node: Long): String {
        val resolved = (node - metadata.nodeCount + metadata.nodeCount * 8).toInt()
        val dataSize = data.slice().capacity()
        require(resolved < dataSize) {
            "IPDB file parse error, resolved($resolved) > file length ($dataSize)"
        }
        val size = data.slice().getShort(resolved).toUShort().toInt()
        println(dataSize)
        println(resolved)
        println(size)

        check(dataSize > resolved + size) {
            "IPDB file parse error, size($size) > file length ($dataSize)"
        }
        val bytes = ByteArray(size)
        data.slice().position(resolved + 2).get(bytes, 0, size)
        return bytes.decodeToString()
    }

    private fun readNode(node: Long, index: Long): Long {
        val off = (node * 8 + index * 4).toInt()
        return data.slice().getInt(off).toUInt().toLong()
    }

    private fun findNode(binary: ByteArray): Long? {
        var node = 0L
        val bit = binary.size * 8
        if (bit == 32) {
            node = v4offset
        }

        for (i in 0..bit) {
            if (node > metadata.nodeCount) {
                return node
            }
            node = readNode(node, (1 and ((0xFF and binary[i / 8].toInt()) shr 7 - (i % 8))).toLong())
        }
        return if (node > metadata.nodeCount) node else null
    }

    /**
     * Return all attributes of [IPAddress], null if not found
     * @throws IllegalArgumentException when ip version of [addr] is different from this IPDB
     * @throws NoSuchLanguageException when language is not supported in this IPDB
     */
    public fun find(addr: IPAddress, language: String): List<String>? {
        require(addr.ipVersion == ipVersion) {
            "The ip version of IPDB is $ipVersion but argument is ${addr.ipVersion}"
        }
        val off = metadata.languages[language] ?: throw NoSuchLanguageException(language)
        val ipv = addr.bytes
        val node = findNode(ipv) ?: return null
        val ctx = resolve(node)
        return ctx.split('\t').drop(off.toInt())
    }

    /**
     * Return field to value pairs of [IPAddress], null if not found
     * @see find
     */
    public fun findToPairs(addr: IPAddress, language: String): List<Pair<String, String>>? {
        val values = find(addr, language) ?: return null
        return metadata.fields.zip(values)
    }

    /**
     * Return parsed [T], null if not found
     * @see [OrderParser]
     * @see [find]
     */
    public fun <T> findThenParse(parser: OrderParser<T>, addr: IPAddress, language: String): T? =
        find(addr, language)?.let { parser.parse(it) }

    /**
     * @see [PairParser]
     * @see [findToPairs]
     */
    public fun <T> findThenParse(parser: PairParser<T>, addr: IPAddress, language: String): T? =
        findToPairs(addr, language)?.let { parser.parsePairs(it) }
}

@Serializable
public data class Metadata(
    val build: Long,
    @SerialName("ip_version") val ipVersion: Int,
    @SerialName("node_count") val nodeCount: Long,
    val languages: HashMap<String, Long>,
    val fields: List<String>,
    @SerialName("total_size") val totalSize: Long
)

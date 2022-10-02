package moe.sdl.ipdb

import inet.ipaddr.IPAddress
import inet.ipaddr.IPAddressString
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import moe.sdl.ipdb.exceptions.NoSuchLanguageException
import moe.sdl.ipdb.parser.OrderParser
import moe.sdl.ipdb.parser.PairParser
import moe.sdl.ipdb.parser.builtins.FullInfo
import java.io.DataInputStream
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer

public suspend inline fun Reader(
    file: File,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
): Reader = Reader(file.inputStream(), dispatcher)

public suspend fun Reader(
    inputStream: InputStream,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
): Reader = withContext(dispatcher) {
    DataInputStream(inputStream.buffered()).use {
        val metaLen = it.readInt()
        val meta = run {
            val meta = ByteArray(metaLen)
            it.readFully(meta)
            Json.decodeFromString<Metadata>(meta.decodeToString())
        }
        val data = it.readAllBytes()
        var node = 0L
        for (i in 0 until 96) {
            if (node >= meta.nodeCount) {
                break
            }
            fun setNode(offset: Long) {
                val node0 = data.sliceArray(offset.toInt() until (offset + 4).toInt())
                node = ByteBuffer.wrap(node0).int.toUInt().toLong()
            }
            setNode(
                if (i >= 80) {
                    node * 8L + 1L * 4L
                } else {
                    node * 8L
                }
            )
        }
        Reader(meta, data, node)
    }
}

public class Reader internal constructor(
    public val metadata: Metadata,
    private val data: ByteArray,
    private val v4offset: Long
) {
    public val isIpv4: Boolean
        get() = metadata.ipVersion and 0x01 == 0x01

    public val isIpv6: Boolean
        get() = metadata.ipVersion and 0x02 == 0x02

    /**
     * The IP version of this IPDB [Reader], return null if unknown
     *
     * @see IPVersion
     */
    public val ipVersion: IPAddress.IPVersion?
        get() = when {
            isIpv4 -> IPAddress.IPVersion.IPV4
            isIpv6 -> IPAddress.IPVersion.IPV6
            else -> null
        }

    private fun resolve(node: Long): String {
        val resolved = (node - metadata.nodeCount + metadata.nodeCount * 8).toInt()
        require(resolved < data.size) {
            "IPDB file parse error, resolved($resolved) > file length (${data.size})"
        }
        val size = ByteBuffer.wrap(
            byteArrayOf(0, 0, data[resolved], data[resolved + 1])
        ).int + resolved + 2
        check(data.size > size) {
            "IPDB file parse error, size($size) > file length (${data.size})"
        }
        return data.sliceArray(resolved + 2 until size).decodeToString()
    }

    private fun readNode(node: Long, index: Long): Long {
        val off = (node * 8 + index * 4).toInt()
        return ByteBuffer.wrap(data.sliceArray(off..off + 4)).int.toLong()
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

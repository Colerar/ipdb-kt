package moe.sdl.ipdb.parser

import moe.sdl.ipdb.parser.builtins.FullInfo

/**
 * IPDB Parser
 */
public sealed interface Parser

/**
 * [OrderParser] takes values sequentially,
 * which is good for performance, but lacks compatibility,
 * so there is no default implementation for it.
 *
 * If you're picky about performance,
 * you can implement it manually like this:
 *
 * ```kotlin
 * @Serializable
 * data class IdcInfo(
 *     val countryName: String,
 *     val regionName: String,
 *     val cityName: String,
 *     val ownerDomain: String,
 *     val ispDomain: String,
 *     val idc: String
 * ) : OrderParser<IdcInfo> {
 *     companion object {
 *         override fun parse(data: List<String>): IdcInfo =
 *             IdcInfo(
 *             countryName = buff[0],
 *             regionName = buff[1],
 *             cityName = buff[2],
 *             ownerDomain = buff[3],
 *             ispDomain = buff[4],
 *             idc = buff[5],
 *         )
 *     }
 * }
 * ```
 *
 * @see PairParser
 */
public interface OrderParser<T> {
    public fun parse(data: List<String>): T
}

/**
 * [PairParser] takes values by key-value pairs,
 * thus allowing for better compatibility, but not for performance.
 *
 * [FullInfo] is the default implementation for such purpose.
 *
 * @see OrderParser
 * @see FullInfo
 */
public interface PairParser<T> {
    public fun parsePairs(data: List<Pair<String, String>>): T
}

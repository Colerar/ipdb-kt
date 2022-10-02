package moe.sdl.ipdb.parser.builtins

import kotlinx.serialization.Serializable
import moe.sdl.ipdb.parser.PairParser

@Serializable
public data class FullInfo(
    val countryName: String,
    val regionName: String,
    val cityName: String,
    val districtName: String,
    val ownerDomain: String,
    val ispDomain: String,
    val latitude: String,
    val longitude: String,
    val timezone: String,
    val utcOffset: String,
    val chinaRegionCode: String,
    val chinaCityCode: String,
    val chinaAdminCode: String,
    val iddCode: String,
    val countryCode: String,
    val continentCode: String,
    val idc: String,
    val baseStation: String,
    val countryCode3: String,
    val europeanUnion: String,
    val currencyCode: String,
    val currencyName: String,
    val anyCast: String,
    val line: String,
    val route: String,
    val asn: String,
    val areaCode: String,
    val usageType: String
) {
    public companion object : PairParser<FullInfo> {
        override fun parsePairs(data: List<Pair<String, String>>): FullInfo {
            val map = data.toMap()
            fun f(name: String) = map[name] ?: ""
            return FullInfo(
                countryName = f("country_name"),
                regionName = f("region_name"),
                cityName = f("city_name"),
                districtName = f("district_name"),
                ownerDomain = f("owner_domain"),
                ispDomain = f("isp_domain"),
                latitude = f("latitude"),
                longitude = f("longitude"),
                timezone = f("timezone"),
                utcOffset = f("utc_offset"),
                chinaRegionCode = f("china_region_code"),
                chinaCityCode = f("china_city_code"),
                chinaAdminCode = f("china_admin_code"),
                iddCode = f("idd_code"),
                countryCode = f("country_code"),
                continentCode = f("continent_code"),
                idc = f("idc"),
                baseStation = f("base_station"),
                countryCode3 = f("country_code3"),
                europeanUnion = f("european_union"),
                currencyCode = f("currency_code"),
                currencyName = f("currency_name"),
                anyCast = f("anycast"),
                line = f("line"),
                route = f("route"),
                asn = f("asn"),
                areaCode = f("area_code"),
                usageType = f("usage_type")
            )
        }
    }
}

package io.nekohasekai.sagernet.database

import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.ktx.app
import java.util.Locale

data class RulePreset(
    val key: String,
    val nameRes: Int,
    val descriptionRes: Int,
    val build: () -> List<RuleEntity>,
)

object RulePresets {

    val All: List<RulePreset>
        get() = listOf(
            bypassMainlandCN(),
            globalProxy(),
            blockAdsOnly(),
            bypassIran(),
            bypassRussia(),
            bypassMultiRegion(),
        )

    fun byKey(key: String): RulePreset? = All.firstOrNull { it.key == key }

    fun defaultForLocale(): RulePreset {
        return if (Locale.getDefault().country == Locale.CHINA.country) {
            bypassMainlandCN()
        } else {
            bypassMultiRegion()
        }
    }

    private fun rule(
        name: String,
        domains: String = "",
        ip: String = "",
        port: String = "",
        network: String = "",
        outbound: Long = 0,
    ): RuleEntity = RuleEntity(
        name = name,
        domains = domains,
        ip = ip,
        port = port,
        network = network,
        outbound = outbound,
        enabled = true,
    )

    private fun blockQuic() = rule(
        name = app.getString(R.string.route_opt_block_quic),
        port = "443",
        network = "udp",
        outbound = -2,
    )

    private fun blockAds() = rule(
        name = app.getString(R.string.route_opt_block_ads),
        domains = "geosite:category-ads-all",
        outbound = -2,
    )

    private fun bypassRegion(country: String, display: String): List<RuleEntity> {
        val rules = mutableListOf<RuleEntity>()
        if (country == "cn") {
            rules.add(
                rule(
                    name = app.getString(R.string.route_play_store, display),
                    domains = "googleapis.cn",
                )
            )
        }
        rules.add(
            rule(
                name = app.getString(R.string.route_bypass_domain, display),
                domains = "geosite:$country",
                outbound = -1,
            )
        )
        rules.add(
            rule(
                name = app.getString(R.string.route_bypass_ip, display),
                ip = "geoip:$country",
                outbound = -1,
            )
        )
        return rules
    }

    fun bypassMainlandCN(): RulePreset = RulePreset(
        key = "bypass_mainland_cn",
        nameRes = R.string.preset_bypass_mainland_cn,
        descriptionRes = R.string.preset_bypass_mainland_cn_desc,
        build = {
            listOf(blockQuic(), blockAds()) + bypassRegion("cn", "中国")
        },
    )

    fun globalProxy(): RulePreset = RulePreset(
        key = "global_proxy",
        nameRes = R.string.preset_global_proxy,
        descriptionRes = R.string.preset_global_proxy_desc,
        build = {
            listOf(blockQuic(), blockAds())
        },
    )

    fun blockAdsOnly(): RulePreset = RulePreset(
        key = "block_ads_only",
        nameRes = R.string.preset_block_ads_only,
        descriptionRes = R.string.preset_block_ads_only_desc,
        build = {
            listOf(blockAds())
        },
    )

    fun bypassIran(): RulePreset = RulePreset(
        key = "bypass_iran",
        nameRes = R.string.preset_bypass_iran,
        descriptionRes = R.string.preset_bypass_iran_desc,
        build = {
            listOf(blockQuic(), blockAds()) + bypassRegion("ir", "Iran")
        },
    )

    fun bypassRussia(): RulePreset = RulePreset(
        key = "bypass_russia",
        nameRes = R.string.preset_bypass_russia,
        descriptionRes = R.string.preset_bypass_russia_desc,
        build = {
            listOf(blockQuic(), blockAds()) + bypassRegion("ru", "Russia")
        },
    )

    fun bypassMultiRegion(): RulePreset = RulePreset(
        key = "bypass_multi_region",
        nameRes = R.string.preset_bypass_multi_region,
        descriptionRes = R.string.preset_bypass_multi_region_desc,
        build = {
            listOf(blockQuic(), blockAds()) +
                bypassRegion("cn", "中国") +
                bypassRegion("ir", "Iran") +
                bypassRegion("ru", "Russia")
        },
    )
}

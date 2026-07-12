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
            // 地区绕过
            bypassMainlandCN(),
            bypassMultiRegion(),
            bypassIran(),
            bypassRussia(),
            // 应用分流
            streamingProxy(),
            aiServicesProxy(),
            socialAppsProxy(),
            googleServicesProxy(),
            telegramProxy(),
            devServicesProxy(),
            // 基础
            globalProxy(),
            blockAdsOnly(),
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

    private fun appProxy(appName: String, geosite: String): RuleEntity = rule(
        name = appName,
        domains = "geosite:$geosite",
        outbound = 0,
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

    private fun bypassCN() = bypassRegion("cn", "中国")

    // ===== 地区绕过预设 =====

    fun bypassMainlandCN(): RulePreset = RulePreset(
        key = "bypass_mainland_cn",
        nameRes = R.string.preset_bypass_mainland_cn,
        descriptionRes = R.string.preset_bypass_mainland_cn_desc,
        build = {
            listOf(blockQuic(), blockAds()) + bypassCN()
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

    // ===== 应用分流预设 =====

    fun streamingProxy(): RulePreset = RulePreset(
        key = "streaming_proxy",
        nameRes = R.string.preset_streaming_proxy,
        descriptionRes = R.string.preset_streaming_proxy_desc,
        build = {
            listOf(blockQuic(), blockAds()) + listOf(
                appProxy("Netflix", "netflix"),
                appProxy("Disney+", "disney"),
                appProxy("YouTube", "youtube"),
                appProxy("Spotify", "spotify"),
                appProxy("TikTok", "tiktok"),
                appProxy("HBO Max", "hbo"),
            ) + bypassCN()
        },
    )

    fun aiServicesProxy(): RulePreset = RulePreset(
        key = "ai_proxy",
        nameRes = R.string.preset_ai_proxy,
        descriptionRes = R.string.preset_ai_proxy_desc,
        build = {
            listOf(blockAds()) + listOf(
                appProxy("OpenAI / ChatGPT", "openai"),
                appProxy("Google / Gemini", "google"),
            ) + bypassCN()
        },
    )

    fun socialAppsProxy(): RulePreset = RulePreset(
        key = "social_proxy",
        nameRes = R.string.preset_social_proxy,
        descriptionRes = R.string.preset_social_proxy_desc,
        build = {
            listOf(blockAds()) + listOf(
                appProxy("Telegram", "telegram"),
                appProxy("WhatsApp", "whatsapp"),
                appProxy("Twitter / X", "twitter"),
                appProxy("Facebook", "facebook"),
                appProxy("Instagram", "instagram"),
                appProxy("Discord", "discord"),
            ) + bypassCN()
        },
    )

    fun googleServicesProxy(): RulePreset = RulePreset(
        key = "google_proxy",
        nameRes = R.string.preset_google_proxy,
        descriptionRes = R.string.preset_google_proxy_desc,
        build = {
            listOf(blockAds()) + listOf(
                appProxy("Google", "google"),
                appProxy("YouTube", "youtube"),
            ) + bypassCN()
        },
    )

    fun telegramProxy(): RulePreset = RulePreset(
        key = "telegram_proxy",
        nameRes = R.string.preset_telegram_proxy,
        descriptionRes = R.string.preset_telegram_proxy_desc,
        build = {
            listOf(blockAds()) + listOf(
                appProxy("Telegram", "telegram"),
            ) + bypassCN()
        },
    )

    fun devServicesProxy(): RulePreset = RulePreset(
        key = "dev_proxy",
        nameRes = R.string.preset_dev_proxy,
        descriptionRes = R.string.preset_dev_proxy_desc,
        build = {
            listOf(blockAds()) + listOf(
                appProxy("GitHub", "github"),
                appProxy("GitLab", "gitlab"),
                appProxy("Docker", "docker"),
            ) + bypassCN()
        },
    )
}

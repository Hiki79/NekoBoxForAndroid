package io.nekohasekai.sagernet.database

data class GeositeEntry(
    val geosite: String,
    val name: String,
    val description: String,
    val tags: List<String>,
)

object GeositeLibrary {

    val All: List<GeositeEntry> = listOf(
        // ===== 流媒体 =====
        GeositeEntry("netflix", "Netflix", "Netflix 流媒体", listOf("流媒体", "video")),
        GeositeEntry("disney", "Disney+", "Disney+ 流媒体", listOf("流媒体", "video")),
        GeositeEntry("youtube", "YouTube", "谷歌视频", listOf("流媒体", "video")),
        GeositeEntry("spotify", "Spotify", "音乐流媒体", listOf("流媒体", "music")),
        GeositeEntry("tiktok", "TikTok", "字节跳动短视频", listOf("流媒体", "social")),
        GeositeEntry("hbo", "HBO Max", "HBO 流媒体", listOf("流媒体", "video")),
        GeositeEntry("hulu", "Hulu", "Hulu 流媒体", listOf("流媒体", "video")),
        GeositeEntry("primevideo", "Prime Video", "亚马逊视频", listOf("流媒体", "video")),
        GeositeEntry("amazon", "Amazon", "亚马逊", listOf("购物", "流媒体")),
        GeositeEntry("twitch", "Twitch", "游戏直播", listOf("流媒体", "gaming")),
        GeositeEntry("appletv", "Apple TV", "苹果视频", listOf("流媒体", "video")),
        GeositeEntry("bilibili", "哔哩哔哩", "B 站", listOf("流媒体", "video", "国内")),
        GeositeEntry("iqiyi", "爱奇艺", "爱奇艺视频", listOf("流媒体", "video", "国内")),
        GeositeEntry("youku", "优酷", "优酷视频", listOf("流媒体", "video", "国内")),
        GeositeEntry("tencentvideo", "腾讯视频", "腾讯视频", listOf("流媒体", "video", "国内")),
        GeositeEntry("mgtv", "芒果 TV", "芒果电视", listOf("流媒体", "video", "国内")),
        GeositeEntry("pandora", "Pandora", "Pandora 音乐", listOf("流媒体", "music")),
        GeositeEntry("deezer", "Deezer", "Deezer 音乐", listOf("流媒体", "music")),
        GeositeEntry("soundcloud", "SoundCloud", "SoundCloud 音乐", listOf("流媒体", "music")),
        GeositeEntry("viu", "Viu", "Viu 流媒体", listOf("流媒体", "video")),
        GeositeEntry("viaplay", "Viaplay", "Viaplay 流媒体", listOf("流媒体", "video")),
        GeositeEntry("tvb", "TVB", "香港无线电视", listOf("流媒体", "video")),
        GeositeEntry("bbc", "BBC", "英国广播公司", listOf("流媒体", "news", "video")),
        GeositeEntry("cwtv", "The CW", "CW 电视网", listOf("流媒体", "video")),
        GeositeEntry("crunchyroll", "Crunchyroll", "动漫流媒体", listOf("流媒体", "video", "anime")),
        GeositeEntry("plex", "Plex", "Plex 媒体服务器", listOf("流媒体", "video")),
        GeositeEntry("paramountplus", "Paramount+", "派拉蒙流媒体", listOf("流媒体", "video")),
        GeositeEntry("nowtv", "Now TV", "香港 Now TV", listOf("流媒体", "video")),
        GeositeEntry("primevideo", "Prime Video", "亚马逊 Prime 视频", listOf("流媒体", "video")),

        // ===== 社交 =====
        GeositeEntry("telegram", "Telegram", "Telegram 即时通讯", listOf("社交", "messaging")),
        GeositeEntry("whatsapp", "WhatsApp", "WhatsApp 即时通讯", listOf("社交", "messaging")),
        GeositeEntry("twitter", "Twitter / X", "推特", listOf("社交")),
        GeositeEntry("facebook", "Facebook", "脸书", listOf("社交")),
        GeositeEntry("instagram", "Instagram", "Instagram 图片社交", listOf("社交")),
        GeositeEntry("discord", "Discord", "Discord 社区", listOf("社交", "gaming")),
        GeositeEntry("reddit", "Reddit", "Reddit 论坛", listOf("社交")),
        GeositeEntry("snapchat", "Snapchat", "Snapchat 阅后即焚", listOf("社交")),
        GeositeEntry("linkedin", "LinkedIn", "领英职场社交", listOf("社交")),
        GeositeEntry("tumblr", "Tumblr", "Tumblr 博客", listOf("社交")),
        GeositeEntry("pinterest", "Pinterest", "Pinterest 图片", listOf("社交")),
        GeositeEntry("weibo", "微博", "新浪微博", listOf("社交", "国内")),
        GeositeEntry("weixin", "微信", "微信", listOf("社交", "国内")),
        GeositeEntry("zhihu", "知乎", "知乎问答", listOf("社交", "国内")),
        GeositeEntry("xiaohongshu", "小红书", "小红书种草", listOf("社交", "国内")),
        GeositeEntry("douyin", "抖音", "抖音短视频", listOf("社交", "国内")),
        GeositeEntry("threads", "Threads", "Meta Threads", listOf("社交")),
        GeositeEntry("signal", "Signal", "Signal 即时通讯", listOf("社交", "messaging")),
        GeositeEntry("mastodon", "Mastodon", "长毛象", listOf("社交")),
        GeositeEntry("bluesky", "Bluesky", "Bluesky 社交", listOf("社交")),

        // ===== AI 服务 =====
        GeositeEntry("openai", "OpenAI / ChatGPT", "OpenAI ChatGPT", listOf("AI")),
        GeositeEntry("anthropic", "Anthropic / Claude", "Anthropic Claude", listOf("AI")),
        GeositeEntry("bard", "Google Gemini", "谷歌 Gemini", listOf("AI")),

        // ===== 谷歌系 =====
        GeositeEntry("google", "Google", "谷歌搜索与服务", listOf("谷歌", "search")),
        GeositeEntry("googleplex", "Google Cloud", "谷歌云", listOf("谷歌", "cloud")),
        GeositeEntry("gmail", "Gmail", "谷歌邮箱", listOf("谷歌", "mail")),
        GeositeEntry("googletranslate", "Google Translate", "谷歌翻译", listOf("谷歌", "tools")),
        GeositeEntry("googledrive", "Google Drive", "谷歌云盘", listOf("谷歌", "cloud")),
        GeositeEntry("googlemaps", "Google Maps", "谷歌地图", listOf("谷歌", "maps")),
        GeositeEntry("gstatic", "Google Static", "谷歌静态资源", listOf("谷歌")),

        // ===== 科技 / 开发者 =====
        GeositeEntry("github", "GitHub", "GitHub 代码托管", listOf("开发", "code")),
        GeositeEntry("gitlab", "GitLab", "GitLab 代码托管", listOf("开发", "code")),
        GeositeEntry("bitbucket", "Bitbucket", "Bitbucket 代码托管", listOf("开发", "code")),
        GeositeEntry("docker", "Docker", "Docker 容器", listOf("开发", "devops")),
        GeositeEntry("stackoverflow", "Stack Overflow", "Stack Overflow 问答", listOf("开发", "code")),
        GeositeEntry("npm", "npm", "npm 包仓库", listOf("开发", "code")),
        GeositeEntry("maven", "Maven", "Maven 仓库", listOf("开发", "code")),
        GeositeEntry("pypi", "PyPI", "Python 包仓库", listOf("开发", "code")),
        GeositeEntry("jetbrains", "JetBrains", "JetBrains IDE", listOf("开发", "tools")),
        GeositeEntry("visualstudio", "Visual Studio", "微软 VS", listOf("开发", "tools")),
        GeositeEntry("vscodium", "VS Code", "VS Code 编辑器", listOf("开发", "tools")),
        GeositeEntry("huggingface", "Hugging Face", "AI 模型仓库", listOf("开发", "AI")),
        GeositeEntry("cloudflare", "Cloudflare", "Cloudflare CDN", listOf("开发", "cloud")),
        GeositeEntry("vercel", "Vercel", "Vercel 托管", listOf("开发", "cloud")),
        GeositeEntry("netlify", "Netlify", "Netlify 托管", listOf("开发", "cloud")),
        GeositeEntry("heroku", "Heroku", "Heroku 云平台", listOf("开发", "cloud")),
        GeositeEntry("digitalocean", "DigitalOcean", "DO 云", listOf("开发", "cloud")),
        GeositeEntry("linode", "Linode", "Linode 云", listOf("开发", "cloud")),
        GeositeEntry("vultr", "Vultr", "Vultr 云", listOf("开发", "cloud")),

        // ===== 微软系 =====
        GeositeEntry("microsoft", "Microsoft", "微软服务", listOf("微软")),
        GeositeEntry("outlook", "Outlook", "微软邮箱", listOf("微软", "mail")),
        GeositeEntry("office365", "Office 365", "微软 Office", listOf("微软")),
        GeositeEntry("onedrive", "OneDrive", "微软云盘", listOf("微软", "cloud")),
        GeositeEntry("bing", "Bing", "必应搜索", listOf("微软", "search")),
        GeositeEntry("xbox", "Xbox", "Xbox 游戏", listOf("微软", "gaming")),
        GeositeEntry("windowsupdate", "Windows Update", "Windows 更新", listOf("微软")),

        // ===== 苹果系 =====
        GeositeEntry("apple", "Apple", "苹果服务", listOf("苹果")),
        GeositeEntry("icloud", "iCloud", "苹果云", listOf("苹果", "cloud")),
        GeositeEntry("appstore", "App Store", "苹果应用商店", listOf("苹果")),
        GeositeEntry("itunes", "iTunes", "苹果 iTunes", listOf("苹果")),
        GeositeEntry("applenews", "Apple News", "苹果新闻", listOf("苹果", "news")),

        // ===== 游戏 =====
        GeositeEntry("steam", "Steam", "Steam 游戏", listOf("游戏", "gaming")),
        GeositeEntry("epicgames", "Epic Games", "Epic 商店", listOf("游戏", "gaming")),
        GeositeEntry("riot", "Riot Games", "拳头游戏", listOf("游戏", "gaming")),
        GeositeEntry("blizzard", "Blizzard", "暴雪战网", listOf("游戏", "gaming")),
        GeositeEntry("playstation", "PlayStation", "索尼 PSN", listOf("游戏", "gaming")),
        GeositeEntry("nintendo", "Nintendo", "任天堂", listOf("游戏", "gaming")),
        GeositeEntry("garena", "Garena", "Garena 游戏", listOf("游戏", "gaming")),
        GeositeEntry("roblox", "Roblox", "Roblox 游戏", listOf("游戏", "gaming")),
        GeositeEntry("ea", "EA", "Electronic Arts", listOf("游戏", "gaming")),
        GeositeEntry("ubisoft", "Ubisoft", "育碧", listOf("游戏", "gaming")),
        GeositeEntry("leagueoflegends", "英雄联盟", "LOL", listOf("游戏", "gaming")),
        GeositeEntry("minecraft", "Minecraft", "我的世界", listOf("游戏", "gaming")),
        GeositeEntry("valorant", "Valorant", "无畏契约", listOf("游戏", "gaming")),

        // ===== 购物 / 电商 =====
        GeositeEntry("ebay", "eBay", "eBay 拍卖", listOf("购物")),
        GeositeEntry("aliexpress", "AliExpress", "速卖通", listOf("购物", "国内")),
        GeositeEntry("taobao", "淘宝", "淘宝", listOf("购物", "国内")),
        GeositeEntry("jd", "京东", "京东", listOf("购物", "国内")),
        GeositeEntry("pinduoduo", "拼多多", "拼多多", listOf("购物", "国内")),
        GeositeEntry("shopify", "Shopify", "Shopify 电商", listOf("购物")),

        // ===== 新闻 / 媒体 =====
        GeositeEntry("nytimes", "NY Times", "纽约时报", listOf("新闻", "news")),
        GeositeEntry("cnn", "CNN", "美国有线电视", listOf("新闻", "news")),
        GeositeEntry("reuters", "Reuters", "路透社", listOf("新闻", "news")),
        GeositeEntry("wikipedia", "Wikipedia", "维基百科", listOf("新闻", "reference")),
        GeositeEntry("wikimedia", "Wikimedia", "维基媒体", listOf("新闻", "reference")),
        GeositeEntry("aljazeera", "Al Jazeera", "半岛电视台", listOf("新闻", "news")),
        GeositeEntry("ft", "Financial Times", "金融时报", listOf("新闻", "news")),
        GeositeEntry("bloomberg", "Bloomberg", "彭博社", listOf("新闻", "news")),
        GeositeEntry("economist", "The Economist", "经济学人", listOf("新闻", "news")),
        GeositeEntry("guardian", "The Guardian", "卫报", listOf("新闻", "news")),
        GeositeEntry("medium", "Medium", "Medium 博客", listOf("新闻", "blog")),

        // ===== 地区绕过 =====
        GeositeEntry("cn", "中国", "中国大陆域名", listOf("地区", "bypass", "国内")),
        GeositeEntry("ru", "俄罗斯", "俄罗斯域名", listOf("地区", "bypass")),
        GeositeEntry("ir", "伊朗", "伊朗域名", listOf("地区", "bypass")),
        GeositeEntry("uk", "英国", "英国域名", listOf("地区")),
        GeositeEntry("jp", "日本", "日本域名", listOf("地区")),
        GeositeEntry("kr", "韩国", "韩国域名", listOf("地区")),
        GeositeEntry("de", "德国", "德国域名", listOf("地区")),
        GeositeEntry("fr", "法国", "法国域名", listOf("地区")),
        GeositeEntry("ca", "加拿大", "加拿大域名", listOf("地区")),
        GeositeEntry("au", "澳大利亚", "澳大利亚域名", listOf("地区")),
        GeositeEntry("hk", "香港", "香港域名", listOf("地区")),
        GeositeEntry("tw", "台湾", "台湾域名", listOf("地区")),

        // ===== 广告 / 屏蔽 =====
        GeositeEntry("category-ads-all", "广告(全分类)", "屏蔽所有广告域名", listOf("广告", "block")),
        GeositeEntry("category-ads-ir", "伊朗广告", "屏蔽伊朗广告", listOf("广告", "block")),
        GeositeEntry("adblock", "Adblock 列表", "广告屏蔽列表", listOf("广告", "block")),

        // ===== 其它常用 =====
        GeositeEntry("paypal", "PayPal", "PayPal 支付", listOf("支付", "finance")),
        GeositeEntry("stripe", "Stripe", "Stripe 支付", listOf("支付", "finance")),
        GeositeEntry("wise", "Wise", "Wise 转账", listOf("支付", "finance")),
        GeositeEntry("venmo", "Venmo", "Venmo 支付", listOf("支付", "finance")),
        GeositeEntry("zoom", "Zoom", "Zoom 视频会议", listOf("工具", "video")),
        GeositeEntry("teams", "Microsoft Teams", "Teams 会议", listOf("微软", "video")),
        GeositeEntry("notion", "Notion", "Notion 笔记", listOf("工具", "productivity")),
        GeositeEntry("slack", "Slack", "Slack 协作", listOf("工具", "productivity")),
        GeositeEntry("trello", "Trello", "Trello 看板", listOf("工具", "productivity")),
        GeositeEntry("dropbox", "Dropbox", "Dropbox 云盘", listOf("工具", "cloud")),
        GeositeEntry("mega", "MEGA", "MEGA 网盘", listOf("工具", "cloud")),
        GeositeEntry("mediafire", "MediaFire", "MediaFire 网盘", listOf("工具", "cloud")),
        GeositeEntry("openai", "OpenAI", "OpenAI API", listOf("AI")),
        GeositeEntry("firefox", "Firefox", "火狐浏览器", listOf("工具", "browser")),
        GeositeEntry("tor", "Tor", "Tor 洋葱路由", listOf("工具", "privacy")),
        GeositeEntry("protonmail", "ProtonMail", "ProtonMail 邮箱", listOf("工具", "mail", "privacy")),
        GeositeEntry("tutanota", "Tutanota", "Tutanota 加密邮箱", listOf("工具", "mail", "privacy")),
        GeositeEntry("duckduckgo", "DuckDuckGo", "隐私搜索", listOf("工具", "search", "privacy")),
        GeositeEntry("cloudflare", "Cloudflare WARP", "Cloudflare WARP", listOf("工具", "cloud")),
        GeositeEntry("speedtest", "Speedtest", "网速测试", listOf("工具", "test")),
        GeositeEntry("fast", "Fast.com", "Netflix 测速", listOf("工具", "test")),
        GeositeEntry("category-public-tracker", "公共 BT Tracker", "BitTorrent 公共追踪", listOf("工具", "bt")),
        GeositeEntry("tld-cn", "中国顶级域", ".cn 等中国顶级域名", listOf("地区", "国内")),
        GeositeEntry("geolocation-!cn", "非中国", "非中国大陆域名", listOf("地区")),
        GeositeEntry("private", "私有网络", "局域网/私有地址", listOf("地区", "bypass")),
    )

    fun search(query: String): List<GeositeEntry> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return All
        return All.filter { e ->
            e.geosite.contains(q) ||
                e.name.lowercase().contains(q) ||
                e.description.contains(q) ||
                e.tags.any { it.lowercase().contains(q) }
        }
    }
}

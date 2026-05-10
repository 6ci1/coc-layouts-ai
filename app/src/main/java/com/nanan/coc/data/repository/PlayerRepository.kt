package com.nanan.coc.data.repository

import com.nanan.coc.data.api.ClashApiService
import com.nanan.coc.data.model.PlayerInfo
import com.nanan.coc.data.model.PlayerParser
import com.nanan.coc.data.model.TechItem
import com.nanan.coc.data.model.UpgradeTimeDB
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerRepository @Inject constructor(
    private val clashApi: ClashApiService
) {
    /**
     * 获取玩家信息
     * @param playerTag 玩家标签，如 "#ABC123" 或 "ABC123"
     * @param apiKey Clash of Clans API Key
     */
    suspend fun getPlayer(playerTag: String, apiKey: String): Result<PlayerInfo> {
        return try {
            // 确保 tag 有 # 前缀，然后 URL encode
            val tag = if (playerTag.startsWith("#")) playerTag else "#$playerTag"
            val encodedTag = tag.replace("#", "%23")

            val json = clashApi.getPlayerInfo(encodedTag, "Bearer $apiKey")
            val player = PlayerParser.parse(json)
            Result.success(player)
        } catch (e: Exception) {
            Result.failure(Exception("获取玩家数据失败: ${e.message}"))
        }
    }

    /**
     * 计算科技发展情况
     * 返回所有兵种/法术/英雄的 TechItem 列表
     */
    fun calculateTech(player: PlayerInfo): List<TechItem> {
        val items = mutableListOf<TechItem>()

        // 兵种
        for (troop in player.troops) {
            val baseMax = troop.maxLevel
            val boostMax = baseMax + 2
            val current = troop.level
            val remaining = (boostMax - current).coerceAtLeast(0)
            val totalTime = if (remaining > 0) {
                UpgradeTimeDB.getUpgradeTime(troop.name, current, boostMax)
            } else 0L

            items.add(
                TechItem(
                    name = troop.name,
                    displayName = getDisplayName(troop.name),
                    category = "troop",
                    currentLevel = current,
                    baseMaxLevel = baseMax,
                    boostMaxLevel = boostMax,
                    upgradeTimePerLevel = 0L,
                    isMaxed = current >= boostMax,
                    remainingLevels = remaining,
                    totalUpgradeTime = totalTime,
                    hallLevelRequired = 0
                )
            )
        }

        // 法术
        for (spell in player.spells) {
            val baseMax = spell.maxLevel
            val boostMax = baseMax + 2
            val current = spell.level
            val remaining = (boostMax - current).coerceAtLeast(0)
            val totalTime = if (remaining > 0) {
                UpgradeTimeDB.getUpgradeTime(spell.name, current, boostMax)
            } else 0L

            items.add(
                TechItem(
                    name = spell.name,
                    displayName = getDisplayName(spell.name),
                    category = "spell",
                    currentLevel = current,
                    baseMaxLevel = baseMax,
                    boostMaxLevel = boostMax,
                    upgradeTimePerLevel = 0L,
                    isMaxed = current >= boostMax,
                    remainingLevels = remaining,
                    totalUpgradeTime = totalTime,
                    hallLevelRequired = 0
                )
            )
        }

        // 英雄
        for (hero in player.heroes) {
            val baseMax = hero.maxLevel
            val boostMax = baseMax + 2
            val current = hero.level
            val remaining = (boostMax - current).coerceAtLeast(0)
            val totalTime = if (remaining > 0) {
                UpgradeTimeDB.getHeroUpgradeTime(hero.name, current, boostMax)
            } else 0L

            items.add(
                TechItem(
                    name = hero.name,
                    displayName = getDisplayName(hero.name),
                    category = "hero",
                    currentLevel = current,
                    baseMaxLevel = baseMax,
                    boostMaxLevel = boostMax,
                    upgradeTimePerLevel = 0L,
                    isMaxed = current >= boostMax,
                    remainingLevels = remaining,
                    totalUpgradeTime = totalTime,
                    hallLevelRequired = 0
                )
            )
        }

        return items
    }

    companion object {
        private val displayNames = mapOf(
            // 兵种
            "Barbarian" to "野蛮人",
            "Archer" to "弓箭手",
            "Giant" to "巨人",
            "Goblin" to "哥布林",
            "Wall Breaker" to "炸弹人",
            "Balloon" to "气球兵",
            "Wizard" to "法师",
            "Healer" to "天使",
            "Dragon" to "飞龙",
            "P.E.K.K.A" to "皮卡超人",
            "Baby Dragon" to "小龙",
            "Miner" to "掘地矿工",
            "Electro Dragon" to "雷龙",
            "Yeti" to "雪怪",
            "Dragon Rider" to "龙骑士",
            "Electro Titan" to "雷电泰坦",
            "Root Rider" to "根蔓骑士",
            "Thrower" to "投弹手",
            "Super Barbarian" to "超级野蛮人",
            "Super Archer" to "超级弓箭手",
            "Super Giant" to "超级巨人",
            "Super Goblin" to "超级哥布林",
            "Super Wall Breaker" to "超级炸弹人",
            "Super Balloon" to "超级气球",
            "Super Wizard" to "超级法师",
            "Super Dragon" to "超级飞龙",
            "Inferno Dragon" to "地狱飞龙",
            "Super Minion" to "超级亡灵",
            "Super Valkyrie" to "超级女武神",
            "Super Witch" to "超级女巫",
            "Ice Hound" to "冰狗",
            "Super Bowler" to "超级蓝胖",
            "Super Miner" to "超级矿工",
            "Super Hog Rider" to "超级野猪骑士",
            // 暗黑兵种
            "Minion" to "亡灵",
            "Hog Rider" to "野猪骑士",
            "Valkyrie" to "女武神",
            "Golem" to "戈仑石人",
            "Witch" to "女巫",
            "Lava Hound" to "熔岩猎犬",
            "Bowler" to "蓝胖",
            "Ice Golem" to "冰石人",
            "Headhunter" to "猎头者",
            "Apprentice Warden" to "学徒守卫",
            "Druid" to "德鲁伊",
            "Furnace" to "熔炉",
            // 法术
            "Lightning Spell" to "雷电法术",
            "Heal Spell" to "治疗法术",
            "Rage Spell" to "狂暴法术",
            "Jump Spell" to "弹跳法术",
            "Freeze Spell" to "冰冻法术",
            "Clone Spell" to "镜像法术",
            "Poison Spell" to "毒药法术",
            "Earthquake Spell" to "地震法术",
            "Haste Spell" to "急速法术",
            "Bat Spell" to "蝙蝠法术",
            "Invisibility Spell" to "隐身法术",
            "Recall Spell" to "召回法术",
            "Overgrowth Spell" to "蔓生法术",
            "Revive Spell" to "复活法术",
            "Skeleton Spell" to "骷髅法术",
            // 英雄
            "Barbarian King" to "蛮王",
            "Archer Queen" to "女王",
            "Grand Warden" to "大守护者",
            "Royal Champion" to "飞盾战神",
            "Minion Prince" to "亡灵王子"
        )

        fun getDisplayName(name: String): String = displayNames[name] ?: name

        fun formatTime(seconds: Long): String {
            if (seconds <= 0) return "已完成"
            val days = seconds / 86400
            val hours = (seconds % 86400) / 3600
            val minutes = (seconds % 3600) / 60
            return buildString {
                if (days > 0) append("${days}天")
                if (hours > 0) append("${hours}时")
                if (minutes > 0 && days == 0L) append("${minutes}分")
                if (isEmpty()) append("不足1分")
            }
        }
    }
}

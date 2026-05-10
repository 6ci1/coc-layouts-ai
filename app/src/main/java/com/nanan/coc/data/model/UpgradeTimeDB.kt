package com.nanan.coc.data.model

/**
 * COC 兵种/法术/英雄升级时间数据库
 * key = "TroopName:level" → 升级到该级所需时间（秒）
 * 升级时间是从上一级升到当前级的时间
 */
object UpgradeTimeDB {

    // 升级时间（秒）：从 level-1 升到 level
    // 来源：COC Wiki 升级时间表（TH17版本）
    private val upgradeTimes = mapOf(
        // === 兵种 ===
        // Barbarian 野蛮人 (max 12)
        "Barbarian:2" to 60L,
        "Barbarian:3" to 300L,
        "Barbarian:4" to 3600L,
        "Barbarian:5" to 14400L,
        "Barbarian:6" to 43200L,
        "Barbarian:7" to 86400L,
        "Barbarian:8" to 172800L,
        "Barbarian:9" to 259200L,
        "Barbarian:10" to 345600L,
        "Barbarian:11" to 518400L,
        "Barbarian:12" to 691200L,

        // Archer 弓箭手 (max 12)
        "Archer:2" to 60L,
        "Archer:3" to 300L,
        "Archer:4" to 3600L,
        "Archer:5" to 14400L,
        "Archer:6" to 43200L,
        "Archer:7" to 86400L,
        "Archer:8" to 172800L,
        "Archer:9" to 259200L,
        "Archer:10" to 345600L,
        "Archer:11" to 518400L,
        "Archer:12" to 691200L,

        // Giant 巨人 (max 12)
        "Giant:2" to 60L,
        "Giant:3" to 1800L,
        "Giant:4" to 7200L,
        "Giant:5" to 28800L,
        "Giant:6" to 57600L,
        "Giant:7" to 115200L,
        "Giant:8" to 172800L,
        "Giant:9" to 259200L,
        "Giant:10" to 345600L,
        "Giant:11" to 518400L,
        "Giant:12" to 691200L,

        // Goblin 哥布林 (max 9)
        "Goblin:2" to 60L,
        "Goblin:3" to 1800L,
        "Goblin:4" to 7200L,
        "Goblin:5" to 28800L,
        "Goblin:6" to 57600L,
        "Goblin:7" to 115200L,
        "Goblin:8" to 259200L,
        "Goblin:9" to 432000L,

        // Wall Breaker 炸弹人 (max 11)
        "Wall Breaker:2" to 60L,
        "Wall Breaker:3" to 1800L,
        "Wall Breaker:4" to 7200L,
        "Wall Breaker:5" to 28800L,
        "Wall Breaker:6" to 57600L,
        "Wall Breaker:7" to 115200L,
        "Wall Breaker:8" to 172800L,
        "Wall Breaker:9" to 345600L,
        "Wall Breaker:10" to 518400L,
        "Wall Breaker:11" to 691200L,

        // Balloon 气球 (max 10)
        "Balloon:2" to 1800L,
        "Balloon:3" to 7200L,
        "Balloon:4" to 28800L,
        "Balloon:5" to 57600L,
        "Balloon:6" to 115200L,
        "Balloon:7" to 172800L,
        "Balloon:8" to 345600L,
        "Balloon:9" to 518400L,
        "Balloon:10" to 691200L,

        // Wizard 法师 (max 12)
        "Wizard:2" to 60L,
        "Wizard:3" to 1800L,
        "Wizard:4" to 7200L,
        "Wizard:5" to 28800L,
        "Wizard:6" to 57600L,
        "Wizard:7" to 115200L,
        "Wizard:8" to 172800L,
        "Wizard:9" to 259200L,
        "Wizard:10" to 345600L,
        "Wizard:11" to 518400L,
        "Wizard:12" to 691200L,

        // Healer 治疗 (max 8)
        "Healer:2" to 7200L,
        "Healer:3" to 28800L,
        "Healer:4" to 86400L,
        "Healer:5" to 172800L,
        "Healer:6" to 345600L,
        "Healer:7" to 518400L,
        "Healer:8" to 691200L,

        // Dragon 龙 (max 11)
        "Dragon:2" to 7200L,
        "Dragon:3" to 28800L,
        "Dragon:4" to 57600L,
        "Dragon:5" to 115200L,
        "Dragon:6" to 172800L,
        "Dragon:7" to 259200L,
        "Dragon:8" to 345600L,
        "Dragon:9" to 518400L,
        "Dragon:10" to 604800L,
        "Dragon:11" to 691200L,

        // P.E.K.K.A (max 10)
        "P.E.K.K.A:2" to 7200L,
        "P.E.K.K.A:3" to 28800L,
        "P.E.K.K.A:4" to 57600L,
        "P.E.K.K.A:5" to 115200L,
        "P.E.K.K.A:6" to 172800L,
        "P.E.K.K.A:7" to 259200L,
        "P.E.K.K.A:8" to 345600L,
        "P.E.K.K.A:9" to 518400L,
        "P.E.K.K.A:10" to 691200L,

        // Baby Dragon (max 9)
        "Baby Dragon:2" to 86400L,
        "Baby Dragon:3" to 172800L,
        "Baby Dragon:4" to 259200L,
        "Baby Dragon:5" to 345600L,
        "Baby Dragon:6" to 432000L,
        "Baby Dragon:7" to 518400L,
        "Baby Dragon:8" to 604800L,
        "Baby Dragon:9" to 691200L,

        // Miner (max 9)
        "Miner:2" to 86400L,
        "Miner:3" to 172800L,
        "Miner:4" to 259200L,
        "Miner:5" to 345600L,
        "Miner:6" to 432000L,
        "Miner:7" to 518400L,
        "Miner:8" to 604800L,
        "Miner:9" to 691200L,

        // Electro Dragon (max 7)
        "Electro Dragon:2" to 172800L,
        "Electro Dragon:3" to 259200L,
        "Electro Dragon:4" to 345600L,
        "Electro Dragon:5" to 518400L,
        "Electro Dragon:6" to 604800L,
        "Electro Dragon:7" to 691200L,

        // Yeti (max 6)
        "Yeti:2" to 172800L,
        "Yeti:3" to 345600L,
        "Yeti:4" to 518400L,
        "Yeti:5" to 604800L,
        "Yeti:6" to 691200L,

        // Dragon Rider (max 4)
        "Dragon Rider:2" to 345600L,
        "Dragon Rider:3" to 518400L,
        "Dragon Rider:4" to 691200L,

        // Electro Titan (max 4)
        "Electro Titan:2" to 345600L,
        "Electro Titan:3" to 518400L,
        "Electro Titan:4" to 691200L,

        // Root Rider (max 3)
        "Root Rider:2" to 518400L,
        "Root Rider:3" to 691200L,

        // Thrower (max 3)
        "Thrower:2" to 518400L,
        "Thrower:3" to 691200L,

        // Minion (max 10)
        "Minion:2" to 7200L,
        "Minion:3" to 14400L,
        "Minion:4" to 28800L,
        "Minion:5" to 57600L,
        "Minion:6" to 86400L,
        "Minion:7" to 172800L,
        "Minion:8" to 259200L,
        "Minion:9" to 345600L,
        "Minion:10" to 518400L,

        // Hog Rider (max 12)
        "Hog Rider:2" to 7200L,
        "Hog Rider:3" to 14400L,
        "Hog Rider:4" to 28800L,
        "Hog Rider:5" to 57600L,
        "Hog Rider:6" to 86400L,
        "Hog Rider:7" to 172800L,
        "Hog Rider:8" to 259200L,
        "Hog Rider:9" to 345600L,
        "Hog Rider:10" to 432000L,
        "Hog Rider:11" to 518400L,
        "Hog Rider:12" to 691200L,

        // Valkyrie (max 9)
        "Valkyrie:2" to 7200L,
        "Valkyrie:3" to 14400L,
        "Valkyrie:4" to 28800L,
        "Valkyrie:5" to 57600L,
        "Valkyrie:6" to 86400L,
        "Valkyrie:7" to 172800L,
        "Valkyrie:8" to 345600L,
        "Valkyrie:9" to 518400L,

        // Golem (max 10)
        "Golem:2" to 14400L,
        "Golem:3" to 28800L,
        "Golem:4" to 57600L,
        "Golem:5" to 86400L,
        "Golem:6" to 172800L,
        "Golem:7" to 259200L,
        "Golem:8" to 345600L,
        "Golem:9" to 518400L,
        "Golem:10" to 691200L,

        // Witch (max 7)
        "Witch:2" to 57600L,
        "Witch:3" to 115200L,
        "Witch:4" to 172800L,
        "Witch:5" to 259200L,
        "Witch:6" to 345600L,
        "Witch:7" to 518400L,

        // Lava Hound (max 7)
        "Lava Hound:2" to 57600L,
        "Lava Hound:3" to 115200L,
        "Lava Hound:4" to 172800L,
        "Lava Hound:5" to 259200L,
        "Lava Hound:6" to 345600L,
        "Lava Hound:7" to 518400L,

        // Bowler (max 7)
        "Bowler:2" to 86400L,
        "Bowler:3" to 172800L,
        "Bowler:4" to 259200L,
        "Bowler:5" to 345600L,
        "Bowler:6" to 518400L,
        "Bowler:7" to 691200L,

        // Ice Golem (max 7)
        "Ice Golem:2" to 86400L,
        "Ice Golem:3" to 172800L,
        "Ice Golem:4" to 259200L,
        "Ice Golem:5" to 345600L,
        "Ice Golem:6" to 518400L,
        "Ice Golem:7" to 691200L,

        // Headhunter (max 4)
        "Headhunter:2" to 172800L,
        "Headhunter:3" to 345600L,
        "Headhunter:4" to 518400L,

        // Apprentice Warden (max 4)
        "Apprentice Warden:2" to 172800L,
        "Apprentice Warden:3" to 345600L,
        "Apprentice Warden:4" to 518400L,

        // Druid (max 3)
        "Druid:2" to 345600L,
        "Druid:3" to 518400L,

        // Furnace (max 3)
        "Furnace:2" to 345600L,
        "Furnace:3" to 518400L,

        // === 法术 ===
        // Lightning Spell (max 11)
        "Lightning Spell:2" to 1800L,
        "Lightning Spell:3" to 7200L,
        "Lightning Spell:4" to 14400L,
        "Lightning Spell:5" to 28800L,
        "Lightning Spell:6" to 57600L,
        "Lightning Spell:7" to 86400L,
        "Lightning Spell:8" to 172800L,
        "Lightning Spell:9" to 259200L,
        "Lightning Spell:10" to 345600L,
        "Lightning Spell:11" to 518400L,

        // Heal Spell (max 9)
        "Heal Spell:2" to 1800L,
        "Heal Spell:3" to 7200L,
        "Heal Spell:4" to 14400L,
        "Heal Spell:5" to 28800L,
        "Heal Spell:6" to 57600L,
        "Heal Spell:7" to 86400L,
        "Heal Spell:8" to 172800L,
        "Heal Spell:9" to 345600L,

        // Rage Spell (max 6)
        "Rage Spell:2" to 14400L,
        "Rage Spell:3" to 28800L,
        "Rage Spell:4" to 57600L,
        "Rage Spell:5" to 172800L,
        "Rage Spell:6" to 345600L,

        // Jump Spell (max 5)
        "Jump Spell:2" to 57600L,
        "Jump Spell:3" to 172800L,
        "Jump Spell:4" to 345600L,
        "Jump Spell:5" to 518400L,

        // Freeze Spell (max 7)
        "Freeze Spell:2" to 28800L,
        "Freeze Spell:3" to 57600L,
        "Freeze Spell:4" to 86400L,
        "Freeze Spell:5" to 172800L,
        "Freeze Spell:6" to 259200L,
        "Freeze Spell:7" to 345600L,

        // Clone Spell (max 7)
        "Clone Spell:2" to 86400L,
        "Clone Spell:3" to 172800L,
        "Clone Spell:4" to 259200L,
        "Clone Spell:5" to 345600L,
        "Clone Spell:6" to 518400L,
        "Clone Spell:7" to 604800L,

        // Poison Spell (max 8)
        "Poison Spell:2" to 14400L,
        "Poison Spell:3" to 28800L,
        "Poison Spell:4" to 57600L,
        "Poison Spell:5" to 86400L,
        "Poison Spell:6" to 172800L,
        "Poison Spell:7" to 259200L,
        "Poison Spell:8" to 345600L,

        // Earthquake Spell (max 5)
        "Earthquake Spell:2" to 14400L,
        "Earthquake Spell:3" to 28800L,
        "Earthquake Spell:4" to 86400L,
        "Earthquake Spell:5" to 172800L,

        // Haste Spell (max 5)
        "Haste Spell:2" to 14400L,
        "Haste Spell:3" to 28800L,
        "Haste Spell:4" to 86400L,
        "Haste Spell:5" to 172800L,

        // Bat Spell (max 6)
        "Bat Spell:2" to 57600L,
        "Bat Spell:3" to 115200L,
        "Bat Spell:4" to 172800L,
        "Bat Spell:5" to 259200L,
        "Bat Spell:6" to 345600L,

        // Invisibility Spell (max 4)
        "Invisibility Spell:2" to 172800L,
        "Invisibility Spell:3" to 259200L,
        "Invisibility Spell:4" to 345600L,

        // Recall Spell (max 4)
        "Recall Spell:2" to 172800L,
        "Recall Spell:3" to 259200L,
        "Recall Spell:4" to 345600L,

        // Overgrowth Spell (max 3)
        "Overgrowth Spell:2" to 259200L,
        "Overgrowth Spell:3" to 432000L,

        // Revive Spell (max 3)
        "Revive Spell:2" to 259200L,
        "Revive Spell:3" to 432000L,

        // === 英雄 ===
        // Barbarian King (max 95 → 升级时间因等级段差异大，简化处理)
        // Archer Queen (max 95)
        // Grand Warden (max 65)
        // Royal Champion (max 40)
        // Minion Prince (max 80)
    )

    /**
     * 获取从 fromLevel 升到 toLevel 的总升级时间（秒）
     */
    fun getUpgradeTime(troopName: String, fromLevel: Int, toLevel: Int): Long {
        var total = 0L
        for (level in (fromLevel + 1)..toLevel) {
            total += upgradeTimes["$troopName:$level"] ?: 0L
        }
        return total
    }

    /**
     * 英雄升级时间估算（按等级段）
     * 英雄升级时间跨度大，用分段估算
     */
    fun getHeroUpgradeTime(heroName: String, fromLevel: Int, toLevel: Int): Long {
        var total = 0L
        for (level in (fromLevel + 1)..toLevel) {
            total += estimateHeroLevelTime(level)
        }
        return total
    }

    private fun estimateHeroLevelTime(level: Int): Long {
        return when {
            level <= 10 -> 43200L       // 12h
            level <= 20 -> 86400L       // 1d
            level <= 30 -> 172800L      // 2d
            level <= 40 -> 259200L      // 3d
            level <= 50 -> 345600L      // 4d
            level <= 60 -> 432000L      // 5d
            level <= 70 -> 518400L      // 6d
            level <= 80 -> 604800L      // 7d
            level <= 90 -> 691200L      // 8d
            else -> 691200L             // 8d
        }
    }
}

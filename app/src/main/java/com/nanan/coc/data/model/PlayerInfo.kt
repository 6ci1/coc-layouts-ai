package com.nanan.coc.data.model

import org.json.JSONObject

data class PlayerInfo(
    val name: String,
    val tag: String,
    val townHallLevel: Int,
    val townHallWeaponLevel: Int = 0,
    val expLevel: Int = 0,
    val trophies: Int = 0,
    val league: String = "",
    val troops: List<TroopInfo>,
    val spells: List<SpellInfo>,
    val heroes: List<HeroInfo>
)

data class TroopInfo(
    val name: String,
    val displayName: String,
    val level: Int,
    val maxLevel: Int,
    val village: String = "home"
)

data class SpellInfo(
    val name: String,
    val displayName: String,
    val level: Int,
    val maxLevel: Int,
    val village: String = "home"
)

data class HeroInfo(
    val name: String,
    val displayName: String,
    val level: Int,
    val maxLevel: Int,
    val village: String = "home"
)

data class TechItem(
    val name: String,
    val displayName: String,
    val category: String, // "troop", "spell", "hero"
    val currentLevel: Int,
    val baseMaxLevel: Int,
    val boostMaxLevel: Int, // baseMax + 2
    val upgradeTimePerLevel: Long, // seconds per remaining level
    val isMaxed: Boolean,
    val remainingLevels: Int,
    val totalUpgradeTime: Long, // total seconds to reach boost max
    val hallLevelRequired: Int // TH level required for this troop's max
)

object PlayerParser {

    fun parse(json: String): PlayerInfo {
        val obj = JSONObject(json)

        val troops = mutableListOf<TroopInfo>()
        obj.optJSONArray("troops")?.let { arr ->
            for (i in 0 until arr.length()) {
                val t = arr.getJSONObject(i)
                if (t.optString("village") == "home") {
                    troops.add(
                        TroopInfo(
                            name = t.getString("name"),
                            displayName = t.getString("name"),
                            level = t.getInt("level"),
                            maxLevel = t.getInt("maxLevel"),
                            village = "home"
                        )
                    )
                }
            }
        }

        val spells = mutableListOf<SpellInfo>()
        obj.optJSONArray("spells")?.let { arr ->
            for (i in 0 until arr.length()) {
                val s = arr.getJSONObject(i)
                if (s.optString("village") == "home") {
                    spells.add(
                        SpellInfo(
                            name = s.getString("name"),
                            displayName = s.getString("name"),
                            level = s.getInt("level"),
                            maxLevel = s.getInt("maxLevel"),
                            village = "home"
                        )
                    )
                }
            }
        }

        val heroes = mutableListOf<HeroInfo>()
        obj.optJSONArray("heroes")?.let { arr ->
            for (i in 0 until arr.length()) {
                val h = arr.getJSONObject(i)
                if (h.optString("village") == "home") {
                    heroes.add(
                        HeroInfo(
                            name = h.getString("name"),
                            displayName = h.getString("name"),
                            level = h.getInt("level"),
                            maxLevel = h.getInt("maxLevel"),
                            village = "home"
                        )
                    )
                }
            }
        }

        val league = obj.optJSONObject("league")?.optString("name") ?: ""

        return PlayerInfo(
            name = obj.getString("name"),
            tag = obj.getString("tag"),
            townHallLevel = obj.getInt("townHallLevel"),
            townHallWeaponLevel = obj.optInt("townHallWeaponLevel", 0),
            expLevel = obj.optInt("expLevel", 0),
            trophies = obj.optInt("trophies", 0),
            league = league,
            troops = troops,
            spells = spells,
            heroes = heroes
        )
    }
}

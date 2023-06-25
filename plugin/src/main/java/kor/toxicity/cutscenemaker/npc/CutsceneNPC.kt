package kor.toxicity.cutscenemaker.npc

import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import kor.toxicity.cutscenemaker.CutsceneManager
import kor.toxicity.cutscenemaker.data.NPCData
import kor.toxicity.cutscenemaker.nms.NMSChecker
import kor.toxicity.cutscenemaker.nms.NPC
import kor.toxicity.cutscenemaker.util.HttpUtil
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.metadata.FixedMetadataValue
import java.util.*

class CutsceneNPC(manager: CutsceneManager, key: String, section: ConfigurationSection) {

    private val origin = if (section.isConfigurationSection("Location")) {
        section.getConfigurationSection("Location").run {
            Bukkit.getWorld(getString("World","world"))?.run {
                Location(
                    this,
                    getDouble("x"),
                    getDouble("y"),
                    getDouble("z"),
                    getDouble("Pitch").toFloat(),
                    getDouble("Yaw").toFloat()
                )
            } ?: throw RuntimeException("World not found!")
        }
    } else if (section.isString("Location")) {
        val loc = section.getString("Location")
        manager.locations.getValue(loc) ?: throw RuntimeException("Unable to find this location: $loc")
    } else {
        throw RuntimeException("The section named \"Location\" not found!")
    }
    var player: NPC? = null
    init {
        section.run {
            getString("Skin")?.run {
                manager.runTaskAsynchronously {
                    var profile: GameProfile? = null
                    while (profile == null) profile = HttpUtil.getPlayerGameProfile(this)
                    manager.runTask {
                        player = NMSChecker.getHandler()?.run {
                            getFakePlayer(profile,origin).apply {
                                setMetadata(NPCData.METADATA_KEY,FixedMetadataValue(manager.plugin,true))
                            }
                        }
                    }
                }
            }
        } ?: run {
            player = NMSChecker.getHandler()?.run {
                getFakePlayer(GameProfile(UUID.randomUUID(),key).apply {
                    section.getString("Value")?.run {
                        properties.put("textures", Property("textures",this,section.getString("Signature")))
                    }
                },origin).apply {
                    setMetadata(NPCData.METADATA_KEY,FixedMetadataValue(manager.plugin,true))
                }
            }
        }
    }

    fun teleportToOrigin() {
        player?.teleport(origin,PlayerTeleportEvent.TeleportCause.PLUGIN)
    }
    fun kill() {
        player?.remove()
    }
}
package kor.toxicity.cutscenemaker.command

import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

enum class SenderType(
    val display: String,
    private val sender: Class<out CommandSender>
) {
    PLAYER("Player",Player::class.java),
    CONSOLE("Console",ConsoleCommandSender::class.java),
    LIVING_ENTITY("Entity",LivingEntity::class.java)
    ;
    fun accept(sender: Class<out CommandSender>) = this.sender.isAssignableFrom(sender)
}
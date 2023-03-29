package kor.toxicity.cutscenemaker.commands

import kor.toxicity.cutscenemaker.CC
import kor.toxicity.cutscenemaker.CM
import org.bukkit.command.CommandSender
import java.lang.StringBuilder

class CommandAPI(prefix: String) {
    private val commandMap = LinkedHashMap<String, CommandData>()
    init {
        create("list").apply {
            aliases = arrayOf("l","리스트")
            description = "show the list of registered command."
            usage = "list"
            opOnly = false
            executor = { sender,_ ->
               commandMap.values.forEach {
                   if (!it.opOnly || sender.isOp) CM.send(sender,"/$prefix ${it.usage} ${
                       if (it.aliases.isNotEmpty()) StringBuilder().append(CC.GRAY.toString()).append('(').apply {
                           it.aliases.forEachIndexed { index, s ->
                               append(s)
                               if (index < it.aliases.size - 1) append(",")
                           }
                       }.append(')').append(CC.WHITE.toString()) else ""
                   }: ${it.description}")
               }
            }
        }.done()
    }


    fun create(tag: String) = CommandBuilder(tag)

    fun execute(command: String, sender: CommandSender, args: Array<String>) {
        (commandMap[command] ?: commandMap.values.firstOrNull { it.aliases.contains(command) })?.run {
            if (opOnly && !sender.isOp) return CM.send(sender,"On only command.")
            if (length > args.size - 1) return CM.send(sender,"this command requires at least $length of arguments.")
            if (allowedSender.none { it.accept(sender.javaClass) }) return CM.send("allowed sender type: ${
                StringBuilder().apply {
                    allowedSender.forEachIndexed { index, senderType ->
                        append(senderType.display)
                        if (index < allowedSender.size - 1) append(", ")
                    }
                }
            }")
            executor(sender,args)
        } ?: CM.send(sender,"Unknown Command: $command")
    }
    fun tabComplete(command: String, sender: CommandSender, args: Array<String>): List<String>? = (commandMap[command] ?: commandMap.values.firstOrNull { it.aliases.contains(command) })?.run {
        if (opOnly && !sender.isOp) return null
        tabComplete(sender,args)
    }
    fun getCommandList(sender: CommandSender) = commandMap.entries.filter {
        !it.value.opOnly || sender.isOp
    }.map {
        it.key
    }
    fun searchCommand(prefix: String, sender: CommandSender): List<String> = ArrayList(getCommandList(sender)).apply {
        commandMap.values.forEach {
            if (!it.opOnly || sender.isOp) addAll(it.aliases)
        }
    }.filter {
        it.startsWith(prefix)
    }

    inner class CommandBuilder(private val tag: String) {
        var aliases: Array<String> = emptyArray()
        var length = 0
        var description = "알 수 없는 설명"
        var usage = "알 수 없는 사용법"
        var opOnly = true
        var allowedSender: Array<SenderType> = arrayOf(SenderType.CONSOLE, SenderType.LIVING_ENTITY)
        var executor: (CommandSender,Array<String>) -> Unit = { _,_ ->
        }
        var tabComplete: (CommandSender,Array<String>) -> List<String>? = { _,_ ->
            null
        }

        fun done(): CommandAPI {
            commandMap[tag] = CommandData(
                length = length,
                aliases = aliases,
                description = description,
                usage = usage,
                opOnly = opOnly,
                allowedSender = allowedSender,
                executor = executor,
                tabComplete = tabComplete,
            )
            return this@CommandAPI
        }
    }
    private class CommandData(
        val aliases: Array<String>,
        val description: String,
        val length: Int,
        val usage: String,
        val opOnly: Boolean,
        val allowedSender: Array<SenderType>,
        val executor: (CommandSender,Array<String>) -> Unit,
        val tabComplete:  (CommandSender,Array<String>) -> List<String>?
    )
}
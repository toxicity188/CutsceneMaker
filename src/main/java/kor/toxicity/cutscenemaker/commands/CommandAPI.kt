package kor.toxicity.cutscenemaker.commands

import kor.toxicity.cutscenemaker.CC
import kor.toxicity.cutscenemaker.CM
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import java.lang.StringBuilder
import java.util.regex.Pattern

class CommandAPI(prefix: String) {
    companion object {
        private val NUMBER_PATTERN = Pattern.compile("^(?<number>([1-9])+)")
    }
    private val commandMap = LinkedHashMap<String, CommandData>()
    private val defaultCommand: (CommandSender,Array<String>) -> Unit = { sender, args ->
        val s = commandMap.size / 6 + 1
        val page = if (args.size > 1) try {
            args[args.size - 1].toInt().coerceAtLeast(1).coerceAtMost(s)
        } catch (ex: Exception) {
            1
        } else 1
        val p = (page - 1) * 6
        sender.sendMessage("")
        sender.sendMessage("${CC.YELLOW}[${CC.WHITE}/${CC.GOLD}$prefix${CC.YELLOW}] ${CC.GRAY}==========< page $page / $s >==========")
        sender.sendMessage("")
        sender.sendMessage(" ${CC.GRAY}Arguments: ${CC.YELLOW}<arg> ${CC.GRAY}- ${CC.WHITE}required${CC.GRAY}, ${CC.DARK_AQUA}[arg] ${CC.GRAY}- ${CC.WHITE}optional")
        sender.sendMessage(" ${CC.GRAY}Aliases: (aliases)")
        sender.sendMessage("")
        val value = commandMap.values.toList()
        value.subList(p,(p + 6).coerceAtMost(value.size)).forEach {
            if (!it.opOnly || sender.isOp) sender.sendMessage("/${CC.GOLD}$prefix ${CC.WHITE}${it.usage} ${
                if (it.aliases.isNotEmpty()) StringBuilder().append(CC.GRAY).append('(').apply {
                    it.aliases.forEachIndexed { index, s ->
                        append(s)
                        if (index < it.aliases.size - 1) append(",")
                    }
                }.append(')').append(CC.WHITE) else CC.WHITE.toString()
            } - ${CC.GREEN}${it.description}")
        }
        sender.sendMessage("")
    }
    init {
        create("help").apply {
            aliases = arrayOf("h","l","리스트","도움말")
            description = "show the list of registered command."
            usage = "help ${CC.DARK_AQUA}[page]"
            opOnly = false
            executor = defaultCommand
        }.done()
    }


    fun create(tag: String) = CommandBuilder(tag)

    fun execute(command: String, sender: CommandSender, args: Array<String>) {
        (commandMap[command] ?: commandMap.values.firstOrNull { it.aliases.contains(command) })?.run {
            if (opOnly && !sender.isOp) return CM.send(sender,"On only command.")
            if (length > args.size - 1) return CM.send(sender,"this command requires at least $length arguments.")
            if (allowedSender.none { it.accept(sender.javaClass) }) return CM.send("allowed sender type: ${
                StringBuilder().apply {
                    allowedSender.forEachIndexed { index, senderType ->
                        append(senderType.display)
                        if (index < allowedSender.size - 1) append(", ")
                    }
                }
            }")
            executor(sender,args)
        } ?: run {
            val matcher = NUMBER_PATTERN.matcher(args.last())
            if (matcher.find()) {
                val l = args.toMutableList()
                l[l.size - 1] = "help"
                l.add(matcher.group("number"))
                defaultCommand(sender,l.toTypedArray())
            } else defaultCommand(sender,args)
        }
    }
    fun tabComplete(command: String, sender: CommandSender, args: Array<String>): List<String>? = (commandMap[command] ?: commandMap.values.firstOrNull { it.aliases.contains(command) })?.run {
        if (opOnly && !sender.isOp) return null
        tabComplete(sender,args)
    }
    private fun getCommandList(sender: CommandSender) = commandMap.entries.filter {
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
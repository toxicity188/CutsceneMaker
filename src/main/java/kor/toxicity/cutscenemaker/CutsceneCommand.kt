package kor.toxicity.cutscenemaker

import kor.toxicity.cutscenemaker.commands.CommandAPI
import kor.toxicity.cutscenemaker.commands.SenderType
import kor.toxicity.cutscenemaker.data.ActionData
import kor.toxicity.cutscenemaker.data.ItemData
import kor.toxicity.cutscenemaker.quests.EditorSupplier
import kor.toxicity.cutscenemaker.util.ConfigWriter
import kor.toxicity.cutscenemaker.util.EvtUtil
import kor.toxicity.cutscenemaker.util.InvUtil
import kor.toxicity.cutscenemaker.util.StorageItem
import kor.toxicity.cutscenemaker.util.blockanims.BlockAnimation
import kor.toxicity.cutscenemaker.util.databases.CutsceneDB
import kor.toxicity.cutscenemaker.util.gui.CallbackManager
import kor.toxicity.cutscenemaker.util.vars.Vars
import kor.toxicity.cutscenemaker.util.vars.VarsContainer
import org.bukkit.*
import org.bukkit.command.*
import org.bukkit.configuration.MemoryConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.time.LocalDateTime
import java.util.*


typealias CM = CutsceneMaker
typealias CC = ChatColor

class CutsceneCommand(private val plugin: CutsceneMaker): TabExecutor, Listener {
    companion object {
        private const val FALLBACK_PREFIX = "cutscenemaker"
        private val registeredCommand: MutableSet<PluginCommand> = HashSet()
        private var commandMap: SimpleCommandMap? = null
        private var createCommand: ((String) -> PluginCommand)? = null
        private val WAND = ItemStack(Material.BOOK).apply {
            itemMeta = itemMeta.apply {
                displayName = "${ChatColor.YELLOW}Cutscene Wand"
                lore = listOf("${ChatColor.WHITE}Left - pos1, Right - pos2")
                addEnchant(Enchantment.DURABILITY,0,true)
                ItemFlag.values().forEach { addItemFlags(it) }
            }
        }
        private val ADMIN_DATA_MAP: MutableMap<Player, AdminData> = HashMap()

        fun createCommand(name: String, executor: CommandExecutor?) {
            if (commandMap == null || createCommand == null) return
            try {
                val command = createCommand!!.invoke(name.lowercase(Locale.getDefault()))
                command.description = "generated command."
                command.label = name
                command.usage = "/$name"
                command.executor = executor
                commandMap!!.register(FALLBACK_PREFIX, command)
                registeredCommand.add(command)
            } catch (e: Exception) {
                CutsceneMaker.warn("unable to register command.")
            }
        }
    }

    fun unregister() {
        val field = SimpleCommandMap::class.java.declaredFields.firstOrNull { f: Field ->
            MutableMap::class.java.isAssignableFrom(
                f.type
            )
        }
        try {
            if (field != null) {
                field.isAccessible = true
                val knownCommand = field[commandMap] as MutableMap<*, *>
                registeredCommand.forEach { p: PluginCommand ->
                    knownCommand.remove(p.name)
                    knownCommand.remove(FALLBACK_PREFIX + ":" + p.name)
                    p.unregister(commandMap)
                }
                field.isAccessible = false
                registeredCommand.clear()
            }
        } catch (e: Exception) {
            CutsceneMaker.warn("unable to unregister command.")
        }
    }
    init {
        EvtUtil.register(plugin, this)
        try {
            val map = Bukkit.getServer().javaClass.declaredFields.firstOrNull { f: Field ->
                SimpleCommandMap::class.java.isAssignableFrom(
                    f.type
                )
            }
            if (map != null) {
                map.isAccessible = true
                commandMap = map[Bukkit.getServer()] as SimpleCommandMap
                val constructor =
                    PluginCommand::class.java.getDeclaredConstructor(String::class.java, Plugin::class.java)
                constructor.isAccessible = true
                createCommand = { s: String? ->
                        try {
                            constructor.newInstance(s, plugin)
                        } catch (e: InstantiationException) {
                            throw RuntimeException(e)
                        } catch (e: IllegalAccessException) {
                            throw RuntimeException(e)
                        } catch (e: InvocationTargetException) {
                            throw RuntimeException(e)
                        }
                    }
            } else CutsceneMaker.warn("unable to find command map.")
        } catch (e: Exception) {
            CutsceneMaker.warn("unable to load command map.")
        }
    }
    private val commandAPI = run {
        val temp = CommandAPI("cutscene temp")
            .create("preset").apply {
                aliases = arrayOf("p","프리셋")
                description = "save the preset"
                usage = "preset <file> <key>"
                length = 3
                allowedSender = arrayOf(SenderType.PLAYER)
                executor = { sender, args ->
                    (sender as Player).run {
                        CallbackManager.callbackInventory(this,InvUtil.create("put your item in here!",5)) {
                            if (it.isEmpty()) CM.send(this,"no item found!")
                            else plugin.manager.runTaskAsynchronously {
                                try {
                                    val file = File(File(plugin.dataFolder,"Preset").apply { mkdir() },"${args[2]}.yml").apply {
                                        if (!exists()) createNewFile()
                                    }
                                    YamlConfiguration().run {
                                        load(file)
                                        set(args[3],MemoryConfiguration().apply {
                                            it.values.forEachIndexed { index, itemStack ->
                                                set(index.toString(), itemStack)
                                            }
                                        })
                                        save(file)
                                    }
                                    CM.send(this,"successfully saved.")
                                } catch (ex: Exception) {
                                    CM.send(this,"an error has occurred.")
                                }
                            }
                        }
                    }
                }
                tabComplete = { _,args ->
                    if (args.size == 3) File(plugin.dataFolder, "Preset").listFiles()?.run {
                        toList().map { it.nameWithoutExtension }.filter { it.startsWith(args[2]) }
                    } ?: emptyList() else null
                }
            }.done()
            .create("give").apply {
                aliases = arrayOf("g","지급")
                description = "give the preset"
                usage = "give <file> <key> <player> [hour]"
                length = 4
                executor = { sender, args ->
                    fun giveOnlinePlayer(player: Player, item: ItemStack, hour: Int) {
                        plugin.manager.getVars(player).tempStorage.add(StorageItem(item, LocalDateTime.now(), hour))
                    }
                    fun giveOfflinePlayer(player: OfflinePlayer, item: ItemStack, hour: Int) {
                        val container = CutsceneDB.read(player,plugin)
                        container.tempStorage.add(StorageItem(item, LocalDateTime.now(), hour))
                        CutsceneDB.save(player, plugin, container)
                    }
                    plugin.manager.runTaskAsynchronously {
                        try {
                            val hour: Int = try {
                                if (args.size > 5) args[5].toInt() else -1
                            } catch (ex: Exception) {
                                -1
                            }
                            val file = File(File(plugin.dataFolder, "Preset").apply { mkdir() }, "${args[2]}.yml").apply {
                                if (!exists()) createNewFile()
                            }
                            val list: List<OfflinePlayer> = when (args[4]) {
                                "*" -> Bukkit.getOfflinePlayers().toList()
                                "*online" -> Bukkit.getOnlinePlayers().toList()
                                "*offline" -> Bukkit.getOfflinePlayers().filter { !it.isOnline }
                                else -> {
                                    Bukkit.getPlayer(args[4])?.run {
                                        listOf(this)
                                    } ?: Bukkit.getOfflinePlayers().firstOrNull { it.name == args[4] }?.run {
                                        listOf(this)
                                    } ?: emptyList()
                                }
                            }
                            if (list.isEmpty()) CM.send(sender, "player not found.")
                            else YamlConfiguration().run {
                                load(file)
                                if (isConfigurationSection(args[3])) {
                                    val section = getConfigurationSection(args[3])
                                    section.getKeys(false).forEach { key ->
                                        if (section.isItemStack(key)) {
                                            val i = section.getItemStack(key)
                                            list.forEach {
                                                if (it is Player) giveOnlinePlayer(it,i,hour) else giveOfflinePlayer(it,i,hour)
                                            }
                                        }
                                    }
                                } else CM.send(sender, "this key doesn't exist: ${args[3]}")
                            }
                            CM.send(sender, "successfully given.")
                        } catch (ex: Exception) {
                            CM.send(sender, "an error has occurred.")
                        }
                    }
                }
                tabComplete = { _,args ->
                    if (args.size == 3) File(plugin.dataFolder, "Preset").listFiles()?.run {
                        toList().map { it.nameWithoutExtension }.filter { it.startsWith(args[2]) }
                    } ?: emptyList() else null
                }
            }.done()
        val item = CommandAPI("cutscene item")
            .create("get").apply {
                aliases = arrayOf("g","지급")
                description = "get the item"
                usage = "get <key>"
                length = 2
                allowedSender = arrayOf(SenderType.PLAYER)
                executor = { sender, args ->
                    (sender as Player).run {
                        ItemData.getItem(this, args[2])?.run {
                            inventory.addItem(this)
                            CutsceneMaker.send(sender, "successfully given.")
                        } ?: run {
                            CutsceneMaker.send(sender, "item not found: ${args[2]}")
                        }
                    }
                }
                tabComplete = { _,args ->
                    if (args.size == 3) ItemData.getItemKeys().filter { it.startsWith(args[2]) } else null
                }
            }
            .done()
            .create("set").apply {
                aliases = arrayOf("s","설정")
                description = "set the item"
                usage = "set <file> <key>"
                length = 3
                allowedSender = arrayOf(SenderType.PLAYER)
                executor = { sender, args ->
                    (sender as Player).run {
                        val item = inventory.itemInMainHand
                        if (item == null || item.type == Material.AIR) {
                            CutsceneMaker.send(sender, "hold the item you want to save.")
                        } else plugin.manager.runTaskAsynchronously {
                            try {
                                val file = File(File(plugin.dataFolder, "Items").apply { mkdir() }, "${args[2]}.yml").apply {
                                    if (!exists()) createNewFile()
                                }
                                YamlConfiguration().run {
                                    load(file)
                                    set(args[3],item)
                                    save(file)
                                }
                                CutsceneMaker.send(sender, "successfully saved.")
                            } catch (ex: Exception) {
                                CutsceneMaker.send(sender, "unable to save the item")
                            }
                        }
                    }
                }
                tabComplete = { _,args ->
                    if (args.size == 3) File(plugin.dataFolder, "Items").listFiles()?.run {
                        toList().map { it.nameWithoutExtension }.filter { it.startsWith(args[2]) }
                    } ?: emptyList() else null
                }
            }
            .done()
        val animation = CommandAPI("cutscene animation")
            .create("save").apply {
                aliases = arrayOf("s","저장")
                description = "save the block animation."
                usage = "save <key>"
                length = 2
                allowedSender = arrayOf(SenderType.PLAYER)
                executor = { sender, args ->
                    ADMIN_DATA_MAP[sender]?.run {
                        if (pos1 != null && pos2 != null) {
                            val world = pos1!!.world

                            val animation = BlockAnimation.get(world, pos1!!.toVector(), pos2!!.toVector())
                            plugin.manager.animationMap[args[2]] = animation
                            animation.write(File(File(File(plugin.dataFolder, "Animation"), world.name), "${args[2]}.anim"))
                            CM.send(sender, "successfully saved.")
                        } else {
                            CM.send(sender, "you must select a region first.")
                        }
                    }
                }
            }
            .done()
            .create("load").apply {
                aliases = arrayOf("로드")
                description = "load the block."
                usage = "load <key>"
                length = 2
                allowedSender = arrayOf(SenderType.PLAYER)
                executor = { sender, args ->
                    plugin.manager.animationMap[args[2]]?.run {
                        set()
                        CM.send(sender,"successfully loaded.")
                    } ?: CM.send(sender,"The block animation \"" + args[2] + "\" doesn't exist!")
                }
            }
            .done()
            .create("air").apply {
                aliases = arrayOf("a","청소")
                description = "clean the block."
                usage = "air <key>"
                length = 2
                allowedSender = arrayOf(SenderType.PLAYER)
                executor = { sender, args ->
                    plugin.manager.animationMap[args[2]]?.run {
                        setToAir()
                        CM.send(sender,"successfully loaded.")
                    } ?: CM.send(sender,"The block animation \"" + args[2] + "\" doesn't exist!")
                }
            }
            .done()
        CommandAPI("cutscene")
            .create("reload").apply {
                aliases = arrayOf("re","rl","리로드")
                description = "reload this plugin."
                usage = "reload"
                executor = { sender,_ ->
                    plugin.load {
                        CutsceneMaker.send(sender, "load finished ($it ms)")
                    }
                }
            }
            .done()
            .create("run").apply {
                aliases = arrayOf("r","실행")
                description = "run the Action."
                length = 1
                usage = "run <action>"
                allowedSender = arrayOf(SenderType.LIVING_ENTITY)
                executor = { sender,args ->
                    if (ActionData.start(args[1], sender as LivingEntity)) CutsceneMaker.send(
                        sender,
                        "run success!"
                    )
                    else CutsceneMaker.send(sender, "run failed!")
                }
                tabComplete = { _,args ->
                    if (args.size == 2) ActionData.getActionKeys().filter { it.startsWith(args[1]) } else null
                }
            }
            .done()
            .create("item").apply {
                aliases = arrayOf("i","아이템")
                description = "show the CutsceneMaker's item command."
                usage = "item"
                executor = { commandSender, strings ->
                    item.execute(if (strings.size > 1) strings[1] else "list",commandSender,strings)
                }
                tabComplete = { sender, args ->
                    if (args.size == 2) item.searchCommand(args[1],sender) else item.tabComplete(args[1],sender,args)
                }
            }
            .done()
            .create("animation").apply {
                aliases = arrayOf("a","anim","애니메이션")
                description = "show the CutsceneMaker's block animation command."
                usage = "animation"
                executor = { commandSender, strings ->
                    animation.execute(if (strings.size > 1) strings[1] else "list",commandSender,strings)
                }
                tabComplete = { sender, args ->
                    if (args.size == 2) animation.searchCommand(args[1],sender) else animation.tabComplete(args[1],sender,args)
                }
            }
            .done()
            .create("temp").apply {
                aliases = arrayOf("storage","s","보관함")
                description = "show the CutsceneMaker's temp storage command."
                usage = "temp"
                executor = { commandSender, strings ->
                    temp.execute(if (strings.size > 1) strings[1] else "list",commandSender,strings)
                }
                tabComplete = { sender, args ->
                    if (args.size == 2) temp.searchCommand(args[1],sender) else temp.tabComplete(args[1],sender,args)
                }
            }
            .done()
            .create("location").apply {
                aliases = arrayOf("loc","좌표")
                description = "save your location to file data."
                usage = "location <file> <key>"
                length = 2
                allowedSender = arrayOf(SenderType.PLAYER)
                executor = { sender, args ->
                    try {
                        val writer = ConfigWriter(
                            File(
                                File(plugin.dataFolder, "Locations").apply {
                                    mkdir()
                                },
                                args[1] + ".yml"
                            )
                        )
                        writer.setLocation(args[2], (sender as Player).location)
                        writer.save()
                        CM.send(sender, "successfully saved.")
                    } catch (e: Exception) {
                        CM.send(sender, "cannot save location.")
                    }
                }
                tabComplete = { _, args ->
                    if (args.size == 2) File(plugin.dataFolder, "Locations").listFiles()?.run {
                        toList().map { it.nameWithoutExtension }.filter { it.startsWith(args[1]) }
                    } ?: emptyList() else null
                }
            }
            .done()
            .create("teleport").apply {
                aliases = arrayOf("tp","텔포")
                description = "teleport to a registered location."
                usage = "teleport <location>"
                length = 1
                allowedSender = arrayOf(SenderType.LIVING_ENTITY)
                executor = { sender, args ->
                    plugin.manager.locations.getValue(args[1])?.run {
                        (sender as LivingEntity).teleport(this,PlayerTeleportEvent.TeleportCause.PLUGIN)
                    } ?: CM.send(sender,"location not found: ${args[1]}")
                }
                tabComplete = { _, args ->
                    if (args.size == 2) plugin.manager.locations.keys.filter { it.startsWith(args[1]) } else null
                }
            }
            .done()
            .create("variable").apply {
                aliases = arrayOf("var","v","변수")
                description = "access to specific player's variables."
                usage = "variable <player> <name> <value>"
                length = 3
                executor = { sender, args ->
                    val player = Bukkit.getPlayer(args[1])
                    if (player == null) {
                        CM.send(sender, "unknown player's name.")
                    } else {
                        val container = plugin.manager.getVars(player)
                        if (container != null) {
                            val vars = container.get(args[2])
                            if (args[3] == "null") {
                                CM.send(sender, "successfully changed. (${vars.getVar()} to <none>)")
                                container.remove(args[2])
                            } else {
                                CM.send(sender, "successfully changed. (${vars.getVar()} to ${args[3]})")
                                vars.setVar(args[3])
                            }
                        }
                    }
                }
                tabComplete = { _, args ->
                    if (args.size == 3) Bukkit.getPlayer(args[1])?.run {
                        plugin.manager.getVars(this).vars.keys.toList().filter { it.startsWith(args[2]) }
                    } else null
                }
            }
            .done()
            .create("material").apply {
                aliases = arrayOf("m","타입")
                description = "get the material of item in sender's main hand."
                usage = "material"
                allowedSender = arrayOf(SenderType.PLAYER)
                executor = { sender, _ ->
                    val get = (sender as Player).inventory.itemInMainHand
                    if (get == null || get.type == null) {
                        CM.send(sender, "hold the item you want to get the material from.")
                    } else {
                        CM.send(sender, "this item's material is: ${get.type}")
                    }
                }
            }
            .done()
            .create("project").apply {
                aliases = arrayOf("p","프로젝트")
                description = "open gui editor."
                usage = "project <editable> <key>"
                length = 2
                allowedSender = arrayOf(SenderType.PLAYER)
                executor = { sender, args ->
                    if (!EditorSupplier.openEditor(
                            (sender as Player),
                            args[1],
                            args[2]
                        )
                    ) {
                        CM.send(sender, "fail to open editor.")
                    }
                }
                tabComplete = { _, args ->
                    when(args.size) {
                        2 -> EditorSupplier.getEditorList().filter { it.startsWith(args[1]) }
                        3 -> EditorSupplier.getEditableObjects(args[1])?.filter { it.startsWith(args[2]) }
                        else -> null
                    }
                }
            }
            .done()
            .create("create").apply {
                aliases = arrayOf("c","생성")
                description = "create new editor."
                usage = "create <editable> <file> <key>"
                length = 3
                allowedSender = arrayOf(SenderType.PLAYER)
                executor = { sender, args ->
                    if (!EditorSupplier.createEditor(
                            (sender as Player), plugin.manager,
                            args[1], args[2], args[3]
                        )
                    ) {
                        CM.send(sender, "fail to create editor.")
                    }
                }
                tabComplete = { _, args ->
                    when(args.size) {
                        2 -> EditorSupplier.getEditorList().filter { it.startsWith(args[1]) }
                        3 -> File(plugin.dataFolder, args[1].lowercase().replaceFirstChar { it.uppercase() }).listFiles()?.run {
                            toList().map { it.nameWithoutExtension }.filter { it.startsWith(args[2]) }
                        } ?: emptyList()
                        else -> null
                    }
                }
            }
            .done()
            .create("log").apply {
                aliases = arrayOf("로그")
                description = "show the variable list of specific player"
                usage = "log"
                length = 1
                executor = { sender, args ->
                    fun showVars(sender: CommandSender, varsMap: Map<String, Vars>) {
                        val vars: MutableList<String> = ArrayList()
                        val quests: MutableList<String> = ArrayList()
                        varsMap.forEach{ (s: String, v: Vars) ->
                            if (s.startsWith("quest.")) quests.add(
                                ChatColor.GOLD.toString() + s.substring(6)
                            ) else vars.add((ChatColor.YELLOW.toString() + s + ChatColor.GRAY) + ": " + ChatColor.GREEN + v.getVar())
                        }
                        CM.send(
                            sender,
                            ((ChatColor.GRAY.toString() + "-----< " + ChatColor.BLUE) + "Variables" + ChatColor.GRAY) + " >-----"
                        )
                        vars.forEach{ s: String? -> CM.send(sender, s) }
                        CM.send(
                            sender,
                            ((ChatColor.GRAY.toString() + "-----< " + ChatColor.BLUE) + "Quests" + ChatColor.GRAY) + " >-----"
                        )
                        quests.forEach{ s: String? -> CM.send(sender, s) }
                    }
                    val name: String = args[1]
                    val player = Bukkit.getPlayer(name)
                    if (player == null) {
                        CM.send(sender, "This player is not online!")
                        val player1 = Arrays.stream(Bukkit.getOfflinePlayers()).filter { o ->
                            o.name.equals(name)
                        }.findFirst().orElse(null)
                        if (player1 != null) {
                            plugin.manager.runTaskAsynchronously {
                                CM.send(
                                    sender,
                                    "This offline-player " + player1.name + "'s variable list. " + ChatColor.GRAY + "(" + player1.uniqueId + ")"
                                )
                                showVars(sender, CutsceneDB.read(player1, plugin).vars)
                            }
                        } else CM.send(sender, "Error: player not found")
                    } else {
                        CM.send(sender, "Player " + player.name + "'s variable list.")
                        val container: VarsContainer? = plugin.manager.getVars(player)
                        if (container == null) {
                            CM.send(sender, "Error: can't get " + player.name + "'s variable list.")
                        } else showVars(sender, container.vars)
                    }
                }
            }.done()
            .create("wand").apply {
                aliases = arrayOf("w","완드")
                description = "get the wand."
                usage = "wand"
                allowedSender = arrayOf(SenderType.PLAYER)
                executor = { sender, _ ->
                    InvUtil.give(sender as Player, WAND)
                    CM.send(sender, "successfully given.")
                }
            }
            .done()

    }

    @EventHandler
    fun onJoin(e: PlayerJoinEvent) {
        if (e.player.isOp) ADMIN_DATA_MAP[e.player] = AdminData()
    }

    @EventHandler
    fun onClick(e: PlayerInteractEvent) {
        if (e.hasBlock() && e.hasItem() && WAND.isSimilar(e.item)) {
            e.isCancelled = true
            val data = ADMIN_DATA_MAP[e.player] ?: return
            val loc = e.clickedBlock.location
            when (e.action) {
                Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK -> {
                    data.pos1 = loc
                    CutsceneMaker.send(
                        e.player,
                        "pos 1: " + ChatColor.GRAY + "(" + loc.blockX + "," + loc.blockY + "," + loc.blockZ + ")"
                    )
                }
                Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK -> {
                    data.pos2 = loc
                    CutsceneMaker.send(
                        e.player,
                        "pos 2: " + ChatColor.GRAY + "(" + loc.blockX + "," + loc.blockY + "," + loc.blockZ + ")"
                    )
                }
                else -> {}
            }
        }
    }
    override fun onTabComplete(
        sender: CommandSender,
        command: Command?,
        alias: String?,
        args: Array<String>
    ): List<String>? {
        return if (args.size == 1) commandAPI.searchCommand(args[0],sender) else commandAPI.tabComplete(args[0],sender,args)
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command?,
        label: String?,
        args: Array<String>
    ): Boolean {
        (if (args.isEmpty()) arrayOf("list") else args).run {
            commandAPI.execute(get(0),sender,this)
        }
        return true
    }


    private class AdminData {
        var pos1: Location? = null
        var pos2: Location? = null
    }
}
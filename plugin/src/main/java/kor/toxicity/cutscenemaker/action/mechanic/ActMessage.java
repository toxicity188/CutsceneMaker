package kor.toxicity.cutscenemaker.action.mechanic;

import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.action.RepeatableAction;
import kor.toxicity.cutscenemaker.util.reflect.DataField;
import kor.toxicity.cutscenemaker.util.TextUtil;
import kor.toxicity.cutscenemaker.util.functions.FunctionPrinter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class ActMessage extends RepeatableAction {

    @DataField(aliases = "m",throwable = true)
    public String message;
    @DataField(aliases = "st")
    public String subtitle = "";
    @DataField(aliases = {"r","rand"})
    public boolean random = false;
    @DataField
    public String type = "message";

    @DataField(aliases = "b")
    public int blank = 1;

    @DataField(aliases = "fi")
    public int fadein = 10;
    @DataField(aliases = "s")
    public int stay = 20;
    @DataField(aliases = "fo")
    public int fadeout = 10;


    private FunctionPrinter[] m;
    private FunctionPrinter[] st;
    private BiConsumer<LivingEntity,Integer> act;
    private final Map<LivingEntity,Integer> id;

    public ActMessage(CutsceneManager pl) {
        super(pl);
        id = new HashMap<>();
    }

    @Override
    public void initialize() {
        super.initialize();
        m = a(message);
        st = a(subtitle);

        switch (type) {
            default:
            case "message":
                if (blank > 0) {
                    Consumer<LivingEntity> c = e -> IntStream.range(0, blank).forEach(i -> e.sendMessage(""));
                    act = (e, i) -> {
                        c.accept(e);
                        e.sendMessage(m[Math.min(m.length-1,i)].print(e));
                        c.accept(e);
                    };
                } else act = (e,i) -> e.sendMessage(m[Math.min(m.length-1,i)].print(e));
                break;
            case "title":
                act = (e,i) -> {
                    if (e instanceof Player) ((Player) e).sendTitle(m[Math.min(m.length-1,i)].print(e), st[Math.min(st.length-1,i)].print(e), fadein, stay, fadeout);
                };
                break;
        }
    }
    private FunctionPrinter[] a(String s) {
        if (s == null) return null;
        return Arrays.stream(TextUtil.split(s, "//")).map(FunctionPrinter::new).toArray(FunctionPrinter[]::new);
    }

    @Override
    protected void initialize(LivingEntity entity) {
        id.put(entity,0);
    }

    @Override
    protected void update(LivingEntity entity) {
        int t = id.get(entity);
        act.accept(entity,(random) ? ThreadLocalRandom.current().nextInt(0,t) : t);
        id.put(entity,t + 1);
    }

    @Override
    protected void end(LivingEntity end) {
        id.remove(end);
    }

}

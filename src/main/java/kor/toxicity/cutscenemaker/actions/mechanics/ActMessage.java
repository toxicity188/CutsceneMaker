package kor.toxicity.cutscenemaker.actions.mechanics;

import kor.toxicity.cutscenemaker.CutsceneManager;
import kor.toxicity.cutscenemaker.actions.RepeatableAction;
import kor.toxicity.cutscenemaker.util.DataField;
import kor.toxicity.cutscenemaker.util.functions.MethodInterpreter;
import kor.toxicity.cutscenemaker.util.TextParser;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
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


    private List<MethodInterpreter> m;
    private List<MethodInterpreter> st;
    private BiConsumer<LivingEntity,Integer> act;
    private final Map<LivingEntity,Integer> id;

    public ActMessage(CutsceneManager pl) {
        super(pl);
        id = new HashMap<>();
    }

    @Override
    public void initialize() {
        super.initialize();
        a(message,s -> m = s);
        a(subtitle,s -> st = s);

        switch (type) {
            default:
            case "message":
                if (blank > 0) {
                    Consumer<LivingEntity> c = e -> IntStream.range(0, blank).forEach(i -> e.sendMessage(""));
                    act = (e, i) -> {
                        c.accept(e);
                        e.sendMessage(m.get(Math.min(m.size()-1,i)).print(e));
                        c.accept(e);
                    };
                } else act = (e,i) -> e.sendMessage(m.get(Math.min(m.size()-1,i)).print(e));
                break;
            case "title":
                act = (e,i) -> {
                    if (e instanceof Player) ((Player) e).sendTitle(m.get(Math.min(m.size()-1,i)).print(e), st.get(Math.min(st.size()-1,i)).print(e), fadein, stay, fadeout);
                };
                break;
        }
    }
    private void a(String s, Consumer<List<MethodInterpreter>> c) {
        if (s == null) return;
        c.accept(Arrays.stream(TextParser.getInstance().split(s.replaceAll("&","§"),"/")).map(q -> new MethodInterpreter(b(q))).collect(Collectors.toList()));
    }
    private String b(String s) {
        return TextParser.getInstance().colored(s);
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

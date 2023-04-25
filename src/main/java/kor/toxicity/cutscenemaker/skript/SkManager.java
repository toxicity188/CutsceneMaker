package kor.toxicity.cutscenemaker.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.Getter;
import ch.njol.yggdrasil.Fields;
import kor.toxicity.cutscenemaker.CutsceneMaker;
import kor.toxicity.cutscenemaker.event.QuestCompleteEvent;
import kor.toxicity.cutscenemaker.quests.QuestSet;
import kor.toxicity.cutscenemaker.skript.effects.EffCompleteQuest;
import kor.toxicity.cutscenemaker.skript.effects.EffGiveQuest;
import kor.toxicity.cutscenemaker.skript.effects.EffRunAction;
import kor.toxicity.cutscenemaker.skript.effects.EffRunDialog;
import kor.toxicity.cutscenemaker.skript.events.EvtQuestComplete;
import kor.toxicity.cutscenemaker.skript.expressions.*;
import org.jetbrains.annotations.NotNull;

public class SkManager {
    private SkManager() {}
    public static void registerAddon() {
        try {
            Classes.registerClass(new ClassInfo<>(QuestSet.class,"questset")
                    .user("questset?")
                    .name("QuestSet")
                    .description("Represents a class of QuestSet in CutsceneMaker")
                    .examples("a name of quest \"name\"")
                    .defaultExpression(new EventValueExpression<>(QuestSet.class))
                    .parser(new Parser<QuestSet>() {
                        @Override
                        public QuestSet parse(@NotNull String s,@NotNull ParseContext context) {
                            return null;
                        }

                        @Override
                        public boolean canParse(@NotNull ParseContext context) {
                            return false;
                        }

                        @NotNull
                        @Override
                        public String toString(QuestSet o, int flags) {
                            return toVariableNameString(o);
                        }

                        @NotNull
                        @Override
                        public String toVariableNameString(QuestSet o) {
                            return "questset:" + o.toString();
                        }

                        @NotNull
                        @Override
                        public String getVariableNamePattern() {
                            return "questset:name";
                        }
                    }).serializer(new Serializer<QuestSet>() {
                        @Override
                        @NotNull
                        public Fields serialize(QuestSet o) {
                            Fields fields = new Fields();
                            fields.putPrimitive("name",o.getName());
                            fields.putPrimitive("type",o.getType());
                            fields.putPrimitive("priority",o.getPriority());
                            fields.putPrimitive("exp",o.getExp());
                            fields.putPrimitive("money",o.getMoney());
                            return fields;
                        }

                        @Override
                        protected QuestSet deserialize(@NotNull Fields fields) {
                            return null;
                        }

                        @Override
                        public void deserialize(QuestSet o, @NotNull Fields f) {
                            throw new UnsupportedOperationException();
                        }

                        @Override
                        public boolean mustSyncDeserialization() {
                            return false;
                        }

                        @Override
                        protected boolean canBeInstantiated() {
                            return false;
                        }
                    })
            );
            Skript.registerExpression(
                    ExprCutsceneVariable.class,
                    String.class,
                    ExpressionType.COMBINED,
                    "[cutscenemaker] [the] cutscene (variable|var) %string% of %player%"
            );
            Skript.registerExpression(
                    ExprCutsceneVariableArray.class,
                    String.class,
                    ExpressionType.COMBINED,
                    "[cutscenemaker] [the] cutscene (variable|var) array %string% of %player%"
            );
            Skript.registerExpression(
                    ExprQuestSetMoney.class,
                    Double.class,
                    ExpressionType.COMBINED,
                    "[cutscenemaker] [the] [questset] money of %questset%"
            );
            Skript.registerExpression(
                    ExprQuestSetExp.class,
                    Double.class,
                    ExpressionType.COMBINED,
                    "[cutscenemaker] [the] [questset] exp of %questset%"
            );
            Skript.registerExpression(
                    ExprQuestSet.class,
                    QuestSet.class,
                    ExpressionType.COMBINED,
                    "[cutscenemaker] [the] questset named %string%"
            );
            Skript.registerEffect(
                    EffRunAction.class,
                    "[cutscenemaker] (run|invoke|call) [the] [cutscene] action named %string% to %entity%"
            );
            Skript.registerEffect(
                    EffRunDialog.class,
                    "[cutscenemaker] (run|invoke|call) [the] [cutscene] dialog named %string% to %player% as talker %string% [with typing sound %-string%]"
            );
            Skript.registerEffect(
                    EffGiveQuest.class,
                    "[cutscenemaker] give [the] [cutscene] %questset% to %player%"
            );
            Skript.registerEffect(
                    EffCompleteQuest.class,
                    "[cutscenemaker] complete [the] [cutscene] %questset% to %player%"
            );
            Skript.registerEvent(
                    "Quest Complete",
                    EvtQuestComplete.class,
                    QuestCompleteEvent.class,
                    "[cutscenemaker] complete [cutscene] quest [of %-string%]"
            );
            EventValues.registerEventValue(
                    QuestCompleteEvent.class,
                    QuestSet.class,
                    new Getter<QuestSet, QuestCompleteEvent>() {
                        @Override
                        public QuestSet get(QuestCompleteEvent arg) {
                            return arg.getQuestSet();
                        }
                    },
                    0
            );
        } catch (Throwable e) {
            CutsceneMaker.warn("Unable to find Skirpt.");
        }
    }
}

package kor.toxicity.cutscenemaker.quests;

public interface DialogAddon {
    boolean isGui();
    void run(Dialog.DialogCurrent current);

    default boolean run(Dialog dialog, Dialog.DialogCurrent current) {
        return dialog.run(current);
    }
}

package kor.toxicity.cutscenemaker.quests.data;

import kor.toxicity.cutscenemaker.quests.QuestSet;
import lombok.Data;

import java.util.List;

@Data
public class QuestCurrent {
    private int page = 1, typeIndex = 0;
    private String type;
    private List<QuestSet> sorted;
    private int totalPage;
    private final List<String> typeList;
}

package kor.toxicity.cutscenemaker.quests.enums;

import kor.toxicity.cutscenemaker.quests.data.QuestCurrent;
import kor.toxicity.cutscenemaker.util.ItemBuilder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum QuestButton {
    PAGE_BEFORE(19) {
        @Override
        public List<String> getLore(QuestCurrent current) {
            synchronized (builder) {
                builder.setLength(0);
                return Collections.singletonList(
                        builder.append(ChatColor.GOLD).append(ChatColor.ITALIC).append("Page: ")
                                .append(ChatColor.YELLOW).append(current.getPage())
                                .append(ChatColor.GRAY).append("/")
                                .append(ChatColor.YELLOW).append(current.getTotalPage())
                                .toString()
                );
            }
        }
    },
    PAGE_AFTER(25) {
        @Override
        public List<String> getLore(QuestCurrent current) {
            synchronized (builder) {
                builder.setLength(0);
                return Collections.singletonList(
                        builder.append(ChatColor.GOLD).append(ChatColor.ITALIC).append("Page: ")
                                .append(ChatColor.YELLOW).append(current.getPage())
                                .append(ChatColor.GRAY).append("/")
                                .append(ChatColor.YELLOW).append(current.getTotalPage())
                                .toString()
                );
            }
        }
    },
    TYPE_SORT(22) {
        @Override
        public List<String> getLore(QuestCurrent current) {
            synchronized (builder) {
                List<String> list = current.getTypeList();
                if (list.isEmpty()) {
                    return Collections.singletonList(ChatColor.GRAY + "---------------");
                } else {
                    int i = 0;
                    int size = list.size();
                    String type = current.getType();
                    builder.setLength(0);
                    builder.append(ChatColor.GRAY);
                    for (String s : list) {
                        if (s.equals(type)) builder.append(ChatColor.WHITE).append(ChatColor.BOLD).append(s);
                        else builder.append(s);
                        if (++i < size) builder.append(ChatColor.GRAY).append(" / ");
                    }
                    return Collections.singletonList(builder.toString());
                }
            }
        }
    },
    ;
    final int defaultSlot;
    final StringBuilder builder = new StringBuilder();
    public static final ItemBuilder DEFAULT_ITEM_BUILDER;
    static {
        ItemStack item = new ItemStack(Material.APPLE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "This is the default item!");
        item.setItemMeta(meta);
        DEFAULT_ITEM_BUILDER = new ItemBuilder(item);
    }

    public abstract List<String> getLore(QuestCurrent current);
}

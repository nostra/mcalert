package io.github.nostra.mcalert.model;

public class Item implements Comparable<Item> {
    private String name;
    private boolean selected;
    private long seenSecondsAgo;

    public Item(String name, boolean selected, long seenSecondsAgo) {
        this.name = name;
        this.selected = selected;
        this.seenSecondsAgo = seenSecondsAgo;
    }

    public String getName() {
        return name;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public int compareTo(Item other) {
        return name.compareTo(other.name);
    }

    public long getSeenSecondsAgo() {
        return seenSecondsAgo;
    }

    public void setSeenSecondsAgo(long seenSecondsAgo) {
        this.seenSecondsAgo = seenSecondsAgo;
    }
}

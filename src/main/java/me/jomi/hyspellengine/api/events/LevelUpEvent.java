package me.jomi.hyspellengine.api.events;

import com.hypixel.hytale.component.system.EcsEvent;
import me.jomi.hyspellengine.api.Experience;

public class LevelUpEvent extends EcsEvent {
    private final Experience experience;
    private final int previousLevel;
    private final int newLevel;

    public LevelUpEvent(Experience experience, int previousLevel, int newLevel) {
        this.experience = experience;
        this.previousLevel = previousLevel;
        this.newLevel = newLevel;
    }

    public Experience getExperience() {
        return experience;
    }

    public int getPreviousLevel() {
        return previousLevel;
    }

    public int getNewLevel() {
        return newLevel;
    }
}

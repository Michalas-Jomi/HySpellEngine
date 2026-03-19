package me.jomi.hyspellengine.api.events;

import me.jomi.hyspellengine.core.SpellContext;

/**
 * Called only once, when spell is learned and not knowing before
 */
public class SpellLearnEvent extends SpellApplyEvent {
    public SpellLearnEvent(SpellContext spell) {
        super(spell);
    }
}

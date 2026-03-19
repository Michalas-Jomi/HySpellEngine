package me.jomi.hyspellengine.api.events;

import com.hypixel.hytale.component.system.CancellableEcsEvent;
import me.jomi.hyspellengine.core.SpellContext;

public class SpellApplyEvent extends CancellableEcsEvent {
    private final SpellContext spell;

    public SpellApplyEvent(SpellContext spell) {
        this.spell = spell;
    }

    public SpellContext getSpell() {
        return spell;
    }
}

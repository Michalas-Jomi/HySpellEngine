package me.jomi.hyspellengine.core;

import me.jomi.hyspellengine.api.Spell;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpellRegistry {
    private final Map<String, Spell> registry = new ConcurrentHashMap<>();

    public void registerSpell(Spell spell) {
        this.registry.put(spell.getName(), spell);
    }

    public void unregisterSpell(String name) {
        this.registry.remove(name);
    }
    public void unregisterSpell(Spell spell) {
        this.unregisterSpell(spell.getName());
    }

    public Spell getSpell(String name) {
        return this.registry.get(name);
    }
}

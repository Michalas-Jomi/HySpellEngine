package me.jomi.hyspellengine.core;

import me.jomi.hyspellengine.api.Spell;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

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

    public Set<String> getKeys() {
        return this.registry.keySet();
    }

    public void forEach(BiConsumer<String, Spell> work) {
        this.registry.forEach(work);
    }
}

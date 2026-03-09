package me.jomi.hyspellengine.core;

import me.jomi.hyspellengine.api.Experience;
import me.jomi.hyspellengine.api.Spell;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class ExperienceRegistry {
    private final Map<String, Experience> registry = new ConcurrentHashMap<>();

    public void registerExperience(Experience exp) {
        this.registry.put(exp.getName(), exp);
    }

    public void unregisterExperience(String name) {
        this.registry.remove(name);
    }
    public void unregisterExperience(Experience exp) {
        this.unregisterExperience(exp.getName());
    }

    public Experience getExperience(String name) {
        return this.registry.get(name);
    }

    public Set<String> getKeys() {
        return this.registry.keySet();
    }

    public void forEach(BiConsumer<String, Experience> work) {
        this.registry.forEach(work);
    }
}

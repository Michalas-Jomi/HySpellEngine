package me.jomi.hyspellengine.core;

import me.jomi.hyspellengine.api.Experience;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExperienceRegistry {
    private final Map<String, Experience> registry = new ConcurrentHashMap<>();

    public void registerExperience(Experience exp) {
        this.registry.put(exp.name(), exp);
    }

    public void unregisterExperience(String name) {
        this.registry.remove(name);
    }
    public void unregisterExperience(Experience exp) {
        this.unregisterExperience(exp.name());
    }

    public Experience getExperience(String name) {
        return this.registry.get(name);
    }
}

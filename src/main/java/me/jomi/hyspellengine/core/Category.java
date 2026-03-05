package me.jomi.hyspellengine.core;

import me.jomi.hyspellengine.api.Experience;

import java.nio.file.Path;

public record Category(Display display, Experience experience, SpellContext root) {
    public Category(Display display, Experience experience, SpellContext root) {
        this.display = display;
        this.experience = experience;
        this.root = root;

        this.root.setCategory(this);
    }

    public static record Display(String name, String description, Path icon) {
    }
}

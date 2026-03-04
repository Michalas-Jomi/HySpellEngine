package me.jomi.hyspellengine.core;

import me.jomi.hyspellengine.api.Experience;

import java.nio.file.Path;

public record Category(Display display, Experience experience, SpellContext root) {
    public static record Display(String name, String description, Path icon) {
    }
}

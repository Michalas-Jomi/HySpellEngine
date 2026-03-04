package me.jomi.hyspellengine.core;

import me.jomi.hyspellengine.api.Experience;
import me.jomi.hyspellengine.api.Spell;

public record Category(String name, Experience experience, SpellContext root) {
}

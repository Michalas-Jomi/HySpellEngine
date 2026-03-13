package me.jomi.hyspellengine.core;

import me.jomi.hyspellengine.api.Experience;

import java.nio.file.Path;
import java.util.*;

public record Category(Display display, Experience experience, SpellContext root, UUID uuid) implements Iterable<SpellContext> {
    public static record Display(String name, String description, Path icon) {
    }

    public Category(Display display, Experience experience, SpellContext root, UUID uuid) {
        this.display = display;
        this.experience = experience;
        this.root = root;
        this.uuid = uuid;

        this.root.setCategory(this);
    }

    public SpellContext getSpell(UUID uuid) {
        for (SpellContext spell : this)
            if (spell.getUuid().equals(uuid))
                return spell;
        return null;
    }

    @Override
    public Iterator<SpellContext> iterator() {
        return new Iterator<>() {
            private final Deque<SpellContext> stack = new ArrayDeque<>();

            {
                SpellContext root = root();
                if (root != null) {
                    stack.push(root);
                }
            }

            @Override
            public boolean hasNext() {
                return !stack.isEmpty();
            }

            @Override
            public SpellContext next() {
                if (stack.isEmpty())
                    throw new NoSuchElementException();

                SpellContext current = stack.pop();

                SpellContext[] children = current.getChildren();
                for (int i = children.length - 1; i >= 0; i--) {
                    stack.push(children[i]);
                }

                return current;
            }
        };
    }
}

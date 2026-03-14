package me.jomi.hyspellengine.api;

import me.jomi.hyspellengine.Data;
import me.jomi.hyspellengine.core.Category;
import me.jomi.hyspellengine.core.SpellContext;
import me.jomi.hyspellengine.utils.Adapter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ExperienceTest {
    Experience makeExp(double... xps) {
        Experience experience = new Experience("test" + UUID.randomUUID(), "test", key -> true);
        Experience.Level[] levels = new Experience.Level[xps.length];
        double exp = 0;
        for (int i = 0; i < xps.length; i++) {
            exp += xps[i];
            levels[i] = new Experience.Level(exp, false, "", "");
        }
        Data.set(experience, levels);

        return experience;
    }
    Experience makeExpInfinity(double infinityExp, double... xps) {
        Experience experience = makeExp(xps);
        Experience.Level[] levels = Arrays.copyOf(experience.getLevels(), experience.getLevels().length + 1);

        levels[levels.length - 1] = new Experience.Level(infinityExp, true, "", "");

        Data.set(experience, levels);

        return experience;
    }

    @Test
    void getExpForLevel() {
        Experience exp10x5 = makeExp(10, 10, 20, 10, 10);

        assert exp10x5.getExpForLevel(-1) == 0;
        assert exp10x5.getExpForLevel(0) == 0;
        assert exp10x5.getExpForLevel(1) == 10;
        assert exp10x5.getExpForLevel(2) == 20;
        assert exp10x5.getExpForLevel(3) == 40;
        assert exp10x5.getExpForLevel(4) == 50;
        assert exp10x5.getExpForLevel(5) == 60;

        assertThrows(IllegalArgumentException.class, () -> exp10x5.getExpForLevel(6));


        Experience exp10x3Inf = makeExpInfinity(5, 10, 10, 20);

        assert exp10x3Inf.getExpForLevel(0) == 0;
        assert exp10x3Inf.getExpForLevel(1) == 10;
        assert exp10x3Inf.getExpForLevel(2) == 20;
        assert exp10x3Inf.getExpForLevel(3) == 40;

        assert exp10x3Inf.getExpForLevel(4) == 45;
        assert exp10x3Inf.getExpForLevel(5) == 50;
        assert exp10x3Inf.getExpForLevel(6) == 55;
    }

    @Test
    void getLevel() {
        Experience exp10x5 = makeExp(10, 10, 20, 10, 10);

        assert exp10x5.getLevel(-1) == 0;
        assert exp10x5.getLevel(0) == 0;
        assert exp10x5.getLevel(3) == 0;
        assert exp10x5.getLevel(10) == 1;
        assert exp10x5.getLevel(20) == 2;
        assert exp10x5.getLevel(30) == 2;
        assert exp10x5.getLevel(35) == 2;
        assert exp10x5.getLevel(40) == 3;
        assert exp10x5.getLevel(50) == 4;


        Experience exp10x3Inf = makeExpInfinity(5, 10, 10, 10);

        assert exp10x3Inf.getLevel(0) == 0;
        assert exp10x3Inf.getLevel(5) == 0;
        assert exp10x3Inf.getLevel(10) == 1;
        assert exp10x3Inf.getLevel(12) == 1;
        assert exp10x3Inf.getLevel(20) == 2;
        assert exp10x3Inf.getLevel(30) == 3;
        assert exp10x3Inf.getLevel(32) == 3;

        assert exp10x3Inf.getLevel(35) == 4;
        assert exp10x3Inf.getLevel(37) == 4;
        assert exp10x3Inf.getLevel(40) == 5;
        assert exp10x3Inf.getLevel(43) == 5;
        assert exp10x3Inf.getLevel(45) == 6;
    }
}
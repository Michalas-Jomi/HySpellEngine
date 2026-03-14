package me.jomi.hyspellengine.api;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import me.jomi.hyspellengine.Data;
import me.jomi.hyspellengine.HySpellEnginePlugin;
import me.jomi.hyspellengine.api.events.ExperienceChangeEvent;
import me.jomi.hyspellengine.api.events.LevelUpEvent;
import me.jomi.hyspellengine.core.Category;
import me.jomi.hyspellengine.core.ExperienceRegistry;
import me.jomi.hyspellengine.utils.EasyCodec;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * can be Overridden
 */
public class Experience {
    /**
     *
     * @param exp experience needed to reach level
     * @param infinite if true, exp will be reused for calculating, can be true only on last element
     * @param chatMessage chat message to send player on level up
     * @param sound sound to play for player on level up
     */
    public static record Level(double exp, boolean infinite, String chatMessage, String sound) {
    }
    public static class ExperienceComponent implements Component<EntityStore> {
        public static final BuilderCodec<ExperienceComponent> CODEC = EasyCodec.create(ExperienceComponent.class);

        public static ComponentType<EntityStore, ExperienceComponent> getComponentType() {
            return HySpellEnginePlugin.getInstance().getComponentType(ExperienceComponent.class);
        }


        // experience.name : exp
        @EasyCodec.ForCodec public Map<String, Double> experiences = new HashMap<>();
        // category.uuid : [points from plugins, spend points]
        @EasyCodec.ForCodec public Map<String, int[]> points = new HashMap<>();

        public double getExp(Experience experience) {
            return this.experiences.containsKey(experience.getName()) ? this.experiences.get(experience.getName()) : 0;
        }
        public void addExp(Experience experience, double exp) {
            exp += this.getExp(experience);
            this.setExp(experience, exp);
        }
        public void setExp(Experience experience, double exp) {
            if (exp <= 0)
                this.experiences.remove(experience.getName());
            else
                this.experiences.put(experience.getName(), exp);
        }

        private int[] ensureAndGetPoints(Category category) {
            if (this.points.containsKey(category.uuid().toString()))
                return this.points.get(category.uuid().toString());
            int[] array = new int[]{0, 0};
            this.points.put(category.uuid().toString(), array);
            return array;
        }

        public int getPluginPoints(Category category) {
            int[] array = this.ensureAndGetPoints(category);
            return array[0];
        }
        public void addPluginPoints(Category category, int points) {
            int[] array = this.ensureAndGetPoints(category);
            array[0] += points;
        }
        public void setPluginPoints(Category category, int points) {
            int[] array = this.ensureAndGetPoints(category);
            array[0] = points;
        }

        public int getSpendPoints(Category category) {
            int[] array = this.ensureAndGetPoints(category);
            return array[1];
        }
        public void addSpendPoints(Category category, int points) {
            int[] array = this.ensureAndGetPoints(category);
            array[1] += points;
        }
        public void setSpendPoints(Category category, int points) {
            int[] array = this.ensureAndGetPoints(category);
            array[1] = points;
        }

        @NullableDecl
        @Override
        public Component<EntityStore> clone() {
            ExperienceComponent copy = new ExperienceComponent();
            copy.experiences.putAll(this.experiences);
            copy.points.putAll(this.points);
            return copy;
        }
    }

    protected final String name;
    /// Visible only in admin tool
    protected final String description;
    public final Predicate<String> keyValidator;
    protected volatile Object2DoubleMap<String> values = new Object2DoubleOpenHashMap<>();
    private boolean visible = false;

    /**
     * Base class for custom experiences
     *
     * @param name Unique name of experience
     * @param info text to show in admin tool about values
     * @param keyValidator validator for admin tool
     */
    public Experience(String name, String info, Predicate<String> keyValidator) {
        this.name = name;
        this.description = info;
        this.keyValidator = keyValidator;
    }


    // Level section

    ///  return max level for experience, configurable from admin tool, -1 if experience is infinite
    public int getMaxLevel() {
        if (this.isInfinite())
            return -1;
        return this.getLevels().length;
    }

    ///  return experiences needed for currently level
    public double getExpForLevel(int level) {
        if (this.getLevels().length == 0)
            return Double.MAX_VALUE;

        if (level <= 0)
            return 0;

        // 1 -> levels[0]
        level -= 1;
        int le = this.getLevels().length;
        if (le > level) {
            Level lvl = this.getLevels()[level];
            if (!lvl.infinite())
                return lvl.exp();
            return this.getExpForLevel(level) + lvl.exp(); // level decremented
        }
        Level inf = this.getLevels()[le - 1];
        if (!inf.infinite())
            throw new IllegalArgumentException("cant get exp for non existing level");
        // [2, 4, 6, 10:inf]
        // le = 4
        // level = (x) -> x - 1
        //
        // (0) -> 0
        // (1) -> 2
        // (2) -> 4
        // (3) -> 6
        // (4) -> 16 -> (3) + 10
        // I am here
        // (5) -> 26 -> level - le = 0 -> n=2
        // (6) -> 36 -> level - le = 1 -> n=3
        // (7) -> 46 -> level - le = 2 -> n=4
        return this.getExpForLevel(le - 1) + inf.exp() * (level - le + 2);
    }
    ///  return level from exp, from 0 to up
    public int getLevel(double exp) {
        int le = this.getLevels().length;
        if (le == 0)
            return 0;

        int i = 0;
        for (Level level : this.getLevels()) {
            if (level.exp() > exp && !level.infinite())
                return i;
            i++;
        }

        Level last = this.getLevels()[le - 1];
        if (!last.infinite())
            return i;

        exp -= this.getExpForLevel(le - 1);
        return le - 1 + (int) (exp / last.exp());
    }

    /**
     * Not recommended to use</br>
     *
     * @return Experience Levels array, if {@code experience.isInfinity()} last one will be infinity Level
     *
     * @see Experience#getLevel(Ref, ComponentAccessor)
     * @see Experience#getExpForLevel(int)
     * @see Experience#getExpForNextLevel(Ref, ComponentAccessor)
     * @see Experience#getMaxLevel() 
     * @see Experience#canReachNextLevel(Ref, ComponentAccessor) 
     */
    public Level[] getLevels() {
        return Data.getLevels(this);
    }



    // Exp section

    /// return currently level of player, from 0 to up
    public int getLevel(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store) {
        double exp = this.getExp(ref, store);
        return this.getLevel(exp);
    }
    /// true if player can reach next level
    public boolean canReachNextLevel(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store) {
        return this.getLevel(ref, store) != this.getMaxLevel();
    }

    /// Get player full experience for next level
    public double getExpForNextLevel(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store) {
        int lvl = this.getLevel(ref, store);

        if (lvl == this.getMaxLevel())
            return -1;

        return this.getExpForLevel(lvl + 1);
    }

    /// Get player full experience
    public double getExp(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store) {
        ExperienceComponent component = this.getComponent(ref, store);
        return component.getExp(this);
    }
    /// Sets player full experience to exp value
    public void setExp(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store, double exp) {
        this.onComponent(ref, store, comp -> {
            double actual = comp.getExp(this);

            if (actual == exp)
                return;

            ExperienceChangeEvent event = new ExperienceChangeEvent(this, exp, ExperienceChangeEvent.Method.SET);
            store.invoke(ref, event);
            if (event.isCancelled())
                return;
            if (actual == event.getExp())
                return;

            comp.setExp(this, event.getExp());

            if (getLevel(event.getExp()) > getLevel(actual)) {
                LevelUpEvent levelUpEvent = new LevelUpEvent(this, getLevel(actual), getLevel(event.getExp()));
                store.invoke(ref, levelUpEvent);
            }
        });
    }
    /// Adds experience for a player
    public void addExp(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store, double exp) {
        if (exp == 0)
            return;
        // this.setExp(ref, store, this.getExp(ref, store) + exp);

        this.onComponent(ref, store, comp -> {
            double actual = comp.getExp(this);

            ExperienceChangeEvent event = new ExperienceChangeEvent(this, exp, ExperienceChangeEvent.Method.ADD);
            store.invoke(ref, event);

            if (event.isCancelled())
                return;
            if (event.getExp() == 0)
                return;

            comp.addExp(this, event.getExp());

            if (getLevel(event.getExp() + actual) > getLevel(actual)) {
                LevelUpEvent levelUpEvent = new LevelUpEvent(this, getLevel(actual), getLevel(event.getExp() + actual));
                store.invoke(ref, levelUpEvent);
            }
        });
    }


    /// true if Experience has not level cap, configurable in admin-tool
    public boolean isInfinite() {
        Level[] levels = this.getLevels();
        if (levels.length == 0)
            return true;
        return levels[levels.length - 1].infinite;
    }
    /// exp needed for lever over cap, -1 if isInfinite() == false
    public double getInfinityExp() {
        if (!this.isInfinite())
            return -1;
        Level[] levels = this.getLevels();
        return levels.length == 0 ? 0 : levels[levels.length - 1].exp;
    }



    // Points section

    /// Get player spell points from any source, spend + unspend
    public int getAllPoints(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store, Category category) {
        return this.getExpPoints(ref, store) + this.getPluginPoints(ref, store, category);
    }

    ///  Get player unspend spell points
    public int getUnspendPoints(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store, Category category) {
        return this.getAllPoints(ref, store, category) - this.getSpendPoints(ref, store, category);
    }

    /// Get player spell points only from experience
    public int getExpPoints(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store) {
        return this.getLevel(ref, store);
    }


    ///  Get player spell points from plugins
    public int getPluginPoints(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store, Category category) {
        return this.fromComponent(ref, store, category, ExperienceComponent::getPluginPoints);
    }
    ///  Add player spell point from plugins
    public void addPluginPoints(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store, Category category, int points) {
        this.onComponent(ref, store, comp -> comp.addPluginPoints(category, points));
    }
    /// Sets player spell points from plugins
    public void setPluginPoints(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store, Category category, int points) {
        this.onComponent(ref, store, comp -> comp.setPluginPoints(category, points));
    }


    ///  Get player spend spell points
    public int getSpendPoints(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store, Category category) {
        return this.fromComponent(ref, store, category, ExperienceComponent::getSpendPoints);
    }
    ///  Spend spell point like a player
    public void addSpendPoints(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store, Category category, int points) {
        this.onComponent(ref, store, comp -> comp.addSpendPoints(category, points));
    }
    ///  Sets player spend spell points
    public void setSpendPoints(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store, Category category, int points) {
        this.onComponent(ref, store, comp -> comp.setSpendPoints(category, points));
    }

    protected final <T> T fromComponent(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store, Category category, BiFunction<ExperienceComponent, Category, T> work) {
        ExperienceComponent component = this.getComponent(ref, store);
        return work.apply(component, category);
    }
    protected final void onComponent(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store, Consumer<ExperienceComponent> work) {
        ExperienceComponent component = this.getComponent(ref, store);;
        work.accept(component);
    }

    /// do work on categories with this experience set
    public final void onCategories(Consumer<Category> work) {
        for (Category category : Data.getCategories())
            if (category.experience() == this)
                work.accept(category);
    }


    /// Get player ExperienceComponent
    protected ExperienceComponent getComponent(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store) {
        return store.ensureAndGetComponent(ref, ExperienceComponent.getComponentType());
    }



    // Values section

    /**
     * Find the highest experiences in values
     *
     * @param test predicate to check keys
     * @return best experience
     *
     * @see Experience#getValue(String)
     */
    public double findBest(Predicate<String> test) {
        AtomicReference<Double> max = new AtomicReference<>((double) 0);
        this.forEachValue((key, exp) -> {
            if (exp > max.get() && test.test(key))
                max.set(exp);
        });
        return max.get();
    }

    /**
     * Get all values key for experience
     * @return all values keys
     *
     * @see Experience#getValue(String)
     */
    public Set<String> getValues() {
        return this.values.keySet();
    }

    /**
     * Check specified value is set
     * @param key value key
     * @return true if exists
     *
     * @see Experience#getValue(String)
     */
    public boolean containsValue(String key) {
        return this.values.containsKey(key);
    }

    /**
     * Iter by every value
     * @param work body
     *
     * @see Experience#getValue(String)
     * @see Experience#findBest(Predicate)
     */
    public void forEachValue(BiConsumer<String, Double> work) {
        this.values.forEach(work);
    }

    /// @see Experience#getValue(String)
    public final double getExp(String key) {
        return this.getValue(key);
    }

    /**
     * get experience for a specified key</br>
     * Values can be modified in admin tool
     *
     * @param key specified key
     * @return experience per key or 0 if not exists
     *
     * @see Experience#findBest(Predicate)
     */
    public double getValue(String key) {
        return this.values.getOrDefault(key, 0);
    }

    /**
     * not recommended to use
     * @param newValues new map of exp values
     */
    public void setValues(Map<String, Double> newValues) {
        this.values.clear();
        this.values.putAll(newValues);
    }



    // Utils section

    /// Get Experience name
    public String getName() {
        return name;
    }
    /// Get Experience values description
    public String getInfo() {
        return this.description;
    }

    /// true means Experience is used in gui, false means ignored
    public boolean isVisible() {
        return visible;
    }
    /// sets experience visible in gui
    public void setVisible(boolean visible) {
        this.visible = visible;
    }


    /// Optionally for Override triggered after loading data or changing data in admin tool
    public void onLoad() {
    }


    /// Easily access to Experience registry, use this in setup()
    public static ExperienceRegistry getRegistry() {
        return HySpellEnginePlugin.getInstance().getExperienceRegistry();
    }
}

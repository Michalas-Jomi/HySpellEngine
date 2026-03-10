package me.jomi.hyspellengine.api;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jomi.hyspellengine.HySpellEnginePlugin;
import me.jomi.hyspellengine.core.ExperienceRegistry;
import me.jomi.hyspellengine.utils.EasyCodec;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.bson.BsonInt32;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Experience {
    /**
     *
     * @param exp experience needed to reach level
     * @param infinite if true, exp will be reused for calculating, can be true only on last element
     * @param chatMessage chat message to send player on level up
     * @param sound sound to play for player on level up
     */
    public static record Level(double exp, boolean infinite, @NullableDecl String chatMessage, @NullableDecl String sound) {
    }
    public static class ExperienceComponent implements Component<EntityStore> {
        public static final BuilderCodec<ExperienceComponent> CODEC = EasyCodec.create(ExperienceComponent.class);

        public static ComponentType<EntityStore, ExperienceComponent> getComponentType() {
            return HySpellEnginePlugin.getInstance().getComponentType(ExperienceComponent.class);
        }


        // Map<String, Double> experience.name : exp
        @EasyCodec.ForCodec public BsonDocument experiences = new BsonDocument();
        // Map<String, [Integer, Integer]> experience.name : [points, spend points]
        @EasyCodec.ForCodec public BsonDocument points = new BsonDocument();

        public double getExp(Experience experience) {
            return experiences.containsKey(experience.getName()) ? experiences.getDouble(experience.getName()).getValue() : 0;
        }
        public void addExp(Experience experience, double exp) {
            exp += this.getExp(experience);
            this.setExp(experience, exp);
        }
        public void setExp(Experience experience, double exp) {
            if (exp <= 0)
                this.experiences.remove(experience.getName());
            else
                this.experiences.put(experience.getName(), new BsonDouble(exp));
        }

        private BsonArray ensureAndGetPoints(Experience experience) {
            if (this.points.containsKey(experience.getName()))
                return this.points.getArray(experience.getName());
            BsonArray array = new BsonArray();
            array.add(new BsonInt32(0));
            array.add(new BsonInt32(0));
            this.points.put(experience.getName(), array);
            return array;
        }

        // unspend + spend
        public int getPoints(Experience experience) {
            BsonArray array = this.ensureAndGetPoints(experience);
            return array.get(0).asInt32().getValue();
        }
        public void addPoints(Experience experience, int points) {
            BsonArray array = this.ensureAndGetPoints(experience);
            int all = array.get(0).asInt32().getValue();
            all += points;
            array.set(0, new BsonInt32(all));
        }
        public void setPoints(Experience experience, int points) {
            BsonArray array = this.ensureAndGetPoints(experience);
            array.set(0, new BsonInt32(points));
        }

        public int getUnspendPoints(Experience experience) {
            BsonArray array = this.ensureAndGetPoints(experience);
            return Math.max(0, array.get(0).asInt32().getValue() - array.get(1).asInt32().getValue());
        }
        // TODO rebuild to points from level + points from plugins
        public int getSpendPoints(Experience experience) {
            BsonArray array = this.ensureAndGetPoints(experience);
            return array.get(1).asInt32().getValue();
        }
        public void addSpendPoints(Experience experience, int points) {
            BsonArray array = this.ensureAndGetPoints(experience);
            int spend = array.get(1).asInt32().getValue();
            array.set(1, new BsonInt32(spend + points));
        }
        public void setSpendPoints(Experience experience, int points) {
            BsonArray array = this.ensureAndGetPoints(experience);
            array.set(1, new BsonInt32(points));
        }

        @NullableDecl
        @Override
        public Component<EntityStore> clone() {
            ExperienceComponent copy = new ExperienceComponent();
            copy.experiences = this.experiences.clone();
            copy.points = this.points.clone();
            return copy;
        }
    }

    private final String name;
    private final String description;
    private boolean visible = false;

    // TODO docs
    public Experience(String name, String info, Predicate<String> keyValidator) {
        this.name = name;
        this.description = info;
    }

    /// Easily access to Experience registry, use this in setup()
    public static ExperienceRegistry getRegistry() {
        return HySpellEnginePlugin.getInstance().getExperienceRegistry();
    }

    /// return currently level of player, from 0 to up
    public int getLevel(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store) {
        double exp = this.getExp(ref, store);
        int i = 0;
        for (Level level : this.getLevels()) { // TODO refactor
            if (level.infinite() || level.exp() > exp)
                break;
            i++;
        }
        return i;
    }

    ///  return max level for experience, configurable from admin tool, -1 if experience is infinite
    public int getMaxLevel() {
        if (this.isInfinite())
            return -1;
        return this.getLevels().length;
    }

    ///  return experiences needed for currently level
    public double getExpNeededForLevel(int level) {
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
            return this.getExpNeededForLevel(level) + lvl.exp(); // level decremented
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
        return this.getExpNeededForLevel(level) + inf.exp() * (level - le + 2);
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

        exp -= this.getExpNeededForLevel(le - 1);
        return le - 1 + (int) (exp / last.exp());
    }

    public Level[] getLevels() {
        return new Level[]{}; // TODO load from admin tool
    }

    public boolean canReachNextLevel(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store) {
        return this.getLevel(ref, store) == this.getMaxLevel();
    }

    /// Get player full experience for next level
    public double getExpForNextLevel(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store) {
        int lvl = this.getLevel(ref, store);

        if (lvl == this.getMaxLevel())
            return -1;

        return this.getExpNeededForLevel(lvl + 1);
    }

    /// Get player full experience
    public double getExp(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store) {
        return this.fromComponent(ref, store, ExperienceComponent::getExp);
    }
    /// Sets player full experience to exp value
    public void setExp(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store, double exp) {
        this.onComponent(ref, store, comp -> comp.setExp(this, exp));
    }
    /// Adds experience for a player
    public void addExp(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store, double exp) {
        int level = getLevel(ref, store);
        this.onComponent(ref, store, comp -> comp.addExp(this, exp));
        PlayerRef player = store.getComponent(ref, PlayerRef.getComponentType());
        HySpellEnginePlugin.debugLog("exp " + this.getName() + (this.isInfinite() ? ":inf" : "") + " gained for " + player.getUsername() + " " + getExp(ref, store) + " / " + getExpForNextLevel(ref, store));
        if (getLevel(ref, store) > level)
            this.onLevelUp(ref, store);
    }

    ///  Get player all spell points spend + unspend
    public int getPoints(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store) {
        return this.fromComponent(ref, store, ExperienceComponent::getPoints);
    }
    ///  Add player spell point
    public void addPoints(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store, int points) {
        this.onComponent(ref, store, comp -> comp.addPoints(this, points));
    }
    /// Sets player spell points
    public void setPoints(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store, int points) {
        this.onComponent(ref, store, comp -> comp.setPoints(this, points));
    }

    ///  Get player unspend spell points
    public int getUnspendPoints(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store) {
        return this.fromComponent(ref, store, ExperienceComponent::getUnspendPoints);
    }
    ///  Get player spend spell points
    public int getSpendPoints(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store) {
        return this.fromComponent(ref, store, ExperienceComponent::getSpendPoints);
    }
    ///  Spend spell point like a player
    public void addSpendPoints(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store, int points) {
        this.onComponent(ref, store, comp -> comp.addSpendPoints(this, points));
    }
    ///  Sets player spend spell points
    public void setSpendPoints(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store, int points) {
        this.onComponent(ref, store, comp -> comp.setSpendPoints(this, points));
    }

    protected <T> T fromComponent(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store, BiFunction<ExperienceComponent, Experience, T> work) {
        ExperienceComponent component = this.getComponent(ref, store);
        return work.apply(component, this);
    }
    protected void onComponent(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store, Consumer<ExperienceComponent> work) {
        ExperienceComponent component = this.getComponent(ref, store);;
        work.accept(component);
    }


    ///  triggers on player level up
    public void onLevelUp(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store) {
        // TODO
    }

    /// Get player ExperienceComponent
    public ExperienceComponent getComponent(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store) {
        return store.ensureAndGetComponent(ref, ExperienceComponent.getComponentType());
    }

    /// Get Experience name
    public String getName() {
        return name;
    }
    /// Get Experience values description
    public String getInfo() {
        return this.description;
    }


    public double findBest(Predicate<String> test) {
        AtomicReference<Double> max = new AtomicReference<>((double) 0);
        this.forEachValue((key, exp) -> {
            if (exp > max.get() && test.test(key))
                max.set(exp);
        });
        return max.get();
    }

    public Set<String> getValues() {
        return null; // TODO
    }
    public boolean containsValues(String value) {
        return true; // TODO
    }
    public void forEachValue(BiConsumer<String, Double> work) {
        // TODO
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


    /// true means Experience is used in gui, false means ignored
    public boolean isVisible() {
        return visible;
    }
    /// sets experience visible in gui
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}

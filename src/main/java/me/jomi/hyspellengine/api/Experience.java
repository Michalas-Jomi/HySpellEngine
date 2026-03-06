package me.jomi.hyspellengine.api;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import me.jomi.hyspellengine.HySpellEnginePlugin;
import me.jomi.hyspellengine.core.ExperienceRegistry;
import me.jomi.hyspellengine.utils.EasyCodec;
import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public record Experience(String name) {
    public static record Level(double exp, @NullableDecl String chatMessage, @NullableDecl String sound) {
    }
    public static class ExperienceComponent implements Component<EntityStore> {
        public static final BuilderCodec<ExperienceComponent> CODEC = EasyCodec.create(ExperienceComponent.class);

        public static ComponentType<EntityStore, ExperienceComponent> getComponentType() {
            return HySpellEnginePlugin.getInstance().getComponentType(ExperienceComponent.class);
        }


        // Map<String, Double> experience.name : exp
        @EasyCodec.ForCodec public BsonDocument experiences = new BsonDocument();

        public double getExp(Experience experience) {
            return experiences.containsKey(experience.name()) ? experiences.getDouble(experience.name()).getValue() : 0;
        }

        public void addExp(Experience experience, double exp) {
            exp += this.getExp(experience);
            this.setExp(experience, exp);
        }

        public void setExp(Experience experience, double exp) {
            if (exp <= 0)
                this.experiences.remove(experience.name());
            else
                this.experiences.put(experience.name(), new BsonDouble(exp));
        }


        @NullableDecl
        @Override
        public Component<EntityStore> clone() {
            ExperienceComponent copy = new ExperienceComponent();
            copy.experiences = this.experiences.clone();
            return copy;
        }
    }

    public static ExperienceRegistry getRegistry() {
        return HySpellEnginePlugin.getInstance().getExperienceRegistry();
    }

    // levels[getLeve()] -> next Level
    public int getLevel(Ref<EntityStore> ref, Store<EntityStore> store) {
        double exp = this.getExp(ref, store);
        int i = 0;
        for (Level level : this.getLevels()) {
            if (level.exp() > exp)
                break;
            i++;
        }
        return i;
    }

    public int getMaxLevel() {
        return this.getLevels().length;
    }

    public Level[] getLevels() {
        return new Level[]{}; // TODO load from admin tool
    }

    public double getExp(Ref<EntityStore> ref, Store<EntityStore> store) {
        ExperienceComponent component = store.getComponent(ref, ExperienceComponent.getComponentType());
        if (component != null)
            return component.getExp(this);
        return 0;
    }

    public double getExpForNextLevel(Ref<EntityStore> ref, Store<EntityStore> store) {
        int lvl = this.getLevel(ref, store);

        if (lvl == this.getMaxLevel())
            return -1;

        return this.getLevels()[lvl].exp();
    }

    public void addExp(Ref<EntityStore> ref, Store<EntityStore> store, double exp) {
        ExperienceComponent component = store.ensureAndGetComponent(ref, ExperienceComponent.getComponentType());
        int level = getLevel(ref, store);
        component.addExp(this, exp);
        if (getLevel(ref, store) > level)
            this.onLevelUp(ref, store);
    }
    public void setExp(Ref<EntityStore> ref, Store<EntityStore> store, double exp) {
        ExperienceComponent component = store.ensureAndGetComponent(ref, ExperienceComponent.getComponentType());
        component.setExp(this, exp);
    }

    // Optionally for Override
    public void onLevelUp(Ref<EntityStore> ref, Store<EntityStore> store) {
    }
}

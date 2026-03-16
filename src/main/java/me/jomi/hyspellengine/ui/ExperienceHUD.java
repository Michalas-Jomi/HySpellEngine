package me.jomi.hyspellengine.ui;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.jomi.hyspellengine.api.Experience;
import me.jomi.hyspellengine.utils.UIBuilder;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ExperienceHUD extends CustomUIHud {
    private static class Memory {
        public List<Experience> expIndexes = new ArrayList<>();
        public List<ScheduledFuture<?>> tasks = new ArrayList<>();
        public CustomUIHud hud;

        public Memory(CustomUIHud hud) {
            this.hud = hud;
        }

        public void set(CustomUIHud hud, Experience experience, int lvl, float percent) {
            ScheduledFuture<?> task = HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                if (hud != this.hud)
                    return;

                int index = expIndexes.indexOf(experience);
                UIBuilder uiBar = new UIBuilder("#ExperienceBarsContainerRoot").at("#Bars", index);
                uiBar.remove("");

                hud.update(false, uiBar.ui());
                expIndexes.remove(index);
                tasks.remove(index);
            }, VISIBLE_TIME, TimeUnit.SECONDS);

            UIBuilder ui = new UIBuilder("#ExperienceBarsContainerRoot");
            UIBuilder uiBar;
            if (expIndexes.contains(experience)) {
                int index = expIndexes.indexOf(experience);
                tasks.get(index).cancel(true);

                uiBar = ui.at("#Bars", index);

                tasks.set(index, task);
            } else {
                ui.append("#Bars", LAYOUT_BAR);
                uiBar = ui.at("#Bars", this.tasks.size());

                this.tasks.add(task);
                this.expIndexes.add(experience);
            }


            uiBar.set("#LevelLabel.Text", lvl + "lvl");
            uiBar.set("#NameLabel.Text", experience.getName());
            uiBar.set("#PercentLabel.Text", Math.round(percent * 100) + "%");
            uiBar.set("#ProgressBar.Value", percent);

            hud.update(false, uiBar.ui());
        }
    }

    public static final String LAYOUT_MAIN = "HySpellEngine/HUD/BarsContainer.ui";
    public static final String LAYOUT_BAR = "HySpellEngine/HUD/ExperienceBar.ui";
    public static final int VISIBLE_TIME = 5; // seconds

    public ExperienceHUD(@NonNullDecl PlayerRef playerRef) {
        super(playerRef);
    }

    @Override
    protected void build(@NonNullDecl UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append(LAYOUT_MAIN);
    }

    // every thread has its own entries
    private static Map<UUID, Memory> map = new Object2ObjectOpenHashMap<>(); // new ConcurrentHashMap<>();
    public static void modify(Ref<EntityStore> ref, ComponentAccessor<EntityStore> store, Experience experience, int lvl, float percent) {
        percent = Math.max(0, Math.min(1, percent));
        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (player == null || playerRef == null)
            return;

        CustomUIHud hud = player.getHudManager().getCustomHud();
        Memory lastUse = map.get(playerRef.getUuid());
        Memory memory = lastUse;

        if (hud == null)
            hud = makeHUD(player, playerRef);
        else if (lastUse == null)
            hud.update(false, new UIBuilder().append(LAYOUT_MAIN).ui());

        if (lastUse == null) {
            memory = new Memory(hud);
            map.put(playerRef.getUuid(), memory);
        }

        memory.set(hud, experience, lvl, percent);

        if (hud != memory.hud)
            memory.hud = hud;
    }
    private static ExperienceHUD makeHUD(Player player, PlayerRef playerRef) {
        ExperienceHUD hud = new ExperienceHUD(playerRef);
        player.getHudManager().setCustomHud(playerRef, hud);
        return hud;
    }
}

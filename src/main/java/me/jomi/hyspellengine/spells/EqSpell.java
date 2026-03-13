package me.jomi.hyspellengine.spells;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.shorts.ShortArraySet;
import me.jomi.hyspellengine.api.Spell;
import me.jomi.hyspellengine.core.SpellContext;
import me.jomi.hyspellengine.core.SpellField;

import java.util.List;
import java.util.Set;

public class EqSpell extends Spell {
    private final SpellField<String> toRemoveField;
    private final SpellField<String> toAddField;

    public EqSpell() {
        super("item", "gives and removed defined items from player inventory");
        this.toRemoveField = this.requireFieldString("remove", "Items list to remove separated by space"); // list, empty for nothing
        this.toAddField = this.requireFieldString("give", "Items list to give separated by space");
    }

    @Override
    public void apply(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store) {
        Player p = store.getComponent(ref, Player.getComponentType());

        this.removeItems(p, this.toRemoveField.getValue(context));

        String toAdd = this.toAddField.getValue(context);
        if (toAdd.isBlank())
            return;

        for (String id : toAdd.split(" ")) {
            p.getInventory().getCombinedArmorHotbarUtilityStorage().addItemStack(new ItemStack(id));
        }
    }

    @Override
    public void unapply(SpellContext context, Ref<EntityStore> ref, Store<EntityStore> store) {
        Player p = store.getComponent(ref, Player.getComponentType());
        removeItems(p, this.toAddField.getValue(context));
    }

    private void removeItems(Player p, String itemList) {
        if (itemList.isBlank())
            return;

        List<String> toRemoveNames = List.of(itemList.split(" "));
        CombinedItemContainer inv = p.getInventory().getCombinedEverything();
        Set<Short> toRemove = new ShortArraySet();
        inv.forEach((slot, item) -> {
            if (item != null && toRemoveNames.contains(item.getItemId()))
                toRemove.add(slot);
        });
        toRemove.forEach(inv::removeItemStackFromSlot);
    }
}

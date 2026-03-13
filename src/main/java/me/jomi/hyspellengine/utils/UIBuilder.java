package me.jomi.hyspellengine.utils;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.*;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;

import java.util.Arrays;
import java.util.List;

/**
 * UI and Events builder for clean code</br>
 * Example usage
 * <pre>
 * {@code
 * @Override
 * public void build(Ref<EntityStore> ref, UICommandBuilder uiBuilder, UIEventBuilder events, Store<EntityStore> store) {
 *     UIBuilder ui = new UIBuilder(this, uiBuilder, events);
 *     ui.append(LAYOUT);
 *     ui.onClick("close", "#CloseButton");
 * }
 * @Override
 * public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, ExampleEventData data) {
 *     switch(data.action) {
 *         /// work
 *     }
 * }}</pre>
 *
 * @param page Page witch Data contains in its codec fields ACTION & @VALUE
 * @param selector Selector in .ui files
 * @param ui Optionally param for InteractiveCustomUIPage.build()
 * @param events Optionally param for InteractiveCustomUIPage.build()
 */
public record UIBuilder(InteractiveCustomUIPage<?> page, String selector, UICommandBuilder ui, UIEventBuilder events) {
    public UIBuilder(InteractiveCustomUIPage<?> page, String selector, UICommandBuilder ui, UIEventBuilder events) {
        this.events = events;
        this.page = page;
        this.ui = ui;

        if (!selector.endsWith(" ") && !selector.isBlank())
            selector += " ";

        this.selector = selector;
    }
    public UIBuilder(InteractiveCustomUIPage<?> page) {
        this(page, "", new UICommandBuilder(), new UIEventBuilder());
    }
    public UIBuilder(InteractiveCustomUIPage<?> page, UICommandBuilder ui, UIEventBuilder events) {
        this(page, "", ui, events);
    }

    private String selector(String selector) {
        if (selector.isBlank())
            return this.selector.trim();

        if (selector.startsWith("."))
            return this.selector.trim() + selector;

        return this.selector + selector;
    }

    /**
     * <pre>
     * {@code
     * ui.at("#Root").set("#Element", value);
     * ui.at("#Root", 2).set("#Element", value);
     * }</pre>
     * equals</br>
     * <pre>
     * {@code
     * ui.set("#Root #Element", value);
     * ui.set("#Root[2] #Element", value);
     * }</pre>
     * with reference to reuse
     * @param selector path in .ui files
     * @return new UIBuilder at selected location
     */
    public UIBuilder at(String selector) {
        return new UIBuilder(this.page, this.selector(selector), this.ui, this.events);
    }

    /** @see UIBuilder#at(String)  */
    public UIBuilder at(String selector, int index) {
        return this.at(selector.trim() + "[" + index + "]");
    }

    ///  Event data needs "ACTION" in codec keys
    public UIBuilder on(CustomUIEventBindingType type, String selector, String action, String... keyValue) {
        EventData eventData = EventData.of("ACTION", action);
        for (int i=0; i < keyValue.length; i += 2)
            eventData.put(keyValue[i], keyValue[i+1]);
        this.events.addEventBinding(type, this.selector(selector), eventData);
        return this;
    }

    public UIBuilder onClick(String selector, String action, String... data) {
        this.on(CustomUIEventBindingType.Activating, selector, action, data);
        return this;
    }
    public UIBuilder onClickRight(String selector, String action, String... data) {
        this.on(CustomUIEventBindingType.RightClicking, selector, action, data);
        return this;
    }
    /// selector must be a TextInput, Event data needs "@VALUE" in codec keys
    public UIBuilder onChange(String selector, String action, String... data) {
        data = Arrays.copyOf(data, data.length + 2);
        data[data.length - 2] = "@VALUE";
        data[data.length - 1] = this.selector(selector).trim() + ".Value";
        this.on(CustomUIEventBindingType.ValueChanged, selector, action, data);
        return this;
    }
    public UIBuilder onCheckBox(String selector, String action, String... data) {
        this.on(CustomUIEventBindingType.ValueChanged, selector, action, data);
        return this;
    }

    public UIBuilder set(String selector, String value) {
        ui.set(this.selector(selector), value);
        return this;
    }
    public UIBuilder set(String selector, boolean value) {
        ui.set(this.selector(selector), value);
        return this;
    }
    public UIBuilder set(String selector, Value<?> value) {
        ui.set(this.selector(selector), value);
        return this;
    }
    /// safe for float too
    public UIBuilder set(String selector, double value) {
        ui.set(this.selector(selector), value);
        return this;
    }
    public UIBuilder set(String selector, int value) {
        ui.set(this.selector(selector), value);
        return this;
    }
    public UIBuilder setNull(String selector) {
        ui.setNull(this.selector(selector));
        return this;
    }
    public UIBuilder set(String selector, List<?> list) {
        ui.set(this.selector(selector), list);
        return this;
    }
    /// Available Objects: (com.hypixel.hytale.server.core.ui) Area ItemGridSlot ItemStack LocalizableString PatchStyle DropdownEntryInfo Anchor
    public UIBuilder setObject(String selector, Object obj) {
        ui.setObject(selector, obj);
        return this;
    }

    public UIBuilder remove(String selector) {
        ui.remove(this.selector(selector));
        return this;
    }

    public UIBuilder clear(String selector) {
        ui.clear(this.selector(selector));
        return this;
    }
    public UIBuilder clear() {
        ui.clear(this.selector.trim());
        return this;
    }

    public UIBuilder append(String selector, String layout) {
        ui.append(this.selector(selector), layout);
        return this;
    }
    public UIBuilder append(String layout) {
        if (this.selector.trim().isBlank())
            ui.append(layout);
        else
            ui.append(this.selector.trim(), layout);
        return this;
    }
}

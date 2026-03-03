package me.jomi.hyspellengine.utils;

import com.hypixel.hytale.component.Component;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public abstract class EasyComponent<S> implements Component<S> {
    @Override
    public Component<S> clone() {
        try {
            EasyComponent<S> instance = getClass().getDeclaredConstructor().newInstance();
            for (Field field : EasyCodec.getFields(getClass())) {
                field.set(instance, field.get(this));
            }
            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}

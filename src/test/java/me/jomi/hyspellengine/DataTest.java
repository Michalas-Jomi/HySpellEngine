package me.jomi.hyspellengine;

import me.jomi.hyspellengine.core.Category;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataTest {

    @Test
    void load() {
        HySpellEnginePlugin.getInstance().setup();

        Data.addCategory(DefaultData.categoryCombat());
        Category[] categories = Data.getCategories();
        Data.save();
        Data.load();

        assert categories.length == Data.getCategories().length;
        for (int i=0; i < categories.length; i++) {
            assert categories[i].equals(Data.getCategories()[i]);
        }
    }
}
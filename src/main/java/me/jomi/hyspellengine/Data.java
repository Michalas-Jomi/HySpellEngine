package me.jomi.hyspellengine;

import me.jomi.hyspellengine.core.Category;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.Arrays;

public class Data {
    private static Category[] categories;
    // TODO load experiences levels, not experiences

    public static void load() {
        categories = new Category[0]; // temp

        try {
            load0();
        } catch (Throwable e) {
        }

        if (categories.length == 0)  {
            ExampleData.makeExampleData();
            save();
        }
    }
    private static void load0() {
        // TODO
    }
    public static void save() {
        // TODO
    }

    @NonNullDecl
    public static Category[] getCategories() {
        return categories;
    }
    public static void setCategories(Category[] categories) {
        Data.categories = categories;
        Data.save();
    }
    public static void addCategory(Category category) {
        categories = Arrays.copyOf(categories, categories.length + 1);
        categories[categories.length - 1] = category;
        Data.save();
    }
    public static void removeCategory(Category category) {
        categories = Arrays.stream(categories)
                .filter(cat -> cat != category)
                .toArray(Category[]::new);
        Data.save();
    }
}

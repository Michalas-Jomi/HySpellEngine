package me.jomi.hyspellengine;

import me.jomi.hyspellengine.api.Experience;
import me.jomi.hyspellengine.core.Category;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.Arrays;

public class Data {
    private static Category[] categories = new Category[0];
    // TODO load experiences levels, not experiences

    public static void load() {
        try {
            load0();
        } catch (Throwable e) {
        }

        if (categories.length == 0)  {
            DefaultData.makeExampleData();
            save();
        }

        updateExperienceVisibility();
    }
    private static void load0() {
        // TODO
    }
    public static void save() {
        // TODO
    }

    public static void updateExperienceVisibility() {
        Experience.getRegistry().forEach((key, exp) -> exp.setVisible(false));
        for (Category category : categories)
            category.experience().setVisible(true);
    }

    @NonNullDecl
    public static Category[] getCategories() {
        return categories;
    }
    public static void setCategories(Category[] categories) {
        Data.categories = categories;
        Data.save();
        updateExperienceVisibility();
    }
    public static void addCategory(Category category) {
        Data.addCategory(category, categories.length);
    }
    public static void addCategory(Category category, int index) {
        Category[] categories = new Category[Data.categories.length + 1];
        int old = 0;
        for (int i=0; i < categories.length; i++) {
            categories[i] = i == index ? category : Data.categories[old++];
        }
        Data.categories = categories;
        Data.save();
        updateExperienceVisibility();
    }
    public static void removeCategory(Category category) {
        categories = Arrays.stream(categories)
                .filter(cat -> cat != category)
                .toArray(Category[]::new);
        Data.save();
        updateExperienceVisibility();
    }

    public static void replaceCategory(Category oldCategory, Category newCategory) {
        int index = -1;
        while (true) {
            if (Data.categories[++index] == oldCategory)
                break;
        }

        Data.categories[index] = newCategory;
        Data.save();
        updateExperienceVisibility();
    }
}

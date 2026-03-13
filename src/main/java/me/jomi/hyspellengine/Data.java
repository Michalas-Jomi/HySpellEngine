package me.jomi.hyspellengine;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import me.jomi.hyspellengine.api.Experience;
import me.jomi.hyspellengine.core.Category;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Data {
    private static volatile Category[] categories = new Category[0];
    private static volatile Map<String, Experience.Level[]> experiences = new Object2ObjectArrayMap<>();

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
        experiences.clear();
        // experience.setValues()

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

    @NonNullDecl
    public static Experience.Level[] getLevels(@NonNullDecl Experience experience) {
        return experiences.computeIfAbsent(experience.getName(), name -> new Experience.Level[0]);
    }
    public static void set(Experience experience, Experience.Level[] levels) {
        experiences.put(experience.getName(), levels);
        Data.save();
    }
    public static void set(Experience experience, String key, double value) {
        Map<String, Double> values = new HashMap<>();
        experience.forEachValue(values::put);
        if (value == 0)
            values.remove(key);
        else
            values.put(key, value);
        experience.setValues(values);
        Data.save();
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

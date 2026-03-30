package me.jomi.hyspellengine;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import me.jomi.hyspellengine.api.Experience;
import me.jomi.hyspellengine.api.Spell;
import me.jomi.hyspellengine.core.Category;
import me.jomi.hyspellengine.core.SpellContext;
import org.bson.*;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.io.BasicOutputBuffer;
import org.bson.io.ByteBufferBsonInput;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Data {
    private static volatile Category[] categories = new Category[0];
    private static volatile Map<String, Experience.Level[]> experiences = new Object2ObjectArrayMap<>();
    private static final Path PATH = HySpellEnginePlugin.getInstance().getDataDirectory().resolve("data.bin");
    private static final byte version = 1;

    public static void load() {
        categories = new Category[0];
        experiences.clear();

        if (Files.notExists(PATH)) {
            DefaultData.makeExampleData(PATH);
        }

        try (DataInputStream in = new DataInputStream(Files.newInputStream(PATH))) {
            load0(in);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        if (categories.length == 0)  {
            DefaultData.makeExampleData(PATH);
            Data.save();
        }

        updateExperienceVisibility();

        HySpellEnginePlugin.getInstance().getExperienceRegistry().forEach((_, exp) -> exp.onLoad());
    }
    private static void load0(DataInputStream in) throws IOException {
        int version = in.readByte();
        migrate(version);

        in.readNBytes(188);


        int le = in.readShort();
        List<Category> categoryList = new ArrayList<>();
        for (int i = 0; i < le; i++) {
            Category category = loadCategory(in);
            if (category != null)
                categoryList.add(category);
        }
        categories = categoryList.toArray(Category[]::new);

        int experiences = in.readShort();
        Set<String> notLoaded = new HashSet<>(Experience.getRegistry().getKeys());
        for (int i=0; i < experiences; i++)
            notLoaded.remove(loadExperience(in));
        if (!notLoaded.isEmpty())
            HySpellEnginePlugin.warn("Data for " + String.join(", ", notLoaded) + " Experiences not found!");
    }
    private static Category loadCategory(DataInputStream in) throws IOException {
        Category.Display display = new Category.Display(
                in.readUTF(),
                in.readUTF(),
                Path.of(in.readUTF())
        );
        UUID uuid = loadUUID(in);
        String experienceName = in.readUTF();
        Experience experience = Experience.getRegistry().getExperience(experienceName);
        if (experience == null)
            HySpellEnginePlugin.warn("Can't load Category " + display.name() + "data with nonexisting Experience " + experienceName + ". Using admin tool (/spellsedit) will permanently remove its data");

        SpellContext root = loadSpell(in);
        if (root == null)
            HySpellEnginePlugin.warn("Can't load Category " + display.name() + "data with nonexisting Spell root. Using admin tool (/spellsedit) will permanently remove its data");

        if (root == null || experience == null)
            return null;

        return new Category(
                display,
                experience,
                root,
                uuid
        );
    }
    private static SpellContext loadSpell(DataInputStream in) throws IOException {
        SpellContext.Display display = new SpellContext.Display(
                in.readUTF(),
                in.readUTF(),
                Path.of(in.readUTF())
        );

        String spellName = in.readUTF();
        Spell spell = Spell.getSpellRegistry().getSpell(spellName);
        if (spell == null)
            HySpellEnginePlugin.warn("Can't load data for nonexisting Spell " + spellName + " in " + display.name() + ". Using admin tool (/spellsedit) will permanently remove its data");

        UUID uuid = loadUUID(in);

        byte[] fieldsData = in.readNBytes(in.readInt());
        BsonBinaryReader reader = new BsonBinaryReader(new ByteBufferBsonInput(new ByteBufNIO(ByteBuffer.wrap(fieldsData))));
        BsonDocument fields = new BsonDocumentCodec().decode(reader, DecoderContext.builder().build());

        int le = in.readByte();
        List<SpellContext> children = new ArrayList<>();
        for (int i=0; i < le; i++) {
            SpellContext child = loadSpell(in);
            if (child != null)
                children.add(child);
        }

        if (spell == null)
            return null;

        return new SpellContext(
                spell,
                display,
                uuid,
                fields,
                children.toArray(SpellContext[]::new)
        );
    }
    private static String loadExperience(DataInputStream in) throws IOException {
        String name = in.readUTF();
        Experience experience = Experience.getRegistry().getExperience(name);
        if (experience == null)
            HySpellEnginePlugin.warn("Can't load data for nonexisting Experience: " + name + ". Using admin tool (/spellsedit) will permanently remove its data");

        int le = in.readShort();
        Map<String, Double> values = new HashMap<>();
        for (int i=0; i < le; i++)
            values.put(in.readUTF(), in.readDouble());
        if (experience != null)
            experience.setValues(values);

        le = in.readShort();
        Experience.Level[] levels = new Experience.Level[le];
        for (int i=0; i < le; i++)
            levels[i] = new Experience.Level(
                    in.readFloat(),
                    false,
                    in.readUTF(),
                    in.readUTF()
            );
        boolean infinity = in.readBoolean();
        if (infinity && le > 0)
            levels[le - 1] = new Experience.Level(
                    levels[le - 1].exp(),
                    true,
                    levels[le - 1].chatMessage(),
                    levels[le - 1].sound()
            );

        if (experience != null)
            experiences.put(name, levels);

        return name;
    }
    private static UUID loadUUID(DataInputStream in) throws IOException {
        return new UUID(
                in.readLong(),
                in.readLong()
        );
    }

    private static void migrate(int fileVersion) {
        if (fileVersion == version)
            return;
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(PATH))) {
            save0(out);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
    private static void save0(DataOutputStream out) throws IOException {
        out.writeByte(version);
        out.write(new byte[]{0x0,0xffffffba,0x41,0x72,0x65,0x20,0x79,0x6f,0x75,0x20,0x72,0x65,0x61,0x6c,0x6c,0x79,0x20,0x74,0x72,0x79,0x69,0x6e,0x67,0x20,0x6f,0x70,0x65,0x6e,0x20,0x74,0x68,0x69,0x73,0x20,0x66,0x69,0x6c,0x65,0x3f,0x20,0x47,0x6f,0x6f,0x64,0x20,0x6c,0x75,0x63,0x6b,0x21,0x20,0x53,0x6f,0x75,0x72,0x63,0x65,0x20,0x73,0x68,0x6f,0x75,0x6c,0x64,0x20,0x62,0x65,0x20,0x68,0x65,0x6c,0x70,0x66,0x75,0x6c,0x20,0x68,0x74,0x74,0x70,0x73,0x3a,0x2f,0x2f,0x67,0x69,0x74,0x68,0x75,0x62,0x2e,0x63,0x6f,0x6d,0x2f,0x4d,0x69,0x63,0x68,0x61,0x6c,0x61,0x73,0x2d,0x4a,0x6f,0x6d,0x69,0x2f,0x48,0x79,0x53,0x70,0x65,0x6c,0x6c,0x45,0x6e,0x67,0x69,0x6e,0x65,0x20,0x6f,0x72,0x20,0x6a,0x75,0x73,0x74,0x20,0x75,0x73,0x65,0x20,0x2f,0x73,0x70,0x65,0x6c,0x6c,0x73,0x65,0x64,0x69,0x74,0x20,0x61,0x6e,0x79,0x20,0x71,0x75,0x65,0x73,0x74,0x69,0x6f,0x6e,0x73,0x3f,0x20,0x61,0x73,0x6b,0x20,0x6d,0x65,0x20,0x6f,0x6e,0x20,0x64,0x69,0x73,0x63,0x6f,0x72,0x64,0x3a,0x20,0x4a,0x6f,0x6d,0x69,0x2e,0x30,0x20});

        out.writeShort(categories.length);
        for (Category category : categories)
            saveCategory(out, category);

        Set<String> keys = Experience.getRegistry().getKeys();
        out.writeShort(keys.size());
        for (String key : keys)
            saveExperience(out, Experience.getRegistry().getExperience(key));
    }
    private static void saveCategory(DataOutputStream out, Category category) throws IOException {
        out.writeUTF(category.display().name());
        out.writeUTF(category.display().description());
        out.writeUTF(category.display().icon().toString());

        saveUUID(out, category.uuid());

        out.writeUTF(category.experience().getName());

        saveSpell(out, category.root());
    }
    private static void saveSpell(DataOutputStream out, SpellContext spell) throws IOException {
        out.writeUTF(spell.getDisplay().name());
        out.writeUTF(spell.getDisplay().description());
        out.writeUTF(spell.getDisplay().icon().toString());

        out.writeUTF(spell.getSpell().getName());

        saveUUID(out, spell.getUuid());


        BasicOutputBuffer buffer = new BasicOutputBuffer();
        new BsonDocumentCodec().encode(
                new BsonBinaryWriter(buffer),
                spell.getFieldsData(),
                EncoderContext.builder().build()
        );
        byte[] fieldsData = buffer.toByteArray();
        out.writeInt(fieldsData.length);
        out.write(fieldsData);

        out.writeByte(spell.getChildren().length);
        for (SpellContext child : spell.getChildren()) {
            saveSpell(out, child);
        }
    }
    private static void saveExperience(DataOutputStream out, Experience experience) throws IOException {
        out.writeUTF(experience.getName());

        Set<String> values = experience.getValues();
        out.writeShort(values.size());
        for (String value : values) {
            out.writeUTF(value);
            out.writeDouble(experience.getValue(value));
        }

        Experience.Level[] levels = experience.getLevels();
        out.writeShort(levels.length);
        for (Experience.Level level : levels) {
            out.writeFloat((float) level.exp());
            out.writeUTF(level.chatMessage());
            out.writeUTF(level.sound());
        }
        out.writeBoolean(experience.isInfinite());
    }
    private static void saveUUID(DataOutputStream out, UUID uuid) throws IOException {
        out.writeLong(uuid.getMostSignificantBits());
        out.writeLong(uuid.getLeastSignificantBits());
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
        for (int i=0; i < categories.length; i++)
            categories[i] = i == index ? category : Data.categories[old++];
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
        experience.onLoad();
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
        experience.onLoad();
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

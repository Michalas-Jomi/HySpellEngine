package me.jomi.hyspellengine.utils;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class EasyCodecTest {
    public static class TestMap {;
        @EasyCodec.ForCodec public Map<String, Double> doubleMap = new HashMap<>();
        @EasyCodec.ForCodec public Map<String, Integer> intMap = new HashMap<>();
        @EasyCodec.ForCodec public Map<String, String> stringMap = new HashMap<>();
    }
    public static class TestList {
        @EasyCodec.ForCodec public List<String> strs = new ArrayList<>();
        @EasyCodec.ForCodec public List<Integer> ints = new ArrayList<>();
        @EasyCodec.ForCodec public List<int[]> intsArr = new ArrayList<>();
    }
    public static class Insertions {
        public static class Level {
            public static final BuilderCodec<Level> CODEC = EasyCodec.create(Level.class);
            @EasyCodec.ForCodec public UUID uuid = null;
        }
        @EasyCodec.ForCodec public Map<String, List<Integer>> map = new HashMap<>();
        @EasyCodec.ForCodec public List<Map<String, Integer>> list = new ArrayList<>();
        @EasyCodec.ForCodec public Level level = null;
    }

    @Test
    public void mapCodec() {
        BuilderCodec<TestMap> codec = EasyCodec.create(TestMap.class);

        TestMap map = new TestMap();

        map.doubleMap.put("d2", 2d);
        map.doubleMap.put("d3.5", 3.5);

        map.intMap.put("i3", 3);

        map.stringMap.put("t", "test");
        map.stringMap.put("t2", "test2");

        TestMap decoded = codec.decode(codec.encode(map));

        assert decoded.doubleMap.get("d2") == 2;
        assert decoded.doubleMap.get("d3.5") == 3.5;

        assert decoded.intMap.get("i3") == 3;

        assert "test".equals(decoded.stringMap.get("t"));
        assert "test2".equals(decoded.stringMap.get("t2"));
    }

    @Test
    public void listCodec() {
        BuilderCodec<TestList> codec = EasyCodec.create(TestList.class);

        TestList list = new TestList();

        list.strs.add("a1");
        list.strs.add("a2");

        list.ints.add(3);
        list.ints.add(5);
        list.ints.add(9);

        list.intsArr.add(new int[]{4, 7});
        list.intsArr.add(new int[]{6, 8});

        TestList decoded = codec.decode(codec.encode(list));

        assert "a1".equals(decoded.strs.get(0));
        assert "a2".equals(decoded.strs.get(1));

        assert decoded.ints.get(0) == 3;
        assert decoded.ints.get(1) == 5;
        assert decoded.ints.get(2) == 9;

        int[] arr;
        arr = decoded.intsArr.get(0);
        assert arr[0] == 4 && arr[1] == 7 && arr.length == 2;
        arr = decoded.intsArr.get(1);
        assert arr[0] == 6 && arr[1] == 8 && arr.length == 2;
    }

    @Test
    public void insertions() {
        BuilderCodec<Insertions> codec = EasyCodec.create(Insertions.class);

        Insertions ins = new Insertions();

        ins.map.put("key1", List.of(3, 4));

        ins.list.add(Map.of("k1", 2, "k2", 4));
        ins.list.add(Map.of("k3", 21, "k4", 43));


        UUID uuid = UUID.randomUUID();
        ins.level = new Insertions.Level();
        ins.level.uuid = uuid;

        Insertions decoded = codec.decode(codec.encode(ins));

        assert decoded.map.get("key1").get(0) == 3;
        assert decoded.map.get("key1").get(1) == 4;
        assert decoded.map.size() == 1;


        assert decoded.list.size() == 2;
        assert decoded.list.get(0).get("k1") == 2;

        assert decoded.list.get(0).get("k2") == 4;
        assert decoded.list.get(0).size() == 2;

        assert decoded.list.get(1).get("k3") == 21;
        assert decoded.list.get(1).get("k4") == 43;
        assert decoded.list.get(1).size() == 2;

        assert decoded.level != null;
        assert uuid.equals(decoded.level.uuid);
    }
}
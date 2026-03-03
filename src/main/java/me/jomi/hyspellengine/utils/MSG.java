package me.jomi.hyspellengine.utils;


import com.hypixel.hytale.server.core.Message;

import java.util.HashMap;
import java.util.Map;

public class MSG extends Exception {
    public final String location;
    final Map<String, String> params = new HashMap<>();
    public MSG() {
        this(null);
    }
    public MSG(String location) {
        this.location = location;
    }

    public MSG param(String name, String value) {
        params.put(name, value);
        return this;
    }
    public MSG p(String name, String value){
        return this.param(name, value);
    }
    public MSG add(String name, String value) {
        return this.param(name, value);
    }

    Message applyParams(Message msg) {
        this.params.forEach(msg::param);
        return msg;
    }
}

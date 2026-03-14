package me.jomi.hyspellengine.api.events;

import com.hypixel.hytale.component.system.CancellableEcsEvent;
import me.jomi.hyspellengine.api.Experience;

public class ExperienceChangeEvent extends CancellableEcsEvent {
    public static enum Method {
        ADD,
        SET;
    }
    private Experience experience;
    private double exp;
    private final Method method;

    private final double originExp;

    public ExperienceChangeEvent(Experience experience, double exp, Method method) {
        this.experience = experience;
        this.method = method;
        this.originExp = exp;
        this.exp = exp;
    }

    public Method getMethod() {
        return method;
    }

    public Experience getExperience() {
        return experience;
    }

    public void setExp(double exp) {
        this.exp = exp;
    }
    public double getExp() {
        return exp;
    }

    public double getOriginExp() {
        return originExp;
    }
}

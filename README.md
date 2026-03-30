# HySpellEngine
Hytale rpg mod with api. Contains category, experience and spells systems.


# How to make custom Spells
- Add dependency to HySpellEngine
- Make your CustomSpell extends me.jomi.hyspellengine.api.Spell
- (optionally) Make new me.jomi.hyspellengine.api.Experience or extends it
- register your Spells and Experiences in Plugin setup using Spell.getSpellRegistry() and Experience.getRegistry()
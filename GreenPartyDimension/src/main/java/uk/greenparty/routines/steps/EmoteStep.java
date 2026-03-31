package uk.greenparty.routines.steps;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import uk.greenparty.routines.NpcRoutineState;
import uk.greenparty.routines.RoutineManager;
import uk.greenparty.routines.RoutineStep;

/**
 * EmoteStep — Play a particle effect and/or sound at an NPC's location.
 *
 * Emote types map to curated particle + sound combinations that convey
 * an NPC's emotional state. Pairs well with SayStep for dramatic effect.
 *
 * Available emotes:
 *   CELEBRATE     — firework burst + happy villager effect + level-up sound
 *   DISTRESS      — smoke particles + villager scold sound
 *   THINKING      — thought bubbles (note particles) + low hum
 *   ANGRY         — thundercloud effect + ghast sound
 *   APPLAUD       — hearts + crowd-cheer (note block)
 *   WHISPER       — small enchant particles + quiet note
 *   GASP          — white flash + inhale sound
 *
 * YAML:
 *   - type: EMOTE
 *     npc: "Councillor Wheatgrass"
 *     emote: CELEBRATE
 */
public class EmoteStep implements RoutineStep {

    public enum EmoteType {
        CELEBRATE,
        DISTRESS,
        THINKING,
        ANGRY,
        APPLAUD,
        WHISPER,
        GASP
    }

    private final String targetNpcName; // null = self
    private final EmoteType emote;

    public EmoteStep(String targetNpcName, EmoteType emote) {
        this.targetNpcName = targetNpcName;
        this.emote = emote;
    }

    @Override
    public StepType getType() { return StepType.EMOTE; }

    @Override
    public int execute(Entity npc, NpcRoutineState state, RoutineManager manager) {
        Entity target = (targetNpcName != null) ? manager.findNpc(targetNpcName) : npc;
        if (target == null) return 0;

        Location loc = target.getLocation().add(0, 1.5, 0); // above head

        switch (emote) {
            case CELEBRATE -> {
                loc.getWorld().spawnParticle(Particle.FIREWORK, loc, 20, 0.3, 0.3, 0.3, 0.1);
                loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 10, 0.2, 0.2, 0.2, 0);
                loc.getWorld().playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5f, 1.2f);
            }
            case DISTRESS -> {
                loc.getWorld().spawnParticle(Particle.SMOKE, loc, 15, 0.2, 0.2, 0.2, 0.02);
                loc.getWorld().playSound(loc, Sound.ENTITY_VILLAGER_NO, 0.8f, 0.8f);
            }
            case THINKING -> {
                loc.getWorld().spawnParticle(Particle.NOTE, loc, 5, 0.3, 0.1, 0.3, 1.0);
                loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.3f, 0.6f);
            }
            case ANGRY -> {
                target.playEffect(org.bukkit.EntityEffect.VILLAGER_ANGRY);
                loc.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, loc, 6, 0.2, 0.2, 0.2, 0);
                loc.getWorld().playSound(loc, Sound.ENTITY_GHAST_WARN, 0.3f, 1.8f);
            }
            case APPLAUD -> {
                loc.getWorld().spawnParticle(Particle.HEART, loc, 8, 0.3, 0.2, 0.3, 0);
                loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_BELL, 0.6f, 1.0f);
            }
            case WHISPER -> {
                loc.getWorld().spawnParticle(Particle.ENCHANT, loc, 8, 0.2, 0.2, 0.2, 0.5);
                loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, 0.2f, 1.5f);
            }
            case GASP -> {
                loc.getWorld().spawnParticle(Particle.CLOUD, loc, 12, 0.2, 0.2, 0.2, 0.05);
                loc.getWorld().playSound(loc, Sound.ENTITY_VILLAGER_HURT, 0.8f, 1.4f);
            }
        }

        return 0; // Particle effects are instant
    }

    @Override
    public String describe() {
        String who = (targetNpcName != null) ? targetNpcName : "self";
        return String.format("Emote[%s, %s]", who, emote);
    }
}

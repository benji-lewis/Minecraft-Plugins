package uk.greenparty.routines.steps;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import uk.greenparty.routines.NpcRoutineState;
import uk.greenparty.routines.RoutineManager;
import uk.greenparty.routines.RoutineStep;

/**
 * AnimateStep — Trigger a named animation on an NPC.
 *
 * Paper 1.21.1 exposes EntityAnimation for living entities. Available types:
 *   ARM_SWING    — main hand swing (the "speaking" gesture)
 *   HURT         — damage flash (red tint)
 *   CRITICAL_HIT — crit particles (anger/emphasis)
 *   MAGIC_CRITICAL_HIT — enchantment particles (celebratory)
 *
 * Note: Villagers don't have complex custom animations in vanilla, so we use
 * these built-in entity animations as expressive shorthand. For richer effects,
 * chain an EmoteStep immediately after.
 *
 * YAML:
 *   - type: ANIMATE
 *     npc: "Councillor Wheatgrass"
 *     animation: ARM_SWING
 */
public class AnimateStep implements RoutineStep {

    public enum AnimationType {
        ARM_SWING,
        HURT,
        CRITICAL_HIT,
        MAGIC_CRITICAL_HIT
    }

    private final String targetNpcName; // null = self
    private final AnimationType animation;

    public AnimateStep(String targetNpcName, AnimationType animation) {
        this.targetNpcName = targetNpcName;
        this.animation = animation;
    }

    @Override
    public StepType getType() { return StepType.ANIMATE; }

    @Override
    public int execute(Entity npc, NpcRoutineState state, RoutineManager manager) {
        Entity target = (targetNpcName != null) ? manager.findNpc(targetNpcName) : npc;
        if (!(target instanceof LivingEntity le)) return 0;

        switch (animation) {
            case ARM_SWING -> manager.swingArm(le);
            case HURT -> le.playEffect(org.bukkit.EntityEffect.HURT);
            case CRITICAL_HIT -> le.playEffect(org.bukkit.EntityEffect.VILLAGER_HEART); // ~happy
            case MAGIC_CRITICAL_HIT -> le.playEffect(org.bukkit.EntityEffect.VILLAGER_ANGRY); // ~angry
        }

        return 0; // Animations are instant
    }

    @Override
    public String describe() {
        String who = (targetNpcName != null) ? targetNpcName : "self";
        return String.format("Animate[%s, %s]", who, animation);
    }
}

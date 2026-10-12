package com.darwinreborn.entity.goal;

import com.darwinreborn.entity.GeneticCreature;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.world.entity.ai.goal.Goal;

/** Herd animals drift towards the nearest herd mate when they have wandered too far apart. */
public class HerdGoal extends Goal {

    private static final double SEARCH_RADIUS = 16.0;
    private static final double TOO_FAR_SQR = 36.0;     // 6 blocks
    private static final double CLOSE_ENOUGH_SQR = 9.0; // 3 blocks

    private final GeneticCreature mob;
    private GeneticCreature mate;
    private int recheck;
    private int cooldown;

    public HerdGoal(GeneticCreature mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!mob.getPhenotype().herdAnimal() || mob.isBaby()) return false;
        // The scan is an area query, so only do it a few times per second per creature.
        if (cooldown-- > 0) return false;
        cooldown = 30 + mob.getRandom().nextInt(30);

        List<GeneticCreature> mates = mob.level().getEntitiesOfClass(
                GeneticCreature.class,
                mob.getBoundingBox().inflate(SEARCH_RADIUS),
                c -> c != mob && c.isAlive() && c.getPhenotype().herdAnimal());
        GeneticCreature nearest = null;
        double best = Double.MAX_VALUE;
        for (GeneticCreature c : mates) {
            double d = mob.distanceToSqr(c);
            if (d < best) {
                best = d;
                nearest = c;
            }
        }
        if (nearest == null || best < TOO_FAR_SQR) return false;
        this.mate = nearest;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return mate != null && mate.isAlive() && mob.distanceToSqr(mate) > CLOSE_ENOUGH_SQR;
    }

    @Override
    public void start() {
        recheck = 0;
    }

    @Override
    public void tick() {
        if (--recheck <= 0) {
            recheck = 10;
            mob.getNavigation().moveTo(mate, 1.0);
        }
    }

    @Override
    public void stop() {
        mate = null;
        mob.getNavigation().stop();
    }
}

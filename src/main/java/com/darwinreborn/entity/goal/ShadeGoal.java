package com.darwinreborn.entity.goal;

import com.darwinreborn.entity.GeneticCreature;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;

/**
 * Creatures with the "shade" gene retreat from open sunlight to any nearby spot without sky exposure
 * (under trees, overhangs, caves). Unlike vanilla's FleeSunGoal this works even when the creature is not burning.
 */
public class ShadeGoal extends Goal {

    private final GeneticCreature mob;
    private double x;
    private double y;
    private double z;
    private int cooldown;

    public ShadeGoal(GeneticCreature mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!mob.getPhenotype().seeksShade() || mob.getTarget() != null) return false;
        if (cooldown-- > 0) return false;
        cooldown = 20 + mob.getRandom().nextInt(20);

        Level level = mob.level();
        BlockPos here = mob.blockPosition();
        if (!level.canSeeSky(here) || level.getMaxLocalRawBrightness(here) < 12) return false;

        for (int i = 0; i < 12; i++) {
            BlockPos p = here.offset(
                    mob.getRandom().nextInt(17) - 8,
                    mob.getRandom().nextInt(7) - 3,
                    mob.getRandom().nextInt(17) - 8);
            if (!level.canSeeSky(p) && !level.getBlockState(p.below()).isAir() && level.getBlockState(p).isAir()) {
                this.x = p.getX() + 0.5;
                this.y = p.getY();
                this.z = p.getZ() + 0.5;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return !mob.getNavigation().isDone() && mob.getTarget() == null;
    }

    @Override
    public void start() {
        mob.getNavigation().moveTo(x, y, z, 1.2);
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }
}

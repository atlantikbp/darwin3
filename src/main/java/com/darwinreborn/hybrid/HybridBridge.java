package com.darwinreborn.hybrid;

import com.darwinreborn.entity.GeneticCreature;
import com.darwinreborn.genetics.Archetype;
import com.darwinreborn.genetics.Breeding;
import com.darwinreborn.genetics.BreedingConfig;
import com.darwinreborn.genetics.Genome;
import com.darwinreborn.genetics.Phenotype;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;

/**
 * Lets vanilla farm animals interbreed with each other and with the creatures of the mod.
 *
 * <p>A vanilla cow, sheep, pig, chicken or rabbit has no stored genome. Instead it gets a <em>virtual</em> one: its
 * species archetype, with carriers and small variations seeded from the animal's UUID, so the same animal always has
 * the same genes. When two animals of different kinds are both "in love" (fed their own breeding food) and stand next
 * to each other, they produce a hybrid: a creature of the mod whose genome is a real cross of both parents.</p>
 *
 * <p>Animals of the same kind still breed the vanilla way; hybrids only happen when no same-kind lover is close.</p>
 */
public final class HybridBridge {

    private static final Map<String, Archetype> SPECIES = new HashMap<>();

    private static final double MEET_DISTANCE_SQR = 2.5 * 2.5;
    private static final double SEEK_DISTANCE = 14.0;
    private static final double SAME_KIND_DISTANCE_SQR = 8.0 * 8.0;

    public static void register() {
        SPECIES.put("cow", Archetype.BOVINE);
        SPECIES.put("sheep", Archetype.WOOLLY);
        SPECIES.put("pig", Archetype.PIGLET);
        SPECIES.put("chicken", Archetype.FOWL);
        SPECIES.put("rabbit", Archetype.HARE);
        SPECIES.put("goat", Archetype.GOAT);
        SPECIES.put("horse", Archetype.HORSE);
        SPECIES.put("donkey", Archetype.HORSE);
        SPECIES.put("wolf", Archetype.HOUND);
        SPECIES.put("cat", Archetype.FELINE);
        SPECIES.put("fox", Archetype.VULPINE);
        ServerTickEvents.END_LEVEL_TICK.register(HybridBridge::tick);
    }

    /** Registry path of an entity's type ("cow", "sheep"...). Matching by name avoids depending on how Minecraft names its constants. */
    private static String typeName(LivingEntity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
    }

    /** The real genome of a creature, the virtual genome of a supported vanilla animal, or null for anything else. */
    public static Genome genomeOf(LivingEntity entity) {
        if (entity instanceof GeneticCreature creature) return creature.getGenome();
        Archetype archetype = SPECIES.get(typeName(entity));
        if (archetype == null) return null;
        UUID id = entity.getUUID();
        Random rnd = new Random(id.getMostSignificantBits() * 31L + id.getLeastSignificantBits());
        return archetype.wild(rnd, 0.04);
    }

    public static boolean isVanillaHybridParent(LivingEntity entity) {
        return !(entity instanceof GeneticCreature) && SPECIES.containsKey(typeName(entity));
    }

    /** Two creatures of the mod share a kind (their own breeding goal handles them); vanilla animals are told apart by type. */
    private static Object kindOf(Animal a) {
        return a instanceof GeneticCreature ? GeneticCreature.class : a.getType();
    }

    private static boolean eligible(Animal a) {
        return a.isAlive() && !a.isBaby() && a.isInLove() && a.getAge() == 0 && genomeOf(a) != null;
    }

    // ------------------------------------------------------------------ the matchmaker

    private static void tick(ServerLevel level) {
        if (level.getGameTime() % 10 != 0) return;
        Set<UUID> handled = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            List<Animal> lovers = level.getEntitiesOfClass(Animal.class, player.getBoundingBox().inflate(24.0), HybridBridge::eligible);
            if (lovers.size() < 2) continue;
            for (Animal a : lovers) {
                if (handled.contains(a.getUUID())) continue;
                if (hasSameKindLoverNearby(a, lovers)) continue;     // let the vanilla goal pair them
                Animal partner = nearestOtherKind(a, lovers, handled);
                if (partner == null) continue;
                if (a.distanceToSqr(partner) <= MEET_DISTANCE_SQR) {
                    handled.add(a.getUUID());
                    handled.add(partner.getUUID());
                    mate(level, a, partner);
                } else {
                    a.getNavigation().moveTo(partner, 1.1);          // lovers walk towards each other
                    partner.getNavigation().moveTo(a, 1.1);
                }
            }
        }
    }

    private static boolean hasSameKindLoverNearby(Animal a, List<Animal> lovers) {
        Object kind = kindOf(a);
        for (Animal other : lovers) {
            if (other != a && kindOf(other).equals(kind) && a.distanceToSqr(other) <= SAME_KIND_DISTANCE_SQR) return true;
        }
        return false;
    }

    private static Animal nearestOtherKind(Animal a, List<Animal> lovers, Set<UUID> handled) {
        Object kind = kindOf(a);
        Animal best = null;
        double bestDist = SEEK_DISTANCE * SEEK_DISTANCE;
        for (Animal other : lovers) {
            if (other == a || handled.contains(other.getUUID()) || kindOf(other).equals(kind)) continue;
            double d = a.distanceToSqr(other);
            if (d < bestDist) {
                bestDist = d;
                best = other;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ the mating itself

    private static void mate(ServerLevel level, Animal a, Animal b) {
        Genome ga = genomeOf(a);
        Genome gb = genomeOf(b);
        Phenotype pa = Phenotype.of(ga);
        Phenotype pb = Phenotype.of(gb);
        Random rnd = new Random(level.getRandom().nextLong());

        // A cross between two different kinds is a little harder to bring off than an ordinary mating.
        double chance = Math.min(1.0, ((pa.fertility() + pb.fertility()) / 2.0) * 0.8 + 0.25);
        int born = 0;
        int mutations = 0;
        BreedingConfig cfg = Breeding.configFor(ga, gb);   // very different parents mutate more
        if (rnd.nextDouble() < chance) {
            int litter = Math.min(2, Breeding.litterSize(pa, pb, rnd));
            for (int i = 0; i < litter; i++) {
                Genome child = Breeding.cross(ga, gb, rnd, cfg);
                if (!Phenotype.of(child).viable()) continue;          // homozygous lethal: embryo lost
                if (GeneticCreature.spawnBaby(level, child, a.getX(), a.getY(), a.getZ()) != null) {
                    born++;
                    mutations += Breeding.novelTraits(child, ga, gb);
                }
            }
        }

        a.setAge(6000);
        b.setAge(6000);
        a.resetLove();
        b.resetLove();
        double x = (a.getX() + b.getX()) / 2.0, y = (a.getY() + b.getY()) / 2.0 + 0.8, z = (a.getZ() + b.getZ()) / 2.0;
        level.sendParticles(born > 0 ? ParticleTypes.HEART : ParticleTypes.SMOKE, x, y, z, 7, 0.5, 0.4, 0.5, 0.02);

        Component message = born > 0
                ? Component.translatable("darwinreborn.hybrid.born", a.getType().getDescription(), b.getType().getDescription(), born)
                : Component.translatable("darwinreborn.hybrid.failed", a.getType().getDescription(), b.getType().getDescription());
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(x, y, z) < 32.0 * 32.0) player.sendSystemMessage(message);
        }
        announceMutations(level, x, y, z, mutations);
    }

    /** Tells players nearby that a newborn shows traits neither parent had. */
    public static void announceMutations(ServerLevel level, double x, double y, double z, int count) {
        if (count <= 0) return;
        level.sendParticles(ParticleTypes.ENCHANT, x, y + 0.5, z, 20, 0.6, 0.6, 0.6, 0.3);
        Component text = Component.translatable("darwinreborn.hybrid.mutation", count);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(x, y, z) < 32.0 * 32.0) player.sendSystemMessage(text);
        }
    }

    private HybridBridge() {}
}

package com.darwinreborn.entity;

import com.darwinreborn.entity.goal.HerdGoal;
import com.darwinreborn.entity.goal.ShadeGoal;
import com.darwinreborn.genetics.Archetype;
import com.darwinreborn.genetics.Breeding;
import com.darwinreborn.genetics.BreedingConfig;
import com.darwinreborn.genetics.Genome;
import com.darwinreborn.genetics.Phenotype;
import com.darwinreborn.hybrid.HybridBridge;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.util.List;
import java.util.Set;

/**
 * One creature class for every species: the genome decides how it looks and behaves.
 *
 * <p>The genome lives on the server and is mirrored to clients through synched entity data
 * (as a hex string), so renderers can build the model from {@link #getPhenotype()}.</p>
 */
public class GeneticCreature extends Animal {

    private static final EntityDataAccessor<String> DATA_GENOME =
            SynchedEntityData.defineId(GeneticCreature.class, EntityDataSerializers.STRING);

    /** Vanilla animals that hunters consider prey. Matched by registry path so no class imports are needed. */
    private static final Set<String> LIVESTOCK = Set.of("cow", "sheep", "pig", "chicken", "rabbit");

    private static final ItemLike[] DROPS = {
            Items.FEATHER, Items.PORKCHOP, Items.MUTTON, Items.LEATHER,
            Items.BONE, Items.STRING, Items.SLIME_BALL, Items.BLAZE_POWDER,
            Items.AMETHYST_SHARD, Items.COAL, Items.IRON_NUGGET, Items.GOLD_NUGGET,
            Items.EGG, Items.INK_SAC, Items.GLOWSTONE_DUST, Items.EMERALD
    };

    private Genome genome = Genome.blank();
    private Phenotype phenotype = Phenotype.of(Genome.blank());
    /** True once the species has been chosen (from biome) or inherited (from parents). */
    private boolean initialized;
    private int huntCooldown;
    /** Age in ticks; compared with the longevity genes. */
    private int lifeTicks;

    public GeneticCreature(EntityType<? extends GeneticCreature> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.TEMPT_RANGE, 10.0);
    }

    // ------------------------------------------------------------------ genetics

    public Genome getGenome() { return genome; }

    public Phenotype getPhenotype() { return phenotype; }

    /** Replaces the genome and applies its effects. On the server this also syncs to clients. */
    public void setGenome(Genome g, boolean heal) {
        this.genome = g;
        this.phenotype = Phenotype.of(g);
        if (!level().isClientSide()) {
            this.entityData.set(DATA_GENOME, g.toHex());
            applyPhenotype(heal);
        }
    }

    private void applyPhenotype(boolean heal) {
        setAttribute(Attributes.MAX_HEALTH, phenotype.maxHealth());
        setAttribute(Attributes.MOVEMENT_SPEED, phenotype.speed());
        setAttribute(Attributes.SCALE, phenotype.size());
        if (heal || getHealth() > getMaxHealth()) setHealth(getMaxHealth());
        refreshDimensions();
    }

    private void setAttribute(Holder<Attribute> attribute, double value) {
        AttributeInstance inst = getAttribute(attribute);
        if (inst != null) inst.setBaseValue(value);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_GENOME, "");
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        super.onSyncedDataUpdated(data);
        if (data == DATA_GENOME && level().isClientSide()) {
            this.genome = Genome.fromHex(this.entityData.get(DATA_GENOME));
            this.phenotype = Phenotype.of(this.genome);
        }
    }

    private void assignSpeciesFromBiome(ServerLevel level) {
        Random rnd = new Random(this.random.nextLong());
        Archetype species = Species.forBiome(level.getBiome(blockPosition()), rnd);
        setGenome(species.wild(rnd, 0.05), true);
        lifeTicks = rnd.nextInt(Math.max(1, phenotype.lifespanTicks() / 2));
        initialized = true;
    }

    // ------------------------------------------------------------------ breeding

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        if (!(partner instanceof GeneticCreature other)) return null;
        Random rnd = new Random(this.random.nextLong());

        // Less fertile pairs sometimes fail to conceive.
        double fertility = (this.phenotype.fertility() + other.phenotype.fertility()) / 2.0;
        if (rnd.nextDouble() > Math.min(1.0, fertility + 0.3)) {
            failConception(other);
            return null;
        }

        int litter = Breeding.litterSize(this.phenotype, other.phenotype, rnd);
        BreedingConfig cfg = Breeding.configFor(this.genome, other.genome);   // distant parents mutate more
        int mutations = 0;
        GeneticCreature first = null;
        for (int i = 0; i < litter; i++) {
            Genome childGenome = Breeding.cross(this.genome, other.genome, rnd, cfg);
            if (!Phenotype.of(childGenome).viable()) continue;   // homozygous lethal: embryo lost
            GeneticCreature child = ModEntities.CREATURE.create(level, EntitySpawnReason.BREEDING);
            if (child == null) continue;
            child.setBaby(true);
            child.initialized = true;
            child.setPos(getX(), getY(), getZ());
            child.setGenome(childGenome, true);
            mutations += Breeding.novelTraits(childGenome, this.genome, other.genome);
            if (first == null) {
                first = child;               // the game spawns this one itself
            } else {
                level.addFreshEntity(child); // extra littermates
            }
        }
        if (first == null) failConception(other);   // every embryo was lost
        HybridBridge.announceMutations(level, getX(), getY(), getZ(), mutations);
        return first;
    }

    /** Spawns a baby with a ready-made genome (used for hybrids of vanilla animals). Returns null if creation fails. */
    public static GeneticCreature spawnBaby(ServerLevel level, Genome genome, double x, double y, double z) {
        GeneticCreature child = ModEntities.CREATURE.create(level, EntitySpawnReason.BREEDING);
        if (child == null) return null;
        child.setBaby(true);
        child.initialized = true;
        child.setPos(x, y, z);
        child.setGenome(genome, true);
        level.addFreshEntity(child);
        return child;
    }

    /**
     * Vanilla only puts the parents on cooldown when a baby is produced. If nothing is born, both would stay
     * "in love" and retry every tick, which would make lethal genes and infertility meaningless.
     */
    private void failConception(GeneticCreature other) {
        this.setAge(6000);
        other.setAge(6000);
        this.resetLove();
        other.resetLove();
    }

    /** Natural spawns only happen on the surface (no caves, no mid-air). */
    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        if (reason == EntitySpawnReason.NATURAL) {
            BlockPos pos = blockPosition();
            if (!level.canSeeSky(pos) || level.getBlockState(pos.below()).isAir()) return false;
        }
        return super.checkSpawnRules(level, reason);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.WHEAT);
    }

    // ------------------------------------------------------------------ behaviour

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.4) {
            @Override
            public boolean canUse() {
                return phenotype.fleesWhenHit() && super.canUse();
            }
        });
        this.goalSelector.addGoal(2, new ShadeGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, true) {
            @Override
            public boolean canUse() {
                return phenotype.hunter() != Phenotype.HunterType.PASSIVE && super.canUse();
            }
        });
        this.goalSelector.addGoal(4, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(5, new TemptGoal(this, 1.1, Ingredient.of(Items.WHEAT), false));
        this.goalSelector.addGoal(6, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(7, new HerdGoal(this));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide() || !(level() instanceof ServerLevel server)) return;

        if (!initialized) assignSpeciesFromBiome(server);

        lifeTicks++;
        if (lifeTicks > phenotype.lifespanTicks() && tickCount % 100 == 0) {
            hurtServer(server, damageSources().generic(), 2.0F);   // old age
        }

        if (phenotype.burnsInSun() && (level().canSeeSky(blockPosition()) && level().getMaxLocalRawBrightness(blockPosition()) >= 12 && !isInWaterOrRain())) {
            igniteForSeconds(8.0F);
        }
        if (phenotype.hurtByWater() && isInWaterOrRain() && tickCount % 20 == 0) {
            hurtServer(server, damageSources().drown(), 1.0F);
        }
        if (phenotype.glows() && tickCount % 20 == 0) {
            addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));
        }
        if (tickCount % 40 == 0) {
            // nocturnal creatures are lively at night and sluggish by day
            boolean bright = level().getMaxLocalRawBrightness(blockPosition()) >= 9;
            double factor = phenotype.nocturnal() ? (bright ? 0.6 : 1.15) : 1.0;
            setAttribute(Attributes.MOVEMENT_SPEED, phenotype.speed() * factor);
        }
        if (huntCooldown > 0) {
            huntCooldown--;
        } else if (phenotype.hunter() != Phenotype.HunterType.PASSIVE && getTarget() == null && !isBaby()) {
            huntCooldown = 40 + random.nextInt(40);
            LivingEntity prey = findPrey(server);
            if (prey != null) setTarget(prey);
        }
    }

    private LivingEntity findPrey(ServerLevel server) {
        List<Animal> candidates = server.getEntitiesOfClass(Animal.class, getBoundingBox().inflate(12.0), this::isPrey);
        Animal best = null;
        double bestDist = Double.MAX_VALUE;
        for (Animal a : candidates) {
            double d = distanceToSqr(a);
            if (d < bestDist) {
                bestDist = d;
                best = a;
            }
        }
        return best;
    }

    private boolean isPrey(Animal other) {
        if (other == this || !other.isAlive()) return false;
        if (other instanceof GeneticCreature creature) {
            return phenotype.hunter() == Phenotype.HunterType.PREDATOR
                    && creature.phenotype.size() < this.phenotype.size() * 0.85;
        }
        String path = BuiltInRegistries.ENTITY_TYPE.getKey(other.getType()).getPath();
        return LIVESTOCK.contains(path);
    }

    @Override
    public boolean fireImmune() {
        return phenotype.fireImmune() || super.fireImmune();
    }

    @Override
    public boolean canFreeze() {
        return !phenotype.coldResistant() && super.canFreeze();
    }

    // ------------------------------------------------------------------ drops

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel server && !isBaby() && random.nextFloat() < 0.8F) {
            int count = 1 + random.nextInt(2);
            ItemStack stack = new ItemStack(DROPS[Math.floorMod(phenotype.dropIndex(), DROPS.length)], count);
            server.addFreshEntity(new ItemEntity(server, getX(), getY(), getZ(), stack));
        }
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("Genome", genome.toHex());
        output.putInt("SpeciesAssigned", initialized ? 1 : 0);
        output.putInt("LifeTicks", lifeTicks);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.getString("Genome").ifPresent(hex -> setGenome(Genome.fromHex(hex), false));
        this.initialized = input.getInt("SpeciesAssigned").orElse(0) == 1;
        this.lifeTicks = input.getInt("LifeTicks").orElse(0);
    }
}

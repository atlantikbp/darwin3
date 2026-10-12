package com.darwinreborn.item;

import com.darwinreborn.genetics.Genome;
import com.darwinreborn.genetics.Phenotype;
import com.darwinreborn.hybrid.HybridBridge;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Right-click a creature to read its genome (what it shows and what it secretly carries).
 * Right-click a second creature and the lens also forecasts the offspring of the pair.
 */
public class GeneticLensItem extends Item {

    /** Last inspected genome per player (in memory only; cleared on server restart). */
    private static final Map<UUID, Genome> LAST_INSPECTED = new ConcurrentHashMap<>();

    public GeneticLensItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        Genome genome = HybridBridge.genomeOf(target);   // real genome of a creature, virtual one of a vanilla farm animal
        if (genome == null) return InteractionResult.PASS;
        if (player.level().isClientSide()) return InteractionResult.SUCCESS;

        if (HybridBridge.isVanillaHybridParent(target)) {
            player.sendSystemMessage(Component.translatable("darwinreborn.lens.vanilla").withStyle(ChatFormatting.DARK_AQUA));
        }
        for (Component line : LensText.report(genome, Phenotype.of(genome))) {
            player.sendSystemMessage(line);
        }

        Genome previous = LAST_INSPECTED.put(player.getUUID(), genome);
        if (previous != null && !previous.equals(genome)) {
            for (Component line : LensText.forecast(previous, genome)) {
                player.sendSystemMessage(line);
            }
        }
        return InteractionResult.SUCCESS;
    }
}

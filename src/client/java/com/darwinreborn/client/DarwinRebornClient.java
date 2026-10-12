package com.darwinreborn.client;

import com.darwinreborn.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.renderer.entity.EntityRenderers;

public final class DarwinRebornClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModelLayerRegistry.registerModelLayer(CreatureModel.LAYER, CreatureModel::createLayer);
        EntityRenderers.register(ModEntities.CREATURE, CreatureRenderer::new);
    }
}

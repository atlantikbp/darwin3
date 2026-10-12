package com.darwinreborn.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Everything the model needs to draw one creature, copied from its phenotype each frame. */
public class CreatureRenderState extends LivingEntityRenderState {
    public int body;
    public int head;
    public int snout;
    public int ears;
    public int tail;
    public int rearLegs;
    public int frontLegs;
    public float legLength;   // 0..1
    public float tiltDegrees; // 0..90
    public int color;         // 0xRRGGBB
    public int pattern;       // 0..11
    public int pattern2;      // 0..11, 0 = none (second pattern, from the other strand)
    public int markColor;     // 0xRRGGBB
    public int eyeColor;      // 0xRRGGBB
    public int eyeShape;      // 0..3
    public int crest;         // 0..3
    public int coat;          // 0 smooth, 1 fur, 2 scales, 3 fluffy
    public int blinkOffset;   // per-animal phase so that not everybody blinks together
}

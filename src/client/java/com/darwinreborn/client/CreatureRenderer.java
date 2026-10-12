package com.darwinreborn.client;

import com.darwinreborn.DarwinReborn;
import com.darwinreborn.entity.GeneticCreature;
import com.darwinreborn.genetics.Phenotype;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Draws a creature from its phenotype in several passes over the same model. The passes are drawn in a fixed order:
 * <ol>
 *   <li>the coat (smooth, fur, scales or fluffy), grey, tinted by the primary colour,</li>
 *   <li>the markings (stripes, spots, crest), tinted by the second colour,</li>
 *   <li>the eyes: white of the eye, iris and pupil (iris colour), a white shine; while the animal blinks these are
 *       replaced by a closed eyelid in a darker shade of the coat.</li>
 * </ol>
 * All textures share one atlas layout (see tools/make_assets.py).
 */
public class CreatureRenderer extends MobRenderer<GeneticCreature, CreatureRenderState, CreatureModel> {

    private static final int COATS = 4;
    private static final int PATTERNS = 12;
    private static final int EYE_SHAPES = 4;
    private static final Identifier[] BASES = new Identifier[COATS];
    private static final Identifier[] MARKS = new Identifier[PATTERNS];
    private static final Identifier[] EYES_WHITE = new Identifier[EYE_SHAPES];
    private static final Identifier[] EYES_IRIS = new Identifier[EYE_SHAPES];
    private static final Identifier[] EYES_SHINE = new Identifier[EYE_SHAPES];
    private static final Identifier[] EYES_CLOSED = new Identifier[EYE_SHAPES];

    static {
        for (int i = 0; i < COATS; i++) BASES[i] = DarwinReborn.id("textures/entity/creature_base_" + i + ".png");
        for (int i = 0; i < PATTERNS; i++) MARKS[i] = DarwinReborn.id("textures/entity/mark_" + i + ".png");
        for (int i = 0; i < EYE_SHAPES; i++) {
            EYES_WHITE[i] = DarwinReborn.id("textures/entity/eyes_white_" + i + ".png");
            EYES_IRIS[i] = DarwinReborn.id("textures/entity/eyes_iris_" + i + ".png");
            EYES_SHINE[i] = DarwinReborn.id("textures/entity/eyes_shine_" + i + ".png");
            EYES_CLOSED[i] = DarwinReborn.id("textures/entity/eyes_closed_" + i + ".png");
        }
    }

    public CreatureRenderer(EntityRendererProvider.Context context) {
        super(context, new CreatureModel(context.bakeLayer(CreatureModel.LAYER)), 0.4F);
        addLayer(new Pass(this, context, Pass.Kind.MARKS));
        addLayer(new Pass(this, context, Pass.Kind.MARKS2));
        addLayer(new Pass(this, context, Pass.Kind.EYE_WHITE));
        addLayer(new Pass(this, context, Pass.Kind.EYE_IRIS));
        addLayer(new Pass(this, context, Pass.Kind.EYE_SHINE));
        addLayer(new Pass(this, context, Pass.Kind.EYELID));
    }

    @Override
    public CreatureRenderState createRenderState() {
        return new CreatureRenderState();
    }

    @Override
    public void extractRenderState(GeneticCreature entity, CreatureRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        Phenotype p = entity.getPhenotype();
        state.body = p.body();
        state.head = p.head();
        state.snout = p.snout();
        state.ears = p.ears();
        state.tail = p.tail();
        state.rearLegs = p.rearLegs();
        state.frontLegs = p.frontLegs();
        state.legLength = (float) p.legLength();
        state.tiltDegrees = (float) p.tiltDegrees();
        state.color = p.color();
        state.pattern = p.pattern();
        state.pattern2 = p.pattern2();
        state.markColor = p.markColor();
        state.eyeColor = p.eyeColor();
        state.eyeShape = p.eyeShape();
        state.crest = p.crest();
        state.coat = p.coat();
        state.blinkOffset = Math.floorMod(entity.getGenome().hashCode(), 1000);
    }

    // No @Override on purpose: if a Minecraft update renames this hook the mod still compiles
    // (creatures just stay grey until the name is updated).
    protected int getModelTint(CreatureRenderState state) {
        return 0xFF000000 | state.color;
    }

    @Override
    public Identifier getTextureLocation(CreatureRenderState state) {
        return BASES[clamp(state.coat, COATS)];
    }

    private static int clamp(int value, int count) {
        return Math.max(0, Math.min(count - 1, value));
    }

    /** True for a few ticks every few seconds; each animal has its own rhythm. */
    private static boolean isBlinking(CreatureRenderState state) {
        int t = (int) state.ageInTicks + state.blinkOffset;
        return t % 90 < 4 || (t % 310 >= 150 && t % 310 < 154);
    }

    /** A shade that stands out against the given colour: lighter if it is dark, darker if it is light. */
    private static int contrast(int rgb) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        double luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0;
        if (luminance > 0.5) return darken(rgb, 0.4);
        return 0xFF000000 | (Math.min(255, r + (255 - r) * 6 / 10) << 16) | (Math.min(255, g + (255 - g) * 6 / 10) << 8)
                | Math.min(255, b + (255 - b) * 6 / 10);
    }

    private static int darken(int rgb, double factor) {
        int r = (int) (((rgb >> 16) & 255) * factor);
        int g = (int) (((rgb >> 8) & 255) * factor);
        int b = (int) ((rgb & 255) * factor);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** One extra pass over the model with its own texture and tint. */
    private static final class Pass extends RenderLayer<CreatureRenderState, CreatureModel> {

        enum Kind { MARKS, MARKS2, EYE_WHITE, EYE_IRIS, EYE_SHINE, EYELID }

        private final Kind kind;
        private final CreatureModel model;

        Pass(CreatureRenderer renderer, EntityRendererProvider.Context context, Kind kind) {
            super(renderer);
            this.kind = kind;
            this.model = new CreatureModel(context.bakeLayer(CreatureModel.LAYER));
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                           CreatureRenderState state, float netHeadYaw, float headPitch) {
            if (kind == Kind.MARKS2 && state.pattern2 == 0) return;
            boolean blinking = isBlinking(state);
            if (kind == Kind.EYELID ? !blinking : (kind != Kind.MARKS && kind != Kind.MARKS2 && blinking)) return;

            Identifier texture;
            int color;
            int shape = clamp(state.eyeShape, EYE_SHAPES);
            switch (kind) {
                case MARKS -> {
                    texture = MARKS[clamp(state.pattern, PATTERNS)];
                    color = 0xFF000000 | state.markColor;
                }
                case MARKS2 -> {
                    // the pattern of the other parent, in a contrasting shade of the marking colour
                    texture = MARKS[clamp(state.pattern2, PATTERNS)];
                    color = contrast(state.markColor);
                }
                case EYE_WHITE -> {
                    texture = EYES_WHITE[shape];
                    color = 0xFFF4F4F0;
                }
                case EYE_IRIS -> {
                    texture = EYES_IRIS[shape];
                    color = 0xFF000000 | state.eyeColor;
                }
                case EYE_SHINE -> {
                    texture = EYES_SHINE[shape];
                    color = 0xFFFFFFFF;
                }
                default -> {
                    texture = EYES_CLOSED[shape];
                    color = darken(state.color, 0.55);
                }
            }
            // order() makes sure the passes are drawn after the coat and in this sequence
            collector.order(1 + kind.ordinal()).submitModel(model, state, poseStack,
                    this.getParentModel().renderType(texture), light, OverlayTexture.NO_OVERLAY, color, null, 0);
        }
    }
}

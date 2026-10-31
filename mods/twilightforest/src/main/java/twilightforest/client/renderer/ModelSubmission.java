package twilightforest.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/** Preserves model, outline and destruction layers across the 26.3 submission split. */
public final class ModelSubmission {
	private ModelSubmission() {}

	public static <S> void submit(OrderedSubmitNodeCollector collector, Model<? super S> model, S state,
		PoseStack pose, Identifier texture, int light, int overlay, int outline,
		ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		submit(collector, model, state, pose, model.renderType(texture), light, overlay, outline, crumbling);
	}

	public static <S> void submit(OrderedSubmitNodeCollector collector, Model<? super S> model, S state,
		PoseStack pose, RenderType renderType, int light, int overlay, int outline,
		ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		submit(collector, model, state, pose, renderType, light, overlay, -1, null, outline, crumbling);
	}

	public static <S> void submit(OrderedSubmitNodeCollector collector, Model<? super S> model, S state,
		PoseStack pose, RenderType renderType, int light, int overlay, int color, @Nullable UvMapping uv,
		int outline, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		collector.submitModel(model, state, pose, renderType, light, overlay, color, uv, outline);
		if (crumbling != null) {
			collector.submitCrumblingOverlay(model, state, pose, renderType, light, overlay, color, crumbling);
		}
	}

	public static <S> void submit(OrderedSubmitNodeCollector collector, Model<S> model, S state,
		PoseStack pose, int light, int overlay, int color, SpriteId sprite, SpriteGetter sprites,
		int outline, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		submit(collector, model, state, pose, sprite.renderType(model.renderType()), light, overlay,
			color, sprites.get(sprite), outline, crumbling);
	}

	public static void submitPart(OrderedSubmitNodeCollector collector, ModelPart model, PoseStack pose,
		RenderType renderType, int light, int overlay, @Nullable UvMapping uv, int color,
		ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		collector.submitModelPart(model, pose, renderType, light, overlay, uv, color, 0);
		if (crumbling != null) {
			collector.submitCrumblingOverlay(model, pose, renderType, light, overlay, color, crumbling);
		}
	}
}

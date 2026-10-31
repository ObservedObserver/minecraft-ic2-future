package twilightforest.asmhooks;

import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.context.ContextKey;
import twilightforest.TwilightForestMod;

@SuppressWarnings({"JavadocReference", "unused"})
public class RenderHooks {

	public static final ContextKey<Boolean> HIDE_HEAD_KEY = new ContextKey<>(TwilightForestMod.prefix("hide_head"));

	/**
	 * {@link twilightforest.asm.transformers.render.HideHeadUnderTrophyTransformer}<p/>
	 *
	 * Injection Point:<br/>
	 * ModelFeatureRenderer.prepareModel(Submit)
	 * Targets: {@link Model#setupAnim(Object)}
	 */
	public static void applyHeadVisibility(ModelFeatureRenderer.Submit<?> submit) {
		setHeadVisibility(submit, false);
	}

	/**
	 * {@link twilightforest.asm.transformers.render.HideHeadUnderTrophyTransformer}<p/>
	 *
	 * Injection Point:<br/>
	 * ModelFeatureRenderer.prepareModel(Submit)
	 * Targets: {@link com.mojang.blaze3d.vertex.PoseStack#popPose()}
	 */
	public static void restoreHeadVisibility(ModelFeatureRenderer.Submit<?> submit) {
		setHeadVisibility(submit, true);
	}

	private static void setHeadVisibility(ModelFeatureRenderer.Submit<?> submit, boolean visible) {
		if (!(submit.model() instanceof HeadedModel headed) || !(submit.state() instanceof EntityRenderState state) || !Boolean.TRUE.equals(state.getRenderData(HIDE_HEAD_KEY)))
			return;

		headed.getHead().visible = visible;
	}
}

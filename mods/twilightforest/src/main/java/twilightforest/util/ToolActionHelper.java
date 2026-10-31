package twilightforest.util;

import net.minecraft.core.Holder;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;

public final class ToolActionHelper {
	private ToolActionHelper() {}

	public static boolean uses(ItemStack stack, ResourceKey<BlockTransformer> transformer) {
		Holder<BlockTransformer> component = stack.get(DataComponents.BLOCK_TRANSFORMER);
		return component != null && component.is(transformer);
	}
}

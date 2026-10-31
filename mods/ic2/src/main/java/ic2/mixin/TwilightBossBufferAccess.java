package ic2.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "twilightforest.entity.boss.BaseTFBoss", remap = false)
public interface TwilightBossBufferAccess {
  @Accessor(value = "dyingInventory", remap = false)
  NonNullList<ItemStack> ic2$getDyingInventory();
}

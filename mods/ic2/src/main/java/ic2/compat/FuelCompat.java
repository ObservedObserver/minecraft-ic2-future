package ic2.compat;

import ic2.core.IC2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.providers.number.floats.ResolvableFloat;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;

public final class FuelCompat {
  private FuelCompat() {}

  public static CookingFuel constant(int ticks) {
    return new CookingFuel(new ResolvableInt.Constant(ticks), new ResolvableFloat.Constant(1.0F));
  }

  public static int burnTime(ItemStack stack) {
    CookingFuel fuel = stack.get(DataComponents.COOKING_FUEL);
    if (fuel == null) return 0;
    if (fuel.burnTime() instanceof ResolvableInt.Constant constant) return constant.value();
    var server = IC2.envProxy.getServer();
    if (server == null) return 0;
    var query = net.neoforged.neoforge.common.loot.NeoForgeLootContextParams.QUERIED_STACK;
    var params =
        new LootParams.Builder(server.overworld())
            .withParameter(query, stack)
            .create(new net.minecraft.util.context.ContextKeySet.Builder().required(query).build());
    return fuel.burnTime()
        .get(new LootContext.Builder(params).create(java.util.Optional.empty()), 0);
  }
}

package ic2.compat;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Block;

public class DiggerItem extends TieredItem {
  public DiggerItem(Tier tier, TagKey<Block> effectiveBlocks, Properties properties) {
    super(
        tier,
        properties.component(
            DataComponents.TOOL,
            new Tool(
                List.of(
                    Tool.Rule.deniesDrops(
                        BuiltInRegistries.acquireBootstrapRegistrationLookup(
                                net.minecraft.core.registries.BuiltInRegistries.BLOCK)
                            .getOrThrow(tier.getIncorrectBlocksForDrops())),
                    Tool.Rule.minesAndDrops(
                        BuiltInRegistries.acquireBootstrapRegistrationLookup(
                                net.minecraft.core.registries.BuiltInRegistries.BLOCK)
                            .getOrThrow(effectiveBlocks),
                        tier.getSpeed())),
                1.0F,
                1,
                true)));
  }

  public static ItemAttributeModifiers createAttributes(Tier tier, float damage, float speed) {
    return ItemAttributeModifiers.builder()
        .add(
            Attributes.ATTACK_DAMAGE,
            new AttributeModifier(
                Item.BASE_ATTACK_DAMAGE_ID,
                damage + tier.getAttackDamageBonus(),
                AttributeModifier.Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND)
        .add(
            Attributes.ATTACK_SPEED,
            new AttributeModifier(
                Item.BASE_ATTACK_SPEED_ID, speed, AttributeModifier.Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND)
        .build();
  }
}

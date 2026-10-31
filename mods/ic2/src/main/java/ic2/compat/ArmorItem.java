package ic2.compat;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.Equippable;

/** IC2 armor behavior using the vanilla equipment component introduced after 1.21.1. */
public class ArmorItem extends Item {
  private final Holder<ArmorMaterial> material;
  private final Type type;

  public ArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
    super(equipmentProperties(properties, material.value(), type));
    this.material = material;
    this.type = type;
  }

  private static Properties equipmentProperties(
      Properties properties, ArmorMaterial material, Type type) {
    properties
        .attributes(material.createAttributes(type.vanilla()))
        .component(
            DataComponents.EQUIPPABLE,
            Equippable.builder(type.getSlot())
                .setEquipSound(material.equipSound())
                .setAsset(material.assetId())
                .build())
        .repairable(material.repairIngredient());
    if (material.enchantmentValue() > 0) {
      properties.enchantable(material.enchantmentValue());
    }
    return properties;
  }

  public EquipmentSlot getEquipmentSlot() {
    return type.getSlot();
  }

  public Holder<ArmorMaterial> getMaterial() {
    return material;
  }

  public Type getType() {
    return type;
  }

  public int getDefense() {
    return material.value().defense().getOrDefault(type.vanilla(), 0);
  }

  public float getToughness() {
    return material.value().toughness();
  }

  public enum Type {
    HELMET(ArmorType.HELMET),
    CHESTPLATE(ArmorType.CHESTPLATE),
    LEGGINGS(ArmorType.LEGGINGS),
    BOOTS(ArmorType.BOOTS),
    BODY(ArmorType.BODY);

    private final ArmorType vanilla;

    Type(ArmorType vanilla) {
      this.vanilla = vanilla;
    }

    public ArmorType vanilla() {
      return vanilla;
    }

    public EquipmentSlot getSlot() {
      return vanilla.getSlot();
    }
  }
}

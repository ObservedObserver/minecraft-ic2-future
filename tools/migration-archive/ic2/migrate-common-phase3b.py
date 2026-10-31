from pathlib import Path
import re
root=Path('src/main/java')
for p in root.rglob('*.java'):
 s=p.read_text()
 if 'simpleCodec(' in s:
  # Vanilla no longer serializes Block instances via per-subclass codecs.
  s=re.sub(r'  public static final com\.mojang\.serialization\.MapCodec<.*?\n      simpleCodec\([^;]+;\n\n  @Override\n  public com\.mojang\.serialization\.MapCodec<.*?\n  }\n', '',s,flags=re.S)
 s=s.replace('PushReaction.NORMAL','PushReaction.PUSH_PULL').replace('PushReaction.BLOCK','PushReaction.IMMOVEABLE').replace('PushReaction.DESTROY','PushReaction.POPPED')
 s=s.replace('net.minecraft.world.item.UseAnim','net.minecraft.world.item.ItemUseAnimation').replace('UseAnim ', 'ItemUseAnimation ').replace('UseAnim.', 'ItemUseAnimation.')
 if p.name in ['SimpleCraftingInventory.java','TileEntityBatchCrafter.java']:
  s=re.sub(r'\bStackedContents\b','StackedItemContents',s)
 if p.name=='TileEntityIndustrialWorkbench.java':
  # Do not collide with Container's new public SlotAccess getSlot(int).
  start=s.index('new SimpleCraftingInventory(2, 1)');end=s.index('    public InvSlotCraftingCombo',start)
  s=s[:start]+s[start:end].replace('getSlot(', 'getInvSlot(')+s[end:]
 if p.name=='Ic2TileEntityBlock.java':
  s=s.replace('Direction.class, Util.allFacings','Direction.class, java.util.List.copyOf(Util.allFacings)').replace('Direction.class, Util.horizontalFacings','Direction.class, java.util.List.copyOf(Util.horizontalFacings)').replace('Direction.class, Util.verticalFacings','Direction.class, java.util.List.copyOf(Util.verticalFacings)').replace('Direction.class, this.supportedFacings','Direction.class, java.util.List.copyOf(this.supportedFacings)')
 if p.name=='Ic2Items.java':
  s=s.replace('import net.minecraft.world.item.*;', 'import net.minecraft.world.item.*;\nimport ic2.compat.Tiers;')
  s=s.replace('new SignItem(new Properties(), Ic2Blocks.RUBBER_SIGN, Ic2Blocks.RUBBER_WALL_SIGN)', 'new StandingAndWallBlockItem(Ic2Blocks.RUBBER_SIGN, Ic2Blocks.RUBBER_WALL_SIGN, new Properties(), net.minecraft.core.Direction.DOWN)')
  s=s.replace('@Override\n            @Override','@Override')
  s=re.sub(r'new SwordItem\(\s*Ic2ToolMaterials.BRONZE,\s*new Properties\(\)\s*\.attributes\(SwordItem.createAttributes\(Ic2ToolMaterials.BRONZE, 3, -2.4F\)\)\)', 'new Item(new Properties().sword(Ic2ToolMaterials.BRONZE.asToolMaterial(), 3, -2.4F))',s)
  s=re.sub(r'new ShovelItem\(\s*Ic2ToolMaterials.BRONZE,\s*new Properties\(\)\s*\.attributes\(ShovelItem.createAttributes\(Ic2ToolMaterials.BRONZE, 1.5F, -3.0F\)\)\)', 'new Item(new Properties().shovel(Ic2ToolMaterials.BRONZE.asToolMaterial(), 1.5F, -3.0F))',s)
 if p.name=='Ic2Blocks.java':
  s=s.replace('new LeavesBlock(\n', 'new LeavesBlock(new net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer(java.util.Optional.empty(), 300, java.util.Optional.empty(), 1, 3),\n')
  s=s.replace('.isViewBlocking(Ic2Blocks::never)', '.isViewBlocking((state, world, pos, bounds) -> false)')
  s=re.sub(r'new TreeGrower\(\s*"ic2:rubber",\s*java.util.Optional.empty\(\),\s*java.util.Optional.of\(\s*ResourceKey.create\(\s*Registries.FEATURE, IC2.getIdentifier\("rubber_tree"\)\)\),\s*java.util.Optional.empty\(\)\)', 'new TreeGrower("ic2:rubber", net.minecraft.util.random.WeightedList.of(Ic2WorldGen.RUBBER_TREE), net.minecraft.util.random.WeightedList.of(), net.minecraft.util.random.WeightedList.of(), Ic2WorldGen.RUBBER_TREE)',s)
  s=s.replace('.lootFrom(() -> RUBBER_SIGN)', '.overrideLootTable(RUBBER_SIGN.getLootTable())')
 if p.name in ['IRecipeInput.java']:
  s=s.replace('Ingredient.of(this.getInputs().stream())', 'Ingredient.of(this.getInputs().stream().map(ItemStack::getItem))')
 if p.name=='VanillaSmeltingResolver.java':
  s=s.replace('recipe.getIngredients()', 'java.util.List.of(recipe.input())').replace('recipe.getResultItem(registries)', 'recipe.result().create()')
 if p.name=='RecipeResolver.java':
  s=s.replace('NonNullList<Ingredient> inputs = irecipe.getIngredients()', 'List<Ingredient> inputs = irecipe.placementInfo().ingredients()').replace('irecipe.getResultItem(registries)', 'ic2.compat.RecipeCompat.result(irecipe, IC2.envProxy.getServer().overworld())')
 if p.name=='RecipeUtil.java':
  s=s.replace('        res.add(new LeanItemStack(stack));\n      }\n    }','        res.add(new LeanItemStack(stack));\n      }\n      ret.add(res);\n    }')
 if p.name in ['SmeltingRecipeManager.java','LaserBulletEntity.java']:
  s=s.replace('.getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), null)', '.getRecipesFor(RecipeType.SMELTING, new SingleRecipeInput(input), null).findFirst()')
  s=s.replace('.getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), world)', '.getRecipesFor(RecipeType.SMELTING, new SingleRecipeInput(stack), world).findFirst()')
  s=re.sub(r'recipe.getResultItem\([^;]+?\)', 'recipe.result().create()',s) if False else s
  s=s.replace('recipe.getResultItem((RegistryAccess) null)', 'recipe.result().create()').replace('recipe.getResultItem(world.registryAccess())', 'recipe.result().create()').replace('recipe.getExperience()', 'recipe.experience()')
 if p.name=='ArmorGameTests.java':s=s.replace('armor.getMaterial().value().getDefense(armor.getType())','armor.getDefense()')
 if p.name=='ContainerIndustrialWorkbench.java':s=s.replace('toCraftingInput(inputInv), this.base.getLevel().registryAccess())','toCraftingInput(inputInv))')
 if p.name in ['TileEntityAdvMiner.java','ToolboxGameTests.java']:
  s=s.replace('.getList("Items", 10)', '.getListOrEmpty("Items")').replace('.getList("filter", 10)', '.getListOrEmpty("filter")')
 p.write_text(s)

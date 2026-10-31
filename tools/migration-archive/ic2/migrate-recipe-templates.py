from pathlib import Path
root=Path('src/main/java')
p=root/'ic2/core/recipe/v2/RecipeIo.java';s=p.read_text()
s=s.replace('''        Item item = GsonHelper.getAsItem(object, "item").value();
        ItemStack stack = new ItemStack(item, count);
        StackUtil.setTag(stack, asNbt(object.get("data"), "data"));
        return new RecipeInputItemStack(stack);''','''        return new RecipeInputItemStack(parseOutputTemplate(object));''')
pos=s.index('  public static ItemStack parseOutput(JsonObject json)')
s=s[:pos]+'''  public static net.minecraft.world.item.ItemStackTemplate parseOutputTemplate(JsonObject json) {
    Item item = GsonHelper.getAsItem(json, "item").value();
    int count = GsonHelper.getAsInt(json, "count", 1);
    if (count <= 0) throw new JsonSyntaxException("Recipe stack count must be positive");
    CompoundTag tag = getNbt(json);
    var patch = net.minecraft.core.component.DataComponentPatch.builder();
    if (tag != null && !tag.isEmpty()) patch.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
        net.minecraft.world.item.component.CustomData.of(tag));
    return new net.minecraft.world.item.ItemStackTemplate(item, count, patch.build());
  }

'''+s[pos:]
a=s.index('    Item item = GsonHelper.getAsItem(json, "item").value();',s.index('  public static ItemStack parseOutput(JsonObject json)'));b=s.index('\n  }',a)
s=s[:a]+'    return parseOutputTemplate(json).create();'+s[b:]
s=s.replace('ItemStack.STREAM_CODEC.encode(buf, stack.input);','net.minecraft.world.item.ItemStackTemplate.STREAM_CODEC.encode(buf, stack.input);').replace('new RecipeInputItemStack(ItemStack.STREAM_CODEC.decode(buf))','new RecipeInputItemStack(net.minecraft.world.item.ItemStackTemplate.STREAM_CODEC.decode(buf))')
p.write_text(s)
# Stack-matching inputs retain templates and instantiate only for runtime matching/display.
p=root/'ic2/core/recipe/input/RecipeInputItemStack.java';s=p.read_text();s=s.replace('  public final ItemStack input;','  public final net.minecraft.world.item.ItemStackTemplate input;')
a=s.index('  public RecipeInputItemStack(ItemStack input)');b=s.index('  @Override',a)
s=s[:a]+'''  public RecipeInputItemStack(ItemStack input) {
    this(net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(input));
  }

  public RecipeInputItemStack(net.minecraft.world.item.ItemStackTemplate input) {
    this.input = java.util.Objects.requireNonNull(input);
  }

'''+s[b:]
s=s.replace('this.input.getItem()', 'this.input.item().value()').replace('StackUtil.getTag(this.input)', 'StackUtil.getTag(this.input.create())').replace('this.input.getCount()', 'this.input.count()').replace('Arrays.asList(this.input)', 'Arrays.asList(this.input.create())').replace('StackUtil.setImmutableSize(this.input,', 'StackUtil.setImmutableSize(this.input.create(),')
s=s.replace('StackUtil.checkItemEqualityStrict((other = (RecipeInputItemStack) obj).input, this.input)','this.input.equals((other = (RecipeInputItemStack) obj).input)')
p.write_text(s)
# Native crafting output templates, including wire representation.
for cls in ['AdvRecipe','AdvShapelessRecipe']:
 p=root/f'ic2/core/recipe/{cls}.java';s=p.read_text();s=s.replace('import net.minecraft.world.item.ItemStack;','import net.minecraft.world.item.ItemStack;\nimport net.minecraft.world.item.ItemStackTemplate;')
 s=s.replace('public final ItemStack output;', 'public final ItemStackTemplate output;')
 if cls=='AdvRecipe':
  # Internal construction is template-based; keep the original reflective factory for existing tests/API callers.
  s=s.replace('      ItemStack result,','      ItemStackTemplate result,')
  marker='  private static AdvRecipe create('
  pos=s.index(marker)
  s=s[:pos]+'''  private static AdvRecipe create(int width, int height, IRecipeInput[] ingredients,
      ItemStack result, boolean consuming, boolean hidden) {
    return create(width, height, ingredients, ItemStackTemplate.fromNonEmptyStack(result), consuming, hidden);
  }

'''+s[pos:]
 else:
  marker='  public AdvShapelessRecipe('
  pos=s.index(marker)
  s=s[:pos]+'''  public AdvShapelessRecipe(Identifier id, IRecipeInput[] input, ItemStack output,
      boolean hidden, boolean consuming) {
    this(id, input, ItemStackTemplate.fromNonEmptyStack(output), hidden, consuming);
  }

'''+s[pos:]
  s=s.replace('Identifier id, IRecipeInput[] input, ItemStack output, boolean hidden, boolean consuming)', 'Identifier id, IRecipeInput[] input, ItemStackTemplate output, boolean hidden, boolean consuming)')
 s=s.replace('return this.output;', 'return this.output.create();').replace('this.output.copy()', 'this.output.create()').replace('this.input, this.output, this.hidden', 'this.input, this.output.create(), this.hidden')
 s=s.replace('ItemStack result = RecipeIo.parseOutput(', 'ItemStackTemplate result = RecipeIo.parseOutputTemplate(')
 s=s.replace('ItemStack result = ItemStack.STREAM_CODEC.decode(buf);','ItemStackTemplate result = ItemStackTemplate.STREAM_CODEC.decode(buf);').replace('null, inputs, ItemStack.STREAM_CODEC.decode(buf),','null, inputs, ItemStackTemplate.STREAM_CODEC.decode(buf),')
 s=s.replace('ItemStack.STREAM_CODEC.encode(buf, recipe.output);','ItemStackTemplate.STREAM_CODEC.encode(buf, recipe.output);').replace('ItemStack.STREAM_CODEC.encode(buf, stack.input);','ItemStackTemplate.STREAM_CODEC.encode(buf, stack.input);').replace('new RecipeInputItemStack(ItemStack.STREAM_CODEC.decode(buf))','new RecipeInputItemStack(ItemStackTemplate.STREAM_CODEC.decode(buf))')
 p.write_text(s)
p=root/'ic2/integration/jei/AdvRecipeCategoryExtension.java';s=p.read_text().replace('List.of(recipe.output)', 'List.of(recipe.output.create())');p.write_text(s)
# Gradual recipes also deserialize before default components are bound.
p=root/'ic2/core/recipe/GradualRecipe.java';s=p.read_text().replace('import net.minecraft.world.item.ItemStack;', 'import net.minecraft.world.item.ItemStack;\nimport net.minecraft.world.item.ItemStackTemplate;').replace('private final ItemStack chargeMaterial;', 'private final ItemStackTemplate chargeMaterial;')
pos=s.index('  public GradualRecipe(')
s=s[:pos]+'''  public GradualRecipe(Identifier id, AbstractDamageableReactorComponent item,
      ItemStack chargeMaterial, int amount, boolean hidden) {
    this(id, item, ItemStackTemplate.fromNonEmptyStack(chargeMaterial), amount, hidden);
  }

'''+s[pos:]
s=s.replace('      ItemStack chargeMaterial,', '      ItemStackTemplate chargeMaterial,').replace('StackUtil.checkItemEquality(stack, this.chargeMaterial)', 'StackUtil.checkItemEquality(stack, this.chargeMaterial.create())').replace('this.chargeMaterial.getItem()', 'this.chargeMaterial.item().value()').replace('ItemStack chargeMaterial =\n          parseItemStack(', 'ItemStackTemplate chargeMaterial =\n          RecipeIo.parseOutputTemplate(').replace('ItemStack chargeMaterial = ItemStack.STREAM_CODEC.decode(buffer);','ItemStackTemplate chargeMaterial = ItemStackTemplate.STREAM_CODEC.decode(buffer);').replace('ItemStack.STREAM_CODEC.encode(buffer, recipe.chargeMaterial);','ItemStackTemplate.STREAM_CODEC.encode(buffer, recipe.chargeMaterial);')
if 'import ic2.core.recipe.v2.RecipeIo;' not in s:s=s.replace('package ic2.core.recipe;','package ic2.core.recipe;\n\nimport ic2.core.recipe.v2.RecipeIo;')
p.write_text(s)

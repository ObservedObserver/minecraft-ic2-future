from pathlib import Path
import re
r=Path('src/main/java')
for p in r.rglob('*.java'):
 s=p.read_text()
 # Block definitions no longer have per-class codecs. These encode construction properties,
 # not world state; block state persistence continues through the registered block ID.
 if '/core/block/' in str(p) and ('simpleCodec(' in s or 'propertiesCodec()' in s):
  s=re.sub(r'  public static final (?:com\.mojang\.serialization\.)?MapCodec<[^;]+? CODEC\s*=\s*[^;]+;\n', '', s)
  s=re.sub(r'  @Override\n  (?:public|protected) (?:com\.mojang\.serialization\.)?MapCodec<.*?\s+codec\(\) \{\s*return CODEC;\s*}\n', '',s,flags=re.S)
 s=s.replace('ic2.core.init.Ic2WorldGen', 'ic2.core.world.Ic2WorldGen')
 s=s.replace('BuiltInRegistries.BLOCK.get(', 'BuiltInRegistries.BLOCK.getValue(').replace('BuiltInRegistries.ITEM.get(', 'BuiltInRegistries.ITEM.getValue(').replace('BuiltInRegistries.FLUID.get(', 'BuiltInRegistries.FLUID.getValue(')
 # Registry lookup holders are still used explicitly via getOrThrow/getOptional as appropriate.
 s=s.replace('MobEffects.CONFUSION', 'MobEffects.NAUSEA').replace('MobEffects.DIG_SLOWDOWN','MobEffects.MINING_FATIGUE').replace('MobEffects.DIG_SPEED','MobEffects.HASTE').replace('MobEffects.HARM','MobEffects.INSTANT_DAMAGE')
 s=s.replace('context.getParamOrNull(', 'context.getOptional(').replace('context.getParam(', 'context.getParameter(')
 s=s.replace('RandomSource.createNewThreadLocalInstance()', 'RandomSource.create()')
 if p.name=='IC2.java':s=s.replace('player.server', 'player.level().getServer()')
 if p.name in ['HandHeldInventory.java','ItemWindRotor.java','ItemBatteryChargeHotbar.java']:
  s=re.sub(r'((?:SideProxyClient\.mc|Minecraft.getInstance\(\)))\.screen\b',r'\1.gui.screen()',s)
 if p.name=='TileEntityChunkLoader.java':s=s.replace('((LongTag) tag).getAsLong()', '((LongTag) tag).longValue()').replace('chunk.toLong()', 'chunk.pack()')
 if p.name=='TileEntityAdvMiner.java':s=s.replace('nbt.getList("Items", Tag.TAG_COMPOUND)', 'nbt.getListOrEmpty("Items")')
 if p.name=='TileEntityWaterKineticGenerator.java':s=s.replace('world.getDayTime()', 'world.getOverworldClockTime()')
 if p.name=='TileEntitySolarGenerator.java':s=s.replace('world.getSunAngle(1.0F)', '(world.environmentAttributes().getDimensionValue(net.minecraft.world.attribute.EnvironmentAttributes.SUN_ANGLE) * ((float) Math.PI / 180.0F))')
 if p.name=='TileEntityTerra.java':s=s.replace('state.isSolidRender(world, pos)', 'state.isSolidRender()')
 if p.name=='TileEntityLuminator.java':s=s.replace('entity.getType().is(', 'entity.getType().builtInRegistryHolder().is(')
 if p.name=='TileEntitySteamRepressurizer.java':s=s.replace('BuiltInRegistries.FLUID.getTag(', 'BuiltInRegistries.FLUID.get(')
 if p.name=='TileEntityTeleporter.java':s=s.replace('for (ItemStack stack : living.getAllSlots()) {', 'for (var slot : net.minecraft.world.entity.EquipmentSlot.values()) {\n        ItemStack stack = living.getItemBySlot(slot);')
 if p.name=='ElectricBoatEntity.java':
  s=re.sub(r'  @Override\n  public @NotNull Item getDropItem\(\) \{.*?\n  }\n', '',s,flags=re.S)
 if p.name in ['ExplosiveEntity.java','ParticleEntity.java']:
  at=s.index('  protected void defineSynchedData(')
  s=s[:at]+'''  @Override
  public boolean hurtServer(net.minecraft.server.level.ServerLevel level,
      net.minecraft.world.damagesource.DamageSource source, float damage) {
    return false;
  }

'''+s[at:]
 if p.name=='TileEntityTesla.java':s=s.replace('living.hurt(', 'living.hurtOrSimulate(')
 if p.name=='BiomeUtil.java':s=s.replace('world.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(biome)', 'world.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(biome)')
 if p.name in ['StackUtil.java','ItemScanner.java']:
  s=s.replace('state.getBlock().getCloneItemStack(world, pos, state)', 'state.getCloneItemStack(world, pos, false)')
  s=s.replace('block.getCloneItemStack(world, pos, state)', 'state.getCloneItemStack(world, pos, false)')
 s=re.sub(r'([\w.]+)\.getValues\(\).containsKey\(([^)]+)\)', r'\1.hasProperty(\2)',s)
 if p.name in ['NanoSaberGameTests.java','NanoSuitGameTests.java']:s=s.replace('EquipmentSlotGroup.MAINHAND,', 'net.minecraft.world.entity.EquipmentSlot.MAINHAND,')
 if p.name=='PainterGameTests.java':s=s.replace('Blocks.BLUE_STAINED_GLASS','Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.BLUE)')
 if p.name=='ElectricItemTooltipHandler.java':s=s.replace('Screen.hasShiftDown()', 'net.minecraft.client.Minecraft.getInstance().hasShiftDown()')
 if p.name=='InteractionResults.java':
  s=s.replace('  private InteractionResults() {}', '''  private InteractionResults() {}
  public static net.minecraft.world.InteractionResult consume(net.minecraft.world.item.ItemStack stack) {
    return net.minecraft.world.InteractionResult.CONSUME.heldItemTransformedTo(stack);
  }''')
 if p.name in ['ItemHydrationCell.java']:
  s=re.sub(r'InteractionResult.sidedSuccess\([^;]+?\)', 'InteractionResult.SUCCESS',s) if False else s
  s=s.replace('InteractionResult.sidedSuccess(world.isClientSide())','InteractionResult.SUCCESS')
 if p.name=='Ic2TileEntityBlock.java':
  s=s.replace('Direction.class, facings)', 'Direction.class, java.util.List.copyOf(facings))')
  s=s.replace('world instanceof ServerLevel serverWorld', 'world instanceof net.minecraft.server.level.ServerLevel serverWorld')
  s=s.replace('onBlockExploded(BlockState state, Level level,', 'onBlockExploded(BlockState state, net.minecraft.server.level.ServerLevel level,')
 if p.name=='AbstractCableBlock.java':
  s=s.replace('      boolean willHarvest,\n', '      ItemStack toolStack,\n      boolean willHarvest,\n')
  s=s.replace('player, willHarvest, fluid)', 'player, toolStack, willHarvest, fluid)')
  s=s.replace('@NotNull LevelReader level, @NotNull BlockPos pos, @NotNull BlockState state)', '@NotNull LevelReader level, @NotNull BlockPos pos, @NotNull BlockState state, boolean includeData)')
  s=s.replace('this.toCableState(state, cableBlock))', 'this.toCableState(state, cableBlock), includeData)')
  s=s.replace('super.getCloneItemStack(level, pos, state)', 'super.getCloneItemStack(level, pos, state, includeData)')
 if p.name=='Ic2LootModifier.java':s=s.replace('public Ic2LootModifier(LootItemCondition[] conditionsIn) {\n    super(conditionsIn);', 'public Ic2LootModifier(java.util.Optional<net.minecraft.core.Holder<LootItemCondition>> condition, int priority) {\n    super(condition, priority);')
 if p.name=='EventHandlerForge.java':s=s.replace('EventHandler.onLivingFall(event.getEntity(), event.getDistance())','EventHandler.onLivingFall(event.getEntity(), (float) event.getDistance())')
 if p.name=='EventHandler.java':s=s.replace('world.random.', 'world.getRandom().')
 p.write_text(s)

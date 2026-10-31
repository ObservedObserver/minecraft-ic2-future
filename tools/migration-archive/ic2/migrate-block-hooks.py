from pathlib import Path
import re
r=Path('src/main/java')
for p in r.rglob('*.java'):
 s=p.read_text().replace('ic2.core.util.ArmorSlot','ic2.core.slot.ArmorSlot')
 s=s.replace('.absMoveTo(', '.absSnapTo(').replace('.getNormal()', '.getUnitVec3i()')
 if p.name=='MiningLaserGameTests.java':s=s.replace('player.moveTo(', 'player.snapTo(')
 if p.name=='Ic2Items.java':s=s.replace('new Properties(), net.minecraft.core.Direction.DOWN)', 'net.minecraft.core.Direction.DOWN, new Properties())')
 if p.name=='Ic2Blocks.java':s=s.replace('Ic2WorldGen.RUBBER_TREE', 'ic2.core.init.Ic2WorldGen.RUBBER_TREE')
 if p.name=='FmlMod.java':s=s.replace('ModConfig.Type.COMMON', 'ModConfig.Type.LOCAL')
 if p.name=='SmeltingRecipeManager.java':s=s.replace('recipe.result().create()', 'recipe.assemble(new SingleRecipeInput(input))')
 if p.name=='VanillaSmeltingResolver.java':s=s.replace('recipe.result().create()', 'recipe.assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(net.minecraft.world.item.ItemStack.EMPTY))')
 if p.name=='LaserBulletEntity.java':
  s=s.replace('super(Ic2Entities.LASER_BULLET, owner, world);', 'super(Ic2Entities.LASER_BULLET, world);\n    this.setOwner(owner);\n    this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());')
  s=s.replace('entity.hurt(', 'entity.hurtOrSimulate(')
  s=s.replace('.getRecipeFor(', '.getRecipesFor(').replace('this.level())\n            .map', 'this.level()).findFirst()\n            .map')
  s=s.replace('recipe.getResultItem(this.level().registryAccess())', 'recipe.assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(inputItemStack))')
  a=s.index('          new Explosion(');b=s.index('));',a)
  s=s[:a]+'''          new net.minecraft.world.level.ServerExplosion(
              (net.minecraft.server.level.ServerLevel) world, this, null, null,
              net.minecraft.world.phys.Vec3.atCenterOf(pos), 1.0F, false,
              BlockInteraction.DESTROY)'''+s[b+1:]
 if p.name in ['ExplosiveEntity.java','ParticleEntity.java']:
  s=re.sub(r'addAdditionalSaveData\(CompoundTag ', 'addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput ',s)
  s=re.sub(r'readAdditionalSaveData\(CompoundTag ', 'readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput ',s)
  s=s.replace('updateInWaterStateAndDoFluidPushing()', 'updateFluidInteraction()')
 if p.name=='Ic2Entities.java':s=s.replace('builder.build(IC2.getIdentifier(name).toString())', 'builder.build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, IC2.getIdentifier(name)))')
 if p.name in ['Ic2TileEntityBlock.java','AbstractCableBlock.java']:
  # updateShape's scheduling context and random source are explicit in modern vanilla.
  a=s.index('  public ' + ('@NotNull ' if p.name=='AbstractCableBlock.java' else '')+'BlockState updateShape(');b=s.index(') {',a)
  s=s[:a]+'''  @Override
  public BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader world,
      net.minecraft.world.level.ScheduledTickAccess ticks, BlockPos pos, Direction direction,
      BlockPos neighborPos, BlockState neighborState, net.minecraft.util.RandomSource random'''+s[b:]
  s=s.replace('super.updateShape(state, direction, neighborState, world, pos, neighborPos)', 'super.updateShape(state, world, ticks, pos, direction, neighborPos, neighborState, random)')
  s=s.replace('world.scheduleTick(pos, Fluids.WATER', 'ticks.scheduleTick(pos, Fluids.WATER')
 if p.name=='Ic2TileEntityBlock.java':
  s=s.replace('BlockPos sourcePos,\n      boolean notify)', 'net.minecraft.world.level.redstone.Orientation orientation,\n      boolean notify)')
  s=s.replace('super.neighborChanged(state, world, pos, sourceBlock, sourcePos, notify)', 'super.neighborChanged(state, world, pos, sourceBlock, orientation, notify)')
  s=s.replace('tileEntity.onNeighborChange(sourceBlock, sourcePos);', '''// The modern callback omits the source position. Refresh all adjacent inputs.
      for (Direction side : Direction.values()) {
        tileEntity.onNeighborChange(sourceBlock, pos.relative(side));
      }''')
  s=s.replace('public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity)', 'public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity, net.minecraft.world.entity.InsideBlockEffectApplier effects, boolean precise)')
  s=s.replace('world.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)', 'world instanceof ServerLevel serverWorld && serverWorld.getGameRules().get(GameRules.MOB_GRIEFING)')
  s=s.replace('super.entityInside(state, world, pos, entity)', 'super.entityInside(state, world, pos, entity, effects, precise)')
  a=s.index('  public void onRemove(');b=s.index('  @Override',a)
  s=s[:a]+s[b:]
 if p.name=='Ic2TileEntity.java':
  needle='  protected void onBlockBreak() {}'
  s=s.replace(needle,'''  @Override
  public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    // Vanilla invokes this before removing the BE. Aux drops already include IC2 inventories.
    if (this.level != null) {
      for (ItemStack stack : this.getAuxDrops(0)) {
        net.minecraft.world.level.block.Block.popResource(this.level, pos, stack);
      }
      this.onBlockBreak();
    }
  }

'''+needle)
 if p.name=='RubberLogBlock.java':
  s=s.replace('public void onRemove(', 'public void affectNeighborsAfterRemoval(').replace('@NotNull Level world,\n      @NotNull BlockPos pos,\n      @NotNull BlockState newState,', '@NotNull ServerLevel world,\n      @NotNull BlockPos pos,')
  s=s.replace('if (!state.is(newState.getBlock()) && !world.isClientSide())', 'if (!world.isClientSide())')
  s=s.replace('super.onRemove(state, world, pos, newState, movedByPiston)', 'super.affectNeighborsAfterRemoval(state, world, pos, movedByPiston)')
 if p.name=='AbstractCableBlock.java':
  s=s.replace('@NotNull BlockPos fromPos,\n      boolean notify)', 'net.minecraft.world.level.redstone.Orientation orientation,\n      boolean notify)').replace('super.neighborChanged(state, world, pos, block, fromPos, notify)', 'super.neighborChanged(state, world, pos, block, orientation, notify)')
  s=s.replace('public void onRemove(', 'public void affectNeighborsAfterRemoval(').replace('@NotNull Level world,\n      @NotNull BlockPos pos,\n      BlockState newState,', '@NotNull net.minecraft.server.level.ServerLevel world,\n      @NotNull BlockPos pos,')
  s=s.replace('if (!newState.is(this) || this.getColor(newState) != this.getColor(state)) {', 'if (!world.getBlockState(pos).is(this) || this.getColor(world.getBlockState(pos)) != this.getColor(state)) {')
  s=s.replace('super.getOcclusionShape(state, world, pos)', 'super.getOcclusionShape(state)')
  s=re.sub(r'getOcclusionShape\(\s*@NotNull BlockState state,\s*@NotNull BlockGetter world,\s*@NotNull BlockPos pos\)', 'getOcclusionShape(@NotNull BlockState state)',s)
 if p.name=='Ic2Explosion.java':s=s.replace('this.worldObj.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)', '((net.minecraft.server.level.ServerLevel) this.worldObj).getGameRules().get(GameRules.BLOCK_DROPS)')
 if p.name=='EventHandler.java':
  s=s.replace('onChunkDataLoad(LevelChunk chunk, CompoundTag data)', 'onChunkDataLoad(LevelChunk chunk, net.minecraft.world.level.chunk.storage.SerializableChunkData data)').replace('onChunkSave(LevelChunk chunk, CompoundTag data)', 'onChunkSave(LevelChunk chunk, net.minecraft.world.level.chunk.storage.SerializableChunkData data)')
 if p.name=='AbstractBoatEntity.java':
  s=s.replace('super(entityType, world);', '''super(entityType, world, () -> {
      if (entityType == ic2.core.ref.Ic2Entities.RUBBER_BOAT) return ic2.core.ref.Ic2Items.BROKEN_RUBBER_BOAT;
      if (entityType == ic2.core.ref.Ic2Entities.ELECTRIC_BOAT) return ic2.core.ref.Ic2Items.ELECTRIC_BOAT;
      return ic2.core.ref.Ic2Items.CARBON_BOAT;
    });''')
  s=s.replace('Class<Boat> boatEntityClass = Boat.class;', 'Class<net.minecraft.world.entity.vehicle.boat.AbstractBoat> boatEntityClass = net.minecraft.world.entity.vehicle.boat.AbstractBoat.class;')
  s=s.replace('public ItemEntity spawnAtLocation(@NotNull ItemLike item)', 'public ItemEntity spawnAtLocation(net.minecraft.server.level.ServerLevel level, @NotNull ItemLike item)')
  s=s.replace('this.spawnAtLocation(new ItemStack(', 'this.spawnAtLocation(level, new ItemStack(').replace('this.spawnAtLocation(this.getExtraDropItemStack())', 'this.spawnAtLocation(level, this.getExtraDropItemStack())').replace('super.spawnAtLocation(item)', 'super.spawnAtLocation(level, item)')
 if p.name in ['RubberBoatEntity.java','ElectricBoatEntity.java','CarbonBoatEntity.java']:
  s=re.sub(r'  @Override\n  public @NotNull Item getDropItem\(\) \{\n    return [^;]+;\n  }\n\n','',s)
 p.write_text(s)

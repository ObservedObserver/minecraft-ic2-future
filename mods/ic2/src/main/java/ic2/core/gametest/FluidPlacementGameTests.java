package ic2.core.gametest;

import ic2.compat.GameTest;
import ic2.core.ref.Ic2Fluids;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

public class FluidPlacementGameTests {
  private static final String TEMPLATE = "gametest/empty3x9x3";
  private static final BlockPos SOURCE_POS = new BlockPos(1, 3, 1);

  // HydrogenBlock overrides onPlace/neighborChanged; if the LiquidBlock supers are skipped, a
  // placed source never gets a fluid tick scheduled and never spreads (upstream 8bee6d7b).
  @GameTest(template = TEMPLATE, timeoutTicks = 60)
  public static void placedHydrogenSourceSpreads(GameTestHelper helper) {
    assertPlacedSourceSpreads(helper, Ic2Fluids.HYDROGEN.still());
  }

  // HotWaterBlock.onPlace only scheduled its cool-down block tick, never the fluid tick
  @GameTest(template = TEMPLATE, timeoutTicks = 60)
  public static void placedHotWaterSourceSpreads(GameTestHelper helper) {
    assertPlacedSourceSpreads(helper, Ic2Fluids.HOT_WATER.still());
  }

  // PahoehoeLavaBlock.onPlace only scheduled its basalt-conversion block tick, never the fluid
  // tick
  @GameTest(template = TEMPLATE, timeoutTicks = 60)
  public static void placedPahoehoeLavaSourceSpreads(GameTestHelper helper) {
    assertPlacedSourceSpreads(helper, Ic2Fluids.PAHOEHOE_LAVA.still());
  }

  // UUMatterBlock.neighborChanged now calls the LiquidBlock super; its custom interaction
  // (adjacent lava source -> obsidian) must keep working, and the placed source must still flow
  @GameTest(template = TEMPLATE, timeoutTicks = 60)
  public static void uuMatterKeepsLavaInteractionAndSpreads(GameTestHelper helper) {
    buildBasinFloor(helper);
    helper.setBlock(
        SOURCE_POS, Ic2Fluids.UU_MATTER.still().defaultFluidState().createLegacyBlock());
    helper.setBlock(SOURCE_POS.west(), Blocks.LAVA);

    helper.succeedWhen(
        () -> {
          helper.assertBlockPresent(Blocks.OBSIDIAN, SOURCE_POS.west());
          FluidState east = helper.getLevel().getFluidState(helper.absolutePos(SOURCE_POS.east()));
          helper.assertTrue(
              east.getType().isSame(Ic2Fluids.UU_MATTER.still()),
              "the UU matter source must spread away from the converted lava");
        });
  }

  private static void buildBasinFloor(GameTestHelper helper) {
    for (int x = 0; x <= 2; x++) {
      for (int z = 0; z <= 2; z++) {
        helper.setBlock(new BlockPos(x, 2, z), Blocks.STONE);
      }
    }
  }

  private static void assertPlacedSourceSpreads(GameTestHelper helper, Fluid fluid) {
    buildBasinFloor(helper);
    helper.setBlock(SOURCE_POS, fluid.defaultFluidState().createLegacyBlock());

    BlockPos[] neighbors = {
      SOURCE_POS.west(), SOURCE_POS.east(), SOURCE_POS.north(), SOURCE_POS.south()
    };
    helper.succeedWhen(
        () -> {
          FluidState source = helper.getLevel().getFluidState(helper.absolutePos(SOURCE_POS));
          helper.assertTrue(
              source.isSource() && source.getType().isSame(fluid),
              "the fluid source must remain at the placement position");
          for (BlockPos neighbor : neighbors) {
            FluidState state = helper.getLevel().getFluidState(helper.absolutePos(neighbor));
            if (state.getType().isSame(fluid) && !state.isSource()) {
              return;
            }
          }
          helper.fail("the placed fluid source must spread to a neighbor");
        });
  }

  @GameTest(template = "gametest/empty7x7x7")
  public static void uuMatterSourceAndFlowingAllowMovement(GameTestHelper h) {
    assertFluidMovement(h, Ic2Fluids.UU_MATTER);
  }

  @GameTest(template = "gametest/empty7x7x7")
  public static void constructionFoamSourceAndFlowingAllowMovement(GameTestHelper h) {
    assertFluidMovement(h, Ic2Fluids.CONSTRUCTION_FOAM);
  }

  @GameTest(template = "gametest/empty7x7x7")
  public static void coolantSourceAndFlowingAllowMovement(GameTestHelper h) {
    assertFluidMovement(h, Ic2Fluids.COOLANT);
  }

  @GameTest(template = "gametest/empty7x7x7")
  public static void creosoteSourceAndFlowingAllowMovement(GameTestHelper h) {
    assertFluidMovement(h, Ic2Fluids.CREOSOTE);
  }

  @GameTest(template = "gametest/empty7x7x7")
  public static void hotCoolantSourceAndFlowingAllowMovement(GameTestHelper h) {
    assertFluidMovement(h, Ic2Fluids.HOT_COOLANT);
  }

  @GameTest(template = "gametest/empty7x7x7")
  public static void pahoehoeLavaSourceAndFlowingAllowMovement(GameTestHelper h) {
    assertFluidMovement(h, Ic2Fluids.PAHOEHOE_LAVA);
  }

  @GameTest(template = "gametest/empty7x7x7")
  public static void biomassSourceAndFlowingAllowMovement(GameTestHelper h) {
    assertFluidMovement(h, Ic2Fluids.BIOMASS);
  }

  @GameTest(template = "gametest/empty7x7x7")
  public static void biogasSourceAndFlowingAllowMovement(GameTestHelper h) {
    assertFluidMovement(h, Ic2Fluids.BIOGAS);
  }

  @GameTest(template = "gametest/empty7x7x7")
  public static void distilledWaterSourceAndFlowingAllowMovement(GameTestHelper h) {
    assertFluidMovement(h, Ic2Fluids.DISTILLED_WATER);
  }

  @GameTest(template = "gametest/empty7x7x7")
  public static void superheatedSteamSourceAndFlowingAllowMovement(GameTestHelper h) {
    assertFluidMovement(h, Ic2Fluids.SUPERHEATED_STEAM);
  }

  @GameTest(template = "gametest/empty7x7x7")
  public static void steamSourceAndFlowingAllowMovement(GameTestHelper h) {
    assertFluidMovement(h, Ic2Fluids.STEAM);
  }

  @GameTest(template = "gametest/empty7x7x7")
  public static void hotWaterSourceAndFlowingAllowMovement(GameTestHelper h) {
    assertFluidMovement(h, Ic2Fluids.HOT_WATER);
  }

  @GameTest(template = "gametest/empty7x7x7")
  public static void weedExSourceAndFlowingAllowMovement(GameTestHelper h) {
    assertFluidMovement(h, Ic2Fluids.WEED_EX);
  }

  @GameTest(template = "gametest/empty7x7x7")
  public static void airSourceAndFlowingAllowMovement(GameTestHelper h) {
    assertFluidMovement(h, Ic2Fluids.AIR);
  }

  @GameTest(template = "gametest/empty7x7x7")
  public static void hydrogenSourceAndFlowingAllowMovement(GameTestHelper h) {
    assertFluidMovement(h, Ic2Fluids.HYDROGEN);
  }

  @GameTest(template = "gametest/empty7x7x7")
  public static void oxygenSourceAndFlowingAllowMovement(GameTestHelper h) {
    assertFluidMovement(h, Ic2Fluids.OXYGEN);
  }

  @GameTest(template = "gametest/empty7x7x7")
  public static void heavyWaterSourceAndFlowingAllowMovement(GameTestHelper h) {
    assertFluidMovement(h, Ic2Fluids.HEAVY_WATER);
  }

  private static final java.util.Set<String> GAS_NAMES = java.util.Set.of(
      "biogas", "superheated_steam", "steam", "air", "hydrogen", "oxygen");
  private static final java.util.Set<String> WATER_NAMES = java.util.Set.of(
      "distilled_water", "hot_water", "heavy_water");

  @GameTest(template = "gametest/empty7x7x7")
  public static void vanillaWaterMovementControl(GameTestHelper h) {
    movementSample(h, net.minecraft.world.level.material.Fluids.WATER, false, "forward");
    h.succeed();
  }

  @GameTest(template = "gametest/empty7x7x7")
  public static void vanillaLavaMovementControl(GameTestHelper h) {
    movementSample(h, net.minecraft.world.level.material.Fluids.LAVA, false, "forward");
    h.succeed();
  }

  @GameTest(template = TEMPLATE)
  public static void allRegisteredIc2FluidPairsAreCovered(GameTestHelper h) throws Exception {
    java.util.Set<Fluid> covered = new java.util.HashSet<>();
    for (var field : Ic2Fluids.class.getFields()) {
      if (field.getType() == ic2.core.fluid.EnvFluidHandler.FluidRefs.class) {
        var refs = (ic2.core.fluid.EnvFluidHandler.FluidRefs) field.get(null);
        covered.add(refs.still()); covered.add(refs.flowing());
      }
    }
    var actual = net.minecraft.core.registries.BuiltInRegistries.FLUID.stream()
        .filter(f -> net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(f)
            .getNamespace().equals("ic2")).collect(java.util.stream.Collectors.toSet());
    h.assertTrue(covered.equals(actual), "every registered IC2 source and flowing variant covered");
    h.assertValueEqual(actual.size(), 34, "17 IC2 fluids each have source and flowing variants");
    h.succeed();
  }

  private static void assertFluidMovement(
      GameTestHelper h, ic2.core.fluid.EnvFluidHandler.FluidRefs refs) {
    String name = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(refs.still()).getPath();
    for (Fluid fluid : new Fluid[] {refs.still(), refs.flowing()}) {
      for (boolean shallow : new boolean[] {true, false}) {
        for (String action : new String[] {"forward", "rise", "fall", "gravity"}) {
          movementSample(h, fluid, shallow, action);
        }
      }
    }
    var currentPlayer = immersedPlayer(h, refs.flowing(), true);
    BlockPos currentPos = new BlockPos(3,2,3);
    h.setBlock(currentPos.west(), refs.still().defaultFluidState().createLegacyBlock());
    h.setBlock(currentPos.east(), refs.flowing().defaultFluidState()
        .setValue(net.minecraft.world.level.material.FlowingFluid.LEVEL, 2)
        .setValue(net.minecraft.world.level.material.FlowingFluid.FALLING, false).createLegacyBlock());
    currentPlayer.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
    currentPlayer.baseTick();
    double push = currentPlayer.getDeltaMovement().x;
    System.out.println("[IC2 FLUID CURRENT] " + name + " x=" + push
        + " flow=" + h.getLevel().getFluidState(h.absolutePos(currentPos))
            .getFlow(h.getLevel(), h.absolutePos(currentPos))
        + " pushed=" + currentPlayer.isPushedByFluid(refs.still().getFluidType())
        + " center=" + h.getLevel().getFluidState(h.absolutePos(currentPos))
        + " west=" + h.getLevel().getFluidState(h.absolutePos(currentPos.west()))
        + " east=" + h.getLevel().getFluidState(h.absolutePos(currentPos.east()))
        + " above=" + h.getLevel().getFluidState(h.absolutePos(currentPos.above())));
    h.assertTrue(GAS_NAMES.contains(name) ? Math.abs(push)<1.0E-8 : push>1.0E-5,
        "liquid currents must push, gases must not: " + name + " x=" + push);
    var player = immersedPlayer(h, refs.still(), false);
    var type = refs.still().getFluidType();
    h.assertTrue(player.isInWater() == WATER_NAMES.contains(name),
        "only actual water varieties select water identity: " + name);
    if (GAS_NAMES.contains(name)) {
      h.assertFalse(type.canSwim(player), "gas must not select swimming: " + name);
      h.assertFalse(type.canDrownIn(player), "gas must not drown the player: " + name);
      h.assertFalse(type.canPushEntity(player), "gas must not push as a liquid current: " + name);
    }
    h.succeed();
  }

  private static net.minecraft.world.entity.player.Player immersedPlayer(
      GameTestHelper h, Fluid fluid, boolean shallow) {
    // Clearing first prevents hot-fluid/water reactions from contaminating the next sample.
    for (int x=1;x<=5;x++) for (int z=1;z<=5;z++) {
      h.setBlock(new BlockPos(x,0,z), Blocks.STONE);
      for(int y=1;y<=5;y++) h.setBlock(new BlockPos(x,y,z), Blocks.AIR);
    }
    var state = fluid.defaultFluidState();
    if (!state.isSource() && state.hasProperty(net.minecraft.world.level.material.FlowingFluid.LEVEL)) {
      state = state.setValue(net.minecraft.world.level.material.FlowingFluid.LEVEL, 7)
          .setValue(net.minecraft.world.level.material.FlowingFluid.FALLING, false);
    }
    for (int x=1;x<=5;x++) for (int z=1;z<=5;z++) {
      for(int y=1;y<=(shallow?2:4);y++) h.setBlock(new BlockPos(x,y,z), state.createLegacyBlock());
    }
    var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
    BlockPos at=h.absolutePos(new BlockPos(3,2,3));
    player.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);
    player.setYRot(0); player.setXRot(0);
    player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
    player.setOnGround(false);
    player.baseTick();
    h.assertTrue(player.getFluidTypeHeight(fluid.getFluidType())>.1,
        "fixture must really touch " + fluid + " shallow=" + shallow);
    return player;
  }

  private static void movementSample(GameTestHelper h, Fluid fluid, boolean shallow, String action) {
    var player = immersedPlayer(h, fluid, shallow);
    var start = player.position();
    var input = net.minecraft.world.phys.Vec3.ZERO;
    if (action.equals("forward")) input = new net.minecraft.world.phys.Vec3(0,0,1);
    if (action.equals("rise")) player.setDeltaMovement(0,.12,0);
    if (action.equals("fall")) player.setDeltaMovement(0,-.12,0);
    player.travel(input);
    if (action.equals("gravity")) player.travel(net.minecraft.world.phys.Vec3.ZERO);
    var moved = player.position().subtract(start);
    System.out.println("[IC2 FLUID PHYSICS] "
        + net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid)
        + " shallow="+shallow+" action="+action+" displacement="+moved
        + " water="+player.isInWater());
    h.assertTrue(action.equals("forward") ? moved.horizontalDistance()>.001
            : action.equals("rise") ? moved.y>.001 : moved.y<-.001,
        "native player movement must advance in " + fluid + " " + action
            + " shallow=" + shallow + ", got " + moved);
  }

  @GameTest(template = "gametest/empty7x7x7", timeoutTicks = 30)
  public static void constructionFoamKeepsSlowness(GameTestHelper h) {
    var entity = hazardVictim(h, Ic2Fluids.CONSTRUCTION_FOAM.still());
    h.succeedWhen(() -> h.assertTrue(entity.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS),
        "moving through construction foam must retain its slowness effect"));
  }

  @GameTest(template = "gametest/empty7x7x7", timeoutTicks = 30)
  public static void hotWaterKeepsRegenerationAndCooling(GameTestHelper h) {
    var entity = hazardVictim(h, Ic2Fluids.HOT_WATER.still());
    h.succeedWhen(() -> {
      h.assertTrue(entity.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION),
          "hot water must retain its regeneration effect");
      h.assertBlockPresent(Blocks.WATER, new BlockPos(3,2,3));
    });
  }

  @GameTest(template = "gametest/empty7x7x7", timeoutTicks = 30)
  public static void hotCoolantKeepsBurning(GameTestHelper h) {
    var entity = hazardVictim(h, Ic2Fluids.HOT_COOLANT.still());
    h.succeedWhen(() -> h.assertTrue(entity.isOnFire(), "hot coolant must still ignite entities"));
  }

  @GameTest(template = "gametest/empty7x7x7", timeoutTicks = 30)
  public static void steamKeepsBlindness(GameTestHelper h) {
    var entity = hazardVictim(h, Ic2Fluids.STEAM.still());
    h.succeedWhen(() -> h.assertTrue(entity.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS),
        "steam must retain its blindness effect"));
  }

  @GameTest(template = "gametest/empty7x7x7", timeoutTicks = 30)
  public static void superheatedSteamKeepsBlindness(GameTestHelper h) {
    var entity = hazardVictim(h, Ic2Fluids.SUPERHEATED_STEAM.still());
    h.succeedWhen(() -> h.assertTrue(entity.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS),
        "superheated steam must retain its blindness effect"));
  }

  @GameTest(template = "gametest/empty7x7x7", timeoutTicks = 30)
  public static void pahoehoeLavaKeepsHeatDamage(GameTestHelper h) {
    var entity = hazardVictim(h, Ic2Fluids.PAHOEHOE_LAVA.still());
    h.succeedWhen(() -> h.assertTrue(entity.isOnFire() && entity.getHealth()<entity.getMaxHealth(),
        "pahoehoe lava must still damage and ignite entities"));
  }

  private static net.minecraft.world.entity.LivingEntity hazardVictim(
      GameTestHelper h, Fluid fluid) {
    immersedPlayer(h, fluid, false);
    for(int x=0;x<=6;x++)for(int z=0;z<=6;z++)h.setBlock(new BlockPos(x,6,z),Blocks.STONE);
    var entity = h.spawn(net.minecraft.world.entity.EntityTypes.COW, new BlockPos(3,2,3));
    entity.setNoAi(true);
    return entity;
  }

  @GameTest(template = "gametest/empty7x7x7", timeoutTicks = 30)
  public static void waterVarietiesExtinguishWithoutChangingFluidIdentity(GameTestHelper h) {
    java.util.List<net.minecraft.world.entity.LivingEntity> entities = new java.util.ArrayList<>();
    int offset = 1;
    for (var refs : new ic2.core.fluid.EnvFluidHandler.FluidRefs[] {
        Ic2Fluids.DISTILLED_WATER, Ic2Fluids.HOT_WATER, Ic2Fluids.HEAVY_WATER}) {
      BlockPos pos = new BlockPos(offset,2,offset); offset += 2;
      h.setBlock(pos.below(),Blocks.STONE);
      h.setBlock(pos,refs.still().defaultFluidState().createLegacyBlock());
      h.assertTrue(h.getLevel().getFluidState(h.absolutePos(pos)).getType()==refs.still(),
          "water-like registration must retain the independent fluid identity");
      var entity=h.spawn(net.minecraft.world.entity.EntityTypes.COW,pos);
      entity.setNoAi(true); entity.igniteForSeconds(5); entities.add(entity);
    }
    h.succeedWhen(() -> {
      for (var entity:entities)h.assertFalse(entity.isOnFire(),
          "water varieties must extinguish burning entities through the native tick");
    });
  }

  @GameTest(template = "gametest/empty7x7x7")
  public static void gasesAroundHeadDoNotOverrideLiquidAtFeet(GameTestHelper h) {
    var pairs = new ic2.core.fluid.EnvFluidHandler.FluidRefs[][] {
      {Ic2Fluids.DISTILLED_WATER, Ic2Fluids.STEAM},
      {Ic2Fluids.COOLANT, Ic2Fluids.AIR},
      {Ic2Fluids.PAHOEHOE_LAVA, Ic2Fluids.BIOGAS}
    };
    for (var pair:pairs) {
      var control=immersedPlayer(h,pair[0].still(),true);
      control.travel(new net.minecraft.world.phys.Vec3(0,0,1));
      var expected=control.getDeltaMovement();
      var player=immersedPlayer(h,pair[0].still(),true);
      for(int x=1;x<=5;x++)for(int z=1;z<=5;z++)
        for(int y=3;y<=4;y++)h.setBlock(new BlockPos(x,y,z),
            pair[1].still().defaultFluidState().createLegacyBlock());
      player.baseTick(); player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
      h.assertTrue(player.getFluidTypeHeight(pair[0].still().getFluidType())>.1
          && player.getFluidTypeHeight(pair[1].still().getFluidType())>.1,
          "mixed-fluid fixture must touch liquid at feet and gas above");
      player.travel(new net.minecraft.world.phys.Vec3(0,0,1));
      h.assertTrue(player.getDeltaMovement().distanceTo(expected)<1.0E-7,
          "gas must not replace the liquid movement, expected=" + expected
              + " actual=" + player.getDeltaMovement());
    }
    h.succeed();
  }

  @GameTest(template = "gametest/empty7x7x7")
  public static void gasesAllowNormalGroundJump(GameTestHelper h) {
    for(var refs : new ic2.core.fluid.EnvFluidHandler.FluidRefs[] {Ic2Fluids.BIOGAS,
        Ic2Fluids.SUPERHEATED_STEAM,Ic2Fluids.STEAM,Ic2Fluids.AIR,Ic2Fluids.HYDROGEN,Ic2Fluids.OXYGEN}) {
      var player=immersedPlayer(h,refs.still(),false);
      var start=player.position();
      player.setOnGround(true); player.setJumping(true);
      player.aiStep();
      double rise=player.getY()-start.y;
      System.out.println("[IC2 GAS JUMP] "+refs.still()+" rise="+rise);
      h.assertTrue(rise>.1,"gas must allow a normal ground jump, not a tiny liquid stroke: "+refs.still());
    }
    h.succeed();
  }
}

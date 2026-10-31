package ic2.core.block;

import ic2.core.block.tileentity.Ic2TileEntityBlock;
import java.util.*;
import java.util.concurrent.locks.StampedLock;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public abstract class DynamicBeModel<T> implements DynamicBlockStateModel {
  protected final Ic2TileEntityBlock block;
  private final List<T> cache;
  private final int cacheSize;
  private final StampedLock cacheLock = new StampedLock();
  protected final BlockStateModelPart baseModel;
  private final BlockStateModelPart activeBaseModel;

  protected DynamicBeModel(Identifier id, ModelBaker baker) {
    Identifier blockId =
        Identifier.fromNamespaceAndPath(
            id.getNamespace(), id.getPath().substring(id.getPath().lastIndexOf('/') + 1));
    if (!(BuiltInRegistries.BLOCK.getValue(blockId) instanceof Ic2TileEntityBlock machine)) {
      throw new IllegalArgumentException("Invalid IC2 machine model " + id);
    }
    this.block = machine;
    Identifier backing = backing(id);
    this.baseModel = SimpleModelWrapper.bake(baker, backing, BlockModelRotation.IDENTITY);
    this.activeBaseModel =
        machine.canActive()
            ? SimpleModelWrapper.bake(
                baker, backing.withSuffix("_active"), BlockModelRotation.IDENTITY)
            : this.baseModel;
    this.cacheSize = (machine.facingProperty != null ? 6 : 1) * (machine.canActive() ? 2 : 1);
    this.cache = new ArrayList<>(Collections.nCopies(cacheSize, null));
  }

  public static Identifier backing(Identifier id) {
    return Identifier.fromNamespaceAndPath(
        id.getNamespace(), id.getPath().replace("block/be/", "block/"));
  }

  protected static BakedQuad rotateQuad(BakedQuad quad, int rot, boolean rotX) {
    rot &= 3;
    if (rot == 0) return quad;
    Vector3fc[] positions = new Vector3fc[4];
    int[] normals = new int[4];
    for (int v = 0; v < 4; v++) {
      Vector3f point = new Vector3f(quad.position(v));
      Vector3f normal = BakedNormals.unpack(quad.bakedNormals().normal(v), null);
      for (int i = 0; i < rot; i++) {
        if (rotX) {
          point.set(point.x(), point.z(), 1.0F - point.y());
          normal.set(normal.x(), normal.z(), -normal.y());
        } else {
          point.set(1.0F - point.z(), point.y(), point.x());
          normal.set(-normal.z(), normal.y(), normal.x());
        }
      }
      positions[v] = point;
      normals[v] = BakedNormals.pack(normal);
    }
    var m = quad.materialInfo();
    var material =
        new BakedQuad.MaterialInfo(
            m.sprite(),
            m.layer(),
            m.itemRenderType(),
            m.itemGlintRenderType(),
            m.itemGlintSpecialRenderType(),
            m.tintIndex(),
            m.shadeDirectionOverride() == null
                ? null
                : rotateFace(m.shadeDirectionOverride(), rot, rotX),
            m.lightEmission(),
            m.ambientOcclusion());
    return new BakedQuad(
        positions[0],
        positions[1],
        positions[2],
        positions[3],
        quad.packedUV0(),
        quad.packedUV1(),
        quad.packedUV2(),
        quad.packedUV3(),
        rotateFace(quad.direction(), rot, rotX),
        material,
        BakedNormals.of(normals[0], normals[1], normals[2], normals[3]),
        quad.bakedColors());
  }

  protected static Direction rotateFace(Direction face, int count, boolean rotX) {
    for (int i = 0; i < (count & 3); i++) {
      if (rotX && face.getAxis() != Axis.X) face = face.getClockWise(Axis.X);
      else if (!rotX && face.getAxis() != Axis.Y) face = face.getClockWise();
    }
    return face;
  }

  @Override
  public Material.Baked particleMaterial() {
    return baseModel.particleMaterial();
  }

  @Override
  public int materialFlags() {
    return baseModel.materialFlags() | activeBaseModel.materialFlags();
  }

  protected T getMesh(BlockState state, boolean active) {
    int idx;
    Direction facing;
    if (this.block.facingProperty != null) {
      facing = state.getValue(this.block.facingProperty);
      idx = facing.ordinal();
    } else {
      facing = Direction.NORTH;
      idx = 0;
    }

    if (active) {
      idx += this.cacheSize >>> 1;
    }

    long stamp = this.cacheLock.readLock();

    try {
      T ret = this.cache.get(idx);
      if (ret != null) {
        return ret;
      }
    } finally {
      this.cacheLock.unlock(stamp);
    }
    T ret =
        this.generateMesh(
            active ? this.activeBaseModel : this.baseModel,
            switch (facing) {
              case DOWN, EAST -> 1;
              case UP, WEST -> 3;
              case NORTH -> 0;
              case SOUTH -> 2;
            },
            facing.getAxis() == Axis.Y);
    stamp = this.cacheLock.readLock();

    try {
      T prev = this.cache.get(idx);
      if (prev != null) {
        return prev;
      }

      long newStamp = this.cacheLock.tryConvertToWriteLock(stamp);
      if (newStamp != 0L) {
        stamp = newStamp;
      } else {
        this.cacheLock.unlock(stamp);
        stamp = this.cacheLock.writeLock();
      }

      prev = this.cache.get(idx);
      if (prev != null) {
        return prev;
      }

      this.cache.set(idx, ret);
      return ret;
    } finally {
      this.cacheLock.unlock(stamp);
    }
  }

  protected abstract T generateMesh(BlockStateModelPart model, int rot, boolean rotX);
}

package ic2.forge.model;

import com.mojang.blaze3d.platform.NativeImage;
import ic2.core.item.tool.ItemObscurator;
import ic2.core.util.StackUtil;
import java.io.IOException;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/** A normal baked base item plus the original alpha-mask geometry, resolved per stack. */
public final class MaskOverlayItemModel implements ItemModel {
  private final Identifier baseModelLocation;
  private final Identifier maskTextureLocation;
  private final boolean scaleOverlay;
  private final float offset;
  private final ItemModel baseModel;
  private final ModelRenderProperties properties;
  private final Matrix4fc transformation;
  private List<OverlayQuad> overlayTemplates = List.of();
  private float uS, vS, uE, vE;

  MaskOverlayItemModel(
      MaskOverlayItemLoader definition, BakingContext context, Matrix4fc transformation) {
    this.baseModelLocation = definition.base();
    this.maskTextureLocation = definition.mask();
    this.scaleOverlay = definition.scaleOverlay();
    this.offset = definition.offset();
    this.transformation = new Matrix4f(transformation);
    this.baseModel =
        new CuboidItemModelWrapper.Unbaked(baseModelLocation, Optional.empty(), List.of())
            .bake(context, transformation);
    var resolved = context.blockModelBaker().getModel(baseModelLocation);
    this.properties =
        ModelRenderProperties.fromResolvedModel(
            context.blockModelBaker(), resolved, resolved.getTopTextureSlots());
    TextureAtlasSprite maskSprite =
        context
            .blockModelBaker()
            .materials()
            .get(new Material(maskTextureLocation), () -> "ic2:mask_overlay/" + baseModelLocation)
            .sprite();
    var resource =
        Minecraft.getInstance()
            .getResourceManager()
            .getResource(maskToResource(maskTextureLocation))
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Missing IC2 item overlay mask " + maskTextureLocation));
    try (var input = resource.open();
        NativeImage maskImage = NativeImage.read(input)) {
      int width = maskImage.getWidth();
      int height = maskImage.getHeight();
      List<Area> areas = searchAreas(readMask(maskImage), width);

      float texWidth = maskSprite.contents().width() / 16f;
      float texHeight = maskSprite.contents().height() / 16f;
      float zF = (7.5f - this.offset) / 16f;
      float zB = (8.5f + this.offset) / 16f;
      List<OverlayQuad> templates = new ArrayList<>(areas.size());

      for (Area area : areas) {
        float xS = area.x / (float) width * texWidth;
        float yS = 1f - area.y / (float) height * texHeight;
        float xE = (area.x + area.width) / (float) width * texWidth;
        float yE = 1f - (area.y + area.height) / (float) height * texHeight;
        BakedQuad frontQuad = createQuad(xS, yS, xE, yE, zF, Direction.NORTH, maskSprite);
        BakedQuad backQuad = createQuad(xS, yS, xE, yE, zB, Direction.SOUTH, maskSprite);
        templates.add(new OverlayQuad(frontQuad, backQuad));
      }

      this.overlayTemplates = templates;
      if (this.scaleOverlay) {
        calcOverlayBounds(areas, width, height, texWidth, texHeight);
      } else {
        this.uS = this.vS = 0f;
        this.uE = this.vE = 1f;
      }
    } catch (IOException e) {
      throw new IllegalStateException(
          "Cannot read IC2 item overlay mask " + maskTextureLocation, e);
    }
  }

  @Override
  public void update(
      ItemStackRenderState output,
      ItemStack item,
      ItemModelResolver resolver,
      ItemDisplayContext displayContext,
      ClientLevel level,
      ItemOwner owner,
      int seed) {
    baseModel.update(output, item, resolver, displayContext, level, owner, seed);
    CompoundTag tag = StackUtil.getTag(item);
    if (tag == null || overlayTemplates.isEmpty()) return;
    BlockState state = ItemObscurator.getState(tag);
    Direction side = ItemObscurator.getSide(tag);
    int[] colors = ItemObscurator.getColorMultipliers(tag);
    if (state == null || side == null || colors == null) return;
    var info = ItemObscurator.getRenderInfo(state, side);
    if (info == null || colors.length * 4 != info.uvs.length) return;
    var quads = buildOverlayQuads(info, colors);
    if (quads.isEmpty()) return;
    var layer = output.newLayer();
    properties.applyToLayer(layer, displayContext);
    layer.setLocalTransform(transformation);
    layer.setQuads(ItemQuads.split(quads));
    layer.setExtents(() -> CuboidItemModelWrapper.computeExtents(quads));
    output.appendModelIdentityElement(tag);
    if (quads.stream()
        .anyMatch(quad -> (quad.materialInfo().flags() & BakedQuad.FLAG_ANIMATED) != 0))
      output.setAnimated();
  }

  private static Identifier maskToResource(Identifier maskLocation) {
    return Identifier.fromNamespaceAndPath(
        maskLocation.getNamespace(), "textures/" + maskLocation.getPath() + ".png");
  }

  private static BakedQuad createQuad(
      float xS, float yS, float xE, float yE, float z, Direction face, TextureAtlasSprite sprite) {
    int[] data = new int[32];
    int normal = computeNormal(face);
    int color = 0xFFFFFFFF;

    data[0] = Float.floatToRawIntBits(xS);
    data[1] = Float.floatToRawIntBits(yS);
    data[2] = Float.floatToRawIntBits(z);
    data[3] = color;
    data[4] = Float.floatToRawIntBits(0f);
    data[5] = Float.floatToRawIntBits(0f);
    data[6] = LightCoordsUtil.FULL_BRIGHT;
    data[7] = normal;

    data[8] = Float.floatToRawIntBits(xE);
    data[9] = Float.floatToRawIntBits(yS);
    data[10] = Float.floatToRawIntBits(z);
    data[11] = color;
    data[12] = Float.floatToRawIntBits(1f);
    data[13] = Float.floatToRawIntBits(0f);
    data[14] = LightCoordsUtil.FULL_BRIGHT;
    data[15] = normal;

    data[16] = Float.floatToRawIntBits(xE);
    data[17] = Float.floatToRawIntBits(yE);
    data[18] = Float.floatToRawIntBits(z);
    data[19] = color;
    data[20] = Float.floatToRawIntBits(1f);
    data[21] = Float.floatToRawIntBits(1f);
    data[22] = LightCoordsUtil.FULL_BRIGHT;
    data[23] = normal;

    data[24] = Float.floatToRawIntBits(xS);
    data[25] = Float.floatToRawIntBits(yE);
    data[26] = Float.floatToRawIntBits(z);
    data[27] = color;
    data[28] = Float.floatToRawIntBits(0f);
    data[29] = Float.floatToRawIntBits(1f);
    data[30] = LightCoordsUtil.FULL_BRIGHT;
    data[31] = normal;

    if (face == Direction.SOUTH) {
      for (int v = 0; v < 2; v++) {
        for (int c = 0; c < 8; c++) {
          int swap = data[v * 8 + c];
          data[v * 8 + c] = data[(3 - v) * 8 + c];
          data[(3 - v) * 8 + c] = swap;
        }
      }
    }
    return ic2.compat.client.QuadCompat.fromVertices(data, -1, face, sprite, false);
  }

  private static int computeNormal(Direction face) {
    return net.neoforged.neoforge.client.model.quad.BakedNormals.pack(
        face.getStepX(), face.getStepY(), face.getStepZ());
  }

  private static BitSet readMask(NativeImage img) {
    int w = img.getWidth();
    int h = img.getHeight();
    BitSet ret = new BitSet(w * h);

    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        if ((img.getPixel(x, y) >>> 24) > 128) {
          ret.set(y * w + x);
        }
      }
    }

    return ret;
  }

  private static List<Area> searchAreas(BitSet pixels, int width) {
    List<Area> ret = new ArrayList<>();
    int idx = 0;

    while ((idx = pixels.nextSetBit(idx)) != -1) {
      int y = idx / width;
      int x = idx - y * width;
      int areaWidth = Math.min(width - x, pixels.nextClearBit(idx + 1) - idx);
      int areaHeight = 1;

      for (int nextLineIdx = idx + width;
          pixels.get(nextLineIdx)
              && pixels.nextClearBit(nextLineIdx + 1) >= nextLineIdx + areaWidth;
          nextLineIdx += width) {
        pixels.clear(nextLineIdx, nextLineIdx + areaWidth);
        areaHeight++;
      }

      ret.add(new Area(x, y, areaWidth, areaHeight));
      idx += areaWidth;
    }

    return ret;
  }

  private List<BakedQuad> buildOverlayQuads(
      ItemObscurator.ObscuredRenderInfo renderInfo, int[] colorMultipliers) {
    int textureCount = renderInfo.uvs.length / 4;
    List<BakedQuad> result = new ArrayList<>(this.overlayTemplates.size() * 2);

    for (int qi = 0; qi < this.overlayTemplates.size(); qi++) {
      OverlayQuad template = this.overlayTemplates.get(qi);
      int ti = qi % textureCount;
      TextureAtlasSprite sprite = renderInfo.sprites[ti];
      int colorMul = colorMultipliers[ti];

      float quS = renderInfo.uvs[ti * 4];
      float qvS = renderInfo.uvs[ti * 4 + 1];
      float quE = renderInfo.uvs[ti * 4 + 2];
      float qvE = renderInfo.uvs[ti * 4 + 3];

      float spriteURange = sprite.getU1() - sprite.getU0();
      float spriteVRange = sprite.getV1() - sprite.getV0();
      float mappedUS =
          this.uS
              + (spriteURange != 0.0f ? (quS - sprite.getU0()) / spriteURange : 0.0f)
                  * (this.uE - this.uS);
      float mappedVS =
          this.vS
              + (spriteVRange != 0.0f ? (qvS - sprite.getV0()) / spriteVRange : 0.0f)
                  * (this.vE - this.vS);
      float mappedUE =
          this.uS
              + (spriteURange != 0.0f ? (quE - sprite.getU0()) / spriteURange : 0.0f)
                  * (this.uE - this.uS);
      float mappedVE =
          this.vS
              + (spriteVRange != 0.0f ? (qvE - sprite.getV0()) / spriteVRange : 0.0f)
                  * (this.vE - this.vS);

      result.add(
          OverlayRenderUtil.remapItemOverlayQuad(
              template.frontQuad(),
              sprite,
              sprite.getU(mappedUS),
              sprite.getV(mappedVS),
              sprite.getU(mappedUE),
              sprite.getV(mappedVE),
              colorMul));
      result.add(
          OverlayRenderUtil.remapItemOverlayQuad(
              template.backQuad(),
              sprite,
              sprite.getU(mappedUS),
              sprite.getV(mappedVS),
              sprite.getU(mappedUE),
              sprite.getV(mappedVE),
              colorMul));
    }

    return result;
  }

  private void calcOverlayBounds(
      List<Area> areas, int width, int height, float texWidth, float texHeight) {
    if (areas.isEmpty()) {
      this.uS = this.vS = 0;
      this.uE = this.vE = 1;
      return;
    }
    int minX = Integer.MAX_VALUE;
    int minY = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE;
    int maxY = Integer.MIN_VALUE;

    for (Area area : areas) {
      if (area.x < minX) minX = area.x;
      if (area.y < minY) minY = area.y;
      if (area.x + area.width > maxX) maxX = area.x + area.width;
      if (area.y + area.height > maxY) maxY = area.y + area.height;
    }

    this.uS = minX / (float) width * texWidth;
    this.vS = 1f - maxY / (float) height * texHeight;
    this.uE = maxX / (float) width * texWidth;
    this.vE = 1f - minY / (float) height * texHeight;
  }

  private record Area(int x, int y, int width, int height) {}

  private record OverlayQuad(BakedQuad frontQuad, BakedQuad backQuad) {}
}

package ic2.core.model;

import ic2.core.block.comp.Obscuration;
import ic2.core.block.tileentity.Ic2TileEntity;
import ic2.core.block.tileentity.TileEntityWall;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.model.data.ModelProperty;

/** Immutable main-thread capture used by chunk-meshing workers. */
public record Ic2ModelSnapshot(boolean active, DyeColor wallColor, List<Face> obscuration) {
  public static final ModelProperty<Ic2ModelSnapshot> PROPERTY = new ModelProperty<>();

  public static Ic2ModelSnapshot capture(Ic2TileEntity tile) {
    var component = tile.getComponent(Obscuration.class);
    var source = component == null ? null : component.getRenderState();
    List<Face> faces = new ArrayList<>(6);
    for (int i = 0; i < 6; i++) {
      var data = source == null ? null : source[i];
      faces.add(
          data == null
              ? null
              : new Face(
                  data.state(),
                  data.variant(),
                  data.side(),
                  java.util.Arrays.stream(data.colorMultipliers()).boxed().toList()));
    }
    return new Ic2ModelSnapshot(
        tile.getActive(),
        tile instanceof TileEntityWall wall
            ? wall.getColor()
            : ic2.core.block.misc.WallBlock.DEFAULT_COLOR,
        Collections.unmodifiableList(faces));
  }

  public record Face(BlockState state, String variant, Direction side, List<Integer> colors) {
    public Obscuration.ObscurationData data() {
      return new Obscuration.ObscurationData(
          state, variant, side, colors.stream().mapToInt(Integer::intValue).toArray());
    }
  }
}

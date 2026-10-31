package twilightforest.datagen.data;

import net.minecraft.data.advancements.AdvancementProvider;
import java.util.List;

public class TFAdvancementProvider extends AdvancementProvider {
    public TFAdvancementProvider() { super(List.of(TFAdvancementGenerator::new)); }
}

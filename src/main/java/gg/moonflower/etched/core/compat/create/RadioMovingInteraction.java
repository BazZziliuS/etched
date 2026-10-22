package gg.moonflower.etched.core.compat.create;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import gg.moonflower.etched.common.menu.RadioMenu;
import gg.moonflower.etched.core.Etched;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

/**
 * Opens the radio menu for a radio on a moving contraption so the station can be changed without stopping.
 */
public class RadioMovingInteraction extends MovingInteractionBehaviour {

    private static final Component CONTAINER_TITLE = Component.translatable("container." + Etched.MOD_ID + ".radio");

    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos, AbstractContraptionEntity contraptionEntity) {
        if (player.level().isClientSide()) {
            return true;
        }

        StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null) {
            return false;
        }

        String url = info.nbt() != null ? info.nbt().getString("Url") : "";
        player.openMenu(new SimpleMenuProvider((menuId, playerInventory, menuPlayer) -> new RadioMenu(menuId,
                user -> CreateCompat.canUseRadio(contraptionEntity, localPos, user),
                newUrl -> CreateCompat.setRadioUrl(contraptionEntity, localPos, newUrl)), CONTAINER_TITLE), buf -> buf.writeUtf(url));
        return true;
    }
}

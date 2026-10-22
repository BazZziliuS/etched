package gg.moonflower.etched.core.compat.create;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import gg.moonflower.etched.common.block.RadioBlock;
import gg.moonflower.etched.common.network.play.ClientboundSetContraptionRadioUrlPacket;
import gg.moonflower.etched.core.Etched;
import gg.moonflower.etched.core.registry.EtchedBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.commons.lang3.tuple.MutablePair;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Lets radios keep playing, and be retuned, while mounted on Create contraptions such as trains.
 */
public class CreateCompat {

    private static final double MAX_USE_DISTANCE_SQ = 8.0 * 8.0;
    private static final Field UPDATE_TAGS = findUpdateTags();

    private static @Nullable Field findUpdateTags() {
        try {
            Field field = Contraption.class.getDeclaredField("updateTags");
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            Etched.LOGGER.warn("Failed to access Create contraption update tags, players loading a contraption may hear its previous radio station", e);
            return null;
        }
    }

    public static void load() {
        MovementBehaviour.REGISTRY.register(EtchedBlocks.RADIO.get(), new RadioMovementBehaviour());
        MovingInteractionBehaviour.REGISTRY.register(EtchedBlocks.RADIO.get(), new RadioMovingInteraction());
    }

    /**
     * @return The URL the radio should currently play or <code>null</code> if it is silent
     */
    public static @Nullable String getPlayingUrl(BlockState state, @Nullable CompoundTag blockEntityData) {
        if (state.hasProperty(RadioBlock.POWERED) && state.getValue(RadioBlock.POWERED)) {
            return null;
        }
        if (blockEntityData == null || !blockEntityData.contains("Url", Tag.TAG_STRING)) {
            return null;
        }
        String url = blockEntityData.getString("Url");
        return StringUtil.isNullOrEmpty(url) ? null : url;
    }

    public static boolean canUseRadio(AbstractContraptionEntity entity, BlockPos localPos, Player player) {
        if (!entity.isAlive() || entity.getContraption() == null) {
            return false;
        }
        StructureBlockInfo info = entity.getContraption().getBlocks().get(localPos);
        return info != null && info.state().is(EtchedBlocks.RADIO.get()) && player.distanceToSqr(entity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0F)) <= MAX_USE_DISTANCE_SQ;
    }

    /**
     * Changes the station of a mounted radio on the server and tells every client tracking the contraption.
     */
    public static void setRadioUrl(AbstractContraptionEntity entity, BlockPos localPos, String url) {
        if (entity.isAlive() && applyRadioUrl(entity.getContraption(), localPos, url)) {
            PacketDistributor.sendToPlayersTrackingEntity(entity, new ClientboundSetContraptionRadioUrlPacket(entity.getId(), localPos, url));
        }
    }

    /**
     * Writes the URL into the stored block entity data so it is kept when the contraption is saved or disassembled.
     */
    static boolean applyRadioUrl(@Nullable Contraption contraption, BlockPos localPos, String url) {
        if (contraption == null) {
            return false;
        }

        StructureBlockInfo info = contraption.getBlocks().get(localPos);
        if (info == null || !info.state().is(EtchedBlocks.RADIO.get())) {
            return false;
        }

        if (info.nbt() == null) {
            info = new StructureBlockInfo(info.pos(), info.state(), new CompoundTag());
            contraption.getBlocks().put(localPos, info);
        }
        info.nbt().putString("Url", url);

        // Clients that start tracking the contraption later are sent the update tags instead of the block data
        CompoundTag updateTag = getUpdateTag(contraption, localPos);
        if (updateTag != null) {
            updateTag.putString("Url", url);
        }

        // Actors may hold their own copy of the data
        MutablePair<StructureBlockInfo, MovementContext> actor = contraption.getActorAt(localPos);
        if (actor != null) {
            if (actor.left.nbt() != null) {
                actor.left.nbt().putString("Url", url);
            }
            MovementContext context = actor.right;
            if (context != null) {
                if (context.blockEntityData == null) {
                    context.blockEntityData = info.nbt();
                }
                context.blockEntityData.putString("Url", url);
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static @Nullable CompoundTag getUpdateTag(Contraption contraption, BlockPos localPos) {
        if (UPDATE_TAGS == null) {
            return null;
        }
        try {
            return ((Map<BlockPos, CompoundTag>) UPDATE_TAGS.get(contraption)).get(localPos);
        } catch (Exception e) {
            return null;
        }
    }
}

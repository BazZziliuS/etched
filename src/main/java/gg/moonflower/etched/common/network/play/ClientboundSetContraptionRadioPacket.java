package gg.moonflower.etched.common.network.play;

import gg.moonflower.etched.core.Etched;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.ApiStatus;

/**
 * @param entityId The id of the contraption entity carrying the radio
 * @param localPos The position of the radio inside the contraption
 * @param url      The new URL of the radio
 * @param volume   The new volume of the radio from 0 to 1
 * @param radius   The new radius of the radio in blocks
 */
@ApiStatus.Internal
public record ClientboundSetContraptionRadioPacket(int entityId, BlockPos localPos, String url, float volume, int radius) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClientboundSetContraptionRadioPacket> TYPE = new CustomPacketPayload.Type<>(Etched.etchedPath("set_contraption_radio"));
    public static final StreamCodec<ByteBuf, ClientboundSetContraptionRadioPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ClientboundSetContraptionRadioPacket::entityId,
            BlockPos.STREAM_CODEC,
            ClientboundSetContraptionRadioPacket::localPos,
            ByteBufCodecs.STRING_UTF8,
            ClientboundSetContraptionRadioPacket::url,
            ByteBufCodecs.FLOAT,
            ClientboundSetContraptionRadioPacket::volume,
            ByteBufCodecs.VAR_INT,
            ClientboundSetContraptionRadioPacket::radius,
            ClientboundSetContraptionRadioPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

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
 */
@ApiStatus.Internal
public record ClientboundSetContraptionRadioUrlPacket(int entityId, BlockPos localPos, String url) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClientboundSetContraptionRadioUrlPacket> TYPE = new CustomPacketPayload.Type<>(Etched.etchedPath("set_contraption_radio_url"));
    public static final StreamCodec<ByteBuf, ClientboundSetContraptionRadioUrlPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ClientboundSetContraptionRadioUrlPacket::entityId,
            BlockPos.STREAM_CODEC,
            ClientboundSetContraptionRadioUrlPacket::localPos,
            ByteBufCodecs.STRING_UTF8,
            ClientboundSetContraptionRadioUrlPacket::url,
            ClientboundSetContraptionRadioUrlPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

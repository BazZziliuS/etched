package gg.moonflower.etched.core.compat.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import gg.moonflower.etched.api.sound.SoundTracker;
import gg.moonflower.etched.common.network.play.ClientboundSetContraptionRadioUrlPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class CreateCompatClient {

    static void tickRadio(MovementContext context) {
        String url = context.disabled ? null : CreateCompat.getPlayingUrl(context.state, context.blockEntityData);
        SoundManager soundManager = Minecraft.getInstance().getSoundManager();

        PlayingRadio playing = context.temporaryData instanceof PlayingRadio radio ? radio : null;
        if (playing != null ? Objects.equals(playing.url, url) : url == null) {
            if (playing != null && playing.sound != null && context.position != null && context.world.random.nextInt(10) == 0 && soundManager.isActive(playing.sound)) {
                context.world.addParticle(ParticleTypes.NOTE, context.position.x, context.position.y + 0.2, context.position.z, context.world.random.nextInt(25) / 24D, 0, 0);
            }
            return;
        }

        if (playing != null && playing.sound != null) {
            soundManager.stop(playing.sound);
        }

        PlayingRadio next = new PlayingRadio(url);
        context.temporaryData = next;
        if (url == null) {
            return;
        }

        AbstractContraptionEntity entity = context.contraption.entity;
        BlockPos localPos = context.localPos;
        // The sound stops itself once the contraption is gone or the radio was retuned
        next.sound = SoundTracker.getMovingRadio(url, () -> entity.isAlive() && context.temporaryData == next ? entity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0F) : null);
        if (next.sound != null) {
            soundManager.play(next.sound);
        }
    }

    public static void handleSetRadioUrl(ClientboundSetContraptionRadioUrlPacket pkt) {
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.getEntity(pkt.entityId()) instanceof AbstractContraptionEntity entity) {
            CreateCompat.applyRadioUrl(entity.getContraption(), pkt.localPos(), pkt.url());
        }
    }

    private static class PlayingRadio {

        private final String url;
        private SoundInstance sound;

        private PlayingRadio(@Nullable String url) {
            this.url = url;
        }
    }
}

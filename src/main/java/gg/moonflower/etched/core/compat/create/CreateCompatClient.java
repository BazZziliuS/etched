package gg.moonflower.etched.core.compat.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import gg.moonflower.etched.api.record.PlayableRecord;
import gg.moonflower.etched.api.sound.SoundTracker;
import gg.moonflower.etched.api.sound.StopListeningSound;
import gg.moonflower.etched.common.blockentity.AlbumJukeboxBlockEntity;
import gg.moonflower.etched.common.blockentity.RadioBlockEntity;
import gg.moonflower.etched.common.network.play.ClientboundSetContraptionRadioPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CreateCompatClient {

    private static final int ALBUM_JUKEBOX_SIZE = 9;

    static void tickRadio(MovementContext context) {
        String url = context.disabled ? null : CreateCompat.getPlayingUrl(context.state, context.blockEntityData);
        SoundManager soundManager = Minecraft.getInstance().getSoundManager();

        PlayingRadio playing = context.temporaryData instanceof PlayingRadio radio ? radio : null;
        if (playing != null ? Objects.equals(playing.url, url) : url == null) {
            if (playing != null) {
                applyRadioSettings(context, playing.sound);
                spawnNotes(context, playing.sound, 0.2);
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
            applyRadioSettings(context, next.sound);
            soundManager.play(next.sound);
        }
    }

    static void tickJukebox(MovementContext context, boolean album) {
        CompoundTag data = context.blockEntityData;
        boolean powered = context.state.hasProperty(BlockStateProperties.POWERED) && context.state.getValue(BlockStateProperties.POWERED);
        Tag discs = context.disabled || powered || data == null ? null : data.get(album ? "Items" : "RecordItem");

        RecordPlayer playing = context.temporaryData instanceof RecordPlayer player ? player : null;
        if (playing != null ? Objects.equals(playing.discsTag, discs) : discs == null) {
            if (playing != null) {
                applyJukeboxSettings(context, playing.sound, album);
                spawnNotes(context, playing.sound, 0.7);
            }
            return;
        }

        if (playing != null) {
            playing.stop();
        }

        RecordPlayer next = new RecordPlayer(context, discs != null ? discs.copy() : null, album);
        context.temporaryData = next;
        next.play();
    }

    private static void applyRadioSettings(MovementContext context, @Nullable SoundInstance sound) {
        SoundTracker.setSoundSettings(sound, CreateCompat.getVolume(context.blockEntityData), CreateCompat.getRadius(context.blockEntityData, RadioBlockEntity.DEFAULT_RADIUS), RadioBlockEntity.DEFAULT_RADIUS);
    }

    private static void applyJukeboxSettings(MovementContext context, @Nullable SoundInstance sound, boolean album) {
        // Vanilla jukeboxes have no settings, so they always play like a default album jukebox
        int radius = album ? CreateCompat.getRadius(context.blockEntityData, AlbumJukeboxBlockEntity.DEFAULT_RADIUS) : AlbumJukeboxBlockEntity.DEFAULT_RADIUS;
        SoundTracker.setSoundSettings(sound, album ? CreateCompat.getVolume(context.blockEntityData) : 1.0F, radius, AlbumJukeboxBlockEntity.DEFAULT_RADIUS);
    }

    private static void spawnNotes(MovementContext context, @Nullable SoundInstance sound, double height) {
        if (sound != null && context.position != null && context.world.random.nextInt(10) == 0 && Minecraft.getInstance().getSoundManager().isActive(sound)) {
            context.world.addParticle(ParticleTypes.NOTE, context.position.x, context.position.y + height, context.position.z, context.world.random.nextInt(25) / 24D, 0, 0);
        }
    }

    public static void handleSetRadio(ClientboundSetContraptionRadioPacket pkt) {
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.getEntity(pkt.entityId()) instanceof AbstractContraptionEntity entity) {
            CreateCompat.applyRadio(entity.getContraption(), pkt.localPos(), pkt.url(), pkt.volume(), pkt.radius());
        }
    }

    private static class PlayingRadio {

        private final String url;
        private SoundInstance sound;

        private PlayingRadio(@Nullable String url) {
            this.url = url;
        }
    }

    /**
     * Plays the discs of a mounted jukebox track by track, the same way placed jukeboxes do.
     */
    private static class RecordPlayer {

        private final MovementContext context;
        private final Tag discsTag;
        private final List<ItemStack> discs;
        private final boolean loop;
        private int index;
        private int track;
        private SoundInstance sound;

        private RecordPlayer(MovementContext context, @Nullable Tag discsTag, boolean album) {
            this.context = context;
            this.discsTag = discsTag;
            this.loop = album;

            HolderLookup.Provider registries = context.world.registryAccess();
            if (discsTag == null) {
                this.discs = List.of();
            } else if (album) {
                NonNullList<ItemStack> items = NonNullList.withSize(ALBUM_JUKEBOX_SIZE, ItemStack.EMPTY);
                CompoundTag container = new CompoundTag();
                container.put("Items", discsTag);
                ContainerHelper.loadAllItems(container, items, registries);
                this.discs = items;
            } else {
                this.discs = discsTag instanceof CompoundTag tag ? List.of(ItemStack.parseOptional(registries, tag)) : List.of();
            }
        }

        private @Nullable Vec3 getPosition() {
            AbstractContraptionEntity entity = this.context.contraption.entity;
            return entity.isAlive() && this.context.temporaryData == this ? entity.toGlobalVector(Vec3.atCenterOf(this.context.localPos), 1.0F) : null;
        }

        private void play() {
            HolderLookup.Provider registries = this.context.world.registryAccess();
            int attempts = this.discs.size();
            for (ItemStack disc : this.discs) {
                attempts += PlayableRecord.getTrackCount(registries, disc);
            }

            // Skip empty slots and tracks that can't be played, giving up once everything was tried
            for (int i = 0; i <= attempts; i++) {
                if (this.index >= this.discs.size()) {
                    if (!this.loop || this.discs.isEmpty()) {
                        break;
                    }
                    this.index = 0;
                }

                ItemStack disc = this.discs.get(this.index);
                if (!PlayableRecord.isPlayableRecord(disc) || this.track >= PlayableRecord.getTrackCount(registries, disc)) {
                    this.index++;
                    this.track = 0;
                    continue;
                }

                Optional<SoundInstance> sound = PlayableRecord.createMovingSound(disc, registries, this::getPosition, this.track, 16);
                if (sound.isPresent()) {
                    this.sound = StopListeningSound.create(sound.get(), () -> Minecraft.getInstance().tell(this::next));
                    applyJukeboxSettings(this.context, this.sound, this.loop);
                    Minecraft.getInstance().getSoundManager().play(this.sound);
                    return;
                }
                this.track++;
            }
            this.sound = null;
        }

        private void next() {
            if (this.getPosition() == null) {
                return;
            }
            this.track++;
            this.play();
        }

        private void stop() {
            if (this.sound != null) {
                if (this.sound instanceof StopListeningSound listening) {
                    listening.stopListening();
                }
                Minecraft.getInstance().getSoundManager().stop(this.sound);
                this.sound = null;
            }
        }
    }
}

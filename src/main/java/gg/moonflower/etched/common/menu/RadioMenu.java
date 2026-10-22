package gg.moonflower.etched.common.menu;

import gg.moonflower.etched.common.blockentity.RadioBlockEntity;
import gg.moonflower.etched.core.registry.EtchedBlocks;
import gg.moonflower.etched.core.registry.EtchedMenus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author Ocelot
 */
public class RadioMenu extends AbstractContainerMenu implements UrlMenu {

    private final Predicate<Player> validator;
    private final Consumer<String> urlSetter;
    private String initialUrl;

    public RadioMenu(int id, String url) {
        this(id, ContainerLevelAccess.NULL);
        this.initialUrl = url;
    }

    public RadioMenu(int id, ContainerLevelAccess access) {
        this(id, player -> stillValid(access, player, EtchedBlocks.RADIO.get()), url -> access.execute((level, pos) -> {
            if (level.getBlockEntity(pos) instanceof RadioBlockEntity be) {
                be.setUrl(url);
            }
        }));
    }

    /**
     * Creates a radio menu for a radio that is not a placed block, such as one mounted on a moving contraption.
     *
     * @param id        The id of the menu
     * @param validator Whether the player can still use the menu
     * @param urlSetter Applies the URL chosen by the player
     */
    public RadioMenu(int id, Predicate<Player> validator, Consumer<String> urlSetter) {
        super(EtchedMenus.RADIO_MENU.get(), id);
        this.validator = validator;
        this.urlSetter = urlSetter;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.validator.test(player);
    }

    @Override
    public void setUrl(String url) {
        this.urlSetter.accept(url);
    }

    /**
     * @return The original URL inside the radio before opening the menu
     */
    public String getInitialUrl() {
        return this.initialUrl;
    }
}
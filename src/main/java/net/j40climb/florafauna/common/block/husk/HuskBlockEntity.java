package net.j40climb.florafauna.common.block.husk;

import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Block entity for the Symbiotic Husk.
 * Stores the full player inventory (41 slots) on death.
 *
 * Slot layout:
 * - 0-35: Main inventory (hotbar 0-8 + main 9-35)
 * - 36-39: Armor slots (feet, legs, chest, head)
 * - 40: Offhand slot
 */
public class HuskBlockEntity extends BlockEntity {
    /**
     * Total slots: 36 main inventory + 4 armor + 1 offhand = 41
     */
    public static final int TOTAL_SLOTS = 41;

    /**
     * Slot indices for inventory mapping
     */
    public static final int MAIN_INV_START = 0;
    public static final int MAIN_INV_END = 36;
    public static final int ARMOR_START = 36;
    public static final int ARMOR_END = 40;
    public static final int OFFHAND_SLOT = 40;

    public final ItemStacksResourceHandler handler = new ItemStacksResourceHandler(TOTAL_SLOTS) {
        @Override
        public boolean isValid(int index, ItemResource resource) {
            return true; // Accept any item in any slot
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            super.onContentsChanged(index, previousContents);
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
                checkAndTransitionToBroken();
            }
        }

        @Override
        protected int getCapacity(int index, ItemResource resource) {
            return 64;
        }
    };

    @Nullable
    private UUID ownerUUID;

    public HuskBlockEntity(BlockPos pos, BlockState blockState) {
        super(FloraFaunaRegistry.HUSK_BE.get(), pos, blockState);
    }

    /**
     * Set the owner of this husk.
     */
    public void setOwner(UUID uuid) {
        this.ownerUUID = uuid;
        setChanged();
    }

    /**
     * Get the owner UUID.
     */
    @Nullable
    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    /**
     * Check if the given player is the owner of this husk.
     */
    public boolean isOwner(Player player) {
        return ownerUUID != null && ownerUUID.equals(player.getUUID());
    }

    /**
     * Populate this husk with all items from a player's inventory.
     * Called when the player dies.
     */
    public void populateFromPlayerInventory(Player player) {
        Inventory inv = player.getInventory();

        // Copy main inventory (slots 0-35)
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                handler.set(i, ItemResource.of(stack), stack.getCount());
            }
        }

        // Copy armor (slots 36-39 in our storage)
        // Use EquipmentSlot mapping: 0=feet, 1=legs, 2=chest, 3=head
        EquipmentSlot[] armorSlots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
        for (int i = 0; i < 4; i++) {
            ItemStack armorStack = player.getItemBySlot(armorSlots[i]);
            if (!armorStack.isEmpty()) {
                handler.set(ARMOR_START + i, ItemResource.of(armorStack), armorStack.getCount());
            }
        }

        // Copy offhand (slot 40)
        ItemStack offhandStack = player.getOffhandItem();
        if (!offhandStack.isEmpty()) {
            handler.set(OFFHAND_SLOT, ItemResource.of(offhandStack), offhandStack.getCount());
        }

        setChanged();
    }

    /**
     * Transfer items from this husk back to the player.
     * Items that don't fit remain in the husk.
     */
    public void transferItemsToPlayer(Player player) {
        Inventory inv = player.getInventory();

        // Transfer main inventory first (slots 0-35)
        for (int i = 0; i < 36; i++) {
            ItemStack stack = handler.getResource(i).toStack(handler.getAmountAsInt(i));
            if (!stack.isEmpty()) {
                // Try to put in the same slot
                if (inv.getItem(i).isEmpty()) {
                    inv.setItem(i, stack);
                    clearSlot(i);
                } else if (inv.add(stack)) {
                    // Added elsewhere
                    clearSlot(i);
                }
                // If can't add, it stays in the husk
            }
        }

        // Transfer armor (try to equip in same slot)
        // Armor slots map: 0=feet, 1=legs, 2=chest, 3=head
        EquipmentSlot[] armorSlots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
        for (int i = 0; i < 4; i++) {
            ItemStack armorStack = handler.getResource(ARMOR_START + i).toStack(handler.getAmountAsInt(ARMOR_START + i));
            if (!armorStack.isEmpty()) {
                if (player.getItemBySlot(armorSlots[i]).isEmpty()) {
                    player.setItemSlot(armorSlots[i], armorStack);
                    clearSlot(ARMOR_START + i);
                } else if (inv.add(armorStack)) {
                    clearSlot(ARMOR_START + i);
                }
            }
        }

        // Transfer offhand
        ItemStack offhandStack = handler.getResource(OFFHAND_SLOT).toStack(handler.getAmountAsInt(OFFHAND_SLOT));
        if (!offhandStack.isEmpty()) {
            if (player.getOffhandItem().isEmpty()) {
                player.setItemSlot(EquipmentSlot.OFFHAND, offhandStack);
                clearSlot(OFFHAND_SLOT);
            } else if (inv.add(offhandStack)) {
                clearSlot(OFFHAND_SLOT);
            }
        }

        setChanged();
    }

    /**
     * Clear a slot in the handler (set to empty).
     */
    private void clearSlot(int slot) {
        handler.set(slot, ItemResource.EMPTY, 0);
    }

    /**
     * Check if this husk is empty (no items).
     */
    public boolean isEmpty() {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (!handler.getResource(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if the husk is empty and transition to BROKEN state.
     */
    private void checkAndTransitionToBroken() {
        if (isEmpty() && level != null) {
            BlockState current = getBlockState();
            HuskType currentType = current.getValue(HuskBlock.HUSK_TYPE);
            if (currentType != HuskType.BROKEN) {
                level.setBlock(getBlockPos(), current.setValue(HuskBlock.HUSK_TYPE, HuskType.BROKEN), 3);
            }
        }
    }

    /**
     * Drop all items in the world.
     * Called when the block is broken by the owner.
     */
    public void drops() {
        SimpleContainer inventory = new SimpleContainer(TOTAL_SLOTS);
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            int amount = handler.getAmountAsInt(i);
            if (amount > 0) {
                inventory.setItem(i, handler.getResource(i).toStack(amount));
            }
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        handler.serialize(output);
        if (ownerUUID != null) {
            output.putLong("husk.owner_most", ownerUUID.getMostSignificantBits());
            output.putLong("husk.owner_least", ownerUUID.getLeastSignificantBits());
        }
        super.saveAdditional(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        handler.deserialize(input);

        long most = input.getLongOr("husk.owner_most", 0);
        long least = input.getLongOr("husk.owner_least", 0);
        if (most != 0 || least != 0) {
            ownerUUID = new UUID(most, least);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}

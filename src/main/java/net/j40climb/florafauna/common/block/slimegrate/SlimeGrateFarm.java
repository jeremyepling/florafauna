package net.j40climb.florafauna.common.block.slimegrate;

import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.mobsymbiote.MobSymbioteHelper;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.NoteBlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = FloraFauna.MOD_ID)
public final class SlimeGrateFarm {
    public static final int RECOAT_TICKS = 600;
    public static final int SPLIT_TICKS = 40;
    private static final int HEARING_RANGE = 10;

    // Only the last shed time is saved and synchronized. A note is a short-lived movement cue.
    public record Lure(BlockPos target, long until) {}

    @SubscribeEvent
    public static void onNote(NoteBlockEvent.Play event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos note = event.getPos();
        Lure lure = new Lure(note.immutable(), level.getGameTime() + 120);
        for (Slime slime : level.getEntitiesOfClass(Slime.class, new AABB(note).inflate(HEARING_RANGE),
                s -> s.getType() == EntityType.SLIME && MobSymbioteHelper.hasMobSymbiote(s)
                        && s.distanceToSqr(Vec3.atCenterOf(note)) <= HEARING_RANGE * HEARING_RANGE)) {
            slime.setPersistenceRequired();
            slime.setData(FloraFaunaRegistry.SLIME_NOTE, lure);
        }
    }

    @SubscribeEvent
    public static void afterMove(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Slime slime) || !(slime.level() instanceof ServerLevel level)
                || !slime.hasData(FloraFaunaRegistry.SLIME_NOTE)) return;
        Lure lure = slime.getData(FloraFaunaRegistry.SLIME_NOTE);
        if (lure.until < level.getGameTime() || !level.getBlockState(lure.target).is(Blocks.NOTE_BLOCK)
                || !MobSymbioteHelper.hasMobSymbiote(slime) || slime.isPassenger() || !slime.isAlive()) {
            slime.removeData(FloraFaunaRegistry.SLIME_NOTE);
            return;
        }

        Vec3 old = slime.oldPosition();
        Vec3 now = slime.position();
        long last = slime.getData(FloraFaunaRegistry.SLIME_LAST_SHED);
        if ((last < 0 || level.getGameTime() - last >= RECOAT_TICKS) && old.distanceToSqr(now) <= 4) {
            AABB swept = new AABB(old, now).expandTowards(0, slime.getBbHeight(), 0);
            for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(swept.getMinPosition()), BlockPos.containing(swept.getMaxPosition()))) {
                BlockState state = level.getBlockState(pos);
                if (state.is(FloraFaunaRegistry.SLIME_GRATE) && crossesPanel(old, now, slime.getBbHeight(), pos, state.getValue(SlimeGrateBlock.AXIS), lure.target)) {
                    slime.setData(FloraFaunaRegistry.SLIME_LAST_SHED, level.getGameTime());
                    Vec3 center = Vec3.atCenterOf(pos);
                    ItemEntity drop = new ItemEntity(level, center.x, pos.getY() + 0.15, center.z, new ItemStack(Items.SLIME_BALL));
                    drop.setDeltaMovement(Vec3.ZERO);
                    level.addFreshEntity(drop);
                    level.playSound(null, pos, SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 1f, 1.35f);
                    level.sendParticles(ParticleTypes.ITEM_SLIME, center.x, center.y, center.z, 9, 0.2, 0.2, 0.2, 0.02);
                    break;
                }
            }
        }
        // Slimes have their own private jump controller. Steer the running jump directly;
        // normal entity physics still handles fences, walls, gravity and landing.
        Vec3 delta = Vec3.atCenterOf(lure.target).subtract(now).multiply(1, 0, 1);
        if (delta.lengthSqr() < 0.45 || slime.getY() + 3 < lure.target.getY()) {
            slime.removeData(FloraFaunaRegistry.SLIME_NOTE);
            return;
        }
        Vec3 hop = delta.normalize().scale(0.25);
        float yaw = (float)(Mth.atan2(-hop.x, hop.z) * Mth.RAD_TO_DEG);
        slime.setYRot(yaw);
        slime.setYBodyRot(yaw);
        slime.setYHeadRot(yaw);
        // A full vanilla jump clears a one-block grate. Flatten the upward arc while following a note.
        double lift = slime.onGround() ? 0.25 : Math.min(0.16, slime.getDeltaMovement().y);
        slime.setDeltaMovement(hop.x, lift, hop.z);
    }

    // Require a true center crossing within the panel, toward the note on the other side.
    private static boolean crossesPanel(Vec3 from, Vec3 to, float height, BlockPos pos, Direction.Axis axis, BlockPos note) {
        double plane = axis.choose(pos.getX(), pos.getY(), pos.getZ()) + 0.5;
        double start = from.get(axis) - plane;
        double end = to.get(axis) - plane;
        double destination = axis.choose(note.getX(), note.getY(), note.getZ()) + 0.5 - plane;
        if (start == end || start * end > 0 || start == 0 || (end - start) * destination <= 0) return false;
        Vec3 crossing = from.lerp(to, -start / (end - start));
        double across = axis == Direction.Axis.X ? crossing.z - pos.getZ() : crossing.x - pos.getX();
        return across >= 0 && across < 1 && crossing.y + height > pos.getY() && crossing.y < pos.getY() + 1;
    }
}

package net.j40climb.florafauna.common.entity.frenchie.frontpack;

import io.netty.buffer.ByteBuf;
import net.j40climb.florafauna.FloraFauna;
import net.j40climb.florafauna.common.entity.frenchie.FrenchieAwareness;
import net.j40climb.florafauna.setup.FloraFaunaRegistry;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = FloraFauna.MOD_ID)
public record FrontpackWarning(long start, float bearing) {
    public static final FrontpackWarning NONE = new FrontpackWarning(-FrenchieAwareness.COOLDOWN_TICKS, 0);
    public static final StreamCodec<ByteBuf, FrontpackWarning> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, FrontpackWarning::start, ByteBufCodecs.FLOAT, FrontpackWarning::bearing, FrontpackWarning::new);

    public boolean visible(long now) {
        return FrenchieAwareness.flashing(now - start);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var pack = player.getExistingDataOrNull(FloraFaunaRegistry.FRENCH_FRONTPACK_DATA);
        var previous = player.getExistingDataOrNull(FloraFaunaRegistry.FRENCHIE_WARNING);
        if (pack == null || !pack.hasCarriedFrenchie() || player.isSleeping()) {
            if (previous != null) player.removeData(FloraFaunaRegistry.FRENCHIE_WARNING);
            return;
        }
        long now = player.level().getGameTime();
        if (previous != null) {
            if (now - previous.start == FrenchieAwareness.SECOND_BARK_TICK) FrenchieAwareness.bark(player, 1.18F);
            if (now - previous.start < FrenchieAwareness.COOLDOWN_TICKS) return;
        }
        if (player.tickCount % 8 != 0) return;
        var threat = FrenchieAwareness.findCreeper(player.level(), player);
        if (threat == null) return;
        float bearing = (float)(Mth.atan2(threat.getZ() - player.getZ(), threat.getX() - player.getX()) * 180 / Math.PI - 90);
        player.setData(FloraFaunaRegistry.FRENCHIE_WARNING, new FrontpackWarning(now, bearing));
        FrenchieAwareness.bark(player, 1.4F);
    }
}

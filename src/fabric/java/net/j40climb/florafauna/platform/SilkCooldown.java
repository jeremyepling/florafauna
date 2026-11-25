package net.j40climb.florafauna.platform;
import net.minecraft.world.entity.monster.spider.Spider;
public final class SilkCooldown {
 private static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<Long> TYPE=net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.<Long>builder().initializer(()->0L).persistent(com.mojang.serialization.Codec.LONG).buildAndRegister(net.minecraft.resources.Identifier.fromNamespaceAndPath("florafauna","silk_ready_at"));
 public static void init() {}
 public static long get(Spider spider){return ((net.fabricmc.fabric.api.attachment.v1.AttachmentTarget)spider).getAttachedOrCreate(TYPE);}
 public static void set(Spider spider,long tick){((net.fabricmc.fabric.api.attachment.v1.AttachmentTarget)spider).setAttached(TYPE,tick);}
}

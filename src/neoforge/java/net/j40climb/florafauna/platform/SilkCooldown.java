package net.j40climb.florafauna.platform;
import net.minecraft.world.entity.monster.spider.Spider;
public final class SilkCooldown {
 public static void init() {}
 public static long get(Spider spider){return spider.getPersistentData().getLongOr("florafauna:silk_ready_at",0);}
 public static void set(Spider spider,long tick){spider.getPersistentData().putLong("florafauna:silk_ready_at",tick);}
}

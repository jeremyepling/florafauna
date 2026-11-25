package net.j40climb.florafauna.platform;
public final class PlatformEnvironment {
 public static boolean isProduction() {
  //? if neoforge {
  return net.neoforged.fml.loading.FMLEnvironment.isProduction();
  //?} else {
  /*return !net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment();
  *///?}
 }
 public static boolean isModLoaded(String id) {
  //? if neoforge {
  return net.neoforged.fml.ModList.get().isLoaded(id);
  //?} else {
  /*return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(id);
  *///?}
 }
}

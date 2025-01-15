package com.technicjelle.BlueMapCustomSkinProvider;

import com.technicjelle.BMUtils.BMNative.BMNConfigDirectory;
import de.bluecolored.bluemap.api.BlueMapAPI;
import java.io.IOException;
import java.nio.file.Path;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

/**
 * @author MochaMousse
 */
@Getter
@ConfigSerializable
public class Config {
  private static final String FILE_NAME = "settings.conf";
  private @Nullable String driverClassName;
  private @Nullable String authDatabaseUrl;
  private @Nullable String authDatabaseUsername;
  private @Nullable String authDatabasePassword;
  private @Nullable String skinDatabaseUrl;
  private @Nullable String skinDatabaseUsername;
  private @Nullable String skinDatabasePassword;
  private @Nullable String filePath;
  private @Nullable String remoteUrl;

  public static Config load(BlueMapAPI api) throws IOException {
    BMNConfigDirectory.BMNCopy.fromJarResource(
        api, Config.class.getClassLoader(), FILE_NAME, FILE_NAME, false);
    Path configDirectory =
        BMNConfigDirectory.getAllocatedDirectory(api, Config.class.getClassLoader());
    Path configFile = configDirectory.resolve(FILE_NAME);
    HoconConfigurationLoader loader =
        HoconConfigurationLoader.builder()
            .defaultOptions(options -> options.implicitInitialization(false))
            .path(configFile)
            .build();
    Config config = loader.load().get(Config.class);
    if (config == null) {
      throw new IOException("Failed to load config");
    }
    return config;
  }
}

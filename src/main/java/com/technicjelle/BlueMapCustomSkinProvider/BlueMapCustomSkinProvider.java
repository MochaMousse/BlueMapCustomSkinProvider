package com.technicjelle.BlueMapCustomSkinProvider;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.technicjelle.BMUtils.BMNative.BMNLogger;
import com.technicjelle.BMUtils.BMNative.BMNMetadata;
import com.technicjelle.BlueMapCustomSkinProvider.service.BlessingSkinService;
import com.technicjelle.BlueMapCustomSkinProvider.service.MultiLoginService;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.plugin.SkinProvider;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author MochaMousse
 */
public final class BlueMapCustomSkinProvider implements Runnable {
  private static final String DEFAULT_TEXTURE =
      "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAMAAACdt4HsAAAAdVBMVEUAAAArHg0zJBGBUzk/KhV2SzOQWT+PXj5JJRAkGAibY0mzeV63g2uqclk0JRL///9SPYlqQDCUYD53QjVCHQomGgo6MYk3NzcoKCgAzMwAr68ApKQKvLxBNZtGOqUElZUAf38FiIgAaGgDenpKSko/Pz9VVVUam28fAAAAAXRSTlMAQObYZgAAAv5JREFUWMPtlu12mzAMhstHneDMa7tmkEIJcdrs/i9x7yvZLbC0OPmzs50KAjInepBkY+nmJkiW5TjzrAhyc6lkeYmTgFvINQAY8zC3IuYKDxAAftcDMg3CaAiXA3LmMCvLwjCFZnU5oJQY8lWhxxU5yJnHrFitIUW6BzL5Fa8lBAQMGA6XBbQs4c34Y5XnFQk5j6rSRVXyeZkUu6wgu/m2cfa7jEtOKMl5vuwBPcY7rdtsnHNWrDQMCSQlBCw/W7m7e2fv71xljTzKKwEtA/imykKce3iAB5BK/EcqmI5FQJVVxq6KH2a1fnzcQilW1uAhAVkKoDCwJ6HYrreYf4OBNVyOPyGJX2UNaZrdLo6fHBDWPdVB4vO2bTuE6s4CdrumeQPAHoSzAKTJLHvwEeCZgBQPPgihkxBmHvR93e/3+xq3Hko9QPAeTGvXqt7hClMYd7gdIBy8A+p3AO77wfuhbQfneBUdlu2bfXs4Hg9QxwAx7CPAv7z4Dia8UCeBrFbMu0EAGJwH4HyBRCPqjBt6dwhyhPA+ATAMEpgwGuHtBPgAGCR51hoeIhhMPeBJCH5028NdAkII4hC/VByYA342o6mMsb/lwEvmOrzWR51DsQJCADhH0zgFMPOebnP6oAugCwAnADPxoBbDPiaiH2DjB8nbIJrM3+DFf/3cp4DX17jeXkUGMRskjxRaw947BTjdOkaA5gSrXzQ9UT3pZMAfDzMqiAjKXgFWAXYMaJoTDU+NaE1Y0pgW7zUvYOHKCdAcGkEk7RNf8t/JswrXdddC+dv+fMk/IPNiy5rE7egiwLjcE2DsmbKe7sFze7YvuMCDzqTug9ov9LFa99oPaGmf9AWfAcRS+4a9lvNAGPcFnwHqUO8jYAjGhKR7QAC02A9EiftB3B/+BMz6Be0HtJywLi57oDVWWxborEWhNXBJUzkJARIagtgdpAE0idovmNiShPK8CJDKLCtBYzGxnttEwLxfiPYxCYuAeb8Q+4GYhmXArF/QGWT6tD+Y//837Wp+wZH/YdMAAAAASUVORK5CYII=";
  public static BMNLogger logger;
  public static Config config;
  private final Map<String, String> uuidNameMap = new HashMap<>();
  private final Consumer<BlueMapAPI> blueMapOnEnableListener =
      api -> {
        try {
          logger.logInfo("加载配置文件");
          config = Config.load(api);
        } catch (IOException e) {
          config = null;
          logger.logError("无法加载配置文件");
          throw new UncheckedIOException(e);
        }
        try {
          logger.logInfo("加载数据源");
          Datasource.init(
              Datasource.AUTH_DATASOURCE,
              config.getDriverClassName(),
              config.getAuthDatabaseUrl(),
              config.getAuthDatabaseUsername(),
              config.getAuthDatabasePassword());
          Datasource.init(
              Datasource.SKIN_DATASOURCE,
              config.getDriverClassName(),
              config.getSkinDatabaseUrl(),
              config.getSkinDatabaseUsername(),
              config.getSkinDatabasePassword());
        } catch (SQLException e) {
          logger.logError("无法加载数据源", e);
        }
        logger.logInfo("设置皮肤加载规则");
        SkinProvider customSkinProvider =
            uuid -> {
              BufferedImage image = getTextureFromLocal(uuid.toString());
              if (image == null) {
                image = getTextureFromRemote(uuid.toString());
              }
              if (image == null) {
                try (ByteArrayInputStream byteArrayInputStream =
                    new ByteArrayInputStream(Base64.getDecoder().decode(DEFAULT_TEXTURE))) {
                  image = ImageIO.read(byteArrayInputStream);
                  logger.logInfo(String.format("为玩家%s加载默认材质", uuidNameMap.get(uuid.toString())));
                } catch (IOException e) {
                  throw new UncheckedIOException(e);
                }
              }
              return Optional.ofNullable(image);
            };
        api.getPlugin().setSkinProvider(customSkinProvider);
      };

  /**
   * Downloads an image from the given URL.
   *
   * @param link URL of the image
   * @return The image, or <code>null</code> if it could not be found, or the link was invalid
   */
  private static @Nullable BufferedImage downloadImage(@NotNull String link) {
    final @NotNull URL url;
    try {
      url = new URI(link).toURL();
    } catch (MalformedURLException | URISyntaxException e) {
      return null;
    }
    try (InputStream in = url.openStream()) {
      return ImageIO.read(in);
    } catch (IOException e) {
      return null;
    }
  }

  private BufferedImage getTextureFromLocal(String uuid) {
    String username;
    if (uuidNameMap.containsKey(uuid)) {
      username = uuidNameMap.get(uuid);
    } else {
      username = MultiLoginService.getUsernameByUuid(uuid);
      uuidNameMap.put(uuid, username);
    }
    String fileName = BlessingSkinService.getTextureFileNameByName(username);
    if (fileName != null && config.getFilePath() != null) {
      try {
        String path = config.getFilePath().concat("/").concat(fileName);
        logger.logInfo(String.format("从%s获取玩家%s材质", path, username));
        return ImageIO.read(new File(path));
      } catch (IOException e) {
        logger.logError(String.format("无法从本地获取玩家%s材质", username), e);
      }
    }
    return null;
  }

  private BufferedImage getTextureFromRemote(String uuid) {
    BufferedImage image = null;
    try {
      URL profileUrl =
          new URI(Objects.requireNonNull(config.getRemoteUrl()).replace("{UUID}", uuid)).toURL();
      URLConnection request = profileUrl.openConnection();
      request.connect();
      Gson gson = new Gson();
      Profile profile =
          gson.fromJson(new String(request.getInputStream().readAllBytes()), Profile.class);
      String textures = null;
      for (Profile.Property property : profile.getProperties()) {
        if ("textures".equals(property.getName())) {
          textures = property.getValue();
          break;
        }
      }
      JsonObject jsonObject =
          gson.fromJson(new String(Base64.getDecoder().decode(textures)), JsonObject.class);
      String textureUrl =
          jsonObject.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
      image = downloadImage(textureUrl);
      logger.logInfo(String.format("从%s获取玩家%s材质", jsonObject, uuidNameMap.get(uuid)));
    } catch (Exception e) {
      logger.logError(String.format("从远程获取玩家%s材质", uuidNameMap.get(uuid)), e);
    }
    return image;
  }

  @Override
  public void run() {
    String addonId;
    String addonVersion;
    try {
      addonId = BMNMetadata.getAddonID(this.getClass().getClassLoader());
      addonVersion = BMNMetadata.getKey(this.getClass().getClassLoader(), "version");
      logger = new BMNLogger(this.getClass().getClassLoader());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    logger.logInfo("Starting " + addonId + " " + addonVersion);
    BlueMapAPI.onEnable(blueMapOnEnableListener);
  }
}

package com.technicjelle.BlueMapCustomSkinProvider.service;

import com.technicjelle.BlueMapCustomSkinProvider.Datasource;
import org.intellij.lang.annotations.Language;

/**
 * @author MochaMousse
 */
public class BlessingSkinService {
  private BlessingSkinService() {}

  public static String getTextureFileNameByName(String username) {
    @Language("MySQL")
    String sql =
        "SELECT hash FROM blessingskin.players AS t1 LEFT JOIN blessingskin.textures AS t2 ON t1.tid_skin = t2.tid WHERE t1.name = ?";
    return Datasource.getOne(Datasource.SKIN_DATASOURCE, sql, username);
  }
}

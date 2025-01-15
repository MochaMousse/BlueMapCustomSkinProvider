package com.technicjelle.BlueMapCustomSkinProvider.service;

import com.technicjelle.BlueMapCustomSkinProvider.Datasource;
import org.intellij.lang.annotations.Language;

/**
 * @author MochaMousse
 */
public class MultiLoginService {
  private MultiLoginService() {}

  public static String getUsernameByUuid(String uuid) {
    @Language("MySQL")
    String sql =
        "SELECT current_username_lower_case FROM multilogin.multilogin_in_game_profile_v3 WHERE in_game_uuid = UNHEX(REPLACE(?, '-', ''))";
    return Datasource.getOne(Datasource.AUTH_DATASOURCE, sql, uuid);
  }
}

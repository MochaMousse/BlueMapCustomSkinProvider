package com.technicjelle.BlueMapCustomSkinProvider;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidPooledConnection;
import java.sql.*;

/**
 * @author MochaMousse
 */
public class Datasource {
  public static final DruidDataSource SKIN_DATASOURCE = new DruidDataSource();
  public static final DruidDataSource AUTH_DATASOURCE = new DruidDataSource();

  private Datasource() {}

  public static void init(
      DruidDataSource dataSource,
      String driverClassName,
      String url,
      String username,
      String password)
      throws SQLException {
    dataSource.setDriverClassName(driverClassName);
    dataSource.setUrl(url);
    dataSource.setUsername(username);
    dataSource.setPassword(password);
    dataSource.init();
  }

  public static String getOne(DruidDataSource dataSource, String sql, String... args) {
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    try (var connection = getConnection(dataSource)) {
      preparedStatement = connection.prepareStatement(sql);
      var i = 1;
      for (var arg : args) {
        preparedStatement.setString(i++, arg);
      }
      resultSet = preparedStatement.executeQuery();
      resultSet.next();
      return resultSet.getString(1);
    } catch (Exception e) {
      BlueMapCustomSkinProvider.logger.logError(e.getMessage(), e);
    } finally {
      closeConnection(resultSet, preparedStatement);
    }
    return null;
  }

  private static DruidPooledConnection getConnection(DruidDataSource dataSource)
      throws SQLException {
    return dataSource.getConnection();
  }

  private static void closeConnection(ResultSet resultSet, PreparedStatement preparedStatement) {
    if (resultSet != null) {
      try {
        resultSet.close();
      } catch (SQLException e) {
        BlueMapCustomSkinProvider.logger.logError(e.getMessage(), e);
      }
    }
    if (preparedStatement != null) {
      try {
        preparedStatement.close();
      } catch (SQLException e) {
        BlueMapCustomSkinProvider.logger.logError(e.getMessage(), e);
      }
    }
  }
}

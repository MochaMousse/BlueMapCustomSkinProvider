package com.technicjelle.BlueMapCustomSkinProvider;

import java.util.List;
import lombok.Data;

/**
 * @author MochaMousse
 */
@Data
public class Profile {
  private String id;
  private String name;
  private List<Property> properties;
  private List<Object> profileActions;

  @Data
  public static class Property {
    private String name;
    private String value;
  }
}

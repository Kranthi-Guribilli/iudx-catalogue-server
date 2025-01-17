package iudx.catalogue.server.apiserver.Item.model;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Provider implements Item {
  private static final String NAME_REGEX =
      "^[a-zA-Z0-9(\\[]([\\w().\\[\\] &\\-]*[a-zA-Z0-9).\\]])?$";
  private static final String UUID_REGEX =
      "^[a-zA-Z0-9]{8}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{12}$";

  private UUID id;
  @NotEmpty(message = "Type cannot be empty")
  private List<String> type;
  @NotEmpty(message = "Name cannot be empty")
  @Pattern(regexp = NAME_REGEX, message = "Invalid name format")
  private String name;
  @NotEmpty(message = "Description cannot be null or empty")
  private String description;
  @NotEmpty(message = "ProviderOrg cannot be null or empty")
  private ProviderOrg providerOrg;
  @NotEmpty(message = "Owner cannot be null or empty")
  @Pattern(regexp = UUID_REGEX, message = "Invalid owner format")
  private UUID ownerUserId;
  private UUID resourceServer;
  private String context;
  private String itemStatus;
  private String itemCreatedAt;

  public Provider(JsonObject json) {
    this.context = json.getString("@context");
    this.id = UUID.fromString(json.getString("id", UUID.randomUUID().toString()));
    this.type = json.getJsonArray("type", new JsonArray().add("iudx:Provider")).getList();
    this.name = json.getString("name");
    this.description = json.getString("description");
    this.ownerUserId = UUID.fromString(json.getString("ownerUserId"));
    this.resourceServer = UUID.fromString(json.getString("resourceServer"));
    this.itemStatus = json.getString("itemStatus");
    this.itemCreatedAt = json.getString("itemCreatedAt");

    JsonObject providerOrgJson = json.getJsonObject("providerOrg");
    if (providerOrgJson != null) {
      this.providerOrg = new ProviderOrg(providerOrgJson);
    }

    validateFields();
  }

  private void validateFields() {
    if (id == null || !id.toString().matches(UUID_REGEX)) {
      throw new IllegalArgumentException(String.format(
          "[ECMA 262 regex \"%s\" does not match input string \"%s\"]",
          UUID_REGEX, id
      ));
    }
    if (name == null) {
      throw new IllegalArgumentException("[object has missing required properties ([\"name\"])])");
    }
    if (description == null) {
      throw new IllegalArgumentException("[object has missing required properties ([\"description\"])])");
    }
    if (!name.matches(NAME_REGEX)) {
      throw new IllegalArgumentException(String.format(
          "[ECMA 262 regex \"%s\" does not match input string \"%s\"]",
          NAME_REGEX, name
      ));
    }
    if (ownerUserId == null) {
      throw new IllegalArgumentException("[object has missing required properties ([\"ownerUserId\"])])");
    }
    if (!ownerUserId.toString().matches(UUID_REGEX)) {
      throw new IllegalArgumentException(String.format(
          "[ECMA 262 regex \"%s\" does not match input string \"%s\"]",
          UUID_REGEX, ownerUserId
      ));
    }
    if (resourceServer == null) {
      throw new IllegalArgumentException("[object has missing required properties ([\"resourceServer\"])])");
    }
    if (!resourceServer.toString().matches(UUID_REGEX)) {
      throw new IllegalArgumentException(String.format(
          "[ECMA 262 regex \"%s\" does not match input string \"%s\"]",
          UUID_REGEX, resourceServer
      ));
    }
  }

  @Override
  public UUID getId() {
    return id;
  }

  @Override
  public void setId(UUID id) {
    this.id = id;
  }

  @Override
  public List<String> getType() {
    return new ArrayList<>(type);
  }

  @Override
  public void setType(List<String> type) {
    this.type = new ArrayList<>(type);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public String getContext() {
    return context;
  }

  @Override
  public void setContext(String context) {
    this.context = context;
  }

  @Override
  public String getItemStatus() {
    return itemStatus;
  }

  @Override
  public void setItemStatus(String itemStatus) {
    this.itemStatus = itemStatus;
  }

  @Override
  public String getItemCreatedAt() {
    return itemCreatedAt;
  }

  @Override
  public void setItemCreatedAt(String itemCreatedAt) {
    this.itemCreatedAt = itemCreatedAt;
  }

  public ProviderOrg getProviderOrg() {
    return providerOrg;
  }

  public void setProviderOrg(ProviderOrg providerOrg) {
    this.providerOrg = providerOrg;
  }

  public UUID getOwnerUserId() {
    return ownerUserId;
  }

  public void setOwnerUserId(UUID ownerUserId) {
    this.ownerUserId = ownerUserId;
  }

  public UUID getResourceServer() {
    return resourceServer;
  }

  public void setResourceServer(UUID resourceServer) {
    this.resourceServer = resourceServer;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();

    json.put("@context", context);
    json.put("id", id.toString());
    json.put("type", new JsonArray(type));
    json.put("name", name);
    json.put("description", description);
    json.put("ownerUserId", ownerUserId.toString());
    json.put("resourceServer", resourceServer.toString());
    json.put("itemStatus", itemStatus);
    json.put("itemCreatedAt", itemCreatedAt);

    if (providerOrg != null) {
      json.put("providerOrg", providerOrg.toJson());
    }

    return json;
  }

  public static class ProviderOrg {
    private String name;
    private String additionalInfoURL;
    private Location location;

    public ProviderOrg(JsonObject json) {
      this.name = json.getString("name");
      this.additionalInfoURL = json.getString("additionalInfoURL");
      JsonObject locationJson = json.getJsonObject("location");
      if (locationJson != null) {
        this.location = new Location(locationJson);
      }
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getAdditionalInfoURL() {
      return additionalInfoURL;
    }

    public void setAdditionalInfoURL(String additionalInfoURL) {
      this.additionalInfoURL = additionalInfoURL;
    }

    public Location getLocation() {
      return location;
    }

    public void setLocation(Location location) {
      this.location = location;
    }

    public JsonObject toJson() {
      JsonObject json = new JsonObject();

      json.put("name", name);
      json.put("additionalInfoURL", additionalInfoURL);

      if (location != null) {
        json.put("location", location.toJson());
      }

      return json;
    }
  }

  public static class Location {
    private String type;
    private String address;
    private Geometry geometry;

    public Location(JsonObject json) {
      this.type = json.getString("type");
      this.address = json.getString("address");
      JsonObject geometryJson = json.getJsonObject("geometry");
      if (geometryJson != null) {
        this.geometry = new Geometry(geometryJson);
      }
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getAddress() {
      return address;
    }

    public void setAddress(String address) {
      this.address = address;
    }

    public Geometry getGeometry() {
      return geometry;
    }

    public void setGeometry(Geometry geometry) {
      this.geometry = geometry;
    }

    public JsonObject toJson() {
      JsonObject json = new JsonObject();

      json.put("type", type);
      json.put("address", address);

      if (geometry != null) {
        json.put("geometry", geometry.toJson());
      }

      return json;
    }
  }

  public static class Geometry {
    private String type;
    private List<Double> coordinates;

    public Geometry(JsonObject json) {
      this.type = json.getString("type");
      this.coordinates = json.getJsonArray("coordinates").getList();
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public List<Double> getCoordinates() {
      return new ArrayList<>(coordinates);
    }

    public void setCoordinates(List<Double> coordinates) {
      this.coordinates = new ArrayList<>(coordinates);
    }

    public JsonObject toJson() {
      JsonObject json = new JsonObject();

      json.put("type", type);
      json.put("coordinates", new JsonArray(coordinates));

      return json;
    }
  }
}
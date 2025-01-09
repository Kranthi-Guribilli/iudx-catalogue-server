package iudx.catalogue.server.apiserver.Item.model;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ResourceServer implements Item {

  private static final String UUID_PATTERN =
      "^[a-zA-Z0-9]{8}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{12}$";
  private static final String NAME_PATTERN = "^[a-zA-Z0-9]([\\w-.]*[a-zA-Z0-9 ])?$";
  private static final String REG_URL_PATTERN = "^[a-zA-Z0-9-]{2,}(\\.[a-zA-Z0-9-]{2,}){1,10}$";


  @NotEmpty(message = "cos cannot be empty")
  @Pattern(regexp = UUID_PATTERN, message = "Invalid ID format")
  private UUID cos;
  @NotEmpty(message = "owner cannot be empty")
  @Pattern(regexp = UUID_PATTERN, message = "Invalid ID format")
  private UUID owner;
  private String itemStatus;
  private String itemCreatedAt;
  @NotEmpty(message = "resourceServerOrg cannot be empty")
  private ResourceServerOrg resourceServerOrg;
  @NotEmpty(message = "location cannot be empty")
  private Location location;
  @NotEmpty(message = "resourceServerRegURL cannot be empty")
  private String resourceServerRegURL;
  @NotEmpty(message = "resourceAccessModalities cannot be empty")
  private List<ResourceAccessModality> resourceAccessModalities;

  private UUID id;
  @NotEmpty(message = "Type cannot be empty")
  private List<String> type;
  @NotEmpty(message = "Name cannot be empty")
  @Pattern(regexp = NAME_PATTERN, message = "Invalid name format")
  private String name;
  @NotEmpty(message = "Description cannot be empty")
  private String description;
  @NotEmpty(message = "Tags cannot be empty")
  private List<String> tags;
  private String context;


  public ResourceServer(JsonObject json) {
    this.context = json.getString("@context");
    this.id = UUID.fromString(json.getString("id", UUID.randomUUID().toString()));
    this.type = json.getJsonArray("type", new JsonArray().add("iudx:ResourceServer")).getList();
    this.name = json.getString("name");
    this.description = json.getString("description");
    this.tags = json.getJsonArray("tags", new JsonArray()).getList();
    this.cos = UUID.fromString(json.getString("cos"));
    this.owner = UUID.fromString(json.getString("owner"));
    this.itemStatus = json.getString("itemStatus");
    this.itemCreatedAt = json.getString("itemCreatedAt");
    this.resourceServerRegURL = json.getString("resourceServerRegURL");
    this.itemStatus = json.getString("itemStatus");
    this.itemCreatedAt = json.getString("itemCreatedAt");

    JsonObject orgJson = json.getJsonObject("resourceServerOrg");
    if (orgJson != null) {
      this.resourceServerOrg = new ResourceServerOrg(orgJson);
    }

    JsonObject locationJson = json.getJsonObject("location");
    if (locationJson != null) {
      this.location = new Location(locationJson);
    }

    JsonArray modalitiesJson = json.getJsonArray("resourceAccessModalities");
    if (modalitiesJson != null) {
      this.resourceAccessModalities = modalitiesJson.stream()
          .map(obj -> new ResourceAccessModality((JsonObject) obj))
          .collect(Collectors.toList());
    }

    validateFields();
  }

  private void validateFields() {
    if (id == null || !id.toString().matches(UUID_PATTERN)) {
      throw new IllegalArgumentException("Invalid ID format");
    }
    if (name == null) {
      throw new IllegalArgumentException("[object has missing required properties ([\"name\"])])");
    }
    if (description == null) {
      throw new IllegalArgumentException("[object has missing required properties ([\"description\"])])");
    }
    if (!name.matches(NAME_PATTERN)) {
      throw new IllegalArgumentException(String.format(
          "[ECMA 262 regex \"%s\" does not match input string \"%s\"]",
          NAME_PATTERN, name
      ));
    }
    if (cos == null) {
      throw new IllegalArgumentException("[object has missing required properties ([\"cos\"])])");
    }
    if (!cos.toString().matches(UUID_PATTERN)) {
      throw new IllegalArgumentException(String.format(
          "[ECMA 262 regex \"%s\" does not match input string \"%s\"]",
          UUID_PATTERN, cos
      ));
    }
    if (owner == null) {
      throw new IllegalArgumentException("[object has missing required properties ([\"owner\"])])");
    }
    if (!owner.toString().matches(UUID_PATTERN)) {
      throw new IllegalArgumentException(String.format(
          "[ECMA 262 regex \"%s\" does not match input string \"%s\"]",
          UUID_PATTERN, owner
      ));
    }
    if (resourceServerRegURL == null) {
      throw new IllegalArgumentException("[object has missing required properties ([\"resourceServerRegURL\"])])");
    }
    if (!resourceServerRegURL.matches(REG_URL_PATTERN)) {
      throw new IllegalArgumentException(String.format(
          "[ECMA 262 regex \"%s\" does not match input string \"%s\"]",
          REG_URL_PATTERN, resourceServerRegURL
      ));
    }
    if (resourceServerOrg == null) {
      throw new IllegalArgumentException("[object has missing required properties ([\"resourceServerOrg\"])])");
    }
    if (location == null) {
      throw new IllegalArgumentException("[object has missing required properties ([\"location\"])])");
    }
    if (resourceAccessModalities == null) {
      throw new IllegalArgumentException("[object has missing required properties ([\"resourceAccessModalities\"])])");
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
    return type;
  }

  @Override
  public void setType(List<String> type) {
    this.type = type;
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

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
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

  public UUID getCos() {
    return cos;
  }

  public void setCos(UUID cos) {
    this.cos = cos;
  }

  public UUID getOwner() {
    return owner;
  }

  public void setOwner(UUID owner) {
    this.owner = owner;
  }

  public ResourceServerOrg getResourceServerOrg() {
    return resourceServerOrg;
  }

  public void setResourceServerOrg(ResourceServerOrg resourceServerOrg) {
    this.resourceServerOrg = resourceServerOrg;
  }

  public Location getLocation() {
    return location;
  }

  public void setLocation(Location location) {
    this.location = location;
  }

  public String getResourceServerRegURL() {
    return resourceServerRegURL;
  }

  public void setResourceServerRegURL(String resourceServerRegURL) {
    this.resourceServerRegURL = resourceServerRegURL;
  }

  public List<ResourceAccessModality> getResourceAccessModalities() {
    return new ArrayList<>(resourceAccessModalities);
  }

  public void setResourceAccessModalities(List<ResourceAccessModality> resourceAccessModalities) {
    this.resourceAccessModalities = new ArrayList<>(resourceAccessModalities);
  }

  public void addResourceAccessModality(ResourceAccessModality resourceAccessModality) {
    this.resourceAccessModalities.add(resourceAccessModality);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.put("@context", context);
    json.put("id", id.toString());
    json.put("type", new JsonArray(type));
    json.put("name", name);
    json.put("description", description);
    json.put("tags", new JsonArray(tags));
    json.put("cos", cos.toString());
    json.put("owner", owner.toString());
    json.put("itemStatus", itemStatus);
    json.put("itemCreatedAt", itemCreatedAt);
    json.put("resourceServerRegURL", resourceServerRegURL);

    if (resourceServerOrg != null) {
      json.put("resourceServerOrg", resourceServerOrg.toJson());
    }
    if (location != null) {
      json.put("location", location.toJson());
    }
    if (resourceAccessModalities != null && !resourceAccessModalities.isEmpty()) {
      json.put("resourceAccessModalities", new JsonArray(resourceAccessModalities.stream()
          .map(ResourceAccessModality::toJson)
          .collect(Collectors.toList())));
    }

    return json;
  }

  public static class ResourceServerOrg {
    private String name;
    private String additionalInfoURL;
    private Location location;

    public ResourceServerOrg(JsonObject json) {
      this.name = json.getString("name");
      this.additionalInfoURL = json.getString("additionalInfoURL");
      JsonObject locationJson = json.getJsonObject("location");
      if (locationJson != null) {
        this.location = new Location(locationJson);
      }
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

    // Getters and setters...
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

    public JsonObject toJson() {
      JsonObject json = new JsonObject();
      json.put("type", type);
      json.put("address", address);
      if (geometry != null) {
        json.put("geometry", geometry.toJson());
      }
      return json;
    }

    // Getters and setters...
  }

  public static class Geometry {
    private String type;
    private List<Double> coordinates;

    public Geometry(JsonObject json) {
      this.type = json.getString("type");
      this.coordinates = json.getJsonArray("coordinates").stream()
          .map(obj -> ((Number) obj).doubleValue())
          .collect(Collectors.toList());
    }

    public JsonObject toJson() {
      JsonObject json = new JsonObject();
      json.put("type", type);
      json.put("coordinates", new JsonArray(coordinates));
      return json;
    }

    // Getters and setters...
  }

  public static class ResourceAccessModality {
    private List<String> type;
    private String protocol;
    private String accessURL;
    private Integer port;

    public ResourceAccessModality(JsonObject json) {
      this.type = json.getJsonArray("type").getList();
      this.protocol = json.getString("protocol");
      this.accessURL = json.getString("accessURL");
      this.port = json.getInteger("port");
    }

    public JsonObject toJson() {
      JsonObject json = new JsonObject();
      json.put("type", new JsonArray(type)); // Convert List<String> to JsonArray
      json.put("protocol", protocol);
      json.put("accessURL", accessURL);
      json.put("port", port);
      return json;
    }
  }
}
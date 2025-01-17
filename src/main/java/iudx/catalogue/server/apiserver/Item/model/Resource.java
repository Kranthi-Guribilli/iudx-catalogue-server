package iudx.catalogue.server.apiserver.Item.model;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.UUID;

public class Resource implements Item {
  private static final String NAME_REGEX = "^[a-zA-Z0-9]([\\w-]*[a-zA-Z0-9 ])?$";
  private static final String UUID_REGEX =
      "^[a-zA-Z0-9]{8}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{12}$";
  private static final String ACCESS_POLICY_REGEX = "^(SECURE|OPEN|PII|secure|open|pii)$";

  @NotEmpty(message = "provider cannot be empty")
  private String provider;
  @NotEmpty(message = "resourceGroup cannot be empty")
  private String resourceGroup;
  @NotEmpty(message = "resourceServer cannot be empty")
  private String resourceServer;
  @NotEmpty(message = "Tags cannot be empty")
  private List<String> tags;
  @NotEmpty(message = "apdURL cannot be empty")
  private String apdURL;
  @NotEmpty(message = "accessPolicy cannot be empty")
  private String accessPolicy;
  private UUID id;
  @NotEmpty(message = "Type cannot be empty")
  private List<String> type;
  @NotEmpty(message = "Name cannot be empty")
  @Pattern(regexp = NAME_REGEX, message = "Invalid name format")
  private String name;
  @NotEmpty(message = "Description cannot be empty")
  private String description;
  private String context;
  private String itemStatus;
  private String itemCreatedAt;

  public Resource(JsonObject json) {
    this.context = json.getString("@context");
    this.id = UUID.fromString(json.getString("id", UUID.randomUUID().toString()));
    this.type = json.getJsonArray("type", new JsonArray()).getList();
    this.name = json.getString("name");
    this.description = json.getString("description");
    this.tags = json.getJsonArray("tags", new JsonArray()).getList();
    this.provider = json.getString("provider");
    this.resourceGroup = json.getString("resourceGroup");
    this.resourceServer = json.getString("resourceServer");
    this.apdURL = json.getString("apdURL");
    this.accessPolicy = json.getString("accessPolicy");
    this.itemStatus = json.getString("itemStatus");
    this.itemCreatedAt = json.getString("itemCreatedAt");
    // Perform validation
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
    if (provider == null || !provider.matches(UUID_REGEX)) {
      throw new IllegalArgumentException("Invalid provider format");
    }
    if (resourceGroup == null || !resourceGroup.matches(UUID_REGEX)) {
      throw new IllegalArgumentException("Invalid resourceGroup format");
    }
    if (resourceServer == null || !resourceServer.matches(UUID_REGEX)) {
      throw new IllegalArgumentException("Invalid resourceServer format");
    }
    if (apdURL == null || apdURL.isEmpty()) {
      throw new IllegalArgumentException("ApdURL cannot be null or empty");
    }
    if (accessPolicy == null || !accessPolicy.matches(ACCESS_POLICY_REGEX)) {
      throw new IllegalArgumentException("Invalid accessPolicy value");
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

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public String getResourceGroup() {
    return resourceGroup;
  }

  public void setResourceGroup(String resourceGroup) {
    if (resourceGroup == null || !resourceGroup.matches(UUID_REGEX)) {
      throw new IllegalArgumentException("Invalid resourceGroup format");
    }
    this.resourceGroup = resourceGroup;
  }

  public String getResourceServer() {
    return resourceServer;
  }

  public void setResourceServer(String resourceServer) {
    this.resourceServer = resourceServer;
  }


  public String getApdURL() {
    return apdURL;
  }

  public void setApdURL(String apdURL) {
    this.apdURL = apdURL;
  }

  public String getAccessPolicy() {
    return accessPolicy;
  }

  public void setAccessPolicy(String accessPolicy) {
    this.accessPolicy = accessPolicy;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();

    json.put("@context", context);
    json.put("id", id.toString());
    json.put("type", new JsonArray(type));
    json.put("name", name);
    json.put("description", description);
    json.put("tags", new JsonArray(tags));
    json.put("provider", provider);
    json.put("resourceGroup", resourceGroup);
    json.put("resourceServer", resourceServer);
    json.put("apdURL", apdURL);
    json.put("accessPolicy", accessPolicy);
    json.put("itemStatus", itemStatus);
    json.put("itemCreatedAt", itemCreatedAt);

    return json;
  }

}
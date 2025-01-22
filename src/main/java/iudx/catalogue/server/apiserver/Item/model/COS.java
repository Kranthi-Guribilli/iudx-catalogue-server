package iudx.catalogue.server.apiserver.Item.model;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.UUID;

public class COS implements Item {
  private static final String UUID_REGEX =
      "^[a-zA-Z0-9]{8}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{12}$";
  private static final String NAME_REGEX = "^[a-zA-Z0-9-]([\\w-.]*[a-zA-Z0-9- ])?$";
  private static final String COS_URL_REGEX = "^[a-zA-Z0-9-]{2,}(\\.[a-zA-Z0-9-/]{2,}){1,5}$";
  private static final String COS_UI_REGEX = "^[a-zA-Z0-9-]{2,}(\\.[a-zA-Z0-9-/]{2,}){1,5}$";

  @NotEmpty(message = "Owner cannot be null or empty")
  @Pattern(regexp = UUID_REGEX, message = "Invalid owner format")
  private UUID owner;
  @NotEmpty(message = "COS URL cannot be null or empty")
  @Pattern(regexp = COS_URL_REGEX, message = "Invalid COS URL format")
  private String cosURL;
  @NotEmpty(message = "COS UI cannot be null or empty")
  @Pattern(regexp = COS_UI_REGEX, message = "Invalid COS UI URL format")
  private String cosUI;
  private String context;
  private String itemStatus;
  private String itemCreatedAt;
  private UUID id;
  @NotEmpty(message = "Type cannot be empty")
  private List<String> type;
  @NotEmpty(message = "Name cannot be empty")
  @Pattern(regexp = NAME_REGEX, message = "Invalid name format")
  private String name;
  @NotEmpty(message = "Description cannot be null or empty")
  private String description;
  private final JsonObject requestJson;

  public COS(JsonObject json) {
    this.requestJson = json.copy(); // Store a copy of the input JSON
    this.context = json.getString("@context");
    this.id = UUID.fromString(json.getString("id", UUID.randomUUID().toString()));
    this.type = json.getJsonArray("type", new JsonArray().add("iudx:COS")).getList();
    this.name = json.getString("name");
    this.description = json.getString("description");
    this.owner = UUID.fromString(json.getString("owner"));
    this.cosURL = json.getString("cosURL");
    this.cosUI = json.getString("cosUI");
    this.itemStatus = json.getString("itemStatus");
    this.itemCreatedAt = json.getString("itemCreatedAt");
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
      throw new IllegalArgumentException(
          "[object has missing required properties ([\"description\"])])");
    }
    if (!name.matches(NAME_REGEX)) {
      throw new IllegalArgumentException(String.format(
          "[ECMA 262 regex \"%s\" does not match input string \"%s\"]",
          NAME_REGEX, name
      ));
    }
    if (owner == null) {
      throw new IllegalArgumentException("[object has missing required properties ([\"owner\"])]");
    }
    if (!owner.toString().matches(UUID_REGEX)) {
      throw new IllegalArgumentException(String.format(
          "[ECMA 262 regex \"%s\" does not match input string \"%s\"]",
          UUID_REGEX, owner
      ));
    }
    if (cosURL == null) {
      throw new IllegalArgumentException("[object has missing required properties ([\"cosURL\"])]");
    }
    if (!cosURL.matches(COS_URL_REGEX)) {
      throw new IllegalArgumentException(String.format(
          "[ECMA 262 regex \"%s\" does not match input string \"%s\"]",
          COS_URL_REGEX, cosURL
      ));
    }
    if (cosUI == null) {
      throw new IllegalArgumentException("[object has missing required properties ([\"cosUI\"])]");
    }
    if (!cosUI.matches(COS_UI_REGEX)) {
      throw new IllegalArgumentException(String.format(
          "[ECMA 262 regex \"%s\" does not match input string \"%s\"]",
          COS_UI_REGEX, cosUI
      ));
    }
  }

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


  public UUID getOwner() {
    return owner;
  }

  public void setOwner(UUID owner) {
    this.owner = owner;
  }

  public String getCosURL() {
    return cosURL;
  }

  public void setCosURL(String cosURL) {
    this.cosURL = cosURL;
  }

  public String getCosUI() {
    return cosUI;
  }

  public void setCosUI(String cosUI) {
    this.cosUI = cosUI;
  }
  public JsonObject getRequestJson() {
    return requestJson;
  }

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();

    // Adding the standard fields to the JSON object
    json.put("@context", context);
    json.put("id", id.toString());
    json.put("type", new JsonArray(type)); // Convert List<String> type to JsonArray
    json.put("name", name);
    json.put("description", description);
    json.put("owner", owner.toString());
    json.put("cosURL", cosURL);
    json.put("cosUI", cosUI);
    json.put("itemStatus", itemStatus);
    json.put("itemCreatedAt", itemCreatedAt);
    // Add additional fields from the original JSON request
    JsonObject requestJson = getRequestJson();
    for (String key : requestJson.fieldNames()) {
      if (!json.containsKey(key)) {
        json.put(key, requestJson.getValue(key));
      }
    }

    return json;
  }

}
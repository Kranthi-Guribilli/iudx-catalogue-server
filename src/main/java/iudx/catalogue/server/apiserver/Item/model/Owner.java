package iudx.catalogue.server.apiserver.Item.model;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Owner implements Item {
  private static final Logger LOGGER = LogManager.getLogger(Owner.class);
  private static final String ID_REGEX =
      "^[a-zA-Z0-9]{8}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{12}$";
  private static final String NAME_REGEX = "^[a-zA-Z0-9]([\\w-]*[a-zA-Z0-9 ])?$";

  @Pattern(regexp = ID_REGEX, message = "Invalid ID format")
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

  public Owner(JsonObject json) throws IllegalArgumentException {
    this.context = json.getString("@context");
    this.id = UUID.fromString(json.getString("id", UUID.randomUUID().toString()));
    this.type = json.getJsonArray("type", new JsonArray().add("iudx:Owner")).getList();
    this.name = json.getString("name");
    this.description = json.getString("description");
    this.itemStatus = json.getString("itemStatus");
    this.itemCreatedAt = json.getString("itemCreatedAt");
    validateFields();
  }

  private void validateFields() {
    if (id == null || !id.toString().matches(ID_REGEX)) {
      LOGGER.debug(id);
      throw new IllegalArgumentException(String.format(
          "[ECMA 262 regex \"%s\" does not match input string \"%s\"]",
          ID_REGEX, id
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

  @Override
  public JsonObject toJson() {
    JsonObject json = new JsonObject();

    json.put("@context", context);
    json.put("id", id.toString());
    json.put("type", new JsonArray(type));
    json.put("name", name);
    json.put("description", description);
    json.put("itemStatus", getItemStatus());
    json.put("itemCreatedAt", getItemCreatedAt());

    return json;
  }

}
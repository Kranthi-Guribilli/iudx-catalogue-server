package iudx.catalogue.server.database.elastic.model;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class ElasticsearchResponse {
  private String id;
  private JsonObject source;
  private JsonArray aggregations;
  private static JsonObject globalAggregations;

  public ElasticsearchResponse() {
    // Default constructor
  }

  public ElasticsearchResponse(JsonObject json) {
    ElasticsearchResponseConverter.fromJson(json, this);
  }

  public ElasticsearchResponse(String id, JsonObject source, JsonArray aggregations) {
    this.id = id;
    this.source = source;
    this.aggregations = aggregations;
  }

  public static JsonObject getGlobalAggregations() {
    return globalAggregations;
  }

  public static void setGlobalAggregations(JsonObject globalAggregations) {
    ElasticsearchResponse.globalAggregations = globalAggregations;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ElasticsearchResponseConverter.toJson(this, json);
    return json;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public JsonObject getSource() {
    return source;
  }

  public void setSource(JsonObject source) {
    this.source = source;
  }

  public JsonArray getAggregations() {
    return aggregations;
  }

  public void setAggregations(JsonArray aggregations) {
    this.aggregations = aggregations;
  }

  @Override
  public String toString() {
    return "ElasticsearchResponse{" +
        "id='" + id + '\'' +
        ", source=" + source +
        ", aggregations=" + aggregations +
        '}';
  }
}

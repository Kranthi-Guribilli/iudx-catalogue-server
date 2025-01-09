//package iudx.catalogue.server.common.util;
//
//import co.elastic.clients.elasticsearch._types.query_dsl.Query;
//import co.elastic.clients.json.JsonpMapper;
//import co.elastic.clients.json.JsonpUtils;
//import io.vertx.core.json.JsonObject;
//
//public class QueryUtils {
//
//  // Serialize a Query object to JsonObject
//  public static JsonObject serializeQuery(Query query) {
//    // Use JsonpUtils to serialize the Query object
//    return new JsonObject(query.toJson(JsonpUtils.defaultJsonMapper()).toString());
//  }
//
//  // Deserialize a JsonObject to a Query object
//  public static Query deserializeQuery(JsonObject json) {
//    // Deserialize using the Query's deserialization methods
//    return Query.fromJson(json.getMap(), JsonpUtils.defaultJsonMapper());
//  }
//}

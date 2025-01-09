package iudx.catalogue.server.common.util;

import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.auditing.util.ResponseBuilder;
import iudx.catalogue.server.common.HttpStatusCode;
import iudx.catalogue.server.common.RespBuilder;
import iudx.catalogue.server.common.ResponseUrn;

public class ResponseUtil {
  public static JsonObject generateResponse(HttpStatusCode statusCode, ResponseUrn urn) {
    return generateResponse(statusCode, urn, statusCode.getDescription());
  }

  public static JsonObject generateResponse(
      HttpStatusCode statusCode, ResponseUrn urn, String message) {
    String type = urn.getUrn();

    return new RespBuilder()
        .withDetail(message)
        .withType(type)
        .withTitle(statusCode.getDescription())
        .getJsonResponse();
  }
}


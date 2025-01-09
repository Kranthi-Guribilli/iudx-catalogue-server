package iudx.catalogue.server.apiserver;

import static iudx.catalogue.server.apiserver.util.Constants.APPLICATION_JSON;
import static iudx.catalogue.server.apiserver.util.Constants.BAD_REQUEST;
import static iudx.catalogue.server.apiserver.util.Constants.CONTENT_TYPE;
import static iudx.catalogue.server.apiserver.util.Constants.HEADER_CONTENT_TYPE;
import static iudx.catalogue.server.apiserver.util.Constants.HEADER_TOKEN;
import static iudx.catalogue.server.apiserver.util.Constants.MIME_APPLICATION_JSON;
import static iudx.catalogue.server.apiserver.util.Constants.USERID;
import static iudx.catalogue.server.auditing.util.Constants.API;
import static iudx.catalogue.server.auditing.util.Constants.EPOCH_TIME;
import static iudx.catalogue.server.auditing.util.Constants.IUDX_ID;
import static iudx.catalogue.server.auditing.util.Constants.USER_ID;
import static iudx.catalogue.server.util.Constants.DETAIL_INVALID_SCHEMA;
import static iudx.catalogue.server.util.Constants.HTTP_METHOD;
import static iudx.catalogue.server.util.Constants.ID;
import static iudx.catalogue.server.util.Constants.REQUEST_PATCH;
import static iudx.catalogue.server.util.Constants.REQUEST_POST;
import static iudx.catalogue.server.util.Constants.TITLE_INVALID_UUID;
import static iudx.catalogue.server.util.Constants.TITLE_TOKEN_INVALID;
import static iudx.catalogue.server.util.Constants.TYPE_CONFLICT;
import static iudx.catalogue.server.util.Constants.TYPE_INVALID_SCHEMA;
import static iudx.catalogue.server.util.Constants.TYPE_INVALID_UUID;
import static iudx.catalogue.server.util.Constants.TYPE_ITEM_NOT_FOUND;
import static iudx.catalogue.server.util.Constants.TYPE_MISSING_TOKEN;
import static iudx.catalogue.server.util.Constants.TYPE_TOKEN_INVALID;
import static iudx.catalogue.server.util.Constants.UUID_PATTERN;
import static iudx.catalogue.server.validator.util.Constants.INVALID_SCHEMA_MSG;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.apiserver.stack.StacServiceImpl;
import iudx.catalogue.server.apiserver.stack.StacSevice;
import iudx.catalogue.server.apiserver.util.RespBuilder;
import iudx.catalogue.server.auditing.service.AuditingService;
import iudx.catalogue.server.authenticator.model.JwtData;
import iudx.catalogue.server.authenticator.service.AuthenticationService;
import iudx.catalogue.server.authenticator.model.JwtAuthenticationInfo;
import iudx.catalogue.server.database.elastic.service.ElasticsearchService;
import iudx.catalogue.server.util.Api;
import iudx.catalogue.server.validator.service.ValidatorService;
import java.time.ZonedDateTime;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StacRestApi {
  private static final Logger LOGGER = LogManager.getLogger(StacRestApi.class);

  private AuthenticationService authService;
  private ValidatorService validatorService;
  private AuditingService auditingService;
  private Api api;
  private Router router;
  private ElasticsearchService esService;
  private StacSevice stackSevice;
  private RespBuilder respBuilder;

  public StacRestApi(
      Router router,
      Api api,
      JsonObject config,
      ValidatorService validatorService,
      AuthenticationService authService,
      AuditingService auditingService,
      ElasticsearchService esService) {
    this.api = api;
    this.router = router;
    this.authService = authService;
    this.validatorService = validatorService;
    this.auditingService = auditingService;
    this.esService = esService;
    stackSevice = new StacServiceImpl(esService, config.getString("docIndex"));
  }

  Router init() {
    router
        .post(api.getStackRestApis())
        .handler(
            routingContext -> {
              if (routingContext.request().headers().contains(HEADER_TOKEN)) {
                handlePostStackRequest(routingContext);
              } else {
                LOGGER.warn("Unauthorized CRUD operation");
                HttpServerResponse response = routingContext.response();
                respBuilder =
                    new RespBuilder()
                        .withType(TYPE_MISSING_TOKEN)
                        .withTitle("Not authorized")
                        .withDetail("Token needed but not present");
                response.setStatusCode(400).end(respBuilder.getResponse());
              }
            });
    router
        .patch(api.getStackRestApis())
        .handler(
            routingContext -> {
              if (routingContext.request().headers().contains(HEADER_TOKEN)) {
                handlePatchStackRequest(routingContext);
              } else {
                LOGGER.warn("Unauthorized CRUD operation");
                HttpServerResponse response = routingContext.response();
                respBuilder =
                    new RespBuilder()
                        .withType(TYPE_MISSING_TOKEN)
                        .withTitle("Not authorized")
                        .withDetail("Token needed but not present");
                response.setStatusCode(400).end(respBuilder.getResponse());
              }
            });

    router
        .get(api.getStackRestApis())
        .handler(routingContext -> handleGetStackRequest(routingContext));
    router
        .delete(api.getStackRestApis())
        .handler(
            routingContext -> {
              if (routingContext.request().headers().contains(HEADER_TOKEN)) {
                deleteStackHandler(routingContext);
              } else {
                LOGGER.warn("Unathorized CRUD operation");
                HttpServerResponse response = routingContext.response();
                respBuilder =
                    new RespBuilder()
                        .withType(TYPE_MISSING_TOKEN)
                        .withTitle("Not authorized")
                        .withDetail("Token needed but not present");
                response.setStatusCode(400).end(respBuilder.getResponse());
              }
            });

    return this.router;
  }

  public void handleGetStackRequest(RoutingContext routingContext) {
    LOGGER.debug("method HandleGetStackRequest() started");
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    String stacId = routingContext.queryParams().get(ID);
    LOGGER.debug("stackId:: {}", stacId);
    if (validateId(stacId)) {
      stackSevice
          .get(stacId)
          .onComplete(
              stackHandler -> {
                if (stackHandler.succeeded()) {
                  JsonObject resultJson = stackHandler.result();
                  handleSuccessResponse(response, 200, resultJson.toString());
                } else {
                  LOGGER.error("Fail: Stack not found;" + stackHandler.cause().getMessage());
                  processBackendResponse(response, stackHandler.cause().getMessage());
                }
              });

    } else {
      respBuilder =
          new RespBuilder()
              .withType(TYPE_INVALID_UUID)
              .withTitle(TITLE_INVALID_UUID)
              .withDetail("The id is invalid or not present");
      LOGGER.error("Error invalid id : {}", stacId);
      processBackendResponse(response, respBuilder.getResponse());
    }
  }

  public void handlePostStackRequest(RoutingContext routingContext) {
    LOGGER.debug("method handlePostStackRequest() started");
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);
    String path = routingContext.normalizedPath();
    HttpServerRequest request = routingContext.request();
    JsonObject requestBody = routingContext.body().asJsonObject();
    JsonObject validationJson = requestBody.copy();
    validationJson.put("stack_type", "post:Stack");

    Future<JsonObject> validateSchemaFuture = validatorService.validateSchema(validationJson);
    JwtAuthenticationInfo jwtAuthenticationInfo = new JwtAuthenticationInfo.Builder()
        .setToken(request.getHeader(HEADER_TOKEN))
        .setMethod(REQUEST_POST)
        .setApiEndpoint(api.getStackRestApis())
        .build();
//    JsonObject jwtAuthenticationInfo =
//        new JsonObject()
//            .put(TOKEN, request.getHeader(HEADER_TOKEN))
//            .put(METHOD, REQUEST_POST)
//            .put(API_ENDPOINT, api.getStackRestApis());
    Future<JwtData> tokenInterospectFuture =
        authService.tokenIntrospect(new JwtData(), jwtAuthenticationInfo);
    Future<JsonObject> createStackFuture = stackSevice.create(requestBody);

    validateSchemaFuture
        .onSuccess(validationSuccessHandler -> {
          tokenInterospectFuture.onSuccess(tokenIntrospectHandler -> {
            JsonObject authInfo = tokenIntrospectHandler.toJson();
            LOGGER.info("authInfo: " + authInfo);
            createStackFuture.onSuccess(stackServiceResult -> {
              LOGGER.debug("stackServiceResult : " + stackServiceResult);
              JsonArray results = stackServiceResult.getJsonArray("results");
              String stackId = results.getJsonObject(0).getString(ID);
              authInfo.put(IUDX_ID, stackId);
              authInfo.put(API, path);
              authInfo.put(HTTP_METHOD, REQUEST_POST);
              Future.future(fu -> updateAuditTable(authInfo));
              response.setStatusCode(201).end(stackServiceResult.toString());
            }).onFailure(stackServiceFailure -> {
              LOGGER.error(
                  "Fail: DB request has failed;"
                      + stackServiceFailure.getMessage());
              processBackendResponse(response, stackServiceFailure.getMessage());
            });

          }).onFailure(authFailure -> {
            processBackendResponse(response, generateAuthFailure(authFailure));
          });
        }).onFailure(validateFailure -> {
          respBuilder =
              new RespBuilder()
                  .withType(TYPE_INVALID_SCHEMA)
                  .withTitle(INVALID_SCHEMA_MSG)
                  .withDetail(DETAIL_INVALID_SCHEMA);
          processBackendResponse(response, respBuilder.getResponse());
        });

  }

  public void handlePatchStackRequest(RoutingContext routingContext) {
    LOGGER.debug("method handlePatchStackRequest() started");
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String path = routingContext.normalizedPath();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    JsonObject requestBody = routingContext.body().asJsonObject();
    JsonObject validationJson = requestBody.copy();
    String stacId = requestBody.getString(ID);
    validationJson.put("stack_type", "patch:Stack");

    Future<JsonObject> validateSchemaFuture = validatorService.validateSchema(validationJson);
    JwtAuthenticationInfo jwtAuthenticationInfo = new JwtAuthenticationInfo.Builder()
        .setToken(request.getHeader(HEADER_TOKEN))
        .setMethod(REQUEST_PATCH)
        .setApiEndpoint(api.getStackRestApis())
        .build();
//    JsonObject jwtAuthenticationInfo =
//        new JsonObject()
//            .put(TOKEN, request.getHeader(HEADER_TOKEN))
//            .put(METHOD, REQUEST_PATCH)
//            .put(API_ENDPOINT, api.getStackRestApis());
    Future<JwtData> tokenInterospectFuture =
        authService.tokenIntrospect(new JwtData(), jwtAuthenticationInfo);
    Future<JsonObject> updateStackFuture = stackSevice.update(requestBody);


    validateSchemaFuture
        .onSuccess(validationSuccessHandler -> {
          tokenInterospectFuture.onSuccess(tokenIntrospectHandler -> {
            JsonObject authInfo = tokenIntrospectHandler.toJson();
            LOGGER.info("authInfo: " + authInfo);
            authInfo.put(IUDX_ID, stacId);
            authInfo.put(API, path);
            authInfo.put(HTTP_METHOD, REQUEST_PATCH);
            updateStackFuture.onSuccess(stackServiceResult -> {
              LOGGER.debug("stackServiceResult : " + stackServiceResult);
              Future.future(fu -> updateAuditTable(authInfo));
              handleSuccessResponse(response, 201, stackServiceResult.toString());
            }).onFailure(stackServiceFailure -> {
              LOGGER.error(
                  "Fail: DB request has failed;"
                      + stackServiceFailure.getMessage());
              processBackendResponse(response, stackServiceFailure.getMessage());
            });
          }).onFailure(authFailure -> {
            processBackendResponse(response, generateAuthFailure(authFailure));
          });
        }).onFailure(validateFailure -> {
          respBuilder =
              new RespBuilder()
                  .withType(TYPE_INVALID_SCHEMA)
                  .withTitle(INVALID_SCHEMA_MSG)
                  .withDetail(DETAIL_INVALID_SCHEMA);
          processBackendResponse(response, respBuilder.getResponse());
        });
  }

  public void deleteStackHandler(RoutingContext routingContext) {
    LOGGER.debug("method deleteStackHandler() started");
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String path = routingContext.normalizedPath();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    String stacId = routingContext.queryParams().get(ID);
    LOGGER.debug("stackId:: {}", stacId);
    if (validateId(stacId)) {

      JwtAuthenticationInfo jwtAuthenticationInfo = new JwtAuthenticationInfo.Builder()
          .setToken(request.getHeader(HEADER_TOKEN))
          .setMethod(REQUEST_PATCH)
          .setApiEndpoint(api.getStackRestApis())
          .build();
//      JsonObject jwtAuthenticationInfo =
//          new JsonObject()
//              .put(TOKEN, request.getHeader(HEADER_TOKEN))
//              .put(METHOD, REQUEST_PATCH)
//              .put(API_ENDPOINT, api.getStackRestApis());
      Future<JwtData> tokenInterospectFuture =
          authService.tokenIntrospect(new JwtData(), jwtAuthenticationInfo);
      Future<JsonObject> deleteStackFuture = stackSevice.delete(stacId);

      tokenInterospectFuture.onSuccess(tokenIntrospectHandler -> {
        JsonObject authInfo = tokenIntrospectHandler.toJson();
        LOGGER.info("authInfo: " + authInfo);
        deleteStackFuture.onSuccess(stackServiceResult -> {
          LOGGER.debug("stackServiceResult : " + stackServiceResult);
          Future.future(fu -> updateAuditTable(authInfo));
          handleSuccessResponse(response, 200, stackServiceResult.toString());

        }).onFailure(stackServiceFailure -> {
          LOGGER.error(
              "Fail: DB request has failed;"
                  + stackServiceFailure.getMessage());
          processBackendResponse(response, stackServiceFailure.getMessage());
        });

      }).onFailure(authFailure -> {
        processBackendResponse(response, generateAuthFailure(authFailure));
      });
    } else {
      respBuilder =
          new RespBuilder()
              .withType(TYPE_INVALID_UUID)
              .withTitle(TITLE_INVALID_UUID)
              .withDetail("The id is invalid or not present");
      LOGGER.error("Invalid id : {}", stacId);
      processBackendResponse(response, respBuilder.getResponse());
    }
  }

  private String generateAuthFailure(Throwable authFailure) {
    LOGGER.error("auth failure: " + authFailure.getMessage());
    return
        new RespBuilder()
            .withType(TYPE_TOKEN_INVALID)
            .withTitle(TITLE_TOKEN_INVALID)
            .withDetail(authFailure.getMessage()).getResponse();

  }

  private boolean validateId(String itemId) {
    if (itemId.isEmpty() || itemId.isBlank()) {
      return false;
    }
    return UUID_PATTERN.matcher(itemId).matches();
  }

  private void processBackendResponse(HttpServerResponse response, String failureMessage) {
    int statusCode;

    try {
      JsonObject json = new JsonObject(failureMessage);
      switch (json.getString("type")) {
        case TYPE_ITEM_NOT_FOUND:
          statusCode = 404;
          break;
        case TYPE_CONFLICT:
          statusCode = 409;
          break;
        case TYPE_TOKEN_INVALID:
          statusCode = 401;
          break;
        case TYPE_INVALID_UUID:
        case TYPE_INVALID_SCHEMA:
          statusCode = 400;
          break;
        default:
          statusCode = 500;
          break;
      }
      response.setStatusCode(statusCode).end(failureMessage);
    } catch (DecodeException ex) {
      LOGGER.error("ERROR : Expecting Json from backend service [ jsonFormattingException ]");
      response.setStatusCode(400).end(BAD_REQUEST);
    }
  }

  private void handleSuccessResponse(HttpServerResponse response, int statusCode, String result) {
    response.putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(statusCode).end(result);
  }

  private void updateAuditTable(JsonObject jwtDecodedInfo) {
    LOGGER.info("Updating audit table on successful transaction");

    JsonObject auditInfo = jwtDecodedInfo;
    ZonedDateTime zst = ZonedDateTime.now();
    LOGGER.debug("TIME ZST: " + zst);
    long epochTime = getEpochTime(zst);
    auditInfo.put(EPOCH_TIME, epochTime).put(USERID, jwtDecodedInfo.getString(USER_ID));
    LOGGER.debug("audit data: " + auditInfo.encodePrettily());
    auditingService.insertAuditingValuesInRmq(auditInfo)
        .onSuccess(result -> {
          LOGGER.info("Message published in RMQ.");
        })
        .onFailure(err -> {
          LOGGER.error("Failed to publish message in RMQ.", err);
        });

  }

  private long getEpochTime(ZonedDateTime zst) {
    return zst.toInstant().toEpochMilli();
  }
}

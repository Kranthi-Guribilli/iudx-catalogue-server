package iudx.catalogue.server.apiserver;

import static iudx.catalogue.server.apiserver.util.Constants.APPLICATION_JSON;
import static iudx.catalogue.server.apiserver.util.Constants.BAD_REQUEST;
import static iudx.catalogue.server.apiserver.util.Constants.CONTENT_TYPE;
import static iudx.catalogue.server.apiserver.util.Constants.HEADER_CONTENT_TYPE;
import static iudx.catalogue.server.apiserver.util.Constants.HEADER_TOKEN;
import static iudx.catalogue.server.apiserver.util.Constants.MIME_APPLICATION_JSON;
import static iudx.catalogue.server.apiserver.util.Constants.ROUTE_STACK;
import static iudx.catalogue.server.apiserver.util.Constants.USERID;
import static iudx.catalogue.server.auditing.util.Constants.API;
import static iudx.catalogue.server.auditing.util.Constants.EPOCH_TIME;
import static iudx.catalogue.server.auditing.util.Constants.IUDX_ID;
import static iudx.catalogue.server.auditing.util.Constants.USER_ID;
import static iudx.catalogue.server.util.Constants.DETAIL_INVALID_SCHEMA;
import static iudx.catalogue.server.util.Constants.DOC_INDEX;
import static iudx.catalogue.server.util.Constants.HTTP_METHOD;
import static iudx.catalogue.server.util.Constants.ID;
import static iudx.catalogue.server.util.Constants.REQUEST_DELETE;
import static iudx.catalogue.server.util.Constants.REQUEST_PATCH;
import static iudx.catalogue.server.util.Constants.REQUEST_POST;
import static iudx.catalogue.server.util.Constants.TITLE_INVALID_UUID;
import static iudx.catalogue.server.util.Constants.TYPE_CONFLICT;
import static iudx.catalogue.server.util.Constants.TYPE_INVALID_SCHEMA;
import static iudx.catalogue.server.util.Constants.TYPE_INVALID_UUID;
import static iudx.catalogue.server.util.Constants.TYPE_ITEM_NOT_FOUND;
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
import iudx.catalogue.server.authenticator.handler.AuthHandler;
import iudx.catalogue.server.authenticator.model.JwtAuthenticationInfo;
import iudx.catalogue.server.authenticator.service.AuthenticationService;
import iudx.catalogue.server.common.ContextHelper;
import iudx.catalogue.server.database.elastic.service.ElasticsearchService;
import iudx.catalogue.server.exceptions.FailureHandler;
import iudx.catalogue.server.util.Api;
import iudx.catalogue.server.validator.service.ValidatorService;
import java.time.ZonedDateTime;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StacController {
  private static final Logger LOGGER = LogManager.getLogger(StacController.class);
  private final ElasticsearchService esService;
  private AuthenticationService authService;
  private ValidatorService validatorService;
  private AuditingService auditingService;
  private AuthHandler authHandler;
  private FailureHandler failureHandler;
  private Api api;
  private Router router;
  private StacSevice stackSevice;
  private RespBuilder respBuilder;

  public StacController(
      Router router,
      Api api,
      JsonObject config,
      ValidatorService validatorService,
      AuthenticationService authService,
      AuditingService auditingService,
      ElasticsearchService esService,
      AuthHandler authHandler,
      FailureHandler failureHandler) {
    this.api = api;
    this.router = router;
    this.authService = authService;
    this.validatorService = validatorService;
    this.auditingService = auditingService;
    this.esService = esService;
    this.authHandler = authHandler;
    this.failureHandler = failureHandler;
    stackSevice = new StacServiceImpl(esService, config.getString(DOC_INDEX));
  }

  Router init() {
    router
        .post(api.getStackRestApis())
        .handler(this::validateAuth)
        .handler(routingContext -> validateSchema(routingContext, REQUEST_POST))
        .handler(authHandler)
        .handler(this::handlePostStackRequest)
        .failureHandler(failureHandler);
    router
        .patch(api.getStackRestApis())
        .handler(this::validateAuth)
        .handler(routingContext -> validateSchema(routingContext, REQUEST_PATCH))
        .handler(authHandler)
        .handler(this::handlePatchStackRequest)
        .failureHandler(failureHandler);

    router.get(api.getStackRestApis()).handler(this::handleGetStackRequest);
    router
        .delete(api.getStackRestApis())
        .handler(this::validateAuth)
        .handler(routingContext -> validateSchema(routingContext, REQUEST_DELETE))
        .handler(authHandler)
        .handler(this::deleteStackHandler)
        .failureHandler(failureHandler);

    return this.router;
  }

  private void validateAuth(RoutingContext routingContext) {
    /* checking authentication info in requests */
    if (routingContext.request().headers().contains(HEADER_TOKEN)) {
      routingContext.next();
    } else {
      LOGGER.warn("Fail: Unauthorized CRUD operation");
      routingContext.response().setStatusCode(400).end(respBuilder.getResponse());
    }
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

  public void validateSchema(RoutingContext routingContext, String method) {
    LOGGER.debug("method validateSchema() started");
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);
    HttpServerRequest request = routingContext.request();
    JsonObject validationJson;
    if (!Objects.equals(method, REQUEST_DELETE)) {
      JsonObject requestBody = routingContext.body().asJsonObject();
      validationJson = requestBody.copy();
    } else {
      validationJson = new JsonObject();
    }

    switch (method) {
      case REQUEST_DELETE:
        break;
      case REQUEST_POST:
        validationJson.put("stack_type", "post:Stack");
        break;
      case REQUEST_PATCH:
        validationJson.put("stack_type", "patch:Stack");
        break;
    }

    if (!method.equals(REQUEST_DELETE)) {

      Future<JsonObject> validateSchemaFuture = validatorService.validateSchema(validationJson);
      JwtAuthenticationInfo jwtAuthenticationInfo =
          new JwtAuthenticationInfo.Builder()
              .setToken(request.getHeader(HEADER_TOKEN))
              .setMethod(method)
              .setApiEndpoint(ROUTE_STACK)
              .build();
      ContextHelper.setJwtAuthInfo(routingContext, jwtAuthenticationInfo);

      validateSchemaFuture
          .onSuccess(
              validationSuccessHandler -> {
                ContextHelper.setValidatedRequest(routingContext, validationJson);
                routingContext.next();
              })
          .onFailure(
              validateFailure -> {
                respBuilder =
                    new RespBuilder()
                        .withType(TYPE_INVALID_SCHEMA)
                        .withTitle(INVALID_SCHEMA_MSG)
                        .withDetail(DETAIL_INVALID_SCHEMA);
                processBackendResponse(response, respBuilder.getResponse());
              });
    } else {
      String stacId = routingContext.queryParams().get(ID);
      LOGGER.debug("stackId:: {}", stacId);
      if (validateId(stacId)) {
        JwtAuthenticationInfo jwtAuthenticationInfo =
            new JwtAuthenticationInfo.Builder()
                .setToken(request.getHeader(HEADER_TOKEN))
                .setMethod(method)
                .setApiEndpoint(ROUTE_STACK)
                .build();
        ContextHelper.setJwtAuthInfo(routingContext, jwtAuthenticationInfo);
        routingContext.next();
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
  }

  public void handlePostStackRequest(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    JsonObject validatedRequestBody = ContextHelper.getValidatedRequest(routingContext);
    String path = routingContext.normalizedPath();
    Future<JsonObject> createStackFuture = stackSevice.create(validatedRequestBody);

    JsonObject authInfo = ContextHelper.getJwtDecodedInfo(routingContext).toJson();
    createStackFuture
        .onSuccess(
            stackServiceResult -> {
              LOGGER.debug("stackServiceResult : " + stackServiceResult);
              JsonArray results = stackServiceResult.getJsonArray("results");
              String stackId = results.getJsonObject(0).getString(ID);
              authInfo.put(IUDX_ID, stackId);
              authInfo.put(API, path);
              authInfo.put(HTTP_METHOD, REQUEST_POST);
              Future.future(fu -> updateAuditTable(authInfo));
              response.setStatusCode(201).end(stackServiceResult.toString());
            })
        .onFailure(
            stackServiceFailure -> {
              LOGGER.error("Fail: DB request has failed;" + stackServiceFailure.getMessage());
              processBackendResponse(response, stackServiceFailure.getMessage());
            });
  }

  public void handlePatchStackRequest(RoutingContext routingContext) {
    LOGGER.debug("method handlePatchStackRequest() started");
    HttpServerResponse response = routingContext.response();
    String path = routingContext.normalizedPath();

    JsonObject validatedRequestBody = ContextHelper.getValidatedRequest(routingContext);
    String stacId = validatedRequestBody.getString(ID);

    Future<JsonObject> updateStackFuture = stackSevice.update(validatedRequestBody);

    JsonObject authInfo = ContextHelper.getJwtDecodedInfo(routingContext).toJson();
    authInfo.put(IUDX_ID, stacId);
    authInfo.put(API, path);
    authInfo.put(HTTP_METHOD, REQUEST_PATCH);
    updateStackFuture
        .onSuccess(
            stackServiceResult -> {
              LOGGER.debug("stackServiceResult : " + stackServiceResult);
              Future.future(fu -> updateAuditTable(authInfo));
              response.setStatusCode(201).end(stackServiceResult.toString());
            })
        .onFailure(
            stackServiceFailure -> {
              LOGGER.error("Fail: DB request has failed;" + stackServiceFailure.getMessage());
              processBackendResponse(response, stackServiceFailure.getMessage());
            });
  }

  public void deleteStackHandler(RoutingContext routingContext) {
    LOGGER.debug("method deleteStackHandler() started");
    HttpServerResponse response = routingContext.response();

    String stacId = routingContext.queryParams().get(ID);
    Future<JsonObject> deleteStackFuture = stackSevice.delete(stacId);

    JsonObject authInfo = ContextHelper.getJwtDecodedInfo(routingContext).toJson();
    deleteStackFuture
        .onSuccess(
            stackServiceResult -> {
              LOGGER.debug("stackServiceResult : " + stackServiceResult);
              Future.future(fu -> updateAuditTable(authInfo));
              handleSuccessResponse(response, 200, stackServiceResult.toString());
            })
        .onFailure(
            stackServiceFailure -> {
              LOGGER.error("Fail: DB request has failed;" + stackServiceFailure.getMessage());
              processBackendResponse(response, stackServiceFailure.getMessage());
            });
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
    auditingService
        .insertAuditingValuesInRmq(auditInfo)
        .onSuccess(
            result -> {
              LOGGER.info("Message published in RMQ.");
            })
        .onFailure(
            err -> {
              LOGGER.error("Failed to publish message in RMQ.", err);
            });
  }

  private long getEpochTime(ZonedDateTime zst) {
    return zst.toInstant().toEpochMilli();
  }
}

package iudx.catalogue.server.apiserver.crud;

import static iudx.catalogue.server.apiserver.util.Constants.*;
import static iudx.catalogue.server.auditing.util.Constants.API;
import static iudx.catalogue.server.auditing.util.Constants.EPOCH_TIME;
import static iudx.catalogue.server.auditing.util.Constants.IUDX_ID;
import static iudx.catalogue.server.common.util.ResponseUtils.invalidSchemaResponse;
import static iudx.catalogue.server.common.util.ResponseUtils.invalidSyntaxResponse;
import static iudx.catalogue.server.common.util.ResponseUtils.invalidUuidResponse;
import static iudx.catalogue.server.common.util.ResponseUtils.itemNotFoundResponse;
import static iudx.catalogue.server.common.util.ResponseUtils.linkValidationFailureResponse;
import static iudx.catalogue.server.rating.util.Constants.USER_ID;
import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.catalogue.server.apiserver.Item.model.*;
import iudx.catalogue.server.auditing.service.AuditingService;
import iudx.catalogue.server.authenticator.handler.AuthHandler;
import iudx.catalogue.server.authenticator.handler.ValidateAccessHandler;
import iudx.catalogue.server.authenticator.model.DxRole;
import iudx.catalogue.server.authenticator.model.JwtAuthenticationInfo;
import iudx.catalogue.server.authenticator.model.JwtAuthenticationInfo.Builder;
import iudx.catalogue.server.authenticator.model.JwtAuthenticationInfo.MutableJwtInfo;
import iudx.catalogue.server.authenticator.model.JwtData;
import iudx.catalogue.server.common.ContextHelper;
import iudx.catalogue.server.exceptions.FailureHandler;
import iudx.catalogue.server.validator.service.ValidatorService;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class handles CRUD (Create, Read, Update, Delete) operations for the catalogue items.
 * <p>
 * It manages routes for CRUD endpoints, validation of request payloads, and the coordination
 * <p>
 * between services for item operations.
 */
public class CrudController {
  private static final Pattern UUID_PATTERN =
      Pattern.compile(
          "^[a-zA-Z0-9]{8}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{12}$");
  private final Logger LOGGER = LogManager.getLogger(CrudController.class);
  private final Router router;
  private final CrudService crudService;
  private final ValidatorService validatorService;
  private final boolean isUac;
  private final AuthHandler authHandler;
  private final ValidateAccessHandler validateAccessHandler;
  private final FailureHandler failureHandler;
  private final String host;
  private AuditingService auditingService;
  private boolean hasAuditService;

  /**
   * CrudController constructor.
   *
   * @param isUac       flag indicating if UAC is enabled
   * @param crudService service for CRUD operations
   */
  public CrudController(Router router, boolean isUac, String host, CrudService crudService,
                        ValidatorService validatorService,
                        AuthHandler authHandler, ValidateAccessHandler validateAccessHandler,
                        FailureHandler failureHandler) {
    this.router = router;
    this.isUac = isUac;
    this.host = host;
    this.crudService = crudService;
    this.validatorService = validatorService;
    this.authHandler = authHandler;
    this.validateAccessHandler = validateAccessHandler;
    this.failureHandler = failureHandler;

    setupRoutes();
  }

  /**
   * Sets the auditing service to log and track requests. Enables audit logging when set.
   *
   * @param auditingService The AuditingService instance to set.
   */
  public void setAuditingService(AuditingService auditingService) {
    this.auditingService = auditingService;
    hasAuditService = true;
  }

  /**
   * Configures the routes for CRUD operations, including creation, update, retrieval, and deletion
   * <p>
   * of items, along with validation, authorization, and auditing functionalities.
   */
  public void setupRoutes() {

    /* Create Item - Body contains data */
    router
        .post(ROUTE_ITEMS)
        .consumes(MIME_APPLICATION_JSON)
        .produces(MIME_APPLICATION_JSON)
        .handler(this::validateAuth)
        .handler(this::validateItemSchema)
        .handler(authHandler)
        .handler(this::itemLinkValidation)
        .handler(validateAccessHandler.forRoleAndEntityAccess(DxRole.COS_ADMIN, DxRole.ADMIN,
            DxRole.PROVIDER, DxRole.DELEGATE))
        .handler(this::createOrUpdateItemHandler)
        .handler(this::auditHandler)
        .failureHandler(failureHandler);

    /* Update Item - Body contains data */
    router
        .put(ROUTE_UPDATE_ITEMS)
        .consumes(MIME_APPLICATION_JSON)
        .produces(MIME_APPLICATION_JSON)
        .handler(this::validateAuth)
        .handler(this::validateItemSchema)
        .handler(authHandler)
        .handler(this::itemLinkValidation)
        .handler(validateAccessHandler.forRoleAndEntityAccess(DxRole.COS_ADMIN, DxRole.ADMIN,
            DxRole.PROVIDER, DxRole.DELEGATE))
        .handler(this::createOrUpdateItemHandler)
        .handler(this::auditHandler)
        .failureHandler(failureHandler);

    /* Get Item */
    router
        .get(ROUTE_ITEMS)
        .produces(MIME_APPLICATION_JSON)
        .handler(this::getItemHandler);

    /* Delete Item - Query param contains id */
    router
        .delete(ROUTE_DELETE_ITEMS)
        .produces(MIME_APPLICATION_JSON)
        .handler(this::validateAuth)
        .handler(this::validateDeleteItemHandler)
        .handler(authHandler)
        .handler(validateAccessHandler.forRoleAndEntityAccess(DxRole.COS_ADMIN, DxRole.ADMIN,
            DxRole.PROVIDER, DxRole.DELEGATE))
        .handler(this::deleteItemHandler)
        .handler(this::auditHandler);

    /* Create instance - Instance name in query param */
    router
        .post(ROUTE_INSTANCE)
        .produces(MIME_APPLICATION_JSON)
        .handler(this::validateAuth)
        .handler(routingContext -> populateAuthInfo(routingContext, REQUEST_POST))
        // Populate authentication info
        .handler(authHandler) // Authentication
        .handler(this::createInstanceHandler)
        .failureHandler(failureHandler);

    /* Delete instance - Instance name in query param */
    router
        .delete(ROUTE_INSTANCE)
        .produces(MIME_APPLICATION_JSON)
        .handler(this::validateAuth)
        .handler(routingContext -> populateAuthInfo(routingContext, REQUEST_DELETE))
        // Populate authentication info
        .handler(authHandler) // Authentication
        .handler(this::deleteInstanceHandler)
        .failureHandler(failureHandler);

  }

  // Method to return the router for mounting
  public Router getRouter() {
    return this.router;
  }

  /**
   * Validates the authorization of the incoming request. Checks if the request contains a
   * token field.
   *
   * @param routingContext {@link RoutingContext}
   */
  private void validateAuth(RoutingContext routingContext) {
    /* checking authentication info in requests */
    if (routingContext.request().headers().contains(HEADER_TOKEN)) {
      routingContext.next();
    } else {
      LOGGER.warn("Fail: Unauthorized CRUD operation");
      routingContext.response().setStatusCode(401).end();
    }
  }

  private void populateAuthInfo(RoutingContext routingContext, String method) {

    HttpServerRequest request = routingContext.request();

    JwtAuthenticationInfo jwtAuthenticationInfo = new JwtAuthenticationInfo.Builder()
        .setToken(request.getHeader(HEADER_TOKEN))
        .setMethod(method)
        .setApiEndpoint(ROUTE_INSTANCE)
        .setItemType(ITEM_TYPE_INSTANCE)
        .setId(host)
        .build();

    ContextHelper.setJwtAuthInfo(routingContext, jwtAuthenticationInfo);
    routingContext.next();
  }

  /**
   * Create/Update Item.
   * Validates the schema of the item being created or updated by performing JSON schema validation.
   * Populates JWT authentication information for further processing.
   *
   * @param routingContext {@link RoutingContext}
   * @TODO Throw error if load failed
   */
  // tag::db-service-calls[]
  private void validateItemSchema(RoutingContext routingContext) {
    LOGGER.debug("Info: Creating/Updating item");
    /* Contains the cat-item */
    JsonObject requestBody = routingContext.body().asJsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    String itemType = getItemType(requestBody, response);

    LOGGER.debug("Info: itemType: " + itemType);
    ContextHelper.setItemType(routingContext, itemType);
    Item item = createItemFromType(itemType, requestBody);
    LOGGER.debug("Success: Schema validation");

    // populating jwt authentication info ->
    JwtAuthenticationInfo jwtAuthenticationInfo = new Builder()
        .setToken(request.getHeader(HEADER_TOKEN))
        .setMethod(REQUEST_POST)
        .setApiEndpoint(ROUTE_ITEMS)
        .setItemType(itemType)
        .build();
    if (isUac) {
      ContextHelper.setJwtAuthInfo(routingContext, jwtAuthenticationInfo);
      ContextHelper.setValidatedRequest(routingContext, requestBody);
      routingContext.next();
    } else {
      MutableJwtInfo mutableJwtInfo = new MutableJwtInfo(jwtAuthenticationInfo);
      handleItemTypeCases(routingContext, requestBody, response, mutableJwtInfo, itemType);
    }
  }

  /**
   * Extracts the item type from the provided JSON request body.
   *
   * @param requestBody the {@link JsonObject}
   * @param response    the {@link HttpServerResponse} object used to send an error response if the item type is invalid or missing.
   * @return a {@link String} representing the valid item type extracted from the request body.
   * Returns an empty string if no valid type is found.
   * @throws IllegalArgumentException if the type field is not present or invalid in the request body.
   */

  private String getItemType(JsonObject requestBody, HttpServerResponse response) {
    Set<String> type = new HashSet<String>(new JsonArray().getList());
    try {
      type = new HashSet<String>(requestBody.getJsonArray(TYPE).getList());
    } catch (Exception e) {
      LOGGER.error("Fail: Invalid type");
      response.setStatusCode(400)
          .end(invalidSchemaResponse("Invalid type for item/type not present"));
    }
    type.retainAll(ITEM_TYPES);
    String itemType = type.toString().replaceAll("\\[", "").replaceAll("\\]", "");
    return itemType;
  }

  // Helper method to create Item based on item type
  private Item createItemFromType(String itemType, JsonObject requestBody) {
    switch (itemType) {
      case ITEM_TYPE_OWNER:
        return new Owner(requestBody);
      case ITEM_TYPE_COS:
        return new COS(requestBody);
      case ITEM_TYPE_RESOURCE_SERVER:
        return new ResourceServer(requestBody);
      case ITEM_TYPE_PROVIDER:
        return new Provider(requestBody);
      case ITEM_TYPE_RESOURCE_GROUP:
        return new ResourceGroup(requestBody);
      case ITEM_TYPE_RESOURCE:
        return new Resource(requestBody);
      default:
        throw new IllegalArgumentException("Invalid item type: " + itemType);
    }
  }

  private void handleItemTypeCases(RoutingContext routingContext, JsonObject requestBody,
                                   HttpServerResponse response,
                                   MutableJwtInfo mutableJwtInfo,
                                   String itemType) {
    if (itemType.equalsIgnoreCase(ITEM_TYPE_COS)
        || itemType.equalsIgnoreCase(ITEM_TYPE_OWNER)
        || itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_SERVER)) {
      ContextHelper.setJwtAuthInfo(routingContext, mutableJwtInfo.get());
      ContextHelper.setValidatedRequest(routingContext, requestBody);
      routingContext.next();
    } else if (itemType.equals(ITEM_TYPE_PROVIDER)) {
      Future<JsonObject> resourceServerUrlFuture =
          getParentObjectInfo(requestBody.getString(RESOURCE_SVR));
      // add resource server url to provider body
      resourceServerUrlFuture.onComplete(resourceServerUrl -> {
        if (resourceServerUrl.succeeded()) {
          String rsUrl = resourceServerUrl.result().getString(RESOURCE_SERVER_URL);
          // used for relationship apis
          String cosId = resourceServerUrl.result().getString(COS_ITEM);

          mutableJwtInfo.set(new Builder(mutableJwtInfo.get())
              .setResourceServerUrl(rsUrl)
              .build());
          requestBody.put(RESOURCE_SERVER_URL, rsUrl);
          requestBody.put(COS_ITEM, cosId);
          ContextHelper.setJwtAuthInfo(routingContext, mutableJwtInfo.get());
          ContextHelper.setValidatedRequest(routingContext, requestBody);
          routingContext.next();
        } else {
          response.setStatusCode(400)
              .end(linkValidationFailureResponse("Resource Server not found"));
        }
      });
    } else {
      Future<JsonObject> ownerUserIdFuture =
          getParentObjectInfo(requestBody.getString(PROVIDER));
      // add provider kc id to requestBody
      ownerUserIdFuture.onComplete(ownerUserId -> {
        if (ownerUserId.succeeded()) {
          String kcId = ownerUserId.result().getString(PROVIDER_USER_ID);
          String rsUrl = ownerUserId.result().getString(RESOURCE_SERVER_URL);

          mutableJwtInfo.set(new Builder(mutableJwtInfo.get())
              .setProviderUserId(kcId)
              .setResourceServerUrl(rsUrl)
              .build());
          // cosId is used for relationship apis
          String cosId = ownerUserId.result().getString(COS_ITEM);
          requestBody.put(PROVIDER_USER_ID, kcId);
          requestBody.put(COS_ITEM, cosId);

          ContextHelper.setJwtAuthInfo(routingContext, mutableJwtInfo.get());
          ContextHelper.setValidatedRequest(routingContext, requestBody);
          routingContext.next();
        } else {
          response.setStatusCode(400)
              .end(linkValidationFailureResponse("Provider not found"));
        }
      });
    }
  }

  private void itemLinkValidation(RoutingContext routingContext) {

    HttpServerResponse response = routingContext.response();
    JsonObject requestBody = ContextHelper.getValidatedRequest(routingContext);

    requestBody.put(HTTP_METHOD, routingContext.request().method().toString());
    /* Link Validating the request to ensure item correctness */
    validatorService.validateItem(requestBody).onComplete(valHandler -> {
      if (valHandler.failed()) {
        LOGGER.error("Fail: Item validation failed;" + valHandler.cause().getMessage());
        if (valHandler.cause().getMessage().contains("validation failed. Incorrect id")) {
          response.setStatusCode(400)
              .end(invalidUuidResponse("Syntax of the UUID is incorrect"));
        } else {
          response.setStatusCode(400)
              .end(linkValidationFailureResponse(valHandler.cause().getMessage()));
        }
        return;
      }

      LOGGER.debug("Success: Item link validation");
      ContextHelper.setValidatedRequest(routingContext, valHandler.result());
      routingContext.next();
    });
  }

  private void createOrUpdateItemHandler(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    JsonObject validatedRequest = ContextHelper.getValidatedRequest(routingContext);

    // If post then create. Else, update
    if (routingContext.request().method().toString().equals(REQUEST_POST)) {
      crudService.createItem(validatedRequest).onComplete(itemHandler -> {
        if (itemHandler.failed()) {
          LOGGER.error("Failed to create item: " + itemHandler.cause().getMessage());
          response
              .setStatusCode(400)
              .end(itemHandler.cause().getMessage());
        } else {
          JsonObject createdItem = itemHandler.result();
          response
              .setStatusCode(201)
              .end(createdItem.encodePrettily());
          routingContext.next();
        }
      });
    } else {
      crudService.updateItem(validatedRequest).onComplete(itemHandler -> {
        if (itemHandler.failed()) {
          LOGGER.error("Failed to update item: " + itemHandler.cause().getMessage());
          if (itemHandler.cause().getMessage().contains("Doc doesn't exist")) {
            routingContext.response()
                .setStatusCode(404)
                .end(itemHandler.cause().getMessage());
          } else {
            routingContext.response()
                .setStatusCode(400)
                .end(itemHandler.cause().getMessage());
          }
        } else {
          JsonObject updatedItem = itemHandler.result();
          routingContext.response()
              .setStatusCode(200)
              .end(updatedItem.encodePrettily());
          routingContext.next();
        }
      });
    }
  }

  /**
   * Get Item.
   *
   * @param routingContext {@link RoutingContext}
   * @TODO Throw error if load failed
   */
  // tag::db-service-calls[]
  public void getItemHandler(RoutingContext routingContext) {

    /* Id in path param */
    HttpServerResponse response = routingContext.response();

    String itemId = routingContext.queryParams().get(ID);

    LOGGER.debug("Info: Getting item; id=" + itemId);

    JsonObject requestBody = new JsonObject().put(ID, itemId);
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    if (validateId(itemId)) {

      crudService.getItem(requestBody).onComplete(getHandler -> {
        if (getHandler.succeeded()) {
          JsonObject retrievedItem = getHandler.result();
          response.setStatusCode(200).end(retrievedItem.toString());
        } else {
          if (getHandler.cause() instanceof NoSuchElementException) {
            LOGGER.error("Fail: Item not found");
            JsonObject errorResponse = new JsonObject()
                .put(STATUS, ERROR)
                .put(TYPE, TYPE_ITEM_NOT_FOUND)
                .put(DETAIL, "doc doesn't exist");
            response.setStatusCode(404).end(errorResponse.toString());
          } else {
            LOGGER.error("Fail: Item retrieval failed; " + getHandler.cause().getMessage());
            response.setStatusCode(400).end(getHandler.cause().getMessage());
          }
        }
      });
    } else {
      LOGGER.error("Fail: Invalid request payload");
      response.setStatusCode(400)
          .end(invalidUuidResponse("The id is invalid"));
    }
  }

  /**
   * Validates the request for deleting an item by checking the item's existence and setting authentication info.
   * Continues to the next handler if validation succeeds.
   *
   * @param routingContext {@link RoutingContext} @TODO Throw error if load failed
   */
  // tag::db-service-calls[]
  public void validateDeleteItemHandler(RoutingContext routingContext) {

    /* Id in path param */
    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    String itemId = routingContext.queryParams().get(ID);

    LOGGER.debug("Info: Deleting item; id=" + itemId);

    //  populating JWT authentication info ->
    HttpServerRequest request = routingContext.request();

    /* authentication related information */
    JwtAuthenticationInfo.Builder jwtInfoBuilder = new Builder()
        .setToken(request.getHeader(HEADER_TOKEN))
        .setMethod(REQUEST_DELETE)
        .setApiEndpoint(ROUTE_ITEMS);

    if (validateId(itemId)) {
      Future<JsonObject> itemTypeFuture = getParentObjectInfo(itemId);
      itemTypeFuture.onComplete(
          itemTypeHandler -> {
            if (itemTypeHandler.succeeded()) {

              Set<String> types =
                  new HashSet<String>(itemTypeHandler.result().getJsonArray(TYPE).getList());
              types.retainAll(ITEM_TYPES);
              String itemType = types.toString().replaceAll("\\[", "")
                  .replaceAll("\\]", "");
              LOGGER.debug("itemType : {} ", itemType);
              ContextHelper.setItemType(routingContext, itemType);
              jwtInfoBuilder.setItemType(itemType);
              if (isUac) {
                ContextHelper.setJwtAuthInfo(routingContext, jwtInfoBuilder.build());
                routingContext.next();
              } else {
                if (itemType.equalsIgnoreCase(ITEM_TYPE_COS)
                    || itemType.equalsIgnoreCase(ITEM_TYPE_OWNER)
                    || itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_SERVER)) {
                  ContextHelper.setJwtAuthInfo(routingContext, jwtInfoBuilder.build());
                  routingContext.next();
                } else if (itemType.equalsIgnoreCase(ITEM_TYPE_PROVIDER)) {
                  LOGGER.debug(itemTypeHandler.result());
                  String rsUrl = itemTypeHandler.result().getString(RESOURCE_SERVER_URL, "");
                  jwtInfoBuilder.setResourceServerUrl(rsUrl);
                  ContextHelper.setJwtAuthInfo(routingContext, jwtInfoBuilder.build());
                  routingContext.next();
                } else {
                  LOGGER.debug(itemTypeHandler.result());
                  Future<JsonObject> providerInfoFuture =
                      getParentObjectInfo(itemTypeHandler.result().getString(PROVIDER));
                  providerInfoFuture.onComplete(
                      providerInfo -> {
                        if (providerInfo.succeeded()) {
                          LOGGER.debug(providerInfo.result());
                          String rsUrl = providerInfo.result().getString(RESOURCE_SERVER_URL, "");
                          String ownerUserId =
                              providerInfo.result().getString(PROVIDER_USER_ID, "");
                          jwtInfoBuilder.setResourceServerUrl(rsUrl)
                              .setProviderUserId(ownerUserId);
                          ContextHelper.setJwtAuthInfo(routingContext, jwtInfoBuilder.build());
                          routingContext.next();
                        } else {
                          response
                              .setStatusCode(400)
                              .end(itemNotFoundResponse("item is not found"));
                        }
                      });
                }
              }
            } else {
              if (itemTypeHandler.cause().getMessage().contains(TYPE_ITEM_NOT_FOUND)) {
                response.setStatusCode(404).end(itemTypeHandler.cause().getMessage());
              } else {
                response.setStatusCode(400).end(itemTypeHandler.cause().getMessage());
              }
            }
          });
    } else {
      LOGGER.error("Fail: Invalid request payload");
      response
          .setStatusCode(400)
          .end(invalidSyntaxResponse("Fail: The syntax of the id is incorrect"));
    }
  }

  /**
   * Delete Item.
   *
   * @param routingContext {@link RoutingContext}
   */
  public void deleteItemHandler(RoutingContext routingContext) {
    JsonObject requestBody = new JsonObject();
    String itemId = routingContext.queryParams().get(ID);
    requestBody.put(ID, itemId);

    crudService.deleteItem(requestBody)
        .onSuccess(result -> {
          LOGGER.info("Item deleted successfully");
          routingContext.response()
              .setStatusCode(200)
              .end(result.toString());
          routingContext.next();
        })
        .onFailure(throwable -> {
          LOGGER.error("Failed to delete item", throwable);
          if (throwable instanceof NoSuchElementException) {
            routingContext.response()
                .setStatusCode(404)
                .end(itemNotFoundResponse("Item not found"));
          } else {
            routingContext.response()
                .setStatusCode(400)
                .end(throwable.getMessage());
          }
        });
  }

  Future<JsonObject> getParentObjectInfo(String itemId) {
    Promise<JsonObject> promise = Promise.promise();
    List<String> includeFields = List.of(TYPE, PROVIDER, PROVIDER_USER_ID, RESOURCE_GRP,
        RESOURCE_SVR, RESOURCE_SERVER_URL, COS, COS_ADMIN);
    JsonObject req = new JsonObject().put(ID, itemId)
        .put(INCLUDE_FIELDS, includeFields);

    LOGGER.debug(req);
    crudService.getItem(req)
        .onComplete(handler -> {
          if (handler.succeeded()) {
            if (handler.result().getInteger(TOTAL_HITS) != 1) {
              promise.fail(
                  itemNotFoundResponse("Fail: Doc doesn't exist, can't perform operation"));
            } else {
              promise.complete(handler.result().getJsonArray("results").getJsonObject(0));
            }
          } else {
            promise.fail(handler.cause());
          }
        });
    return promise.future();
  }

  /**
   * Creates a new catalogue instance and handles the request/response flow.
   *
   * @param routingContext the routing context for handling HTTP requests and responses
   * @throws RuntimeException if item creation fails
   */
  public void createInstanceHandler(RoutingContext routingContext) {

    LOGGER.debug("Info: Creating new instance");

    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    String instance = routingContext.queryParams().get(ID);

    /* INSTANCE = "" to make sure createItem can be used for onboarding instance and items */
    JsonObject body = new JsonObject().put(ID, instance)
        .put(TYPE, new JsonArray().add(ITEM_TYPE_INSTANCE))
        .put(INSTANCE, "");
    crudService.createItem(body).onComplete(res -> {
      if (res.succeeded()) {
        LOGGER.info("Success: Instance created;");
        response.setStatusCode(201)
            .end(res.result().toString());
        // TODO: call auditing service here
      } else {
        LOGGER.error("Fail: Creating instance");
        response.setStatusCode(400).end(res.cause().getMessage());
      }
    });
    LOGGER.debug("Success: Authenticated instance creation request");
  }

  /**
   * Deletes the specified instance from the database.
   *
   * @param routingContext the routing context
   * @throws NullPointerException if routingContext is null
   * @throws RuntimeException     if the instance cannot be deleted
   * @TODO call auditing service after successful deletion
   */
  public void deleteInstanceHandler(RoutingContext routingContext) {

    LOGGER.debug("Info: Deleting instance");

    HttpServerResponse response = routingContext.response();
    response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

    String instance = routingContext.queryParams().get(ID);

    /* INSTANCE = "" to make sure createItem can be used for onboarding instance and items */
    JsonObject body = new JsonObject().put(ID, instance)
        .put(INSTANCE, "");
    crudService.deleteItem(body).onComplete(res -> {
      if (res.succeeded()) {
        LOGGER.info("Success: Instance deleted;");
        response.setStatusCode(200)
            .end(res.result().toString());
        // TODO: call auditing service here
      } else {
        LOGGER.error("Fail: Deleting instance");
        response.setStatusCode(404).end(res.cause().getMessage());
      }
    });
    LOGGER.debug("Success: Authenticated instance creation request");
  }

  /**
   * Check if the itemId contains certain invalid characters.
   *
   * @param itemId which is a String
   * @return true if the item ID contains invalid characters, false otherwise
   */
  private boolean validateId(String itemId) {
    return UUID_PATTERN.matcher(itemId).matches();

  }

  private void auditHandler(RoutingContext routingContext) {
    JwtData jwtDecodedInfo = ContextHelper.getJwtDecodedInfo(routingContext);
    String id;
    String httpMethod = routingContext.request().method().toString();

    if (httpMethod.equals(REQUEST_DELETE)) {
      id = routingContext.queryParams().get(ID);
    } else {
      id = ContextHelper.getValidatedRequest(routingContext).getString(ID);
    }

    if (hasAuditService && !isUac) {
      updateAuditTable(
          jwtDecodedInfo.toJson(),
          new String[] {id,
              ROUTE_ITEMS, httpMethod});
    }
  }

  /**
   * function to handle call to audit service.
   *
   * @param jwtDecodedInfo contains the user-role, user-id, iid
   * @param otherInfo      contains item-id, api-endpoint and the HTTP method.
   */
  private void updateAuditTable(JsonObject jwtDecodedInfo, String[] otherInfo) {
    LOGGER.info("Updating audit table on successful transaction");

    JsonObject auditInfo = jwtDecodedInfo;
    ZonedDateTime zst = ZonedDateTime.now();
    LOGGER.debug("TIME ZST: " + zst);
    long epochTime = getEpochTime(zst);
    auditInfo.put(IUDX_ID, otherInfo[0])
        .put(API, otherInfo[1])
        .put(HTTP_METHOD, otherInfo[2])
        .put(EPOCH_TIME, epochTime)
        .put(USERID, jwtDecodedInfo.getString(USER_ID));

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

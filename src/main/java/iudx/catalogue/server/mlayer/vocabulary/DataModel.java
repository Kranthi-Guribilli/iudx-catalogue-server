package iudx.catalogue.server.mlayer.vocabulary;

import static iudx.catalogue.server.database.Constants.GET_ALL_DATASETS_BY_RS_GRP;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import iudx.catalogue.server.database.ElasticClient;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataModel {
  private static final Logger LOGGER = LogManager.getLogger(DataModel.class);
  private static final int MAX_CONCURRENT_REQUESTS = 10; // limit to 10 concurrent requests
  private final ElasticClient client;
  private final WebClient webClient;
  private final String docIndex;
  private final Semaphore semaphore = new Semaphore(MAX_CONCURRENT_REQUESTS);

  /**
   * Constructor for DataModel.
   *
   * @param client The ElasticClient instance
   * @param docIndex The index name where data are stored/retrieved in elastic.
   */
  public DataModel(ElasticClient client, String docIndex) {
    WebClientOptions options =
        new WebClientOptions().setMaxPoolSize(10).setKeepAlive(true).setConnectTimeout(5000);
    this.webClient = WebClient.create(Vertx.vertx(), options);
    this.client = client;
    this.docIndex = docIndex;
  }

  /**
   * Retrieves data model information asynchronously.
   *
   * @return Future containing JsonObject with id to subclass mappings.
   */
  public Future<JsonObject> getDataModelInfo() {
    Promise<JsonObject> promise = Promise.promise();

    // Get All datasets by resource group
    Future<JsonArray> searchFuture = getAllDatasetsByRsGrp();

    // Process search results when completed and Fetch data models asynchronously
    searchFuture
        .compose(this::fetchDataModels)
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
                promise.complete(ar.result());
              } else {
                promise.complete(new JsonObject());
              }
            });

    return promise.future();
  }

  /**
   * Performs Elasticsearch search asynchronously.
   *
   * @return Future containing JsonArray of search results.
   */
  private Future<JsonArray> getAllDatasetsByRsGrp() {
    Promise<JsonArray> promise = Promise.promise();

    client.searchAsync(
        GET_ALL_DATASETS_BY_RS_GRP,
        docIndex,
        searchHandler -> {
          if (searchHandler.succeeded()) {
            LOGGER.debug("Successful Elastic request");
            JsonObject response = searchHandler.result();
            JsonArray results = response.getJsonArray("results");
            promise.complete(results);
          } else {
            LOGGER.error("Failed Elastic Request: {}", searchHandler.cause().getMessage());
            promise.complete(new JsonArray());
          }
        });

    return promise.future();
  }

  /**
   * Fetches data models asynchronously.
   *
   * @param results The JsonArray of results from Elasticsearch search.
   * @return Future containing JsonObject with id to subclass mappings.
   */
  private Future<JsonObject> fetchDataModels(JsonArray results) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject idToSubClassMap = new JsonObject();

    if (results.isEmpty()) {
      promise.complete(idToSubClassMap);
      return promise.future();
    }

    Set<String> uniqueClassIds = new HashSet<>();
    String contextUrl = results.getJsonObject(0).getString("@context");
    Map<String, String> idToClassIdMap = new HashMap<>();
    for (int i = 0; i < results.size(); i++) {
      JsonObject result = results.getJsonObject(i);
      String id = result.getString("id");
      JsonArray typeArray = result.getJsonArray("type");

      if (typeArray == null || typeArray.size() < 2) {
        LOGGER.error("Invalid type array in result: {}", result.encode());
        continue;
      }

      String type = typeArray.getString(1);
      String classId = type.split(":")[1];
      uniqueClassIds.add(classId);
      idToClassIdMap.put(id, classId);
    }

    AtomicInteger pendingRequests = new AtomicInteger(uniqueClassIds.size());

    for (String classId : uniqueClassIds) {
      String dmUrl = contextUrl + classId + ".jsonld";
      acquireSemaphoreAndFetchDataModel(
          classId, dmUrl, idToClassIdMap, idToSubClassMap, pendingRequests, promise);
    }

    return promise.future();
  }

  private void acquireSemaphoreAndFetchDataModel(
      String classId,
      String dmUrl,
      Map<String, String> idToClassIdMap,
      JsonObject idToSubClassMap,
      AtomicInteger pendingRequests,
      Promise<JsonObject> promise) {
    try {
      semaphore.acquire();
      webClient
          .getAbs(dmUrl)
          .send(
              dmAr -> {
                handleDataModelResponse(
                    dmAr,
                    classId,
                    idToClassIdMap,
                    idToSubClassMap,
                    pendingRequests,
                    promise,
                    dmUrl);
                semaphore.release();
              });
    } catch (InterruptedException e) {
      LOGGER.error("Semaphore acquisition interrupted for URL: {}", dmUrl, e);
      semaphore.release();
      if (pendingRequests.decrementAndGet() == 0) {
        promise.complete(idToSubClassMap);
      }
    }
  }

  /**
   * Handles the response from fetching data model information.
   *
   * @param dmAr The async result of the HTTP response.
   * @param classId The class id of the data model.
   * @param idToClassIdMap The map of id to classId.
   * @param idToSubClassMap The JsonObject mapping id to subclass.
   * @param pendingRequests The AtomicInteger tracking pending requests.
   * @param promise The Promise to complete with idToSubClassMap.
   * @param dmUrl The URL of the data model.
   */
  private void handleDataModelResponse(
      AsyncResult<HttpResponse<Buffer>> dmAr,
      String classId,
      Map<String, String> idToClassIdMap,
      JsonObject idToSubClassMap,
      AtomicInteger pendingRequests,
      Promise<JsonObject> promise,
      String dmUrl) {
    if (dmAr.succeeded()) {
      HttpResponse<Buffer> dmResponse = dmAr.result();
      Buffer dmBody = dmResponse.body();

      if (dmBody == null) {
        LOGGER.error("No response body received for URL: {}", dmUrl);
      } else if (!dmResponse.headers().get("content-type").contains("application/json")) {
        LOGGER.error("Invalid content-type received for URL: {}", dmUrl);
      } else {
        JsonObject dmJson;
        try {
          dmJson = dmBody.toJsonObject();
        } catch (Exception e) {
          LOGGER.error("Failed to parse JSON response from URL: {}", dmUrl, e);
          dmJson = null;
        }

        if (dmJson != null) {
          JsonArray graph = dmJson.getJsonArray("@graph");

          if (graph != null) {
            for (Object obj : graph) {
              if (obj instanceof JsonObject) {
                JsonObject graphItem = (JsonObject) obj;
                if (("iudx:" + classId).equals(graphItem.getString("@id"))) {
                  JsonObject subClassOfObj = graphItem.getJsonObject("rdfs:subClassOf");
                  if (subClassOfObj != null) {
                    String subClassIdStr = subClassOfObj.getString("@id");
                    if (subClassIdStr != null && subClassIdStr.contains(":")) {
                      String subClassId = subClassIdStr.split(":")[1];
                      for (Map.Entry<String, String> entry : idToClassIdMap.entrySet()) {
                        if (entry.getValue().equals(classId)) {
                          idToSubClassMap.put(entry.getKey(), subClassId);
                        }
                      }
                    } else {
                      LOGGER.error("Invalid @id in rdfs:subClassOf for class ID: {}", classId);
                    }
                  } else {
                    LOGGER.error("Missing rdfs:subClassOf for class ID: {}", classId);
                  }
                  break;
                }
              }
            }
          } else {
            LOGGER.error("Invalid graph array in response for URL: {}", dmUrl);
          }
        }
      }
    } else {
      LOGGER.error("Failed to fetch data model for URL: {}", dmUrl, dmAr.cause());
    }

    if (pendingRequests.decrementAndGet() == 0) {
      promise.complete(idToSubClassMap);
    }
  }
}

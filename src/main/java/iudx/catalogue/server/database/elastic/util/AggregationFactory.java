package iudx.catalogue.server.database.elastic.util;

import static iudx.catalogue.server.database.elastic.util.Constants.SIZE_KEY;
import static iudx.catalogue.server.util.Constants.FIELD;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import iudx.catalogue.server.database.elastic.model.QueryModel;
import java.util.Map;

public class AggregationFactory {

  // Factory method to create aggregations based on type
  public static Aggregation createAggregation(QueryModel queryModel) {
    return buildAggregation(queryModel).build();
  }

  // Recursive method to handle sub-aggregations
  private static Aggregation.Builder buildAggregation(QueryModel queryModel) {
    Aggregation.Builder builder = new Aggregation.Builder();
    AggregationType aggregationType = queryModel.getAggregationType();
    Map<String, Object> aggregationParameters = queryModel.getAggregationParameters();
    switch (aggregationType) {
      case TERMS:
        builder.terms(
            t -> {
              t.field((String) aggregationParameters.get(FIELD)); // Set the field

              // Check if SIZE_KEY is present and not null, then set the size
              if (aggregationParameters.containsKey(SIZE_KEY)
                  && aggregationParameters.get(SIZE_KEY) != null) {
                t.size((Integer) aggregationParameters.get(SIZE_KEY)); // Set the size
              }
              return t;
            });
        break;
      case HISTOGRAM:
        builder.histogram(
            h ->
                h.field((String) aggregationParameters.get(FIELD))
                    .interval((Double) aggregationParameters.get("interval")));
        break;
      case AVG:
        builder.avg(a -> a.field((String) aggregationParameters.get(FIELD)));
        break;
      case MAX:
        builder.max(m -> m.field((String) aggregationParameters.get(FIELD)));
        break;
      case MIN:
        builder.min(m -> m.field((String) aggregationParameters.get(FIELD)));
        break;
      case SUM:
        builder.sum(s -> s.field((String) aggregationParameters.get(FIELD)));
        break;
      case CARDINALITY:
        builder.cardinality(c -> c.field((String) aggregationParameters.get(FIELD)));
        break;
      case VALUE_COUNT:
        builder.valueCount(vc -> vc.field((String) aggregationParameters.get(FIELD)));
        break;
      case FILTER:
        builder.filter(
            f ->
                f.term(
                    t ->
                        t.field((String) aggregationParameters.get(FIELD))
                            .value(FieldValue.of(aggregationParameters.get("value")))));
        break;
      case GLOBAL:
        builder.global(g -> g);
        break;

      default:
        throw new UnsupportedOperationException(
            "Aggregation type not supported: " + aggregationType);
    }

    // Add sub-aggregations
    if (queryModel.getAggregationsMap() != null) {
      queryModel
          .getAggregationsMap()
          .forEach(
              (name, subQueryModel) ->
                  builder.aggregations(name, buildAggregation(subQueryModel).build()));
    }

    return builder;
  }
}

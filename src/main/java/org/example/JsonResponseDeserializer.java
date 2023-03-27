package org.example;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.example.JsonResponse.Aggregate;
import org.example.JsonResponse.CacheItem;
import org.example.JsonResponse.Datum;
import org.example.JsonResponse.Field;
import org.example.JsonResponse.Grouping;
import org.example.JsonResponse.Report;
import org.example.JsonResponse.Rule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

class JsonResponseDeserializer extends StdDeserializer<JsonResponse> {

    public JsonResponseDeserializer() {
        this(null);
    }

    public JsonResponseDeserializer(Class<?> vc) {
        super(vc);
    }
    
    @Override
    public JsonResponse deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {

        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        String expiresAt = node.get("Rule").get("ExpiresAt").asText();
        int expirationPolicy = node.get("Rule").get("ExpirationPolicy").asInt();
        Rule rule = new Rule(expiresAt, expirationPolicy);

        JsonNode cacheItemAsNode = node.get("CacheItem");
        ArrayList <CacheItem> cacheItem = new ArrayList<>();
        for (JsonNode c: cacheItemAsNode) {
            int reportId = c.get("report").get("reportId").asInt();

            JsonNode fieldsAsNode = c.get("report").get("fields");
            ArrayList<JsonResponse.Field> fields = new ArrayList<>();
            for (JsonNode objNode: fieldsAsNode) {
                fields.add(new Field(objNode.get("field").asText(), objNode.get("type").asText()));
            }
    
            JsonNode groupingAsNode = c.get("report").get("grouping");
            ArrayList<JsonResponse.Grouping> grouping = new ArrayList<>();
            for (JsonNode objNode: groupingAsNode) {
                grouping.add(new Grouping(objNode.get("field").asText(), objNode.get("type").asText()));
            }
    
            JsonNode aggregateAsNode = c.get("report").get("aggregate");
            ArrayList<JsonResponse.Aggregate> aggregate = new ArrayList<>();
            for (JsonNode objNode: aggregateAsNode) {
                aggregate.add(new Aggregate(objNode.get("field").asText(), objNode.get("aggregate").asText()));
            }
    
            JsonNode dataAsNode = c.get("report").get("data");
            ArrayList<JsonResponse.Datum> data = new ArrayList<>();
            for (JsonNode objNode: dataAsNode) {
                HashMap<String, String> fieldsAndValues = new HashMap<>();
                Iterator<String> it = objNode.fieldNames();
                while (it.hasNext()) {
                    String f = it.next();
                    String value = objNode.get(f).asText();
                    fieldsAndValues.put(f, value);
                }
                Datum datum = new Datum(fieldsAndValues);
                data.add(datum);
            }
    
            Report report = new Report(reportId, fields, grouping, aggregate, data);
            cacheItem.add(new CacheItem(report));
        }

        return new JsonResponse(rule, cacheItem);
    }
}
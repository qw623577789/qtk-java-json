package team.ytk.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.FormatFeature;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import lombok.SneakyThrows;
import team.ytk.json.point.Point;
import team.ytk.json.point.Point.DefaultType;

public class JSON {

    private static ObjectMapper defaultJackson = new ObjectMapper()
    .setNodeFactory(JsonNodeFactory.withExactBigDecimals(true)); //修复bigDecimal 1.0 转化后丢失.0问题

    public ObjectMapper jackson;

    private JsonNode json;

    public static JSONConfig config() {
        JsonMapper.Builder customJacksonBuilder = JsonMapper
            .builder()
            .nodeFactory(JsonNodeFactory.withExactBigDecimals(true)); //修复bigDecimal 1.0 转化后丢失.0问题

        return new JSONConfig(customJacksonBuilder);
    }

    public JSON(JsonNode jacksonNode) {
        this(jacksonNode, defaultJackson);
    }

    public JSON(boolean isObject) {
        this(isObject, defaultJackson);
    }

    public static JSON missingNode() {
        return new JSON(defaultJackson.missingNode());
    }

    public static JSON nullNode() {
        return new JSON(defaultJackson.nullNode());
    }

    public static JSON sPut(String id, Object value) {
        return sPut(defaultJackson, id, value);
    }

    public static JSON sAdd(Object... value) {
        return sAdd(defaultJackson, value);
    }

    @SneakyThrows
    public static JSON parse(Object object) {
        return parse(defaultJackson, object);
    }

    private JSON(boolean isObject, ObjectMapper jacksonMapper) {
        this.jackson = jacksonMapper;
        this.json = isObject ? this.jackson.createObjectNode() : this.jackson.createArrayNode();
    }

    private JSON(JsonNode jacksonNode, ObjectMapper jacksonMapper) {
        this.json = jacksonNode;
        this.jackson = jacksonMapper;
    }

    private static JSON sPut(ObjectMapper jacksonMapper, String id, Object value) {
        JSON json = new JSON(true, jacksonMapper);
        json.put(id, value);
        return json;
    }

    private static JSON sAdd(ObjectMapper jacksonMapper, Object... value) {
        JSON json = new JSON(false, jacksonMapper);
        json.add(value);
        return json;
    }

    private static JSON missingNode(ObjectMapper jacksonMapper) {
        return new JSON(jacksonMapper.missingNode(), jacksonMapper);
    }

    private static JSON nullNode(ObjectMapper jacksonMapper) {
        return new JSON(jacksonMapper.nullNode(), jacksonMapper);
    }

    @SneakyThrows
    private static JSON parse(ObjectMapper jacksonMapper, Object object) {
        if (object instanceof String) {
            String string = (String) object;
            return (
                    (string.startsWith("{") && string.endsWith("}")) ||
                    (string.startsWith("[") && string.endsWith("]"))
                )
                ? new JSON(jacksonMapper.readTree(string), jacksonMapper)
                : new JSON(jacksonMapper.valueToTree(object).deepCopy(), jacksonMapper);
        } else if (object instanceof File) {
            return new JSON(jacksonMapper.readTree((File) object), jacksonMapper);
        } else {
            return new JSON(jacksonMapper.valueToTree(object).deepCopy(), jacksonMapper);
        }
    }

    public Point point(String point) {
        return new Point(point, "", new HashMap<String, DefaultType>(), this.json, this);
    }

    public Point point(String point, Supplier<Object> defaultValue) {
        return new Point(point, "", new HashMap<String, DefaultType>(), this.json, this)
        .defaultValue(defaultValue);
    }

    public Point point(String point, Supplier<Object> defaultValue, boolean toUpdateNode) {
        return new Point(point, "", new HashMap<String, DefaultType>(), this.json, this)
        .defaultValue(defaultValue, toUpdateNode);
    }

    public Point point(String point, Object defaultValue) {
        return new Point(point, "", new HashMap<String, DefaultType>(), this.json, this)
        .defaultValue(defaultValue);
    }

    public Point point(String point, Object defaultValue, boolean toUpdateNode) {
        return new Point(point, "", new HashMap<String, DefaultType>(), this.json, this)
        .defaultValue(defaultValue, toUpdateNode);
    }

    public Point point() {
        return point(".");
    }

    public JSON put(String id, Object value) {
        ObjectNode objectNode = (ObjectNode) (this.json);
        if (value == null) {
            objectNode.set(id, null);
        } else if (value instanceof JSON) {
            objectNode.set(id, ((JSON) value).getJacksonNode());
        } else {
            objectNode.set(id, jackson.valueToTree(value));
        }

        return this;
    }

    public JSON add(Object... value) {
        ArrayNode arrayNode = (ArrayNode) this.json;

        if (value == null) {
            arrayNode.add(jackson.valueToTree(null));
        } else {
            Arrays
                .asList(value)
                .forEach(
                    item -> {
                        if (item == null) {
                            arrayNode.add(jackson.valueToTree(null));
                        } else if (item instanceof JSON) {
                            arrayNode.add(((JSON) item).getJacksonNode());
                        } else {
                            arrayNode.add(jackson.valueToTree(item));
                        }
                    }
                );
        }

        return this;
    }

    public JSON concat(List<?> list) {
        add(list.toArray());
        return this;
    }

    public JSON deepCopy() {
        return new JSON(this.json.deepCopy());
    }

    public JsonNode getJacksonNode() {
        return this.json;
    }

    public String toString(boolean pretty) {
        return this.toString(pretty, 4);
    }

    public String toString() {
        return this.toString(false);
    }

    @SneakyThrows
    public String toString(boolean pretty, int spaceAmount) {
        if (pretty) {
            PrettyPrinter printer = new JsonStringifyPrettyPrinter(spaceAmount);
            return jackson.writer(printer).writeValueAsString(this.json);
        } else {
            return this.json.toString();
        }
    }

    public static class JSONConfig {

        private JsonMapper.Builder customJacksonBuilder;

        public JSONConfig(JsonMapper.Builder customJacksonBuilder) {
            this.customJacksonBuilder = customJacksonBuilder;
        }

        public JSONConfig features(HashMap<FormatFeature, Boolean> features) {
            features
                .entrySet()
                .forEach(
                    item -> {
                        Object feature = item.getKey();
                        if (feature instanceof JsonReadFeature) {
                            customJacksonBuilder.configure((JsonReadFeature) feature, item.getValue());
                        } else if (feature instanceof JsonWriteFeature) {
                            customJacksonBuilder.configure((JsonWriteFeature) feature, item.getValue());
                        } else {
                            throw new RuntimeException("no support feature:" + feature.getClass().getName());
                        }
                    }
                );
            return this;
        }

        public JSONConfig serializationInclusion(JsonInclude.Include setSerializationInclusion) {
            customJacksonBuilder.serializationInclusion(setSerializationInclusion);
            return this;
        }

        public JSON JSON(JsonNode jacksonNode) {
            return new JSON(jacksonNode, customJacksonBuilder.build());
        }

        public JSON JSON(boolean isObject) {
            return new JSON(isObject, customJacksonBuilder.build());
        }

        public JSON missingNode() {
            return JSON.missingNode(customJacksonBuilder.build());
        }

        public JSON nullNode() {
            return JSON.nullNode(customJacksonBuilder.build());
        }

        public JSON sPut(String id, Object value) {
            return JSON.sPut(customJacksonBuilder.build(), id, value);
        }

        public JSON sAdd(Object... value) {
            return JSON.sAdd(customJacksonBuilder.build(), value);
        }

        public JSON parse(Object object) {
            return JSON.parse(customJacksonBuilder.build(), object);
        }
    }
}

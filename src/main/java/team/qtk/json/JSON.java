package team.qtk.json;

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
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import team.qtk.json.point.Point;
import team.qtk.json.point.Point.DefaultValueMap;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

public class JSON {

    private static ObjectMapper defaultJackson = new ObjectMapper()
        .setNodeFactory(JsonNodeFactory.withExactBigDecimals(true)) //修复bigDecimal 1.0 转化后丢失.0问题
        .registerModule(new JavaTimeModule().addDeserializer(LocalDateTime.class, new JsonDateTimeParser()));

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
        return new Point(point, "", new HashMap<>(), this.json, this);
    }

    public Point point(String point, Supplier<Object> defaultValue) {
        return new Point(point, "", new HashMap<>(), this.json, this)
            .defaultValue(defaultValue);
    }

    public Point point(String point, Object defaultValue) {
        if (defaultValue instanceof DefaultValueMap) {
            return new Point(point, "", new HashMap<>(), this.json, this)
                .defaultValue((DefaultValueMap) defaultValue);
        } else {
            return new Point(point, "", new HashMap<>(), this.json, this)
                .defaultValue(defaultValue);
        }
    }

    public Point point(String point, Supplier<Object> defaultValue, boolean toUpdateNode) {
        return new Point(point, "", new HashMap<>(), this.json, this)
            .defaultValue(defaultValue, toUpdateNode);
    }

    public Point point(String point, Object defaultValue, boolean toUpdateNode) {
        if (defaultValue instanceof DefaultValueMap) {
            return new Point(point, "", new HashMap<>(), this.json, this)
                .defaultValue((DefaultValueMap) defaultValue, toUpdateNode);
        } else {
            return new Point(point, "", new HashMap<>(), this.json, this)
                .defaultValue(defaultValue, toUpdateNode);
        }
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
            PrettyPrinter printer = new team.qtk.json.JsonStringifyPrettyPrinter(spaceAmount);
            return jackson.writer(printer).writeValueAsString(this.json);
        } else {
            return this.json.toString();
        }
    }

    public <T> T getAs(String point, Class<T> type) {
        return this.point(point).get().as(type);
    }

    public <T> T getAs(String point, Class<T> type, Object defaultValue) {
        return this.point(point, defaultValue).get().as(type);
    }

    public <T> T getAs(String point, Class<T> type, DefaultValueMap defaultValueMap) {
        return this.point(point, defaultValueMap).get().as(type);
    }

    public <T> T getAs(String point, Class<T> type, Supplier<Object> defaultValueSupplier) {
        return this.point(point, defaultValueSupplier).get().as(type);
    }

    public <T> T getNullableAs(String point, Class<T> type) {
        return this.point(point).get(true).as(type);
    }

    public <T> T getNullableAs(String point, Class<T> type, Object defaultValue) {
        return this.point(point, defaultValue).get(true).as(type);
    }

    public <T> T getNullableAs(String point, Class<T> type, Supplier<Void> defaultValueSupplier) {
        return this.point(point, defaultValueSupplier).get(true).as(type);
    }

    public <T> T getNullableAs(String point, Class<T> type, DefaultValueMap defaultValueMap) {
        return this.point(point, defaultValueMap).get(true).as(type);
    }

    public Void getNull(String point) {
        return getAs(point, Void.class);
    }

    public Void getNull(String point, boolean returnNull) {
        return getAs(point, Void.class, (Void) null);
    }

    public Void getNull(String point, Supplier<Void> defaultValueSupplier) {
        return getAs(point, Void.class, defaultValueSupplier);
    }

    public Void getNull(String point, DefaultValueMap defaultValueMap) {
        return getAs(point, Void.class, defaultValueMap);
    }

    public Void getNullableNull(String point) {
        return getNullableAs(point, Void.class);
    }

    public Void getNullableNull(String point, Object defaultValue) {
        return getNullableAs(point, Void.class, defaultValue);
    }

    public Void getNullableNull(String point, Supplier<Object> defaultValueSupplier) {
        return getNullableAs(point, Void.class, defaultValueSupplier);
    }

    public Void getNullableNull(String point, DefaultValueMap defaultValueMap) {
        return getNullableAs(point, Void.class, defaultValueMap);
    }

    public String getString(String point) {
        return getAs(point, String.class);
    }

    public String getString(String point, String defaultValue) {
        return getAs(point, String.class, defaultValue);
    }

    public String getString(String point, Supplier<String> defaultValueSupplier) {
        return getAs(point, String.class, defaultValueSupplier);
    }

    public String getString(String point, DefaultValueMap defaultValueMap) {
        return getAs(point, String.class, defaultValueMap);
    }

    public String getNullableString(String point) {
        return getNullableAs(point, String.class);
    }

    public String getNullableString(String point, String defaultValue) {
        return getNullableAs(point, String.class, defaultValue);
    }

    public String getNullableString(String point, Supplier<String> defaultValueSupplier) {
        return getNullableAs(point, String.class, defaultValueSupplier);
    }

    public String getNullableString(String point, DefaultValueMap defaultValueMap) {
        return getNullableAs(point, String.class, defaultValueMap);
    }

    public Boolean getBoolean(String point) {
        return getAs(point, Boolean.class);
    }

    public Boolean getBoolean(String point, Boolean defaultValue) {
        return getAs(point, Boolean.class, defaultValue);
    }

    public Boolean getBoolean(String point, Supplier<Boolean> defaultValueSupplier) {
        return getAs(point, Boolean.class, defaultValueSupplier);
    }

    public Boolean getBoolean(String point, DefaultValueMap defaultValueMap) {
        return getAs(point, Boolean.class, defaultValueMap);
    }

    public Boolean getNullableBoolean(String point) {
        return getNullableAs(point, Boolean.class);
    }

    public Boolean getNullableBoolean(String point, Boolean defaultValue) {
        return getNullableAs(point, Boolean.class, defaultValue);
    }

    public Boolean getNullableBoolean(String point, Supplier<Boolean> defaultValueSupplier) {
        return getNullableAs(point, Boolean.class, defaultValueSupplier);
    }

    public Boolean getNullableBoolean(String point, DefaultValueMap defaultValueMap) {
        return getNullableAs(point, Boolean.class, defaultValueMap);
    }

    public LocalDateTime getLocalDateTime(String point) {
        return getAs(point, LocalDateTime.class);
    }

    public LocalDateTime getLocalDateTime(String point, Integer defaultValue) {
        return getAs(point, LocalDateTime.class, defaultValue);
    }

    public LocalDateTime getLocalDateTime(String point, Supplier<LocalDateTime> defaultValueSupplier) {
        return getAs(point, LocalDateTime.class, defaultValueSupplier);
    }

    public LocalDateTime getLocalDateTime(String point, DefaultValueMap defaultValueMap) {
        return getAs(point, LocalDateTime.class, defaultValueMap);
    }

    public LocalDateTime getNullableLocalDateTime(String point) {
        return getNullableAs(point, LocalDateTime.class);
    }

    public LocalDateTime getNullableLocalDateTime(String point, LocalDateTime defaultValue) {
        return getNullableAs(point, LocalDateTime.class, defaultValue);
    }

    public LocalDateTime getNullableLocalDateTime(String point, Supplier<LocalDateTime> defaultValueSupplier) {
        return getNullableAs(point, LocalDateTime.class, defaultValueSupplier);
    }

    public LocalDateTime getNullableLocalDateTime(String point, DefaultValueMap defaultValueMap) {
        return getNullableAs(point, LocalDateTime.class, defaultValueMap);
    }

    public Integer getInt(String point) {
        return getAs(point, Integer.class);
    }

    public Integer getInt(String point, Integer defaultValue) {
        return getAs(point, Integer.class, defaultValue);
    }

    public Integer getInt(String point, Supplier<Integer> defaultValueSupplier) {
        return getAs(point, Integer.class, defaultValueSupplier);
    }

    public Integer getInt(String point, DefaultValueMap defaultValueMap) {
        return getAs(point, Integer.class, defaultValueMap);
    }

    public Integer getNullableInt(String point) {
        return getNullableAs(point, Integer.class);
    }

    public Integer getNullableInt(String point, Integer defaultValue) {
        return getNullableAs(point, Integer.class, defaultValue);
    }

    public Integer getNullableInt(String point, Supplier<Integer> defaultValueSupplier) {
        return getNullableAs(point, Integer.class, defaultValueSupplier);
    }

    public Integer getNullableInt(String point, DefaultValueMap defaultValueMap) {
        return getNullableAs(point, Integer.class, defaultValueMap);
    }

    public Double getDouble(String point) {
        return getAs(point, Double.class);
    }

    public Double getDouble(String point, Double defaultValue) {
        return getAs(point, Double.class, defaultValue);
    }

    public Double getDouble(String point, Supplier<Double> defaultValueSupplier) {
        return getAs(point, Double.class, defaultValueSupplier);
    }

    public Double getDouble(String point, DefaultValueMap defaultValueMap) {
        return getAs(point, Double.class, defaultValueMap);
    }

    public Double getNullableDouble(String point) {
        return getNullableAs(point, Double.class);
    }

    public Double getNullableDouble(String point, Double defaultValue) {
        return getNullableAs(point, Double.class, defaultValue);
    }

    public Double getNullableDouble(String point, Supplier<Double> defaultValueSupplier) {
        return getNullableAs(point, Double.class, defaultValueSupplier);
    }

    public Double getNullableDouble(String point, DefaultValueMap defaultValueMap) {
        return getNullableAs(point, Double.class, defaultValueMap);
    }

    public Long getLong(String point) {
        return getAs(point, Long.class);
    }

    public Long getLong(String point, Long defaultValue) {
        return getAs(point, Long.class, defaultValue);
    }

    public Long getLong(String point, Supplier<Long> defaultValueSupplier) {
        return getAs(point, Long.class, defaultValueSupplier);
    }

    public Long getLong(String point, DefaultValueMap defaultValueMap) {
        return getAs(point, Long.class, defaultValueMap);
    }

    public Long getNullableLong(String point) {
        return getNullableAs(point, Long.class);
    }

    public Long getNullableLong(String point, Long defaultValue) {
        return getNullableAs(point, Long.class, defaultValue);
    }

    public Long getNullableLong(String point, Supplier<Long> defaultValueSupplier) {
        return getNullableAs(point, Long.class, defaultValueSupplier);
    }

    public Long getNullableLong(String point, DefaultValueMap defaultValueMap) {
        return getNullableAs(point, Long.class, defaultValueMap);
    }

    public Float getFloat(String point) {
        return getAs(point, Float.class);
    }

    public Float getFloat(String point, Float defaultValue) {
        return getAs(point, Float.class, defaultValue);
    }

    public Float getFloat(String point, Supplier<Float> defaultValueSupplier) {
        return getAs(point, Float.class, defaultValueSupplier);
    }

    public Float getFloat(String point, DefaultValueMap defaultValueMap) {
        return getAs(point, Float.class, defaultValueMap);
    }

    public Float getNullableFloat(String point) {
        return getNullableAs(point, Float.class);
    }

    public Float getNullableFloat(String point, Float defaultValue) {
        return getNullableAs(point, Float.class, defaultValue);
    }

    public Float getNullableFloat(String point, Supplier<Float> defaultValueSupplier) {
        return getNullableAs(point, Float.class, defaultValueSupplier);
    }

    public Float getNullableFloat(String point, DefaultValueMap defaultValueMap) {
        return getNullableAs(point, Float.class, defaultValueMap);
    }

    public BigDecimal getBigDecimal(String point) {
        return getAs(point, BigDecimal.class);
    }

    public BigDecimal getBigDecimal(String point, BigDecimal defaultValue) {
        return getAs(point, BigDecimal.class, defaultValue);
    }

    public BigDecimal getBigDecimal(String point, Supplier<BigDecimal> defaultValueSupplier) {
        return getAs(point, BigDecimal.class, defaultValueSupplier);
    }

    public BigDecimal getBigDecimal(String point, DefaultValueMap defaultValueMap) {
        return getAs(point, BigDecimal.class, defaultValueMap);
    }

    public BigDecimal getNullableBigDecimal(String point) {
        return getNullableAs(point, BigDecimal.class);
    }

    public BigDecimal getNullableBigDecimal(String point, BigDecimal defaultValue) {
        return getNullableAs(point, BigDecimal.class, defaultValue);
    }

    public BigDecimal getNullableBigDecimal(String point, Supplier<BigDecimal> defaultValueSupplier) {
        return getNullableAs(point, BigDecimal.class, defaultValueSupplier);
    }

    public BigDecimal getNullableBigDecimal(String point, DefaultValueMap defaultValueMap) {
        return getNullableAs(point, BigDecimal.class, defaultValueMap);
    }

    public JSON getJSON(String point) {
        return getAs(point, JSON.class);
    }

    public JSON getJSON(String point, JSON defaultValue) {
        return getAs(point, JSON.class, defaultValue);
    }

    public JSON getJSON(String point, Supplier<JSON> defaultValueSupplier) {
        return getAs(point, JSON.class, defaultValueSupplier);
    }

    public JSON getJSON(String point, DefaultValueMap defaultValueMap) {
        return getAs(point, JSON.class, defaultValueMap);
    }

    public JSON getNullableJSON(String point) {
        return getNullableAs(point, JSON.class);
    }

    public JSON getNullableJSON(String point, JSON defaultValue) {
        return getNullableAs(point, JSON.class, defaultValue);
    }

    public JSON getNullableJSON(String point, Supplier<JSON> defaultValueSupplier) {
        return getNullableAs(point, JSON.class, defaultValueSupplier);
    }

    public JSON getNullableJSON(String point, DefaultValueMap defaultValueMap) {
        return getNullableAs(point, JSON.class, defaultValueMap);
    }

    public <T> List<T> getList(String point, Class<T> itemType) {
        return this.point(point).get().asList(itemType);
    }

    public <T> List<T> getList(String point, Class<T> itemType, Object defaultValue) {
        return this.point(point, defaultValue).get().asList(itemType);
    }

    public <T> List<T> getList(String point, Class<T> itemType, Supplier<Object> defaultValueSupplier) {
        return this.point(point, defaultValueSupplier).get().asList(itemType);
    }

    public <T> List<T> getList(String point, Class<T> itemType, DefaultValueMap defaultValueMap) {
        return this.point(point, defaultValueMap).get().asList(itemType);
    }

    public <T> List<T> getNullableList(String point, Class<T> itemType) {
        return this.point(point).get(true).asList(itemType);
    }

    public <T> List<T> getNullableList(String point, Class<T> itemType, Object defaultValue) {
        return this.point(point, defaultValue).get(true).asList(itemType);
    }

    public <T> List<T> getNullableList(
        String point,
        Class<T> itemType,
        Supplier<Object> defaultValueSupplier
    ) {
        return this.point(point, defaultValueSupplier).get(true).asList(itemType);
    }

    public <T> List<T> getNullableList(String point, Class<T> itemType, DefaultValueMap defaultValueMap) {
        return this.point(point, defaultValueMap).get(true).asList(itemType);
    }

    public List<Object> getList(String point) {
        return getList(point, Object.class);
    }

    public List<Object> getList(String point, List<Object> defaultValue) {
        return getList(point, Object.class, defaultValue);
    }

    public List<Object> getList(String point, Supplier<Object> defaultValueSupplier) {
        return getList(point, Object.class, defaultValueSupplier);
    }

    public List<Object> getList(String point, DefaultValueMap defaultValueMap) {
        return getList(point, Object.class, defaultValueMap);
    }

    public List<Object> getNullableList(String point) {
        return getNullableList(point, Object.class);
    }

    public List<Object> getNullableList(String point, List<Object> defaultValue) {
        return getNullableList(point, Object.class, defaultValue);
    }

    public List<Object> getNullableList(String point, Supplier<Object> defaultValueSupplier) {
        return getNullableList(point, Object.class, defaultValueSupplier);
    }

    public List<Object> getNullableList(String point, DefaultValueMap defaultValueMap) {
        return getNullableList(point, Object.class, defaultValueMap);
    }

    public HashMap<String, Object> getMap(String point) {
        return getMap(point, Object.class);
    }

    public HashMap<String, Object> getMap(String point, Object defaultValue) {
        return this.point(point, defaultValue).get().asMap(Object.class);
    }

    public HashMap<String, Object> getMap(String point, Supplier<Object> defaultValueSupplier) {
        return this.point(point, defaultValueSupplier).get().asMap(Object.class);
    }

    public HashMap<String, Object> getMap(String point, DefaultValueMap defaultValueMap) {
        return this.point(point, defaultValueMap).get().asMap(Object.class);
    }

    public HashMap<String, Object> getNullableMap(String point) {
        return this.point(point).get(true).asMap(Object.class);
    }

    public HashMap<String, Object> getNullableMap(String point, Object defaultValue) {
        return this.point(point, defaultValue).get(true).asMap(Object.class);
    }

    public HashMap<String, Object> getNullableMap(String point, Supplier<Object> defaultValueSupplier) {
        return this.point(point, defaultValueSupplier).get(true).asMap(Object.class);
    }

    public HashMap<String, Object> getNullableMap(String point, DefaultValueMap defaultValueMap) {
        return this.point(point, defaultValueMap).get(true).asMap(Object.class);
    }

    public <T> HashMap<String, T> getMap(String point, Class<T> valueType) {
        return this.point(point).get().asMap(valueType);
    }

    public <T> HashMap<String, T> getMap(String point, Class<T> valueType, Object defaultValue) {
        return this.point(point, defaultValue).get().asMap(valueType);
    }

    public <T> HashMap<String, T> getMap(
        String point,
        Class<T> valueType,
        Supplier<Object> defaultValueSupplier
    ) {
        return this.point(point, defaultValueSupplier).get().asMap(valueType);
    }

    public <T> HashMap<String, T> getMap(String point, Class<T> valueType, DefaultValueMap defaultValueMap) {
        return this.point(point, defaultValueMap).get().asMap(valueType);
    }

    public <T> HashMap<String, T> getNullableMap(String point, Class<T> valueType) {
        return this.point(point).get(true).asMap(valueType);
    }

    public <T> HashMap<String, T> getNullableMap(String point, Class<T> valueType, Object defaultValue) {
        return this.point(point, defaultValue).get(true).asMap(valueType);
    }

    public <T> HashMap<String, T> getNullableMap(
        String point,
        Class<T> valueType,
        Supplier<T> defaultValueSupplier
    ) {
        return this.point(point, defaultValueSupplier).get(true).asMap(valueType);
    }

    public <T> HashMap<String, T> getNullableMap(
        String point,
        Class<T> valueType,
        DefaultValueMap defaultValueMap
    ) {
        return this.point(point, defaultValueMap).get(true).asMap(valueType);
    }

    public static class JSONConfig {

        private JsonMapper.Builder customJacksonBuilder;

        public JSONConfig(JsonMapper.Builder customJacksonBuilder) {
            this.customJacksonBuilder = customJacksonBuilder;
        }

        public JSONConfig features(HashMap<FormatFeature, Boolean> features) {
            features
                .forEach((key, value) -> {
                    if (key instanceof JsonReadFeature) {
                        customJacksonBuilder.configure((JsonReadFeature) key, value);
                    } else if (key instanceof JsonWriteFeature) {
                        customJacksonBuilder.configure((JsonWriteFeature) key, value);
                    } else {
                        throw new RuntimeException("no support feature:" + ((Object) key).getClass().getName());
                    }
                });
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

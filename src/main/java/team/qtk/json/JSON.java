package team.qtk.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import team.qtk.json.point.Point;
import team.qtk.json.point.Point.DefaultValueMap;

import java.io.File;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class JSON {

    private static ObjectMapper defaultJackson = new ObjectMapper()
        .setNodeFactory(JsonNodeFactory.withExactBigDecimals(true)) //修复bigDecimal 1.0 转化后丢失.0问题
        .registerModule(new JavaTimeModule().addDeserializer(LocalDateTime.class, new JsonDateTimeParser()));

    public ObjectMapper jackson;

    private JsonNode json;

    /**
     * 自定义配置ObjectMapper
     */
    public static JSONConfig config() {
        JsonMapper.Builder customJacksonBuilder = JsonMapper
            .builder()
            .nodeFactory(JsonNodeFactory.withExactBigDecimals(true)) //修复bigDecimal 1.0 转化后丢失.0问题
            .addModule(new JavaTimeModule().addDeserializer(LocalDateTime.class, new JsonDateTimeParser()));

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

    public static JSON createObject() {
        return new JSON(true, defaultJackson);
    }

    public static JSON createArray() {
        return new JSON(false, defaultJackson);
    }

    @SneakyThrows
    public static JSON assign(Object target, Object... sources) {
        ObjectReader merger = defaultJackson.readerForUpdating(target instanceof JSON ? ((JSON) target).getJacksonNode() : target);

        for (Object object : sources) {
            merger.readValue(parse(defaultJackson, object).getJacksonNode());
        }

        return parse(defaultJackson, target);
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
        } else if (object instanceof JSON) {
            return new JSON(((JSON) object).getJacksonNode(), jacksonMapper);
        } else if (object instanceof Reader) {
            return new JSON(jacksonMapper.readTree((Reader) object), jacksonMapper);
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

    @SneakyThrows
    public JSON merge(Object... objects) {
        ObjectReader merger = jackson.readerForUpdating(this.json);

        for (Object object : objects) {
            merger.readValue(parse(jackson, object).getJacksonNode());
        }

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

    public <T> T getAs(Class<T> type) {
        return this.getAs(".", type);
    }

    public <T> T getAs(String point, Class<T> type, Object defaultValue) {
        return this.point(point, defaultValue).get().as(type);
    }

    public <T> T getAs(Class<T> type, Object defaultValue) {
        return this.getAs(".", type, defaultValue);
    }

    public <T> T getAs(String point, Class<T> type, DefaultValueMap defaultValueMap) {
        return this.point(point, defaultValueMap).get().as(type);
    }

    public <T> T getAs(Class<T> type, DefaultValueMap defaultValueMap) {
        return this.getAs(".", type, defaultValueMap);
    }

    public <T> T getAs(String point, Class<T> type, Supplier<Object> defaultValueSupplier) {
        return this.point(point, defaultValueSupplier).get().as(type);
    }

    public <T> T getAs(Class<T> type, Supplier<Object> defaultValueSupplier) {
        return this.getAs(".", type, defaultValueSupplier);
    }

    public <T> T getNullableAs(String point, Class<T> type) {
        return this.point(point).get(true).as(type);
    }

    public <T> T getNullableAs(Class<T> type) {
        return this.getNullableAs(".", type);
    }

    public <T> T getNullableAs(String point, Class<T> type, Object defaultValue) {
        return this.point(point, defaultValue).get(true).as(type);
    }

    public <T> T getNullableAs(Class<T> type, Object defaultValue) {
        return this.getNullableAs("point", type, defaultValue);
    }

    public <T> T getNullableAs(String point, Class<T> type, Supplier<Void> defaultValueSupplier) {
        return this.point(point, defaultValueSupplier).get(true).as(type);
    }

    public <T> T getNullableAs(Class<T> type, Supplier<Void> defaultValueSupplier) {
        return this.getNullableAs(".", type, defaultValueSupplier);
    }

    public <T> T getNullableAs(String point, Class<T> type, DefaultValueMap defaultValueMap) {
        return this.point(point, defaultValueMap).get(true).as(type);
    }

    public <T> T getNullableAs(Class<T> type, DefaultValueMap defaultValueMap) {
        return this.getNullableAs(".", type, defaultValueMap);
    }

    public Void getNull(String point) {
        return getAs(point, Void.class);
    }

    public Void getNull() {
        return getNull(".");
    }

    public Void getNull(String point, boolean returnNull) {
        return getAs(point, Void.class, (Void) null);
    }

    public Void getNull(boolean returnNull) {
        return getNull(".");
    }

    public Void getNull(String point, Supplier<Void> defaultValueSupplier) {
        return getAs(point, Void.class, defaultValueSupplier);
    }

    public Void getNull(Supplier<Void> defaultValueSupplier) {
        return getNull(".", defaultValueSupplier);
    }

    public Void getNull(String point, DefaultValueMap defaultValueMap) {
        return getAs(point, Void.class, defaultValueMap);
    }

    public Void getNull(DefaultValueMap defaultValueMap) {
        return getNull(".", defaultValueMap);
    }

    public Void getNullableNull(String point) {
        return getNullableAs(point, Void.class);
    }

    public Void getNullableNull() {
        return getNullableNull(".");
    }

    public Void getNullableNull(String point, Object defaultValue) {
        return getNullableAs(point, Void.class, defaultValue);
    }

    public Void getNullableNull(Object defaultValue) {
        return getNullableNull(".", defaultValue);
    }

    public Void getNullableNull(String point, Supplier<Object> defaultValueSupplier) {
        return getNullableAs(point, Void.class, defaultValueSupplier);
    }

    public Void getNullableNull(Supplier<Object> defaultValueSupplier) {
        return getNullableNull(".", defaultValueSupplier);
    }

    public Void getNullableNull(String point, DefaultValueMap defaultValueMap) {
        return getNullableAs(point, Void.class, defaultValueMap);
    }

    public Void getNullableNull(DefaultValueMap defaultValueMap) {
        return getNullableNull(".", defaultValueMap);
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

    public Boolean getBoolean() {
        return getBoolean(".");
    }

    public Boolean getBoolean(String point, Boolean defaultValue) {
        return getAs(point, Boolean.class, defaultValue);
    }

    public Boolean getBoolean(Boolean defaultValue) {
        return getBoolean(".", defaultValue);
    }

    public Boolean getBoolean(String point, Supplier<Boolean> defaultValueSupplier) {
        return getAs(point, Boolean.class, defaultValueSupplier);
    }

    public Boolean getBoolean(Supplier<Boolean> defaultValueSupplier) {
        return getBoolean(".", defaultValueSupplier);
    }

    public Boolean getBoolean(String point, DefaultValueMap defaultValueMap) {
        return getAs(point, Boolean.class, defaultValueMap);
    }

    public Boolean getBoolean(DefaultValueMap defaultValueMap) {
        return getBoolean(".", defaultValueMap);
    }

    public Boolean getNullableBoolean(String point) {
        return getNullableAs(point, Boolean.class);
    }

    public Boolean getNullableBoolean() {
        return getNullableBoolean(".");
    }

    public Boolean getNullableBoolean(String point, Boolean defaultValue) {
        return getNullableAs(point, Boolean.class, defaultValue);
    }

    public Boolean getNullableBoolean(Boolean defaultValue) {
        return getNullableBoolean(".", defaultValue);
    }

    public Boolean getNullableBoolean(String point, Supplier<Boolean> defaultValueSupplier) {
        return getNullableAs(point, Boolean.class, defaultValueSupplier);
    }

    public Boolean getNullableBoolean(Supplier<Boolean> defaultValueSupplier) {
        return getNullableBoolean(".", defaultValueSupplier);
    }

    public Boolean getNullableBoolean(String point, DefaultValueMap defaultValueMap) {
        return getNullableAs(point, Boolean.class, defaultValueMap);
    }

    public Boolean getNullableBoolean(DefaultValueMap defaultValueMap) {
        return getNullableBoolean(".", defaultValueMap);
    }

    public LocalDateTime getLocalDateTime(String point) {
        return getAs(point, LocalDateTime.class);
    }

    public LocalDateTime getLocalDateTime() {
        return getLocalDateTime(".");
    }

    public LocalDateTime getLocalDateTime(String point, Integer defaultValue) {
        return getAs(point, LocalDateTime.class, defaultValue);
    }

    public LocalDateTime getLocalDateTime(Integer defaultValue) {
        return getLocalDateTime(".", defaultValue);
    }

    public LocalDateTime getLocalDateTime(String point, Supplier<LocalDateTime> defaultValueSupplier) {
        return getAs(point, LocalDateTime.class, defaultValueSupplier);
    }

    public LocalDateTime getLocalDateTime(Supplier<LocalDateTime> defaultValueSupplier) {
        return getLocalDateTime(".", defaultValueSupplier);
    }

    public LocalDateTime getLocalDateTime(String point, DefaultValueMap defaultValueMap) {
        return getAs(point, LocalDateTime.class, defaultValueMap);
    }

    public LocalDateTime getLocalDateTime(DefaultValueMap defaultValueMap) {
        return getLocalDateTime(".", defaultValueMap);
    }

    public LocalDateTime getNullableLocalDateTime(String point) {
        return getNullableAs(point, LocalDateTime.class);
    }

    public LocalDateTime getNullableLocalDateTime() {
        return getNullableLocalDateTime(".");
    }

    public LocalDateTime getNullableLocalDateTime(String point, LocalDateTime defaultValue) {
        return getNullableAs(point, LocalDateTime.class, defaultValue);
    }

    public LocalDateTime getNullableLocalDateTime(LocalDateTime defaultValue) {
        return getNullableLocalDateTime(".", defaultValue);
    }

    public LocalDateTime getNullableLocalDateTime(String point, Supplier<LocalDateTime> defaultValueSupplier) {
        return getNullableAs(point, LocalDateTime.class, defaultValueSupplier);
    }

    public LocalDateTime getNullableLocalDateTime(Supplier<LocalDateTime> defaultValueSupplier) {
        return getNullableLocalDateTime(".", defaultValueSupplier);
    }

    public LocalDateTime getNullableLocalDateTime(String point, DefaultValueMap defaultValueMap) {
        return getNullableAs(point, LocalDateTime.class, defaultValueMap);
    }

    public LocalDateTime getNullableLocalDateTime(DefaultValueMap defaultValueMap) {
        return getNullableLocalDateTime(".", defaultValueMap);
    }

    public Integer getInt(String point) {
        return getAs(point, Integer.class);
    }

    public Integer getInt() {
        return getInt(".");
    }

    public Integer getInt(String point, Integer defaultValue) {
        return getAs(point, Integer.class, defaultValue);
    }

    public Integer getInt(Integer defaultValue) {
        return getInt(".", defaultValue);
    }

    public Integer getInt(String point, Supplier<Integer> defaultValueSupplier) {
        return getAs(point, Integer.class, defaultValueSupplier);
    }

    public Integer getInt(Supplier<Integer> defaultValueSupplier) {
        return getInt(".", defaultValueSupplier);
    }

    public Integer getInt(String point, DefaultValueMap defaultValueMap) {
        return getAs(point, Integer.class, defaultValueMap);
    }

    public Integer getInt(DefaultValueMap defaultValueMap) {
        return getInt(".", defaultValueMap);
    }

    public Integer getNullableInt(String point) {
        return getNullableAs(point, Integer.class);
    }

    public Integer getNullableInt() {
        return getNullableInt(".");
    }

    public Integer getNullableInt(String point, Integer defaultValue) {
        return getNullableAs(point, Integer.class, defaultValue);
    }

    public Integer getNullableInt(Integer defaultValue) {
        return getNullableInt(".", defaultValue);

    }

    public Integer getNullableInt(String point, Supplier<Integer> defaultValueSupplier) {
        return getNullableAs(point, Integer.class, defaultValueSupplier);
    }

    public Integer getNullableInt(Supplier<Integer> defaultValueSupplier) {
        return getNullableInt(".", defaultValueSupplier);
    }

    public Integer getNullableInt(String point, DefaultValueMap defaultValueMap) {
        return getNullableAs(point, Integer.class, defaultValueMap);
    }

    public Integer getNullableInt(DefaultValueMap defaultValueMap) {
        return getNullableInt(".", defaultValueMap);
    }

    public Double getDouble(String point) {
        return getAs(point, Double.class);
    }

    public Double getDouble() {
        return getDouble(".");
    }

    public Double getDouble(String point, Double defaultValue) {
        return getAs(point, Double.class, defaultValue);
    }

    public Double getDouble(Double defaultValue) {
        return getDouble(".", defaultValue);
    }

    public Double getDouble(String point, Supplier<Double> defaultValueSupplier) {
        return getAs(point, Double.class, defaultValueSupplier);
    }

    public Double getDouble(Supplier<Double> defaultValueSupplier) {
        return getDouble(".", defaultValueSupplier);
    }

    public Double getDouble(String point, DefaultValueMap defaultValueMap) {
        return getAs(point, Double.class, defaultValueMap);
    }

    public Double getDouble(DefaultValueMap defaultValueMap) {
        return getDouble(".", defaultValueMap);
    }

    public Double getNullableDouble(String point) {
        return getNullableAs(point, Double.class);
    }

    public Double getNullableDouble() {
        return getNullableDouble(".");
    }

    public Double getNullableDouble(String point, Double defaultValue) {
        return getNullableAs(point, Double.class, defaultValue);
    }

    public Double getNullableDouble(Double defaultValue) {
        return getNullableDouble(".", defaultValue);
    }

    public Double getNullableDouble(String point, Supplier<Double> defaultValueSupplier) {
        return getNullableAs(point, Double.class, defaultValueSupplier);
    }

    public Double getNullableDouble(Supplier<Double> defaultValueSupplier) {
        return getNullableDouble(".", defaultValueSupplier);
    }

    public Double getNullableDouble(String point, DefaultValueMap defaultValueMap) {
        return getNullableAs(point, Double.class, defaultValueMap);
    }

    public Double getNullableDouble(DefaultValueMap defaultValueMap) {
        return getNullableDouble(".", defaultValueMap);
    }

    public Long getLong(String point) {
        return getAs(point, Long.class);
    }

    public Long getLong() {
        return getLong(".");
    }

    public Long getLong(String point, Long defaultValue) {
        return getAs(point, Long.class, defaultValue);
    }

    public Long getLong(Long defaultValue) {
        return getLong(".", defaultValue);
    }

    public Long getLong(String point, Supplier<Long> defaultValueSupplier) {
        return getAs(point, Long.class, defaultValueSupplier);
    }

    public Long getLong(Supplier<Long> defaultValueSupplier) {
        return getLong(".", defaultValueSupplier);
    }

    public Long getLong(String point, DefaultValueMap defaultValueMap) {
        return getAs(point, Long.class, defaultValueMap);
    }

    public Long getLong(DefaultValueMap defaultValueMap) {
        return getLong(".", defaultValueMap);
    }

    public Long getNullableLong(String point) {
        return getNullableAs(point, Long.class);
    }

    public Long getNullableLong() {
        return getNullableLong(".");
    }

    public Long getNullableLong(String point, Long defaultValue) {
        return getNullableAs(point, Long.class, defaultValue);
    }

    public Long getNullableLong(Long defaultValue) {
        return getNullableLong(".", defaultValue);
    }

    public Long getNullableLong(String point, Supplier<Long> defaultValueSupplier) {
        return getNullableAs(point, Long.class, defaultValueSupplier);
    }

    public Long getNullableLong(Supplier<Long> defaultValueSupplier) {
        return getNullableLong(".", defaultValueSupplier);
    }

    public Long getNullableLong(String point, DefaultValueMap defaultValueMap) {
        return getNullableAs(point, Long.class, defaultValueMap);
    }

    public Long getNullableLong(DefaultValueMap defaultValueMap) {
        return getNullableLong(".", defaultValueMap);
    }

    public Float getFloat(String point) {
        return getAs(point, Float.class);
    }

    public Float getFloat() {
        return getFloat(".");
    }

    public Float getFloat(String point, Float defaultValue) {
        return getAs(point, Float.class, defaultValue);
    }

    public Float getFloat(Float defaultValue) {
        return getFloat(".", defaultValue);
    }

    public Float getFloat(String point, Supplier<Float> defaultValueSupplier) {
        return getAs(point, Float.class, defaultValueSupplier);
    }

    public Float getFloat(Supplier<Float> defaultValueSupplier) {
        return getFloat(".", defaultValueSupplier);
    }

    public Float getFloat(String point, DefaultValueMap defaultValueMap) {
        return getAs(point, Float.class, defaultValueMap);
    }

    public Float getFloat(DefaultValueMap defaultValueMap) {
        return getFloat(".", defaultValueMap);
    }

    public Float getNullableFloat(String point) {
        return getNullableAs(point, Float.class);
    }

    public Float getNullableFloat() {
        return getNullableFloat(".");
    }

    public Float getNullableFloat(String point, Float defaultValue) {
        return getNullableAs(point, Float.class, defaultValue);
    }

    public Float getNullableFloat(Float defaultValue) {
        return getNullableFloat(".", defaultValue);
    }

    public Float getNullableFloat(String point, Supplier<Float> defaultValueSupplier) {
        return getNullableAs(point, Float.class, defaultValueSupplier);
    }

    public Float getNullableFloat(Supplier<Float> defaultValueSupplier) {
        return getNullableFloat(".", defaultValueSupplier);
    }

    public Float getNullableFloat(String point, DefaultValueMap defaultValueMap) {
        return getNullableAs(point, Float.class, defaultValueMap);
    }

    public Float getNullableFloat(DefaultValueMap defaultValueMap) {
        return getNullableFloat(".", defaultValueMap);
    }

    public BigDecimal getBigDecimal(String point) {
        return getAs(point, BigDecimal.class);
    }

    public BigDecimal getBigDecimal() {
        return getBigDecimal(".");
    }

    public BigDecimal getBigDecimal(String point, BigDecimal defaultValue) {
        return getAs(point, BigDecimal.class, defaultValue);
    }

    public BigDecimal getBigDecimal(BigDecimal defaultValue) {
        return getBigDecimal(".", defaultValue);
    }

    public BigDecimal getBigDecimal(String point, Supplier<BigDecimal> defaultValueSupplier) {
        return getAs(point, BigDecimal.class, defaultValueSupplier);
    }

    public BigDecimal getBigDecimal(Supplier<BigDecimal> defaultValueSupplier) {
        return getBigDecimal(".", defaultValueSupplier);
    }

    public BigDecimal getBigDecimal(String point, DefaultValueMap defaultValueMap) {
        return getAs(point, BigDecimal.class, defaultValueMap);
    }

    public BigDecimal getBigDecimal(DefaultValueMap defaultValueMap) {
        return getBigDecimal(".", defaultValueMap);

    }

    public BigDecimal getNullableBigDecimal(String point) {
        return getNullableAs(point, BigDecimal.class);
    }

    public BigDecimal getNullableBigDecimal() {
        return getNullableBigDecimal(".");
    }

    public BigDecimal getNullableBigDecimal(String point, BigDecimal defaultValue) {
        return getNullableAs(point, BigDecimal.class, defaultValue);
    }

    public BigDecimal getNullableBigDecimal(BigDecimal defaultValue) {
        return getNullableBigDecimal(".", defaultValue);
    }

    public BigDecimal getNullableBigDecimal(String point, Supplier<BigDecimal> defaultValueSupplier) {
        return getNullableAs(point, BigDecimal.class, defaultValueSupplier);
    }

    public BigDecimal getNullableBigDecimal(Supplier<BigDecimal> defaultValueSupplier) {
        return getNullableBigDecimal(".", defaultValueSupplier);
    }

    public BigDecimal getNullableBigDecimal(String point, DefaultValueMap defaultValueMap) {
        return getNullableAs(point, BigDecimal.class, defaultValueMap);
    }

    public BigDecimal getNullableBigDecimal(DefaultValueMap defaultValueMap) {
        return getNullableBigDecimal(".", defaultValueMap);
    }

    public JSON getJSON(String point) {
        return getAs(point, JSON.class);
    }

    public JSON getJSON() {
        return getJSON(".");
    }

    public JSON getJSON(String point, JSON defaultValue) {
        return getAs(point, JSON.class, defaultValue);
    }

    public JSON getJSON(JSON defaultValue) {
        return getJSON(".", defaultValue);
    }

    public JSON getJSON(String point, Supplier<JSON> defaultValueSupplier) {
        return getAs(point, JSON.class, defaultValueSupplier);
    }

    public JSON getJSON(Supplier<JSON> defaultValueSupplier) {
        return getJSON(".", defaultValueSupplier);
    }

    public JSON getJSON(String point, DefaultValueMap defaultValueMap) {
        return getAs(point, JSON.class, defaultValueMap);
    }

    public JSON getJSON(DefaultValueMap defaultValueMap) {
        return getJSON(".", defaultValueMap);
    }

    public JSON getNullableJSON(String point) {
        return getNullableAs(point, JSON.class);
    }

    public JSON getNullableJSON() {
        return getNullableJSON(".");
    }

    public JSON getNullableJSON(String point, JSON defaultValue) {
        return getNullableAs(point, JSON.class, defaultValue);
    }

    public JSON getNullableJSON(JSON defaultValue) {
        return getNullableJSON(".", defaultValue);
    }

    public JSON getNullableJSON(String point, Supplier<JSON> defaultValueSupplier) {
        return getNullableAs(point, JSON.class, defaultValueSupplier);
    }

    public JSON getNullableJSON(Supplier<JSON> defaultValueSupplier) {
        return getNullableJSON(".", defaultValueSupplier);
    }

    public JSON getNullableJSON(String point, DefaultValueMap defaultValueMap) {
        return getNullableAs(point, JSON.class, defaultValueMap);
    }

    public JSON getNullableJSON(DefaultValueMap defaultValueMap) {
        return getNullableJSON(".", defaultValueMap);
    }

    public <T> List<T> getList(String point, Class<T> itemType) {
        return this.point(point).get().asList(itemType);
    }

    public <T> List<T> getList(Class<T> itemType) {
        return this.getList(".", itemType);
    }

    public <T> List<T> getList(String point, Class<T> itemType, Object defaultValue) {
        return this.point(point, defaultValue).get().asList(itemType);
    }

    public <T> List<T> getList(Class<T> itemType, Object defaultValue) {
        return this.getList(".", itemType, defaultValue);
    }

    public <T> List<T> getList(String point, Class<T> itemType, Supplier<Object> defaultValueSupplier) {
        return this.point(point, defaultValueSupplier).get().asList(itemType);
    }

    public <T> List<T> getList(Class<T> itemType, Supplier<Object> defaultValueSupplier) {
        return this.getList(".", itemType, defaultValueSupplier);
    }

    public <T> List<T> getList(String point, Class<T> itemType, DefaultValueMap defaultValueMap) {
        return this.point(point, defaultValueMap).get().asList(itemType);
    }

    public <T> List<T> getList(Class<T> itemType, DefaultValueMap defaultValueMap) {
        return this.getList(".", itemType, defaultValueMap);
    }

    public <T> List<T> getNullableList(String point, Class<T> itemType) {
        return this.point(point).get(true).asList(itemType);
    }

    public <T> List<T> getNullableList(Class<T> itemType) {
        return this.getNullableList(".", itemType);
    }

    public <T> List<T> getNullableList(String point, Class<T> itemType, Object defaultValue) {
        return this.point(point, defaultValue).get(true).asList(itemType);
    }

    public <T> List<T> getNullableList(Class<T> itemType, Object defaultValue) {
        return this.getNullableList(".", itemType, defaultValue);
    }

    public <T> List<T> getNullableList(
        String point,
        Class<T> itemType,
        Supplier<Object> defaultValueSupplier
    ) {
        return this.point(point, defaultValueSupplier).get(true).asList(itemType);
    }

    public <T> List<T> getNullableList(
        Class<T> itemType,
        Supplier<Object> defaultValueSupplier
    ) {
        return this.getNullableList(".", itemType, defaultValueSupplier);
    }

    public <T> List<T> getNullableList(String point, Class<T> itemType, DefaultValueMap defaultValueMap) {
        return this.point(point, defaultValueMap).get(true).asList(itemType);
    }

    public <T> List<T> getNullableList(Class<T> itemType, DefaultValueMap defaultValueMap) {
        return this.getNullableList(".", itemType, defaultValueMap);
    }

    public List<Object> getList(String point) {
        return getList(point, Object.class);
    }

    public List<Object> getList() {
        return getList(".");
    }

    public List<Object> getList(String point, List<Object> defaultValue) {
        return getList(point, Object.class, defaultValue);
    }

    public List<Object> getList(List<Object> defaultValue) {
        return getList(".", defaultValue);
    }

    public List<Object> getList(String point, Supplier<Object> defaultValueSupplier) {
        return getList(point, Object.class, defaultValueSupplier);
    }

    public List<Object> getList(Supplier<Object> defaultValueSupplier) {
        return getList(".", defaultValueSupplier);
    }

    public List<Object> getList(String point, DefaultValueMap defaultValueMap) {
        return getList(point, Object.class, defaultValueMap);
    }

    public List<Object> getList(DefaultValueMap defaultValueMap) {
        return getList(".", defaultValueMap);

    }

    public List<Object> getNullableList(String point) {
        return getNullableList(point, Object.class);
    }

    public List<Object> getNullableList() {
        return getNullableList(".");
    }

    public List<Object> getNullableList(String point, List<Object> defaultValue) {
        return getNullableList(point, Object.class, defaultValue);
    }

    public List<Object> getNullableList(List<Object> defaultValue) {
        return getNullableList(".", defaultValue);
    }

    public List<Object> getNullableList(String point, Supplier<Object> defaultValueSupplier) {
        return getNullableList(point, Object.class, defaultValueSupplier);
    }

    public List<Object> getNullableList(Supplier<Object> defaultValueSupplier) {
        return getNullableList(".", defaultValueSupplier);
    }

    public List<Object> getNullableList(String point, DefaultValueMap defaultValueMap) {
        return getNullableList(point, Object.class, defaultValueMap);
    }

    public List<Object> getNullableList(DefaultValueMap defaultValueMap) {
        return getNullableList(".", defaultValueMap);
    }

    public HashMap<String, Object> getMap(String point) {
        return getMap(point, Object.class);
    }

    public HashMap<String, Object> getMap() {
        return getMap(".");
    }

    public HashMap<String, Object> getMap(String point, Object defaultValue) {
        return this.point(point, defaultValue).get().asMap(Object.class);
    }

    public HashMap<String, Object> getMap(Object defaultValue) {
        return this.getMap(".", defaultValue);
    }

    public HashMap<String, Object> getMap(String point, Supplier<Object> defaultValueSupplier) {
        return this.point(point, defaultValueSupplier).get().asMap(Object.class);
    }

    public HashMap<String, Object> getMap(Supplier<Object> defaultValueSupplier) {
        return this.getMap(".", defaultValueSupplier);
    }

    public HashMap<String, Object> getMap(String point, DefaultValueMap defaultValueMap) {
        return this.point(point, defaultValueMap).get().asMap(Object.class);
    }

    public HashMap<String, Object> getMap(DefaultValueMap defaultValueMap) {
        return this.getMap(".", defaultValueMap);
    }

    public HashMap<String, Object> getNullableMap(String point) {
        return this.point(point).get(true).asMap(Object.class);
    }

    public HashMap<String, Object> getNullableMap() {
        return this.getNullableMap(".");
    }

    public HashMap<String, Object> getNullableMap(String point, Object defaultValue) {
        return this.point(point, defaultValue).get(true).asMap(Object.class);
    }

    public HashMap<String, Object> getNullableMap(Object defaultValue) {
        return this.getNullableMap(".", defaultValue);
    }

    public HashMap<String, Object> getNullableMap(String point, Supplier<Object> defaultValueSupplier) {
        return this.point(point, defaultValueSupplier).get(true).asMap(Object.class);
    }

    public HashMap<String, Object> getNullableMap(Supplier<Object> defaultValueSupplier) {
        return this.getNullableMap(".", defaultValueSupplier);
    }

    public HashMap<String, Object> getNullableMap(String point, DefaultValueMap defaultValueMap) {
        return this.point(point, defaultValueMap).get(true).asMap(Object.class);
    }

    public HashMap<String, Object> getNullableMap(DefaultValueMap defaultValueMap) {
        return this.getNullableMap(".", defaultValueMap);
    }

    public <T> HashMap<String, T> getMap(String point, Class<T> valueType) {
        return this.point(point).get().asMap(valueType);
    }

    public <T> HashMap<String, T> getMap(Class<T> valueType) {
        return this.getMap(".", valueType);
    }

    public <T> HashMap<String, T> getMap(String point, Class<T> valueType, Object defaultValue) {
        return this.point(point, defaultValue).get().asMap(valueType);
    }

    public <T> HashMap<String, T> getMap(Class<T> valueType, Object defaultValue) {
        return this.getMap(".", valueType, defaultValue);
    }

    public <T> HashMap<String, T> getMap(
        String point,
        Class<T> valueType,
        Supplier<Object> defaultValueSupplier
    ) {
        return this.point(point, defaultValueSupplier).get().asMap(valueType);
    }

    public <T> HashMap<String, T> getMap(
        Class<T> valueType,
        Supplier<Object> defaultValueSupplier
    ) {
        return this.getMap(".", valueType, defaultValueSupplier);
    }

    public <T> HashMap<String, T> getMap(String point, Class<T> valueType, DefaultValueMap defaultValueMap) {
        return this.point(point, defaultValueMap).get().asMap(valueType);
    }

    public <T> HashMap<String, T> getMap(Class<T> valueType, DefaultValueMap defaultValueMap) {
        return this.getMap(".", valueType, defaultValueMap);
    }

    public <T> HashMap<String, T> getNullableMap(String point, Class<T> valueType) {
        return this.point(point).get(true).asMap(valueType);
    }

    public <T> HashMap<String, T> getNullableMap(Class<T> valueType) {
        return this.getNullableMap(".", valueType);
    }

    public <T> HashMap<String, T> getNullableMap(String point, Class<T> valueType, Object defaultValue) {
        return this.point(point, defaultValue).get(true).asMap(valueType);
    }

    public <T> HashMap<String, T> getNullableMap(Class<T> valueType, Object defaultValue) {
        return this.getNullableMap(".", valueType, defaultValue);
    }

    public <T> HashMap<String, T> getNullableMap(
        String point,
        Class<T> valueType,
        Supplier<T> defaultValueSupplier
    ) {
        return this.point(point, defaultValueSupplier).get(true).asMap(valueType);
    }

    public <T> HashMap<String, T> getNullableMap(
        Class<T> valueType,
        Supplier<T> defaultValueSupplier
    ) {
        return this.getNullableMap(".", valueType, defaultValueSupplier);
    }

    public <T> HashMap<String, T> getNullableMap(
        String point,
        Class<T> valueType,
        DefaultValueMap defaultValueMap
    ) {
        return this.point(point, defaultValueMap).get(true).asMap(valueType);
    }

    public <T> HashMap<String, T> getNullableMap(
        Class<T> valueType,
        DefaultValueMap defaultValueMap
    ) {
        return this.getNullableMap(".", valueType, defaultValueMap);
    }

    public static class JSONConfig {

        private JsonMapper.Builder customJacksonBuilder;

        private ObjectMapper customJacksonMapper;

        public JSONConfig(JsonMapper.Builder customJacksonBuilder) {
            this.customJacksonBuilder = customJacksonBuilder;
        }

        public JSONConfig features(HashMap<Object, Boolean> features) {
            features
                .forEach((key, value) -> {
                    if (key instanceof JsonReadFeature) {
                        customJacksonBuilder.configure((JsonReadFeature) key, value);
                    } else if (key instanceof JsonWriteFeature) {
                        customJacksonBuilder.configure((JsonWriteFeature) key, value);
                    } else if (key instanceof SerializationFeature) {
                        customJacksonBuilder.configure((SerializationFeature) key, value);
                    } else if (key instanceof DeserializationFeature) {
                        customJacksonBuilder.configure((DeserializationFeature) key, value);
                    } else {
                        throw new RuntimeException("no support feature:" + key.getClass().getName());
                    }
                });
            return this;
        }

        public JSONConfig serializationInclusion(JsonInclude.Include setSerializationInclusion) {
            customJacksonBuilder.serializationInclusion(setSerializationInclusion);
            return this;
        }

        /**
         * 增加注册模块
         */
        public JSONConfig registerModule(com.fasterxml.jackson.databind.Module... module) {
            customJacksonBuilder.addModules(module);
            return this;
        }

        /**
         * 最终生成ObjectMapper
         */
        public JSONConfig confirmToCreateMapper() {
            customJacksonMapper = customJacksonBuilder.build();
            return this;
        }

        public JSON JSON(JsonNode jacksonNode) {
            return new JSON(jacksonNode, customJacksonMapper);
        }

        public JSON JSON(boolean isObject) {
            return new JSON(isObject, customJacksonMapper);
        }

        public JSON missingNode() {
            return JSON.missingNode(customJacksonMapper);
        }

        public JSON nullNode() {
            return JSON.nullNode(customJacksonMapper);
        }

        public JSON sPut(String id, Object value) {
            return JSON.sPut(customJacksonMapper, id, value);
        }

        public JSON sAdd(Object... value) {
            return JSON.sAdd(customJacksonMapper, value);
        }

        public JSON createObject() {
            return new JSON(true, customJacksonMapper);
        }

        public JSON createArray() {
            return new JSON(false, customJacksonMapper);
        }

        @SneakyThrows
        public JSON assign(Object target, Object... sources) {
            ObjectReader merger = customJacksonMapper.readerForUpdating(target instanceof JSON ? ((JSON) target).getJacksonNode() : target);

            for (Object object : sources) {
                merger.readValue(JSON.parse(customJacksonMapper, object).getJacksonNode());
            }

            return JSON.parse(customJacksonMapper, target);
        }

        public JSON parse(Object object) {
            return JSON.parse(customJacksonMapper, object);
        }
    }
}

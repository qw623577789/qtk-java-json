package team.ytk.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import lombok.SneakyThrows;
import team.ytk.json.point.Point;
import team.ytk.json.point.Point.DefaultType;

public class JSON {

    private static ObjectMapper jackson = new ObjectMapper();
    private JsonNode json;

    public JSON(JsonNode jacksonNode) {
        this.json = jacksonNode;
    }

    public JSON(boolean isObject) {
        this.json = isObject ? jackson.createObjectNode() : jackson.createArrayNode();
    }

    public static JSON sPut(String id, Object value) {
        JSON json = new JSON(true);
        json.put(id, value);
        return json;
    }

    public static JSON sAdd(Object... value) {
        JSON json = new JSON(false);
        json.add(value);
        return json;
    }

    @SneakyThrows
    public static JSON parse(Object object) {
        if (object instanceof String) {
            String string = (String) object;
            return string.startsWith("{") && string.endsWith("}")
                ? new JSON(jackson.readTree(string))
                : new JSON(jackson.valueToTree(object).deepCopy());
        } else {
            return new JSON(jackson.valueToTree(object).deepCopy());
        }
    }

    public Point point(String point) {
        return new Point(point, "", new HashMap<String, DefaultType>(), this.json, this);
    }

    public Point point(String point, Supplier<Object> defaultValue) {
        return new Point(point, "", new HashMap<String, DefaultType>(), this.json, this)
        .defaultValue(defaultValue);
    }

    public Point point(String point, Object defaultValue) {
        return new Point(point, "", new HashMap<String, DefaultType>(), this.json, this)
        .defaultValue(defaultValue);
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

    public static JSON missingNode() {
        return new JSON(jackson.missingNode());
    }

    public static JSON nullNode() {
        return new JSON(jackson.nullNode());
    }

    public String toString() {
        return this.toString(false);
    }

    public String toString(boolean pretty) {
        return pretty ? this.json.toPrettyString() : this.json.toString();
    }
}

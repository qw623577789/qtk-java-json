package team.ytk.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;
import java.util.logging.StreamHandler;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import team.ytk.json.JSON.Point.DefaultType;

public class JSON {

    private static ObjectMapper jackson = new ObjectMapper();
    private JsonNode json;

    public JSON(JsonNode valueNode) {
        this.json = valueNode;
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
            return new JSON(jackson.readTree((String) object));
        } else {
            return new JSON(jackson.valueToTree(object).deepCopy());
        }
    }

    public Point point(String point) {
        return new Point(point, "", new HashMap<String, DefaultType>(), this.json, this);
    }

    public Point point() {
        return point(".");
    }

    public JSON put(String id, Object value) {
        if (value == null) {
            ((ObjectNode) (this.json)).set(id, null);
        } else if (value instanceof JSON) {
            ((ObjectNode) (this.json)).set(id, ((JSON) value).getValueNode());
        } else {
            ((ObjectNode) (this.json)).set(id, jackson.valueToTree(value));
        }

        return this;
    }

    public JSON add(Object... value) {
        if (value == null) {
            ((ArrayNode) (this.json)).add(jackson.valueToTree(null));
        } else {
            Arrays
                .asList(value)
                .forEach(
                    item -> {
                        if (item == null) {
                            ((ArrayNode) (this.json)).add(jackson.valueToTree(null));
                        } else if (item instanceof JSON) {
                            ((ArrayNode) (this.json)).add(((JSON) item).getValueNode());
                        } else {
                            ((ArrayNode) (this.json)).add(jackson.valueToTree(item));
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

    public JsonNode getValueNode() {
        return this.json;
    }

    public String toString() {
        return this.json.toString();
    }

    public static class Point {

        private String breakcrumb = "";

        private String point;

        private JsonNode instance;

        private JSON jsonHelper;

        @Getter
        @Setter
        private HashMap<String, DefaultType> defaultValueMapper = new HashMap<>();

        public Point(
            String point,
            String breakcrumb,
            HashMap<String, DefaultType> defaultValueMapper,
            JsonNode instance,
            JSON jsonHelper
        ) {
            this.point = point;
            this.breakcrumb = breakcrumb;
            this.defaultValueMapper = defaultValueMapper;
            this.instance = instance;
            this.jsonHelper = jsonHelper;
        }

        public Point point(String point) {
            return new Point(
                point,
                this.breakcrumb + this.point,
                this.defaultValueMapper,
                this.instance,
                this.jsonHelper
            );
        }

        public Get get() {
            Get getValue = new Get(this.instance, this.defaultValueMapper);
            return getValue.get(this.breakcrumb, this.point, true, true);
        }

        public JSON put(String id, Object value) {
            return put(id, jackson.valueToTree(value));
        }

        public JSON put(String id, JSON value) {
            return put(id, value.getValueNode());
        }

        public JSON put(String id, JsonNode value) {
            Get pointValue = get();

            ArrayList<ObjectNode> nodes = new ArrayList<ObjectNode>();
            if (pointValue.isArray()) {
                nodes.addAll(pointValue.asList(ObjectNode.class));
            } else {
                nodes.add(pointValue.as(ObjectNode.class));
            }
            nodes
                .stream()
                .forEach(
                    item -> {
                        if (item.isMissingNode()) return;
                        if (!item.isObject()) {
                            throw new RuntimeException(
                                "path:" + pointValue.nodePathMapper.get(item.hashCode()) + "节点必须为对象"
                            );
                        }
                        item.set(id, value.deepCopy());
                    }
                );

            return this.jsonHelper;
        }

        public boolean has() {
            Get getValue = new Get(this.instance, this.defaultValueMapper);
            JsonNode result = getValue.get(this.breakcrumb, this.point, true, false).valueNode;
            return result.isArray()
                ? (
                    result.size() == 0 //空数组也算has
                        ? true
                        : StreamSupport
                            .stream(result.spliterator(), false)
                            .anyMatch(item -> !item.isMissingNode())
                )
                : !result.isMissingNode();
        }

        public boolean has(boolean toWithDefault) {
            Get getValue = new Get(this.instance, this.defaultValueMapper);
            JsonNode result = getValue.get(this.breakcrumb, this.point, toWithDefault, false).valueNode;
            return result.isArray()
                ? (
                    result.size() == 0 //空数组也算has
                        ? true
                        : StreamSupport
                            .stream(result.spliterator(), false)
                            .anyMatch(item -> !item.isMissingNode())
                )
                : !result.isMissingNode();
        }

        public JSON delete() {
            String absouleBreakcrumb = this.breakcrumb + this.point;

            List<String> keys = Pattern
                .compile("\\.\".*?\"|\\..*?(?=\\.)|\\..*$")
                .matcher(absouleBreakcrumb)
                .results()
                .map(item -> item.group())
                .collect(Collectors.toList());

            String lastPointKey = keys.get(keys.size() - 1).replaceAll("\"", "").substring(1);

            // 若lastPointKey为数组key
            List<String> keyInfo = Pattern
                .compile("([\\w|\\.]{1,})|(?<=\\[)([0-9]{1,}|\\*)(?=\\])")
                .matcher(lastPointKey)
                .results()
                .map(i -> i.group())
                .collect(Collectors.toList());

            // 是否为纯数组节点
            boolean isArrayKey = Pattern
                .compile("^(\\[([0-9]{1,}|\\*)\\]){1,}[\\?]{0,1}$")
                .matcher(lastPointKey)
                .matches();

            lastPointKey = keyInfo.get(0);

            List<String> parentPointKeyStage = keys.subList(0, keys.size() - 1);

            if (keyInfo.size() > 1) {
                parentPointKeyStage.add(
                    ".\"" +
                    keyInfo.get(0) + //key
                    keyInfo //除最后一个数组以外的剩余的数组元素
                        .subList(1, keyInfo.size() - 1)
                        .stream()
                        .map(item -> "[" + item + "]")
                        .collect(Collectors.joining("")) +
                    "\""
                );
                lastPointKey = keyInfo.get(keyInfo.size() - 1);
                isArrayKey = true;
            }

            String parentPointKey = parentPointKeyStage.stream().collect(Collectors.joining(""));

            Get parentNode = new Get(this.instance, this.defaultValueMapper);
            parentNode.get("", parentPointKey, true, false);

            String finalLastPointKey = lastPointKey;
            boolean finalLastPointKeyIsArrayKey = isArrayKey;
            ArrayNode nodes = JSON.jackson.createArrayNode();
            if (parentPointKey.contains("[*]")) {
                nodes.addAll((ArrayNode) parentNode.valueNode);
            } else {
                nodes.add(parentNode.valueNode);
            }

            StreamSupport
                .stream(nodes.spliterator(), false)
                .forEach(
                    item -> {
                        if (item.isMissingNode()) return;

                        if (finalLastPointKeyIsArrayKey) {
                            if (!item.isArray()) {
                                throw new RuntimeException(
                                    "path:" +
                                    parentNode.nodePathMapper.get(item.hashCode()) +
                                    "节点必须为数组"
                                );
                            }
                            ((ArrayNode) item).remove(Integer.parseInt(finalLastPointKey));
                        } else { // 父指针是个数组
                            if (!item.isObject()) {
                                throw new RuntimeException(
                                    "path:" +
                                    parentNode.nodePathMapper.get(item.hashCode()) +
                                    "节点必须为对象"
                                );
                            }
                            ((ObjectNode) item).remove(finalLastPointKey);
                        }
                    }
                );

            return this.jsonHelper;
        }

        public JSON add(Object... items) {
            Get pointValue = get();

            String absouleBreakcrumb = this.breakcrumb + this.point;

            ArrayNode nodes = JSON.jackson.createArrayNode();
            if (absouleBreakcrumb.contains("[*]")) {
                nodes.addAll((ArrayNode) pointValue.valueNode);
            } else {
                nodes.add(pointValue.valueNode);
            }

            StreamSupport
                .stream(nodes.spliterator(), false)
                .forEach(
                    node -> {
                        if (node.isMissingNode()) return;
                        if (!node.isArray()) {
                            throw new RuntimeException(
                                "path:" + pointValue.nodePathMapper.get(node.hashCode()) + "节点必须为数组"
                            );
                        }

                        ArrayNode arrayNode = (ArrayNode) node;
                        Arrays
                            .asList(items)
                            .stream()
                            .forEach(
                                item -> {
                                    if (item instanceof JSON) {
                                        arrayNode.add((((JSON) item).getValueNode()).deepCopy());
                                    } else if (item instanceof JsonNode) {
                                        arrayNode.add(((JsonNode) item).deepCopy());
                                    } else {
                                        arrayNode.add(jackson.valueToTree(item));
                                    }
                                }
                            );
                    }
                );

            return this.jsonHelper;
        }

        public JSON concat(List<Object> list) {
            return add(list.toArray());
        }

        public Point defaultValue(Supplier<Object> defaultValue) {
            return defaultValue(defaultValue);
        }

        public Point defaultValue(Object defaultValue) {
            DefaultType def = DefaultType.builder().value(defaultValue).build();
            this.defaultValueMapper.put(this.breakcrumb + this.point, def);
            return this;
        }

        public Point defaultValue(Supplier<Object> defaultValue, boolean strict) {
            return defaultValue(defaultValue, strict);
        }

        public Point defaultValue(Object defaultValue, boolean strict) {
            this.defaultValueMapper.put(
                    this.breakcrumb + this.point,
                    DefaultType.builder().strict(strict).value(defaultValue).build()
                );
            return this;
        }

        // default的point必须为point本体子集
        public Point defaultValue(List<DefaultType> defaultList) {
            defaultList
                .stream()
                .forEach(
                    item -> {
                        String point = item.getPoint();
                        if (!this.point.replaceAll("\\?", "").startsWith(point)) {
                            throw new RuntimeException(
                                "point:" + point + "必须为point:" + this.point + "子集"
                            );
                        }
                        this.defaultValueMapper.put(this.breakcrumb + this.point + item.getPoint(), item);
                    }
                );
            return this;
        }

        @Builder
        @Data
        public static class DefaultType {

            @Builder.Default
            private boolean strict = false;

            @Builder.Default
            private String point = ".";

            private Object value;
        }

        public class Get {
            @Getter
            private JsonNode valueNode;

            private HashMap<Pattern, DefaultType> regexpDefaultValueMapper = new HashMap<>();

            private HashMap<String, DefaultType> defaultValueMapper = new HashMap<>();

            private ConcurrentHashMap<Integer, String> nodePathMapper = new ConcurrentHashMap<Integer, String>();

            private ConcurrentLinkedQueue<String> missingNodeList = new ConcurrentLinkedQueue<>();

            public Get(JsonNode value, HashMap<String, DefaultType> defaultValueMapper) {
                this.valueNode = value;
                defaultValueMapper
                    .entrySet()
                    .stream()
                    .forEach(
                        entry -> {
                            this.regexpDefaultValueMapper.put(
                                    Pattern.compile(
                                        entry
                                            .getKey()
                                            .replaceAll("*", "\\d+")
                                            .replaceAll("[", "\\[")
                                            .replaceAll("]", "\\]")
                                    ),
                                    entry.getValue()
                                );
                        }
                    );
            }

            public boolean isArray() {
                return valueNode.isArray();
            }

            public Get get(
                String breadcrumb,
                String point,
                boolean toWithDefault,
                boolean supportNullishKey
            ) {
                String absouleBreakcrumb = breakcrumb + point;

                this.nodePathMapper.put(instance.hashCode(), ".");
                this.valueNode =
                    absouleBreakcrumb.equals(".")
                        ? instance
                        : IndexStream
                            .add(
                                Pattern
                                    .compile("\\.\".*?\"|\\..*?(?=\\.)|\\..*$")
                                    .matcher(absouleBreakcrumb)
                                    .results()
                                    .map(item -> item.group())
                            )
                            .reduce(
                                instance,
                                (valueNode, node) -> {
                                    boolean isLastKey = node.getIndex() == node.getNodes().size() - 1;

                                    String key = node.getNode().substring(1).replaceAll("\"", "");

                                    if (!valueNode.isArray()) {
                                        valueNode = jackson.createArrayNode().add(valueNode);
                                    }

                                    ArrayNode returnNodes =
                                        this.iterValue(
                                                key,
                                                (ArrayNode) valueNode,
                                                toWithDefault,
                                                supportNullishKey
                                            );

                                    if (isLastKey == false) { //非最后一个key的话，其值节点应该是一个object
                                        StreamSupport
                                            .stream(returnNodes.spliterator(), false)
                                            .forEach(
                                                item -> {
                                                    boolean check =
                                                        item.isObject() ||
                                                        item.isArray() ||
                                                        item.isMissingNode() ||
                                                        item.isNull();

                                                    if (check == false) throw new RuntimeException(
                                                        "path:" +
                                                        nodePathMapper.get(item.hashCode()) +
                                                        "节点非为对象或者空值节点"
                                                    );
                                                }
                                            );
                                    }

                                    if (absouleBreakcrumb.contains("[*]")) {
                                        return returnNodes;
                                    } else {
                                        return returnNodes.get(0);
                                    }
                                },
                                (l, r) -> l
                            );

                return this;
            }

            private ArrayNode iterValue(
                String key,
                ArrayNode valueNode,
                boolean toWithDefault,
                boolean supportNullishKey
            ) {
                // 是否为纯数组节点
                boolean isArrayKey = Pattern
                    .compile("^(\\[([0-9]{1,}|\\*)\\]){1,}[\\?]{0,1}$")
                    .matcher(key)
                    .matches();

                // 是否为可选链节点
                Boolean nullishKey = supportNullishKey ? key.endsWith("?") : false;

                List<String> keyInfo = Pattern
                    .compile("([\\w|\\.]{1,})|(?<=\\[)([0-9]{1,}|\\*)(?=\\])")
                    .matcher(key)
                    .results()
                    .map(i -> i.group())
                    .collect(Collectors.toList());

                if (isArrayKey) {
                    String parentNodePath = nodePathMapper.get(valueNode.hashCode());
                    return jackson
                        .createArrayNode()
                        .addAll(this.flatArrayNode(parentNodePath, "", keyInfo, valueNode, nullishKey));
                }

                /**
                 * 下面为对象节点/对象数组节点处理逻辑
                 */
                String realKey = keyInfo.get(0);

                List<String> arrayIndexs = keyInfo.subList(1, keyInfo.size());

                return StreamSupport
                    .stream(valueNode.spliterator(), false)
                    .reduce(
                        jackson.createArrayNode(),
                        (collection, n) -> {
                            if (n.isMissingNode() || n.isNull()) {
                                collection.add(n);
                                return collection;
                            }

                            String parentNodePath = nodePathMapper.get(n.hashCode());

                            JsonNode iterValueNode = n.get(realKey);

                            if (!n.has(realKey)) { // iterValueNode.isMissing
                                if (nullishKey) {
                                    collection.add(NullNode.getInstance().deepCopy());
                                } else {
                                    collection.add(MissingNode.getInstance().deepCopy());
                                    missingNodeList.add(
                                        (parentNodePath.equals(".") ? "" : parentNodePath) + "." + realKey
                                    );
                                }
                                return collection;
                            }

                            if (arrayIndexs.size() == 0) { //对象
                                nodePathMapper.put(
                                    iterValueNode.hashCode(),
                                    (parentNodePath.equals(".") ? "" : parentNodePath) + "." + realKey
                                );
                                collection.add(iterValueNode);
                            } else {
                                collection.addAll(
                                    this.flatArrayNode(
                                            parentNodePath,
                                            realKey,
                                            arrayIndexs,
                                            iterValueNode,
                                            nullishKey
                                        )
                                );
                            }

                            return collection;
                        },
                        (l, r) -> l
                    );
            }

            private ArrayNode flatArrayNode(
                String parentNodePath,
                String key,
                List<String> arrayIndexs,
                JsonNode valueNode,
                Boolean nullishKey
            ) {
                return IndexStream
                    .add(arrayIndexs.stream())
                    .reduce(
                        jackson.createArrayNode(),
                        (prev, arrayIndexNode) -> {
                            String arrayIndex = arrayIndexNode.getNode();
                            if (arrayIndex.equals("*")) { // 数组遍历
                                if (arrayIndexNode.getIndex() == 0) {
                                    if (!valueNode.isArray()) {
                                        throw new RuntimeException(
                                            "path:" + parentNodePath + "其值必须为数组"
                                        );
                                    }

                                    ArrayNode array = jackson.createArrayNode();
                                    IndexStream
                                        .add(StreamSupport.stream(valueNode.spliterator(), false))
                                        .forEach(
                                            node -> {
                                                nodePathMapper.put(
                                                    node.getNode().hashCode(),
                                                    (parentNodePath.equals(".") ? "" : parentNodePath) +
                                                    "." +
                                                    key +
                                                    "[" +
                                                    node.getIndex() +
                                                    "]"
                                                );
                                                array.add(node.getNode());
                                            }
                                        );
                                    prev = array;
                                } else {
                                    prev =
                                        StreamSupport
                                            .stream(prev.spliterator(), false)
                                            .reduce(
                                                jackson.createArrayNode(),
                                                (c, curr) -> {
                                                    String iterParentNodePath = nodePathMapper.get(
                                                        curr.hashCode()
                                                    );

                                                    if (!curr.isArray()) {
                                                        throw new RuntimeException(
                                                            "path:" + iterParentNodePath + "其值必须为数组"
                                                        );
                                                    }

                                                    IndexStream
                                                        .add(StreamSupport.stream(curr.spliterator(), false))
                                                        .forEach(
                                                            node -> {
                                                                nodePathMapper.put(
                                                                    node.getNode().hashCode(),
                                                                    iterParentNodePath +
                                                                    "[" +
                                                                    node.getIndex() +
                                                                    "]"
                                                                );
                                                                c.add(node.getNode());
                                                            }
                                                        );
                                                    return c;
                                                },
                                                (l, r) -> l
                                            );
                                }
                            } else {
                                if (arrayIndexNode.getIndex() == 0) {
                                    if (!valueNode.isArray()) {
                                        throw new RuntimeException(
                                            "path:" + parentNodePath + "其值必须为数组"
                                        );
                                    }

                                    JsonNode node = valueNode.has(Integer.parseInt(arrayIndex))
                                        ? valueNode.get(Integer.parseInt(arrayIndex))
                                        : (
                                            nullishKey
                                                ? NullNode.getInstance()
                                                : MissingNode.getInstance().deepCopy()
                                        );

                                    if (node.isMissingNode()) {
                                        missingNodeList.add(
                                            (parentNodePath.equals(".") ? "" : parentNodePath) +
                                            "." +
                                            key +
                                            "[" +
                                            arrayIndex +
                                            "]"
                                        );
                                    }
                                    nodePathMapper.put(
                                        node.hashCode(),
                                        (parentNodePath.equals(".") ? "" : parentNodePath) +
                                        "." +
                                        key +
                                        "[" +
                                        arrayIndex +
                                        "]"
                                    );

                                    prev.add(node);
                                } else {
                                    prev =
                                        StreamSupport
                                            .stream(prev.spliterator(), false)
                                            .reduce(
                                                jackson.createArrayNode(),
                                                (c, item) -> {
                                                    String iterParentNodePath = nodePathMapper.get(
                                                        item.hashCode()
                                                    );

                                                    if (item.isMissingNode() || item.isNull()) {
                                                        c.add(item);
                                                    } else {
                                                        if (!item.isArray()) {
                                                            throw new RuntimeException(
                                                                "path:" +
                                                                iterParentNodePath +
                                                                "其值必须为数组"
                                                            );
                                                        }

                                                        JsonNode node = item.has(Integer.parseInt(arrayIndex))
                                                            ? item.get(Integer.parseInt(arrayIndex))
                                                            : (
                                                                nullishKey
                                                                    ? NullNode.getInstance().deepCopy()
                                                                    : MissingNode.getInstance().deepCopy()
                                                            );

                                                        if (node.isMissingNode()) {
                                                            missingNodeList.add(
                                                                iterParentNodePath + "[" + arrayIndex + "]"
                                                            );
                                                        }

                                                        nodePathMapper.put(
                                                            node.hashCode(),
                                                            iterParentNodePath + "[" + arrayIndex + "]"
                                                        );
                                                        c.add(node);
                                                    }

                                                    return c;
                                                },
                                                (l, r) -> l
                                            );
                                }
                            }
                            return prev;
                        },
                        (l, r) -> l
                    );
            }

            private JsonNode fixValueWithDefault(JsonNode value) {
                String nodePath = nodePathMapper.get(value.hashCode());
                DefaultType defaultType = Optional
                    .ofNullable(this.defaultValueMapper.get(breakcrumb))
                    .orElseGet(
                        () -> {
                            return this.regexpDefaultValueMapper.entrySet()
                                .stream()
                                .filter(entry -> entry.getKey().asPredicate().test(nodePath))
                                .findFirst()
                                .map(m -> m.getValue())
                                .orElse(null);
                        }
                    );

                if (defaultType == null) return value;
                if (defaultType.isStrict()) {
                    if (value == null) {
                        Object defaultValue = defaultType.getValue();
                        return JSON.jackson.valueToTree(
                            defaultValue instanceof Supplier ? ((Supplier) defaultValue).get() : defaultValue
                        );
                    }
                } else {
                    if (
                        value == null ||
                        (value.isNumber() && value.asInt() == 0) ||
                        (value.isBoolean() && value.asBoolean() == false)
                    ) {
                        Object defaultValue = defaultType.getValue();
                        return JSON.jackson.valueToTree(
                            defaultValue instanceof Supplier ? ((Supplier) defaultValue).get() : defaultValue
                        );
                    }
                }
                return value;
            }

            public String asString() {
                if (valueNode.isMissingNode()) {
                    throw new NullPointerException(
                        this.missingNodeList.stream().collect(Collectors.joining(",")) + " is missing"
                    );
                }
                return valueNode.isNull() ? null : valueNode.asText();
            }

            public Long asLong() {
                if (valueNode.isMissingNode()) {
                    throw new NullPointerException(
                        this.missingNodeList.stream().collect(Collectors.joining(",")) + " is missing"
                    );
                }
                return valueNode.isNull() ? null : valueNode.asLong();
            }

            public Integer asInt() {
                if (valueNode.isMissingNode()) {
                    throw new NullPointerException(
                        this.missingNodeList.stream().collect(Collectors.joining(",")) + " is missing"
                    );
                }
                return valueNode.isNull() ? null : valueNode.asInt();
            }

            public Boolean asBoolean() {
                if (valueNode.isMissingNode()) {
                    throw new NullPointerException(
                        this.missingNodeList.stream().collect(Collectors.joining(",")) + " is missing"
                    );
                }
                return valueNode.isNull() ? null : valueNode.asBoolean();
            }

            public Double asDouble() {
                if (valueNode.isMissingNode()) {
                    throw new NullPointerException(
                        this.missingNodeList.stream().collect(Collectors.joining(",")) + " is missing"
                    );
                }
                return valueNode.isNull() ? null : valueNode.asDouble();
            }

            public Double asNull() {
                if (valueNode.isMissingNode()) {
                    throw new NullPointerException(
                        this.missingNodeList.stream().collect(Collectors.joining(",")) + " is missing"
                    );
                }
                if (!valueNode.isNull()) {
                    throw new RuntimeException("the value is not null");
                }
                return null;
            }

            public BigDecimal asBigDecimal() {
                if (valueNode.isMissingNode()) {
                    throw new NullPointerException(
                        this.missingNodeList.stream().collect(Collectors.joining(",")) + " is missing"
                    );
                }
                return valueNode.isNull() ? null : valueNode.decimalValue();
            }

            public Float asFloat() {
                if (valueNode.isMissingNode()) {
                    throw new NullPointerException(
                        this.missingNodeList.stream().collect(Collectors.joining(",")) + " is missing"
                    );
                }
                return valueNode.isNull() ? null : valueNode.floatValue();
            }

            public <T> T as(Class<T> type) {
                if (valueNode.isMissingNode()) {
                    throw new NullPointerException(
                        this.missingNodeList.stream().collect(Collectors.joining(",")) + " is missing"
                    );
                }
                return valueNode.isNull() ? null : jackson.convertValue(this.valueNode, type);
            }

            public List<Object> asList() {
                return asList(Object.class);
            }

            public <T> List<T> asList(Class<T> itemType) {
                if (!valueNode.isArray()) {
                    throw new RuntimeException("最终节点非数组节点");
                }

                if (
                    StreamSupport
                        .stream(valueNode.spliterator(), false)
                        .anyMatch(item -> item.isMissingNode())
                ) {
                    throw new NullPointerException(
                        this.missingNodeList.stream().collect(Collectors.joining(",")) + " is missing"
                    );
                }
                // if (valueNode..isNull()) return null;
                ArrayList<T> list = new ArrayList<>();
                valueNode.elements().forEachRemaining(item -> list.add(jackson.convertValue(item, itemType)));
                return list;
            }

            public HashMap<String, Object> asMap() {
                if (valueNode.isMissingNode()) {
                    throw new NullPointerException(
                        this.missingNodeList.stream().collect(Collectors.joining(",")) + " is missing"
                    );
                }
                if (valueNode.isNull()) return null;
                HashMap<String, Object> map = new HashMap<>();

                valueNode.fields().forEachRemaining(entry -> map.put(entry.getKey(), entry.getValue()));

                return map;
            }
        }
    }
}

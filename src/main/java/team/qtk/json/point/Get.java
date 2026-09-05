package team.qtk.json.point;

import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.Getter;
import lombok.SneakyThrows;
import team.qtk.json.JSON;
import team.qtk.json.JsonStringifyPrettyPrinter;
import team.qtk.json.node.ArrayNode;
import team.qtk.json.node.Node;
import team.qtk.json.node.QOneOf;
import team.qtk.json.point.Point.DefaultType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Get {

    private static final String[] ESCAPE_CHARS = { "\\", "$", "(", ")", "*", "+", ".", "[", "]", "?", "^", "{", "}", "|", "-" };

    private static final Pattern BREADCRUMB_PATTERN = Pattern.compile("\\??\\.\".*?\"|\\??\\..*?(?=\\??\\.)|\\??\\..*$");
    private static final Pattern ARRAY_KEY_PATTERN = Pattern.compile("^(\\[([0-9]+|\\*)])+\\??$");
    private static final Pattern KEY_INFO_PATTERN = Pattern.compile("([^\\[|\\]?]+)|(?<=\\[)([0-9]+|\\*)(?=])");

    @Getter
    private Node valueNode;

    private HashMap<Pattern, DefaultType> regexpDefaultValueMapper;

    private boolean nullable = false;

    private JSON jsonHelper;

    public Get(JsonNode value, HashMap<String, DefaultType> defaultValueMapper, JSON jsonHelper) {
        this.valueNode = Node.gen(value, ".");
        if (defaultValueMapper != null && !defaultValueMapper.isEmpty()) {
            this.regexpDefaultValueMapper = new HashMap<>();
            defaultValueMapper
                .forEach((key, value1) -> this.regexpDefaultValueMapper.put(
                    Pattern.compile(
                        "^" +
                            this.escapeExprSpecialWord(key).replaceAll("\\*", "d+") +
                            "$"
                    ),
                    value1
                ));
        }
        this.jsonHelper = jsonHelper;
    }

    public String escapeExprSpecialWord(String keyword) {
        for (String key : ESCAPE_CHARS) {
            if (keyword.contains(key)) {
                keyword = keyword.replace(key, "\\" + key);
            }
        }
        return keyword;
    }

    public boolean isArray() {
        return valueNode.isArray();
    }

    public boolean isNull() {
        return valueNode.isNull();
    }

    public boolean isMissing() {
        return valueNode.isMissingNode();
    }

    public boolean isEmpty() {
        return valueNode.isEmpty();
    }

    public boolean isObject() {
        return valueNode.isObject();
    }

    public boolean isString() {
        return valueNode.getJacksonNode() instanceof TextNode;
    }

    public boolean isNumber() {
        return valueNode.getJacksonNode() instanceof NumericNode;
    }

    public Get get(
        String breadcrumb,
        String point,
        boolean toWithDefault,
        boolean supportNullishKey,
        boolean nullable
    ) {
        this.nullable = nullable;

        String absouleBreakcrumb = breadcrumb + point;

        if (absouleBreakcrumb.equals(".") || absouleBreakcrumb.equals("?.")) return this;

        boolean isWildcardPath = absouleBreakcrumb.contains("[*]");

        var matchResults = BREADCRUMB_PATTERN.matcher(absouleBreakcrumb).results().toList();
        int size = matchResults.size();

        for (int i = 0; i < size; i++) {
            var node = matchResults.get(i);
            boolean isLastKey = i == size - 1;

            String nodeGroup = node.group();

            String key = nodeGroup.substring(nodeGroup.indexOf(".") + 1).replace("\"", "");

            boolean hasNullishKey = supportNullishKey && nodeGroup.startsWith("?");

            // 若节点为非数组，也处理成数组，下面结果输出时再转化出来
            if (!this.valueNode.isArray()) {
                this.valueNode = ArrayNode.create("无意义", this.jsonHelper).add(this.valueNode);
            }

            ArrayNode returnNodes =
                this.getIterValue(key, (ArrayNode) this.valueNode, toWithDefault, hasNullishKey);

            if (!isLastKey) { //非最后一个key的话，其值节点应该是一个非基础元素节点
                for (Node item : returnNodes.getNodes()) {
                    boolean isValidNode =
                        item.isObject() ||
                            item.isArray() ||
                            item.isMissingNode() ||
                            item.isNull();

                    if (!isValidNode) throw new RuntimeException(
                        "path:" + item.getPath() + "节点非为对象或者空值节点"
                    );
                }
            }

            // 若路径中存在[*],那结果肯定为数组，否则只需取第一个元素即可(上面非数组节点特殊处理后的转化)
            this.valueNode = isWildcardPath ? returnNodes : returnNodes.get(0);
        }

        return this;
    }

    private ArrayNode getIterValue(
        String key,
        ArrayNode valueNode,
        boolean toWithDefault,
        boolean hasNullishKey
    ) {
        // 是否为纯数组节点
        boolean isArrayKey = ARRAY_KEY_PATTERN
            .matcher(key)
            .matches();

        List<String> keyInfo = KEY_INFO_PATTERN
            .matcher(key)
            .results()
            .map(MatchResult::group)
            .collect(Collectors.toList());

        // 纯数组
        if (isArrayKey) {
            return ArrayNode
                .create("无意义", this.jsonHelper)
                .addAll(this.flatArrayNode(keyInfo, valueNode, toWithDefault, hasNullishKey));
        }

        // 下面为对象节点/对象数组节点处理逻辑
        String realKey = keyInfo.get(0); //　纯数组已经在上面处理，这里必为对象，第一个必为字段名

        List<String> arrayIndexes = keyInfo.subList(1, keyInfo.size());

        return valueNode
            .stream()
            .reduce(
                ArrayNode.create("无意义", this.jsonHelper),
                (collection, node) -> {
                    if (node.isMissingNode() || node.isNull()) {
                        collection.add(node);
                        return collection;
                    }

                    String nodePath = node.getPath();

                    String subNodePath = (nodePath.equals(".") ? "" : nodePath) + "." + realKey;

                    Node subNode = null;

                    if (!node.has(realKey)) { // 节点不存在
                        if (toWithDefault) subNode = fixValueWithDefault(subNodePath, node, realKey);
                        if (!toWithDefault || subNode.isMissingNode()) {
                            if (hasNullishKey) {
                                collection.add(Node.createNullNode(subNodePath));
                            } else {
                                collection.add(Node.createMissingNode(subNodePath));
                            }
                            return collection;
                        }
                    } else {
                        subNode = node.get(realKey);
                    }

                    if (arrayIndexes.isEmpty()) { //对象
                        collection.add(subNode);
                    } else {
                        collection.addAll(
                            this.flatArrayNode(arrayIndexes, subNode, toWithDefault, hasNullishKey)
                        );
                    }

                    return collection;
                },
                (l, r) -> l
            );
    }

    /**
     * @param arrayIndexes  多维数组里下标数组
     * @param valueNode     节点
     * @param toWithDefault 是否使用默认值填充
     * @param hasNullishKey 是否有可选链
     */
    private ArrayNode flatArrayNode(
        List<String> arrayIndexes,
        Node valueNode,
        boolean toWithDefault,
        Boolean hasNullishKey
    ) {
        return arrayIndexes
            .stream()
            .reduce(
                ArrayNode.create("无意义", this.jsonHelper).add(valueNode), // 适配第一次遍历
                (prev, arrayIndex) -> {
                    if (arrayIndex.equals("*")) { // 数组遍历
                        return prev
                            .stream()
                            .reduce(
                                ArrayNode.create("无意义", this.jsonHelper),
                                (collection, node) -> {
                                    String nodePath = node.getPath();

                                    if (!node.isArray()) {
                                        throw new RuntimeException("path:" + nodePath + "其值必须为数组");
                                    }

                                    node.stream().forEach(collection::add);

                                    return collection;
                                },
                                (l, r) -> l
                            );
                    } else {
                        return prev
                            .stream()
                            .reduce(
                                ArrayNode.create("无意义", this.jsonHelper),
                                (collection, node) -> {
                                    String nodePath = node.getPath();

                                    String subNodePath = (nodePath.equals(".") ? "." : nodePath) + "[" + arrayIndex + "]";

                                    if (node.isMissingNode() || node.isNull()) {
                                        collection.add(node);
                                    } else {
                                        if (!node.isArray()) {
                                            throw new RuntimeException("path:" + nodePath + "其值必须为数组");
                                        }

                                        Node subNode = null;
                                        if (!node.has(Integer.parseInt(arrayIndex))) {
                                            if (toWithDefault) subNode =
                                                fixValueWithDefault(
                                                    subNodePath,
                                                    node,
                                                    "[" + arrayIndex + "]"
                                                );
                                            if (!toWithDefault || subNode.isMissingNode()) {
                                                subNode =
                                                    hasNullishKey
                                                        ? Node.createNullNode(subNodePath)
                                                        : Node.createMissingNode(subNodePath);
                                            }
                                        } else {
                                            subNode = node.get(Integer.parseInt(arrayIndex));
                                        }

                                        collection.add(subNode);
                                    }

                                    return collection;
                                },
                                (l, r) -> l
                            );
                    }
                },
                (l, r) -> l
            );
    }

    private Node fixValueWithDefault(String nodePath, Node node, String fieldName) {
        DefaultType defaultType = null;
        if (this.regexpDefaultValueMapper != null) {
            defaultType = this.regexpDefaultValueMapper.entrySet()
                .stream()
                .filter(entry -> entry.getKey().asPredicate().test(nodePath.startsWith(".") ? nodePath : "." + nodePath))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(null);
        }

        if (defaultType == null) return Node.createMissingNode(nodePath);

        Object defaultValue = defaultType.getValue();
        if (defaultValue instanceof Supplier) defaultValue = ((Supplier<?>) defaultValue).get();

        JsonNode jacksonNode = jsonHelper.toJsonNode(defaultValue);

        Node fixNode = Node.gen(jacksonNode, nodePath);

        if (defaultType.isToUpdateNode()) {
            if (node.isObject()) {
                node.set(fieldName, jacksonNode);
            } else if (node.isArray()) {
                ((ArrayNode) node).add(jacksonNode);
            } else {
                throw new RuntimeException(
                    "不支持" + node.getJacksonNode().getNodeType().name() + "补充默认值"
                );
            }
        }

        return fixNode;
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
            return JSON.writer(this.jsonHelper.jackson)
                .with(printer)
                .writeValueAsString(this.valueNode.getJacksonNode());
        } else {
            return this.valueNode.getJacksonNode().toString();
        }
    }

    public String asString() {
        return as(String.class);
    }

    public Long asLong() {
        return as(Long.class);
    }

    public Integer asInt() {
        return as(Integer.class);
    }

    public LocalDateTime asLocalDateTime() {
        return as(LocalDateTime.class);
    }

    public Boolean asBoolean() {
        return as(Boolean.class);
    }

    public Double asDouble() {
        return as(Double.class);
    }

    public Object asObject() {
        return as(Object.class);
    }

    public Void asNull() {
        return as(Void.class);
    }

    public BigDecimal asBigDecimal() {
        return as(BigDecimal.class);
    }

    public Float asFloat() {
        return as(Float.class);
    }

    /**
     * 深拷贝方法
     *
     * @return
     */
    public JSON asJSON() {
        return new JSON(this.valueNode.getJacksonNode().deepCopy(), this.jsonHelper.jackson);
    }

    @SneakyThrows
    public <T> T as(Class<T> type) {
        if (type == JSON.class) return (T) asJSON();
        if (this.valueNode.isMissingNode()) {
            if (this.nullable) {
                return null;
            } else {
                throw new NullPointerException((this.valueNode.getPath().startsWith(".") ? "" : ".") + this.valueNode.getPath() + " is missing");
            }
        }

        /*
         * 根节点是QOneOf的话，在此执行解析
         */
        if (QOneOf.class.isAssignableFrom(type)) {
            QOneOf oneOf = JSON.newOneOf(type.asSubclass(QOneOf.class));
            if (!this.valueNode.isNull()) {
                var jackNode = this.valueNode.getJacksonNode();
                if (jackNode.isTextual()) {
                    oneOf.value = jackNode.textValue();
                } else if (jackNode.isBoolean()) {
                    oneOf.value = jackNode.booleanValue();
                } else if (jackNode.isInt() || jackNode.isLong()) {
                    oneOf.value = jackNode.longValue();
                } else if (jackNode.isNumber()) {
                    oneOf.value = jackNode.decimalValue();
                } else if (jackNode.isArray()) {
                    oneOf.value = this.jsonHelper.jackson.convertValue(jackNode, ArrayList.class);
                } else if (jackNode.isObject()) {
                    oneOf.value = this.jsonHelper.jackson.convertValue(jackNode, LinkedHashMap.class);
                } else {
                    oneOf.value = this.jsonHelper.jackson.convertValue(jackNode, Object.class);
                }
            }
            return (T) oneOf;
        }

        if (this.valueNode.isNull()) {
            return null;
        }

        var jackNode = this.valueNode.getJacksonNode();

        // 标量类型且节点类型匹配时直接取值，避免convertValue的ObjectReader创建开销
        if (type == String.class && jackNode.isTextual()) {
            return (T) jackNode.textValue();
        } else if (type == Boolean.class && jackNode.isBoolean()) {
            return (T) Boolean.valueOf(jackNode.booleanValue());
        } else if (type == Integer.class && jackNode.isInt()) {
            return (T) Integer.valueOf(jackNode.intValue());
        } else if (type == Long.class && (jackNode.isInt() || jackNode.isLong())) {
            return (T) Long.valueOf(jackNode.longValue());
        } else if (type == Double.class && jackNode.isNumber() && !jackNode.isBigInteger()) {
            return (T) Double.valueOf(jackNode.doubleValue());
        } else if (type == Float.class && jackNode.isNumber() && !jackNode.isBigInteger()) {
            return (T) Float.valueOf(jackNode.floatValue());
        } else if (type == BigDecimal.class && jackNode.isNumber()) {
            return (T) jackNode.decimalValue();
        }

        return this.jsonHelper.jackson.convertValue(jackNode, type);
    }

    public List<Object> asList() {
        return asList(Object.class);
    }

    public int size() {
        if (!valueNode.isArray()) throw new RuntimeException("最终节点非数组节点");

        return valueNode.getJacksonNode().size();
    }

    @SneakyThrows
    public <T> List<T> asList(Class<T> itemType) {
        if (valueNode.isNull()) return null;

        if (valueNode.isMissingNode()) {
            if (this.nullable) {
                return null;
            } else {
                String missingPath = valueNode.getPath();
                missingPath = missingPath.startsWith(".") ? missingPath : ("." + missingPath);
                throw new NullPointerException(missingPath + " is missing");
            }
        }

        if (valueNode.stream().findAny().isPresent() && valueNode.stream().allMatch(Node::isMissingNode)) {
            if (this.nullable) {
                return valueNode.stream().map(node -> (T) null).collect(Collectors.toList());
            } else {
                String missingPath = valueNode
                    .stream()
                    .map(Node::getPath)
                    .collect(Collectors.joining(","));
                missingPath = (missingPath.startsWith(".") ? "" : ".") + missingPath;
                throw new NullPointerException(missingPath + " is missing");
            }
        }

        if (!valueNode.isArray()) throw new RuntimeException("最终节点非数组节点");

        if (itemType == JSON.class) return this.valueNode.stream()
            .map(item -> (T) new JSON(item.getJacksonNode().deepCopy(), this.jsonHelper.jackson))
            .collect(Collectors.toList());

        ObjectReader reader = this.jsonHelper.jackson.readerFor(itemType);

        ArrayList<T> list = new ArrayList<>();
        var elements = valueNode.getJacksonNode().elements();
        while (elements.hasNext()) {
            JsonNode item = elements.next();
            if (!item.isMissingNode()) list.add(reader.readValue(item));
        }
        return list;
    }

    @SneakyThrows
    public <T> HashMap<String, T> asMap(Class<T> valueType) {
        if (valueNode.isNull()) return null;

        if (valueNode.isMissingNode()) {
            if (this.nullable) {
                return null;
            } else {
                throw new NullPointerException((valueNode.getPath().startsWith(".") ? "" : ".") + valueNode.getPath() + " is missing");
            }
        }

        if (!valueNode.isObject()) throw new RuntimeException("最终节点非对象节点");

        ObjectReader reader = this.jsonHelper.jackson.readerFor(valueType);

        HashMap<String, T> map = new HashMap<>();

        for (Map.Entry<String, JsonNode> entry : valueNode.getJacksonNode().properties()) {
            map.put(entry.getKey(), reader.readValue(entry.getValue()));
        }

        return map;
    }

    public HashMap<String, Object> asMap() {
        return asMap(Object.class);
    }
}

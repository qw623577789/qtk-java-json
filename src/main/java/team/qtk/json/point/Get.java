package team.qtk.json.point;

import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.SneakyThrows;
import team.qtk.json.JSON;
import team.qtk.json.JsonStringifyPrettyPrinter;
import team.qtk.json.node.ArrayNode;
import team.qtk.json.node.Node;
import team.qtk.json.point.Point.DefaultType;
import team.qtk.stream.Stream;

public class Get {

    @Getter
    private Node valueNode;

    private HashMap<Pattern, DefaultType> regexpDefaultValueMapper = new HashMap<>();

    private boolean nullable = false;

    private JSON jsonHelper;

    public Get(JsonNode value, HashMap<String, DefaultType> defaultValueMapper, JSON jsonHelper) {
        this.valueNode = Node.gen(value, ".");
        defaultValueMapper
            .forEach((key, value1) -> this.regexpDefaultValueMapper.put(
                Pattern.compile(
                    "^" +
                        this.escapeExprSpecialWord(key).replaceAll("\\*", "\\d+") +
                        "$"
                ),
                value1
            ));
        this.jsonHelper = jsonHelper;
    }

    public String escapeExprSpecialWord(String keyword) {
        String[] fbsArr = { "\\", "$", "(", ")", "*", "+", ".", "[", "]", "?", "^", "{", "}", "|", "-" };
        for (String key : fbsArr) {
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

        this.valueNode =
            Stream
                .from(
                    Pattern
                        .compile("\\??\\.\".*?\"|\\??\\..*?(?=\\??\\.)|\\??\\..*$") //?????????."????????????"????."????????????"????.????????????.????.????????????
                        .matcher(absouleBreakcrumb)
                        .results()
                        .map(MatchResult::group)
                )
                .reduce(
                    this.valueNode,
                    (valueNode, node) -> {
                        boolean isLastKey = node.index == node.size - 1;

                        String key = node.value.substring(node.value.indexOf(".") + 1).replaceAll("\"", "");

                        boolean hasNullishKey = supportNullishKey && node.value.startsWith("?");

                        // ?????????????????????????????????????????????????????????????????????????????????
                        if (!valueNode.isArray()) valueNode =
                            ArrayNode.create("?????????", this.jsonHelper).add(valueNode);

                        ArrayNode returnNodes =
                            this.getIterValue(key, (ArrayNode) valueNode, toWithDefault, hasNullishKey);

                        if (!isLastKey) { //???????????????key?????????????????????????????????????????????????????????
                            returnNodes
                                .stream()
                                .forEach(
                                    item -> {
                                        boolean isValidNode =
                                            item.isObject() ||
                                            item.isArray() ||
                                            item.isMissingNode() ||
                                            item.isNull();

                                        if (!isValidNode) throw new RuntimeException(
                                            "path:" + item.getPath() + "????????????????????????????????????"
                                        );
                                    }
                                );
                        }

                        //?????????????????????[*],???????????????????????????????????????????????????????????????(?????????????????????????????????????????????)
                        return absouleBreakcrumb.contains("[*]") ? returnNodes : returnNodes.get(0);
                    },
                    (l, r) -> l
                );

        return this;
    }

    private ArrayNode getIterValue(
        String key,
        ArrayNode valueNode,
        boolean toWithDefault,
        boolean hasNullishKey
    ) {
        // ????????????????????????
        boolean isArrayKey = Pattern
            .compile("^(\\[([0-9]+|\\*)])+\\??$") //?????? .[?????????*]{1,n}???????????????
            .matcher(key)
            .matches();

        List<String> keyInfo = Pattern
            .compile("([^\\[|\\]?]+)|(?<=\\[)([0-9]+|\\*)(?=])") //????????????????????????????????????1[????????????2], ??????????????????????????????
            .matcher(key)
            .results()
            .map(MatchResult::group)
            .collect(Collectors.toList());

        // ?????????
        if (isArrayKey) {
            return ArrayNode
                .create("?????????", this.jsonHelper)
                .addAll(this.flatArrayNode(keyInfo, valueNode, toWithDefault, hasNullishKey));
        }

        // ?????????????????????/??????????????????????????????
        String realKey = keyInfo.get(0); //?????????????????????????????????????????????????????????????????????????????????

        List<String> arrayIndexes = keyInfo.subList(1, keyInfo.size());

        return valueNode
            .stream()
            .reduce(
                ArrayNode.create("?????????", this.jsonHelper),
                (collection, node) -> {
                    if (node.isMissingNode() || node.isNull()) {
                        collection.add(node);
                        return collection;
                    }

                    String nodePath = node.getPath();

                    String subNodePath = (nodePath.equals(".") ? "" : nodePath) + "." + realKey;

                    Node subNode = null;

                    if (!node.has(realKey)) { // ???????????????
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

                    if (arrayIndexes.size() == 0) { //??????
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
     *
     * @param arrayIndexes ???????????????????????????
     * @param valueNode ??????
     * @param toWithDefault ???????????????????????????
     * @param hasNullishKey ??????????????????
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
                ArrayNode.create("?????????", this.jsonHelper).add(valueNode), // ?????????????????????
                (prev, arrayIndex) -> {
                    if (arrayIndex.equals("*")) { // ????????????
                        return prev
                            .stream()
                            .reduce(
                                ArrayNode.create("?????????", this.jsonHelper),
                                (collection, node) -> {
                                    String nodePath = node.getPath();

                                    if (!node.isArray()) {
                                        throw new RuntimeException("path:" + nodePath + "?????????????????????");
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
                                ArrayNode.create("?????????", this.jsonHelper),
                                (collection, node) -> {
                                    String nodePath = node.getPath();

                                    String subNodePath =
                                        (nodePath.equals(".") ? "." : nodePath) + "[" + arrayIndex + "]";

                                    if (node.isMissingNode() || node.isNull()) {
                                        collection.add(node);
                                    } else {
                                        if (!node.isArray()) {
                                            throw new RuntimeException("path:" + nodePath + "?????????????????????");
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
        DefaultType defaultType =
            this.regexpDefaultValueMapper.entrySet()
                .stream()
                .filter(entry -> entry.getKey().asPredicate().test(nodePath))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(null);

        if (defaultType == null) return Node.createMissingNode(nodePath);

        Object defaultValue = defaultType.getValue();
        if (defaultValue instanceof Supplier) defaultValue = ((Supplier<?>) defaultValue).get();

        JsonNode jacksonNode;
        if (defaultValue instanceof JSON) {
            jacksonNode = ((JSON) defaultValue).getJacksonNode().deepCopy();
        } else if (defaultValue instanceof JsonNode) {
            jacksonNode = ((JsonNode) defaultValue).deepCopy();
        } else {
            jacksonNode = this.jsonHelper.jackson.valueToTree(defaultValue);
        }

        Node fixNode = Node.gen(jacksonNode, nodePath);

        if (defaultType.isToUpdateNode()) {
            if (node.isObject()) {
                node.set(fieldName, jacksonNode);
            } else if (node.isArray()) {
                ((ArrayNode) node).add(jacksonNode);
            } else {
                throw new RuntimeException(
                    "?????????" + node.getJacksonNode().getNodeType().name() + "???????????????"
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
            return this.jsonHelper.jackson.writer(printer)
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

    public Void asNull() {
        return as(Void.class);
    }

    public BigDecimal asBigDecimal() {
        return as(BigDecimal.class);
    }

    public Float asFloat() {
        return as(Float.class);
    }

    public JSON asJSON() {
        return JSON.parse(this.valueNode.getJacksonNode());
    }

    public <T> T as(Class<T> type) {
        if (type == JSON.class) return (T) asJSON();
        if (this.valueNode.isMissingNode()) {
            if (this.nullable) {
                return null;
            } else {
                throw new NullPointerException(this.valueNode.getPath() + " is missing");
            }
        }
        return this.valueNode.isNull()
            ? null
            : this.jsonHelper.jackson.convertValue(this.valueNode.getJacksonNode(), type);
    }

    public List<Object> asList() {
        return asList(Object.class, true);
    }

    public <T> List<T> asList(Class<T> itemType) {
        return asList(itemType, true);
    }

    public int size() {
        if (!valueNode.isArray()) throw new RuntimeException("???????????????????????????");

        return valueNode.getJacksonNode().size();
    }

    public <T> List<T> asList(Class<T> itemType, boolean ignoreMissingNode) {
        if (valueNode.isNull()) return null;

        if (valueNode.isMissingNode()) {
            if (this.nullable) {
                return null;
            } else {
                String missingPath = valueNode.getPath();
                throw new NullPointerException(missingPath + " is missing");
            }
        }

        if (valueNode.stream().allMatch(Node::isMissingNode)) {
            if (this.nullable) {
                return valueNode.stream().map(node -> (T) null).collect(Collectors.toList());
            } else {
                String missingPath = valueNode
                    .stream()
                    .map(Node::getPath)
                    .collect(Collectors.joining(","));
                throw new NullPointerException(missingPath + " is missing");
            }
        }

        if (!valueNode.isArray()) throw new RuntimeException("???????????????????????????");

        if (itemType == JSON.class) return this.valueNode.stream()
            .map(item -> (T) JSON.parse(item.getJacksonNode()))
            .collect(Collectors.toList());

        ArrayList<T> list = new ArrayList<>();
        valueNode
            .getJacksonNode()
            .elements()
            .forEachRemaining(
                item -> {
                    if (!item.isMissingNode()) list.add(this.jsonHelper.jackson.convertValue(item, itemType));
                }
            );
        return list;
    }

    public <T> HashMap<String, T> asMap(Class<T> valueType) {
        if (valueNode.isNull()) return null;

        if (valueNode.isMissingNode()) {
            if (this.nullable) {
                return null;
            } else {
                throw new NullPointerException(valueNode.getPath() + " is missing");
            }
        }

        if (!valueNode.isObject()) throw new RuntimeException("???????????????????????????");

        HashMap<String, T> map = new HashMap<>();

        valueNode
            .getJacksonNode()
            .fields()
            .forEachRemaining(
                entry ->
                    map.put(entry.getKey(), this.jsonHelper.jackson.convertValue(entry.getValue(), valueType))
            );

        return map;
    }

    public HashMap<String, Object> asMap() {
        return asMap(Object.class);
    }
}

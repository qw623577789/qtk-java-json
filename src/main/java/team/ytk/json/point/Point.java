package team.ytk.json.point;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import team.ytk.json.JSON;
import team.ytk.json.node.Node;

public class Point {

    private String breakcrumb = "";

    private String point;

    private JsonNode instance;

    private JSON jsonHelper;

    private static ObjectMapper jackson = new ObjectMapper();

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

    public Get get(boolean toWithDefault, boolean supportNullishKey, boolean nullable) {
        return new Get(this.instance, this.defaultValueMapper)
        .get(this.breakcrumb, this.point, toWithDefault, supportNullishKey, nullable);
    }

    public Get get() {
        return get(true, true, false);
    }

    public Get get(boolean nullable) {
        return get(true, true, nullable);
    }

    public JSON put(String id, Object value) {
        return put(id, jackson.valueToTree(value));
    }

    public JSON put(String id, JSON value) {
        return put(id, value.getJacksonNode());
    }

    public JSON put(String id, JsonNode value) {
        String absouleBreakcrumb = this.breakcrumb + this.point;

        Get pointValue = get(false, false, false);

        if (absouleBreakcrumb.contains("[*]")) {
            pointValue
                .getValueNode()
                .stream()
                .forEach(
                    operaNode -> {
                        operaNode.set(id, value.deepCopy());
                    }
                );
        } else {
            Node operaNode = pointValue.getValueNode();
            operaNode.set(id, value.deepCopy());
        }

        return this.jsonHelper;
    }

    public boolean has() {
        return has(true);
    }

    public boolean has(boolean toWithDefault) {
        Get result = get(toWithDefault, false, false);
        return result.isArray()
            ? (
                result.size() == 0 //空数组也算has
                    ? true
                    : result.getValueNode().stream().anyMatch(item -> !item.isMissingNode())
            )
            : !result.getValueNode().isMissingNode();
    }

    public JSON delete() {
        String absouleBreakcrumb = this.breakcrumb + this.point;

        List<String> keys = Pattern
            .compile("\\.\".*?\"|\\..*?(?=\\.)|\\..*$") //匹配　."匹配内容"、.匹配内容.、.匹配内容
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
        if (parentPointKey.equals("")) parentPointKey = ".";

        Get parentNode = new Get(this.instance, this.defaultValueMapper);
        parentNode.get("", parentPointKey, true, false, false);

        String finalLastPointKey = lastPointKey;

        if (parentPointKey.contains("[*]")) {
            final boolean isArrayKeyFinally = isArrayKey;
            parentNode
                .getValueNode()
                .stream()
                .forEach(
                    operaNode -> {
                        if (isArrayKeyFinally) {
                            ((team.ytk.json.node.ArrayNode) operaNode).remove(
                                    Integer.parseInt(finalLastPointKey)
                                );
                        } else {
                            operaNode.remove(finalLastPointKey);
                        }
                    }
                );
        } else {
            Node operaNode = parentNode.getValueNode();
            if (isArrayKey) {
                ((team.ytk.json.node.ArrayNode) operaNode).remove(Integer.parseInt(finalLastPointKey));
            } else {
                operaNode.remove(finalLastPointKey);
            }
        }

        return this.jsonHelper;
    }

    public JSON add(Object... items) {
        Get pointValue = get(false, false, false);

        String absouleBreakcrumb = this.breakcrumb + this.point;

        if (absouleBreakcrumb.contains("[*]")) {
            pointValue
                .getValueNode()
                .stream()
                .forEach(
                    i -> {
                        team.ytk.json.node.ArrayNode operaNode = (team.ytk.json.node.ArrayNode) i;
                        Arrays
                            .asList(items)
                            .stream()
                            .forEach(
                                item -> {
                                    if (item instanceof JSON) {
                                        operaNode.add((((JSON) item).getJacksonNode()).deepCopy());
                                    } else if (item instanceof JsonNode) {
                                        operaNode.add(((JsonNode) item).deepCopy());
                                    } else {
                                        operaNode.add(jackson.valueToTree(item));
                                    }
                                }
                            );
                    }
                );
        } else {
            team.ytk.json.node.ArrayNode operaNode = (team.ytk.json.node.ArrayNode) pointValue.getValueNode();
            Arrays
                .asList(items)
                .stream()
                .forEach(
                    item -> {
                        if (item instanceof JSON) {
                            operaNode.add((((JSON) item).getJacksonNode()).deepCopy());
                        } else if (item instanceof JsonNode) {
                            operaNode.add(((JsonNode) item).deepCopy());
                        } else {
                            operaNode.add(jackson.valueToTree(item));
                        }
                    }
                );
        }

        return this.jsonHelper;
    }

    public JSON concat(List<Object> list) {
        return add(list.toArray());
    }

    public Point defaultValue(Supplier<Object> defaultValueFunc) {
        return defaultValue((Object) defaultValueFunc);
    }

    public Point defaultValue(Object defaultValue) {
        DefaultType def = DefaultType.builder().value(defaultValue).build();
        this.defaultValueMapper.put((this.breakcrumb + this.point).replaceAll("\"", ""), def);
        return this;
    }

    // default的point必须为point本体子集
    public Point defaultValue(HashMap<String, ?> defaultValueMap) {
        defaultValueMap
            .entrySet()
            .stream()
            .forEach(
                item -> {
                    String pointPath = item.getKey();
                    if (!this.point.replaceAll("\\?", "").startsWith(pointPath)) {
                        throw new RuntimeException(
                            "point:" + pointPath + "必须为point:" + this.point + "子集"
                        );
                    }
                    String absolutePath = (this.breakcrumb + pointPath).replaceAll("\"", "");
                    this.defaultValueMapper.put(
                            absolutePath,
                            DefaultType.builder().value(item.getValue()).build()
                        );
                }
            );
        return this;
    }

    public String toString() {
        return this.instance.toString();
    }

    @Builder
    @Data
    public static class DefaultType {

        private Object value;
    }
}
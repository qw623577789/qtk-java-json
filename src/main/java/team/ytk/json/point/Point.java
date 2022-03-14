package team.ytk.json.point;

import com.fasterxml.jackson.databind.JsonNode;
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
        String newBreakcrumb = this.breakcrumb + this.point;
        return new Point(
            point,
            newBreakcrumb.equals(".") ? "" : newBreakcrumb,
            this.defaultValueMapper,
            this.instance,
            this.jsonHelper
        );
    }

    public Get get(boolean toWithDefault, boolean supportNullishKey, boolean nullable) {
        return new Get(this.instance, this.defaultValueMapper, this.jsonHelper)
        .get(this.breakcrumb, this.point, toWithDefault, supportNullishKey, nullable);
    }

    public Get get() {
        return get(true, true, false);
    }

    public Get get(boolean nullable) {
        return get(true, true, nullable);
    }

    public JSON put(String id, Object value) {
        JsonNode jacksonNode;
        if (value instanceof JSON) {
            jacksonNode = ((JSON) value).getJacksonNode();
        } else if (value instanceof JsonNode) {
            jacksonNode = (JsonNode) value;
        } else {
            jacksonNode = jsonHelper.jackson.valueToTree(value);
        }

        String absouleBreakcrumb = this.breakcrumb + this.point;

        Get pointValue = get(false, false, false);

        if (absouleBreakcrumb.contains("[*]")) {
            pointValue
                .getValueNode()
                .stream()
                .forEach(
                    operaNode -> {
                        operaNode.set(id, jacksonNode.deepCopy());
                    }
                );
        } else {
            Node operaNode = pointValue.getValueNode();
            operaNode.set(id, jacksonNode.deepCopy());
        }

        return this.jsonHelper;
    }

    public JSON put(Object value) {
        JsonNode jacksonNode;
        if (value instanceof JSON) {
            jacksonNode = ((JSON) value).getJacksonNode();
        } else if (value instanceof JsonNode) {
            jacksonNode = (JsonNode) value;
        } else {
            jacksonNode = jsonHelper.jackson.valueToTree(value);
        }

        String absouleBreakcrumb = this.breakcrumb + this.point;

        Object[] parentKeyInfo = this.getParentPointKey(absouleBreakcrumb);
        String parentPointKey = (String) parentKeyInfo[0];
        String lastPointKey = (String) parentKeyInfo[1];

        Get parentNode = new Get(this.instance, this.defaultValueMapper, this.jsonHelper);
        parentNode.get("", parentPointKey, true, false, false);

        if (absouleBreakcrumb.contains("[*]")) {
            parentNode
                .getValueNode()
                .stream()
                .forEach(
                    operaNode -> {
                        operaNode.set(lastPointKey, jacksonNode.deepCopy());
                    }
                );
        } else {
            Node operaNode = parentNode.getValueNode();
            operaNode.set(lastPointKey, jacksonNode.deepCopy());
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

    private Object[] getParentPointKey(String absouleBreakcrumb) {
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

        return new Object[] { parentPointKey, lastPointKey, isArrayKey };
    }

    public JSON delete() {
        String absouleBreakcrumb = this.breakcrumb + this.point;

        Object[] parentKeyInfo = this.getParentPointKey(absouleBreakcrumb);
        String parentPointKey = (String) parentKeyInfo[0];
        String lastPointKey = (String) parentKeyInfo[1];

        // 是否为纯数组节点
        boolean isArrayKey = (boolean) parentKeyInfo[2];

        String finalLastPointKey = lastPointKey;

        Get parentNode = new Get(this.instance, this.defaultValueMapper, this.jsonHelper);
        parentNode.get("", parentPointKey, true, false, false);

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
                                        operaNode.add(jsonHelper.jackson.valueToTree(item));
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
                            operaNode.add(jsonHelper.jackson.valueToTree(item));
                        }
                    }
                );
        }

        return this.jsonHelper;
    }

    public JSON concat(List<Object> list) {
        return add(list.toArray());
    }

    public Point defaultValue(Supplier<Object> defaultValueFunc, boolean toUpdateNode) {
        return defaultValue((Object) defaultValueFunc, toUpdateNode);
    }

    public Point defaultValue(Supplier<Object> defaultValueFunc) {
        return defaultValue((Object) defaultValueFunc, false);
    }

    // default的point必须为point本体子集
    public Point defaultValue(DefaultValueMap defaultValueMap) {
        return defaultValue(defaultValueMap, false);
    }

    public Point defaultValue(Object defaultValue) {
        return defaultValue(defaultValue, false);
    }

    public Point defaultValue(Object defaultValue, boolean toUpdateNode) {
        DefaultType def = DefaultType.builder().value(defaultValue).toUpdateNode(toUpdateNode).build();
        this.defaultValueMapper.put((this.breakcrumb + this.point).replaceAll("\"", ""), def);
        return this;
    }

    // default的point必须为point本体子集
    public Point defaultValue(DefaultValueMap defaultValueMap, boolean toUpdateNode) {
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
                            DefaultType.builder().value(item.getValue()).toUpdateNode(toUpdateNode).build()
                        );
                }
            );
        return this;
    }

    /**
     * 获取Point所属的JSON实例
     * @return
     */
    public JSON backToJSON() {
        return this.jsonHelper;
    }

    @Builder
    @Data
    public static class DefaultType {

        private Object value;

        @Builder.Default
        private boolean toUpdateNode = false;
    }

    public static class DefaultValueMap extends HashMap<String, Object> {}
}

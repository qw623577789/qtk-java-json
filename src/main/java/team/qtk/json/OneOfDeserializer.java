package team.qtk.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import lombok.SneakyThrows;
import team.qtk.json.node.QOneOf;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OneOfDeserializer extends StdDeserializer<QOneOf> implements ContextualDeserializer {

    private final Constructor<?> constructor;

    public OneOfDeserializer() {
        super(QOneOf.class);
        this.constructor = null;
    }

    // 带类型的构造函数（用于 createContextual），构造器随实例缓存，避免每次反序列化都反射查找
    private OneOfDeserializer(JavaType targetType) {
        super(QOneOf.class);
        this.constructor = getConstructor(targetType.getRawClass());
    }

    @SneakyThrows
    private static Constructor<?> getConstructor(Class<?> targetClass) {
        return targetClass.getConstructor();
    }

    /*
    利用此函数捕获OneOf类名
     */
    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        JavaType type = ctxt.getContextualType();
        if (type == null) type = property.getType();
        // 返回新的反序列化器实例，携带实际类型信息
        return new OneOfDeserializer(type);
    }

    /**
     * 处理非null情况下赋值
     *
     * @param p    Parser used for reading JSON content
     * @param ctxt Context that can be used to access information about
     *             this deserialization activity.
     * @return
     */
    @Override
    @SneakyThrows
    public QOneOf deserialize(JsonParser p, DeserializationContext ctxt) {
        QOneOf instance = (QOneOf) constructor.newInstance();
        JsonNode node = p.getCodec().readTree(p);
        instance.value = convertJsonNode(node);
//        instance.value = parseValueStream(p, ctxt);
        return instance;
    }

    /**
     * 处理显式赋null
     *
     * @param ctxt
     * @return
     */
    @Override
    @SneakyThrows
    public QOneOf getNullValue(DeserializationContext ctxt) {
        QOneOf instance = (QOneOf) constructor.newInstance();
        instance.value = null;
        return instance;
    }

    // 递归转换 JsonNode 为 Java 对象
    private Object convertJsonNode(JsonNode node) {
        if (node.isObject()) {
            Map<String, Object> map = new HashMap<>(node.size());
            for (Map.Entry<String, JsonNode> entry : node.properties()) {
                map.put(entry.getKey(), convertJsonNode(entry.getValue()));
            }
            return map;
        } else if (node.isArray()) {
            List<Object> list = new ArrayList<>(node.size());
            node.forEach(item -> list.add(convertJsonNode(item)));
            return list;
        } else if (node.isLong()) {
            return node.longValue();
        } else if (node.isInt()) {
            return node.longValue();
        } else if (node.isNumber()) {
            return node.decimalValue();
        } else if (node.isBoolean()) {
            return node.asBoolean();
        } else if (node.isTextual()) {
            return node.asText();
        } else if (node.isNull()) {
            return null;
        }
        return node.asText();
    }

}

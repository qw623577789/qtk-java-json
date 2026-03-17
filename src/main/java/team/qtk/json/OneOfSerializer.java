package team.qtk.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import team.qtk.json.node.QOneOf;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/*
 *
 * QOneOf序列化控制器，但是这个类有bug：已经进入序列化是没法跳过这个字段了，所以这段实现不对
 * 该类仅供参考
 */
@Deprecated
public class OneOfSerializer extends StdSerializer<QOneOf> implements ContextualSerializer {

    private final BeanProperty property;
    private final JsonInclude.Include includeStrategy;

    // 默认构造函数
    public OneOfSerializer() {
        super(QOneOf.class);
        this.property = null;
        this.includeStrategy = null;
    }

    // 带属性的构造函数（createContextual 调用）
    private OneOfSerializer(BeanProperty property, JsonInclude.Include includeStrategy) {
        super(QOneOf.class);
        this.property = property;
        this.includeStrategy = includeStrategy;
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        // 获取字段上的 @JsonInclude 注解
        JsonInclude.Include strategy = null;
        if (property != null) {
            JsonInclude annotation = property.getAnnotation(JsonInclude.class);
            if (annotation != null) {
                strategy = annotation.value();
            }

            // 如果没有注解，检查字段所属类的配置
            if (strategy == null && property.getMember() != null) {
                var jsonInclude = property.getMember().getAnnotation(JsonInclude.class);
                if (jsonInclude != null) {
                    strategy = jsonInclude.value();
                }
            }
        }

        // 默认策略：NON_NULL
        if (strategy == null) {
            strategy = JsonInclude.Include.NON_NULL;
        }

        return new OneOfSerializer(property, strategy);
    }

    @Override
    public void serialize(QOneOf value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        // 绝大多数情况，value不会为null，有key的情况因为反序列化器OneOfDeserializer已经设置显式值为null时，也会转为对象；没存在key情况则是schema生成类会赋默认值
        if (value == null) {
            gen.writeNull();
            return;
        }

        // 根据 @JsonInclude 策略决定是否序列化
        if (shouldSkip(value)) {
            // TODO已经进入序列化是没法跳过这个字段了，所以这段实现不对
            provider.defaultSerializeValue(value.value, gen);
            return;
        }

        // 序列化 value 字段的内容
        Object fieldValue = value.value;
        if (fieldValue == null) {
            gen.writeNull();
        } else if (fieldValue instanceof Map) {
            gen.writeObject(fieldValue);
        } else if (fieldValue instanceof List) {
            gen.writeObject(fieldValue);
        } else {
            gen.writeObject(fieldValue);
        }
    }

    private boolean shouldSkip(QOneOf value) {
        if (includeStrategy == null) {
            return false;
        }

        return switch (includeStrategy) {
            case ALWAYS ->
                // 总是序列化，即使 value 为 null
                false;
            case NON_NULL ->
                // value 为 null 时跳过
                value.value == null;
            case NON_EMPTY ->
                // value 为 null 或空集合/空字符串时跳过
                empty(value.value);
            case NON_ABSENT ->
                // Optional 相关，这里简化处理为 NON_NULL
                value.value == null;
            case NON_DEFAULT ->
                // 与默认值比较，这里简化处理为 NON_NULL
                value.value == null;
            default -> false;
        };
    }

    // 判断是否为空值
    private boolean empty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof Map) {
            return ((Map<?, ?>) value).isEmpty();
        }
        if (value instanceof Collection) {
            return ((Collection<?>) value).isEmpty();
        }
        if (value instanceof String) {
            return ((String) value).isEmpty();
        }
        if (value.getClass().isArray()) {
            return java.lang.reflect.Array.getLength(value) == 0;
        }
        return false;
    }
}

package team.qtk.json.node;

import com.fasterxml.jackson.annotation.JsonIgnore;
import team.qtk.json.JSON;
import team.qtk.json.point.Point;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Predicate;

public class QOneOf<T extends QOneOf> {
    public Object value;

    @JsonIgnore
    private JSON jsonValue;

    public void jacksonInject(Object value) {
        this.value = value;
    }

    /**
     * 序列化统一对value操作
     * 获取可变类的内部值
     **/
    @com.fasterxml.jackson.annotation.JsonValue
    public Object getRawValue() {

        if (
            value instanceof Long ||
                value instanceof Integer ||
                value instanceof BigDecimal ||
                value instanceof Boolean ||
                value instanceof String ||
                value instanceof List<?>
        ) {
            return value;
        }
        //值为null且没有其他值，可断定为刚初始化，按规则为null
        else if (value == null && this.getOtherFields().isEmpty()) {
            return null;
        } else {
            //对象类型下，可能getOtherFields有额外字段，JsonValue注解优先级高于JsonAnyGetter
            // 这里做个转换补充
            var mapValue = new LinkedHashMap<String, Object>();
            mapValue.putAll(this.getOtherFields());
            if (value != null) {
                mapValue.putAll(JSON.parse(value).getAs(LinkedHashMap.class));
            }
            return mapValue;
        }
    }

    public String toString() {
        return JSON.parse(this).toString();
    }

    public boolean isBoolean() {
        return this.value != null && this.value instanceof Boolean;
    }

    public boolean isInteger() {
        return this.value != null && this.value instanceof Long;
    }

    public boolean isNumber() {
        return this.value != null && this.value instanceof BigDecimal;
    }

    public boolean isString() {
        return this.value != null && this.value instanceof String;
    }

    public boolean isNull() {
        return this.value == null && this.getOtherFields().isEmpty();
    }

    public boolean isObject() {
        return this.value != null && this.value instanceof LinkedHashMap;
    }

    public boolean isArray() {
        return this.value != null && this.value instanceof ArrayList;
    }

    public boolean assertObject(String point, Predicate<Point> pointer) {
        if (!isObject()) return false;
        if (jsonValue == null) jsonValue = JSON.parse(this.value);
        return pointer.test(jsonValue.point(point));
    }

    public boolean assertObjectPointHas(String point) {
        return assertObject(point, Point::has);
    }

    public boolean assertObjectPointValueEquals(String point, Object value) {
        return assertObject(point, pointer -> pointer.get(true).as(value.getClass()).equals(value));
    }

    //给对象设置额外字段用的
    @com.fasterxml.jackson.annotation.JsonIgnore
    final java.util.Map<String, Object> unknownFields = new java.util.HashMap<>();

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    public java.util.Map<String, Object> getOtherFields() {
        return unknownFields;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public team.qtk.json.point.Point getOtherFields(String jsonPoint) {
        return team.qtk.json.JSON.parse(unknownFields).point(jsonPoint);
    }

    /**
     * 给未定义的字段赋值，序列化JSON后可见
     * 若通过本方法给已定义的字段赋值，对象状态下可分别读取到两个值，
     * 序列化后，通过setOtherField设置的值拥有最高级别，会覆盖掉setDefinedField的值
     */
    @com.fasterxml.jackson.annotation.JsonAnySetter
    public T setOtherField(String name, Object value) {
        unknownFields.put(name, value);
        return (T) this;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public T setOtherField(java.util.Map<String, Object> keyValues) {
        unknownFields.putAll(keyValues);
        return (T) this;
    }

    public <TT> TT to(Class<TT> transforClass) {
        return JSON.parse(this).getAs(transforClass);
    }

    public T clone() {
        return (T) JSON.parse(this).deepCopy().getAs(this.getClass());
    }

}

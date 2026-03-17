package team.qtk.json.node;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import team.qtk.json.JSON;
import team.qtk.json.OneOfDeserializer;
import team.qtk.json.point.Point;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.function.Predicate;

//@JsonSerialize(using = OneOfSerializer.class)
@JsonDeserialize(using = OneOfDeserializer.class)

public class QOneOf<T extends QOneOf> {
    public Object value;

    @JsonIgnore
    private JSON jsonValue;

    /**
     * 序列化统一对value操作
     * 获取可变类的内部值
     **/
    @com.fasterxml.jackson.annotation.JsonValue
    public Object getRawValue() {
        return value;
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
        return this.value == null;
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

    public <TT> TT to(Class<TT> transforClass) {
        return JSON.parse(this).getAs(transforClass);
    }

    public T clone() {
        return (T) JSON.parse(this).deepCopy().getAs(this.getClass());
    }

}

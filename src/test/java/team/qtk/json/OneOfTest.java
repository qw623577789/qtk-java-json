package team.qtk.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import team.qtk.json.node.QOneOf;

import static org.junit.jupiter.api.Assertions.*;

public class OneOfTest {

    //    @JsonDeserialize(using = A.ADeserializer.class)
    @Data
    @EqualsAndHashCode(callSuper = false)
    @NoArgsConstructor
    public static class A extends QOneOf<A> {

//        @com.fasterxml.jackson.annotation.JsonCreator()
//        public static A jacksonFill(@JsonProperty("value") Object value) {
//            return new A(value);
//        }
//
//        private A(Object value) {
//            this.value = value;
//        }

//        public static class ADeserializer extends StdDeserializer<A> {
//            public ADeserializer() {
//                super(A.class);
//            }
//
//            @Override
//            public A deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
//                if (p.getCurrentToken() == JsonToken.VALUE_NULL) {
//                    return new A();
//                }
//                return p.readValueAs(A.class);
//            }
//        }
    }

    @Test
    void main() {
//        var i = JSON.parse(
////            Map.of("a1", "b1")
//            null
////            List.of("1", "2")
////            1L
////            "[1,2]"
////            "{\"a\":1}"
//        ).getAs(A.class);
//        i.setOtherField("a12", Map.of("a2", "b2"));
//
//        var ii = i.clone();
////        var array = i.to(HashMap.class);
//        System.out.println(i);

        //root
        var objRoot = JSON.parse("{\"c\":{\"d\":1}}").getAs(A.class);
        assertNotNull(objRoot);
        assertFalse(JSON.parse(objRoot).toString().contains("null"));

        var kobjRoot = JSON.parse("{}").getAs(A.class);
        assertNotNull(kobjRoot);
        assertFalse(JSON.parse(kobjRoot).toString().contains("null"));
//
        var numberRoot = JSON.parse("11").getAs(A.class);
        assertNotNull(numberRoot);
        assertFalse(JSON.parse(numberRoot).toString().contains("null"));

        var arrayRoot = JSON.parse("[22]").getAs(A.class);
        assertNotNull(arrayRoot);
        assertFalse(JSON.parse(arrayRoot).toString().contains("null"));

        var 显式nullRoot = JSON.parse(null).getAs(A.class);
        assertTrue(
            显式nullRoot != null && 显式nullRoot.isNull()
        );
        // property
        var obj = JSON.parse("{\"always\":{\"c\":{\"d\":1}},\"nonNull\":{\"c\":{\"d\":1}}}").getAs(B.class);
        assertTrue(obj.getAlways() != null && obj.getNonNull() != null);
        assertTrue(JSON.parse(obj).toString().contains("always") && JSON.parse(obj).toString().contains("nonNull"));
//
        var number = JSON.parse("{\"always\":11,\"nonNull\":22}").getAs(B.class);
        assertTrue(number.getAlways() != null && number.getNonNull() != null);
        assertTrue(JSON.parse(number).toString().contains("always") && JSON.parse(number).toString().contains("nonNull"));

        var array = JSON.parse("{\"always\":[22],\"nonNull\":[22]}").getAs(B.class);
        assertTrue(array.getAlways() != null && array.getNonNull() != null);
        assertTrue(JSON.parse(array).toString().contains("always") && JSON.parse(array).toString().contains("nonNull"));

        var kobj = JSON.parse("{\"always\":{},\"nonNull\":{}}").getAs(B.class);
        assertTrue(kobj.getAlways() != null && kobj.getNonNull() != null);
        assertTrue(JSON.parse(kobj).toString().contains("always") && JSON.parse(kobj).toString().contains("nonNull"));

        var 显式null = JSON.parse("{\"always\":null,\"nonNull\":null}").getAs(B.class);
        assertTrue(
            显式null.getAlways() != null &&
                显式null.getNonNull() != null &&
                显式null.getAlways().isNull() &&
                显式null.getNonNull().isNull()
        );
        assertTrue(JSON.parse(显式null).toString().contains("always") && !JSON.parse(显式null).toString().contains("nonNull"));

        var 隐式null = JSON.parse("{}").getAs(B.class);
        assertTrue(
            隐式null.getAlways() != null &&
                隐式null.getNonNull() != null &&
                隐式null.getAlways().isNull() &&
                隐式null.getNonNull().isNull()
        );
        assertTrue(JSON.parse(隐式null).toString().contains("always") && !JSON.parse(隐式null).toString().contains("nonNull"));

    }

    @Data
    @NoArgsConstructor
    public static class B {
        @com.fasterxml.jackson.annotation.JsonInclude(JsonInclude.Include.ALWAYS)
        private A always = new A();

        @com.fasterxml.jackson.annotation.JsonInclude(
            value = com.fasterxml.jackson.annotation.JsonInclude.Include.CUSTOM,
            valueFilter = team.qtk.json.OneOfSerializerFilter.class
        )
        private A nonNull = new A();
    }
}

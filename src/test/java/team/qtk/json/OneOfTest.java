package team.qtk.json;

import team.qtk.json.node.QOneOf;

import java.util.Map;

public class OneOfTest {

    public static class A extends QOneOf<A> {
//        @com.fasterxml.jackson.annotation.JsonCreator
//        public static A jacksonFill(Object value) {
//            return new A(value);
//        }
//
//        private A(Object value) {
//            this.value = value;
//        }
    }

    public static void main(String[] args) {
        var i = JSON.parse(
//            Map.of("a1", "b1")
            null
//            List.of("1", "2")
//            1L
//            "[1,2]"
//            "{\"a\":1}"
        ).getAs(A.class);
        i.setOtherField("a12", Map.of("a2", "b2"));

        var ii = i.clone();
//        var array = i.to(HashMap.class);
        System.out.println(i);
    }
}

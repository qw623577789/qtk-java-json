package team.qtk.json;

import team.qtk.json.node.QOneOf;

/*
由于oneof类永远不为null，故
@JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
是无法实现当为null时过滤字段的
需借助@JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.CUSTOM, team.qtk.json.OneOfSerializerFilter.class)实现
 */
public class OneOfSerializerFilter {

    public boolean equals(Object obj) {
        var oneOf = (QOneOf) obj;
        return oneOf.isNull();
    }
}

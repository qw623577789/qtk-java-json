YTK-Json
================
基于**com.fasterxml.jackson**二次封装的JSON操作库,相比于原库,提供更加友好方便的JSON操作体验,更支持使用**JSON POINT**操作节点及类似Node.js ES2020可选链功能

## Installation

```groovy
//gradle
repositories {
    ...
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.qw623577789:ytk-json:v1.0.0'
}
```

## Features
- 超级方便的JSON节点操作函数, 支持链式操作，丝滑般开发体验，具体见下面详细说明
- 支持ES2020特性的可选链JSON POINT
- 友好的错误提示，当节点路径不存在时，报错可提示具体节点路径(节点遍历也可以支持)
## Function

- **JSON new JSON(boolean isObject)** 创建一个JSON实例, true/false控制创建出来是**JSON对象**还是**JSON数组**
- **JSON new JSON(JsonNode jacksonNode)** 将com.fasterxml.jackson的``JsonNode``转化为JSON实例
- **JSON parse(Object object)** 可将大部分Java对象转换为JSON实例
- **String toString()** 将JSON实例转换为JSON字符串
- **JSON deepCopy()** 深拷贝JSON实例
- **JsonNode getJacksonNode()** 将JSON实例转换为com.fasterxml.jackson的``JsonNode``
- **JSON sPut(String id, Object value)** *静态方法*,　用于创建**JSON对象实例**,并设置key/value, value支持*大部分Java对象*及*JSON实例*
- **JSON put(String id, Object value)** 在**JSON对象实例**上设置key/value, value支持*大部分Java对象*及*JSON实例*
- **JSON sAdd(Object ...value)** *静态方法*,　用于创建**JSON数组实例**,并一次性添加无限个元素, value支持*大部分Java对象*及*JSON实例*
- **JSON add(Object ...value)** 在**JSON数组实例**上一次性添加无限个元素, value支持*大部分Java对象*及*JSON实例*
- **JSON concat(List<?> list)** 在**JSON数组实例**上添加存于``List``的元素，value支持*大部分Java对象*及*JSON实例*
- **Point point(String point)** 根据JSON路径(例如 \.aaa.bbb[0].ccc[*].ddd)返回一个``Point``对象，用于对节点进行``get``、``put``、``delete``、``has``、``add``、``concat``操作
- **Point point(String point, Object defaultValue)** 在**Point point(String point)**基础上，在``get``、``has``操作时，若**节点不存在**时．返回默认值
- **Point point(String point, Supplier\<Object\> defaultValueFunc)** 在**Point point(String point)**基础上，在``get``、``has``操作时，若**节点不存在**时．执行函数返回默认值
- **Point point(String point, HashMap<String, Object> defaultValueMap)** 在**Point point(String point)**基础上，可为``第一个参数的point``路径批量设置默认值(point=".aaa.bbb.ccc", 可先设置.aaa = 对象，再设置.aaa.bbb = 对象)，在``get``、``has``操作时，若**节点不存在**时．**执行函数/直接**返回默认值
- Point操作
    - **Get get()** 返回``Get``实例，根据point获取节点值，有``asString()``、``asXXXX()``等方法最终得到值(开启**默认值、可选链特性**)
        - **String asString()**、**Long asLong()**、**Integer asInt()**、**Boolean asBoolean()**、**Double asDouble()**、**Void asNull()**、**BigDecimal asBigDecimal()**、**Float asFloat()**、**\<T\> T as(Class\<T\> type)**、**List\<Object\> asList**、**\<T\> List\<T\> asList(Class\<T\> itemType)**、**List\<T\> asList(Class\<T\> itemType, boolean ignoreMissingNode)**、**HashMap\<String, T\> asMap(Class\<T\> valueType)**、**HashMap\<String, Object\> asMap()**、**size()**
    - **Get get(boolean toWithDefault, boolean supportNullishKey, boolean nullable)** 在上get方法基础上，可以控制**是否开启默认值**、**是否开启可选链**特性, ``nullable``控制当最终结果不存在时，**是否返回null，否则在没有可选链标注情况下，会抛空指针错误**,*此属性等同于给point每个节点加上可选链标志*
    - **JSON put(String id, Object value)** 给对应point的JSON对象节点赋值，支持**大部分JAVA对象**、**JSON实例**、**com.fasterxml.jackson的``JsonNode``**
    - **JSON add(Object... items)** 给对应point的JSON数组节点添加元素，支持**大部分JAVA对象**、**JSON实例**、**com.fasterxml.jackson的``JsonNode``**
    - **JSON concat(List\<Object\> list)** 给对应point的JSON数组节点添加元素，支持**大部分JAVA对象**、**JSON实例**、**com.fasterxml.jackson的``JsonNode``**
    - **boolean has()** 判断point对应节点值有没有存在(*默认会开启默认值特性*)
    - **boolean has(boolean toWithDefault)** 判断point对应节点值有没有存在,可设定启不启用*默认值特性*
    - **JSON delete()** 删除point节点的值
    - **Point defaultValue(Object defaultValue)/Point defaultValue(HashMap\<String, Object\> defaultValueMap)** 设置节点默认值
    - **Point point(String point)** 在原来Point上在延伸point, 例如: point(".aaa.bbb")等同于point(".aaa").point(".bbb"),此特性用于支持point节点分级


## Usage

更多的样例请看测试用例

### 对象构建
```java
String json = JSON
    .sPut("int", 1)
    .put("string", "2")
    .put("float", 2.5f)
    .put("double", 2.5d)
    .put("BigDecimal", BigDecimal.valueOf(1))
    .put("boolean", false)
    .put("null", null)
    .put(
        "map",
        new HashMap<String, String>() {
            {
                put("a", "1");
                put("b", "2");
            }
        }
    )
    .put("JSON.Map", JSON.sPut("m1", "1").put("m2", "2"))
    .put(
        "List",
        new ArrayList<String>() {
            {
                add("1");
                add("2");
            }
        }
    )
    .put(
        "JSON.List",
        JSON
            .sAdd(1)
            .add(2, 4)
            .add(5)
            .add(
                new ArrayList<Integer>() {
                    {
                        add(6);
                        add(7);
                    }
                }
            )
    )
    .toString();
```
```json
{
    "int": 1,
    "string": "2",
    "float": 2.5,
    "double": 2.5,
    "BigDecimal": 1,
    "boolean": false,
    "null": null,
    "map": {
        "a": "1",
        "b": "2"
    },
    "JSON.Map": {
        "m1": "1",
        "m2": "2"
    },
    "List": [
        "1",
        "2"
    ],
    "JSON.List": [1, 2, 4, 5, [ 6, 7 ]]
}
```

### 数组构建
```java
String json = JSON
    .sAdd(
        1,
        3,
        new ArrayList<Integer>() {
            {
                add(6);
                add(7);
            }
        }
    )
    .add(2, 4)
    .add(5)
    .add(
        new ArrayList<Integer>() {
            {
                add(6);
                add(7);
            }
        }
    )
    .add((Object) null)
    .add(
        JSON
            .sPut("array.map.int", 1)
            .put(
                "array.map.map",
                new HashMap<String, String>() {
                    {
                        put("m1", "1");
                    }
                }
            )
            .put(
                "array.map.array",
                new ArrayList<String>() {
                    {
                        add("1");
                    }
                }
            )
    )
    .concat(
        new ArrayList<String>() {
            {
                add("concat.1");
            }
        }
    )
    .toString();
```

```json
[
    1,
    3,
    [
        6,
        7
    ],
    2,
    4,
    5,
    [
        6,
        7
    ],
    null,
    {
        "array.map.int": 1,
        "array.map.map": {
            "m1": "1"
        },
        "array.map.array": [
            "1"
        ]
    },
    "concat.1"
]

```

### Point.get
现json如下
```json
{
    "int": 1,
    "string": "2",
    "float": 2.5,
    "double": 2.5,
    "BigDecimal": 1,
    "boolean": false,
    "null": null,
    "map": {
        "a": "1",
        "b": "2"
    },
    "JSON.Map": {
        "m1": "1",
        "m2": "2"
    },
    "List": [
        "1",
        "2"
    ],
    "ListMap": [
        {
            "id1": "1",
            "id2": "2",
            "id3": [
                [
                    {
                        "id11": "1",
                        "id22": "2",
                        "id33": {
                            "id333": "value1"
                        }
                    }
                ]
            ]
        },
        {
            "id1": "11",
            "id2": "22",
            "id3": [
                [
                    {
                        "id11": "11",
                        "id22": "22",
                        "id33": {
                            "id333": "value2"
                        }
                    }
                ]
            ]
        }
    ],
    "JSON.List": [
        1,
        2,
        4,
        5,
        [
            6,
            7
        ]
    ]
}
```

```java
Assertions.assertEquals(json.point(".int").get().asInt(), 1);

// key带有.的，需要用引号把key包起来
Assertions.assertEquals(
    json.point(".\"JSON.Map\"").get().asMap().hashCode(),
    new HashMap<String, String>() {
        {
            put("m1", "1");
            put("m2", "2");
        }
    }
        .hashCode()
);

Assertions.assertEquals(
    json.point(".List").get().asList(String.class).hashCode(),
    new ArrayList<String>() {
        {
            add("1");
            add("2");
        }
    }
        .hashCode()
);

Assertions.assertEquals(json.point(".List[0]").get().asString(), "1");

Assertions.assertEquals(
    json.point(".ListMap[0].id3[0][0]").get().asMap().hashCode(),
    new HashMap<>() {
        {
            put("id11", "1");
            put("id22", "2");
            put(
                "id33",
                new HashMap<String, String>() {
                    {
                        put("id333", "value1");
                    }
                }
            );
        }
    }
        .hashCode()
);

// *表示遍历节点，注意：一旦用上*,结果必定是数组
Assertions.assertEquals(
    json.point(".ListMap[*].id3[*][*].id11").get().asList(String.class).hashCode(),
    new ArrayList<>() {
        {
            add("1");
            add("11");
        }
    }
        .hashCode()
);

// json数组获取节点
Assertions.assertEquals(
    jsonArray.point(".[0].ListMap[*].id3[*][*].id33.id333").get().asList(String.class).hashCode(),
    new ArrayList<>() {
        {
            add("value1");
            add("value2");
        }
    }
        .hashCode()
);
```

#### 可选链

```java
Assertions.assertEquals(json.point("?.int-null").get().asInt(), null);

Assertions.assertEquals(json.point("?.\"JSON.Map-null\"").get().asMap(), null);

Assertions.assertEquals(json.point("?.List[3]").get().asString(), null);

Assertions.assertEquals(json.point(".ListMap[0]?.id3[0][1]").get().asMap(), null);

Assertions.assertEquals(
    json.point(".ListMap[*].id3[*][*]?.不存在节点").get().asList(String.class).hashCode(),
    new ArrayList<>() {
        {
            add(null);
            add(null);
        }
    }
        .hashCode()
);

Assertions.assertEquals(
    json.point(".ListMap[*]?.不存在节点[*][*]?.不存在节点").get().asList(String.class).hashCode(),
    new ArrayList<>() {
        {
            add(null);
            add(null);
        }
    }
        .hashCode()
);

Assertions.assertEquals(
    jsonArray.point("?.[2].ListMap[*].id3[*][*].id33.id333").get().asList(String.class).hashCode(),
    new ArrayList<>() {
        {
            add(null);
        }
    }
        .hashCode()
);

```

#### 默认值
```java
Assertions.assertEquals(json.point(".int-null", 1).get().asInt(), 1);

Assertions.assertEquals(
    json.point(".ListMap[*].id3[*][*].id11-null", "fix").get().asList(String.class).hashCode(),
    new ArrayList<>() {
        {
            add("fix");
            add("1"); // 节点存在则返回原值
            add("fix");
        }
    }
        .hashCode()
);

Assertions.assertEquals(
    jsonArray
        .point(".[0].ListMap[*].id3[*][*].id33.id333-null", () -> "fix")
        .get()
        .asList(String.class)
        .hashCode(),
    new ArrayList<>() {
        {
            add("fix");
            add("fix");
            add("fix");
        }
    }
        .hashCode()
);

Assertions.assertEquals(
    json
        .point(".ListMap[0].id3[0][0].不存在节点.不存在节点")
        .defaultValue(
            new HashMap<String, Object>() {
                {
                    put(".ListMap[0].id3[0][0].不存在节点", new HashMap<String, Object>() {});
                    put(".ListMap[0].id3[0][0].不存在节点.不存在节点", "fix");
                }
            }
        )
        .get()
        .asString(),
    "fix"
);
```

### Point.has

```java
Assertions.assertEquals(json.point(".int").has(), true);

// 遍历的情况，只要其中一个元素有都算有
Assertions.assertEquals(json.point(".ListMap[*].id3[*][*].id11").has(), true);

// 默认启用默认值特性
Assertions.assertEquals(json.point(".ListMap[*].id3[*][*].不存在节点", 1).has(), true);

// 关闭默认值特性
Assertions.assertEquals(json.point(".ListMap[*].id3[*][*].不存在节点", 1).has(false), false);
```

### Point.delete
```java
json.point(".int").delete();

json.point(".ListMap[0].id3[0][0].id33.id333").delete();

// 遍历删除
json.point(".ListMap[1].id3[*][*].id11").delete();
```

### Point.put
```java
json.point(".").put("int_新节点", 1);

// 遍历数组节点设置key
json.point(".ListMap[*].id3[*][*]").put("int_新节点", 1);

// 迭代point特性
json.point(".ListMap[1]").point(".id3[*][*]").put("int_新节点", 1);
Assertions.assertEquals(json.point(".ListMap[1].id3[0][0].int_新节点").has(), true);
Assertions.assertEquals(json.point(".ListMap[0].id3[0][0].int_新节点").has(), false);

jsonArray.point(".[*].ListMap[*].id3[*][*].id33").put("int_新节点", 1);
Assertions.assertEquals(jsonArray.point(".[0].ListMap[0].id3[0][0].id33.int_新节点").has(), true);
Assertions.assertEquals(jsonArray.point(".[0].ListMap[1].id3[0][0].id33.int_新节点").has(), true);
Assertions.assertEquals(jsonArray.point(".[1].ListMap[0].id3[0][0].id33.int_新节点").has(), true);
Assertions.assertEquals(jsonArray.point(".[1].ListMap[1].id3[0][0].id33.int_新节点").has(), true);
```

### Point.add
```java
json
    .point(".ListMap")
    .add(
        JSON
            .sPut("id1", "1")
            .put("id2", "2")
            .put(
                "id3",
                JSON.sAdd(
                    JSON.sAdd(
                        JSON
                            .sPut("id11", "1")
                            .put("id22", "2")
                            .put("id33", JSON.sPut("id333", "value1"))
                    )
                )
            )
    );

// 遍历节点添加元素
json
    .point(".ListMap[*].id3")
    .add(
        JSON.sAdd(JSON.sPut("id11", "1").put("id22", "2").put("id33", JSON.sPut("id333", "value1")))
    );
```
QTK-Json
========

基于**com.fasterxml.jackson**二次封装的JSON操作库,相比于原库,提供更加友好方便的JSON操作体验,更支持使用**JSON POINT**
操作节点及类似Node.js ES2020可选链功能## Installation

```groovy
//gradle
repositories {
    ...
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.qw623577789:qtk-json:1.15.0'
}
```

## Features

- 超级方便的JSON节点操作函数, 支持链式操作，丝滑般开发体验，具体见下面详细说明
- 支持ES2020特性的可选链JSON POINT
- 友好的错误提示，当节点路径不存在时，报错可提示具体节点路径(节点遍历也可以支持)
- 支持常用日期格式时间戳转化为``LocalDateTime``
- 美化输出JSON字符串时支持控制缩进空格数
- 支持JSON Merge操作(类似Node Object.assign函数)，合并key操作
- 支持JSON只摘取/忽略某些节点

## 方法

### 创建JSON对象

- **JSON new JSON(boolean isObject)** 创建一个JSON实例, true/false控制创建出来是**JSON对象**还是**JSON数组**
- **JSON new JSON(JsonNode jacksonNode)** 将com.fasterxml.jackson的``JsonNode``转化为JSON实例
- **JSON parse(Object object)** 可将大部分Java对象(*Class、Map、List、File、Reader*)转换为JSON实例
- **String toString(boolean pretty, int spaceAmount)** 将JSON实例转换为JSON字符串, ``pretty``
  控制是否美化输出json, ``spaceAmount``可以控制美化输出时空格数量
- **JSON deepCopy()** 深拷贝JSON实例
- **JSON missingNode()** 快速创建``missing``值的JSON实例
- **JSON nullNode()** 快速创建``null``值的JSON实例
- **JSON createObject()** 快速创建空对象的JSON实例
- **JSON createArray()** 快速创建空数组的JSON实例
- **JSON assign(Object target, Object... sources)** JSON对象合并
- **JsonNode getJacksonNode()** 将JSON实例转换为com.fasterxml.jackson的``JsonNode``
- **JSON rmNull()** 删除对象节点下所有为null的key

### JSON新增

- **JSON sPut(String id, Object value)** *静态方法*, 用于创建**JSON对象实例**,并设置key/value, value支持*大部分Java对象*
  及*JSON实例*
- **JSON put(String id, Object value)** 在**JSON对象实例**上设置key/value, value支持*大部分Java对象*及*JSON实例*
- **JSON sAdd(Object ...value)** *静态方法*, 用于创建**JSON数组实例**,并一次性添加无限个元素, value支持*大部分Java对象*及
  *JSON实例*
- **JSON add(Object ...value)** 在**JSON数组实例**上一次性添加无限个元素, value支持*大部分Java对象*及*JSON实例*
- **JSON concat(List<?> list)** 在**JSON数组实例**上添加存于``List``的元素，value支持*大部分Java对象*及*JSON实例*

### JSON Point指针操作 Get/Put/Delete/Has/Add/Concat

- **Point point(String point)** 根据JSON路径(例如 \.aaa.bbb[0].ccc[*].ddd)返回一个``Point``
  对象，用于对节点进行``get``、``put``、``delete``、``has``、``add``、``concat``操作
- **Point point(String point, Object defaultValue, boolean toUpdateNode)** 在**Point point(String
  point)**基础上，在``get``、``has``操作时，若**节点不存在**时．返回默认值; **``toUpdateNode``默认值为``false``
  ,即使用默认值时不会更改原来对象的值, 设为true后，若节点不存在，则会修改JSON实例，将默认值补上**
- **Point point(String point, Supplier\<Object\> defaultValueFunc, boolean toUpdateNode)** 在**Point point(String
  point)**基础上，在``get``、``has``操作时，若**节点不存在**时．执行函数返回默认值
- **Point point(String point, HashMap<String, Object> defaultValueMap, boolean toUpdateNode)** 在**Point point(String
  point)**基础上，可为``第一个参数的point``路径批量设置默认值(point=".aaa.bbb.ccc", 可先设置.aaa = 对象，再设置.aaa.bbb =
  对象)，在``get``、``has``操作时，若**节点不存在**时．**执行函数/直接**返回默认值
- **JSONConfig config()** 自定义jackson库特性后进行JSON操作
- Point操作
    - **Get get()** 返回``Get``实例，根据point获取节点值，有``asString()``、``asXXXX()``等方法最终得到值(开启**默认值、可选链特性
      **)
        - **String asString()**、**Long asLong()**、**Integer asInt()**、**Boolean asBoolean()**、**Double asDouble()**、*
          *Void asNull()**、**BigDecimal asBigDecimal()**、**Float asFloat()**、**\<T\> T as(Class\<T\> type)**、*
          *List\<Object\> asList**、**\<T\> List\<T\> asList(Class\<T\> itemType)**、**List\<T\> asList(Class\<T\>
          itemType, boolean ignoreMissingNode)**、**HashMap\<String, T\> asMap(Class\<T\> valueType)**、**HashMap\<String,
          Object\> asMap()**、**JSON asJSON()**、**size()**
        - **Object getAsIf(Function<JSON, Class> ifs)、Object getAsIf(GetAsIf... ifs)** 根据值情况，动态指定getAs类型
    - **Get get(boolean toWithDefault, boolean supportNullishKey, boolean nullable)** 在上get方法基础上，可以控制*
      *是否开启默认值**、**是否开启可选链**特性, ``nullable``控制当最终结果不存在时，**是否返回null，否则在没有可选链标注情况下，会抛空指针错误
      **,*此属性等同于给point每个节点加上可选链标志*
    - **JSON put(String id, Object value)** 给对应point的JSON对象节点赋值，支持**大部分JAVA对象**、**JSON实例**、*
      *com.fasterxml.jackson的``JsonNode``**
    - **JSON put(Object value)** 给对应point的JSON对象节点赋值(**节点名在point里**)，支持**大部分JAVA对象**、**JSON实例**、
      **com.fasterxml.jackson的``JsonNode``**
    - **JSON add(Object... items)** 给对应point的JSON数组节点添加元素，支持**大部分JAVA对象**、**JSON实例**、*
      *com.fasterxml.jackson的``JsonNode``**
    - **JSON concat(List\<Object\> list)** 给对应point的JSON数组节点添加元素，支持**大部分JAVA对象**、**JSON实例**、*
      *com.fasterxml.jackson的``JsonNode``**
    - **boolean has()** 判断point对应节点值有没有存在(*默认会开启默认值特性*)
    - **boolean has(boolean toWithDefault)** 判断point对应节点值有没有存在,可设定启不启用*默认值特性*
    - **JSON delete()** 删除point节点的值
    - **JSON backToJSON()** 返回``Point``实例所在的``JSON``实例
    - **Point defaultValue(Object defaultValue, boolean toUpdateNode)/Point defaultValue(HashMap\<String, Object\>
      defaultValueMap, boolean toUpdateNode)** 设置节点默认值，``toUpdateNode``含义见上面有相同说明
    - **Point point(String point)** 在原来Point上在延伸point, 例如: point(".aaa.bbb")等同于point(".aaa").point(".bbb")
      ,此特性用于支持point节点分级
    - **boolean isArray()** 返回point节点是否为数组节点
    - **boolean isObject()** 返回point节点是否为对象节点
    - **boolean isNull()** 返回point节点是否为Null节点
    - **boolean isMissing()** 返回point节点是否为Missing节点
    - **boolean isEmpty()** 返回point节点是否为空(*数组节点则是空数组,对象节点则是空对象*)
    - **String toString(boolean pretty, int spaceAmount)** 将Point实例转换为JSON字符串, ``pretty``
      控制是否美化输出json,``spaceAmount``可以控制美化输出时空格数量
    - **JSON retain(String... fieldName)** 只保留当前节点下某些字段(*此操作对象引用操作，会影响原来的数据*)
    - **JSON exclude(String... fieldName)** 排除当前节点下某些字段(*此操作对象引用操作，会影响原来的数据*)
    - **JSON rmNull()** 删除point对象节点下所有为null的key

### JSON摘取/忽略节点

- **JSON pick(String... paths)** 只摘取JSON某些路径字段，支持子路径，若是数组节点会自动遍历(
  *此操作会对原对象进行一次深拷贝，故不影响原来的数据*)
- **JSON exclude(String... paths)** 忽略JSON某些路径字段，支持子路径，若是数组节点会自动遍历(
  *此操作会对原对象进行一次深拷贝，故不影响原来的数据*)

### JSON合并

- **(静态方法)JSON assign(Object target, Object... sources)**
  将所有可枚举属性的值从一个或多个源对象复制到目标对象，类似Node的``Object.assign`，可结合给字段加``Jackson的@JsonMerge``
  实现深浅拷贝
- **(实例方法)JSON merge(Object... sources)** 将所有可枚举属性的值从一个或多个源对象复制到当前对象

### 定制JSON库特性(Jackson特性)

- JSONConfig jackson库特性配置，并且使用设置的特性进行JSON操作
    - **JSONConfig features(HashMap\<FormatFeature, Boolean\> features)** 控制``jackson``库``enable/disable``特性
    - **JSONConfig serializationInclusion(JsonInclude.Include setSerializationInclusion)** 控制``jackson``
      库``setSerializationInclusion``特性
    - **JSONConfig registerModule(com.fasterxml.jackson.databind.Module... module)** 为``jackson``库注册模块
    - **JSONConfig confirmToCreateMapper()** **<font color=red>最终生成``jackson``库``ObjectMapper``
      ,应用于后续的操作</font>**
    - **JSON new JSON(boolean isObject)** 创建一个JSON实例, true/false控制创建出来是**JSON对象**还是**JSON数组**
    - **JSON new JSON(JsonNode jacksonNode)** 将com.fasterxml.jackson的``JsonNode``转化为JSON实例
    - **JSON parse(Object object)** 可将大部分Java对象转换为JSON实例
    - **JSON missingNode()** 快速创建``missing``值的JSON实例
    - **JSON nullNode()** 快速创建``null``值的JSON实例
    - **JSON createObject()** 快速创建空对象的JSON实例
    - **JSON createArray()** 快速创建空数组的JSON实例
    - **JSON assign(Object target, Object... sources)** JSON对象合并
    - **JSON sPut(String id, Object value)** *静态方法*, 用于创建**JSON对象实例**,并设置key/value, value支持
      *大部分Java对象*及*JSON实例*
    - **JSON sAdd(Object ...value)** *静态方法*, 用于创建**JSON数组实例**,并一次性添加无限个元素, value支持
      *大部分Java对象*及*JSON实例*

## Usage

更多的样例请看测试用例

### 对象构建

```groovy
String json = JSON
    .sPut("int", 1)
    .put("string", "2")
    .put("float", 2.5f)
    .put("double", 2.5d)
    .put("BigDecimal", BigDecimal.valueOf(1))
    .put("boolean", false)
    .put("null", null)
    .put("map", Map.of("a", "1", "b", "2"))
    .put("JSON.Map", JSON.sPut("m1", "1").put("m2", "2"))
    .put("List", List.of("1", "2"))
    .put("JSON.List", JSON
        .sAdd(1)
        .add(2, 4)
        .add(5)
        .add(List.of(6, 7))
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

### 数组构建

```groovy
import java.util.List;
import java.util.Map;

String json = JSON
    .sAdd(1, 3, List.of(6, 7))
    .add(2, 4)
    .add(5)
    .add(List.of(6, 7))
    .add((Object) null)
    .add(
        JSON
            .sPut("array.map.int", 1)
            .put("array.map.map", Map.of("m1", "1"))
            .put("array.map.array", List.of("1"))
    )
    .concat(List.of("concat.1"))
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

```groovy
Assertions.assertEquals(json.point(".int").get().asInt(), 1);

// key带有.的，需要用引号把key包起来
Assertions.assertEquals(
    json.point(".\"JSON.Map\"").get().asMap().hashCode(),
    Map.of("m1", "1", "m2", "2").hashCode()
);

Assertions.assertEquals(
    json.point(".List").get().asList(String.class).hashCode(),
    List.of("1", "2").hashCode()
);

Assertions.assertEquals(json.point(".List[0]").get().asString(), "1");

Assertions.assertEquals(
    json.point(".ListMap[0].id3[0][0]").get().asMap().hashCode(),
    Map.of(
        "id11", "1",
        "id22", "2",
        "id33", Map.of("id333", "value1")
    ).hashCode()
);

// *表示遍历节点，注意：一旦用上*,结果必定是数组
Assertions.assertEquals(
    json.point(".ListMap[*].id3[*][*].id11").get().asList(String.class).hashCode(),
    List.of("1", "11").hashCode()
);

// json数组获取节点
Assertions.assertEquals(
    jsonArray.point(".[0].ListMap[*].id3[*][*].id33.id333").get().asList(String.class).hashCode(),
    List.of("value1", "value2").hashCode()
);
```

#### 可选链

```groovy
Assertions.assertEquals(json.point("?.int-null").get().asInt(), null);

Assertions.assertEquals(json.point("?.\"JSON.Map-null\"").get().asMap(), null);

Assertions.assertEquals(json.point("?.List[3]").get().asString(), null);

Assertions.assertEquals(json.point(".ListMap[0]?.id3[0][1]").get().asMap(), null);

Assertions.assertEquals(
    json.point(".ListMap[*].id3[*][*]?.不存在节点").get().asList(String.class).hashCode(),
    List.of(null, null).hashCode()
);

Assertions.assertEquals(
    json.point(".ListMap[*]?.不存在节点[*][*]?.不存在节点").get().asList(String.class).hashCode(),
    List.of(null, null).hashCode()
);

Assertions.assertEquals(
    jsonArray.point("?.[2].ListMap[*].id3[*][*].id33.id333").get().asList(String.class).hashCode(),
    List.of(null).hashCode()
);

```

#### 默认值

```groovy
Assertions.assertEquals(json.point(".int-null", 1).get().asInt(), 1);

Assertions.assertEquals(
    json.point(".ListMap[*].id3[*][*].id11-null", "fix").get().asList(String.class).hashCode(),
    List.of("fix", "1", "fix").hashCode()
);

Assertions.assertEquals(
    jsonArray.point(".[0].ListMap[*].id3[*][*].id33.id333-null", () -> "fix").get().asList(String.class).hashCode(),
    List.of("fix", "fix", "fix").hashCode()
);

Assertions.assertEquals(
    json.point(".ListMap[0].id3[0][0].不存在节点.不存在节点")
        .defaultValue(
            new DefaultValueMap() {
                {
                    put(".ListMap[0].id3[0][0].不存在节点", Map.of());
                    put(".ListMap[0].id3[0][0].不存在节点.不存在节点", "fix");
                }
            }
        )
        .get().asString(),
    "fix"
);
```

### Point.has

```groovy
Assertions.assertEquals(json.point(".int").has(), true);

// 遍历的情况，只要其中一个元素有都算有
Assertions.assertEquals(json.point(".ListMap[*].id3[*][*].id11").has(), true);

// 默认启用默认值特性
Assertions.assertEquals(json.point(".ListMap[*].id3[*][*].不存在节点", 1).has(), true);

// 关闭默认值特性
Assertions.assertEquals(json.point(".ListMap[*].id3[*][*].不存在节点", 1).has(false), false);
```

### Point.delete

```groovy
json.point(".int").delete();

json.point(".ListMap[0].id3[0][0].id33.id333").delete();

// 遍历删除
json.point(".ListMap[1].id3[*][*].id11").delete();
```

### Point.put

```groovy
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

```groovy
json.point(".ListMap").add(
    JSON.sPut("id1", "1")
        .put("id2", "2")
        .put("id3",
            JSON.sAdd(
                JSON.sAdd(
                    JSON.sPut("id11", "1")
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

### 时间转换

```groovy
JSON json = JSON
    .sPut("timestamp", 1657174400)
    .put("milliTimestamp", 1657174400000L)
    .put("date", "2022-07-07")
    .put("dateTime", "2022-07-07 14:05:35");

System.out.println(json.toString());

assertEquals(json.point(".timestamp").get().asLocalDateTime(), LocalDateTime.parse("2022-07-07T14:13:20"));

assertEquals(json.point(".milliTimestamp").get().asLocalDateTime(), LocalDateTime.parse("2022-07-07T14:13:20"));

assertEquals(json.point(".dateTime").get().asLocalDateTime(), LocalDateTime.parse("2022-07-07T14:05:35"));

assertEquals(json.point(".date").get().asLocalDateTime(), LocalDateTime.parse("2022-07-07T00:00:00"));

assertEquals(json.getLocalDateTime(".timestamp"), LocalDateTime.parse("2022-07-07T14:13:20"));

assertEquals(json.getLocalDateTime(".milliTimestamp"), LocalDateTime.parse("2022-07-07T14:13:20"));

assertEquals(json.getLocalDateTime(".dateTime"), LocalDateTime.parse("2022-07-07T14:05:35"));

assertEquals(json.getLocalDateTime(".date"), LocalDateTime.parse("2022-07-07T00:00:00"));
```

### 控制Jackson库特性

```groovy
Assertions.assertEquals(
    JSON.config()
        .features(Map.of(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES, true))
        .serializationInclusion(JsonInclude.Include.NON_NULL)
        .confirmToCreateMapper()
        .parse("{a:1}")
        .point()
        .get()
        .isObject(),
    true
);
```

### 语法糖

```groovy
json.getXXX(".point"); //等同于json.point(".point").get().asXXX()json.getXXX(); //等同于json.point(".").get().asXXX()

json.getXXX(".point", "defaultValue"); //等同于json.point(".point").defaultValue("defaultValue").get().asXXX()

json.getNullableXXX(".point"); //等同于json.point(".point").get(true).asXXX()

json.getNullableXXX(); //等同于json.point(".").get(true).asXXX()

json.getNullableXXX(".point", "defaultValue"); //等同于json.point(".point").get(true).defaultValue("defaultValue").asXXX()
```

### 注意

- 设置了默认值的asList情况, 若point里有``[]``数组下标操作,则默认值是元素的默认值;若无,则是整个节点的默认值.
    - 例如: point(".a[1].b", "1").asList(), 意思为给a数组下标为1的对象的b节点赋值1; point(".a", new
      ArrayList\<String\>(){}).asList(),意思是当对象的a节点不存在时,给对象添加一个数组节点a
- 若point路径某段字段带``.``，那段字段要加上引号。例如{"a": {"aa.bb": 1}}, point正确应该为``.a."aa.bb"``

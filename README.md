## jackson support
### 1. @GenerateField
在对外序列化成 string 时，根据 @GenerateField生成原 java 对象未字义的字段。适用于一些特殊场景，比如 S3/oss 对象的 URL：在后台操作时我们一般只关注存储的相对路径，但是当输出给前端的会是完整可访问的 URL。此时在 JSON序列化阶段做一个统一的转换操作是一个简洁优雅的方式。
例:
```java
// VO 对象
public class Foo {
    @GenerateField(fieldName = "a1")
    private String a;
    @GenerateField(fieldName = {"b1", "b2"})
    private String b;
    @GenerateField(fieldName = {"c1"}, group = "group1", outputOriginalField = false)
    private String c;
    @GenerateField(fieldName = "d", group = "group2", outputOriginalField = false)
    private String d;
}
```
具体使用，见unit test代码

## json object modifier
对给定的json，更新某个字段的值，就像js一样操作。比如原json对象{"a":{"b":"v1"}}，通过直接指定路径a.b修改值

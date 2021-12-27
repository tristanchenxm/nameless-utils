package nameless.common.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jdk.nashorn.internal.objects.annotations.Getter;
import jdk.nashorn.internal.objects.annotations.Setter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@SuppressWarnings("unchecked")
public class GenerateFieldSerializerTest {
    public static class Foo {
        @GenerateField(fieldName = "a1")
        private String a;
        @GenerateField(fieldName = {"b1", "b2"})
        private String b;
        @GenerateField(fieldName = {"c1"}, outputOriginalField = false)
        private String c;
        @GenerateField(fieldName = "d", group = "group2", outputOriginalField = false)
        private String d;

        public String getA() {
            return a;
        }

        public void setA(String a) {
            this.a = a;
        }

        public String getB() {
            return b;
        }

        public void setB(String b) {
            this.b = b;
        }

        public String getC() {
            return c;
        }

        public void setC(String c) {
            this.c = c;
        }

        public String getD() {
            return d;
        }

        public void setD(String d) {
            this.d = d;
        }
    }

    @Test
    public void test() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        BiFunction<Object, String, String> function = (o, group) -> {
            if ("group2".equals(group)) {
                return "$" + o + "$";
            }
            return "#" + o + "#";
        };
        objectMapper.setAnnotationIntrospector(new GenerateFieldAnnotationIntrospector(function));
        Foo foo = new Foo();
        foo.a = "a";
        foo.b = "b";
        foo.c = "c";
        foo.d = "d";
        String jsonString = objectMapper.writeValueAsString(foo);
        Map<String, String> deserialized = objectMapper.readValue(jsonString, Map.class);
        Assertions.assertEquals(foo.getA(), deserialized.get("a"));
        Assertions.assertEquals(function.apply(foo.getA(), null), deserialized.get("a1"));
        Assertions.assertEquals(function.apply(foo.getB(), null), deserialized.get("b1"));
        Assertions.assertEquals(function.apply(foo.getB(), null), deserialized.get("b2"));
        Assertions.assertEquals(function.apply(foo.getC(), null), deserialized.get("c1"));
        Assertions.assertEquals(function.apply(foo.getD(), "group2"), deserialized.get("d"));
    }

    public static class Foo2 {
        @GenerateField(fieldName = {"e1"})
        private List<String> e;
        @GenerateField(fieldName = "f1")
        private int f;
        @GenerateField(fieldName = "g1", outputOriginalField = false)
        private List<Integer> g;

        public List<String> getE() {
            return e;
        }

        public void setE(List<String> e) {
            this.e = e;
        }

        public int getF() {
            return f;
        }

        public void setF(int f) {
            this.f = f;
        }

        public List<Integer> getG() {
            return g;
        }

        public void setG(List<Integer> g) {
            this.g = g;
        }
    }

    @Test
    public void testList() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        BiFunction<Object, String, ?> function = (o, group) -> o;
        objectMapper.setAnnotationIntrospector(new GenerateFieldAnnotationIntrospector(function));
        Foo2 foo = new Foo2();
        foo.e = Arrays.asList("a", "b", "c");
        foo.f = 10;
        foo.g = Arrays.asList(1, 2, 3);
        String jsonString = objectMapper.writeValueAsString(foo);
        Map<String, String> deserialized = objectMapper.readValue(jsonString, Map.class);
        Assertions.assertEquals(function.apply(foo.getE(), null), deserialized.get("e1"));
        Assertions.assertEquals(function.apply(foo.getF(), null), deserialized.get("f1"));
        Assertions.assertEquals(function.apply(foo.getG(), null), deserialized.get("g1"));
    }
}

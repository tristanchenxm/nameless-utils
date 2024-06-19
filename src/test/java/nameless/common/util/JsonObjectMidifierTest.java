package nameless.common.util;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

@SuppressWarnings("unchecked")
public class JsonObjectMidifierTest {
    @Test
    public void test1() throws Exception {
        String json = "{\"a\": {\"b\": [{\"c\": 1}]}}";
        List<NameValuePair> nameValuePairs = Collections.singletonList(new NameValuePair("name", "value"));
        Assertions.assertThrows(
                RuntimeException.class,
                () -> JsonObjectModifier.setObjectFieldValue(json, nameValuePairs)
        );
    }

    @Test
    public void test2() throws Exception {
        String json = "{\"a\": {\"b\": [{\"c\": 1}]}}";
        List<NameValuePair> nameValuePairs = Collections.singletonList(new NameValuePair("a.b[0].c", 2));
        Map<String, Object> result = (Map<String, Object>) JsonObjectModifier.setObjectFieldValue(json, nameValuePairs);
        Assertions.assertEquals(((Map<String,List<Map<String, Object>>>)result.get("a")).get("b").get(0).get("c"), 2);
    }

    @Test
    public void test3() throws Exception {
        String json = "{\"a\": {\"b\": [{\"c\": 1}]}}";
        List<NameValuePair> nameValuePairs = Collections.singletonList(new NameValuePair("a.b[0].d[0][1]", new ArrayList<>(Arrays.asList(1, 2, 3)), false));
        Map<String, Map<String, List<Map<String, List<Object>>>>> result = (Map<String, Map<String, List<Map<String, List<Object>>>>>)JsonObjectModifier.setObjectFieldValue(json, nameValuePairs);
        List<List<Integer>> d_0_1 = (List<List<Integer>>)result.get("a").get("b").get(0).get("d").get(0);
        Assertions.assertNull(d_0_1.get(0));
        Assertions.assertEquals(d_0_1.get(1).get(0), 1);
        Assertions.assertEquals(d_0_1.get(1).get(1), 2);
        Assertions.assertEquals(d_0_1.get(1).get(2), 3);
    }

    @Test
    public void test4() throws Exception {
        String json = "{\"a\": {\"b\": [{\"c\": 1}]}}";
        List<NameValuePair> nameValuePairs = Collections.singletonList(new NameValuePair("a.b", new ArrayList<>(Arrays.asList(1, 2, 3)), false));
        Map<String, Map<String, List<Integer>>> result = (Map<String, Map<String, List<Integer>>>)JsonObjectModifier.setObjectFieldValue(json, nameValuePairs);
        List<Integer> b = result.get("a").get("b");
        Assertions.assertEquals(b.get(0), (1));
    }

    @Test
    public void test5() throws Exception {
        String json = "{}";
        List<NameValuePair> nameValuePairs = Collections.singletonList(new NameValuePair("a[0].b", new ArrayList<>(Arrays.asList(1, 2, 3)), false));
        Map<String, List<Map<String, List<Integer>>>> r1 = (Map<String, List<Map<String, List<Integer>>>>)JsonObjectModifier.setObjectFieldValue(json, nameValuePairs);
        List<Integer> b = r1.get("a").get(0).get("b");
        assert b.get(0).equals(1);

        nameValuePairs = Arrays.asList(new NameValuePair("a[0].c", "c-value-1", false),
                new NameValuePair("a[1].c", "c-value-2", false));
        Map<String, List<Map<String, Object>>> r2 = (Map<String, List<Map<String, Object>>>)JsonObjectModifier.setObjectFieldValue((Map)r1, nameValuePairs);
        Assertions.assertEquals(r2.get("a").get(0).get("c"), "c-value-1");
        Assertions.assertEquals(r2.get("a").get(1).get("c"), "c-value-2");
        Assertions.assertEquals(((List<Integer>)r2.get("a").get(0).get("b")).get(2), 3);
    }
}

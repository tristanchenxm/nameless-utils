package nameless.common.util;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

/**
 * Utility class to modify json fields by specifying the path to the field.
 */
@SuppressWarnings("unchecked")
public class JsonObjectModifier {
    public static final ObjectMapper COMMON_OBJECT_MAPPER = new ObjectMapper();

    static {
        COMMON_OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * a helper class to represent the AST of the field path.
     */
    private static class AstNode {
        public enum Type {
            MAP, LIST;
        }

        private String name;
        private Type type;
        private int arrayIndex = -1;
        private boolean failOnUnknownProperties;
        private AstNode child;
        private AstNode parent;

        public AstNode(String name, Type type, int arrayIndex, boolean failOnUnknownProperties) {
            this.name = name;
            this.type = type;
            this.arrayIndex = arrayIndex;
            this.failOnUnknownProperties = failOnUnknownProperties;
        }

        /**
         * Get the original full path of the field.
         */
        public String getPath() {
            String displayName = name == null ? "[" + arrayIndex + "]" : name;
            if (parent == null) {
                return displayName;
            } else {
                return parent.getPath() + (name == null ? "" : ".") + displayName;
            }

        }

        public Object newObject() {
            return type.equals(Type.LIST) ? new ArrayList<Object>() : new HashMap<String, Object>();
        }

        public static AstNode parse(String keyString, boolean failOnUnknownProperties) {
            AstNode head = new AstNode(null, null, -1, failOnUnknownProperties);
            AstNode node = head;
            String[] keys = keyString.split("\\.");
            int cursor = 0;
            String currentKey = keys[cursor];
            while (true) {
                int indexOfArrayStart = currentKey.indexOf("[");
                if (indexOfArrayStart == -1) {
                    node.child = new AstNode(currentKey, Type.MAP, -1, failOnUnknownProperties);
                    node.child.parent = node;
                    node = node.child;
                } else if (indexOfArrayStart == 0) {
                    int indexOfArrayEnd = currentKey.indexOf("]");
                    int arrayIndex = Integer.parseInt(currentKey.substring(indexOfArrayStart + 1, indexOfArrayEnd));
                    boolean isLast = indexOfArrayEnd == currentKey.length() - 1;
                    node.child = new AstNode(null, isLast ? Type.MAP : Type.LIST, arrayIndex, failOnUnknownProperties);
                    node.child.parent = node;
                    node = node.child;
                    if (!isLast) {
                        currentKey = currentKey.substring(indexOfArrayEnd + 1);
                        continue;
                    }

                } else {
                    String arrayName = currentKey.substring(0, indexOfArrayStart);
                    node.child = new AstNode(arrayName, Type.LIST, -1, failOnUnknownProperties);
                    node.child.parent = node;
                    node = node.child;
                    currentKey = currentKey.substring(indexOfArrayStart);
                    continue;
                }

                if (++cursor >= keys.length) {
                    break;
                }

                currentKey = keys[cursor];
            }

            head.child.parent = null;
            return head.child;
        }

    }

    public static Object setObjectFieldValue(String jsonString, List<NameValuePair> nameValuePairs) throws JsonProcessingException {
        if (jsonString.startsWith("[")) {
            return setObjectFieldValue(COMMON_OBJECT_MAPPER.readValue(jsonString, List.class), nameValuePairs);
        }

        return setObjectFieldValue(COMMON_OBJECT_MAPPER.readValue(jsonString, HashMap.class), nameValuePairs);
    }

    public static List<Object> setObjectFieldValue(List<Object> list, List<NameValuePair> nameValuePairs) {
        for (NameValuePair nameValuePair : nameValuePairs) {
            AstNode astNode = AstNode.parse(nameValuePair.getName(), nameValuePair.isFailOnUnknownProperties());
            setObjectFieldValue0(list, astNode, nameValuePair.getValue());
        }

        return list;
    }

    public static Map<String, Object> setObjectFieldValue(Map<String, Object> map, List<NameValuePair> nameValuePairs) {
        for (NameValuePair nameValuePair : nameValuePairs) {
            AstNode astNode = AstNode.parse(nameValuePair.getName(), nameValuePair.isFailOnUnknownProperties());
            setObjectFieldValue0(map, astNode, nameValuePair.getValue());
        }

        return map;
    }

    private static void setObjectFieldValue0(List<Object> list, final AstNode astNode, Object value) {
        if (astNode.arrayIndex == -1) {
            throw new RuntimeException("Try to put a map value, but the existing value is a list: " + astNode.getPath());
        }

        if (astNode.arrayIndex >= list.size()) {
            if (astNode.failOnUnknownProperties) {
                throw new RuntimeException("Index out of bounds: " + astNode.getPath());
            }
            list.addAll(Collections.nCopies(astNode.arrayIndex - list.size() + 1, null));
            list.set(astNode.arrayIndex, astNode.newObject());
        }


        setChildValue(astNode, list, value);
    }

    private static void setChildValue(AstNode astNode, List<Object> l, Object value) {
        if (astNode.child != null) {
            Object o = l.get(astNode.arrayIndex);
            if (o == null) {
                o = astNode.newObject();
                l.set(astNode.arrayIndex, o);
            }

            if (o instanceof List) {
                setObjectFieldValue0((List<Object>) o, astNode.child, value);
            } else {
                setObjectFieldValue0((Map<String, Object>) o, astNode.child, value);
            }

        } else {
            l.set(astNode.arrayIndex, value);
        }

    }

    private static void setObjectFieldValue0(Map<String, Object> o, final AstNode astNode, Object value) {
        if (astNode.name == null) {
            throw new RuntimeException("Try to put a list value, but the existing value is a map: " + astNode.getPath());
        }

        if (!o.containsKey(astNode.name) && astNode.failOnUnknownProperties) {
            throw new RuntimeException("Key not found: " + astNode.getPath());
        }

        setChildValue(astNode, o, value);
    }

    private static void setChildValue(AstNode astNode, Map<String, Object> map, Object value) {
        if (astNode.child != null) {
            Object v = map.get(astNode.name);
            if (v == null) {
                v = astNode.newObject();
                map.put(astNode.name, v);
            }

            if (v instanceof List) {
                setObjectFieldValue0((List<Object>) v, astNode.child, value);
            } else {
                setObjectFieldValue0((Map<String, Object>) v, astNode.child, value);
            }

        } else {
            map.put(astNode.name, value);
        }

    }
}

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
            String displayName = name == null ? "[" + getArrayIndex() + "]" : name;
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
                    node.setChild(new AstNode(currentKey, Type.MAP, -1, failOnUnknownProperties));
                    node.getChild().setParent(node);
                    node = node.getChild();
                } else if (indexOfArrayStart == 0) {
                    int indexOfArrayEnd = currentKey.indexOf("]");
                    int arrayIndex = Integer.parseInt(currentKey.substring(indexOfArrayStart + 1, indexOfArrayEnd));
                    boolean isLast = indexOfArrayEnd == currentKey.length() - 1;
                    node.setChild(new AstNode(null, isLast ? Type.MAP : Type.LIST, arrayIndex, failOnUnknownProperties));
                    node.getChild().setParent(node);
                    node = node.getChild();
                    if (!isLast) {
                        currentKey = currentKey.substring(indexOfArrayEnd + 1);
                        continue;
                    }

                } else {
                    String arrayName = currentKey.substring(0, indexOfArrayStart);
                    node.setChild(new AstNode(arrayName, Type.LIST, -1, failOnUnknownProperties));
                    node.getChild().setParent(node);
                    node = node.getChild();
                    currentKey = currentKey.substring(indexOfArrayStart);
                    continue;
                }

                if (++cursor >= keys.length) {
                    break;
                }

                currentKey = keys[cursor];
            }

            head.getChild().setParent(null);
            return head.getChild();
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        public int getArrayIndex() {
            return arrayIndex;
        }

        public void setArrayIndex(int arrayIndex) {
            this.arrayIndex = arrayIndex;
        }

        public boolean getFailOnUnknownProperties() {
            return failOnUnknownProperties;
        }

        public boolean isFailOnUnknownProperties() {
            return failOnUnknownProperties;
        }

        public void setFailOnUnknownProperties(boolean failOnUnknownProperties) {
            this.failOnUnknownProperties = failOnUnknownProperties;
        }

        public AstNode getChild() {
            return child;
        }

        public void setChild(AstNode child) {
            this.child = child;
        }

        public AstNode getParent() {
            return parent;
        }

        public void setParent(AstNode parent) {
            this.parent = parent;
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
            AstNode astNode = AstNode.parse(nameValuePair.getName(), nameValuePair.getFailOnUnknownProperties());
            setObjectFieldValue0(list, astNode, nameValuePair.getValue());
        }

        return list;
    }

    public static Map<String, Object> setObjectFieldValue(Map<String, Object> map, List<NameValuePair> nameValuePairs) {
        for (NameValuePair nameValuePair : nameValuePairs) {
            AstNode astNode = AstNode.parse(nameValuePair.getName(), nameValuePair.getFailOnUnknownProperties());
            setObjectFieldValue0(map, astNode, nameValuePair.getValue());
        }

        return map;
    }

    private static void setObjectFieldValue0(List<Object> list, final AstNode astNode, Object value) {
        if (astNode.getArrayIndex() == -1) {
            throw new RuntimeException("Try to put a map value, but the existing value is a list: " + astNode.getPath());
        }

        if (astNode.getArrayIndex() >= list.size()) {
            if (astNode.getFailOnUnknownProperties()) {
                throw new RuntimeException("Index out of bounds: " + astNode.getPath());
            }
            list.addAll(Collections.nCopies(astNode.getArrayIndex() - list.size() + 1, null));
            list.set(astNode.arrayIndex, astNode.newObject());
        }


        setChildValue(astNode, list, value);
    }

    private static void setChildValue(AstNode astNode, List<Object> l, Object value) {
        if (astNode.getChild() != null) {
            Object o = l.get(astNode.getArrayIndex());
            if (o == null) {
                o = astNode.newObject();
                l.set(astNode.getArrayIndex(), o);
            }

            if (o instanceof List) {
                setObjectFieldValue0((List<Object>) o, astNode.getChild(), value);
            } else {
                setObjectFieldValue0((Map<String, Object>) o, astNode.getChild(), value);
            }

        } else {
            l.set(astNode.getArrayIndex(), value);
        }

    }

    private static void setObjectFieldValue0(Map<String, Object> o, final AstNode astNode, Object value) {
        if (astNode.getName() == null) {
            throw new RuntimeException("Try to put a list value, but the existing value is a map: " + astNode.getPath());
        }

        if (!o.containsKey(astNode.getName()) && astNode.getFailOnUnknownProperties()) {
            throw new RuntimeException("Key not found: " + astNode.getPath());
        }

        setChildValue(astNode, o, value);
    }

    private static void setChildValue(AstNode astNode, Map<String, Object> map, Object value) {
        if (astNode.getChild() != null) {
            Object v = map.get(astNode.getName());
            if (v == null) {
                v = astNode.newObject();
                map.put(astNode.getName(), v);
            }

            if (v instanceof List) {
                setObjectFieldValue0((List<Object>) v, astNode.getChild(), value);
            } else {
                setObjectFieldValue0((Map<String, Object>) v, astNode.getChild(), value);
            }

        } else {
            map.put(astNode.getName(), value);
        }

    }
}

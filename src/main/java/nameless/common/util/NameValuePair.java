package nameless.common.util;

/**
 * name value pair with extra field failOnUnknownProperties.
 */
public class NameValuePair {
    private String name;
    private Object value;
    /**
     * Whether to fail on non-existing properties. Default is true.
     * if set to false, setting any non-existing properties will be successful.
     * else it will throw an exception immediately.
     */
    private boolean failOnUnknownProperties = true;

    public NameValuePair() {
    }

    public NameValuePair(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public NameValuePair(String name, Object value, boolean failOnUnknownProperties) {
        this.name = name;
        this.value = value;
        this.failOnUnknownProperties = failOnUnknownProperties;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
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
}
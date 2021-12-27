package nameless.common.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Collection;
import java.util.function.BiFunction;

public class GenerateFieldSerializer extends StdSerializer<Object> {

    private static final long serialVersionUID = -1590462861374128690L;

    private final GenerateField generateField;

    private final BiFunction<Object, String, ?> generateFunction;

    public GenerateFieldSerializer(GenerateField generateField, BiFunction<Object, String, ?> generateFunction) {
        super(Object.class);
        this.generateField = generateField;
        this.generateFunction = generateFunction;
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        Object generatedValue = generateFunction.apply(value, generateField.group());
        int i = 0;
        if (generateField.outputOriginalField()) {
            // 先输出原始字段与值
            write(gen, value);
        } else {
            /* 将 generateField.fieldName 的第一项当作输出的fieldName，
               输出fieldName的逻辑在 DynamicFieldAnnotationIntrospector.findNameForSerialization
               最终效果相当于将原始的字段名替换成了generateField.fieldName[0]
               也相当于原始字段加上@JsonIgnore
             */

            write(gen, generatedValue);
            i++;
        }

        // 如果!dynamicField.outputOriginalField()，因为前面已经输出过fieldName[0]，接下来只需要输出剩下的fieldNames
        String[] fieldNames = generateField.fieldName();
        for (; i < fieldNames.length; i++) {
            gen.writeFieldName(fieldNames[i]);
            write(gen, generatedValue);
        }

    }

    public void write(JsonGenerator gen, Object value) throws IOException {
        if (value == null) {
            gen.writeNull();
        } else if (value instanceof String) {
            gen.writeString((String) value);
        } else if (value instanceof Boolean) {
            gen.writeBoolean((Boolean) value);
        } else if (value instanceof Number) {
            gen.writeNumber(value.toString());
        } else if (value instanceof Collection) {
            gen.writeStartArray();
            for (Object e : (Collection<?>) value) {
                write(gen, e);
            }
            gen.writeEndArray();
        } else {
            gen.writeString(value.toString());
        }
    }
}

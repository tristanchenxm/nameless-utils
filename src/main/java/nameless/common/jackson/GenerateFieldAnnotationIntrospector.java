package nameless.common.jackson;

import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

import java.util.function.BiFunction;

public class GenerateFieldAnnotationIntrospector extends JacksonAnnotationIntrospector {
    private static final long serialVersionUID = -5730103369877027992L;

    private final BiFunction<Object, String, ?> generateFunction;

    public GenerateFieldAnnotationIntrospector(BiFunction<Object, String, ?> generateFunction) {
        this.generateFunction = generateFunction;
    }

    @Override
    public Object findSerializer(Annotated annotated) {
        GenerateField generateField = annotated.getAnnotation(GenerateField.class);
        if (generateField != null) {
            return new GenerateFieldSerializer(generateField, generateFunction);
        } else {
            return super.findSerializer(annotated);
        }
    }

    /**
     * 如果 !generateField.outputOriginalField()，则将 generateField.fieldName 的第一项当作输出的 fieldName，
     * 对于原字段相当于替换成了generateField.fieldName[0]
     */
    @Override
    public PropertyName findNameForSerialization(Annotated annotated) {
        GenerateField generateField = annotated.getAnnotation(GenerateField.class);
        if (generateField != null && !generateField.outputOriginalField()) {
            return PropertyName.construct(generateField.fieldName()[0]);
        } else {
            return super.findNameForSerialization(annotated);
        }
    }

}

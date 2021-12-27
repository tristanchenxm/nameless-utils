package nameless.common.jackson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注一个serialize操作时需要生成的动态字段
 */
@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface GenerateField {

    String DEFAULT_NONE = "\n\t\t\n\t\t\n\uE000\uE001\uE002\n\t\t\t\t\n";

    /**
     * 目标输出字段名
     */
    String[] fieldName();

    /**
     * 自定义分组，使用时可以根据分组做不同输出实现
     */
    String group() default DEFAULT_NONE;

    /**
     * 是否将原字段输出
     * 例如
     * <pre>{@code
     *  @GenerateField(fieldName = "g1", outputOriginalField = true)
     *  private String var1;
     *
     *  @GenerateField(fieldName = "g2", outputOriginalField = false)
     *  private String var2;
     * }
     * </pre>
     * 生成JSON字符串后，会包含 g1, var1 和 g2 三个字段，而不会有var2字段
     */
    boolean outputOriginalField() default true;
}


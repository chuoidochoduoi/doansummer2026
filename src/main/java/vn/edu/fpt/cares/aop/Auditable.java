package vn.edu.fpt.cares.aop;

import vn.edu.fpt.cares.enums.AuditAction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    AuditAction action();
    String entityName();
    /**
     * Name of the path variable that contains the entity ID (e.g., "id").
     * If not provided, the aspect will attempt to extract the ID from the return object.
     */
    String idParamName() default "";
    String description() default "";
}

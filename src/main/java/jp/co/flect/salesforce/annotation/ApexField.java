package jp.co.flect.salesforce.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * モデルのフィールドに付与するAnnotation
 * 引数はApexで定義したオブジェクトのフィールド名を示す
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApexField {
	String value() default "";
}

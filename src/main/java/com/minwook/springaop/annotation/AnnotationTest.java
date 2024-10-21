package com.minwook.springaop.annotation;


import java.lang.annotation.*;

// 어노테이션은 메서드에 적용되고 런타임까지 유지됨
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface AnnotationTest {
    //
}

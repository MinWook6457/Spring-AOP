package com.minwook.springaop.annotation;


import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME) // 런타임에도 어노테이션 사용가능(리플렉션)
public @interface AnnotationTest {
    //
}

package com.ibicd.mvc.annotaion;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CustRequestMapping {

    /**
     * 路径
     *
     * @return
     */
    String value();
}

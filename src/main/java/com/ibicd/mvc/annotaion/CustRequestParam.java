package com.ibicd.mvc.annotaion;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CustRequestParam {

    /**
     * 参数名
     *
     * @return
     */
    String value();
}

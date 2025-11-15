package com.sky.anno;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//生效的类
@Target(ElementType.METHOD)
//生效的属性
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoFill {
    OperationType value() ;
}

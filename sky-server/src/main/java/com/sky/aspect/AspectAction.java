package com.sky.aspect;

import com.sky.anno.AutoFill;
import com.sky.context.BaseContext;
import com.sky.entity.Employee;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.apache.ibatis.ognl.OgnlRuntime.setFieldValue;

@Slf4j
@Aspect
@Component
public class AspectAction {
    //切入点,service/impl包下的所有方法和带anno注解的方法
    @Pointcut("execution( * com.sky.mapper.*.*(..)) && @annotation(com.sky.anno.AutoFill)")
    public void pt(){}
    //通知
    @Before("pt()")
    public void before(JoinPoint joinPoint){
        log.info("公共字段填充");
        //1、从调用现场获取签名信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();//转换类型是为了获取具体方法参数
        //2、从签名信息中获取方法参数
        Method method = signature.getMethod();
        //3、看看方法上有没有AutoFill注解，有的话，就获取注解对象
        AutoFill autoFill = method.getAnnotation(AutoFill.class);
        //4、获取注解对象中定义的操作类型
        OperationType operationType = autoFill.value();
        //5、获取该方法的参数
        Object[] args = joinPoint.getArgs();
        //6、判断这个方法有没有参数，没有说明不需要填充
        if (args == null || args.length == 0){
            return;
        }
        //7、约定第一个参数是实体对象
        Object entity = args[0];
        //8、准备赋值的数据
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();
        //9、根据对应的操作类型，为对应的字段赋值
        try {
            if (operationType == OperationType.INSERT) {
                setField(entity, "createTime", now);
                setField(entity, "updateTime", now);
                setField(entity, "createUser", currentId);
                setField(entity, "updateUser", currentId);
            } else if (operationType == OperationType.UPDATE) {
                setField(entity, "updateTime", now);
                setField(entity, "updateUser", currentId);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * 通过反射给对象设置字段值
     */
    private void setField(Object entity, String fieldName, Object value) throws Exception {
        Class<?> clazz = entity.getClass();
        Field field = null;

        // 支持父类字段（比如公共的 BaseEntity）
        while (clazz != null) {
            try {
                field = clazz.getDeclaredField(fieldName);
                break;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }

        if (field == null) {
            // 这个实体没有这个字段，直接跳过
            log.debug("实体 {} 上不存在字段 {}，跳过填充", entity.getClass().getName(), fieldName);
            return;
        }

        field.setAccessible(true);
        field.set(entity, value);
    }







}

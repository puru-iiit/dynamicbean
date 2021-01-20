package com.puru.bean;

import java.util.List;

/**
 * A function to encapsulate custom logic in Java code and make it available in mapping formula
 */
public interface MapperFunction {

    public static final Class VOID_RETURN_TYPE = VoidReturnType.class;

    String getName();

    Class returnType();

    Object evaluate(Object[] parameters);

    List<Class> getParameterTypes();

    interface VoidReturnType {

    }
}

package com.puru.bean;

import java.util.Date;
import java.util.List;

@Mapper(setterAttribute = { "cutOffDate" })
public class DateMapper implements MapperFunction {

    public static final String NAME = "getCutOffDate";
    private String cutOffDate;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Class returnType() {
        return Date.class;
    }

    @Override
    public Date evaluate(final Object[] parameters) {
        return null;
    }

    @Override
    public List<Class> getParameterTypes() {
        return null;
    }
    public void setCutOffDate(String cutOffDate) {
        this.cutOffDate = cutOffDate;
    }
}


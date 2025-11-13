package org.xgen.lua.doc.generator.doc;

import java.util.ArrayList;
import java.util.List;

public class DocBlock {
    private LuaClass.Builder classBuilder;
    private LuaField.Builder typeBuilder;
    private List<LuaReturnValue.Builder> returnBuilder = new ArrayList<>();
    private List<LuaField.Builder> fields = new ArrayList<>();
    private List<LuaParameter.Builder> parameters = new ArrayList<>();
    private String description = null;
    private boolean hasNonStatic = false;
    private boolean isEnum = false;

    public void setClassBuilder(LuaClass.Builder classBuilder) {
        this.classBuilder = classBuilder;
    }

    public void setTypeBuilder(LuaField.Builder typeBuilder) {
        this.typeBuilder = typeBuilder;
    }

    public void addReturnBuilder(LuaReturnValue.Builder returnBuilder) {
        this.returnBuilder.add(returnBuilder);
    }

    public void addField(LuaField.Builder field) {
        fields.add(field);
    }

    public void addParameter(LuaParameter.Builder parameter) {
        parameters.add(parameter);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setHasNonStatic(boolean hasNonStatic) {
        this.hasNonStatic = hasNonStatic;
    }

    public void setIsEnum(boolean isEnum) {
        this.isEnum = isEnum;
    }

    public boolean isEnum() {
        return isEnum;
    }

    public boolean hasNonStatic() {
        return hasNonStatic;
    }

    public String getDescription() {
        return description;
    }

    public List<LuaParameter.Builder> getParameters() {
        return parameters;
    }

    public List<LuaField.Builder> getFields() {
        return fields;
    }

    public List<LuaReturnValue.Builder> getReturnBuilders() {
        return returnBuilder;
    }

    public LuaField.Builder getTypeBuilder() {
        return typeBuilder;
    }


    public LuaClass.Builder getClassBuilder() {
        return classBuilder;
    }
}

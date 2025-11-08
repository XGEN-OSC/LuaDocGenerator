package org.xgen.lua.doc.generator.doc;

import java.util.Optional;

public class LuaFieldBuilder implements LuaDocumentableObjectBuilder {
    private static record LuaFieldImpl(boolean isStatic, String name, String type, Optional<String> description)
            implements LuaField {
    }

    private boolean isStatic = true;
    private String name = null;
    private String type = null;
    private String description = null;

    public void setStatic(boolean isStatic) {
        this.isStatic = isStatic;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public LuaField build() {
        if (name == null)
            throw new IllegalStateException("Lua Field must have a name");
        if (type == null)
            throw new IllegalStateException("Lua Field must have a type");
        return new LuaFieldImpl(isStatic, name, type, Optional.ofNullable(description));
    }
}

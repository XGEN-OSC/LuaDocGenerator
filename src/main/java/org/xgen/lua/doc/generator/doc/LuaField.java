package org.xgen.lua.doc.generator.doc;

import java.util.Optional;

public interface LuaField extends LuaDocumentableObject {
    boolean isStatic();
    String name();
    String type();
    Optional<String> description();

    @Override
    default LuaDocumentableType documentableType() {
        return LuaDocumentableType.FIELD;
    }

    public record Impl(boolean isStatic, String name, String type, Optional<String> description) implements LuaField { }

    public class Builder implements org.xgen.lua.doc.generator.doc.Builder<LuaField> {
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

        public String getDescription() {
            return description;
        }

        public String getType() {
            return type;
        }
        
        public String getName() {
            return name;
        }

        public boolean isStatic() {
            return isStatic;
        }

        @Override
        public LuaField build() {
            if (name == null)
                throw new IllegalStateException("Lua Field must have a name");
            if (type == null)
                throw new IllegalStateException("Lua Field must have a type");
            return new LuaField.Impl(isStatic, name, type, Optional.ofNullable(description));
        }
    }
}

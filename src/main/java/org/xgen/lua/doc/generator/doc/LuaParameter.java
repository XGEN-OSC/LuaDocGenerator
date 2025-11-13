package org.xgen.lua.doc.generator.doc;

import java.util.Optional;

public interface LuaParameter {
    String name();
    String type();
    boolean optional();
    Optional<String> description();

    public record Impl(String name, String type, boolean optional, Optional<String> description) implements LuaParameter { }

    public static class Builder implements org.xgen.lua.doc.generator.doc.Builder<LuaParameter> {
        private String name = null;
        private String type = null;
        private boolean optional = false;
        private String description = null;

        public void setName(String name) {
            this.name = name;
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setOptional(boolean optional) {
            this.optional = optional;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public boolean isOptional() {
            return optional;
        }

        public String getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        @Override
        public LuaParameter build() {
            if (name == null)
                throw new IllegalStateException("Lua Parameter must have a name");
            if (type == null)
                throw new IllegalStateException("Lua Parameter must have a type");
            return new LuaParameter.Impl(name, type, optional, Optional.ofNullable(description));
        }
    }
}

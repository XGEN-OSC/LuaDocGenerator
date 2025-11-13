package org.xgen.lua.doc.generator.doc;

import java.util.Optional;

public interface LuaReturnValue {
    String type();
    String name();
    Optional<String> description();

    public record Impl(String type, String name, Optional<String> description) implements LuaReturnValue { }

    public class Builder implements org.xgen.lua.doc.generator.doc.Builder<LuaReturnValue> {
        private String type;
        private String name;
        private String description;

        public Builder setType(String type) {
            this.type = type;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public String getDescription() {
            return description;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public LuaReturnValue build() {
            return new Impl(type, name != null ? name : "", Optional.ofNullable(description));
        }
    }
}

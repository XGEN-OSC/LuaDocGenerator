package org.xgen.lua.doc.generator.doc;

import java.util.List;
import java.util.Optional;

public interface LuaClass extends LuaDocumentableObject {
    String name();
    Optional<String> description();
    List<LuaField> fields();
    List<LuaFunction> functions();

    @Override
    default LuaDocumentableType documentableType() {
        return LuaDocumentableType.CLASS;
    }

    public record Impl(String name, Optional<String> description, List<LuaField> fields, List<LuaFunction> functions) implements LuaClass { }

    public static class Builder implements org.xgen.lua.doc.generator.doc.Builder<LuaClass> {
        private String name;
        private String description;
        private final List<LuaField> fields = new java.util.ArrayList<>();
        private final List<LuaFunction> functions = new java.util.ArrayList<>();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder addField(LuaField field) {
            this.fields.add(field);
            return this;
        }

        public Builder addFunction(LuaFunction function) {
            this.functions.add(function);
            return this;
        }

        public List<LuaFunction> getFunctions() {
            return functions;
        }

        public List<LuaField> getFields() {
            return fields;
        }
        public String getDescription() {
            return description;
        }

        public String getName() {
            return name;
        }

        @Override
        public LuaClass build() {
            return new Impl(name, Optional.ofNullable(description), List.copyOf(fields), List.copyOf(functions));
        }
    }
}

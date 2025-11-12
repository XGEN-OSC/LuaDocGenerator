package org.xgen.lua.doc.generator.doc;

import java.util.List;
import java.util.Optional;

public interface LuaFunction extends LuaDocumentableObject {
    String name();
    boolean isStatic();
    Optional<String> description();
    List<LuaParameter> parameters();
    List<LuaReturnValue> returns();

    @Override
    default LuaDocumentableType documentableType() {
        return LuaDocumentableType.FUNCTION;
    }

    public record Impl(String name, boolean isStatic, Optional<String> description, List<LuaParameter> parameters, List<LuaReturnValue> returns) implements LuaFunction { }

    public static class Builder implements org.xgen.lua.doc.generator.doc.Builder<LuaFunction> {
        private String name;
        private boolean isStatic;
        private String description;
        private final List<LuaParameter> parameters = new java.util.ArrayList<>();
        private final List<LuaReturnValue> returns = new java.util.ArrayList<>();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder isStatic(boolean isStatic) {
            this.isStatic = isStatic;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder addParameter(LuaParameter parameter) {
            this.parameters.add(parameter);
            return this;
        }

        public Builder addReturnValue(LuaReturnValue returnValue) {
            this.returns.add(returnValue);
            return this;
        }

        @Override
        public LuaFunction build() {
            return new Impl(name, isStatic, Optional.ofNullable(description), List.copyOf(parameters), List.copyOf(returns));
        }
    }
}
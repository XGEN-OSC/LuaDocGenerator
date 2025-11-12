package org.xgen.lua.doc.generator.doc;

import java.util.Optional;

public interface LuaParameter {
    String name();
    String type();
    boolean optional();
    Optional<String> description();

    public record Impl(String name, String type, boolean optional, Optional<String> description) implements LuaParameter { }
}

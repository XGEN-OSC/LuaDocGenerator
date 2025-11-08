package org.xgen.lua.doc.generator.doc;

import java.util.Optional;

public interface LuaReturnValue {
    String type();
    String name();
    Optional<String> description();
}

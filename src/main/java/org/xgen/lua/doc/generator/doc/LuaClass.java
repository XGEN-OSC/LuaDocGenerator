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
}

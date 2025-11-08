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
}

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
}

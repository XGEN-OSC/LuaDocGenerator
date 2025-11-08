package org.xgen.lua.doc.generator.doc;

import java.util.List;

public record LuaNamespace(String name, List<LuaFunction> functions, List<LuaClass> classes, List<LuaField> fields) {
}

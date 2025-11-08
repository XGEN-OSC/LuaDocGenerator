package org.xgen.lua.doc.generator.write;

import org.xgen.lua.doc.generator.doc.*;

public class JsonExport {
    
    private final boolean prettyPrint;
    private final String indent;
    
    public JsonExport() {
        this(true);
    }
    
    public JsonExport(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
        this.indent = prettyPrint ? "  " : "";
    }
    
    /**
     * Export a LuaDoc to JSON format
     */
    public String export(LuaDoc doc) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        if (prettyPrint) json.append("\n");
        
        appendKey(json, "namespaces", 1);
        json.append("[");
        if (prettyPrint) json.append("\n");
        
        boolean firstNamespace = true;
        for (LuaNamespace namespace : doc.namespaces()) {
            if (!firstNamespace) {
                json.append(",");
                if (prettyPrint) json.append("\n");
            }
            firstNamespace = false;
            appendNamespace(json, namespace, 2);
        }
        
        if (prettyPrint) json.append("\n").append(indent(1));
        json.append("]");
        if (prettyPrint) json.append("\n");
        json.append("}");
        
        return json.toString();
    }
    
    private void appendNamespace(StringBuilder json, LuaNamespace namespace, int level) {
        json.append(indent(level)).append("{");
        if (prettyPrint) json.append("\n");
        
        appendKeyValue(json, "name", namespace.name(), level + 1);
        json.append(",");
        if (prettyPrint) json.append("\n");
        
        // Classes
        appendKey(json, "classes", level + 1);
        json.append("[");
        if (prettyPrint) json.append("\n");
        
        boolean firstClass = true;
        for (LuaClass clazz : namespace.classes()) {
            if (!firstClass) {
                json.append(",");
                if (prettyPrint) json.append("\n");
            }
            firstClass = false;
            appendClass(json, clazz, level + 2);
        }
        
        if (prettyPrint) json.append("\n").append(indent(level + 1));
        json.append("]");
        json.append(",");
        if (prettyPrint) json.append("\n");
        
        // Functions
        appendKey(json, "functions", level + 1);
        json.append("[");
        if (prettyPrint) json.append("\n");
        
        boolean firstFunc = true;
        for (LuaFunction function : namespace.functions()) {
            if (!firstFunc) {
                json.append(",");
                if (prettyPrint) json.append("\n");
            }
            firstFunc = false;
            appendFunction(json, function, level + 2);
        }
        
        if (prettyPrint) json.append("\n").append(indent(level + 1));
        json.append("]");
        json.append(",");
        if (prettyPrint) json.append("\n");
        
        // Fields
        appendKey(json, "fields", level + 1);
        json.append("[");
        if (prettyPrint) json.append("\n");
        
        boolean firstField = true;
        for (LuaField field : namespace.fields()) {
            if (!firstField) {
                json.append(",");
                if (prettyPrint) json.append("\n");
            }
            firstField = false;
            appendField(json, field, level + 2);
        }
        
        if (prettyPrint) json.append("\n").append(indent(level + 1));
        json.append("]");
        if (prettyPrint) json.append("\n");
        
        json.append(indent(level)).append("}");
    }
    
    private void appendClass(StringBuilder json, LuaClass clazz, int level) {
        json.append(indent(level)).append("{");
        if (prettyPrint) json.append("\n");
        
        appendKeyValue(json, "name", clazz.name(), level + 1);
        json.append(",");
        if (prettyPrint) json.append("\n");
        
        appendKey(json, "description", level + 1);
        if (clazz.description().isPresent()) {
            json.append(escapeJson(clazz.description().get()));
        } else {
            json.append("null");
        }
        json.append(",");
        if (prettyPrint) json.append("\n");
        
        // Fields
        appendKey(json, "fields", level + 1);
        json.append("[");
        if (prettyPrint) json.append("\n");
        
        boolean firstField = true;
        for (LuaField field : clazz.fields()) {
            if (!firstField) {
                json.append(",");
                if (prettyPrint) json.append("\n");
            }
            firstField = false;
            appendField(json, field, level + 2);
        }
        
        if (prettyPrint) json.append("\n").append(indent(level + 1));
        json.append("]");
        json.append(",");
        if (prettyPrint) json.append("\n");
        
        // Functions
        appendKey(json, "functions", level + 1);
        json.append("[");
        if (prettyPrint) json.append("\n");
        
        boolean firstFunc = true;
        for (LuaFunction function : clazz.functions()) {
            if (!firstFunc) {
                json.append(",");
                if (prettyPrint) json.append("\n");
            }
            firstFunc = false;
            appendFunction(json, function, level + 2);
        }
        
        if (prettyPrint) json.append("\n").append(indent(level + 1));
        json.append("]");
        if (prettyPrint) json.append("\n");
        
        json.append(indent(level)).append("}");
    }
    
    private void appendField(StringBuilder json, LuaField field, int level) {
        json.append(indent(level)).append("{");
        if (prettyPrint) json.append("\n");
        
        appendKeyValue(json, "name", field.name(), level + 1);
        json.append(",");
        if (prettyPrint) json.append("\n");
        
        appendKeyValue(json, "type", field.type(), level + 1);
        json.append(",");
        if (prettyPrint) json.append("\n");
        
        appendKey(json, "isStatic", level + 1);
        json.append(field.isStatic());
        json.append(",");
        if (prettyPrint) json.append("\n");
        
        appendKey(json, "description", level + 1);
        if (field.description().isPresent()) {
            json.append(escapeJson(field.description().get()));
        } else {
            json.append("null");
        }
        if (prettyPrint) json.append("\n");
        
        json.append(indent(level)).append("}");
    }
    
    private void appendFunction(StringBuilder json, LuaFunction function, int level) {
        json.append(indent(level)).append("{");
        if (prettyPrint) json.append("\n");
        
        appendKeyValue(json, "name", function.name(), level + 1);
        json.append(",");
        if (prettyPrint) json.append("\n");
        
        appendKey(json, "isStatic", level + 1);
        json.append(function.isStatic());
        json.append(",");
        if (prettyPrint) json.append("\n");
        
        appendKey(json, "description", level + 1);
        if (function.description().isPresent()) {
            json.append(escapeJson(function.description().get()));
        } else {
            json.append("null");
        }
        json.append(",");
        if (prettyPrint) json.append("\n");
        
        // Parameters
        appendKey(json, "parameters", level + 1);
        json.append("[");
        if (prettyPrint) json.append("\n");
        
        boolean firstParam = true;
        for (LuaParameter param : function.parameters()) {
            if (!firstParam) {
                json.append(",");
                if (prettyPrint) json.append("\n");
            }
            firstParam = false;
            appendParameter(json, param, level + 2);
        }
        
        if (prettyPrint) json.append("\n").append(indent(level + 1));
        json.append("]");
        json.append(",");
        if (prettyPrint) json.append("\n");
        
        // Returns
        appendKey(json, "returns", level + 1);
        json.append("[");
        if (prettyPrint) json.append("\n");
        
        boolean firstReturn = true;
        for (LuaReturnValue returnValue : function.returns()) {
            if (!firstReturn) {
                json.append(",");
                if (prettyPrint) json.append("\n");
            }
            firstReturn = false;
            appendReturnValue(json, returnValue, level + 2);
        }
        
        if (prettyPrint) json.append("\n").append(indent(level + 1));
        json.append("]");
        if (prettyPrint) json.append("\n");
        
        json.append(indent(level)).append("}");
    }
    
    private void appendParameter(StringBuilder json, LuaParameter param, int level) {
        json.append(indent(level)).append("{");
        if (prettyPrint) json.append("\n");
        
        appendKeyValue(json, "name", param.name(), level + 1);
        json.append(",");
        if (prettyPrint) json.append("\n");
        
        appendKeyValue(json, "type", param.type(), level + 1);
        json.append(",");
        if (prettyPrint) json.append("\n");
        
        appendKey(json, "optional", level + 1);
        json.append(param.optional());
        json.append(",");
        if (prettyPrint) json.append("\n");
        
        appendKey(json, "description", level + 1);
        if (param.description().isPresent()) {
            json.append(escapeJson(param.description().get()));
        } else {
            json.append("null");
        }
        if (prettyPrint) json.append("\n");
        
        json.append(indent(level)).append("}");
    }
    
    private void appendReturnValue(StringBuilder json, LuaReturnValue returnValue, int level) {
        json.append(indent(level)).append("{");
        if (prettyPrint) json.append("\n");
        
        appendKeyValue(json, "type", returnValue.type(), level + 1);
        json.append(",");
        if (prettyPrint) json.append("\n");
        
        appendKeyValue(json, "name", returnValue.name(), level + 1);
        json.append(",");
        if (prettyPrint) json.append("\n");
        
        appendKey(json, "description", level + 1);
        if (returnValue.description().isPresent()) {
            json.append(escapeJson(returnValue.description().get()));
        } else {
            json.append("null");
        }
        if (prettyPrint) json.append("\n");
        
        json.append(indent(level)).append("}");
    }
    
    private void appendKey(StringBuilder json, String key, int level) {
        json.append(indent(level)).append("\"").append(key).append("\":");
        if (prettyPrint) json.append(" ");
    }
    
    private void appendKeyValue(StringBuilder json, String key, String value, int level) {
        appendKey(json, key, level);
        json.append(escapeJson(value));
    }
    
    private String indent(int level) {
        if (!prettyPrint) return "";
        return indent.repeat(level);
    }
    
    private String escapeJson(String value) {
        if (value == null) {
            return "null";
        }
        
        StringBuilder escaped = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20 || c == 0x7F) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        escaped.append("\"");
        return escaped.toString();
    }
}

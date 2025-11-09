package org.xgen.lua.doc.generator.read;

import org.xgen.lua.doc.generator.doc.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocParser {
    
    // Patterns for parsing Lua doc comments
    private static final Pattern CLASS_PATTERN = Pattern.compile("---@class\\s+(\\S+)(?:\\s*:\\s*(\\S+))?\\s*(.*)");
    private static final Pattern FIELD_PATTERN = Pattern.compile("---@field\\s+(?:(private|public)\\s+)?(\\w+)\\s+((?:[\\w.|?]+(?:<[^>]+>)?)+)\\s*(.*)");
    private static final Pattern TYPE_PATTERN = Pattern.compile("---@type\\s+((?:[\\w.|?]+(?:<[^>]+>)?)+)(?:\\s+(.+))?");
    private static final Pattern PARAM_PATTERN = Pattern.compile("---@param\\s+(\\w+)\\s+((?:[\\w.|?]+(?:<[^>]+>)?)+)(?:\\s+(.+))?");
    private static final Pattern RETURN_DOC_PATTERN = Pattern.compile("---@return\\s+((?:[\\w.|?]+(?:<[^>]+>)?)+)(?:\\s+(\\w+))?(?:\\s+(.+))?");
    private static final Pattern ENUM_PATTERN = Pattern.compile("---@enum\\s+(\\S+)(?:\\s+(.+))?");
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("function\\s+(?:(\\w+(?:\\.\\w+)*)([.:]))?([\\w]+)\\s*\\(([^)]*)\\)");
    private static final Pattern ASSIGNMENT_PATTERN = Pattern.compile("(\\w+(?:\\.\\w+)*)\\s*=");
    private static final Pattern LOCAL_PATTERN = Pattern.compile("local\\s+");
    private static final Pattern RETURN_PATTERN = Pattern.compile("return\\s+");
    
    /**
     * Parse a Lua file and extract documentation
     */
    public LuaDoc parse(String luaContent) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(luaContent));
        Map<String, ClassBuilder> classes = new LinkedHashMap<>();
        List<FunctionBuilder> globalFunctions = new ArrayList<>();
        
        String line;
        List<String> commentBlock = new ArrayList<>();
        
        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();
            
            // Collect comment lines
            if (trimmed.startsWith("---")) {
                // Skip meta and example comments
                if (!trimmed.startsWith("---@meta") && !trimmed.startsWith("---@example")) {
                    commentBlock.add(trimmed);
                }
            } else if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                // Process the collected comments with the current line
                if (!commentBlock.isEmpty()) {
                    processDocBlock(commentBlock, trimmed, classes, globalFunctions, reader);
                    commentBlock.clear();
                } else {
                    // No doc comments, check for undocumented function
                    processUndocumentedFunction(trimmed, classes, globalFunctions, reader);
                }
            } else if (trimmed.isEmpty()) {
                // Empty line breaks the comment block
                if (!commentBlock.isEmpty()) {
                    // Process class/enum definitions that don't have code on the next line
                    DocBlock docBlock = parseCommentBlock(commentBlock);
                    if (docBlock.classInfo != null) {
                        String className = docBlock.classInfo.name;
                        ClassBuilder classBuilder = classes.computeIfAbsent(className, ClassBuilder::new);
                        classBuilder.setDescription(docBlock.description);
                        for (FieldInfo field : docBlock.fields) {
                            classBuilder.addField(createField(field, false));
                        }
                    } else if (docBlock.enumInfo != null) {
                        // Enums need the code line, so we can't process them here
                        // Keep the comment block for now
                        continue;
                    }
                    commentBlock.clear();
                }
            }
        }
        
        // Build final documentation
        List<LuaClass> classList = new ArrayList<>();
        for (ClassBuilder builder : classes.values()) {
            classList.add(builder.build());
        }
        
        // Build global functions
        List<LuaFunction> globalFunctionList = new ArrayList<>();
        for (FunctionBuilder builder : globalFunctions) {
            globalFunctionList.add(builder.build());
        }
        
        // Create namespace with all parsed elements
        List<LuaNamespace> namespaces = new ArrayList<>();
        namespaces.add(new LuaNamespace("global", globalFunctionList, classList, new ArrayList<>()));
        
        return new LuaDoc(namespaces);
    }
    
    private void processDocBlock(List<String> comments, String codeLine, 
                                  Map<String, ClassBuilder> classes,
                                  List<FunctionBuilder> globalFunctions,
                                  BufferedReader reader) throws IOException {
        
        DocBlock docBlock = parseCommentBlock(comments);
        
        if (docBlock.classInfo != null) {
            // Handle @class
            String className = docBlock.classInfo.name;
            ClassBuilder classBuilder = classes.computeIfAbsent(className, ClassBuilder::new);
            classBuilder.setDescription(docBlock.description);
            
            // Add fields defined in class comment
            for (FieldInfo field : docBlock.fields) {
                classBuilder.addField(createField(field, false));
            }
        } else if (docBlock.enumInfo != null) {
            // Handle @enum as a class
            String enumName = docBlock.enumInfo.name;
            ClassBuilder classBuilder = classes.computeIfAbsent(enumName, ClassBuilder::new);
            classBuilder.setDescription(docBlock.enumInfo.description);
            
            // Parse enum values (codeLine should contain the opening brace)
            parseEnumValues(reader, classBuilder, codeLine);
        } else if (docBlock.typeInfo != null) {
            // Handle @type for static fields
            Matcher assignMatcher = ASSIGNMENT_PATTERN.matcher(codeLine);
            if (assignMatcher.find()) {
                String fullName = assignMatcher.group(1);
                
                // Skip local variables
                if (LOCAL_PATTERN.matcher(codeLine).find()) {
                    return;
                }
                
                // Check if it's a class field
                String[] parts = fullName.split("\\.");
                if (parts.length == 2) {
                    String className = parts[0];
                    String fieldName = parts[1];
                    
                    ClassBuilder classBuilder = classes.computeIfAbsent(className, ClassBuilder::new);
                    FieldInfo fieldInfo = new FieldInfo();
                    fieldInfo.name = fieldName;
                    fieldInfo.type = docBlock.typeInfo.type;
                    fieldInfo.description = docBlock.typeInfo.description;
                    classBuilder.addField(createField(fieldInfo, true));
                }
            }
        } else {
            // Handle function
            // Skip local functions
            if (LOCAL_PATTERN.matcher(codeLine).find()) {
                return;
            }
            
            Matcher funcMatcher = FUNCTION_PATTERN.matcher(codeLine);
            if (funcMatcher.find()) {
                String className = funcMatcher.group(1);
                String separator = funcMatcher.group(2);
                String funcName = funcMatcher.group(3);
                String params = funcMatcher.group(4);
                
                FunctionBuilder funcBuilder = new FunctionBuilder(funcName);
                funcBuilder.setDescription(docBlock.description);
                
                // Determine if static (. = static, : = instance)
                boolean isStatic = separator == null || ".".equals(separator);
                funcBuilder.setStatic(isStatic);
                
                // Parse parameters
                List<String> paramNames = parseParameterNames(params);
                for (String paramName : paramNames) {
                    ParamInfo paramInfo = docBlock.params.stream()
                        .filter(p -> p.name.equals(paramName))
                        .findFirst()
                        .orElse(null);
                    
                    if (paramInfo != null) {
                        funcBuilder.addParameter(createParameter(paramInfo));
                    } else {
                        // No documentation, use "any" type
                        funcBuilder.addParameter(createParameter(paramName, "any", false, null));
                    }
                }
                
                // Add return values from documentation
                if (!docBlock.returns.isEmpty()) {
                    for (ReturnInfo returnInfo : docBlock.returns) {
                        funcBuilder.addReturn(createReturn(returnInfo.type, returnInfo.name, returnInfo.description));
                    }
                } else if (hasReturnStatement(codeLine, reader)) {
                    // No @return documentation but has return statement
                    funcBuilder.addReturn(createReturn("any", null, null));
                }
                
                // Add to class or global functions
                if (className != null) {
                    ClassBuilder classBuilder = classes.computeIfAbsent(className, ClassBuilder::new);
                    classBuilder.addFunction(funcBuilder.build());
                } else {
                    globalFunctions.add(funcBuilder);
                }
            }
        }
    }
    
    private void processUndocumentedFunction(String codeLine,
                                              Map<String, ClassBuilder> classes,
                                              List<FunctionBuilder> globalFunctions,
                                              BufferedReader reader) throws IOException {
        // Skip local functions
        if (LOCAL_PATTERN.matcher(codeLine).find()) {
            return;
        }
        
        Matcher funcMatcher = FUNCTION_PATTERN.matcher(codeLine);
        if (funcMatcher.find()) {
            String className = funcMatcher.group(1);
            String separator = funcMatcher.group(2);
            String funcName = funcMatcher.group(3);
            String params = funcMatcher.group(4);
            
            FunctionBuilder funcBuilder = new FunctionBuilder(funcName);
            funcBuilder.setDescription(null);
            
            // Determine if static
            boolean isStatic = separator == null || ".".equals(separator);
            funcBuilder.setStatic(isStatic);
            
            // Parse parameters - all type "any"
            List<String> paramNames = parseParameterNames(params);
            for (String paramName : paramNames) {
                funcBuilder.addParameter(createParameter(paramName, "any", false, null));
            }
            
            // Check if function has return statement
            if (hasReturnStatement(codeLine, reader)) {
                funcBuilder.addReturn(createReturn("any", null, null));
            }
            
            // Add to class or global functions
            if (className != null) {
                ClassBuilder classBuilder = classes.computeIfAbsent(className, ClassBuilder::new);
                classBuilder.addFunction(funcBuilder.build());
            } else {
                globalFunctions.add(funcBuilder);
            }
        }
    }
    
    private DocBlock parseCommentBlock(List<String> comments) {
        DocBlock block = new DocBlock();
        StringBuilder description = new StringBuilder();
        FieldInfo lastField = null;
        ParamInfo lastParam = null;
        
        for (String comment : comments) {
            String content = comment.substring(3).trim(); // Remove "---"
            
            if (content.startsWith("@class")) {
                lastField = null;
                lastParam = null;
                Matcher matcher = CLASS_PATTERN.matcher(comment);
                if (matcher.find()) {
                    block.classInfo = new ClassInfo();
                    block.classInfo.name = matcher.group(1);
                    block.classInfo.parent = matcher.group(2);
                    String desc = matcher.group(3);
                    if (desc != null && !desc.isEmpty()) {
                        description.append(desc);
                    }
                }
            } else if (content.startsWith("@field")) {
                lastParam = null;
                Matcher matcher = FIELD_PATTERN.matcher(comment);
                if (matcher.find()) {
                    FieldInfo field = new FieldInfo();
                    field.visibility = matcher.group(1);
                    field.name = matcher.group(2);  // name comes before type
                    field.type = matcher.group(3);  // type comes after name
                    field.description = matcher.group(4);
                    block.fields.add(field);
                    lastField = field;
                }
            } else if (content.startsWith("@type")) {
                lastField = null;
                lastParam = null;
                Matcher matcher = TYPE_PATTERN.matcher(comment);
                if (matcher.find()) {
                    block.typeInfo = new TypeInfo();
                    block.typeInfo.type = matcher.group(1);
                    block.typeInfo.description = matcher.group(2);
                }
            } else if (content.startsWith("@param")) {
                lastField = null;
                Matcher matcher = PARAM_PATTERN.matcher(comment);
                if (matcher.find()) {
                    ParamInfo param = new ParamInfo();
                    param.name = matcher.group(1);
                    param.type = matcher.group(2);
                    param.description = matcher.group(3);
                    block.params.add(param);
                    lastParam = param;
                }
            } else if (content.startsWith("@return")) {
                lastField = null;
                lastParam = null;
                Matcher matcher = RETURN_DOC_PATTERN.matcher(comment);
                if (matcher.find()) {
                    ReturnInfo returnInfo = new ReturnInfo();
                    returnInfo.type = matcher.group(1);
                    returnInfo.name = matcher.group(2);
                    returnInfo.description = matcher.group(3);
                    block.returns.add(returnInfo);
                }
            } else if (content.startsWith("@enum")) {
                lastField = null;
                lastParam = null;
                Matcher matcher = ENUM_PATTERN.matcher(comment);
                if (matcher.find()) {
                    block.enumInfo = new EnumInfo();
                    block.enumInfo.name = matcher.group(1);
                    block.enumInfo.description = matcher.group(2);
                }
            } else if (content.startsWith("@cast") || content.startsWith("@")) {
                // Ignore unrecognized annotations
                lastField = null;
                lastParam = null;
                continue;
            } else if (content.isEmpty()) {
                // Empty line - reset continuation context
                lastField = null;
                lastParam = null;
            } else {
                // Regular description line - append to the appropriate element
                if (lastField != null) {
                    // Append to last field's description
                    if (lastField.description == null || lastField.description.isEmpty()) {
                        lastField.description = content;
                    } else {
                        lastField.description += "\n" + content;
                    }
                } else if (lastParam != null) {
                    // Append to last param's description
                    if (lastParam.description == null || lastParam.description.isEmpty()) {
                        lastParam.description = content;
                    } else {
                        lastParam.description += "\n" + content;
                    }
                } else {
                    // Append to general description
                    if (description.length() > 0) {
                        description.append("\n");
                    }
                    description.append(content);
                }
            }
        }
        
        if (description.length() > 0) {
            block.description = description.toString();
        }
        
        return block;
    }
    
    private void parseEnumValues(BufferedReader reader, ClassBuilder classBuilder, String firstLine) throws IOException {
        String line;
        boolean inEnum = false;
        List<String> valueComments = new ArrayList<>();
        
        // Check if the opening brace is on the first line
        if (firstLine.contains("{")) {
            inEnum = true;
        }
        
        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();
            
            // Check for opening brace
            if (!inEnum && trimmed.contains("{")) {
                inEnum = true;
                // If there's content after the brace on same line, process it
                int braceIndex = trimmed.indexOf('{');
                if (braceIndex < trimmed.length() - 1) {
                    String afterBrace = trimmed.substring(braceIndex + 1).trim();
                    if (!afterBrace.isEmpty() && !afterBrace.startsWith("--")) {
                        processEnumValue(afterBrace, valueComments, classBuilder);
                    }
                }
                continue;
            }
            
            // Check for closing brace
            if (trimmed.startsWith("}") || trimmed.equals("}")) {
                break;
            }
            
            if (inEnum) {
                if (trimmed.startsWith("---@type")) {
                    valueComments.add(trimmed);
                } else if (trimmed.startsWith("---")) {
                    // Ignore other comments
                } else if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                    processEnumValue(trimmed, valueComments, classBuilder);
                }
            }
        }
    }
    
    private void processEnumValue(String line, List<String> valueComments, ClassBuilder classBuilder) {
        // Parse enum value - handle lines that might have commas and comments
        String cleanLine = line;
        
        // Remove trailing comma and any inline comments
        if (cleanLine.contains(",")) {
            cleanLine = cleanLine.substring(0, cleanLine.indexOf(","));
        }
        if (cleanLine.contains("--")) {
            cleanLine = cleanLine.substring(0, cleanLine.indexOf("--"));
        }
        cleanLine = cleanLine.trim();
        
        Matcher assignMatcher = Pattern.compile("(\\w+)\\s*=").matcher(cleanLine);
        if (assignMatcher.find()) {
            String valueName = assignMatcher.group(1);
            
            FieldInfo fieldInfo = new FieldInfo();
            fieldInfo.name = valueName;
            fieldInfo.type = "any";
            fieldInfo.description = null;
            
            // Check if there's a type annotation
            if (!valueComments.isEmpty()) {
                DocBlock valueBlock = parseCommentBlock(valueComments);
                if (valueBlock.typeInfo != null) {
                    fieldInfo.type = valueBlock.typeInfo.type;
                    fieldInfo.description = valueBlock.typeInfo.description;
                }
                valueComments.clear();
            }
            
            classBuilder.addField(createField(fieldInfo, false));
        }
    }
    
    private List<String> parseParameterNames(String params) {
        List<String> names = new ArrayList<>();
        if (params == null || params.trim().isEmpty()) {
            return names;
        }
        
        for (String param : params.split(",")) {
            String name = param.trim();
            if (!name.isEmpty()) {
                names.add(name);
            }
        }
        return names;
    }
    
    private boolean hasReturnStatement(String functionLine, BufferedReader reader) throws IOException {
        // This is a simplified check - in reality would need to parse the function body
        // For now, we'll just mark the reader and read ahead
        reader.mark(10000);
        String line;
        int braceDepth = 0;
        boolean foundReturn = false;
        boolean inFunction = false;
        
        // Include the function line
        if (functionLine.contains("return")) {
            foundReturn = true;
        }
        
        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();
            
            if (!inFunction && trimmed.contains("function")) {
                inFunction = true;
            }
            
            // Track depth for nested functions
            if (trimmed.startsWith("function")) {
                braceDepth++;
            }
            if (trimmed.startsWith("end")) {
                braceDepth--;
                if (braceDepth <= 0 && inFunction) {
                    break;
                }
            }
            
            if (RETURN_PATTERN.matcher(trimmed).find()) {
                foundReturn = true;
            }
        }
        
        reader.reset();
        return foundReturn;
    }
    
    private LuaField createField(FieldInfo info, boolean isStatic) {
        LuaFieldBuilder builder = new LuaFieldBuilder();
        builder.setName(info.name);
        builder.setType(info.type);
        builder.setStatic(isStatic);
        if (info.description != null && !info.description.isEmpty()) {
            builder.setDescription(info.description);
        }
        return builder.build();
    }
    
    private LuaParameter createParameter(ParamInfo info) {
        boolean optional = info.type.endsWith("?");
        String type = optional ? info.type.substring(0, info.type.length() - 1) : info.type;
        return createParameter(info.name, type, optional, info.description);
    }
    
    private LuaParameter createParameter(String name, String type, boolean optional, String description) {
        return new ParameterImpl(name, type, optional, Optional.ofNullable(description));
    }
    
    private LuaReturnValue createReturn(String type, String name, String description) {
        boolean optional = type.endsWith("?");
        String cleanType = optional ? type.substring(0, type.length() - 1) : type;
        return new ReturnValueImpl(cleanType, name != null ? name : "", Optional.ofNullable(description));
    }
    
    // Inner classes for parsing
    private static class DocBlock {
        String description;
        ClassInfo classInfo;
        List<FieldInfo> fields = new ArrayList<>();
        TypeInfo typeInfo;
        List<ParamInfo> params = new ArrayList<>();
        List<ReturnInfo> returns = new ArrayList<>();
        EnumInfo enumInfo;
    }
    
    private static class ClassInfo {
        String name;
        @SuppressWarnings("unused") // Reserved for future use
        String parent;
    }
    
    private static class FieldInfo {
        @SuppressWarnings("unused") // Reserved for future use
        String visibility;
        String type;
        String name;
        String description;
    }
    
    private static class TypeInfo {
        String type;
        String description;
    }
    
    private static class ParamInfo {
        String name;
        String type;
        String description;
    }
    
    private static class ReturnInfo {
        String type;
        String name;
        String description;
    }
    
    private static class EnumInfo {
        String name;
        String description;
    }
    
    // Builder classes
    private static class ClassBuilder {
        private final String name;
        private String description;
        private final List<LuaField> fields = new ArrayList<>();
        private final List<LuaFunction> functions = new ArrayList<>();
        
        ClassBuilder(String name) {
            this.name = name;
        }
        
        void setDescription(String description) {
            this.description = description;
        }
        
        void addField(LuaField field) {
            fields.add(field);
        }
        
        void addFunction(LuaFunction function) {
            functions.add(function);
        }
        
        LuaClass build() {
            return new ClassImpl(name, Optional.ofNullable(description), 
                                new ArrayList<>(fields), new ArrayList<>(functions));
        }
    }
    
    private static class FunctionBuilder {
        private final String name;
        private boolean isStatic = true;
        private String description;
        private final List<LuaParameter> parameters = new ArrayList<>();
        private final List<LuaReturnValue> returns = new ArrayList<>();
        
        FunctionBuilder(String name) {
            this.name = name;
        }
        
        void setStatic(boolean isStatic) {
            this.isStatic = isStatic;
        }
        
        void setDescription(String description) {
            this.description = description;
        }
        
        void addParameter(LuaParameter parameter) {
            parameters.add(parameter);
        }
        
        void addReturn(LuaReturnValue returnValue) {
            returns.add(returnValue);
        }
        
        LuaFunction build() {
            return new FunctionImpl(name, isStatic, Optional.ofNullable(description),
                                   new ArrayList<>(parameters), new ArrayList<>(returns));
        }
    }
    
    // Implementation classes
    private record ClassImpl(String name, Optional<String> description,
                            List<LuaField> fields, List<LuaFunction> functions) 
            implements LuaClass {
    }
    
    private record FunctionImpl(String name, boolean isStatic, Optional<String> description,
                               List<LuaParameter> parameters, List<LuaReturnValue> returns) 
            implements LuaFunction {
    }
    
    private record ParameterImpl(String name, String type, boolean optional, 
                                Optional<String> description) 
            implements LuaParameter {
    }
    
    private record ReturnValueImpl(String type, String name, Optional<String> description) 
            implements LuaReturnValue {
    }
}

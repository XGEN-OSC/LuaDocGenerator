package org.xgen.lua.doc.generator.process;

import org.jetbrains.annotations.NotNull;
import org.xgen.lua.doc.generator.doc.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocParser {
    private final BufferedReader reader;
    private final Set<String> localVariables = new HashSet<>();
    private final List<String> commentBlock = new ArrayList<>();
    private final Map<String, LuaClass.Builder> classes = new LinkedHashMap<>();
    private final List<LuaFunction.Builder> globalFunctions = new ArrayList<>();

    public DocParser(final @NotNull String luaContent) {
        this.reader = new BufferedReader(new StringReader(luaContent));
    }

    public LuaDoc parse() throws IOException {
        String line;
        
        while ((line = reader.readLine()) != null) {
            processLine(line, reader);
        }

        return build();
    }
    
    private LuaDoc build() {
        List<LuaClass> classList = new ArrayList<>();
        for (LuaClass.Builder builder : classes.values()) {
            classList.add(builder.build());
        }
        
        List<LuaFunction> globalFunctionList = new ArrayList<>();
        for (LuaFunction.Builder builder : globalFunctions) {
            globalFunctionList.add(builder.build());
        }
        
        List<LuaNamespace> namespaces = new ArrayList<>();
        namespaces.add(new LuaNamespace("global", globalFunctionList, classList, new ArrayList<>()));
        
        return new LuaDoc(namespaces);
    }

    private void processLine(final @NotNull String line, final @NotNull BufferedReader reader) throws IOException {
        String trimmed = line.trim();
        
        if (Patterns.LOCAL.matcher(trimmed).find()) {
            Matcher assignMatcher = Patterns.ASSIGNMENT.matcher(trimmed);
            if (assignMatcher.find()) {
                String varName = assignMatcher.group(1);
                localVariables.add(varName);
            }
        }
        
        if (Patterns.DOC_COMMENT.matcher(trimmed).find() && !Patterns.META.matcher(trimmed).find()) {
                commentBlock.add(trimmed);
        } else if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
            // Process the collected comments with the current line
            if (!commentBlock.isEmpty()) {
                processDocBlock(commentBlock, trimmed, classes, globalFunctions, localVariables, reader);
                commentBlock.clear();
            } else {
                // No doc comments, check for undocumented function
                processUndocumentedFunction(trimmed, classes, globalFunctions, localVariables, reader);
            }
        } else if (trimmed.isEmpty()) {
            // Empty line breaks the comment block
            if (!commentBlock.isEmpty()) {
                // Process class/enum definitions that don't have code on the next line
                DocBlock docBlock = parseCommentBlock(commentBlock);
                if (docBlock.getClassBuilder() != null) {
                    String className = docBlock.getClassBuilder().getName();
                    LuaClass.Builder classBuilder = classes.getOrDefault(className, new LuaClass.Builder());
                    classBuilder.name(className);
                    classBuilder.description(docBlock.getDescription());
                    classes.put(className, classBuilder);
                    for (LuaField.Builder field : docBlock.getFields()) {
                        classBuilder.addField(field.build());
                    }
                } else if (docBlock.getClassBuilder() != null) {
                    return;
                }
                commentBlock.clear();
            }
        }
    }

    private void processDocBlock(List<String> comments, String codeLine, 
                                  Map<String, LuaClass.Builder> classes,
                                  List<LuaFunction.Builder> globalFunctions,
                                  Set<String> localVariables,
                                  BufferedReader reader) throws IOException {
        
        DocBlock docBlock = parseCommentBlock(comments);
        
        if (docBlock.getClassBuilder() != null && !docBlock.isEnum()) {
            // Handle @class
            String className = docBlock.getClassBuilder().getName();
            LuaClass.Builder classBuilder = classes.getOrDefault(className, new LuaClass.Builder());
            classes.put(className, classBuilder);
            classBuilder.name(className);
            classBuilder.description(docBlock.getDescription());
            for (LuaField.Builder field : docBlock.getFields()) {
                classBuilder.addField(field.build());
            }
        } else if (docBlock.getClassBuilder() != null && docBlock.isEnum()) {
            // Handle @enum as a class
            String enumName = docBlock.getClassBuilder().getName();
            LuaClass.Builder classBuilder = classes.getOrDefault(enumName, new LuaClass.Builder());
            classes.put(enumName, classBuilder);
            classBuilder.name(enumName);
            classBuilder.description(docBlock.getDescription());
            
            // Parse enum values (codeLine should contain the opening brace)
            parseEnumValues(reader, classBuilder, codeLine);
        } else if (docBlock.getTypeBuilder() != null) {
            // Handle @type for static fields
            Matcher assignMatcher = Patterns.ASSIGNMENT.get().matcher(codeLine);
            if (assignMatcher.find()) {
                String fullName = assignMatcher.group(1);
                
                // Skip local variables
                if (Patterns.LOCAL.get().matcher(codeLine).find()) {
                    return;
                }
                
                // Check if it's a class field
                String[] parts = fullName.split("\\.");
                if (parts.length == 2) {
                    String className = parts[0];
                    String fieldName = parts[1];
                    
                    // Skip if the class is a local variable
                    if (localVariables.contains(className)) {
                        return;
                    }
                    
                    LuaClass.Builder classBuilder = classes.getOrDefault(className, new LuaClass.Builder());
                    classes.put(className, classBuilder);
                    classBuilder.name(className);
                    LuaField.Builder fieldBuilder = new LuaField.Builder();
                    fieldBuilder.setName(fieldName);
                    fieldBuilder.setType(docBlock.getTypeBuilder().getType());
                    fieldBuilder.setDescription(docBlock.getTypeBuilder().getDescription());
                    classBuilder.addField(fieldBuilder.build());
                }
            }
        } else {
            // Handle function
            // Skip local functions
            if (Patterns.LOCAL.get().matcher(codeLine).find()) {
                return;
            }
            
            Matcher funcMatcher = Patterns.FUNCTION.get().matcher(codeLine);
            if (funcMatcher.find()) {
                String className = funcMatcher.group(1);
                String separator = funcMatcher.group(2);
                String funcName = funcMatcher.group(3);
                String params = funcMatcher.group(4);
                
                // Skip if the function belongs to a local variable
                if (className != null && localVariables.contains(className.split("\\.")[0])) {
                    return;
                }
                
                LuaFunction.Builder funcBuilder = new LuaFunction.Builder().name(funcName);
                funcBuilder.description(docBlock.getDescription());
                
                // Check for @non-static or @none-static annotation
                boolean hasNonStaticAnnotation = docBlock.hasNonStatic();
                
                // Determine if static (. = static, : = instance)
                boolean isStatic;
                if (hasNonStaticAnnotation) {
                    // Explicit @non-static annotation overrides separator
                    isStatic = false;
                } else {
                    // Use separator to determine (. = static, : = instance, null = static)
                    isStatic = separator == null || ".".equals(separator);
                }
                funcBuilder.isStatic(isStatic);
                
                // Parse parameters
                List<String> paramNames = parseParameterNames(params);
                for (String paramName : paramNames) {
                    LuaParameter.Builder paramInfo = docBlock.getParameters().stream()
                        .filter(p -> p.getName().equals(paramName))
                        .findFirst()
                        .orElse(null);
                    
                    if (paramInfo != null) {
                        funcBuilder.addParameter(paramInfo.build());
                    } else {
                        LuaParameter.Builder paramBuilder = new LuaParameter.Builder();
                        paramBuilder.setName(paramName);
                        funcBuilder.addParameter(paramBuilder.build());
                    }
                }
                
                // Add return values from documentation
                if (!docBlock.getReturnBuilders().isEmpty()) {
                    for (LuaReturnValue.Builder returnBuilder : docBlock.getReturnBuilders()) {
                        funcBuilder.addReturnValue(returnBuilder.build());
                    }
                } else if (hasReturnStatement(codeLine, reader)) {
                    LuaReturnValue.Builder returnBuilder = new LuaReturnValue.Builder();
                    returnBuilder.setType("any");
                    funcBuilder.addReturnValue(returnBuilder.build());
                }
                // If neither @return nor return statement exists, don't add any return value
                
                // Add to class or global functions
                if (className != null) {
                    LuaClass.Builder classBuilder = classes.getOrDefault(className, new LuaClass.Builder());
                    classes.put(className, classBuilder);
                    classBuilder.name(className);
                    classBuilder.addFunction(funcBuilder.build());
                } else {
                    globalFunctions.add(funcBuilder);
                }
            }
        }
    }
    
    private void processUndocumentedFunction(String codeLine,
                                              Map<String, LuaClass.Builder> classes,
                                              List<LuaFunction.Builder> globalFunctions,
                                              Set<String> localVariables,
                                              BufferedReader reader) throws IOException {
        // Skip local functions
        if (Patterns.LOCAL.get().matcher(codeLine).find()) {
            return;
        }
        
        Matcher funcMatcher = Patterns.FUNCTION.get().matcher(codeLine);
        if (funcMatcher.find()) {
            String className = funcMatcher.group(1);
            String separator = funcMatcher.group(2);
            String funcName = funcMatcher.group(3);
            String params = funcMatcher.group(4);
            
            // Skip if the function belongs to a local variable
            if (className != null && localVariables.contains(className.split("\\.")[0])) {
                return;
            }
            
            LuaFunction.Builder funcBuilder = new LuaFunction.Builder().name(funcName);
            funcBuilder.description(null);
            
            // Determine if static
            boolean isStatic = separator == null || ".".equals(separator);
            funcBuilder.isStatic(isStatic);
            
            // Parse parameters - all type "any"
            List<String> paramNames = parseParameterNames(params);
            for (String paramName : paramNames) {
                LuaParameter.Builder paramBuilder = new LuaParameter.Builder();
                paramBuilder.setName(paramName);
                paramBuilder.setType("any");
                funcBuilder.addParameter(paramBuilder.build());
            }
            
            // Only add return value if function actually has a return statement
            if (hasReturnStatement(codeLine, reader)) {
                LuaReturnValue.Builder returnBuilder = new LuaReturnValue.Builder();
                returnBuilder.setType("any");
                funcBuilder.addReturnValue(returnBuilder.build());
            }
            // If no return statement exists, don't add any return value
            
            // Add to class or global functions
            if (className != null) {
                LuaClass.Builder classBuilder = classes.getOrDefault(className, new LuaClass.Builder());
                classes.put(className, classBuilder);
                classBuilder.name(className);
                classBuilder.addFunction(funcBuilder.build());
            } else {
                globalFunctions.add(funcBuilder);
            }
        }
    }
    
    private DocBlock parseCommentBlock(List<String> comments) {
        DocBlock block = new DocBlock();
        StringBuilder description = new StringBuilder();
        LuaField.Builder lastField = null;
        LuaParameter.Builder lastParam = null;
        
        for (String comment : comments) { 
            Matcher matcher = Patterns.CLASS.get().matcher(comment);
            if (matcher.find()) {
                lastField = null;
                lastParam = null;
                LuaClass.Builder classBuilder = new LuaClass.Builder();
                block.setClassBuilder(classBuilder);
                classBuilder.name(matcher.group(1));
                String desc = matcher.group(3);
                if (desc != null && !desc.isEmpty()) {
                    description.append(desc);
                }
                continue;
            }
            matcher = Patterns.ENUM.get().matcher(comment);
            if (matcher.find()) {
                lastField = null;
                lastParam = null;
                LuaClass.Builder classBuilder = new LuaClass.Builder();
                block.setClassBuilder(classBuilder);
                block.setIsEnum(true);
                classBuilder.name(matcher.group(1));
                String desc = matcher.group(3);
                if (desc != null && !desc.isEmpty()) {
                    description.append(desc);
                }
                continue;
            }

            matcher = Patterns.FIELD.get().matcher(comment);
            if (matcher.find()) {
                lastParam = null;
                LuaField.Builder fieldBuilder = new LuaField.Builder();
                fieldBuilder.setStatic(false);
                fieldBuilder.setName(matcher.group(2));
                fieldBuilder.setType(matcher.group(3));
                fieldBuilder.setDescription(matcher.group(4));
                block.addField(fieldBuilder);
                lastField = fieldBuilder;
                continue;
            }

            matcher = Patterns.TYPE.get().matcher(comment);
            if (matcher.find()) {
                lastField = null;
                lastParam = null;
                LuaField.Builder fieldBuilder = new LuaField.Builder();
                fieldBuilder.setType(matcher.group(1));
                fieldBuilder.setDescription(matcher.group(2));
                block.setTypeBuilder(fieldBuilder);
                continue;
            }

            matcher = Patterns.PARAM.get().matcher(comment);
            if (matcher.find()) {
                lastField = null;

                LuaParameter.Builder paramBuilder = new LuaParameter.Builder();
                paramBuilder.setName(matcher.group(1));
                paramBuilder.setType(matcher.group(2));
                paramBuilder.setDescription(matcher.group(3));
                block.addParameter(paramBuilder);
                
                lastParam = paramBuilder;
                continue;
            }
            
            matcher = Patterns.RETURN_DOC.get().matcher(comment);
            if (matcher.find()) {
                lastField = null;
                lastParam = null;
                
                LuaReturnValue.Builder returnBuilder = new LuaReturnValue.Builder();
                returnBuilder.setType(matcher.group(1));
                returnBuilder.setName(matcher.group(2));
                returnBuilder.setDescription(matcher.group(3));
                block.addReturnBuilder(returnBuilder);
                
                continue;
            }
            
            matcher = Patterns.NON_STATIC.get().matcher(comment);
            if (matcher.find()) {
                block.setHasNonStatic(true);
                lastField = null;
                lastParam = null;
                continue;
            }
            
            matcher = Patterns.ANY_ANNOTATION.get().matcher(comment);
            if (matcher.find()) {
                // Ignore unrecognized annotations
                lastField = null;
                lastParam = null;
                continue;
            }
            
            if (comment.isEmpty()) {
                lastField = null;
                lastParam = null;
                continue;
            }

            if (lastField != null) {
                String desc = lastField.getDescription() != null ? lastField.getDescription() : "";
                desc += (desc.isEmpty() ? "" : "\n") + comment;
                lastField.setDescription(desc);
                continue;
            } 
            
            if (lastParam != null) {
                String desc = lastParam.getDescription() != null ? lastParam.getDescription() : "";
                desc += (desc.isEmpty() ? "" : "\n") + comment;
                lastParam.setDescription(desc);
                continue;
            }

            if (description.length() > 0) {
                description.append("\n");
            }

            description.append(comment.substring(3).trim());
        }
        
        if (description.length() > 0) {
            block.setDescription(description.toString());
        }
        
        return block;
    }
    
    private void parseEnumValues(BufferedReader reader, LuaClass.Builder classBuilder, String firstLine) {
        String line;
        List<String> valueComments = new ArrayList<>();
        boolean inEnum = false;
        
        try {
            // Check if opening brace is on the first line
            if (firstLine.contains("{")) {
                inEnum = true;
                int braceIndex = firstLine.indexOf('{');
                if (braceIndex < firstLine.length() - 1) {
                    String afterBrace = firstLine.substring(braceIndex + 1).trim();
                    if (!afterBrace.isEmpty() && !afterBrace.startsWith("--")) {
                        processEnumValue(afterBrace, valueComments, classBuilder);
                    }
                }
            }
            
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                
                // Check for opening brace if not already in enum
                if (!inEnum && trimmed.contains("{")) {
                    inEnum = true;
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void processEnumValue(String line, List<String> valueComments, LuaClass.Builder classBuilder) {
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
            
            LuaField.Builder fieldBuilder = new LuaField.Builder();
            fieldBuilder.setName(valueName);
            fieldBuilder.setType("any");
            
            if (!valueComments.isEmpty()) {
                DocBlock valueBlock = parseCommentBlock(valueComments);
                if (valueBlock.getTypeBuilder() != null) {
                    fieldBuilder.setType(valueBlock.getTypeBuilder().getType());
                    fieldBuilder.setDescription(valueBlock.getTypeBuilder().getDescription());
                }
                valueComments.clear();
            }
            
            classBuilder.addField(fieldBuilder.build());
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
        // The function line has already been read, so we start looking from the body
        reader.mark(10000);
        String line;
        boolean foundReturn = false;
        
        // Check the function line itself (after the function declaration)
        String cleanFunctionLine = functionLine;
        if (cleanFunctionLine.contains("--")) {
            cleanFunctionLine = cleanFunctionLine.substring(0, cleanFunctionLine.indexOf("--"));
        }
        // Check if there's a return statement on the same line as the function declaration
        int functionIndex = cleanFunctionLine.indexOf("function");
        if (functionIndex >= 0) {
            String afterFunction = cleanFunctionLine.substring(functionIndex);
            if (Patterns.RETURN.get().matcher(afterFunction).find()) {
                foundReturn = true;
            }
        }
        
        // Now read the function body until we hit 'end'
        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();
            
            // Skip comment-only lines
            if (trimmed.startsWith("--")) {
                continue;
            }
            
            // Remove inline comments before checking
            String codePart = trimmed;
            if (codePart.contains("--")) {
                codePart = codePart.substring(0, codePart.indexOf("--")).trim();
            }
            
            // Stop at the first 'end' (this closes our function)
            if (codePart.equals("end")) {
                break;
            }
            
            // Check for return statement in code (not in comments)
            if (Patterns.RETURN.get().matcher(codePart).find()) {
                foundReturn = true;
            }
        }
        
        reader.reset();
        return foundReturn;
    }
}

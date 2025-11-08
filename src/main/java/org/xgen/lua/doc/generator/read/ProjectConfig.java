package org.xgen.lua.doc.generator.read;

import org.xgen.lua.doc.generator.doc.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProjectConfig {
    
    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\\[([^\\]]+)\\]");
    private static final Pattern FILE_PATH_PATTERN = Pattern.compile("\"([^\"]+)\"");
    
    /**
     * Parse a project configuration JSON file and generate documentation
     */
    public LuaDoc parseProject(String jsonConfigPath) throws IOException {
        String jsonContent = Files.readString(Paths.get(jsonConfigPath));
        Map<String, List<String>> namespaceFiles = parseJsonConfig(jsonContent);
        
        Path basePath = Paths.get(jsonConfigPath).getParent();
        if (basePath == null) {
            basePath = Paths.get(".");
        }
        
        DocParser parser = new DocParser();
        List<LuaNamespace> namespaces = new ArrayList<>();
        
        for (Map.Entry<String, List<String>> entry : namespaceFiles.entrySet()) {
            String namespaceName = entry.getKey();
            List<String> files = entry.getValue();
            
            // Parse all files for this namespace
            List<LuaClass> allClasses = new ArrayList<>();
            List<LuaFunction> allFunctions = new ArrayList<>();
            List<LuaField> allFields = new ArrayList<>();
            
            for (String relativeFilePath : files) {
                Path fullPath = basePath.resolve(relativeFilePath);
                
                if (!Files.exists(fullPath)) {
                    System.err.println("Warning: File not found: " + fullPath);
                    continue;
                }
                
                String luaContent = Files.readString(fullPath);
                LuaDoc doc = parser.parse(luaContent);
                
                // Extract all elements from the parsed document
                for (LuaNamespace ns : doc.namespaces()) {
                    allClasses.addAll(ns.classes());
                    allFunctions.addAll(ns.functions());
                    allFields.addAll(ns.fields());
                }
            }
            
            // Create namespace with all collected elements
            LuaNamespace namespace = new LuaNamespace(namespaceName, allFunctions, allClasses, allFields);
            namespaces.add(namespace);
        }
        
        return new LuaDoc(namespaces);
    }
    
    /**
     * Parse a simple JSON configuration without using a full JSON library
     */
    private Map<String, List<String>> parseJsonConfig(String jsonContent) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        
        // Remove whitespace and newlines for easier parsing
        String normalized = jsonContent.replaceAll("\\s+", " ").trim();
        
        // Find all arrays in the JSON
        Matcher arrayMatcher = JSON_ARRAY_PATTERN.matcher(normalized);
        
        while (arrayMatcher.find()) {
            String namespaceName = arrayMatcher.group(1);
            String arrayContent = arrayMatcher.group(2);
            
            List<String> files = new ArrayList<>();
            Matcher fileMatcher = FILE_PATH_PATTERN.matcher(arrayContent);
            
            while (fileMatcher.find()) {
                String filePath = fileMatcher.group(1);
                files.add(filePath);
            }
            
            result.put(namespaceName, files);
        }
        
        return result;
    }
}

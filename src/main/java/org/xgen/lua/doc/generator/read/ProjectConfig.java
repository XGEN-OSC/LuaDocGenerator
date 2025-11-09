package org.xgen.lua.doc.generator.read;

import org.xgen.lua.doc.generator.doc.*;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
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
                // Check if the path contains glob patterns
                if (relativeFilePath.contains("*") || relativeFilePath.contains("?")) {
                    // Expand glob pattern
                    List<Path> matchingFiles = expandGlobPattern(basePath, relativeFilePath);
                    for (Path fullPath : matchingFiles) {
                        parseAndCollectFile(fullPath, parser, allClasses, allFunctions, allFields);
                    }
                } else {
                    // Regular file path
                    Path fullPath = basePath.resolve(relativeFilePath);
                    parseAndCollectFile(fullPath, parser, allClasses, allFunctions, allFields);
                }
            }
            
            // Create namespace with all collected elements
            LuaNamespace namespace = new LuaNamespace(namespaceName, allFunctions, allClasses, allFields);
            namespaces.add(namespace);
        }
        
        return new LuaDoc(namespaces);
    }
    
    /**
     * Parse a single file and collect its documentation elements
     */
    private void parseAndCollectFile(Path fullPath, DocParser parser, 
                                     List<LuaClass> allClasses, 
                                     List<LuaFunction> allFunctions, 
                                     List<LuaField> allFields) throws IOException {
        if (!Files.exists(fullPath)) {
            System.err.println("Warning: File not found: " + fullPath);
            return;
        }
        
        if (!Files.isRegularFile(fullPath)) {
            return;
        }
        
        String luaContent = Files.readString(fullPath);
        LuaDoc doc = parser.parse(luaContent);
        
        // Extract all elements from the parsed document
        for (LuaNamespace ns : doc.namespaces()) {
            // Merge classes instead of just adding them
            for (LuaClass newClass : ns.classes()) {
                mergeClass(allClasses, newClass);
            }
            allFunctions.addAll(ns.functions());
            allFields.addAll(ns.fields());
        }
    }
    
    /**
     * Merge a class into the list of classes. If a class with the same name already exists,
     * merge their fields and functions. Otherwise, add the new class.
     */
    private void mergeClass(List<LuaClass> allClasses, LuaClass newClass) {
        // Find existing class with the same name (EXACT match only)
        int existingIndex = -1;
        
        for (int i = 0; i < allClasses.size(); i++) {
            if (allClasses.get(i).name().equals(newClass.name())) {
                existingIndex = i;
                break;
            }
        }
        
        if (existingIndex >= 0) {
            // Merge the classes
            LuaClass existingClass = allClasses.get(existingIndex);
            
            // Combine fields (avoiding duplicates by name)
            Map<String, LuaField> fieldMap = new LinkedHashMap<>();
            for (LuaField field : existingClass.fields()) {
                fieldMap.put(field.name(), field);
            }
            for (LuaField field : newClass.fields()) {
                fieldMap.putIfAbsent(field.name(), field);
            }
            List<LuaField> mergedFields = new ArrayList<>(fieldMap.values());
            
            // Combine functions (avoiding duplicates by name)
            Map<String, LuaFunction> functionMap = new LinkedHashMap<>();
            for (LuaFunction func : existingClass.functions()) {
                functionMap.put(func.name(), func);
            }
            for (LuaFunction func : newClass.functions()) {
                functionMap.putIfAbsent(func.name(), func);
            }
            List<LuaFunction> mergedFunctions = new ArrayList<>(functionMap.values());
            
            // Use the first non-empty description
            Optional<String> mergedDescription = existingClass.description().isPresent() 
                ? existingClass.description() 
                : newClass.description();
            
            // Create merged class
            LuaClass mergedClass = new MergedClass(
                existingClass.name(),
                mergedDescription,
                mergedFields,
                mergedFunctions
            );
            
            // Replace the existing class
            allClasses.set(existingIndex, mergedClass);
        } else {
            // No existing class, just add the new one
            allClasses.add(newClass);
        }
    }
    
    /**
     * A simple implementation of LuaClass for merged classes
     */
    private record MergedClass(
        String name,
        Optional<String> description,
        List<LuaField> fields,
        List<LuaFunction> functions
    ) implements LuaClass {
    }
    
    /**
     * Expand glob patterns like "server/**​/*.lua" to actual file paths
     */
    private List<Path> expandGlobPattern(Path basePath, String globPattern) throws IOException {
        List<Path> matchingFiles = new ArrayList<>();
        
        // Create a PathMatcher for the glob pattern
        String pattern = "glob:" + globPattern;
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher(pattern);
        
        // Determine the starting directory for traversal
        // Extract the non-glob prefix (e.g., "server" from "server/**​/*.lua")
        String[] parts = globPattern.split("[*?]", 2);
        String startDir = parts[0];
        // Remove trailing slash if present
        if (startDir.endsWith("/") || startDir.endsWith("\\")) {
            startDir = startDir.substring(0, startDir.length() - 1);
        }
        
        Path searchRoot = startDir.isEmpty() ? basePath : basePath.resolve(startDir);
        
        if (!Files.exists(searchRoot)) {
            System.err.println("Warning: Directory not found: " + searchRoot);
            return matchingFiles;
        }
        
        // Walk the file tree and find matching files
        Files.walkFileTree(searchRoot, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                // Get relative path from basePath
                Path relativePath = basePath.relativize(file);
                // Normalize to use forward slashes for matching
                String relativePathStr = relativePath.toString().replace('\\', '/');
                
                // Check if the relative path matches the glob pattern
                Path relativePathForMatching = Paths.get(relativePathStr);
                if (matcher.matches(relativePathForMatching)) {
                    matchingFiles.add(file);
                } else if (globPattern.contains("**/")) {
                    // For patterns like "client/**/*.lua", the ** should match zero or more directories
                    // So we also need to check if it matches with ** removed (zero directories case)
                    // E.g. "client/**/*.lua" should also match "client/*.lua"
                    String zeroDepthPattern = globPattern.replace("**/", "");
                    PathMatcher zeroDepthMatcher = FileSystems.getDefault().getPathMatcher("glob:" + zeroDepthPattern);
                    if (zeroDepthMatcher.matches(relativePathForMatching)) {
                        matchingFiles.add(file);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                System.err.println("Warning: Failed to access: " + file);
                return FileVisitResult.CONTINUE;
            }
        });
        
        return matchingFiles;
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

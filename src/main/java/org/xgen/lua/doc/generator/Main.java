package org.xgen.lua.doc.generator;

import org.xgen.lua.doc.generator.doc.LuaDoc;
import org.xgen.lua.doc.generator.process.DocParser;
import org.xgen.lua.doc.generator.read.ProjectConfig;
import org.xgen.lua.doc.generator.write.JsonExport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Usage:");
            System.err.println("  Single file mode: java Main <lua-file> [output-json-file]");
            System.err.println("  Project mode:     java Main --project <config-json> [output-json-file]");
            System.err.println();
            System.err.println("Project config JSON format:");
            System.err.println("  {");
            System.err.println("    \"namespace1\": [\"file1.lua\", \"file2.lua\"],");
            System.err.println("    \"namespace2\": [\"file3.lua\"]");
            System.err.println("  }");
            System.exit(1);
        }
        
        LuaDoc doc;
        
        // Check if using project mode
        if (args[0].equals("--project") || args[0].equals("-p")) {
            if (args.length < 2) {
                System.err.println("Error: --project flag requires a config file path");
                System.exit(1);
            }
            
            String configPath = args[1];
            System.out.println("Parsing project from config: " + configPath);
            
            ProjectConfig projectConfig = new ProjectConfig();
            doc = projectConfig.parseProject(configPath);
            
            System.out.println("Successfully parsed project documentation!");
            
            // Export to JSON
            JsonExport jsonExport = new JsonExport(true);
            String json = jsonExport.export(doc);
            
            // Write to file or stdout
            if (args.length > 2) {
                String outputPath = args[2];
                Files.writeString(Paths.get(outputPath), json);
                System.out.println("JSON documentation written to: " + outputPath);
            } else {
                System.out.println("\nJSON Output:");
                System.out.println(json);
            }
        } else {
            // Single file mode
            String luaFilePath = args[0];
            String content = Files.readString(Paths.get(luaFilePath));
            
            DocParser parser = new DocParser(content);
            doc = parser.parse();
            
            System.out.println("Successfully parsed Lua documentation!");
            
            // Export to JSON
            JsonExport jsonExport = new JsonExport(true);
            String json = jsonExport.export(doc);
            
            // Write to file or stdout
            if (args.length > 1) {
                String outputPath = args[1];
                Files.writeString(Paths.get(outputPath), json);
                System.out.println("JSON documentation written to: " + outputPath);
            } else {
                System.out.println("\nJSON Output:");
                System.out.println(json);
            }
        }
    }
}

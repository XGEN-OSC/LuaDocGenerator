package org.xgen.lua.doc.generator.process;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;

/**
 * Enum containing regex patterns for parsing Lua documentation comments.
 */
public enum Patterns {
    DOC_COMMENT("---"),
    CLASS("---@class\\s+(\\S+)(?:\\s*:\\s*(\\S+))?\\s*(.*)"),
    FIELD("---@field\\s+(?:(private|public)\\s+)?(\\w+)\\s+((?:fun\\([^)]*\\)(?:\\s*:\\s*[^\\s]+)?|\\S+)(?:\\s*\\|\\s*(?:fun\\([^)]*\\)(?:\\s*:\\s*[^\\s]+)?|\\S+))*)\\s*(.*)"),
    TYPE("---@type\\s+((?:fun\\([^)]*\\)(?:\\s*:\\s*[^\\s]+)?|\\S+)(?:\\s*\\|\\s*(?:fun\\([^)]*\\)(?:\\s*:\\s*[^\\s]+)?|\\S+))*)(?:\\s+(.+))?"),
    PARAM("---@param\\s+(\\w+)\\s+((?:fun\\([^)]*\\)(?:\\s*:\\s*[^\\s]+)?|\\S+)(?:\\s*\\|\\s*(?:fun\\([^)]*\\)(?:\\s*:\\s*[^\\s]+)?|\\S+))*)(?:\\s+(.+))?"),
    RETURN_DOC("---@return\\s+((?:fun\\([^)]*\\)(?:\\s*:\\s*[^\\s]+)?|\\S+)(?:\\s*\\|\\s*(?:fun\\([^)]*\\)(?:\\s*:\\s*[^\\s]+)?|\\S+))*)(?:\\s+(\\w+))?(?:\\s+(.+))?"),
    ENUM("---@enum\\s+(\\S+)(?:\\s*:\\s*(\\S+))?\\s*(.*)"),
    FUNCTION("function\\s+(?:(\\w+(?:\\.\\w+)*)([.:]))?([\\w]+)\\s*\\(([^)]*)\\)"),
    ASSIGNMENT("(\\w+(?:\\.\\w+)*)\\s*="),
    LOCAL("local\\s+"),
    RETURN("return\\s+"),
    META("---@meta"),
    NON_STATIC("---(@non-static|@none-static)"),
    ANY_ANNOTATION("---@\\w+.*");
    ;
    private final @NotNull String pattern;

    Patterns(final @NotNull String pattern) {
        this.pattern = pattern;
    }

    public @NotNull Pattern get() {
        return Pattern.compile(pattern);
    }

    public @NotNull Matcher matcher(final @NotNull String input) {
        return get().matcher(input);
    }
}

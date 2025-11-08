package org.xgen.lua.doc.generator.read;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.stream.Stream;

public class FileStream {
    private final @NotNull File file;

    public FileStream(final @NotNull File file) {
        this.file = file;
    }

    public Stream<String> lines() {
        try {
            return java.nio.file.Files.lines(this.file.toPath());
        } catch (final java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }
}

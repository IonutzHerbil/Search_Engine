package app.processor;

import app.model.FileRecord;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;
import java.util.stream.Collectors;

public class ContentExtractor {

    private static final int PREVIEW_LINES = 3;

    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            "txt", "md", "java", "py", "js", "ts", "html", "css",
            "json", "xml", "csv", "sh", "bat", "c", "cpp", "h", "sql",
            "log", "properties", "yml", "yaml","text","cs"
    );

    public FileRecord extract(Path file, BasicFileAttributes attrs) {
        String name = file.getFileName().toString();
        String ext = parseExtension(name);
        String preview = null;
        String content = null;

        if (isTextFile(ext, attrs.size())) {
            try {
                content = Files.readString(file);
                preview = extractPreview(content);
            } catch (IOException ignored) {
            }
        }

        return new FileRecord(
                file.toAbsolutePath().toString(),
                name,
                ext,
                attrs.size(),
                attrs.lastModifiedTime().toMillis(),
                preview,
                content
        );
    }

    private boolean isTextFile(String ext, long size) {
        return TEXT_EXTENSIONS.contains(ext.toLowerCase());
    }

    private String parseExtension(String name) {
        int dot = name.lastIndexOf('.');
        return (dot > 0 && dot < name.length() - 1) ? name.substring(dot + 1) : "";
    }

    private String extractPreview(String text) {
        return text.lines()
                .filter(line -> !line.isBlank())
                .limit(PREVIEW_LINES)
                .collect(Collectors.joining("\n"));
    }
}
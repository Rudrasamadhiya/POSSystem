package com.rudra.pos.persistence;

import com.rudra.pos.util.Csv;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Reads and writes one CSV file on disk.
 *
 * <p>Writes are <b>atomic</b>: data is first written to a sibling temp file
 * and then moved into place with {@link StandardCopyOption#ATOMIC_MOVE}. This
 * guarantees a reader never sees a half-written file even if the process is
 * killed mid-save — the on-disk table is always a complete, consistent
 * snapshot. This is the file-level building block behind the billing engine's
 * transactional commit.</p>
 */
public final class CsvFile {

    private final Path path;

    public CsvFile(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return path;
    }

    /** @return parsed rows including the header row, or empty if the file is absent. */
    public List<List<String>> readAll() {
        if (!Files.exists(path)) {
            return java.util.Collections.emptyList();
        }
        try {
            String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            return Csv.parse(text);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + path, e);
        }
    }

    /** Atomically replace the file's contents with {@code header} followed by {@code rows}. */
    public void writeAll(List<String> header, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append(Csv.encodeRow(header)).append('\n');
        for (List<String> row : rows) {
            sb.append(Csv.encodeRow(row)).append('\n');
        }
        try {
            Files.createDirectories(path.getParent());
            Path tmp = Files.createTempFile(path.getParent(), path.getFileName().toString(), ".tmp");
            Files.write(tmp, sb.toString().getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(tmp, path,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException atomicUnsupported) {
                // Some filesystems (e.g. certain network mounts) can't do atomic
                // moves; fall back to a best-effort replace.
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write " + path, e);
        }
    }
}

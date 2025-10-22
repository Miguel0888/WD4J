package de.bund.zrb.persistence.json;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import de.bund.zrb.model.Precondition;
import de.bund.zrb.persistence.PreconditionRepository;
import de.bund.zrb.persistence.dto.PreconditionDocument;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Persist preconditions as JSON on disk using a provided Gson instance.
 * Read leniently; write atomically via temp file + move.
 */
public class GsonPreconditionRepository implements PreconditionRepository {

    private final Path jsonPath;
    private final Gson gson;

    public GsonPreconditionRepository(Path jsonPath, Gson gson) {
        if (jsonPath == null) throw new IllegalArgumentException("jsonPath must not be null");
        if (gson == null) throw new IllegalArgumentException("gson must not be null");
        this.jsonPath = jsonPath;
        this.gson = gson;
    }

    @Override
    public List<Precondition> loadAll() throws IOException {
        if (!Files.exists(jsonPath)) {
            return Collections.emptyList();
        }

        byte[] bytes = Files.readAllBytes(jsonPath);
        String json = new String(bytes, StandardCharsets.UTF_8);

        // Read leniently to be tolerant with trailing commas etc.
        Reader sr = new StringReader(json);
        JsonReader reader = new JsonReader(sr);
        reader.setLenient(true);

        PreconditionDocument doc = gson.fromJson(reader, PreconditionDocument.class);
        if (doc == null || doc.getPreconditions() == null) {
            return Collections.emptyList();
        }
        return doc.getPreconditions();
    }

    @Override
    public void saveAll(List<Precondition> preconditions) throws IOException {
        PreconditionDocument doc = new PreconditionDocument();
        doc.setPreconditions(preconditions);

        String json = gson.toJson(doc);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        Path parent = jsonPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        // Write atomically: write to temp and move (best-effort ATOMIC_MOVE)
        Path tmp = jsonPath.resolveSibling(jsonPath.getFileName().toString() + ".tmp");
        Files.write(tmp, bytes);

        try {
            Files.move(tmp, jsonPath,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicUnsupported) {
            Files.move(tmp, jsonPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }
}

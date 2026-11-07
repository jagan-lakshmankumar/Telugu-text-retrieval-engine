import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Keeps vocabulary metadata in memory; seeks individual postings lists on disk. */
public final class DiskIndex {
    public record Metadata(int df, int cf, long offset, int length) { }
    private final Path directory;
    private final Map<String, Metadata> vocabulary;

    private DiskIndex(Path directory, Map<String, Metadata> vocabulary) {
        this.directory = directory;
        this.vocabulary = vocabulary;
    }

    public static void write(Map<String, Map<String, List<Integer>>> index, Path directory) throws IOException {
        Files.createDirectories(directory);
        Map<String, Metadata> vocabulary = new TreeMap<>();
        Path postingsPath = directory.resolve("postings.dat");
        try (var out = Files.newOutputStream(postingsPath)) {
            long offset = 0;
            for (String term : new TreeMap<>(index).keySet()) {
                Map<String, List<Integer>> docs = index.get(term);
                StringBuilder line = new StringBuilder(term).append('\t');
                int cf = 0;
                boolean firstDocument = true;
                for (String docId : new TreeMap<>(docs).keySet()) {
                    if (!firstDocument) line.append(';');
                    firstDocument = false;
                    line.append(docId).append(':');
                    List<Integer> positions = docs.get(docId);
                    cf += positions.size();
                    for (int i = 0; i < positions.size(); i++) {
                        if (i > 0) line.append(',');
                        line.append(positions.get(i));
                    }
                }
                byte[] bytes = (line + "\n").getBytes(StandardCharsets.UTF_8);
                out.write(bytes);
                vocabulary.put(term, new Metadata(docs.size(), cf, offset, bytes.length));
                offset += bytes.length;
            }
        }
        StringBuilder vocabFile = new StringBuilder("term\tdf\tcf\toffset\tlength\n");
        for (var entry : vocabulary.entrySet()) {
            Metadata m = entry.getValue();
            vocabFile.append(entry.getKey()).append('\t').append(m.df()).append('\t').append(m.cf())
                    .append('\t').append(m.offset()).append('\t').append(m.length()).append('\n');
        }
        Files.writeString(directory.resolve("vocabulary.tsv"), vocabFile.toString(), StandardCharsets.UTF_8);
    }

    public static DiskIndex open(Path directory) throws IOException {
        Map<String, Metadata> vocabulary = new TreeMap<>();
        List<String> rows = Files.readAllLines(directory.resolve("vocabulary.tsv"), StandardCharsets.UTF_8);
        for (int i = 1; i < rows.size(); i++) {
            String[] fields = rows.get(i).split("\\t");
            vocabulary.put(fields[0], new Metadata(Integer.parseInt(fields[1]), Integer.parseInt(fields[2]),
                    Long.parseLong(fields[3]), Integer.parseInt(fields[4])));
        }
        return new DiskIndex(directory, vocabulary);
    }

    public Metadata metadata(String term) { return vocabulary.get(term); }

    /** In-memory vocabulary and statistics; postings themselves remain on disk. */
    public Map<String, Metadata> vocabulary() { return Map.copyOf(vocabulary); }

    public Map<String, List<Integer>> postings(String term) throws IOException {
        Metadata metadata = vocabulary.get(term);
        if (metadata == null) return null;
        byte[] bytes = new byte[metadata.length()];
        try (RandomAccessFile file = new RandomAccessFile(directory.resolve("postings.dat").toFile(), "r")) {
            file.seek(metadata.offset());
            file.readFully(bytes);
        }
        String payload = new String(bytes, StandardCharsets.UTF_8).strip();
        String[] pieces = payload.split("\\t", 2);
        Map<String, List<Integer>> result = new LinkedHashMap<>();
        if (pieces.length < 2 || pieces[1].isEmpty()) return result;
        for (String document : pieces[1].split(";")) {
            String[] documentParts = document.split(":", 2);
            List<Integer> positions = new java.util.ArrayList<>();
            for (String position : documentParts[1].split(",")) positions.add(Integer.parseInt(position));
            result.put(documentParts[0], positions);
        }
        return result;
    }
}

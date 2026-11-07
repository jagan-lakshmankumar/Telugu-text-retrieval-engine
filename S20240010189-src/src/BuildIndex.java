import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Builds a positional inverted index from UTF-8 Telugu .txt files. */
public final class BuildIndex {
    public static Map<String, Map<String, List<Integer>>> build(Path corpus) throws IOException {
        Map<String, Map<String, List<Integer>>> index = new TreeMap<>();
        try (var files = Files.list(corpus)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".txt")).sorted().toList()) {
                String documentId = file.getFileName().toString().replaceFirst("\\.txt$", "");
                List<String> tokens = TeluguTokenizer.tokenize(Files.readString(file, StandardCharsets.UTF_8));
                for (int position = 0; position < tokens.size(); position++) {
                    index.computeIfAbsent(tokens.get(position), ignored -> new TreeMap<>())
                            .computeIfAbsent(documentId, ignored -> new ArrayList<>()).add(position);
                }
            }
        }
        return index;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Usage: java BuildIndex <corpus-folder> <index-folder>");
        Path corpus = Path.of(args[0]);
        Map<String, Map<String, List<Integer>>> index = build(corpus);
        DiskIndex.write(index, Path.of(args[1]));
        System.out.printf("Indexed %d terms from %d documents.%n", index.size(), Files.list(corpus).filter(p -> p.toString().endsWith(".txt")).count());
    }
}

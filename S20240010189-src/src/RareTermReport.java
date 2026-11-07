import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Generates the required ten rare-term positional-index examples from a disk index. */
public final class RareTermReport {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Usage: java RareTermReport <index-folder> <output-file>");
        DiskIndex index = DiskIndex.open(Path.of(args[0]));
        StringBuilder output = new StringBuilder("Ten rare Telugu Wikipedia terms (zero-based positions)\n\n");
        int count = 0;
        for (Map.Entry<String, DiskIndex.Metadata> entry : index.vocabulary().entrySet()) {
            if (entry.getValue().df() != 1 || !entry.getKey().matches("[\\u0C00-\\u0C7F]{3,}")) continue;
            output.append(++count).append(". term=").append(entry.getKey())
                    .append(", DF=").append(entry.getValue().df()).append(", CF=").append(entry.getValue().cf())
                    .append(", postings=").append(index.postings(entry.getKey())).append('\n');
            if (count == 10) break;
        }
        Files.writeString(Path.of(args[1]), output, StandardCharsets.UTF_8);
        System.out.println("Wrote " + count + " rare-term examples.");
    }
}

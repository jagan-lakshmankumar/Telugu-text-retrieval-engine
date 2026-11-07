import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Command-line term and exact phrase search using the disk-resident postings file. */
public final class SearchIndex {
    public static Map<String, List<Integer>> phraseMatches(DiskIndex index, String phrase) throws Exception {
        List<String> terms = TeluguTokenizer.tokenize(phrase);
        if (terms.isEmpty()) return Map.of();
        List<Map<String, List<Integer>>> lists = new ArrayList<>();
        for (String term : terms) {
            Map<String, List<Integer>> postings = index.postings(term);
            if (postings == null) return Map.of();
            lists.add(postings);
        }
        Set<String> common = new HashSet<>(lists.get(0).keySet());
        for (Map<String, List<Integer>> postings : lists) common.retainAll(postings.keySet());
        Map<String, List<Integer>> results = new TreeMap<>();
        for (String doc : common) {
            Set<Integer> starts = new HashSet<>(lists.get(0).get(doc));
            for (int offset = 1; offset < lists.size(); offset++) {
                Set<Integer> adjusted = new HashSet<>();
                for (int position : lists.get(offset).get(doc)) adjusted.add(position - offset);
                starts.retainAll(adjusted);
            }
            if (!starts.isEmpty()) results.put(doc, starts.stream().sorted().toList());
        }
        return results;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) throw new IllegalArgumentException("Usage: java SearchIndex <index-folder> term|phrase <query>");
        DiskIndex index = DiskIndex.open(Path.of(args[0]));
        if (args[1].equals("term")) {
            List<String> terms = TeluguTokenizer.tokenize(args[2]);
            if (terms.size() != 1) throw new IllegalArgumentException("A term query needs one token.");
            String term = terms.get(0);
            System.out.println("term=" + term + " metadata=" + index.metadata(term) + " postings=" + index.postings(term));
        } else if (args[1].equals("phrase")) {
            System.out.println("phrase=" + args[2] + " matches=" + phraseMatches(index, args[2]));
        } else throw new IllegalArgumentException("Second argument must be term or phrase.");
    }
}

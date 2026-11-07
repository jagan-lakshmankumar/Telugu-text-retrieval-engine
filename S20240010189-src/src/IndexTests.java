import java.nio.file.Files;
import java.nio.file.Path;

/** Five repeatable functional checks. */
public final class IndexTests {
    private static void check(boolean value, String name) { if (!value) throw new AssertionError(name); }
    public static void main(String[] args) throws Exception {
        Path indexPath = Files.createTempDirectory("telugu-index-");
        DiskIndex.write(BuildIndex.build(Path.of("sample_corpus")), indexPath);
        DiskIndex index = DiskIndex.open(indexPath);
        check(index.metadata("క్వాంటం").df() == 1, "Test 1: rare-term DF");
        check(index.metadata("భారతదేశం").cf() == 2, "Test 2: collection frequency");
        check(index.postings("క్వాంటం").toString().equals("{doc06=[0]}"), "Test 3: positional posting");
        check(SearchIndex.phraseMatches(index, "కృత్రిమ మేధస్సు").toString().equals("{doc04=[0]}"), "Test 4: phrase query");
        check(SearchIndex.phraseMatches(index, "సౌర శక్తి").toString().equals("{doc02=[0]}"), "Test 5: phrase query");
        System.out.println("All 5 Telugu positional-index tests passed.");
    }
}

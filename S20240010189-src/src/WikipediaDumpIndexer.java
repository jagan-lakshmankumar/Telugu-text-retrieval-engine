import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

/** Streams a MediaWiki .xml.bz2 dump; it never expands the full dump into memory or onto disk. */
public final class WikipediaDumpIndexer {
    private static InputStream decompressedDump(Path dump) throws Exception {
        // Python's standard-library bz2 module is used only as a streaming decompressor.
        // Java owns the XML parser and all indexing; the dump is never expanded to a file.
        String script = "import bz2,shutil,sys\n"
                + "try:\n"
                + "    shutil.copyfileobj(bz2.open(sys.argv[1], 'rb'), sys.stdout.buffer, 1048576)\n"
                + "except BrokenPipeError:\n"
                + "    pass\n";
        Process process = new ProcessBuilder("py", "-3", "-c", script, dump.toAbsolutePath().toString())
                .redirectError(ProcessBuilder.Redirect.INHERIT).start();
        return new BufferedInputStream(process.getInputStream());
    }

    public static Map<String, Map<String, List<Integer>>> build(Path dump, int maxDocuments) throws Exception {
        Map<String, Map<String, List<Integer>>> index = new TreeMap<>();
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        int documents = 0;
        try (InputStream stream = decompressedDump(dump)) {
            XMLStreamReader xml = factory.createXMLStreamReader(stream, StandardCharsets.UTF_8.name());
            String title = null, pageId = null;
            StringBuilder text = null;
            boolean inPage = false, inRevision = false, inText = false;
            while (xml.hasNext() && documents < maxDocuments) {
                int event = xml.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    switch (xml.getLocalName()) {
                        case "page" -> { inPage = true; title = null; pageId = null; text = new StringBuilder(); }
                        case "revision" -> inRevision = true;
                        case "text" -> inText = true;
                        case "title" -> { if (inPage) title = xml.getElementText(); }
                        case "id" -> { if (inPage && !inRevision && pageId == null) pageId = xml.getElementText(); }
                    }
                } else if (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) {
                    if (inText) text.append(xml.getText());
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    String element = xml.getLocalName();
                    if (element.equals("text")) inText = false;
                    else if (element.equals("revision")) inRevision = false;
                    else if (element.equals("page")) {
                        // Remove the most common MediaWiki markup before Telugu tokenization.
                        String articleText = text.toString().replaceAll("(?s)<[^>]*>", " ")
                                .replaceAll("(?s)\\{\\{.*?\\}\\}", " ")
                                .replaceAll("(?s)\\[\\[([^\\]|]+)\\|([^\\]]+)\\]\\]", "$2")
                                .replaceAll("(?s)\\[\\[([^\\]]+)\\]\\]", "$1");
                        List<String> tokens = TeluguTokenizer.tokenize(articleText);
                        if (pageId != null && !tokens.isEmpty() && (title == null || !title.startsWith("Redirect:"))) {
                            String documentId = "wiki" + pageId;
                            for (int position = 0; position < tokens.size(); position++) {
                                index.computeIfAbsent(tokens.get(position), ignored -> new TreeMap<>())
                                        .computeIfAbsent(documentId, ignored -> new ArrayList<>()).add(position);
                            }
                            documents++;
                        }
                        inPage = false;
                    }
                }
            }
            xml.close();
        }
        System.out.printf("Streamed %d Telugu Wikipedia articles.%n", documents);
        return index;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2 || args.length > 3)
            throw new IllegalArgumentException("Usage: java WikipediaDumpIndexer <dump.xml.bz2> <index-folder> [max-documents]");
        int maxDocuments = args.length == 3 ? Integer.parseInt(args[2]) : 1000;
        if (maxDocuments < 1) throw new IllegalArgumentException("max-documents must be positive");
        Map<String, Map<String, List<Integer>>> index = build(Path.of(args[0]), maxDocuments);
        DiskIndex.write(index, Path.of(args[1]));
        System.out.printf("Stored %d vocabulary terms.%n", index.size());
    }
}

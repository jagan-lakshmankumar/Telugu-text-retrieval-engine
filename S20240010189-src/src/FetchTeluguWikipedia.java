import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Downloads Telugu Wikipedia plaintext extracts through the official MediaWiki API. */
public final class FetchTeluguWikipedia {
    private static final String API = "https://te.wikipedia.org/w/api.php";
    private static String request(HttpClient client, String query) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(API + "?" + query))
                .header("User-Agent", "TeluguPositionalIndexAssignment/1.0 (educational use)")
                .timeout(Duration.ofSeconds(30)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
    }

    private static String decodeJson(String value) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != '\\' || i + 1 == value.length()) { result.append(c); continue; }
            char escaped = value.charAt(++i);
            switch (escaped) {
                case 'n' -> result.append('\n'); case 'r' -> result.append('\r'); case 't' -> result.append('\t');
                case '"' -> result.append('"'); case '\\' -> result.append('\\'); case '/' -> result.append('/');
                case 'u' -> { result.append((char) Integer.parseInt(value.substring(i + 1, i + 5), 16)); i += 4; }
                default -> result.append(escaped);
            }
        }
        return result.toString();
    }

    /** Returns JSON string field values without regex recursion on long Wikipedia extracts. */
    private static List<String> stringValues(String json, String field) {
        String prefix = "\"" + field + "\":\"";
        List<String> values = new ArrayList<>();
        int start = 0;
        while ((start = json.indexOf(prefix, start)) >= 0) {
            int cursor = start + prefix.length();
            StringBuilder encoded = new StringBuilder();
            boolean escaped = false;
            while (cursor < json.length()) {
                char character = json.charAt(cursor++);
                if (character == '"' && !escaped) break;
                encoded.append(character);
                if (character == '\\' && !escaped) escaped = true;
                else escaped = false;
            }
            values.add(decodeJson(encoded.toString()));
            start = cursor;
        }
        return values;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2)
            throw new IllegalArgumentException("Usage: java FetchTeluguWikipedia <output-folder> [article-count]");
        Path output = Path.of(args[0]);
        int wanted = args.length == 2 ? Integer.parseInt(args[1]) : 20;
        if (wanted < 1 || wanted > 50) throw new IllegalArgumentException("article-count must be 1..50");
        Files.createDirectories(output);
        HttpClient client = HttpClient.newHttpClient();
        String search = "action=query&list=search&srnamespace=0&srlimit=" + wanted + "&format=json&formatversion=2&srsearch="
                + URLEncoder.encode("తెలుగు", StandardCharsets.UTF_8);
        List<String> selected = new ArrayList<>();
        for (String title : stringValues(request(client, search), "title")) {
            if (selected.size() == wanted) break;
            selected.add(title);
        }
        StringBuilder manifest = new StringBuilder("doc_id\ttitle\tsource_url\n");
        int saved = 0;
        for (String title : selected) {
            String pageQuery = "action=query&prop=extracts&explaintext=1&redirects=1&format=json&formatversion=2&titles="
                    + URLEncoder.encode(title, StandardCharsets.UTF_8);
            List<String> extracts = stringValues(request(client, pageQuery), "extract");
            if (extracts.isEmpty()) continue;
            String text = extracts.get(0).trim();
            if (text.isBlank()) continue;
            String docId = String.format("wiki%03d", ++saved);
            Files.writeString(output.resolve(docId + ".txt"), text, StandardCharsets.UTF_8);
            manifest.append(docId).append('\t').append(title.replace('\t', ' ')).append('\t')
                    .append("https://te.wikipedia.org/wiki/").append(URLEncoder.encode(title.replace(' ', '_'), StandardCharsets.UTF_8)).append('\n');
        }
        Files.writeString(output.resolve("manifest.tsv"), manifest.toString(), StandardCharsets.UTF_8);
        System.out.printf("Downloaded %d Telugu Wikipedia article extracts to %s%n", saved, output);
    }
}

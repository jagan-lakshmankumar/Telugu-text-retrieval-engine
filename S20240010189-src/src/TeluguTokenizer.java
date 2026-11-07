import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Unicode-aware tokenizer for Telugu and English text. */
public final class TeluguTokenizer {
    private static final Pattern TOKEN = Pattern.compile("[\\u0C00-\\u0C7F]+|[A-Za-z]+");

    private TeluguTokenizer() { }

    public static List<String> tokenize(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN.matcher(normalized);
        while (matcher.find()) tokens.add(matcher.group());
        return tokens;
    }
}

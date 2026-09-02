package dev.sirnik.blog.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class HTMLParser {

    private static final String FALLBACK_EMPTY_PREVIEW_STRING = "No preview";

    private HTMLParser() {
    }

    public static String getPreviewString(String htmlBody) {
        Document doc = Jsoup.parseBodyFragment(htmlBody);
        Element firstParagraph = doc.selectFirst("p");

        if (firstParagraph == null) {
            return FALLBACK_EMPTY_PREVIEW_STRING;
        }

        firstParagraph.select("img, picture, video").remove();

        String preview = firstParagraph.text();
        return preview.isBlank() ? FALLBACK_EMPTY_PREVIEW_STRING : preview;
    }
}

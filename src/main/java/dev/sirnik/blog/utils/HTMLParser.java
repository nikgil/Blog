package dev.sirnik.blog.utils;

import java.util.Random;

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

    public static boolean insertAtRandomPosition(Element parent,
        Element element, Random randomiser, boolean preventChaining) {
        int position = randomiser.nextInt(parent.childrenSize() + 1);

        if (preventChaining
            && wouldChainIfInserted(parent, position, element.tagName())) {
            return false;
        }

        if (position == parent.childrenSize()) {
            parent.appendChild(element);
        } else {
            parent.child(position).before(element);
        }

        return true;
    }

    private static boolean wouldChainIfInserted(Element parent, int position,
        String elementTag) {
        Element previousElement = position == 0
            ? null
            : parent.child(position - 1);
        Element nextElement = position == parent.childrenSize()
            ? null
            : parent.child(position);

        boolean previousMatches = previousElement != null
            && previousElement.tagName().equals(elementTag);
        boolean nextMatches = nextElement != null
            && nextElement.tagName().equals(elementTag);

        return previousMatches || nextMatches;
    }
}

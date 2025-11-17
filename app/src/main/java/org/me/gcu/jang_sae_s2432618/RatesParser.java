package org.me.gcu.jang_sae_s2432618;

import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RatesParser {

    private static final String TAG = "RatesParser";

    private static final DateTimeFormatter RFC822_UTC =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'UTC'")
                    .withZone(ZoneOffset.UTC);

    // Regex helpers
    private static final Pattern CODE_IN_PARENS   = Pattern.compile("\\(([A-Z]{3})\\)\\s*$");
    private static final Pattern CODE_AFTER_SLASH = Pattern.compile("GBP\\s*/\\s*([A-Z]{3})");
    private static final Pattern ANY_TRIPLE       = Pattern.compile("\\b([A-Z]{3})\\b");
    private static final Pattern FEED_RATE        =
            Pattern.compile("GBP\\s*=\\s*([0-9]+(?:[\\.,][0-9]+)?)\\s*[A-Z]{3}");

    /** Parse the RSS XML into a ParseResult: title, lastUpdated, items. */
    public static ParseResult parse(String xml) throws Exception {
        String safe = (xml == null) ? "" : xml.trim();
        if (safe.isEmpty()) {
            return new ParseResult("", "", new ArrayList<>());
        }

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();
        xpp.setInput(new StringReader(safe));

        String feedTitle = "";
        String updated   = "";
        List<RateItem> items = new ArrayList<>();

        boolean inItem = false;
        String currentTag = null;

        String itemTitle = null;
        String itemDesc  = null;

        int event = xpp.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            switch (event) {
                case XmlPullParser.START_TAG:
                    currentTag = xpp.getName();
                    if ("item".equalsIgnoreCase(currentTag)) {
                        inItem = true;
                        itemTitle = null;
                        itemDesc  = null;
                    }
                    break;

                case XmlPullParser.TEXT: {
                    String t = xpp.getText();
                    if (t != null) t = t.trim();
                    if (t == null || t.isEmpty()) break;

                    if (inItem) {
                        if ("title".equalsIgnoreCase(currentTag)) {
                            itemTitle = appendText(itemTitle, t);
                        } else if ("description".equalsIgnoreCase(currentTag)) {
                            itemDesc = appendText(itemDesc, t);
                        }
                    } else {
                        if ("title".equalsIgnoreCase(currentTag)) {
                            feedTitle = t;
                        } else if ("pubDate".equalsIgnoreCase(currentTag)
                                || "lastBuildDate".equalsIgnoreCase(currentTag)) {
                            updated = t;
                        }
                    }
                    break;
                }

                case XmlPullParser.END_TAG:
                    String end = xpp.getName();
                    if ("item".equalsIgnoreCase(end) && inItem) {
                        RateItem item = buildItemFrom(itemTitle, itemDesc);
                        if (item != null) items.add(item);
                        inItem = false;
                    }
                    currentTag = null;
                    break;
            }
            event = xpp.next();
        }

        // Fallback to "now" if no date present in feed
        String lastUpdated = (updated == null || updated.trim().isEmpty())
                ? RFC822_UTC.format(ZonedDateTime.now(ZoneOffset.UTC))
                : updated.trim();

        String titleOut = (feedTitle == null) ? "" : feedTitle.trim();

        Log.d(TAG, "Parsed items: " + items.size());
        return new ParseResult(titleOut, lastUpdated, items);
    }

    // --- helpers -------------------------------------------------------------------------------

    private static String appendText(String base, String add) {
        if (base == null || base.isEmpty()) return add;
        if (add == null || add.isEmpty())   return base;
        // Some parsers deliver TEXT in multiple chunks
        return base + " " + add;
    }

    private static RateItem buildItemFrom(String title, String desc) {
        if (title == null) title = "";
        if (desc  == null) desc  = "";

        // Identify the 3-letter code
        String code = pickCode(title);

        // Derive a readable name (right-hand side of slash, minus the (CODE))
        String name = deriveName(title, code);

        // Parse numeric rate from description (supports 1234, 12.34, 12,34)
        Double rate = pickRate(desc);

        if (code == null || rate == null) return null;
        if (name == null || name.isEmpty()) name = code;

        // country field left blank
        return new RateItem(code, name, "", rate);
    }

    private static String pickCode(String title) {
        String t = (title == null) ? "" : title.trim();

        Matcher m = CODE_IN_PARENS.matcher(t);
        if (m.find()) return m.group(1);

        m = CODE_AFTER_SLASH.matcher(t);
        if (m.find()) return m.group(1);

        String code = null;
        m = ANY_TRIPLE.matcher(t);
        while (m.find()) {
            String c = m.group(1);
            if (!"GBP".equals(c)) code = c;
        }
        return code;
    }

    private static String deriveName(String title, String code) {
        if (title == null) return null;
        String t = title;


        int slash = t.indexOf('/');
        if (slash >= 0) {
            String rhs = t.substring(slash + 1).trim();
            rhs = rhs.replaceAll("\\(.*?\\)", "").trim();
            if (!rhs.isEmpty()) return rhs;
        }


        t = t.replaceAll("\\(.*?\\)", "");
        if (t.contains("/")) t = t.substring(t.indexOf('/') + 1);

        t = t.replace("GBP", "").replaceAll("\\s+", " ").trim();
        if (t.isEmpty() || (code != null && t.equalsIgnoreCase(code))) return null;
        return t;
    }

    private static Double pickRate(String desc) {
        if (desc == null) return null;

        String plain = desc.replaceAll("<.*?>", " ")
                .replace("&nbsp;", " ")
                .trim();


        Matcher m = FEED_RATE.matcher(plain);
        String num = null;
        if (m.find()) {
            num = m.group(1);
        } else {
            m = Pattern.compile("([0-9]+(?:[\\.,][0-9]+)?)").matcher(plain);
            while (m.find()) num = m.group(1);
        }
        if (num == null) return null;

        // Convert European decimal comma to dot if needed
        if (num.indexOf(',') >= 0 && num.indexOf('.') < 0) {
            num = num.replace(',', '.');
        }
        try {
            return Double.parseDouble(num);
        } catch (Exception e) {
            return null;
        }
    }
}

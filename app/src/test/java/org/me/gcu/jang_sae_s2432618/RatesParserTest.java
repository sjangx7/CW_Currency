package org.me.gcu.jang_sae_s2432618;

import static org.junit.Assert.*;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class RatesParserTest {

    static class MiniItem { String title; String desc; }

    private static List<MiniItem> parse(String xml) throws Exception {
        List<MiniItem> out = new ArrayList<>();
        XmlPullParserFactory f = XmlPullParserFactory.newInstance();
        f.setNamespaceAware(true);
        XmlPullParser x = f.newPullParser();
        x.setInput(new StringReader(xml));

        int e = x.getEventType();
        MiniItem cur = null;
        while (e != XmlPullParser.END_DOCUMENT) {
            if (e == XmlPullParser.START_TAG) {
                String tag = x.getName();
                if ("item".equalsIgnoreCase(tag)) {
                    cur = new MiniItem();
                } else if (cur != null && "title".equalsIgnoreCase(tag)) {
                    cur.title = nextText(x);
                } else if (cur != null && "description".equalsIgnoreCase(tag)) {
                    cur.desc = nextText(x);
                }
            } else if (e == XmlPullParser.END_TAG) {
                if ("item".equalsIgnoreCase(x.getName()) && cur != null) {
                    out.add(cur);
                    cur = null;
                }
            }
            e = x.next();
        }
        return out;
    }

    private static String nextText(XmlPullParser x) throws Exception {
        int ev = x.next();
        if (ev == XmlPullParser.TEXT) {
            String t = x.getText();
            // advance to end tag
            while (x.getEventType() != XmlPullParser.END_TAG) x.next();
            return t == null ? "" : t.trim();
        }
        return "";
    }

    @Test
    public void parsesSampleRss() throws Exception {
        String sample = "<?xml version=\"1.0\"?><rss version=\"2.0\"><channel>"
                + "<title>British Pound Sterling(GBP) Currency Exchange Rates</title>"
                + "<item><title>GBP/AED</title><description>1 GBP = 4.9471 AED</description></item>"
                + "<item><title>GBP/JPY</title><description>1 GBP = 183.42 JPY</description></item>"
                + "</channel></rss>";

        List<MiniItem> items = parse(sample);

        assertNotNull(items);
        assertEquals(2, items.size());
        assertTrue(items.get(0).title.contains("AED"));
        assertTrue(items.get(0).desc.contains("4.9471"));
        assertTrue(items.get(1).title.contains("JPY"));
        assertTrue(items.get(1).desc.contains("183.42"));
    }
}

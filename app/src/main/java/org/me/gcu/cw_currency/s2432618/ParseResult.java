package org.me.gcu.cw_currency.s2432618;

import java.util.List;

public class ParseResult {
    public final String title;    // channel title
    public final String pubDate;  // legacy name (parser filled this)
    public final List<RateItem> rates; // legacy name (parser filled this)

    // canonical / VM-expected names (were never set before)
    public String updated;       // alias of pubDate
    public List<RateItem> items; // alias of rates
    public String lastUpdated;   // alias of pubDate

    public ParseResult(String title, String pubDate, List<RateItem> rates) {
        this.title = title == null ? "" : title;
        this.pubDate = pubDate == null ? "" : pubDate;
        this.rates = (rates == null) ? java.util.Collections.emptyList() : rates;

        // --- IMPORTANT: keep VM happy by wiring aliases ---
        this.items = this.rates;          // VM reads items
        this.lastUpdated = this.pubDate;  // VM reads lastUpdated
        this.updated = this.pubDate;      // some code may read updated
    }
}

package org.me.gcu.cw_currency.s2432618;
public class RateItem {
    public final String code;     // e.g., "AED"
    public final String name;     // e.g., "United Arab Emirates Dirham"
    public final String country;  // feed doesn’t provide a separate field (leave "")
    public final double rate;     // GBP -> code (e.g., 4.9471)

    public RateItem(String code, String name, String country, double rate) {
        this.code = code;
        this.name = name;
        this.country = country;
        this.rate = rate;
    }
}

package org.me.gcu.cw_currency.s2432618;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class FlagUtil {
    private FlagUtil() {}

    /** Build a flag emoji from an ISO-3166 alpha-2 code (e.g., "GB" -> 🇬🇧). */
    private static String flagEmoji(String iso2) {
        if (iso2 == null || iso2.length() != 2) return "";
        int base = 0x1F1E6; // Regional Indicator Symbol Letter A
        int first = Character.toUpperCase(iso2.charAt(0)) - 'A' + base;
        int second = Character.toUpperCase(iso2.charAt(1)) - 'A' + base;
        return new String(Character.toChars(first)) + new String(Character.toChars(second));
    }

    /** Static map of currency code -> representative ISO-3166 alpha-2 for flag. */
    private static final Map<String, String> C2F = new HashMap<>();
    static {
        // Major & common
        C2F.put("GBP","GB"); C2F.put("USD","US"); C2F.put("EUR","EU"); C2F.put("JPY","JP");
        C2F.put("AUD","AU"); C2F.put("CAD","CA"); C2F.put("CHF","CH"); C2F.put("CNY","CN");
        C2F.put("HKD","HK"); C2F.put("NZD","NZ"); C2F.put("SEK","SE"); C2F.put("NOK","NO");
        C2F.put("DKK","DK"); C2F.put("PLN","PL"); C2F.put("HUF","HU"); C2F.put("CZK","CZ");
        C2F.put("ILS","IL"); C2F.put("SGD","SG"); C2F.put("KRW","KR"); C2F.put("TWD","TW");
        C2F.put("INR","IN"); C2F.put("BRL","BR"); C2F.put("MXN","MX"); C2F.put("ZAR","ZA");

        // Middle East & Africa
        C2F.put("AED","AE"); C2F.put("SAR","SA"); C2F.put("QAR","QA"); C2F.put("KWD","KW");
        C2F.put("BHD","BH"); C2F.put("OMR","OM"); C2F.put("EGP","EG"); C2F.put("MAD","MA");
        C2F.put("TND","TN"); C2F.put("DZD","DZ"); C2F.put("LYD","LY"); C2F.put("NGN","NG");
        C2F.put("KES","KE"); C2F.put("TZS","TZ"); C2F.put("UGX","UG"); C2F.put("GHS","GH");
        C2F.put("XOF","SN"); // West African CFA – pick Senegal 🇸🇳
        C2F.put("XAF","CM"); // Central African CFA – pick Cameroon 🇨🇲
        C2F.put("RWF","RW"); C2F.put("ETB","ET"); C2F.put("ZMW","ZM"); C2F.put("SZL","SZ");
        C2F.put("SOS","SO"); C2F.put("SDG","SD"); C2F.put("SLL","SL"); C2F.put("SLE","SL"); // new code
        C2F.put("MUR","MU"); C2F.put("MGA","MG"); C2F.put("BWP","BW"); C2F.put("NAD","NA");
        C2F.put("GMD","GM"); C2F.put("CDF","CD"); C2F.put("BIF","BI"); C2F.put("RSD","RS"); // (Europe but keep)

        // Europe (wider)
        C2F.put("RON","RO"); C2F.put("HRK","HR"); // (now EUR, but still appears sometimes)
        C2F.put("BGN","BG"); C2F.put("ISK","IS"); C2F.put("UAH","UA"); C2F.put("RUB","RU");
        C2F.put("BYN","BY"); C2F.put("MDL","MD"); C2F.put("MKD","MK"); C2F.put("GIP","GI");
        C2F.put("FKP","FK"); C2F.put("SHP","SH"); C2F.put("IMP","IM"); C2F.put("JEP","JE"); // GBP territories
        C2F.put("ALL","AL"); C2F.put("AMD","AM"); C2F.put("AZN","AZ"); C2F.put("GEL","GE");
        C2F.put("KZT","KZ"); C2F.put("UZS","UZ"); C2F.put("TJS","TJ"); C2F.put("TMT","TM");
        C2F.put("XPF","PF"); // CFP franc – pick French Polynesia 🇵🇫

        // Americas
        C2F.put("ARS","AR"); C2F.put("BOB","BO"); C2F.put("BRL","BR"); C2F.put("CLP","CL");
        C2F.put("COP","CO"); C2F.put("CRC","CR"); C2F.put("CUP","CU"); C2F.put("DOP","DO");
        C2F.put("GYD","GY"); C2F.put("GTQ","GT"); C2F.put("HNL","HN"); C2F.put("JMD","JM");
        C2F.put("NIO","NI"); C2F.put("PAB","PA"); C2F.put("PEN","PE"); C2F.put("PYG","PY");
        C2F.put("TTD","TT"); C2F.put("UYU","UY"); C2F.put("VEF","VE"); C2F.put("VES","VE");
        C2F.put("BBD","BB"); C2F.put("BZD","BZ"); C2F.put("BSD","BS"); C2F.put("KYD","KY");
        C2F.put("XCD","AG"); // East Caribbean dollar – pick Antigua & Barbuda 🇦🇬

        // Asia
        C2F.put("AFN","AF"); C2F.put("AZN","AZ"); C2F.put("BDT","BD"); C2F.put("BND","BN");
        C2F.put("BTN","BT"); C2F.put("KHR","KH"); C2F.put("CNY","CN"); C2F.put("TWD","TW");
        C2F.put("FJD","FJ"); C2F.put("GEL","GE"); C2F.put("HKD","HK"); C2F.put("IDR","ID");
        C2F.put("IQD","IQ"); C2F.put("IRR","IR"); C2F.put("ILS","IL"); C2F.put("JOD","JO");
        C2F.put("KZT","KZ"); C2F.put("KGS","KG"); C2F.put("LAK","LA"); C2F.put("LBP","LB");
        C2F.put("MOP","MO"); C2F.put("MYR","MY"); C2F.put("MVR","MV"); C2F.put("MNT","MN");
        C2F.put("MMK","MM"); C2F.put("NPR","NP"); C2F.put("OMR","OM"); C2F.put("PKR","PK");
        C2F.put("PHP","PH"); C2F.put("QAR","QA"); C2F.put("SAR","SA"); C2F.put("LKR","LK");
        C2F.put("SYP","SY"); C2F.put("THB","TH"); C2F.put("TRY","TR"); C2F.put("TJS","TJ");
        C2F.put("TMT","TM"); C2F.put("AED","AE"); C2F.put("VND","VN"); C2F.put("YER","YE");

        // Oceania & Pacific
        C2F.put("AUD","AU"); C2F.put("NZD","NZ"); C2F.put("PGK","PG"); C2F.put("SBD","SB");
        C2F.put("WST","WS"); C2F.put("TOP","TO"); C2F.put("VUV","VU"); C2F.put("KID","KI"); // Kiribati (uses AUD but keep)
        C2F.put("XPF","PF");

        // Caribbean / Atlantic territories
        C2F.put("ANG","CW"); // Netherlands Antillean guilder – choose Curaçao 🇨🇼
        C2F.put("AWG","AW"); C2F.put("HTG","HT"); C2F.put("SRD","SR"); C2F.put("CUP","CU");

        // Others & duplicates already covered elsewhere:
        C2F.put("BAM","BA"); C2F.put("MDL","MD"); C2F.put("MKD","MK"); C2F.put("MGA","MG");
        C2F.put("MRU","MR"); C2F.put("MZN","MZ"); C2F.put("GNF","GN"); C2F.put("DJF","DJ");
        C2F.put("KMF","KM"); C2F.put("LRD","LR"); C2F.put("LSL","LS"); C2F.put("AOA","AO");
        C2F.put("SOS","SO"); C2F.put("SCR","SC"); C2F.put("STN","ST"); C2F.put("SSP","SS");
        C2F.put("TRY","TR"); C2F.put("AZN","AZ");
    }

    /**
     * Return a representative flag emoji for a currency code.
     * For unions or multi-country currencies, we pick a sensible default:
     *  EUR→🇪🇺, XOF→🇸🇳, XAF→🇨🇲, XCD→🇦🇬, XPF→🇵🇫, ANG→🇨🇼.
     */
    public static String flagForCurrency(String code) {
        if (code == null) return "";
        String cc = code.toUpperCase(Locale.ROOT);
        if ("EUR".equals(cc)) return "🇪🇺"; // EU has its own emoji
        String iso2 = C2F.get(cc);
        return (iso2 == null) ? "" : flagEmoji(iso2);
    }
}

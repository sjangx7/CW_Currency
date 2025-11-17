package org.me.gcu.jang_sae_s2432618;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

public class CurrencyRepository {

    private static final String TAG = "CurrencyRepository";

    private static final DateTimeFormatter RFC822_UTC =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'UTC'")
                    .withZone(ZoneOffset.UTC);

    // Backing MutableLiveData (private), exposed as immutable LiveData (public)
    private final MutableLiveData<List<RateItem>> _rates       =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<String>         _lastUpdated =
            new MutableLiveData<>("");
    private final MutableLiveData<Boolean>        _loading     =
            new MutableLiveData<>(false);
    private final MutableLiveData<String>         _error       =
            new MutableLiveData<>(null);
    private final MutableLiveData<String>         _title       =
            new MutableLiveData<>("British Pound Sterling(GBP) Currency\nExchange Rates");

    public LiveData<List<RateItem>> rates()       { return _rates; }
    public LiveData<String>         lastUpdated() { return _lastUpdated; }
    public LiveData<Boolean>        loading()     { return _loading; }
    public LiveData<String>         error()       { return _error; }
    public LiveData<String>         title()       { return _title; }

    /** Apply a parsed result coming from a worker thread (thread-safe). */
    public void applyParsed(ParseResult pr) {
        if (pr == null) {
            _error.postValue("No parse result");
            _loading.postValue(false);
            return;
        }

        // Log size safely
        final List<RateItem> items = (pr.items == null) ? Collections.emptyList() : pr.items;
        Log.d(TAG, "applyParsed: items=" + items.size());

        // Title (keep previous if parser didn't provide one)
        if (pr.title != null && !pr.title.trim().isEmpty()) {
            _title.postValue(pr.title.trim());
        }

        // Errors (clear if none)
        if (pr.error != null && !pr.error.trim().isEmpty()) {
            _error.postValue(pr.error.trim());
        } else {
            _error.postValue(null);
        }

        // Rates list
        _rates.postValue(items);

        // Show the fetch/apply time so the UI updates every refresh cycle
        _lastUpdated.postValue(RFC822_UTC.format(ZonedDateTime.now(ZoneOffset.UTC)));

        // Done loading
        _loading.postValue(false);
    }

    /** Mark repository as loading (safe from any thread). */
    public void setLoading(boolean v) { _loading.postValue(v); }

    /** Set an error message (safe from any thread). */
    public void setError(String msg)  { _error.postValue(msg); }

    /** Force "updated now" timestamp in RFC-822 UTC (safe from any thread). */
    public void setUpdatedNow() {
        _lastUpdated.postValue(RFC822_UTC.format(ZonedDateTime.now(ZoneOffset.UTC)));
    }

    /**
     * Legacy helper: if you still receive raw RSS xml as a String, parse it here.
     * Safe to call from any thread.
     */
    public void updateFromRssString(String rss) {
        try {
            setLoading(true);
            ParseResult pr = RatesParser.parse(rss);   // assumes your existing parser
            Log.d(TAG, "updateFromRssString -> parsed items=" +
                    (pr.items == null ? 0 : pr.items.size()));
            applyParsed(pr);
        } catch (Exception e) {
            Log.e(TAG, "updateFromRssString parse error", e);
            setError("Parse error: " + e.getMessage());
            setLoading(false);
        }
    }
}

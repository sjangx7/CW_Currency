package org.me.gcu.cw_currency.s2432618;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Collections;
import java.util.List;

public class CurrencyRepository {

    private static final String TAG = "Repo";

    private final MutableLiveData<List<RateItem>> rates       = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<String>         lastUpdated = new MutableLiveData<>("");
    private final MutableLiveData<Boolean>        loading     = new MutableLiveData<>(false);
    private final MutableLiveData<String>         error       = new MutableLiveData<>(null);
    private final MutableLiveData<String>         title       = new MutableLiveData<>("British Pound Sterling(GBP) Currency\nExchange Rates");

    public LiveData<List<RateItem>> rates()       { return rates; }
    public LiveData<String>         lastUpdated() { return lastUpdated; }
    public LiveData<Boolean>        loading()     { return loading; }
    public LiveData<String>         error()       { return error; }
    public LiveData<String>         title()       { return title; }

    /** Call this from a background thread (e.g., RatesViewModel.refreshFromRssAsync). */
    public void applyParsed(ParseResult pr) {
        if (pr == null) {
            setError("No parse result");
            setLoading(false);
            return;
        }
        final List<RateItem> items = (pr.items == null) ? Collections.emptyList() : pr.items;
        Log.d(TAG, "applyParsed: items=" + items.size());

        // postValue() is safe from background threads
        title.postValue(pr.title == null ? title.getValue() : pr.title);
        lastUpdated.postValue(pr.lastUpdated == null ? "" : pr.lastUpdated);
        error.postValue(null);
        rates.postValue(items);
        loading.postValue(false);
    }

    public void setLoading(boolean v) { loading.postValue(v); }
    public void setError(String msg)  { error.postValue(msg); }

    /**
     * Legacy helper if something still hands raw XML to the repo directly.
     * Safe to call from any thread.
     */
    public void updateFromRssString(String xml) {
        try {
            setLoading(true);
            ParseResult pr = RatesParser.parse(xml);
            Log.d(TAG, "updateFromRssString -> parsed items=" + (pr.items == null ? 0 : pr.items.size()));
            applyParsed(pr);
        } catch (Exception e) {
            Log.e(TAG, "updateFromRssString parse error", e);
            setError("Parse error: " + e.getMessage());
            setLoading(false);
        }
    }
}

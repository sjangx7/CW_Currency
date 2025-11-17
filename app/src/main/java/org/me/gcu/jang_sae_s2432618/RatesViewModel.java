package org.me.gcu.jang_sae_s2432618;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RatesViewModel extends ViewModel {

    private static final String TAG = "RatesViewModel";
    private static final long SEARCH_DEBOUNCE_MS = 200L;

    private final CurrencyRepository repo = new CurrencyRepository();

    // Expose immutable LiveData from the repository
    public final LiveData<List<RateItem>> rates       = repo.rates();
    public final LiveData<String>         lastUpdated = repo.lastUpdated();
    public final LiveData<Boolean>        loading     = repo.loading();
    public final LiveData<String>         error       = repo.error();
    public final LiveData<String>         title       = repo.title();

    // Query starts empty so the full list shows initially
    private final MutableLiveData<String> query = new MutableLiveData<>("");

    // Filtered list based on `rates` + `query`
    public final MediatorLiveData<List<RateItem>> filteredRates = new MediatorLiveData<>();

    // Background + main thread helpers
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private Runnable pendingSearch;

    public RatesViewModel() {
        filteredRates.setValue(new ArrayList<>());

        // Recompute when the data changes...
        filteredRates.addSource(rates, list -> {
            Log.d(TAG, "rates changed size=" + (list == null ? 0 : list.size()));
            recompute(list, query.getValue());
        });

        // ...or when the query changes
        filteredRates.addSource(query, q -> {
            Log.d(TAG, "query changed='" + q + "'");
            recompute(rates.getValue(), q);
        });
    }

    private void recompute(List<RateItem> list, String q) {
        if (list == null) {
            filteredRates.setValue(new ArrayList<>());
            return;
        }

        final String needle = (q == null) ? "" : q.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            // Always give a new list instance so ListAdapter detects changes consistently
            filteredRates.setValue(new ArrayList<>(list));
            return;
        }

        List<RateItem> out = new ArrayList<>();
        for (RateItem r : list) {
            String code = safe(r.code);
            String name = safe(r.name);
            String country = safe(r.country);
            if (code.contains(needle) || name.contains(needle) || country.contains(needle)) {
                out.add(r);
            }
        }
        filteredRates.setValue(out);
    }

    private static String safe(String v) {
        return v == null ? "" : v.toLowerCase(Locale.ROOT);
    }

    /** Immediate search (no debounce). */
    public void setQuery(String q) {
        String v = (q == null ? "" : q);
        String current = query.getValue();
        if (current != null && current.equals(v)) return; // skip no-op
        query.setValue(v);
    }

    /** Debounced search to avoid recomputing on every keystroke. */
    public void setQueryDebounced(String q) {
        if (pendingSearch != null) main.removeCallbacks(pendingSearch);
        final String value = (q == null ? "" : q);
        pendingSearch = () -> setQuery(value); // reuse dedup logic
        main.postDelayed(pendingSearch, SEARCH_DEBOUNCE_MS);
    }


    /** Parse RSS XML off the main thread, then push into the repository. */
    public void refreshFromRssAsync(String xml) {
        if (xml == null || xml.trim().isEmpty()) return;

        repo.setLoading(true);
        io.execute(() -> {
            try {
                ParseResult pr = RatesParser.parse(xml);
                Log.d(TAG, "parsed ok items=" + (pr.items == null ? 0 : pr.items.size()));
                repo.applyParsed(pr); // posts to LiveData; observers will fire
                // Re-apply current filter on main thread
                main.post(() -> setQuery(query.getValue()));
            } catch (Exception e) {
                Log.e(TAG, "refreshFromRssAsync", e);
                repo.setError("Parse error: " + e.getMessage());
                repo.setLoading(false);
            }
        });
    }

    /**
     * Convenience: parse and update using the repository helper.
     * NOTE: This runs on the caller's thread; prefer refreshFromRssAsync() for UI code.
     */
    public void refreshFromRss(String rssText) {
        repo.updateFromRssString(rssText);
    }

    /** Bump "Updated:" to now (used by KEY_FETCH_TS listener). */
    public void markRefreshedNow() { repo.setUpdatedNow(); }

    @Override
    protected void onCleared() {
        // Cancel any pending debounced runnable and stop the executor
        if (pendingSearch != null) {
            main.removeCallbacks(pendingSearch);
            pendingSearch = null;
        }
        io.shutdownNow();
        super.onCleared();
    }
}

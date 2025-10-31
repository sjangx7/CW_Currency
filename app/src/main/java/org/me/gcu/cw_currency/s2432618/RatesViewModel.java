package org.me.gcu.cw_currency.s2432618;

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

    private static final String TAG = "VM";

    private final CurrencyRepository repo = new CurrencyRepository();

    public final LiveData<List<RateItem>> rates       = repo.rates();
    public final LiveData<String>         lastUpdated = repo.lastUpdated();
    public final LiveData<Boolean>        loading     = repo.loading();
    public final LiveData<String>         error       = repo.error();
    public final LiveData<String>         title       = repo.title();

    // IMPORTANT: start with an empty string so the full list is shown
    private final MutableLiveData<String> query = new MutableLiveData<>("");

    public final MediatorLiveData<List<RateItem>> filteredRates = new MediatorLiveData<>();

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private Runnable pending;

    public RatesViewModel() {
        filteredRates.setValue(new ArrayList<>());

        filteredRates.addSource(rates, list -> {
            Log.d(TAG, "rates changed size=" + (list == null ? 0 : list.size()));
            recompute(list, query.getValue());
        });
        filteredRates.addSource(query, q -> {
            Log.d(TAG, "query changed='" + q + "'");
            recompute(rates.getValue(), q);
        });
    }

    private void recompute(List<RateItem> list, String q) {
        if (list == null) { filteredRates.setValue(new ArrayList<>()); return; }
        String s = (q == null) ? "" : q.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) { filteredRates.setValue(list); return; }

        List<RateItem> out = new ArrayList<>();
        for (RateItem r : list) {
            String code = safe(r.code);
            String name = safe(r.name);
            String country = safe(r.country);
            if (code.contains(s) || name.contains(s) || country.contains(s)) out.add(r);
        }
        filteredRates.setValue(out);
    }

    private static String safe(String v) { return v == null ? "" : v.toLowerCase(Locale.ROOT); }

    public void setQuery(String q) { query.setValue(q == null ? "" : q); }

    public void setQueryDebounced(String q) {
        if (pending != null) main.removeCallbacks(pending);
        final String v = (q == null ? "" : q);
        pending = () -> query.setValue(v);
        main.postDelayed(pending, 200);
    }

    /** Parse XML off the main thread, then post into repo. */
    public void refreshFromRssAsync(String xml) {
        if (xml == null || xml.trim().isEmpty()) return;
        repo.setLoading(true);
        io.execute(() -> {
            try {
                ParseResult pr = RatesParser.parse(xml);
                Log.d(TAG, "parsed ok items=" + (pr.items == null ? 0 : pr.items.size()));
                repo.applyParsed(pr);              // postValue() -> triggers observers
                main.post(() -> setQuery(query.getValue())); // re-apply current filter
            } catch (Exception e) {
                repo.setError("Parse error: " + e.getMessage());
                repo.setLoading(false);
                Log.e(TAG, "refreshFromRssAsync", e);
            }
        });
    }

    public void refreshFromRss(String rssText) { repo.updateFromRssString(rssText); }

    @Override protected void onCleared() { io.shutdownNow(); }
}

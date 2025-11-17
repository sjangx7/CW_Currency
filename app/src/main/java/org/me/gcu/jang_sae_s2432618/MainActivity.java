package org.me.gcu.jang_sae_s2432618;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    // RSS feed we use for the currency data
    private static final String URL_SOURCE = "https://www.fx-exchange.com/gbp/rss.xml";

    private RatesViewModel vm;
    private RatesAdapter adapter;

    private TextView titleText, updatedText, errorBanner;
    private RecyclerView list;
    private SearchView searchView;
    private View btnUSD, btnEUR, btnJPY;

    // second screen (converter)
    private android.widget.ViewFlipper vf;
    private MaterialToolbar topAppBar;

    private SharedPreferences sp;
    private SharedPreferences.OnSharedPreferenceChangeListener spListener;

    // remember last RSS we actually used so we don’t refresh for no reason
    private String lastAppliedRss = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vf          = findViewById(R.id.vf);
        topAppBar   = findViewById(R.id.topAppBar);
        titleText   = findViewById(R.id.titleText);
        updatedText = findViewById(R.id.updatedText);
        errorBanner = findViewById(R.id.errorBanner);
        list        = findViewById(R.id.ratesList);
        searchView  = findViewById(R.id.searchView);
        btnUSD      = findViewById(R.id.btnUSD);
        btnEUR      = findViewById(R.id.btnEUR);
        btnJPY      = findViewById(R.id.btnJPY);

        if (errorBanner != null) errorBanner.setVisibility(View.GONE);

        // top bar back button + title for converter screen
        if (topAppBar != null) {
            topAppBar.setNavigationIcon(R.drawable.ic_arrow_back_24);
            topAppBar.setNavigationOnClickListener(v -> goBackOrFinish());
            topAppBar.setTitle("Converter");
        }

        vm = new ViewModelProvider(this).get(RatesViewModel.class);

        // set up list + adapter
        adapter = new RatesAdapter(this::openConverter);
        if (list != null) {
            list.setLayoutManager(new LinearLayoutManager(this));
            list.setHasFixedSize(true);
            list.setItemAnimator(null);
            list.setAdapter(adapter);
        }

        // search bar
        if (searchView != null) configureSearchView();

        // quick filter buttons for main currencies
        setupQuickButtons();

        // hook up live data from the view model
        observeViewModel();

        // periodic background refresh (15 mins)
        PeriodicWorkRequest req =
                new PeriodicWorkRequest.Builder(FetchRssWorker.class, 15, TimeUnit.MINUTES)
                        .setConstraints(FetchRssWorker.netConstraints())
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "fetch_rss_periodic",
                ExistingPeriodicWorkPolicy.UPDATE,
                req
        );

        // first load from the network
        fetchRssAndRefresh();

        // handle back button when we’re on converter screen
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { goBackOrFinish(); }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        sp = getSharedPreferences(FetchRssWorker.PREFS, MODE_PRIVATE);

        // listen for changes from the worker (new RSS or new timestamp)
        spListener = (prefs, key) -> {
            if (FetchRssWorker.KEY_RSS.equals(key)) {
                String newer = prefs.getString(FetchRssWorker.KEY_RSS, "");
                if (newer != null && !newer.isEmpty() && !newer.equals(lastAppliedRss)) {
                    vm.refreshFromRssAsync(newer);
                    lastAppliedRss = newer;
                }
            } else if (FetchRssWorker.KEY_FETCH_TS.equals(key)) {
                // worker ran again, just bump the “Updated:” text
                runOnUiThread(vm::markRefreshedNow);
            }
        };
        sp.registerOnSharedPreferenceChangeListener(spListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // if cached RSS is newer than what we used, refresh from that
        SharedPreferences spLocal = getSharedPreferences(FetchRssWorker.PREFS, MODE_PRIVATE);
        String cached = spLocal.getString(FetchRssWorker.KEY_RSS, "");
        if (cached != null && !cached.trim().isEmpty() && !cached.equals(lastAppliedRss)) {
            vm.refreshFromRssAsync(cached);
            lastAppliedRss = cached;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (sp != null && spListener != null) {
            sp.unregisterOnSharedPreferenceChangeListener(spListener);
        }
    }

    // ----------------- search and list setup -----------------

    private void configureSearchView() {
        // keep it always open, no icon
        searchView.setIconifiedByDefault(false);
        searchView.setIconified(false);

        // tweak text colour etc
        TextView svText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        if (svText != null) {
            svText.setTextColor(ContextCompat.getColor(this, android.R.color.black));
            svText.setHintTextColor(0x99000000);
            svText.setSingleLine(true);
        }

        // tap anywhere on the search to focus
        searchView.setOnClickListener(v -> {
            if (svText != null) svText.requestFocus();
        });

        // when user types or submits, update the view model
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) {
                vm.setQuery(query);
                hideKeyboardAndClearSearchFocus();
                return true;
            }
            @Override public boolean onQueryTextChange(String newText) {
                vm.setQueryDebounced(newText);
                return true;
            }
        });

        // when search loses focus, close keyboard
        searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) hideKeyboardAndClearSearchFocus();
        });

        // scrolling the list should also close the keyboard
        if (list != null) {
            list.setOnTouchListener((v, event) -> {
                hideKeyboardAndClearSearchFocus();
                return false;
            });
        }
    }

    // set up the USD / EUR / JPY buttons
    private void setupQuickButtons() {
        View.OnClickListener mainPairClick = v -> {
            String wanted = (v.getId() == R.id.btnUSD) ? "USD"
                    : (v.getId() == R.id.btnEUR) ? "EUR"
                    : "JPY";

            RateItem item = findByCode(vm.rates.getValue(), wanted);
            if (item != null) {
                openConverter(item);
            } else {
                // if we don’t have it loaded yet, at least filter the list
                vm.setQuery(wanted);
            }
        };

        if (btnUSD != null) btnUSD.setOnClickListener(mainPairClick);
        if (btnEUR != null) btnEUR.setOnClickListener(mainPairClick);
        if (btnJPY != null) btnJPY.setOnClickListener(mainPairClick);
    }

    // hook LiveData up to the UI
    private void observeViewModel() {
        vm.title.observe(this, t -> {
            if (t != null && !t.isEmpty() && titleText != null) titleText.setText(t);
        });

        vm.lastUpdated.observe(this, s -> {
            if (updatedText != null) {
                updatedText.setText("Updated: " + (s == null ? "—" : s));
            }
        });

        vm.filteredRates.observe(this, list -> {
            if (adapter != null) adapter.submitList(list);
            if (list != null && !list.isEmpty() && vm.error.getValue() == null && errorBanner != null) {
                errorBanner.setVisibility(View.GONE);
            }
        });

        vm.error.observe(this, e -> {
            if (errorBanner == null) return;
            if (e == null || e.isEmpty()) {
                errorBanner.setVisibility(View.GONE);
            } else {
                errorBanner.setText(e);
                errorBanner.setVisibility(View.VISIBLE);
            }
        });
    }

    // ----------------- converter + navigation -----------------

    private void openConverter(RateItem item) {
        if (item == null) return;

        if (topAppBar != null) {
            topAppBar.setTitle("GBP ↔ " + item.code);
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.converterContainer, ConverterFragment.newInstance(item))
                .addToBackStack("converter")
                .commit();

        if (vf != null && vf.getDisplayedChild() == 0) {
            vf.showNext();
        }
    }

    private void goBackOrFinish() {
        if (vf != null && vf.getDisplayedChild() == 1) {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
            }
            vf.showPrevious();
        } else {
            finish();
        }
    }

    // ----------------- small helpers -----------------

    private void hideKeyboardAndClearSearchFocus() {
        if (searchView != null) {
            searchView.clearFocus();
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
            }
        }
    }

    // download RSS on a background thread and pass it to the view model
    private void fetchRssAndRefresh() {
        new Thread(() -> {
            String rss = "";
            Exception problem = null;

            try {
                java.net.URL url = new java.net.URL(URL_SOURCE);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(10_000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; CurrencyApp/1.0)");

                int code = conn.getResponseCode();
                if (code == 200) {
                    try (java.io.BufferedReader in = new java.io.BufferedReader(
                            new java.io.InputStreamReader(conn.getInputStream(),
                                    java.nio.charset.StandardCharsets.UTF_8))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) sb.append(line).append('\n');
                        rss = sb.toString();
                    }
                } else {
                    problem = new RuntimeException("HTTP " + code);
                }
                conn.disconnect();

                // try to strip out anything before/after the actual RSS XML
                if (rss != null && !rss.isEmpty()) {
                    int i = rss.indexOf("<?xml");
                    if (i >= 0) rss = rss.substring(i);
                    int end = rss.indexOf("</rss>");
                    if (end > 0) rss = rss.substring(0, end + 6);
                }
            } catch (Exception e) {
                problem = e;
            }

            final String finalRss = rss;
            final Exception finalProblem = problem;

            runOnUiThread(() -> {
                if (finalProblem != null) {
                    if (errorBanner != null) {
                        errorBanner.setText("Network error: " +
                                (finalProblem.getMessage() == null
                                        ? finalProblem.getClass().getSimpleName()
                                        : finalProblem.getMessage()));
                        errorBanner.setVisibility(View.VISIBLE);
                    }
                    return;
                }

                if (finalRss == null || finalRss.trim().isEmpty()) {
                    if (errorBanner != null) {
                        errorBanner.setText("No data received. Check connection or try again.");
                        errorBanner.setVisibility(View.VISIBLE);
                    }
                } else {
                    vm.refreshFromRssAsync(finalRss);
                    vm.setQuery("");
                }
            });
        }).start();
    }

    private RateItem findByCode(List<RateItem> list, String code) {
        if (list == null || code == null) return null;
        for (RateItem r : list) if (code.equalsIgnoreCase(r.code)) return r;
        return null;
    }
}

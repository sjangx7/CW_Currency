package org.me.gcu.cw_currency.s2432618;

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

/**
 * Main Activity — handles the currency rates list, search, and in-app converter (single-activity flow)
 */
public class MainActivity extends AppCompatActivity {

    // --------------------------------------------------------------------------------------------
    // Constants
    // --------------------------------------------------------------------------------------------

    private static final String URL_SOURCE = "https://www.fx-exchange.com/gbp/rss.xml";

    // Demo fallback XML for offline/diagnostic use
    private static final String DEMO_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><rss><channel>"
                    + "<title>Demo GBP Rates</title>"
                    + "<lastBuildDate>Tue, 14 Oct 2025 06:00:55 UTC</lastBuildDate>"
                    + "<item><title>AED - United Arab Emirates Dirham</title><description>4.9471</description></item>"
                    + "<item><title>JPY - Japanese Yen</title><description>183.4200</description></item>"
                    + "</channel></rss>";

    // --------------------------------------------------------------------------------------------
    // Views & fields
    // --------------------------------------------------------------------------------------------

    private RatesViewModel vm;
    private RatesAdapter adapter;

    private TextView titleText, updatedText, errorBanner;
    private RecyclerView list;
    private SearchView searchView;
    private View btnUSD, btnEUR, btnJPY;

    // Second page (converter)
    private android.widget.ViewFlipper vf;
    private MaterialToolbar topAppBar;

    // Track last applied RSS to avoid unnecessary refreshes
    private String lastAppliedRss = "";

    // --------------------------------------------------------------------------------------------
    // Lifecycle
    // --------------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- View lookups ---
        vf = findViewById(R.id.vf);
        topAppBar = findViewById(R.id.topAppBar);
        titleText = findViewById(R.id.titleText);
        updatedText = findViewById(R.id.updatedText);
        errorBanner = findViewById(R.id.errorBanner);
        list = findViewById(R.id.ratesList);
        searchView = findViewById(R.id.searchView);
        btnUSD = findViewById(R.id.btnUSD);
        btnEUR = findViewById(R.id.btnEUR);
        btnJPY = findViewById(R.id.btnJPY);

        // --- Toolbar setup ---
        if (topAppBar != null) {
            topAppBar.setNavigationIcon(R.drawable.ic_arrow_back_24);
            topAppBar.setNavigationOnClickListener(v -> goBackOrFinish());
            topAppBar.setTitle("Converter");
        }

        // --- ViewModel ---
        vm = new ViewModelProvider(this).get(RatesViewModel.class);

        // --- RecyclerView setup ---
        adapter = new RatesAdapter(this::openConverter);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setHasFixedSize(true);
        list.setItemAnimator(null);
        list.setAdapter(adapter);

        // --- SearchView setup ---
        configureSearchView();

        // --- Quick currency buttons ---
        setupQuickButtons();

        // --- ViewModel observers ---
        observeViewModel();

        // --- Demo data seed (for first load) ---
        vm.refreshFromRssAsync(DEMO_XML);
        vm.setQuery("");

        // --- Background worker (refresh every 15 min) ---
        PeriodicWorkRequest req =
                new PeriodicWorkRequest.Builder(FetchRssWorker.class, 15, TimeUnit.MINUTES)
                        .setConstraints(FetchRssWorker.netConstraints())
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "fetch_rss_periodic",
                ExistingPeriodicWorkPolicy.UPDATE,
                req
        );

        // --- Initial fetch ---
        fetchRssAndRefresh();

        // --- Handle system back (converter page) ---
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { goBackOrFinish(); }
        });

        // --- Long-press title to load demo ---
        if (titleText != null) {
            titleText.setOnLongClickListener(v -> {
                vm.refreshFromRssAsync(DEMO_XML);
                lastAppliedRss = DEMO_XML;
                errorBanner.setText("");
                errorBanner.setVisibility(View.GONE);
                return true;
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reload cached data if newer
        SharedPreferences sp = getSharedPreferences(FetchRssWorker.PREFS, MODE_PRIVATE);
        String cached = sp.getString(FetchRssWorker.KEY_RSS, "");
        if (cached != null && !cached.trim().isEmpty() && !cached.equals(lastAppliedRss)) {
            vm.refreshFromRssAsync(cached);
            lastAppliedRss = cached;
        }
    }

    // --------------------------------------------------------------------------------------------
    // UI Setup helpers
    // --------------------------------------------------------------------------------------------

    /** Configure SearchView appearance, behavior, and keyboard handling */
    private void configureSearchView() {
        // Always expanded
        searchView.setIconifiedByDefault(false);
        searchView.setIconified(false);

        // Fix invisible text (ensure visible on white background)
        TextView svText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        svText.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        svText.setHintTextColor(0x99000000);
        svText.setSingleLine(true);

        // Focus field when user taps anywhere
        searchView.setOnClickListener(v -> svText.requestFocus());

        // Handle query events
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

        // Hide keyboard when focus lost
        searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) hideKeyboardAndClearSearchFocus();
        });

        // Hide keyboard if user scrolls/taps list
        list.setOnTouchListener((v, event) -> {
            hideKeyboardAndClearSearchFocus();
            return false;
        });
    }

    /** Setup quick-currency buttons (USD, EUR, JPY) */
    private void setupQuickButtons() {
        View.OnClickListener mainPairClick = v -> {
            String wanted = (v.getId() == R.id.btnUSD) ? "USD"
                    : (v.getId() == R.id.btnEUR) ? "EUR"
                    : "JPY";

            RateItem item = findByCode(vm.rates.getValue(), wanted);
            if (item != null) openConverter(item);
            else vm.setQuery(wanted); // fallback: pre-filter
        };

        btnUSD.setOnClickListener(mainPairClick);
        btnEUR.setOnClickListener(mainPairClick);
        btnJPY.setOnClickListener(mainPairClick);
    }

    /** Observe LiveData in the ViewModel */
    private void observeViewModel() {
        vm.title.observe(this, t -> {
            if (t != null && !t.isEmpty()) titleText.setText(t);
        });

        vm.lastUpdated.observe(this, s ->
                updatedText.setText("Updated: " + (s == null ? "—" : s))
        );

        vm.filteredRates.observe(this, list -> {
            adapter.submitList(list);
            if (list != null && !list.isEmpty() && vm.error.getValue() == null) {
                errorBanner.setVisibility(View.GONE);
            }
        });

        vm.error.observe(this, e -> {
            if (e == null || e.isEmpty()) {
                errorBanner.setVisibility(View.GONE);
            } else {
                errorBanner.setText(e);
                errorBanner.setVisibility(View.VISIBLE);
            }
        });
    }

    // --------------------------------------------------------------------------------------------
    // Converter / Navigation
    // --------------------------------------------------------------------------------------------

    /** Opens converter fragment inside the flipper (page 2) */
    private void openConverter(RateItem item) {
        if (topAppBar != null && item != null) {
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

    /** Handles back navigation (converter → list) */
    private void goBackOrFinish() {
        if (vf != null && vf.getDisplayedChild() == 1) {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0)
                getSupportFragmentManager().popBackStack();
            vf.showPrevious();
        } else {
            finish();
        }
    }

    // --------------------------------------------------------------------------------------------
    // Utilities
    // --------------------------------------------------------------------------------------------

    /** Hide the soft keyboard and clear focus from the SearchView */
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

    /** Fetch RSS from network (runs on background thread) */
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

                // Trim to clean XML
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
                    errorBanner.setText("Network error: " +
                            (finalProblem.getMessage() == null
                                    ? finalProblem.getClass().getSimpleName()
                                    : finalProblem.getMessage()));
                    errorBanner.setVisibility(View.VISIBLE);
                    return;
                }

                if (finalRss == null || finalRss.trim().isEmpty()) {
                    errorBanner.setText("No data received. Check connection or try again.");
                    errorBanner.setVisibility(View.VISIBLE);
                } else {
                    vm.refreshFromRssAsync(finalRss);
                    vm.setQuery("");
                }
            });
        }).start();
    }

    /** Find a rate by its currency code */
    private RateItem findByCode(List<RateItem> list, String code) {
        if (list == null || code == null) return null;
        for (RateItem r : list) if (code.equalsIgnoreCase(r.code)) return r;
        return null;
    }
}

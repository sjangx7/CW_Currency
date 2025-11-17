package org.me.gcu.jang_sae_s2432618;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class FetchRssWorker extends Worker {

    // SharedPreferences keys (must match MainActivity listener)
    public static final String PREFS        = "rss_prefs";
    public static final String KEY_RSS      = "rss_text";
    public static final String KEY_FETCH_TS = "rss_fetch_time";

    private static final String URL_SOURCE  = "https://www.fx-exchange.com/gbp/rss.xml";

    public FetchRssWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(URL_SOURCE);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(10_000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; CurrencyApp/1.0)");

            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {

                return Result.retry();
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = in.readLine()) != null) sb.append(line).append('\n');
            }

            // Trim to a clean XML block
            String rss = sb.toString();
            if (rss != null && !rss.isEmpty()) {
                int i = rss.indexOf("<?xml");
                if (i >= 0) rss = rss.substring(i);
                int end = rss.indexOf("</rss>");
                if (end > 0) rss = rss.substring(0, end + 6);
                rss = rss.trim();
            } else {
                return Result.retry();
            }

            // Write to SharedPreferences
            SharedPreferences sp = getApplicationContext()
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor ed = sp.edit();

            // Only update the RSS text if changed
            String old = sp.getString(KEY_RSS, "");
            if (!rss.equals(old)) {
                ed.putString(KEY_RSS, rss);
            }

            // Always bump fetch timestamp so listeners fire every run
            ed.putLong(KEY_FETCH_TS, System.currentTimeMillis());

            ed.apply();
            return Result.success();

        } catch (Exception e) {
            return Result.retry();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /** Network-connected constraint for enqueuing this worker. */
    public static Constraints netConstraints() {
        return new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
    }
}

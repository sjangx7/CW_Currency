package org.me.gcu.cw_currency.s2432618;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class FetchRssWorker extends Worker {

    public static final String PREFS = "cw_prefs";
    public static final String KEY_RSS = "latest_rss";
    public static final String KEY_TIME = "latest_time";
    private static final String URL_SOURCE = "https://www.fx-exchange.com/gbp/rss.xml";

    public FetchRssWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            URL aurl = new URL(URL_SOURCE);
            URLConnection yc = aurl.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) sb.append(line);
            in.close();

            String rss = sb.toString();
            int i = rss.indexOf("<?"); if (i >= 0) rss = rss.substring(i);
            int end = rss.indexOf("</rss>"); if (end > 0) rss = rss.substring(0, end + 6);

            SharedPreferences sp = getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            sp.edit()
                    .putString(KEY_RSS, rss)
                    .putLong(KEY_TIME, System.currentTimeMillis())
                    .apply();

            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }

    public static Constraints netConstraints() {
        return new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
    }
}

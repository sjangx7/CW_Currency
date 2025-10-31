package org.me.gcu.cw_currency.s2432618;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class ConverterActivity extends AppCompatActivity {
    private static final String EXTRA_CODE = "e_code";
    private static final String EXTRA_NAME = "e_name";
    private static final String EXTRA_RATE = "e_rate";

    public static Intent intent(Context c, RateItem item) {
        Intent i = new Intent(c, ConverterActivity.class);
        i.putExtra(EXTRA_CODE, item.code);
        i.putExtra(EXTRA_NAME, item.name);
        i.putExtra(EXTRA_RATE, item.rate);
        return i;
    }

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_converter);

        if (savedInstanceState == null) {
            String code = getIntent().getStringExtra(EXTRA_CODE);
            String name = getIntent().getStringExtra(EXTRA_NAME);
            double rate = getIntent().getDoubleExtra(EXTRA_RATE, 1.0);

            // Build a temp RateItem from the extras
            RateItem tmp = new RateItem(code, name, "", rate);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.converterRoot, ConverterFragment.newInstance(tmp))
                    .commit();
        }
    }
}

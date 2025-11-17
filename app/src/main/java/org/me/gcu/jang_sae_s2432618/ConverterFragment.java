package org.me.gcu.jang_sae_s2432618;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public class ConverterFragment extends Fragment {

    private static final String ARG_CODE = "code";
    private static final String ARG_NAME = "name";
    private static final String ARG_RATE = "rate"; // 1 GBP -> rate * CODE

    public static ConverterFragment newInstance(RateItem item) {
        Bundle b = new Bundle();
        b.putString(ARG_CODE, item.code);
        b.putString(ARG_NAME, item.name);
        b.putDouble(ARG_RATE, item.rate);
        ConverterFragment f = new ConverterFragment();
        f.setArguments(b);
        return f;
    }

    private String code = "USD";
    private String name = "Dollar";
    private double rate = 1.0;

    private TextView pairTitle, rateInfo, resultText, warnText;
    private RadioGroup directionGroup;
    private EditText inputAmount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle s) {
        return inflater.inflate(R.layout.fragment_converter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        // Close (✕) button
        ImageButton closeBtn = v.findViewById(R.id.closeBtn);
        if (closeBtn != null) {
            closeBtn.setOnClickListener(view -> {
                View rightPane = requireActivity().findViewById(R.id.converterContainer);
                if (rightPane != null) {
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .remove(this)
                            .commit();
                } else {
                    requireActivity().finish();
                }
            });
        }

        pairTitle      = v.findViewById(R.id.pairTitle);
        rateInfo       = v.findViewById(R.id.rateInfo);
        resultText     = v.findViewById(R.id.resultText);
        warnText       = v.findViewById(R.id.warnText);
        directionGroup = v.findViewById(R.id.directionGroup);
        inputAmount    = v.findViewById(R.id.inputAmount);
        Button convertBtn = v.findViewById(R.id.convertBtn);

        // Read args
        Bundle args = getArguments();
        if (args != null) {
            String aCode = args.getString(ARG_CODE);
            String aName = args.getString(ARG_NAME);
            double aRate = args.getDouble(ARG_RATE, Double.NaN);
            if (!TextUtils.isEmpty(aCode)) code = aCode;
            if (!TextUtils.isEmpty(aName)) name = aName;
            if (!Double.isNaN(aRate)) rate = aRate;
        }

        pairTitle.setText(String.format(Locale.UK, "GBP ↔ %s", code));


        rateInfo.setText(String.format(
                Locale.UK,
                "Rate (GBP : %s): %s",
                code,
                BigDecimal.valueOf(rate)
                        .setScale(6, RoundingMode.HALF_UP)
                        .toPlainString()
        ));

        convertBtn.setOnClickListener(view -> doConvert());
        directionGroup.setOnCheckedChangeListener((g, id) -> {
            resultText.setText("Result: —");
            warnText.setText("");
        });
    }

    private void doConvert() {
        warnText.setText("");

        String raw = (inputAmount.getText() != null)
                ? inputAmount.getText().toString().trim()
                : "";
        if (TextUtils.isEmpty(raw)) {
            warnText.setText("Please enter an amount.");
            return;
        }

        // allow comma decimal
        raw = raw.replace(',', '.');

        BigDecimal amt;
        try {
            amt = new BigDecimal(raw);
        } catch (NumberFormatException e) {
            warnText.setText("Invalid number.");
            return;
        }

        if (amt.signum() < 0) {
            warnText.setText("Amount must be non-negative.");
            return;
        }
        if (rate <= 0) {
            warnText.setText("Rate unavailable. Try refreshing.");
            return;
        }

        boolean gbpToX = (directionGroup.getCheckedRadioButtonId() == R.id.dirGbpToX);

        BigDecimal rateBD = BigDecimal.valueOf(rate);

        // Do precise math, keep extra precision during division
        BigDecimal rawOut = gbpToX
                ? amt.multiply(rateBD)                              // GBP -> CODE
                : amt.divide(rateBD, 12, RoundingMode.HALF_UP);     // CODE -> GBP

        // Display values at 2 dp
        BigDecimal shownAmt = amt.setScale(2, RoundingMode.HALF_UP);
        BigDecimal shownOut = rawOut.setScale(2, RoundingMode.HALF_UP);

        if (gbpToX) {
            resultText.setText(String.format(
                    Locale.UK,
                    "Result: £%s GBP = %s %s",
                    shownAmt.toPlainString(),
                    shownOut.toPlainString(),
                    code
            ));
        } else {
            resultText.setText(String.format(
                    Locale.UK,
                    "Result: %s %s = £%s GBP",
                    shownAmt.toPlainString(),
                    code,
                    shownOut.toPlainString()
            ));
        }
    }
}

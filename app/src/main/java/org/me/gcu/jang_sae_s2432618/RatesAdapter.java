package org.me.gcu.jang_sae_s2432618;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RatesAdapter extends RecyclerView.Adapter<RatesAdapter.VH> {

    public interface OnItemClick { void onRateClicked(RateItem item); }

    private final List<RateItem> data = new ArrayList<>();
    private final OnItemClick click;

    public RatesAdapter(OnItemClick click) { this.click = click; }

    public void submitList(List<RateItem> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_rate_item, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        final RateItem it = data.get(position);

        // A11y: row-level content description (code, name, rate)
        h.itemView.setContentDescription(
                h.itemView.getResources().getString(
                        R.string.cd_currency_full,
                        it.code,
                        it.name,
                        String.format(Locale.US, "%.4f", it.rate)
                )
        );

        // Flag + code + name
        String flag = "";
        try {
            flag = FlagUtil.flagForCurrency(it.code);
        } catch (Throwable ignore) { /* optional utility */ }

        String title = ((flag == null || flag.isEmpty()) ? "" : (flag + " "))
                + (it.code == null ? "" : it.code)
                + " • "
                + (it.name == null ? "" : it.name);
        h.codeName.setText(title);

        h.country.setText(it.country == null ? "" : it.country);
        h.rate.setText(String.format(Locale.UK, "%.4f", it.rate));

        // Colour by ranges: <1, 1–5, 5–10, >10
        int colorRes;
        if (it.rate < 1.0)       colorRes = R.color.rate_lt_1;
        else if (it.rate < 5.0)  colorRes = R.color.rate_1_5;
        else if (it.rate < 10.0) colorRes = R.color.rate_5_10;
        else                     colorRes = R.color.rate_gt_10;

        int tint = ContextCompat.getColor(h.itemView.getContext(), colorRes);

        // Safely tint the badge background
        Drawable bg = h.rate.getBackground();
        if (bg != null) {
            Drawable wrapped = DrawableCompat.wrap(bg.mutate());
            DrawableCompat.setTint(wrapped, tint);
            h.rate.setBackground(wrapped);
        }

        // Click
        h.itemView.setOnClickListener(v -> {
            if (click != null) click.onRateClicked(it);
        });
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView codeName, country, rate;
        VH(@NonNull View itemView) {
            super(itemView);
            codeName = itemView.findViewById(R.id.codeNameText);
            country  = itemView.findViewById(R.id.countryText);
            rate     = itemView.findViewById(R.id.rateText);
        }
    }
}

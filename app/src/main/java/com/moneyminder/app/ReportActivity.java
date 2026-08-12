package com.moneyminder.app;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.moneyminder.app.model.BudgetStatus;
import com.moneyminder.app.model.Category;
import com.moneyminder.app.model.TipCard;
import com.moneyminder.app.model.WeeklyReport;

import java.util.ArrayList;
import java.util.List;

public class ReportActivity extends AppCompatActivity {

    private static final int[] PASTEL_BG = {
        0xFFD6F5F0, 0xFFFFE0EF, 0xFFDCEBFE,
        0xFFEDE7F6, 0xFFFFEDD5, 0xFFFFE5E3, 0xFFF0EFF5
    };

    private MoneyMinderViewModel vm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        vm = new ViewModelProvider(this).get(MoneyMinderViewModel.class);
        WeeklyReport report = vm.generateReport();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        TextView tvTotal    = findViewById(R.id.tvTotalSpent);
        TextView tvPct      = findViewById(R.id.tvBudgetPct);
        TextView tvStreak   = findViewById(R.id.tvStreakBanner);
        TextView tvJarTotal = findViewById(R.id.tvJarTotal);
        TextView tvTxnCount = findViewById(R.id.tvTxnCount);
        TextView tvSubtitle = findViewById(R.id.tvReportSubtitle);

        tvTotal.setText(String.format("₹%.2f", report.total));
        int pct = report.totalBudget > 0 ? (int)(report.total / report.totalBudget * 100) : 0;
        tvPct.setText(pct + "%");

        double jar = vm.roundUpJar.getValue() != null ? vm.roundUpJar.getValue() : 0;
        tvJarTotal.setText(String.format("₹%.2f", jar));

        List<com.moneyminder.app.model.Transaction> txns = vm.transactions.getValue();
        long weekStart = vm.weekStart.getValue() != null ? vm.weekStart.getValue() : 0L;
        int count = 0;
        if (txns != null) {
            for (com.moneyminder.app.model.Transaction t : txns)
                if (t.timestamp >= weekStart) count++;
        }
        tvTxnCount.setText(String.valueOf(count));

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault());
        String weekLabel = sdf.format(new java.util.Date(weekStart)) + " – " +
                           sdf.format(new java.util.Date(weekStart + 6L * 86400000));
        tvSubtitle.setText(weekLabel + "  ·  " + count + " purchase" + (count == 1 ? "" : "s"));

        if (report.streakWeeks > 0) {
            tvStreak.setVisibility(View.VISIBLE);
            tvStreak.setText("🔥 " + report.streakWeeks + " week"
                    + (report.streakWeeks == 1 ? "" : "s") + " in a row staying on budget!");
        } else {
            tvStreak.setVisibility(View.GONE);
        }

        ProgressBar progressOverall = findViewById(R.id.progressOverall);
        TextView tvOverallPct       = findViewById(R.id.tvOverallPct);
        TextView tvRemaining        = findViewById(R.id.tvRemainingBudget);

        progressOverall.setMax(100);
        progressOverall.setProgress(Math.min(100, pct));
        tvOverallPct.setText(pct + "%");

        int barColor;
        double remaining = report.totalBudget - report.total;
        if (pct >= 100) {
            barColor = Color.parseColor("#FF6B5E");
            tvRemaining.setText(String.format("Over by ₹%.2f ⚠️", -remaining));
            tvRemaining.setTextColor(barColor);
        } else if (pct >= 80) {
            barColor = Color.parseColor("#FF8C42");
            tvRemaining.setText(String.format("₹%.2f remaining — be careful 👀", remaining));
            tvRemaining.setTextColor(Color.parseColor("#B8860B"));
        } else {
            barColor = Color.parseColor("#7B5EA7");
            tvRemaining.setText(String.format("₹%.2f remaining — great job! 🎯", remaining));
            tvRemaining.setTextColor(Color.parseColor("#9498B3"));
        }
        progressOverall.getProgressDrawable().setColorFilter(barColor, android.graphics.PorterDuff.Mode.SRC_IN);

        setupChart(findViewById(R.id.pieChart), report);

        LinearLayout llBreakdown = findViewById(R.id.llCategoryBreakdown);
        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) {
            Category cat = cats[i];
            double spent = report.byCategory.getOrDefault(cat, 0.0);
            BudgetStatus status = vm.checkBudgetLimit(cat);
            if (spent == 0 && status.limit == 0) continue;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(32f);
            bg.setColor(PASTEL_BG[i]);
            row.setBackground(bg);
            int pad = dp(14);
            row.setPadding(pad, pad, pad, pad);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowLp.bottomMargin = dp(8);
            row.setLayoutParams(rowLp);

            LinearLayout top = new LinearLayout(this);
            top.setOrientation(LinearLayout.HORIZONTAL);
            top.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView emojiView = new TextView(this);
            emojiView.setText(cat.emoji);
            emojiView.setTextSize(18f);
            LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(dp(36), dp(36));
            elp.rightMargin = dp(10);
            emojiView.setLayoutParams(elp);
            emojiView.setGravity(android.view.Gravity.CENTER);

            TextView nameView = new TextView(this);
            nameView.setText(cat.label);
            nameView.setTextColor(Color.parseColor("#14163A"));
            nameView.setTextSize(13f);
            nameView.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            nameView.setLayoutParams(nlp);

            TextView amtView = new TextView(this);
            amtView.setText(String.format("₹%.0f / ₹%.0f", spent, status.limit));
            amtView.setTextColor(Color.parseColor("#5B5F7B"));
            amtView.setTextSize(12f);
            amtView.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);

            top.addView(emojiView);
            top.addView(nameView);
            top.addView(amtView);

            ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            bar.setMax(100);
            bar.setProgress((int) Math.min(100, status.pct * 100));
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(8));
            blp.topMargin = dp(8);
            bar.setLayoutParams(blp);
            int catBarColor = status.state == BudgetStatus.State.OVER ? Color.parseColor("#FF6B5E") :
                              status.state == BudgetStatus.State.WARNING ? Color.parseColor("#FF8C42") : cat.color;
            bar.getProgressDrawable().setColorFilter(catBarColor, android.graphics.PorterDuff.Mode.SRC_IN);

            row.addView(top);
            row.addView(bar);
            llBreakdown.addView(row);
        }

        List<com.moneyminder.app.model.RecurringCharge> recurring = vm.detectRecurringCharges();
        TextView tvRecurringHeading = findViewById(R.id.tvRecurringHeading);
        LinearLayout llRecurring = findViewById(R.id.llRecurring);
        llRecurring.removeAllViews();
        if (!recurring.isEmpty()) {
            tvRecurringHeading.setVisibility(View.VISIBLE);
            for (com.moneyminder.app.model.RecurringCharge r : recurring) {
                addRecurringChargeView(llRecurring, r);
            }
        } else {
            tvRecurringHeading.setVisibility(View.GONE);
        }

        TextView tvStoryHeading = findViewById(R.id.tvStoryHeading);
        View hsvStories         = findViewById(R.id.hsvStories);
        LinearLayout llStories  = findViewById(R.id.llStories);
        llStories.removeAllViews();
        if (!report.storyCards.isEmpty()) {
            tvStoryHeading.setVisibility(View.VISIBLE);
            hsvStories.setVisibility(View.VISIBLE);
            for (TipCard card : report.storyCards) addStoryCard(llStories, card);
        } else {
            tvStoryHeading.setVisibility(View.GONE);
            hsvStories.setVisibility(View.GONE);
        }

        LinearLayout llTips = findViewById(R.id.llTips);
        llTips.removeAllViews();
        if (report.tipCards.isEmpty()) {
            addTipView(llTips, "Log a few purchases to see personalised tips here.", Color.parseColor("#9498B3"));
        } else {
            for (TipCard card : report.tipCards) addTipCard(llTips, card);
        }
    }

    private int urgencyColor(TipCard.Urgency u) {
        switch (u) {
            case RED:    return Color.parseColor("#FF6B5E");
            case YELLOW: return Color.parseColor("#E0A100");
            case GREEN:  return Color.parseColor("#2FBFAA");
            case BLUE:   return Color.parseColor("#4285F4");
            default:     return Color.parseColor("#7B5EA7");
        }
    }

    private int urgencyTint(TipCard.Urgency u) {
        switch (u) {
            case RED:    return Color.parseColor("#FFF1EF");
            case YELLOW: return Color.parseColor("#FFF8E5");
            case GREEN:  return Color.parseColor("#EAFBF7");
            case BLUE:   return Color.parseColor("#EDF3FF");
            default:     return Color.parseColor("#F5F0FB");
        }
    }

    private void addTipCard(LinearLayout parent, TipCard card) {
        int accent = urgencyColor(card.urgency);
        int tint   = urgencyTint(card.urgency);

        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.HORIZONTAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(16));
        bg.setColor(tint);
        outer.setBackground(bg);
        LinearLayout.LayoutParams olp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        olp.bottomMargin = dp(10);
        outer.setLayoutParams(olp);

        View strip = new View(this);
        strip.setBackgroundColor(accent);
        strip.setLayoutParams(new LinearLayout.LayoutParams(dp(5), LinearLayout.LayoutParams.MATCH_PARENT));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(14);
        content.setPadding(pad, pad, pad, pad);
        content.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView iconView = new TextView(this);
        iconView.setText(card.icon);
        iconView.setTextSize(16f);
        GradientDrawable bubble = new GradientDrawable();
        bubble.setShape(GradientDrawable.OVAL);
        bubble.setColor(Color.WHITE);
        iconView.setBackground(bubble);
        iconView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(dp(32), dp(32));
        ilp.rightMargin = dp(10);
        iconView.setLayoutParams(ilp);

        TextView headerView = new TextView(this);
        headerView.setText(card.header);
        headerView.setTextColor(Color.parseColor("#14163A"));
        headerView.setTextSize(13.5f);
        headerView.setTypeface(null, Typeface.BOLD);

        headerRow.addView(iconView);
        headerRow.addView(headerView);

        TextView msgView = new TextView(this);
        msgView.setText(card.message);
        msgView.setTextColor(Color.parseColor("#4A4E6B"));
        msgView.setTextSize(12.5f);
        msgView.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mlp.topMargin = dp(8);
        msgView.setLayoutParams(mlp);

        content.addView(headerRow);
        content.addView(msgView);

        if (card.savingsTag != null || card.actionLabel != null) {
            LinearLayout bottomRow = new LinearLayout(this);
            bottomRow.setOrientation(LinearLayout.HORIZONTAL);
            bottomRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            blp.topMargin = dp(10);
            bottomRow.setLayoutParams(blp);

            if (card.savingsTag != null) {
                TextView tagView = new TextView(this);
                tagView.setText(card.savingsTag);
                tagView.setTextSize(11f);
                tagView.setTypeface(null, Typeface.BOLD);
                tagView.setTextColor(accent);
                GradientDrawable tagBg = new GradientDrawable();
                tagBg.setShape(GradientDrawable.RECTANGLE);
                tagBg.setCornerRadius(dp(20));
                tagBg.setColor(Color.WHITE);
                tagView.setBackground(tagBg);
                tagView.setPadding(dp(10), dp(5), dp(10), dp(5));
                LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                tagView.setLayoutParams(tlp);
                bottomRow.addView(tagView);
            } else {
                View spacer = new View(this);
                spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1f));
                bottomRow.addView(spacer);
            }

            if (card.actionLabel != null) {
                Button actionBtn = new Button(this);
                actionBtn.setText(card.actionLabel);
                actionBtn.setTextSize(11.5f);
                actionBtn.setTypeface(null, Typeface.BOLD);
                actionBtn.setTextColor(Color.WHITE);
                actionBtn.setAllCaps(false);
                actionBtn.setMinHeight(0);
                actionBtn.setMinimumHeight(0);
                actionBtn.setPadding(dp(14), dp(8), dp(14), dp(8));
                GradientDrawable btnBg = new GradientDrawable();
                btnBg.setShape(GradientDrawable.RECTANGLE);
                btnBg.setCornerRadius(dp(20));
                btnBg.setColor(accent);
                actionBtn.setBackground(btnBg);
                LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                alp.leftMargin = dp(8);
                actionBtn.setLayoutParams(alp);
                actionBtn.setOnClickListener(v -> handleTipAction(card));
                bottomRow.addView(actionBtn);
            }

            content.addView(bottomRow);
        }

        outer.addView(strip);
        outer.addView(content);
        parent.addView(outer);
    }

    private void addStoryCard(LinearLayout parent, TipCard card) {
        int accent = urgencyColor(card.urgency);
        int tint   = urgencyTint(card.urgency);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(16));
        bg.setColor(tint);
        bg.setStroke(dp(1), accent & 0x55FFFFFF | 0x33000000);
        col.setBackground(bg);
        int pad = dp(14);
        col.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(dp(200), LinearLayout.LayoutParams.WRAP_CONTENT);
        clp.rightMargin = dp(10);
        col.setLayoutParams(clp);

        TextView iconView = new TextView(this);
        iconView.setText(card.icon);
        iconView.setTextSize(20f);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ilp.bottomMargin = dp(8);
        iconView.setLayoutParams(ilp);

        TextView headerView = new TextView(this);
        headerView.setText(card.header);
        headerView.setTextColor(Color.parseColor("#14163A"));
        headerView.setTextSize(12.5f);
        headerView.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hlp.bottomMargin = dp(4);
        headerView.setLayoutParams(hlp);

        TextView msgView = new TextView(this);
        msgView.setText(card.message);
        msgView.setTextColor(Color.parseColor("#4A4E6B"));
        msgView.setTextSize(11.5f);
        msgView.setLineSpacing(dp(1), 1f);

        col.addView(iconView);
        col.addView(headerView);
        col.addView(msgView);

        if (card.savingsTag != null) {
            TextView tagView = new TextView(this);
            tagView.setText(card.savingsTag);
            tagView.setTextSize(10.5f);
            tagView.setTypeface(null, Typeface.BOLD);
            tagView.setTextColor(accent);
            GradientDrawable tagBg = new GradientDrawable();
            tagBg.setShape(GradientDrawable.RECTANGLE);
            tagBg.setCornerRadius(dp(20));
            tagBg.setColor(Color.WHITE);
            tagView.setBackground(tagBg);
            tagView.setPadding(dp(9), dp(4), dp(9), dp(4));
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            tlp.topMargin = dp(8);
            tagView.setLayoutParams(tlp);
            col.addView(tagView);
        }

        if (card.actionLabel != null) {
            col.setOnClickListener(v -> handleTipAction(card));
        }

        parent.addView(col);
    }

    private void handleTipAction(TipCard card) {
        if (card.actionKey == null) return;
        if (card.actionKey.startsWith("cap:")) {
            String catName = card.actionKey.substring(4);
            try {
                Category cat = Category.valueOf(catName);
                BudgetStatus s = vm.checkBudgetLimit(cat);
                double newLimit = Math.max(50, s.limit * 0.85);
                vm.updateBudget(cat, newLimit);
                Toast.makeText(this, cat.emoji + " " + cat.label + " limit tightened to ₹" + (int) newLimit,
                        Toast.LENGTH_SHORT).show();
                recreate();
            } catch (IllegalArgumentException ignored) {}
        } else if (card.actionKey.equals("open_edit_budgets")) {
            startActivity(new Intent(this, EditBudgetsActivity.class));
        } else if (card.actionKey.equals("distribute_jar")) {
            vm.autoDistributeRoundUps();
            Toast.makeText(this, "🎉 Round-up jar distributed to your goals!", Toast.LENGTH_SHORT).show();
            recreate();
        } else if (card.actionKey.equals("open_jars")) {
            startActivity(new Intent(this, SavingsJarsActivity.class));
        } else if (card.actionKey.equals("open_report")) {
            
            Toast.makeText(this, "You're already viewing this week's report 👀", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupChart(PieChart chart, WeeklyReport report) {
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors  = new ArrayList<>();
        for (Category cat : Category.values()) {
            double amt = report.byCategory.getOrDefault(cat, 0.0);
            if (amt > 0) { entries.add(new PieEntry((float) amt, cat.label)); colors.add(cat.color); }
        }
        if (entries.isEmpty()) {
            chart.setNoDataText("Log a few purchases to see your breakdown here.");
            chart.setNoDataTextColor(Color.parseColor("#9498B3"));
            chart.invalidate(); return;
        }
        PieDataSet ds = new PieDataSet(entries, "");
        ds.setColors(colors); ds.setValueTextSize(11f);
        ds.setValueTextColor(Color.parseColor("#14163A")); ds.setSliceSpace(2f);
        PieData data = new PieData(ds);
        data.setValueFormatter(new PercentFormatter(chart));
        chart.setData(data);
        chart.setUsePercentValues(true);
        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(Color.parseColor("#F5F3FF"));
        chart.setHoleRadius(60f); chart.setTransparentCircleRadius(64f);
        chart.setDrawEntryLabels(false); chart.setDescription(null);
        chart.animateY(800, Easing.EaseInOutQuad);
        Legend l = chart.getLegend();
        l.setTextColor(Color.parseColor("#5B5F7B")); l.setTextSize(11f);
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.CENTER);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        l.setOrientation(Legend.LegendOrientation.VERTICAL);
        chart.invalidate();
    }

    private void addRecurringChargeView(LinearLayout parent, com.moneyminder.app.model.RecurringCharge r) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(14));
        bg.setColor(Color.WHITE);
        row.setBackground(bg);
        int pad = dp(14);
        row.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.bottomMargin = dp(8);
        row.setLayoutParams(rowLp);

        TextView emojiView = new TextView(this);
        emojiView.setText(r.category.emoji);
        emojiView.setTextSize(18f);
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(dp(36), dp(36));
        elp.rightMargin = dp(10);
        emojiView.setLayoutParams(elp);
        emojiView.setGravity(android.view.Gravity.CENTER);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textCol.setLayoutParams(tlp);

        TextView nameView = new TextView(this);
        nameView.setText(r.description);
        nameView.setTextColor(Color.parseColor("#14163A"));
        nameView.setTextSize(13f);
        nameView.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView subView = new TextView(this);
        subView.setText(r.occurrences + " charges · ~₹" + String.format("%.2f", r.monthlyAmount) + "/mo");
        subView.setTextColor(Color.parseColor("#9498B3"));
        subView.setTextSize(11f);

        textCol.addView(nameView);
        textCol.addView(subView);

        TextView annualView = new TextView(this);
        annualView.setText(String.format("₹%.0f/yr", r.annualCost));
        annualView.setTextColor(Color.parseColor("#FF6B5E"));
        annualView.setTextSize(13f);
        annualView.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);

        row.addView(emojiView);
        row.addView(textCol);
        row.addView(annualView);
        parent.addView(row);
    }

    private void addTipView(LinearLayout parent, String text, int textColor) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(13f);
        tv.setTextColor(textColor);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(14));
        bg.setColor(Color.WHITE);
        tv.setBackground(bg);
        int pad = dp(14);
        tv.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        tv.setLayoutParams(lp);
        parent.addView(tv);
    }

    private int dp(int dp) {
        return (int)(dp * getResources().getDisplayMetrics().density);
    }
}

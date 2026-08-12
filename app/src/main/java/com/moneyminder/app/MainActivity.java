package com.moneyminder.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.moneyminder.app.model.BudgetStatus;
import com.moneyminder.app.model.Category;
import com.moneyminder.app.model.Transaction;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_UI = "centsibl_ui";
    private static final String KEY_NAME = "user_name";

    private static final int[] PASTEL_BG = {
        0xFFD6F5F0,
        0xFFFFE0EF,
        0xFFDCEBFE,
        0xFFEDE7F6,
        0xFFFFEDD5,
        0xFFFFE5E3,
        0xFFF0EFF5
    };

    private MoneyMinderViewModel vm;
    private SharedPreferences uiPrefs;

    private EditText etLog;
    private EditText etAfford;
    private TextView tvAffordResult;
    private TextView tvPreview, tvStreak, tvJar, tvEmpty, tvGreeting;
    private LinearLayout llBudgets;
    private RecyclerView rvTransactions;
    private TransactionAdapter adapter;
    
    private android.widget.ProgressBar progressWeek;
    private TextView tvWeekSpent, tvWeekBudget, tvWeekPct, tvWeekStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vm       = new ViewModelProvider(this).get(MoneyMinderViewModel.class);
        uiPrefs  = getSharedPreferences(PREFS_UI, MODE_PRIVATE);

        etLog          = findViewById(R.id.etLog);
        etAfford       = findViewById(R.id.etAfford);
        tvAffordResult = findViewById(R.id.tvAffordResult);
        tvPreview      = findViewById(R.id.tvPreview);
        tvStreak       = findViewById(R.id.tvStreak);
        tvJar          = findViewById(R.id.tvJar);
        tvEmpty        = findViewById(R.id.tvEmpty);
        llBudgets      = findViewById(R.id.llBudgets);
        rvTransactions = findViewById(R.id.rvTransactions);
        tvGreeting     = findViewById(R.id.tvGreeting);
        progressWeek   = findViewById(R.id.progressWeek);
        tvWeekSpent    = findViewById(R.id.tvWeekSpent);
        tvWeekBudget   = findViewById(R.id.tvWeekBudget);
        tvWeekPct      = findViewById(R.id.tvWeekPct);
        tvWeekStatus   = findViewById(R.id.tvWeekStatus);

        adapter = new TransactionAdapter(
                vm.transactions.getValue() != null ? vm.transactions.getValue() : new ArrayList<>());
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(adapter);

        String savedName = uiPrefs.getString(KEY_NAME, null);
        if (savedName == null) {
            askForName();
        } else {
            tvGreeting.setText("👋 Hello, " + savedName + "!  ✏️");
        }
        
        tvGreeting.setOnClickListener(v -> showEditNameDialog());

        findViewById(R.id.btnLog).setOnClickListener(v -> submitLog());
        etLog.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                submitLog(); return true;
            }
            return false;
        });
        etLog.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { updatePreview(s.toString()); }
            public void afterTextChanged(android.text.Editable s) {}
        });

        findViewById(R.id.btnAfford).setOnClickListener(v -> submitAffordabilityCheck());
        etAfford.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                submitAffordabilityCheck(); return true;
            }
            return false;
        });

        tvJar.setOnClickListener(v -> showJarExplanation());

        findViewById(R.id.btnReport).setOnClickListener(v ->
                startActivity(new Intent(this, ReportActivity.class)));

        findViewById(R.id.btnSavingsJars).setOnClickListener(v ->
                startActivity(new Intent(this, SavingsJarsActivity.class)));

        findViewById(R.id.tvEditAll).setOnClickListener(v ->
                startActivity(new Intent(this, EditBudgetsActivity.class)));

        findViewById(R.id.btnAddIncome).setOnClickListener(v -> showAddIncomeDialog());

        findViewById(R.id.btnNewWeek).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Start a new week?")
                .setMessage("Your current week's data will be archived and a fresh week begins.")
                .setPositiveButton("Yes, start fresh", (d, w) -> {
                    vm.startNewWeek();
                    playSound(R.raw.sound_chime);
                    Toast.makeText(this, "🎉 Fresh week started!", Toast.LENGTH_SHORT).show();
                    renderAll();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        findViewById(R.id.btnReset).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Reset all data?")
                .setMessage("This will permanently delete all transactions, budgets and your streak. This cannot be undone.")
                .setPositiveButton("Reset everything", (d, w) -> {
                    vm.resetAllData();
                    playSound(R.raw.sound_over);
                    Toast.makeText(this, "Data reset.", Toast.LENGTH_SHORT).show();
                    renderAll();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        vm.transactions.observe(this, txns -> renderAll());
        vm.budgets.observe(this, b -> renderAll());

        vm.roundUpJar.observe(this, jar ->
                tvJar.setText(String.format("🫙 ₹%.2f jar", jar)));

        vm.streakWeeks.observe(this, streak ->
                tvStreak.setText(streak > 0 ? "🔥 " + streak + " week streak" : "🔥 Start a streak"));

        vm.lastEvent.observe(this, event -> {
            if (event == null) return;
            String[] parts = event.split(":");
            switch (parts[0]) {
                case "no_amount":
                    playSound(R.raw.sound_over);
                    Toast.makeText(this, "⚠️ Couldn't find an amount — try \"₹20 on lunch\"", Toast.LENGTH_LONG).show();
                    break;
                case "over":
                    playSound(R.raw.sound_over);
                    Toast.makeText(this, "🚨 Over budget in " + safeGet(parts, 1) + "!", Toast.LENGTH_LONG).show();
                    break;
                case "warning":
                    playSound(R.raw.sound_warning);
                    Toast.makeText(this, "👀 " + safeGet(parts, 1) + " is getting close to its limit", Toast.LENGTH_LONG).show();
                    break;
                case "income":
                    playSound(R.raw.sound_coin);
                    Toast.makeText(this, "💵 Saved ₹" + safeGet(parts, 1) + " (20%) · ₹" + safeGet(parts, 2) + " is yours to spend",
                            Toast.LENGTH_LONG).show();
                    break;
                default:
                    playSound(R.raw.sound_success);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> playSound(R.raw.sound_coin), 250);
                    Toast.makeText(this, "✅ Logged!", Toast.LENGTH_SHORT).show();
            }
            vm.consumeEvent();
        });

        renderAll();
    }

    private String safeGet(String[] arr, int idx) {
        return (arr != null && arr.length > idx) ? arr[idx] : "that category";
    }

    private void askForName() {
        final EditText input = new EditText(this);
        input.setHint("Your name");
        input.setPadding(40, 20, 40, 20);

        new AlertDialog.Builder(this)
            .setTitle("👋 Welcome to Centsibl!")
            .setMessage("What should we call you?")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Let's go!", (d, w) -> {
                String name = input.getText().toString().trim();
                if (name.isEmpty()) name = "there";
                uiPrefs.edit().putString(KEY_NAME, name).apply();
                tvGreeting.setText("👋 Hello, " + name + "!  ✏️");
                playSound(R.raw.sound_chime);
            })
            .show();
    }

    private void showEditNameDialog() {
        final EditText input = new EditText(this);
        input.setHint("Your name");
        String current = uiPrefs.getString(KEY_NAME, "");
        input.setText(current);
        if (current != null) input.setSelection(current.length());
        input.setPadding(40, 20, 40, 20);

        new AlertDialog.Builder(this)
            .setTitle("✏️ Edit your name")
            .setView(input)
            .setPositiveButton("Save", (d, w) -> {
                String name = input.getText().toString().trim();
                if (name.isEmpty()) name = "there";
                uiPrefs.edit().putString(KEY_NAME, name).apply();
                tvGreeting.setText("👋 Hello, " + name + "!  ✏️");
                playSound(R.raw.sound_chime);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showJarExplanation() {
        double jar = vm.roundUpJar.getValue() != null ? vm.roundUpJar.getValue() : 0;
        new AlertDialog.Builder(this)
            .setTitle("🫙 Round-Up Savings Jar")
            .setMessage(
                "Every time you log a purchase, the app rounds up to the next whole rupee " +
                "and saves the spare change in your jar.\n\n" +
                "Example: you log ₹43.60 → ₹0.40 goes into the jar automatically.\n\n" +
                "Current jar balance: ₹" + String.format("%.2f", jar) + "\n\n" +
                "Round-ups add up " +
                "to real savings over time without you even noticing!"
            )
            .setPositiveButton("Got it 👍", null)
            .show();
    }

    private void showAddIncomeDialog() {
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Income amount in ₹");
        input.setPadding(40, 20, 40, 20);

        new AlertDialog.Builder(this)
            .setTitle("💵 Log income")
            .setMessage("Enter how much you received. We'll automatically set aside 20% into savings — the rest is yours to spend.")
            .setView(input)
            .setPositiveButton("Save 20%", (dialog, which) -> {
                String val = input.getText().toString().trim();
                if (val.isEmpty()) return;
                try {
                    double amount = Double.parseDouble(val);
                    if (amount <= 0) return;
                    vm.logIncome(amount, null);
                } catch (NumberFormatException ignored) {}
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void playSound(int rawResId) {
        try {
            MediaPlayer mp = MediaPlayer.create(this, rawResId);
            if (mp == null) return;
            mp.setOnCompletionListener(MediaPlayer::release);
            mp.start();
        } catch (Exception ignored) {}
    }

    private void submitLog() {
        String text = etLog.getText().toString().trim();
        if (text.isEmpty()) return;
        vm.logExpense(text);
        etLog.setText("");
        tvPreview.setVisibility(View.GONE);
    }

    private void submitAffordabilityCheck() {
        String text = etAfford.getText().toString().trim();
        if (text.isEmpty()) return;
        String verdict = vm.checkAffordability(text);
        tvAffordResult.setText(verdict);
        tvAffordResult.setVisibility(View.VISIBLE);
        playSound(verdict.contains("Wait") || verdict.contains("Skip") ? R.raw.sound_warning : R.raw.sound_chime);
    }

    private void updatePreview(String text) {
        if (text.trim().isEmpty()) { tvPreview.setVisibility(View.GONE); return; }
        double amount = vm.parseAmount(text);
        if (amount < 0) { tvPreview.setVisibility(View.GONE); return; }
        Category cat = vm.categorizeExpense(text);
        tvPreview.setText(String.format("→ ₹%.2f  %s  %s", amount, cat.emoji, cat.label));
        tvPreview.setVisibility(View.VISIBLE);
    }

    private void renderAll() {
        renderWeeklyBar();
        renderBudgets();
        List<Transaction> txns = vm.transactions.getValue();
        boolean empty = (txns == null || txns.isEmpty());
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvTransactions.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (!empty) adapter.update(txns);
    }

    private void renderWeeklyBar() {
        double totalSpent = 0;
        double totalBudget = 0;
        java.util.Map<com.moneyminder.app.model.Category, Double> budgetMap = vm.budgets.getValue();
        for (com.moneyminder.app.model.Category cat : com.moneyminder.app.model.Category.values()) {
            com.moneyminder.app.model.BudgetStatus s = vm.checkBudgetLimit(cat);
            totalSpent += s.spent;
            totalBudget += s.limit;
        }
        tvWeekSpent.setText(String.format("₹%.0f", totalSpent));
        tvWeekBudget.setText(String.format("₹%.0f", totalBudget));
        int pct = totalBudget > 0 ? (int) Math.min(100, totalSpent / totalBudget * 100) : 0;
        tvWeekPct.setText(pct + "%");
        progressWeek.setMax(100);
        progressWeek.setProgress(pct);
        double remaining = totalBudget - totalSpent;
        if (pct >= 100) {
            progressWeek.getProgressDrawable().setColorFilter(Color.parseColor("#FF6B5E"), android.graphics.PorterDuff.Mode.SRC_IN);
            tvWeekStatus.setText("Over budget this week ⚠️");
            tvWeekStatus.setTextColor(Color.parseColor("#FF6B5E"));
        } else if (pct >= 80) {
            progressWeek.getProgressDrawable().setColorFilter(Color.parseColor("#FF8C42"), android.graphics.PorterDuff.Mode.SRC_IN);
            tvWeekStatus.setText(String.format("₹%.0f remaining — spend carefully 👀", remaining));
            tvWeekStatus.setTextColor(Color.parseColor("#B8860B"));
        } else {
            progressWeek.getProgressDrawable().setColorFilter(Color.parseColor("#7B5EA7"), android.graphics.PorterDuff.Mode.SRC_IN);
            tvWeekStatus.setText(String.format("₹%.0f remaining — you're on track 🎯", remaining));
            tvWeekStatus.setTextColor(Color.parseColor("#9498B3"));
        }
    }

    private void renderBudgets() {
        llBudgets.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        Category[] cats = Category.values();

        for (int i = 0; i < cats.length; i++) {
            Category cat = cats[i];
            BudgetStatus status = vm.checkBudgetLimit(cat);

            View row = inflater.inflate(R.layout.item_budget_row, llBudgets, false);

            TextView tvEmoji   = row.findViewById(R.id.tvCatEmoji);
            TextView tvName    = row.findViewById(R.id.tvCatName);
            TextView tvAmounts = row.findViewById(R.id.tvCatAmounts);
            ProgressBar bar    = row.findViewById(R.id.progressBar);
            TextView tvStatus  = row.findViewById(R.id.tvCatStatus);

            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setShape(GradientDrawable.RECTANGLE);
            cardBg.setCornerRadius(48f);
            cardBg.setColor(PASTEL_BG[i]);
            row.setBackground(cardBg);

            GradientDrawable bubble = new GradientDrawable();
            bubble.setShape(GradientDrawable.OVAL);
            bubble.setColor(cat.color & 0x33FFFFFF | 0x28000000);
            bubble.setColor(PASTEL_BG[i]);
            bubble.setStroke(2, cat.color);
            tvEmoji.setBackground(bubble);
            tvEmoji.setText(cat.emoji);

            tvName.setText(cat.label);
            tvAmounts.setText(String.format("₹%.0f / ₹%.0f", status.spent, status.limit));

            bar.setMax(100);
            bar.setProgress((int) Math.min(100, status.pct * 100));

            int barColor;
            String statusText;
            switch (status.state) {
                case OVER:
                    barColor = Color.parseColor("#FF6B5E");
                    statusText = String.format("Over by ₹%.2f ⚠️", status.spent - status.limit);
                    tvStatus.setTextColor(barColor);
                    break;
                case WARNING:
                    barColor = Color.parseColor("#FF8C42");
                    statusText = (int)(status.pct * 100) + "% used — getting close!";
                    tvStatus.setTextColor(Color.parseColor("#B8860B"));
                    break;
                default:
                    barColor = cat.color;
                    statusText = status.limit > 0 ? ((int)(status.pct * 100) + "% used") : "Tap to set a limit";
                    tvStatus.setTextColor(Color.parseColor("#9498B3"));

                    String bustDay = vm.forecastBustDay(cat);
                    if (bustDay != null) {
                        statusText = "📈 On pace to hit limit by " + bustDay;
                        tvStatus.setTextColor(Color.parseColor("#4285F4"));
                    }
            }
            bar.getProgressDrawable().setColorFilter(barColor, android.graphics.PorterDuff.Mode.SRC_IN);
            tvStatus.setText(statusText);

            final int idx = i;
            row.setOnClickListener(v -> showEditBudgetDialog(cat, status.limit));

            llBudgets.addView(row);
        }
    }

    private void showEditBudgetDialog(Category cat, double currentLimit) {
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(currentLimit > 0 ? String.valueOf((int) currentLimit) : "");
        input.setHint("Enter amount in ₹");
        input.setPadding(40, 20, 40, 20);

        new AlertDialog.Builder(this)
            .setTitle(cat.emoji + "  " + cat.label + " — Weekly Limit")
            .setMessage("Set how much you want to spend on " + cat.label + " this week (₹)")
            .setView(input)
            .setPositiveButton("Save", (dialog, which) -> {
                String val = input.getText().toString().trim();
                if (val.isEmpty()) return;
                try {
                    double newLimit = Double.parseDouble(val);
                    vm.updateBudget(cat, newLimit);
                    playSound(R.raw.sound_chime);
                    
                    renderBudgets();
                    Toast.makeText(this,
                            cat.emoji + " " + cat.label + " limit set to ₹" + (int)newLimit,
                            Toast.LENGTH_SHORT).show();
                } catch (NumberFormatException ignored) {}
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}

package com.moneyminder.app;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.moneyminder.app.model.SavingsGoal;

import java.util.List;

public class SavingsJarsActivity extends AppCompatActivity {

    private static final int JAR_HEIGHT_DP = 150;

    private MoneyMinderViewModel vm;
    private LinearLayout llJars;
    private TextView tvJarsEmpty, tvRoundUpJar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_savings_jars);

        vm = new ViewModelProvider(this).get(MoneyMinderViewModel.class);

        llJars       = findViewById(R.id.llJars);
        tvJarsEmpty  = findViewById(R.id.tvJarsEmpty);
        tvRoundUpJar = findViewById(R.id.tvRoundUpJar);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnAddGoal).setOnClickListener(v -> addGoal());

        findViewById(R.id.btnAutoDistribute).setOnClickListener(v -> {
            double jar = vm.roundUpJar.getValue() != null ? vm.roundUpJar.getValue() : 0.0;
            if (jar <= 0) {
                Toast.makeText(this, "Your round-up jar is empty right now.", Toast.LENGTH_SHORT).show();
                return;
            }
            vm.autoDistributeRoundUps();
            Toast.makeText(this, "🎉 Distributed to your goals!", Toast.LENGTH_SHORT).show();
        });

        vm.roundUpJar.observe(this, jar ->
                tvRoundUpJar.setText(String.format("₹%.2f", jar)));

        vm.savingsGoals.observe(this, goals -> renderJars(goals));
    }

    private void addGoal() {
        EditText etName   = findViewById(R.id.etGoalName);
        EditText etTarget = findViewById(R.id.etGoalTarget);

        String name = etName.getText().toString().trim();
        String targetStr = etTarget.getText().toString().trim();
        if (name.isEmpty() || targetStr.isEmpty()) {
            Toast.makeText(this, "Give your goal a name and a target amount.", Toast.LENGTH_SHORT).show();
            return;
        }
        double target;
        try {
            target = Double.parseDouble(targetStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Target amount looks off — try a plain number.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (target <= 0) {
            Toast.makeText(this, "Target needs to be more than ₹0.", Toast.LENGTH_SHORT).show();
            return;
        }

        vm.addSavingsGoal(name, target);
        etName.setText("");
        etTarget.setText("");
        Toast.makeText(this, "🎯 \"" + name + "\" goal created!", Toast.LENGTH_SHORT).show();
    }

    private void renderJars(List<SavingsGoal> goals) {
        llJars.removeAllViews();
        boolean empty = (goals == null || goals.isEmpty());
        tvJarsEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) return;

        LayoutInflater inflater = LayoutInflater.from(this);
        for (SavingsGoal goal : goals) {
            View jarView = inflater.inflate(R.layout.item_savings_jar, llJars, false);

            FrameLayout jarContainer = jarView.findViewById(R.id.jarContainer);
            View fill               = jarView.findViewById(R.id.jarFill);
            TextView tvPct          = jarView.findViewById(R.id.tvJarPct);
            TextView tvName         = jarView.findViewById(R.id.tvGoalName);
            TextView tvAmounts      = jarView.findViewById(R.id.tvGoalAmounts);
            Button btnAdd           = jarView.findViewById(R.id.btnAddMoney);

            double progress = Math.min(1.0, goal.progress());
            int pct = (int) Math.round(progress * 100);
            boolean complete = goal.isComplete();

            fill.setBackgroundResource(complete ? R.drawable.bg_jar_fill_complete : R.drawable.bg_jar_fill);
            tvPct.setText(pct + "%");
            tvPct.setTextColor(android.graphics.Color.parseColor(progress > 0.55 ? "#FFFFFF" : "#14163A"));
            tvName.setText((complete ? "🏆 " : "") + goal.name);
            tvAmounts.setText(String.format("₹%.0f / ₹%.0f", goal.saved, goal.target));

            int targetHeightPx = (int) (dp(JAR_HEIGHT_DP) * progress);
            fill.post(() -> animateJarFill(fill, targetHeightPx));

            btnAdd.setOnClickListener(v -> showAddMoneyDialog(goal));

            llJars.addView(jarView);
        }
    }

    private void animateJarFill(View fill, int targetHeightPx) {
        ValueAnimator animator = ValueAnimator.ofInt(0, targetHeightPx);
        animator.setDuration(900);
        animator.setInterpolator(new OvershootInterpolator(0.6f));
        animator.addUpdateListener(anim -> {
            int value = (int) anim.getAnimatedValue();
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) fill.getLayoutParams();
            lp.height = Math.max(0, value);
            fill.setLayoutParams(lp);
        });
        animator.start();
    }

    private void showAddMoneyDialog(SavingsGoal goal) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Amount in ₹");
        input.setPadding(40, 20, 40, 20);

        new AlertDialog.Builder(this)
            .setTitle("Add to \"" + goal.name + "\"")
            .setView(input)
            .setPositiveButton("Add", (dialog, which) -> {
                String val = input.getText().toString().trim();
                if (val.isEmpty()) return;
                try {
                    double amount = Double.parseDouble(val);
                    if (amount <= 0) return;
                    vm.contributeToGoal(goal.id, amount);
                    Toast.makeText(this, "💰 Added ₹" + val + " to " + goal.name, Toast.LENGTH_SHORT).show();
                } catch (NumberFormatException ignored) {}
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}

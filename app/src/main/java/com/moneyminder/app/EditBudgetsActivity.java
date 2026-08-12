package com.moneyminder.app;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.moneyminder.app.model.Category;

import java.util.HashMap;
import java.util.Map;

public class EditBudgetsActivity extends AppCompatActivity {

    private MoneyMinderViewModel vm;
    private TextView tvRunningTotal;
    private final Map<Category, EditText> fields = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_budgets);

        vm = new ViewModelProvider(this).get(MoneyMinderViewModel.class);

        tvRunningTotal = findViewById(R.id.tvRunningTotal);
        LinearLayout llRows = findViewById(R.id.llEditRows);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        Map<Category, Double> current = vm.budgets.getValue();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Category cat : Category.values()) {
            View row = inflater.inflate(R.layout.item_edit_budget_row, llRows, false);
            TextView tvEmoji = row.findViewById(R.id.tvEmoji);
            TextView tvName  = row.findViewById(R.id.tvCatName);
            EditText etAmount = row.findViewById(R.id.etAmount);

            tvEmoji.setText(cat.emoji);
            tvName.setText(cat.label);
            double val = (current != null && current.containsKey(cat)) ? current.get(cat) : 0.0;
            etAmount.setText(val > 0 ? String.valueOf((int) val) : "");

            etAmount.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                public void onTextChanged(CharSequence s, int a, int b, int c) { updateRunningTotal(); }
                public void afterTextChanged(Editable s) {}
            });

            fields.put(cat, etAmount);
            llRows.addView(row);
        }

        findViewById(R.id.btnSave).setOnClickListener(v -> saveAll());

        updateRunningTotal();
    }

    private void updateRunningTotal() {
        double total = 0;
        for (EditText et : fields.values()) {
            String val = et.getText().toString().trim();
            if (!val.isEmpty()) {
                try { total += Double.parseDouble(val); } catch (NumberFormatException ignored) {}
            }
        }
        tvRunningTotal.setText(String.format("₹%.0f", total));
    }

    private void saveAll() {
        Map<Category, Double> newBudgets = new HashMap<>();
        for (Map.Entry<Category, EditText> e : fields.entrySet()) {
            String val = e.getValue().getText().toString().trim();
            double amount = 0;
            if (!val.isEmpty()) {
                try { amount = Double.parseDouble(val); } catch (NumberFormatException ignored) {}
            }
            newBudgets.put(e.getKey(), amount);
        }
        vm.updateAllBudgets(newBudgets);
        Toast.makeText(this, "✅ Budgets saved!", Toast.LENGTH_SHORT).show();
        finish();
    }
}

package com.moneyminder.app;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.moneyminder.app.model.Transaction;

import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.VH> {

    private List<Transaction> items;

    public TransactionAdapter(List<Transaction> items) { this.items = items; }

    public void update(List<Transaction> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Transaction t = items.get(position);
        holder.emoji.setText(t.category.emoji);
        holder.desc.setText(t.description);
        String time = DateFormat.format("EEE, h:mm a", t.timestamp).toString();
        holder.meta.setText(t.category.label + " · " + time);
        holder.amount.setText(String.format("₹%.2f", t.amount));
    }

    @Override
    public int getItemCount() { return items == null ? 0 : items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView emoji, desc, meta, amount;
        VH(View v) {
            super(v);
            emoji  = v.findViewById(R.id.tvCatEmoji);
            desc   = v.findViewById(R.id.tvTxnDesc);
            meta   = v.findViewById(R.id.tvTxnMeta);
            amount = v.findViewById(R.id.tvTxnAmount);
        }
    }
}

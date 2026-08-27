package com.kasirpintar.kaspintest.ui.history;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.kasirpintar.kaspintest.R;
import com.kasirpintar.kaspintest.data.local.entity.TransactionEntity;
import com.kasirpintar.kaspintest.databinding.ItemTransactionBinding;
import com.kasirpintar.kaspintest.util.Formats;

public class TransactionAdapter extends ListAdapter<TransactionEntity, TransactionAdapter.TxViewHolder> {

    public TransactionAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<TransactionEntity> DIFF_CALLBACK = new DiffUtil.ItemCallback<TransactionEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull TransactionEntity oldItem, @NonNull TransactionEntity newItem) {
            return oldItem.txId.equals(newItem.txId);
        }

        @Override
        public boolean areContentsTheSame(@NonNull TransactionEntity oldItem, @NonNull TransactionEntity newItem) {
            return oldItem.status == newItem.status;
        }
    };

    @NonNull
    @Override
    public TxViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTransactionBinding binding = ItemTransactionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new TxViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TxViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class TxViewHolder extends RecyclerView.ViewHolder {
        private final ItemTransactionBinding binding;

        TxViewHolder(ItemTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(TransactionEntity tx) {
            binding.textProductName.setText(tx.productName);
            binding.textDetail.setText(Formats.quantity(tx.qty) + " — " + Formats.rupiah(tx.totalPrice));
            binding.textTime.setText(Formats.dateTime(tx.createdAt));
            binding.textStatus.setText(tx.status.name());

            int colorRes;
            switch (tx.status) {
                case SYNCED:
                    colorRes = R.color.status_synced;
                    break;
                case FAILED:
                    colorRes = R.color.status_failed;
                    break;
                default:
                    colorRes = R.color.status_pending;
            }
            binding.textStatus.setBackgroundColor(ContextCompat.getColor(binding.getRoot().getContext(), colorRes));
        }
    }
}

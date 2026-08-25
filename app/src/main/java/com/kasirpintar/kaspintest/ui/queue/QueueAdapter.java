package com.kasirpintar.kaspintest.ui.queue;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.kasirpintar.kaspintest.R;
import com.kasirpintar.kaspintest.data.local.entity.OutboxEntity;
import com.kasirpintar.kaspintest.databinding.ItemOutboxBinding;

import java.util.Locale;

public class QueueAdapter extends ListAdapter<OutboxEntity, QueueAdapter.QueueViewHolder> {

    public interface OnRetryClickListener {
        void onRetryClick(OutboxEntity entry);
    }

    private final OnRetryClickListener listener;

    public QueueAdapter(OnRetryClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<OutboxEntity> DIFF_CALLBACK = new DiffUtil.ItemCallback<OutboxEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull OutboxEntity oldItem, @NonNull OutboxEntity newItem) {
            return oldItem.outboxId == newItem.outboxId;
        }

        @Override
        public boolean areContentsTheSame(@NonNull OutboxEntity oldItem, @NonNull OutboxEntity newItem) {
            return oldItem.status == newItem.status
                    && oldItem.retryCount == newItem.retryCount
                    && java.util.Objects.equals(oldItem.lastError, newItem.lastError);
        }
    };

    @NonNull
    @Override
    public QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOutboxBinding binding = ItemOutboxBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new QueueViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull QueueViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class QueueViewHolder extends RecyclerView.ViewHolder {
        private final ItemOutboxBinding binding;

        QueueViewHolder(ItemOutboxBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(OutboxEntity entry, OnRetryClickListener listener) {
            String shortId = entry.txId.length() > 8 ? entry.txId.substring(0, 8) + "…" : entry.txId;
            binding.textTxId.setText(shortId);
            binding.textStatus.setText(String.format(Locale.getDefault(),
                    "%s — Percobaan ke-%d", entry.status.name(), entry.retryCount));
            binding.textError.setText(entry.lastError);
            binding.textError.setVisibility(entry.lastError == null ? android.view.View.GONE : android.view.View.VISIBLE);
            binding.buttonRetry.setVisibility(
                    entry.status == com.kasirpintar.kaspintest.data.local.entity.TxStatus.FAILED
                            ? android.view.View.VISIBLE : android.view.View.GONE);
            binding.buttonRetry.setOnClickListener(v -> listener.onRetryClick(entry));
        }
    }
}

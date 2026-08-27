package com.kasirpintar.kaspintest.ui.product;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.kasirpintar.kaspintest.data.local.entity.ProductEntity;
import com.kasirpintar.kaspintest.databinding.ItemProductBinding;
import com.kasirpintar.kaspintest.util.Formats;

public class ProductAdapter extends ListAdapter<ProductEntity, ProductAdapter.ProductViewHolder> {

    public interface OnBuyClickListener {
        void onBuyClick(ProductEntity product);
    }

    private final OnBuyClickListener listener;

    public ProductAdapter(OnBuyClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<ProductEntity> DIFF_CALLBACK = new DiffUtil.ItemCallback<ProductEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull ProductEntity oldItem, @NonNull ProductEntity newItem) {
            return oldItem.productId.equals(newItem.productId);
        }

        @Override
        public boolean areContentsTheSame(@NonNull ProductEntity oldItem, @NonNull ProductEntity newItem) {
            return oldItem.stock == newItem.stock
                    && oldItem.price == newItem.price
                    && oldItem.name.equals(newItem.name);
        }
    };

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProductBinding binding = ItemProductBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ProductViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        private final ItemProductBinding binding;

        ProductViewHolder(ItemProductBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ProductEntity product, OnBuyClickListener listener) {
            binding.textName.setText(product.name);
            binding.textStock.setText(Formats.stock(product.stock));
            binding.textPrice.setText(Formats.rupiah(product.price));
            binding.buttonBuy.setEnabled(product.stock > 0);
            binding.buttonBuy.setOnClickListener(v -> listener.onBuyClick(product));
        }
    }
}

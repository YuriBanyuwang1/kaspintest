package com.kasirpintar.kaspintest.ui.product;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.kasirpintar.kaspintest.R;
import com.kasirpintar.kaspintest.data.local.entity.ProductEntity;
import com.kasirpintar.kaspintest.databinding.DialogCreateTransactionBinding;
import com.kasirpintar.kaspintest.databinding.FragmentProductBinding;

public class ProductFragment extends Fragment {

    private FragmentProductBinding binding;
    private ProductViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        binding = FragmentProductBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProductViewModel.class);

        ProductAdapter adapter = new ProductAdapter(this::showCreateTransactionDialog);
        binding.recyclerProducts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerProducts.setAdapter(adapter);

        viewModel.getProducts().observe(getViewLifecycleOwner(), adapter::submitList);

        viewModel.getMessage().observe(getViewLifecycleOwner(), event -> {
            String text = event.consume();
            if (text != null) {
                Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCreateTransactionDialog(ProductEntity product) {
        DialogCreateTransactionBinding dialogBinding =
                DialogCreateTransactionBinding.inflate(getLayoutInflater());
        dialogBinding.textProductName.setText(product.name);
        dialogBinding.editQty.setText("1");

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_title_create_tx)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.dialog_confirm, (dialog, which) -> {
                    int qty = parseQty(dialogBinding.editQty.getText().toString());
                    if (qty > 0) {
                        viewModel.createTransaction(product.productId, qty);
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private int parseQty(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

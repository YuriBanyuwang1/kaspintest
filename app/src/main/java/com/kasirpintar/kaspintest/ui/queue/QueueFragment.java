package com.kasirpintar.kaspintest.ui.queue;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.kasirpintar.kaspintest.databinding.FragmentQueueBinding;

public class QueueFragment extends Fragment {

    private FragmentQueueBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        binding = FragmentQueueBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        QueueViewModel viewModel = new ViewModelProvider(this).get(QueueViewModel.class);

        QueueAdapter adapter = new QueueAdapter(entry -> viewModel.retry(entry.txId));
        binding.recyclerQueue.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerQueue.setAdapter(adapter);

        binding.switchForceFail.setChecked(viewModel.isForceFailEnabled());
        binding.switchForceFail.setOnCheckedChangeListener((buttonView, isChecked) ->
                viewModel.setForceFail(isChecked));

        viewModel.getOutbox().observe(getViewLifecycleOwner(), entries -> {
            adapter.submitList(entries);
            binding.textEmpty.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

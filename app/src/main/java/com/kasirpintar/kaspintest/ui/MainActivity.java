package com.kasirpintar.kaspintest.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.kasirpintar.kaspintest.databinding.ActivityMainBinding;
import com.kasirpintar.kaspintest.ui.history.HistoryFragment;
import com.kasirpintar.kaspintest.ui.product.ProductFragment;
import com.kasirpintar.kaspintest.ui.queue.QueueFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState == null) {
            showFragment(new ProductFragment());
        }

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == com.kasirpintar.kaspintest.R.id.nav_product) {
                showFragment(new ProductFragment());
                return true;
            } else if (id == com.kasirpintar.kaspintest.R.id.nav_history) {
                showFragment(new HistoryFragment());
                return true;
            } else if (id == com.kasirpintar.kaspintest.R.id.nav_queue) {
                showFragment(new QueueFragment());
                return true;
            }
            return false;
        });
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(com.kasirpintar.kaspintest.R.id.fragmentContainer, fragment)
                .commit();
    }
}

package com.lucpastc.sideandrstore;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Define a aba inicial ao abrir o app (ex: Browse ou News)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, new BrowseFragment())
                    .commit();
        }

        // Listener para alternar entre as telas
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.nav_news) {
                selectedFragment = new NewsFragment();
            } else if (itemId == R.id.nav_sources) {
                selectedFragment = new SourcesFragment();
            } else if (itemId == R.id.nav_browse) {
                selectedFragment = new BrowseFragment();
            } else if (itemId == R.id.nav_my_apps) {
                selectedFragment = new MyAppsFragment();
            } else if (itemId == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });
    }
}
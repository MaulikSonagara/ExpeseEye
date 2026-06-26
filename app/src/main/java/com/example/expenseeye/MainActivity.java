package com.example.expenseeye;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load and apply theme preference
        com.example.expenseeye.theme.ThemeManager.applyTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Run reminder checker engine
        com.example.expenseeye.utils.ReminderEngine.checkAndProcessReminders(this);

        ViewPager2 viewPager = findViewById(R.id.view_pager);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        MainPagerAdapter pagerAdapter = new MainPagerAdapter(this);
        if (viewPager != null) {
            viewPager.setAdapter(pagerAdapter);

            // Sync BottomNavigationView selection with ViewPager2 page changes
            viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    int itemId;
                    switch (position) {
                        case 0:
                            itemId = R.id.dashboardFragment;
                            break;
                        case 1:
                            itemId = R.id.expensesFragment;
                            break;
                        case 2:
                            itemId = R.id.checklistFragment;
                            break;
                        case 3:
                            itemId = R.id.reportsFragment;
                            break;
                        case 4:
                            itemId = R.id.settingsFragment;
                            break;
                        default:
                            itemId = R.id.dashboardFragment;
                    }
                    if (bottomNavigationView != null && bottomNavigationView.getSelectedItemId() != itemId) {
                        bottomNavigationView.setSelectedItemId(itemId);
                    }
                }
            });
        }

        // Sync ViewPager2 page selection with BottomNavigationView clicks
        if (bottomNavigationView != null && viewPager != null) {
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.dashboardFragment) {
                    viewPager.setCurrentItem(0, true);
                    return true;
                } else if (itemId == R.id.expensesFragment) {
                    viewPager.setCurrentItem(1, true);
                    return true;
                } else if (itemId == R.id.checklistFragment) {
                    viewPager.setCurrentItem(2, true);
                    return true;
                } else if (itemId == R.id.reportsFragment) {
                    viewPager.setCurrentItem(3, true);
                    return true;
                } else if (itemId == R.id.settingsFragment) {
                    viewPager.setCurrentItem(4, true);
                    return true;
                }
                return false;
            });
        }

        if (viewPager != null) {
            handleIntentNavigation(viewPager);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        ViewPager2 viewPager = findViewById(R.id.view_pager);
        if (viewPager != null) {
            handleIntentNavigation(viewPager);
        }
    }

    private void handleIntentNavigation(ViewPager2 viewPager) {
        if (getIntent() != null && getIntent().hasExtra("navigate_to")) {
            int destId = getIntent().getIntExtra("navigate_to", R.id.dashboardFragment);
            try {
                int index = 0;
                if (destId == R.id.expensesFragment) {
                    index = 1;
                } else if (destId == R.id.checklistFragment) {
                    index = 2;
                } else if (destId == R.id.reportsFragment) {
                    index = 3;
                } else if (destId == R.id.settingsFragment) {
                    index = 4;
                }
                viewPager.setCurrentItem(index, false);
                BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
                if (bottomNavigationView != null) {
                    bottomNavigationView.setSelectedItemId(destId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class MainPagerAdapter extends FragmentStateAdapter {
        public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new com.example.expenseeye.fragments.DashboardFragment();
                case 1:
                    return new com.example.expenseeye.fragments.ExpensesFragment();
                case 2:
                    return new com.example.expenseeye.fragments.ChecklistFragment();
                case 3:
                    return new com.example.expenseeye.fragments.ReportsFragment();
                case 4:
                    return new com.example.expenseeye.fragments.SettingsFragment();
                default:
                    return new com.example.expenseeye.fragments.DashboardFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 5;
        }
    }
}
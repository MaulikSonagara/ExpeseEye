package com.example.expenseeye;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.fragment.app.Fragment;
import com.example.expenseeye.utils.AlarmScheduler;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private final java.util.List<Integer> navigationHistory = new java.util.ArrayList<>();
    private boolean isBackNavigation = false;
    private androidx.activity.OnBackPressedCallback backPressedCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load and apply theme preference
        com.example.expenseeye.theme.ThemeManager.applyTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Notification Channels
        com.example.expenseeye.utils.NotificationHelper.initNotificationChannels(this);

        // Handle Android 13+ Notification Permission
        requestNotificationPermission();

        // Handle Android 12+ Exact Alarm Permission
        checkExactAlarmPermission();

        // Handle Battery Optimization
        checkBatteryOptimization();

        // Run reminder checker engine
        com.example.expenseeye.utils.ReminderEngine.checkAndProcessReminders(this);

        ViewPager2 viewPager = findViewById(R.id.view_pager);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        if (viewPager != null) {
            backPressedCallback = new androidx.activity.OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    if (navigationHistory.size() > 1) {
                        navigationHistory.remove(navigationHistory.size() - 1);
                        int previousPage = navigationHistory.get(navigationHistory.size() - 1);
                        isBackNavigation = true;
                        viewPager.setCurrentItem(previousPage, true);
                        isBackNavigation = false;
                    } else {
                        setEnabled(false);
                        getOnBackPressedDispatcher().onBackPressed();
                    }
                }
            };
            getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
        }

        com.google.android.material.card.MaterialCardView cardBottomNav = findViewById(R.id.card_bottom_nav);
        if (cardBottomNav != null) {
            int surfaceColor = com.example.expenseeye.theme.ThemeManager.getColor(this, com.example.expenseeye.theme.ThemeManager.ThemeColor.SURFACE);
            // Apply 85% opacity (0xD9) to the current theme's surface color to create a premium glass look
            int translucentColor = (surfaceColor & 0x00FFFFFF) | (0xD9 << 24);
            cardBottomNav.setCardBackgroundColor(translucentColor);
        }

        MainPagerAdapter pagerAdapter = new MainPagerAdapter(this);
        if (viewPager != null) {
            viewPager.setAdapter(pagerAdapter);

            // Sync BottomNavigationView selection with ViewPager2 page changes
            viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);

                    if (!isBackNavigation) {
                        if (navigationHistory.isEmpty() || navigationHistory.get(navigationHistory.size() - 1) != position) {
                            navigationHistory.add(position);
                        }
                    }
                    if (backPressedCallback != null) {
                        backPressedCallback.setEnabled(true);
                    }

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
                    updateBottomNavIconSizes(bottomNavigationView, itemId);
                }
            });
        }

        // Sync ViewPager2 page selection with BottomNavigationView clicks
        if (bottomNavigationView != null && viewPager != null) {
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                updateBottomNavIconSizes(bottomNavigationView, itemId);
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

        if (bottomNavigationView != null) {
            bottomNavigationView.post(() -> updateBottomNavIconSizes(bottomNavigationView, bottomNavigationView.getSelectedItemId()));
        }

        if (viewPager != null) {
            handleIntentNavigation(viewPager);
        }
    }

    private void updateBottomNavIconSizes(BottomNavigationView bottomNavigationView, int selectedItemId) {
        if (bottomNavigationView == null) return;
        int selectedSize = (int) (20 * getResources().getDisplayMetrics().density);
        int unselectedSize = (int) (16 * getResources().getDisplayMetrics().density);

        android.view.Menu menu = bottomNavigationView.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            android.view.MenuItem item = menu.getItem(i);
            android.view.View itemView = bottomNavigationView.findViewById(item.getItemId());
            if (itemView != null) {
                android.widget.ImageView iconView = findImageView(itemView);
                if (iconView != null) {
                    android.view.ViewGroup.LayoutParams lp = iconView.getLayoutParams();
                    if (item.getItemId() == selectedItemId) {
                        lp.width = selectedSize;
                        lp.height = selectedSize;
                    } else {
                        lp.width = unselectedSize;
                        lp.height = unselectedSize;
                    }
                    iconView.setLayoutParams(lp);
                }
            }
        }
    }

    private android.widget.ImageView findImageView(android.view.View view) {
        if (view instanceof android.widget.ImageView) {
            return (android.widget.ImageView) view;
        }
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup vg = (android.view.ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                android.widget.ImageView iv = findImageView(vg.getChildAt(i));
                if (iv != null) return iv;
            }
        }
        return null;
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission granted
                } else {
                    Toast.makeText(this, "Notifications are disabled. You might miss important reminders.", Toast.LENGTH_LONG).show();
                }
            });

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!AlarmScheduler.canScheduleExactAlarms(this)) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Permission Required")
                        .setMessage("To receive reminders exactly on time, the app needs permission to schedule exact alarms. Please enable it in Settings.")
                        .setPositiveButton("Settings", (dialog, which) -> {
                            AlarmScheduler.requestExactAlarmPermission(this);
                        })
                        .setNegativeButton("Maybe Later", null)
                        .show();
            }
        }
    }

    private void checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Optimization Detected")
                        .setMessage("To ensure reminders work reliably in the background, please disable battery optimization for ExpenseEye.")
                        .setPositiveButton("Fix Now", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                            startActivity(intent);
                        })
                        .setNegativeButton("Skip", null)
                        .show();
            }
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
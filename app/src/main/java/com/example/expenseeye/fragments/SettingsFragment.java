package com.example.expenseeye.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.expenseeye.R;
import com.example.expenseeye.adapters.ThemeSelectionAdapter;
import com.example.expenseeye.theme.ThemePreferenceHelper;
import com.example.expenseeye.theme.ThemeManager;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.util.Arrays;
import java.util.List;

public class SettingsFragment extends Fragment {

    private ThemePreferenceHelper themeHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        themeHelper = new ThemePreferenceHelper(requireContext());

        setupThemeSelection(view);
        setupModeSwitch(view);
        setupActionButtons(view);
    }

    private void setupThemeSelection(View view) {
        RecyclerView rvThemes = view.findViewById(R.id.rv_themes);
        List<String> themes = Arrays.asList(
                ThemePreferenceHelper.THEME_MIDNIGHT,
                ThemePreferenceHelper.THEME_FOREST,
                ThemePreferenceHelper.THEME_SAND,
                ThemePreferenceHelper.THEME_OCEAN
        );

        ThemeSelectionAdapter adapter = new ThemeSelectionAdapter(themes, themeHelper.getTheme(), themeName -> {
            themeHelper.setTheme(themeName);
            requireActivity().recreate();
        });

        rvThemes.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvThemes.setAdapter(adapter);
    }

    private void setupModeSwitch(View view) {
        MaterialSwitch switchDark = view.findViewById(R.id.switch_dark_mode);
        switchDark.setChecked(themeHelper.isDarkMode());

        switchDark.setOnCheckedChangeListener((buttonView, isChecked) -> {
            themeHelper.setMode(isChecked ? ThemePreferenceHelper.MODE_DARK : ThemePreferenceHelper.MODE_LIGHT);
            requireActivity().recreate();
        });
    }

    private void setupActionButtons(View view) {
        view.findViewById(R.id.btn_export_pdf).setOnClickListener(v -> Toast.makeText(getContext(), "Exporting PDF...", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.btn_export_csv).setOnClickListener(v -> Toast.makeText(getContext(), "Exporting CSV...", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.btn_backup).setOnClickListener(v -> Toast.makeText(getContext(), "Creating Backup...", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.btn_restore).setOnClickListener(v -> Toast.makeText(getContext(), "Restoring...", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.btn_configure_widget_shortcuts).setOnClickListener(v -> Toast.makeText(getContext(), "Opening Widget Config...", Toast.LENGTH_SHORT).show());
    }
}

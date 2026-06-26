package com.example.expenseeye;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.expenseeye.theme.ThemePreferenceHelper;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SmartFeaturesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.example.expenseeye.theme.ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_features);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        ThemePreferenceHelper prefHelper = new ThemePreferenceHelper(this);

        MaterialSwitch switchExpenseSuggestions = findViewById(R.id.switch_expense_title_suggestions);
        MaterialSwitch switchExpenseClassifier = findViewById(R.id.switch_expense_smart_classifier);
        MaterialSwitch switchChecklistSuggestions = findViewById(R.id.switch_checklist_title_suggestions);
        MaterialSwitch switchChecklistClassifier = findViewById(R.id.switch_checklist_smart_classifier);

        // Bind initial values
        switchExpenseSuggestions.setChecked(prefHelper.isTitleSuggestionsEnabled());
        switchExpenseClassifier.setChecked(prefHelper.isSmartClassifierEnabled());
        switchChecklistSuggestions.setChecked(prefHelper.isChecklistTitleSuggestionsEnabled());
        switchChecklistClassifier.setChecked(prefHelper.isChecklistSmartClassifierEnabled());

        // Listeners to update preferences
        switchExpenseSuggestions.setOnCheckedChangeListener((buttonView, isChecked) -> 
            prefHelper.setTitleSuggestionsEnabled(isChecked));
        
        switchExpenseClassifier.setOnCheckedChangeListener((buttonView, isChecked) -> 
            prefHelper.setSmartClassifierEnabled(isChecked));
            
        switchChecklistSuggestions.setOnCheckedChangeListener((buttonView, isChecked) -> 
            prefHelper.setChecklistTitleSuggestionsEnabled(isChecked));
            
        switchChecklistClassifier.setOnCheckedChangeListener((buttonView, isChecked) -> 
            prefHelper.setChecklistSmartClassifierEnabled(isChecked));
    }
}

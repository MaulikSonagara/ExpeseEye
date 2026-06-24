package com.example.expenseeye;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.PathInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load theme preference early
        com.example.expenseeye.theme.ThemeManager.applyTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView imgLogo = findViewById(R.id.img_splash_logo);
        TextView tvTitle = findViewById(R.id.tv_splash_title);
        TextView tvSubtitle = findViewById(R.id.tv_splash_subtitle);
        ProgressBar progressSplash = findViewById(R.id.progress_splash);
        TextView tvStatus = findViewById(R.id.tv_splash_status);

        // Set initial states to avoid flashing
        imgLogo.setAlpha(0f);
        imgLogo.setScaleX(0.82f);
        imgLogo.setScaleY(0.82f);

        tvTitle.setAlpha(0f);
        tvTitle.setTranslationY(24f);

        tvSubtitle.setAlpha(0f);
        tvSubtitle.setTranslationY(24f);

        progressSplash.setAlpha(0f);
        progressSplash.setProgress(8);

        tvStatus.setAlpha(0f);

        // Setup Animators
        PathInterpolator logoInterpolator = new PathInterpolator(0.16f, 1f, 0.3f, 1f);

        ObjectAnimator logoAlpha = ObjectAnimator.ofFloat(imgLogo, "alpha", 0f, 1f);
        logoAlpha.setDuration(500);
        logoAlpha.setStartDelay(50);
        logoAlpha.setInterpolator(logoInterpolator);

        ObjectAnimator logoScaleX = ObjectAnimator.ofFloat(imgLogo, "scaleX", 0.82f, 1f);
        logoScaleX.setDuration(500);
        logoScaleX.setStartDelay(50);
        logoScaleX.setInterpolator(logoInterpolator);

        ObjectAnimator logoScaleY = ObjectAnimator.ofFloat(imgLogo, "scaleY", 0.82f, 1f);
        logoScaleY.setDuration(500);
        logoScaleY.setStartDelay(50);
        logoScaleY.setInterpolator(logoInterpolator);

        ObjectAnimator titleAlpha = ObjectAnimator.ofFloat(tvTitle, "alpha", 0f, 1f);
        titleAlpha.setDuration(400);
        titleAlpha.setStartDelay(200);

        ObjectAnimator titleTranslateY = ObjectAnimator.ofFloat(tvTitle, "translationY", 24f, 0f);
        titleTranslateY.setDuration(400);
        titleTranslateY.setStartDelay(200);

        ObjectAnimator subtitleAlpha = ObjectAnimator.ofFloat(tvSubtitle, "alpha", 0f, 1f);
        subtitleAlpha.setDuration(400);
        subtitleAlpha.setStartDelay(300);

        ObjectAnimator subtitleTranslateY = ObjectAnimator.ofFloat(tvSubtitle, "translationY", 24f, 0f);
        subtitleTranslateY.setDuration(400);
        subtitleTranslateY.setStartDelay(300);

        ObjectAnimator progressAlpha = ObjectAnimator.ofFloat(progressSplash, "alpha", 0f, 1f);
        progressAlpha.setDuration(300);
        progressAlpha.setStartDelay(400);

        ObjectAnimator statusAlpha = ObjectAnimator.ofFloat(tvStatus, "alpha", 0f, 1f);
        statusAlpha.setDuration(300);
        statusAlpha.setStartDelay(400);

        ValueAnimator progressValueAnimator = ValueAnimator.ofInt(8, 100);
        progressValueAnimator.setDuration(500);
        progressValueAnimator.setStartDelay(400);
        progressValueAnimator.addUpdateListener(animation -> {
            int val = (int) animation.getAnimatedValue();
            progressSplash.setProgress(val);
        });

        // Combine into AnimatorSet
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(
                logoAlpha, logoScaleX, logoScaleY,
                titleAlpha, titleTranslateY,
                subtitleAlpha, subtitleTranslateY,
                progressAlpha, statusAlpha,
                progressValueAnimator
        );

        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        animatorSet.start();
    }
}

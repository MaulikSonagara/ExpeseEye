package com.example.expenseeye.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");
        int id = intent.getIntExtra("id", 100);
        
        if (title == null) title = "Reminder";
        if (message == null) message = "Time to log your daily expenses or check your checklist!";
        
        NotificationHelper.showNotification(context, id, title, message);
    }
}

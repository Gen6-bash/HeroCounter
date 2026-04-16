package com.herocounter.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fires when a task reminder alarm goes off.
 * Posts a notification and re-schedules for the next matching day.
 */
public class ReminderReceiver extends BroadcastReceiver {

    static final String CHANNEL_ID     = "hero_counter_reminders";
    static final String EXTRA_TASK_ID  = "task_id";
    static final String EXTRA_TASK_NAME= "task_name";

    @Override
    public void onReceive(Context context, Intent intent) {
        int taskId    = intent.getIntExtra(EXTRA_TASK_ID, -1);
        String name   = intent.getStringExtra(EXTRA_TASK_NAME);
        if (taskId < 0 || name == null) return;

        // Post notification
        postNotification(context, taskId, name);

        // Re-schedule for next occurrence on a background thread
        ExecutorService ex = Executors.newSingleThreadExecutor();
        ex.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            Task task = db.taskDao().getTaskById(taskId);
            if (task != null && task.reminderEnabled) {
                ReminderManager.scheduleReminder(context, task);
            }
            ex.shutdown();
        });
    }

    private void postNotification(Context context, int taskId, String taskName) {
        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create channel (safe to call repeatedly — no-op if exists)
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Hero Counter Reminders",
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Daily reminders to log your count progress");
        nm.createNotificationChannel(channel);

        // Tap intent — opens MainActivity with the task pre-selected
        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.putExtra(EXTRA_TASK_ID, taskId);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(context, taskId, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("Hero Counter")
                .setContentText("Would you like to record your " + taskName + " progress?")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Would you like to record your " + taskName + " progress?"))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pi)
                .setAutoCancel(true);

        nm.notify(taskId, builder.build());
    }
}

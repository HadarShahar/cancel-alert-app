package com.myapp.cancelalert;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import static com.myapp.cancelalert.App.CHANNEL_ID;

public class MyService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Map<String, String> data = remoteMessage.getData();

        String title = data.get("title");
        String body = data.get("body");
        String everyone = "";
        if (data.containsKey("everyone")) {
            everyone = data.get("everyone");
        }

        if (everyone.equals("true")) {
            sendNotification(title, body);
        } else {
            String[] selectedLessons = LessonsSelection.getSelectedLessons(this);
            for (String lesson : selectedLessons) {
                if (!lesson.equals("") && title.contains(lesson)) {
                    sendNotification(title, body);
                    break;
                }
            }
        }
    }


    public void sendNotification(String title, String msg) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(),
                        R.drawable.notification_icon))

                .setContentTitle(title)
                .setContentText(msg)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        int oneTimeID = (int) SystemClock.uptimeMillis();
        notificationManager.notify(oneTimeID, builder.build());
    }
    
}

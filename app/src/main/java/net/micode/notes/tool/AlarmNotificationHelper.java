/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.micode.notes.tool;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.ui.AlarmAlertActivity;

/**
 * 便签提醒通知工具。负责创建高优先级通知渠道，并通过通知 + 全屏 Intent
 * 在 Android 10+ 后台启动限制下打开提醒界面。
 */
public final class AlarmNotificationHelper {

    public static final String CHANNEL_ID = "note_reminder";

    private AlarmNotificationHelper() {
    }

    /**
     * 生成便签对应的通知 ID。
     */
    public static int getNotificationId(long noteId) {
        return (int) (noteId & 0x7fffffffL);
    }

    /**
     * 显示便签提醒通知，并请求系统以全屏 Intent 打开提醒界面。
     */
    public static void showReminder(Context context, long noteId, String snippet) {
        if (noteId <= 0) {
            return;
        }

        NotificationManager manager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel(context, manager);
        }

        Intent intent = new Intent(context, AlarmAlertActivity.class);
        intent.setData(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int notificationId = getNotificationId(noteId);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String content = TextUtils.isEmpty(snippet)
                ? context.getString(R.string.reminder_notification_text) : snippet;

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
        }

        Notification notification = builder
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(context.getString(R.string.reminder_notification_title))
                .setContentText(content)
                .setStyle(new Notification.BigTextStyle().bigText(content))
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(pendingIntent, true)
                .setCategory(Notification.CATEGORY_ALARM)
                .setPriority(Notification.PRIORITY_MAX)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .build();

        manager.notify(notificationId, notification);
    }

    /**
     * 创建提醒通知渠道。声音由 AlarmAlertActivity 统一播放，避免重复响铃。
     */
    private static void createChannel(Context context, NotificationManager manager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(context.getString(R.string.reminder_channel_description));
        channel.setSound(null, null);
        channel.enableVibration(true);
        manager.createNotificationChannel(channel);
    }

    /**
     * 取消指定便签的提醒通知。
     */
    public static void cancel(Context context, long noteId) {
        NotificationManager manager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null && noteId > 0) {
            manager.cancel(getNotificationId(noteId));
        }
    }
}

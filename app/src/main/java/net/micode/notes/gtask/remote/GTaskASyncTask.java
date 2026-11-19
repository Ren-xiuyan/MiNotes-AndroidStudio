
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

package net.micode.notes.gtask.remote;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;

import net.micode.notes.R;
import net.micode.notes.ui.NotesListActivity;
import net.micode.notes.ui.NotesPreferenceActivity;

/**
 * Google Tasks 异步同步任务。在后台执行同步，并通过通知和回调向界面反馈进度与结果。
 */
public class GTaskASyncTask extends AsyncTask<Void, String, Integer> {

    private static int GTASK_SYNC_NOTIFICATION_ID = 5234235;

    private static final String GTASK_SYNC_CHANNEL_ID = "gtask_sync";

    /**
     * 同步完成监听器。同步任务结束后回调调用方。
     */
    public interface OnCompleteListener {
        /**
         * 任务完成回调。
         */
        void onComplete();
    }

    private Context mContext;

    private NotificationManager mNotifiManager;

    private GTaskManager mTaskManager;

    private OnCompleteListener mOnCompleteListener;

    /**
     * 构造 GTaskASyncTask 对象，并初始化内部状态。
     */
    public GTaskASyncTask(Context context, OnCompleteListener listener) {
        mContext = context;
        mOnCompleteListener = listener;
        mNotifiManager = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mTaskManager = GTaskManager.getInstance();
    }

    /**
     * 取消同步。
     */
    public void cancelSync() {
        mTaskManager.cancelSync();
    }

    /**
     * 发布当前同步进度。
     */
    public void publishProgess(String message) {
        publishProgress(new String[] {
            message
        });
    }

    /**
     * 创建或更新同步进度通知，并绑定点击跳转。
     */
    private void showNotification(int tickerId, String content) {
        Intent intent;
        if (tickerId != R.string.ticker_success) {
            intent = new Intent(mContext, NotesPreferenceActivity.class);
        } else {
            intent = new Intent(mContext, NotesListActivity.class);
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(GTASK_SYNC_CHANNEL_ID,
                    mContext.getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW);
            mNotifiManager.createNotificationChannel(channel);
            builder = new Notification.Builder(mContext, GTASK_SYNC_CHANNEL_ID);
        } else {
            builder = new Notification.Builder(mContext);
        }

        Notification notification = builder
                .setSmallIcon(R.drawable.notification)
                .setTicker(mContext.getString(tickerId))
                .setWhen(System.currentTimeMillis())
                .setContentTitle(mContext.getString(R.string.app_name))
                .setContentText(content)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .build();
        mNotifiManager.notify(GTASK_SYNC_NOTIFICATION_ID, notification);
    }

    /**
     * 在后台线程执行 Google Tasks 同步。
     */
    @Override
    protected Integer doInBackground(Void... unused) {
        publishProgess(mContext.getString(R.string.sync_progress_login, NotesPreferenceActivity
                .getSyncAccountName(mContext)));
        return mTaskManager.sync(mContext, this);
    }

    /**
     * 把后台同步进度更新到通知栏。
     */
    @Override
    protected void onProgressUpdate(String... progress) {
        showNotification(R.string.ticker_syncing, progress[0]);
        if (mContext instanceof GTaskSyncService) {
            ((GTaskSyncService) mContext).sendBroadcast(progress[0]);
        }
    }

    /**
     * 根据同步结果更新通知，并触发完成回调。
     */
    @Override
    protected void onPostExecute(Integer result) {
        if (result == GTaskManager.STATE_SUCCESS) {
            showNotification(R.string.ticker_success, mContext.getString(
                    R.string.success_sync_account, mTaskManager.getSyncAccount()));
            NotesPreferenceActivity.setLastSyncTime(mContext, System.currentTimeMillis());
        } else if (result == GTaskManager.STATE_NETWORK_ERROR) {
            showNotification(R.string.ticker_fail, mContext.getString(R.string.error_sync_network));
        } else if (result == GTaskManager.STATE_INTERNAL_ERROR) {
            showNotification(R.string.ticker_fail, mContext.getString(R.string.error_sync_internal));
        } else if (result == GTaskManager.STATE_SYNC_CANCELLED) {
            showNotification(R.string.ticker_cancel, mContext
                    .getString(R.string.error_sync_cancelled));
        }
        if (mOnCompleteListener != null) {
            new Thread(new Runnable() {

                /**
                 * 线程任务：执行 Runnable 中的逻辑。
                 */
                public void run() {
                    mOnCompleteListener.onComplete();
                }
            }).start();
        }
    }
}

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

package net.micode.notes.ui;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import net.micode.notes.tool.AlarmNotificationHelper;
import net.micode.notes.tool.DataUtils;

/**
 * 便签提醒广播接收器。收到闹钟广播后显示高优先级通知，并通过全屏 Intent 打开提醒界面。
 */
public class AlarmReceiver extends BroadcastReceiver {

    /**
     * 收到闹钟广播后读取便签摘要并显示提醒通知。
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Uri data = intent.getData();
        if (data == null) {
            return;
        }

        long noteId;
        try {
            noteId = ContentUris.parseId(data);
        } catch (RuntimeException e) {
            return;
        }
        if (noteId <= 0) {
            return;
        }

        String snippet = DataUtils.getSnippetById(context.getContentResolver(), noteId);
        AlarmNotificationHelper.showReminder(context, noteId, snippet);
    }
}

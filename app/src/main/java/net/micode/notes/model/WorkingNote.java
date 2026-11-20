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

package net.micode.notes.model;

import android.appwidget.AppWidgetManager;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.CallNote;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.DataConstants;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.data.Notes.TextNote;
import net.micode.notes.tool.ResourceParser.NoteBgResources;

/**
 * 便签编辑工作对象。负责加载、保存、删除便签，并在编辑过程中维护界面所需状态。
 */
public class WorkingNote {

    private Note mNote;

    private long mNoteId;

    private String mContent;

    private int mMode;

    private long mAlertDate;

    private long mModifiedDate;

    private int mBgColorId;

    private int mWidgetId;

    private int mWidgetType;

    private long mFolderId;

    private boolean mPinned;

    private String mTags;

    private Context mContext;

    private static final String TAG = "WorkingNote";

    private boolean mIsDeleted;

    private NoteSettingChangedListener mNoteSettingStatusListener;

    public static final String[] DATA_PROJECTION = new String[] {
            DataColumns.ID,
            DataColumns.CONTENT,
            DataColumns.MIME_TYPE,
            DataColumns.DATA1,
            DataColumns.DATA2,
            DataColumns.DATA3,
            DataColumns.DATA4,
    };

    public static final String[] NOTE_PROJECTION = new String[] {
            NoteColumns.PARENT_ID,
            NoteColumns.ALERTED_DATE,
            NoteColumns.BG_COLOR_ID,
            NoteColumns.WIDGET_ID,
            NoteColumns.WIDGET_TYPE,
            NoteColumns.MODIFIED_DATE,
            NoteColumns.PINNED,
            NoteColumns.TAGS
    };

    private static final int DATA_ID_COLUMN = 0;

    private static final int DATA_CONTENT_COLUMN = 1;

    private static final int DATA_MIME_TYPE_COLUMN = 2;

    private static final int DATA_MODE_COLUMN = 3;

    private static final int NOTE_PARENT_ID_COLUMN = 0;

    private static final int NOTE_ALERTED_DATE_COLUMN = 1;

    private static final int NOTE_BG_COLOR_ID_COLUMN = 2;

    private static final int NOTE_WIDGET_ID_COLUMN = 3;

    private static final int NOTE_WIDGET_TYPE_COLUMN = 4;

    private static final int NOTE_MODIFIED_DATE_COLUMN = 5;

    private static final int NOTE_PINNED_COLUMN = 6;

    private static final int NOTE_TAGS_COLUMN = 7;

    /**
     * 构造 WorkingNote 对象，并初始化内部状态。
     */
    private WorkingNote(Context context, long folderId) {
        mContext = context;
        mAlertDate = 0;
        mModifiedDate = System.currentTimeMillis();
        mFolderId = folderId;
        mNote = new Note();
        mNoteId = 0;
        mIsDeleted = false;
        mMode = 0;
        mWidgetType = Notes.TYPE_WIDGET_INVALIDE;
        mWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        mContent = "";
        mTags = "";
        mPinned = false;
    }

    /**
     * 构造 WorkingNote 对象，并初始化内部状态。
     */
    private WorkingNote(Context context, long noteId, long folderId) {
        mContext = context;
        mNoteId = noteId;
        mFolderId = folderId;
        mIsDeleted = false;
        mNote = new Note();
        loadNote();
    }

    /**
     * 读取便签主表字段，恢复便签的基本信息。
     */
    private void loadNote() {
        Cursor cursor = mContext.getContentResolver().query(
                ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, mNoteId), NOTE_PROJECTION, null,
                null, null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    mFolderId = cursor.getLong(NOTE_PARENT_ID_COLUMN);
                    mBgColorId = cursor.getInt(NOTE_BG_COLOR_ID_COLUMN);
                    mWidgetId = cursor.getInt(NOTE_WIDGET_ID_COLUMN);
                    mWidgetType = cursor.getInt(NOTE_WIDGET_TYPE_COLUMN);
                    mAlertDate = cursor.getLong(NOTE_ALERTED_DATE_COLUMN);
                    mModifiedDate = cursor.getLong(NOTE_MODIFIED_DATE_COLUMN);
                    mPinned = cursor.getInt(NOTE_PINNED_COLUMN) > 0;
                    String tags = cursor.getString(NOTE_TAGS_COLUMN);
                    mTags = tags == null ? "" : tags;
                } else {
                    throw new IllegalArgumentException("Unable to find note with id " + mNoteId);
                }
            } finally {
                cursor.close();
            }
        } else {
            Log.e(TAG, "No note with id:" + mNoteId);
            throw new IllegalArgumentException("Unable to find note with id " + mNoteId);
        }
        loadNoteData();
    }

    /**
     * 读取便签数据表，恢复文本内容或通话信息。
     */
    private void loadNoteData() {
        Cursor cursor = mContext.getContentResolver().query(Notes.CONTENT_DATA_URI, DATA_PROJECTION,
                DataColumns.NOTE_ID + "=?", new String[] {
                    String.valueOf(mNoteId)
                }, null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        String type = cursor.getString(DATA_MIME_TYPE_COLUMN);
                        if (DataConstants.NOTE.equals(type)) {
                            String content = cursor.getString(DATA_CONTENT_COLUMN);
                            mContent = content == null ? "" : content;
                            mMode = cursor.getInt(DATA_MODE_COLUMN);
                            mNote.setTextDataId(cursor.getLong(DATA_ID_COLUMN));
                        } else if (DataConstants.CALL_NOTE.equals(type)) {
                            mNote.setCallDataId(cursor.getLong(DATA_ID_COLUMN));
                        } else {
                            Log.d(TAG, "Wrong note type with type:" + type);
                        }
                    } while (cursor.moveToNext());
                }
            } finally {
                cursor.close();
            }
        } else {
            // 新便签或数据行被并发删除时不应崩溃，保持空内容即可。
            Log.w(TAG, "No data with id:" + mNoteId);
        }
    }

    /**
     * 创建一个空便签对象，用于新建便签或插入介绍内容。
     */
    public static WorkingNote createEmptyNote(Context context, long folderId, int widgetId,
            int widgetType, int defaultBgColorId) {
        WorkingNote note = new WorkingNote(context, folderId);
        note.setBgColorId(defaultBgColorId);
        note.setWidgetId(widgetId);
        note.setWidgetType(widgetType);
        return note;
    }

    /**
     * 从数据库加载指定便签，并初始化编辑所需的数据和状态。
     */
    public static WorkingNote load(Context context, long id) {
        try {
            return new WorkingNote(context, id, 0);
        } catch (IllegalArgumentException e) {
            // 便签可能在进入编辑页前被删除，返回 null 由调用方按“便签不存在”处理。
            Log.e(TAG, "Load note failed with id:" + id, e);
            return null;
        }
    }

    /**
     * 保存便签主表和数据表内容，并更新界面状态。
     */
    public synchronized boolean saveNote() {
        // 只有内容有效或已有修改时才需要写库。
        if (isWorthSaving()) {
            // 新便签先申请数据库 ID，再保存内容。
            if (!existInDatabase()) {
                if ((mNoteId = Note.getNewNoteId(mContext, mFolderId)) == 0) {
                    Log.e(TAG, "Create new note fail with id:" + mNoteId);
                    return false;
                }
            }

            // 把便签主表和数据表的差异字段统一提交。
            mNote.syncNote(mContext, mNoteId);

            // 便签绑定了桌面小部件时，通知小部件刷新。
            if (mWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID
                    && mWidgetType != Notes.TYPE_WIDGET_INVALIDE
                    && mNoteSettingStatusListener != null) {
                mNoteSettingStatusListener.onWidgetChanged();
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * 判断当前便签是否已经保存到数据库。
     */
    public boolean existInDatabase() {
        return mNoteId > 0;
    }

    /**
     * 判断当前对象是否值得保存。
     */
    private boolean isWorthSaving() {
        if (mIsDeleted
                || (!existInDatabase() && TextUtils.isEmpty(mContent) && TextUtils.isEmpty(mTags))
                || (existInDatabase() && !mNote.isLocalModified())) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * 设置便签设置变化监听器。
     */
    public void setOnSettingStatusChangedListener(NoteSettingChangedListener l) {
        mNoteSettingStatusListener = l;
    }

    /**
     * 设置提醒日期。
     */
    public void setAlertDate(long date, boolean set) {
        if (date != mAlertDate) {
            mAlertDate = date;
            mNote.setNoteValue(NoteColumns.ALERTED_DATE, String.valueOf(mAlertDate));
        }
        if (mNoteSettingStatusListener != null) {
            mNoteSettingStatusListener.onClockAlertChanged(date, set);
        }
    }

    /**
     * 标记便签为已删除，并同步更新数据库记录。
     */
    public void markDeleted(boolean mark) {
        mIsDeleted = mark;
        if (mWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID
                && mWidgetType != Notes.TYPE_WIDGET_INVALIDE && mNoteSettingStatusListener != null) {
                mNoteSettingStatusListener.onWidgetChanged();
        }
    }

    /**
     * 设置背景颜色ID。
     */
    public void setBgColorId(int id) {
        if (id != mBgColorId) {
            mBgColorId = id;
            if (mNoteSettingStatusListener != null) {
                mNoteSettingStatusListener.onBackgroundColorChanged();
            }
            mNote.setNoteValue(NoteColumns.BG_COLOR_ID, String.valueOf(id));
        }
    }

    /**
     * 设置清单列表模式。
     */
    public void setCheckListMode(int mode) {
        if (mMode != mode) {
            if (mNoteSettingStatusListener != null) {
                mNoteSettingStatusListener.onCheckListModeChanged(mMode, mode);
            }
            mMode = mode;
            mNote.setTextData(TextNote.MODE, String.valueOf(mMode));
        }
    }

    /**
     * 设置便签置顶状态。
     */
    public void setPinned(boolean pinned) {
        if (mPinned != pinned) {
            mPinned = pinned;
            mNote.setNoteValue(NoteColumns.PINNED, pinned ? "1" : "0");
            mModifiedDate = System.currentTimeMillis();
        }
    }

    /**
     * 判断便签是否置顶。
     */
    public boolean isPinned() {
        return mPinned;
    }

    /**
     * 设置便签标签（已规范化的英文逗号分隔字符串）。
     */
    public void setTags(String tags) {
        String normalized = tags == null ? "" : tags;
        if (!TextUtils.equals(mTags, normalized)) {
            mTags = normalized;
            mNote.setNoteValue(NoteColumns.TAGS, mTags);
            mModifiedDate = System.currentTimeMillis();
        }
    }

    /**
     * 获取便签标签（英文逗号分隔）。
     */
    public String getTags() {
        return mTags == null ? "" : mTags;
    }

    /**
     * 设置小部件类型。
     */
    public void setWidgetType(int type) {
        if (type != mWidgetType) {
            mWidgetType = type;
            mNote.setNoteValue(NoteColumns.WIDGET_TYPE, String.valueOf(mWidgetType));
        }
    }

    /**
     * 设置小部件ID。
     */
    public void setWidgetId(int id) {
        if (id != mWidgetId) {
            mWidgetId = id;
            mNote.setNoteValue(NoteColumns.WIDGET_ID, String.valueOf(mWidgetId));
        }
    }

    /**
     * 设置工作文本。
     */
    public void setWorkingText(String text) {
        if (!TextUtils.equals(mContent, text)) {
            mContent = text;
            mNote.setTextData(DataColumns.CONTENT, mContent);
        }
    }

    /**
     * 把普通便签转换为通话记录便签，并保存电话号码和通话日期。
     */
    public void convertToCallNote(String phoneNumber, long callDate) {
        mNote.setCallData(CallNote.CALL_DATE, String.valueOf(callDate));
        mNote.setCallData(CallNote.PHONE_NUMBER, phoneNumber);
        mNote.setNoteValue(NoteColumns.PARENT_ID, String.valueOf(Notes.ID_CALL_RECORD_FOLDER));
    }

    /**
     * 判断是否有闹钟提醒。
     */
    public boolean hasClockAlert() {
        return (mAlertDate > 0 ? true : false);
    }

    /**
     * 获取内容。
     */
    public String getContent() {
        return mContent;
    }

    /**
     * 获取提醒日期。
     */
    public long getAlertDate() {
        return mAlertDate;
    }

    /**
     * 获取修改日期。
     */
    public long getModifiedDate() {
        return mModifiedDate;
    }

    /**
     * 获取当前背景色对应的资源 ID。
     */
    public int getBgColorResId() {
        return NoteBgResources.getNoteBgResource(mBgColorId);
    }

    /**
     * 获取背景颜色ID。
     */
    public int getBgColorId() {
        return mBgColorId;
    }

    /**
     * 获取当前标题栏背景资源 ID。
     */
    public int getTitleBgResId() {
        return NoteBgResources.getNoteTitleBgResource(mBgColorId);
    }

    /**
     * 获取清单列表模式。
     */
    public int getCheckListMode() {
        return mMode;
    }

    /**
     * 获取便签ID。
     */
    public long getNoteId() {
        return mNoteId;
    }

    /**
     * 获取文件夹ID。
     */
    public long getFolderId() {
        return mFolderId;
    }

    /**
     * 获取小部件ID。
     */
    public int getWidgetId() {
        return mWidgetId;
    }

    /**
     * 获取小部件类型。
     */
    public int getWidgetType() {
        return mWidgetType;
    }

    /**
     * 便签设置变化监听器。便签提醒时间或背景色变化时通知界面。
     */
    public interface NoteSettingChangedListener {

        /**
         * 背景颜色变化回调：刷新便签背景。
         */
        void onBackgroundColorChanged();

        /**
         * 提醒时间变化回调：更新提醒状态。
         */
        void onClockAlertChanged(long date, boolean set);

        /**
         * 处理小部件变化。
         */
        void onWidgetChanged();

        /**
         * 清单模式变化回调：切换编辑模式。
         */
        void onCheckListModeChanged(int oldMode, int newMode);
    }
}

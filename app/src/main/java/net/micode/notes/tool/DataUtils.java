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

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.CallNote;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.ui.NotesListAdapter.AppWidgetAttribute;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 便签数据工具类。封装便签的批量删除、批量移动、可见性判断和摘要处理等数据库操作。
 */
public class DataUtils {
    public static final String TAG = "DataUtils";

    /** 单个标签允许的最大长度。 */
    private static final int MAX_TAG_LENGTH = 32;

    /** 单条便签允许的最大标签数量。 */
    private static final int MAX_TAG_COUNT = 20;

    /**
     * 批量删除指定便签。
     */
    public static boolean batchDeleteNotes(ContentResolver resolver, HashSet<Long> ids) {
        if (ids == null) {
            Log.d(TAG, "the ids is null");
            return true;
        }
        if (ids.size() == 0) {
            Log.d(TAG, "no id is in the hashset");
            return true;
        }

        ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
        for (long id : ids) {
            if(id == Notes.ID_ROOT_FOLDER) {
                Log.e(TAG, "Don't delete system folder root");
                continue;
            }
            ContentProviderOperation.Builder builder = ContentProviderOperation
                    .newDelete(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, id));
            operationList.add(builder.build());
        }
        if (operationList.isEmpty()) {
            // 选中项全部被过滤掉时无需访问 ContentProvider。
            return true;
        }
        try {
            ContentProviderResult[] results = resolver.applyBatch(Notes.AUTHORITY, operationList);
            if (results == null || results.length == 0 || results[0] == null) {
                Log.d(TAG, "delete notes failed, ids:" + ids.toString());
                return false;
            }
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
        } catch (OperationApplicationException e) {
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
        }
        return false;
    }

    /**
     * 批量把便签移动到目标文件夹。
     */
    public static boolean batchMoveToFolder(ContentResolver resolver, HashSet<Long> ids,
            long folderId) {
        if (ids == null) {
            Log.d(TAG, "the ids is null");
            return true;
        }
        if (ids.isEmpty()) {
            Log.d(TAG, "no id is in the hashset");
            return true;
        }

        ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
        for (long id : ids) {
            ContentProviderOperation.Builder builder = ContentProviderOperation
                    .newUpdate(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, id));
            builder.withValue(NoteColumns.PARENT_ID, folderId);
            builder.withValue(NoteColumns.LOCAL_MODIFIED, 1);
            operationList.add(builder.build());
        }
        if (operationList.isEmpty()) {
            return true;
        }

        try {
            ContentProviderResult[] results = resolver.applyBatch(Notes.AUTHORITY, operationList);
            if (results == null || results.length == 0 || results[0] == null) {
                Log.d(TAG, "delete notes failed, ids:" + ids.toString());
                return false;
            }
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
        } catch (OperationApplicationException e) {
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
        }
        return false;
    }

    /**
     * 批量设置便签的置顶状态。
     *
     * @return 全部更新成功返回 true；参数为空时视为无需操作并返回 true。
     */
    public static boolean setNotesPinned(ContentResolver resolver, HashSet<Long> ids, boolean pinned) {
        if (resolver == null || ids == null || ids.isEmpty()) {
            return true;
        }

        ContentValues values = new ContentValues();
        values.put(NoteColumns.PINNED, pinned ? 1 : 0);
        values.put(NoteColumns.LOCAL_MODIFIED, 1);

        ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
        for (long id : ids) {
            if (id <= 0) {
                // 系统文件夹或非法 ID 不允许置顶。
                continue;
            }
            operationList.add(ContentProviderOperation
                    .newUpdate(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, id))
                    .withValues(values)
                    .build());
        }
        if (operationList.isEmpty()) {
            return true;
        }

        try {
            ContentProviderResult[] results = resolver.applyBatch(Notes.AUTHORITY, operationList);
            return results != null && results.length > 0;
        } catch (RemoteException e) {
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
        } catch (OperationApplicationException e) {
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
        }
        return false;
    }

    /**
     * 统计所有便签使用过的标签，按名称排序返回。
     */
    public static List<String> getAllTags(ContentResolver resolver) {
        List<String> tags = new ArrayList<String>();
        if (resolver == null) {
            return tags;
        }
        Cursor cursor = resolver.query(Notes.CONTENT_NOTE_URI,
                new String[] { NoteColumns.TAGS },
                NoteColumns.TYPE + "=? AND " + NoteColumns.PARENT_ID + "<>?",
                new String[] { String.valueOf(Notes.TYPE_NOTE),
                        String.valueOf(Notes.ID_TRASH_FOLER) },
                null);
        if (cursor == null) {
            return tags;
        }

        Set<String> uniqueTags = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        try {
            while (cursor.moveToNext()) {
                String rawTags = cursor.getString(0);
                if (TextUtils.isEmpty(rawTags)) {
                    continue;
                }
                for (String tag : rawTags.split(",")) {
                    String trimmed = tag.trim();
                    if (!TextUtils.isEmpty(trimmed)) {
                        uniqueTags.add(trimmed);
                    }
                }
            }
        } finally {
            cursor.close();
        }
        tags.addAll(uniqueTags);
        return tags;
    }

    /**
     * 把用户输入的标签规范化为英文逗号分隔的字符串。
     *
     * <p>支持中英文逗号、分号、顿号和空白作为分隔符，自动去重、去掉前导 # 和首尾空白。</p>
     */
    public static String normalizeTags(String rawTags) {
        if (TextUtils.isEmpty(rawTags)) {
            return "";
        }
        String[] parts = rawTags.split("[,，;；、\\s]+");
        LinkedHashSet<String> tags = new LinkedHashSet<String>();
        // 用大小写不敏感的集合去重，保留用户第一次输入的写法。
        Set<String> seenTags = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        for (String part : parts) {
            String tag = part == null ? "" : part.trim();
            while (tag.startsWith("#")) {
                tag = tag.substring(1).trim();
            }
            if (!TextUtils.isEmpty(tag)) {
                // 单个标签限制长度，避免异常输入撑大数据库和列表。
                if (tag.length() > MAX_TAG_LENGTH) {
                    tag = tag.substring(0, MAX_TAG_LENGTH);
                }
                if (!seenTags.add(tag)) {
                    continue;
                }
                tags.add(tag);
                if (tags.size() >= MAX_TAG_COUNT) {
                    break;
                }
            }
        }
        return TextUtils.join(",", tags);
    }

    /**
     * 把数据库中的标签格式化为列表展示文本，例如 "#工作 #重要"。
     */
    public static String formatTagsForDisplay(String tags) {
        if (TextUtils.isEmpty(tags)) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String tag : tags.split(",")) {
            String trimmed = tag.trim();
            if (TextUtils.isEmpty(trimmed)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append('#').append(trimmed);
        }
        return builder.toString();
    }

    /**
     * 生成按标签筛选便签的 SQL 条件，配合 {@link #buildTagFilterArgs(String)} 使用。
     *
     * <p>标签以逗号分隔存储，这里通过拼接首尾逗号实现完整标签匹配，避免 "work" 命中
     * "homework" 之类的子串。</p>
     */
    public static String buildTagFilterSelection() {
        return "('' || ',' || " + NoteColumns.TAGS + " || ',') LIKE ? ESCAPE '\\'";
    }

    /**
     * 生成按标签筛选的 selectionArgs。
     */
    public static String[] buildTagFilterArgs(String tag) {
        return new String[] { "%," + escapeLike(tag) + ",%" };
    }

    /**
     * 转义 LIKE 通配符，避免标签中的 %、_、\ 被当作模式字符。
     */
    public static String escapeLike(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' || c == '%' || c == '_') {
                builder.append('\\');
            }
            builder.append(c);
        }
        return builder.toString();
    }

    /**
     * 统计用户自建文件夹数量。
     */
    public static int getUserFolderCount(ContentResolver resolver) {
        Cursor cursor =resolver.query(Notes.CONTENT_NOTE_URI,
                new String[] { "COUNT(*)" },
                NoteColumns.TYPE + "=? AND " + NoteColumns.PARENT_ID + "<>?",
                new String[] { String.valueOf(Notes.TYPE_FOLDER), String.valueOf(Notes.ID_TRASH_FOLER)},
                null);

        int count = 0;
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    count = cursor.getInt(0);
                }
            } catch (IndexOutOfBoundsException e) {
                Log.e(TAG, "get folder count failed:" + e.toString());
            } finally {
                // 无论查询结果是否为空都要关闭游标，避免泄漏。
                cursor.close();
            }
        }
        return count;
    }

    /**
     * 判断便签在数据库中是否可见（未被删除）。
     */
    public static boolean visibleInNoteDatabase(ContentResolver resolver, long noteId, int type) {
        Cursor cursor = resolver.query(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId),
                null,
                NoteColumns.TYPE + "=? AND " + NoteColumns.PARENT_ID + "<>" + Notes.ID_TRASH_FOLER,
                new String [] {String.valueOf(type)},
                null);

        boolean exist = false;
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                exist = true;
            }
            cursor.close();
        }
        return exist;
    }

    /**
     * 判断便签 ID 是否存在于便签表。
     */
    public static boolean existInNoteDatabase(ContentResolver resolver, long noteId) {
        Cursor cursor = resolver.query(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId),
                null, null, null, null);

        boolean exist = false;
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                exist = true;
            }
            cursor.close();
        }
        return exist;
    }

    /**
     * 判断数据 ID 是否存在于数据表。
     */
    public static boolean existInDataDatabase(ContentResolver resolver, long dataId) {
        Cursor cursor = resolver.query(ContentUris.withAppendedId(Notes.CONTENT_DATA_URI, dataId),
                null, null, null, null);

        boolean exist = false;
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                exist = true;
            }
            cursor.close();
        }
        return exist;
    }

    /**
     * 检查文件夹名称是否已被占用或非法。
     */
    public static boolean checkVisibleFolderName(ContentResolver resolver, String name) {
        Cursor cursor = resolver.query(Notes.CONTENT_NOTE_URI, null,
                NoteColumns.TYPE + "=" + Notes.TYPE_FOLDER +
                " AND " + NoteColumns.PARENT_ID + "<>" + Notes.ID_TRASH_FOLER +
                " AND " + NoteColumns.SNIPPET + "=?",
                new String[] { name }, null);
        boolean exist = false;
        if(cursor != null) {
            if(cursor.getCount() > 0) {
                exist = true;
            }
            cursor.close();
        }
        return exist;
    }

    /**
     * 获取文件夹或小部件对应的便签 ID。
     */
    public static HashSet<AppWidgetAttribute> getFolderNoteWidget(ContentResolver resolver, long folderId) {
        Cursor c = resolver.query(Notes.CONTENT_NOTE_URI,
                new String[] { NoteColumns.WIDGET_ID, NoteColumns.WIDGET_TYPE },
                NoteColumns.PARENT_ID + "=?",
                new String[] { String.valueOf(folderId) },
                null);

        HashSet<AppWidgetAttribute> set = null;
        if (c != null) {
            if (c.moveToFirst()) {
                set = new HashSet<AppWidgetAttribute>();
                do {
                    try {
                        AppWidgetAttribute widget = new AppWidgetAttribute();
                        widget.widgetId = c.getInt(0);
                        widget.widgetType = c.getInt(1);
                        set.add(widget);
                    } catch (IndexOutOfBoundsException e) {
                        Log.e(TAG, e.toString());
                    }
                } while (c.moveToNext());
            }
            c.close();
        }
        return set;
    }

    /**
     * 根据便签 ID 获取关联的电话号码。
     */
    public static String getCallNumberByNoteId(ContentResolver resolver, long noteId) {
        Cursor cursor = resolver.query(Notes.CONTENT_DATA_URI,
                new String [] { CallNote.PHONE_NUMBER },
                CallNote.NOTE_ID + "=? AND " + CallNote.MIME_TYPE + "=?",
                new String [] { String.valueOf(noteId), CallNote.CONTENT_ITEM_TYPE },
                null);

        if (cursor == null) {
            return "";
        }
        try {
            if (cursor.moveToFirst()) {
                String phoneNumber = cursor.getString(0);
                return phoneNumber == null ? "" : phoneNumber;
            }
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "Get call number fails " + e.toString());
        } finally {
            // 即使没有查询结果也必须关闭游标。
            cursor.close();
        }
        return "";
    }

    /**
     * 根据电话号码和通话日期查找通话记录便签。
     */
    public static long getNoteIdByPhoneNumberAndCallDate(ContentResolver resolver, String phoneNumber, long callDate) {
        Cursor cursor = resolver.query(Notes.CONTENT_DATA_URI,
                new String [] { CallNote.NOTE_ID },
                CallNote.CALL_DATE + "=? AND " + CallNote.MIME_TYPE + "=? AND PHONE_NUMBERS_EQUAL("
                + CallNote.PHONE_NUMBER + ",?)",
                new String [] { String.valueOf(callDate), CallNote.CONTENT_ITEM_TYPE, phoneNumber },
                null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    try {
                        return cursor.getLong(0);
                    } catch (IndexOutOfBoundsException e) {
                        Log.e(TAG, "Get call note id fails " + e.toString());
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return 0;
    }

    /**
     * 根据便签 ID 获取摘要文本。
     */
    public static String getSnippetById(ContentResolver resolver, long noteId) {
        Cursor cursor = resolver.query(Notes.CONTENT_NOTE_URI,
                new String [] { NoteColumns.SNIPPET },
                NoteColumns.ID + "=?",
                new String [] { String.valueOf(noteId)},
                null);

        if (cursor != null) {
            String snippet = "";
            try {
                if (cursor.moveToFirst()) {
                    String value = cursor.getString(0);
                    snippet = value == null ? "" : value;
                }
            } finally {
                cursor.close();
            }
            return snippet;
        }
        // 便签可能在查询前被删除，返回空串而不是抛异常导致调用方崩溃。
        Log.w(TAG, "Note is not found with id: " + noteId);
        return "";
    }

    /**
     * 把便签内容格式化为列表展示用的摘要。
     */
    public static String getFormattedSnippet(String snippet) {
        if (snippet == null) {
            return "";
        }
        snippet = snippet.trim();
        int index = snippet.indexOf('\n');
        if (index != -1) {
            snippet = snippet.substring(0, index);
        }
        return snippet;
    }
}

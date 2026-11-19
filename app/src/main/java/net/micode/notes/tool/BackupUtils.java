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

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.DataConstants;
import net.micode.notes.data.Notes.NoteColumns;

import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 便签备份工具类。负责把便签内容导出为文本文件。
 * 导出的目标文件由 Storage Access Framework 选择，兼容 Android 10+ 分区存储。
 */
public class BackupUtils {
    private static final String TAG = "BackupUtils";

    private static BackupUtils sInstance;

    public static final int STATE_SYSTEM_ERROR = 3;

    public static final int STATE_SUCCESS = 4;

    private final TextExport mTextExport;

    /**
     * 获取单例实例。
     */
    public static synchronized BackupUtils getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new BackupUtils(context);
        }
        return sInstance;
    }

    /**
     * 构造便签备份工具。
     */
    private BackupUtils(Context context) {
        mTextExport = new TextExport(context);
    }

    /**
     * 生成导出文件建议名称，供 SAF 文件选择器使用。
     */
    public String getSuggestedFileName() {
        return mTextExport.getSuggestedFileName();
    }

    /**
     * 把全部便签写入 SAF 返回的目标 Uri。
     */
    public int exportToText(ContentResolver resolver, Uri uri) {
        OutputStream outputStream = null;
        try {
            outputStream = resolver.openOutputStream(uri, "wt");
            if (outputStream == null) {
                Log.e(TAG, "open output stream failed");
                return STATE_SYSTEM_ERROR;
            }

            PrintStream printStream = new PrintStream(outputStream, false, "UTF-8");
            mTextExport.exportToText(printStream);
            printStream.flush();
            boolean hasError = printStream.checkError();
            printStream.close();
            outputStream = null;
            return hasError ? STATE_SYSTEM_ERROR : STATE_SUCCESS;
        } catch (Exception e) {
            Log.e(TAG, "export notes failed", e);
            return STATE_SYSTEM_ERROR;
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception e) {
                    Log.e(TAG, "close output stream failed", e);
                }
            }
        }
    }

    /**
     * 文本导出内部类，负责遍历文件夹和便签并写入打印流。
     */
    private static class TextExport {
        private static final String[] NOTE_PROJECTION = {
                NoteColumns.ID,
                NoteColumns.MODIFIED_DATE,
                NoteColumns.SNIPPET,
                NoteColumns.TYPE
        };

        private static final int NOTE_COLUMN_ID = 0;

        private static final int NOTE_COLUMN_MODIFIED_DATE = 1;

        private static final int NOTE_COLUMN_SNIPPET = 2;

        private static final String[] DATA_PROJECTION = {
                DataColumns.CONTENT,
                DataColumns.MIME_TYPE,
                DataColumns.DATA1,
                DataColumns.DATA2,
                DataColumns.DATA3,
                DataColumns.DATA4,
        };

        private static final int DATA_COLUMN_CONTENT = 0;

        private static final int DATA_COLUMN_MIME_TYPE = 1;

        private static final int DATA_COLUMN_CALL_DATE = 2;

        private static final int DATA_COLUMN_PHONE_NUMBER = 4;

        private final String[] TEXT_FORMAT;

        private static final int FORMAT_FOLDER_NAME = 0;

        private static final int FORMAT_NOTE_DATE = 1;

        private static final int FORMAT_NOTE_CONTENT = 2;

        private final Context mContext;

        /**
         * 构造文本导出对象，读取导出格式。
         */
        TextExport(Context context) {
            TEXT_FORMAT = context.getResources().getStringArray(R.array.format_for_exported_note);
            mContext = context;
        }

        /**
         * 生成带时间戳的导出文件名。
         */
        String getSuggestedFileName() {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(new Date());
            return String.format(mContext.getString(R.string.file_name_txt_format), timestamp);
        }

        /**
         * 获取当前导出格式。
         */
        private String getFormat(int id) {
            return TEXT_FORMAT[id];
        }

        /**
         * 把全部文件夹和便签写入打印流。
         */
        void exportToText(PrintStream printStream) {
            exportFolders(printStream);
            exportRootNotes(printStream);
        }

        /**
         * 导出文件夹及其中的便签。
         */
        private void exportFolders(PrintStream printStream) {
            Cursor folderCursor = mContext.getContentResolver().query(
                    Notes.CONTENT_NOTE_URI,
                    NOTE_PROJECTION,
                    "(" + NoteColumns.TYPE + "=" + Notes.TYPE_FOLDER + " AND "
                            + NoteColumns.PARENT_ID + "<>" + Notes.ID_TRASH_FOLER + ") OR "
                            + NoteColumns.ID + "=" + Notes.ID_CALL_RECORD_FOLDER, null, null);
            if (folderCursor == null) {
                return;
            }

            try {
                while (folderCursor.moveToNext()) {
                    String folderName;
                    if (folderCursor.getLong(NOTE_COLUMN_ID) == Notes.ID_CALL_RECORD_FOLDER) {
                        folderName = mContext.getString(R.string.call_record_folder_name);
                    } else {
                        folderName = folderCursor.getString(NOTE_COLUMN_SNIPPET);
                    }
                    if (!TextUtils.isEmpty(folderName)) {
                        printStream.println(String.format(getFormat(FORMAT_FOLDER_NAME), folderName));
                    }
                    exportFolderToText(folderCursor.getString(NOTE_COLUMN_ID), printStream);
                }
            } finally {
                folderCursor.close();
            }
        }

        /**
         * 导出根目录下的便签。
         */
        private void exportRootNotes(PrintStream printStream) {
            Cursor noteCursor = mContext.getContentResolver().query(
                    Notes.CONTENT_NOTE_URI,
                    NOTE_PROJECTION,
                    NoteColumns.TYPE + "=" + Notes.TYPE_NOTE + " AND " + NoteColumns.PARENT_ID
                            + "=0", null, null);
            if (noteCursor == null) {
                return;
            }

            try {
                while (noteCursor.moveToNext()) {
                    printStream.println(String.format(getFormat(FORMAT_NOTE_DATE),
                            DateFormat.format(mContext.getString(R.string.format_datetime_mdhm),
                                    noteCursor.getLong(NOTE_COLUMN_MODIFIED_DATE))));
                    exportNoteToText(noteCursor.getString(NOTE_COLUMN_ID), printStream);
                }
            } finally {
                noteCursor.close();
            }
        }

        /**
         * 导出文件夹内的便签。
         */
        private void exportFolderToText(String folderId, PrintStream printStream) {
            Cursor notesCursor = mContext.getContentResolver().query(Notes.CONTENT_NOTE_URI,
                    NOTE_PROJECTION, NoteColumns.PARENT_ID + "=?", new String[] {
                        folderId
                    }, null);
            if (notesCursor == null) {
                return;
            }

            try {
                while (notesCursor.moveToNext()) {
                    printStream.println(String.format(getFormat(FORMAT_NOTE_DATE),
                            DateFormat.format(mContext.getString(R.string.format_datetime_mdhm),
                                    notesCursor.getLong(NOTE_COLUMN_MODIFIED_DATE))));
                    exportNoteToText(notesCursor.getString(NOTE_COLUMN_ID), printStream);
                }
            } finally {
                notesCursor.close();
            }
        }

        /**
         * 导出单个便签的文本或通话数据。
         */
        private void exportNoteToText(String noteId, PrintStream printStream) {
            Cursor dataCursor = mContext.getContentResolver().query(Notes.CONTENT_DATA_URI,
                    DATA_PROJECTION, DataColumns.NOTE_ID + "=?", new String[] {
                        noteId
                    }, null);
            if (dataCursor == null) {
                return;
            }

            try {
                while (dataCursor.moveToNext()) {
                    String mimeType = dataCursor.getString(DATA_COLUMN_MIME_TYPE);
                    if (DataConstants.CALL_NOTE.equals(mimeType)) {
                        String phoneNumber = dataCursor.getString(DATA_COLUMN_PHONE_NUMBER);
                        long callDate = dataCursor.getLong(DATA_COLUMN_CALL_DATE);
                        String location = dataCursor.getString(DATA_COLUMN_CONTENT);

                        if (!TextUtils.isEmpty(phoneNumber)) {
                            printStream.println(String.format(getFormat(FORMAT_NOTE_CONTENT),
                                    phoneNumber));
                        }
                        printStream.println(String.format(getFormat(FORMAT_NOTE_CONTENT),
                                DateFormat.format(mContext.getString(R.string.format_datetime_mdhm),
                                        callDate)));
                        if (!TextUtils.isEmpty(location)) {
                            printStream.println(String.format(getFormat(FORMAT_NOTE_CONTENT),
                                    location));
                        }
                    } else if (DataConstants.NOTE.equals(mimeType)) {
                        String content = dataCursor.getString(DATA_COLUMN_CONTENT);
                        if (!TextUtils.isEmpty(content)) {
                            printStream.println(String.format(getFormat(FORMAT_NOTE_CONTENT),
                                    content));
                        }
                    }
                }
            } finally {
                dataCursor.close();
            }

            printStream.println();
        }
    }
}

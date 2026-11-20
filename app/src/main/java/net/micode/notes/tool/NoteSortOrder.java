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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import net.micode.notes.R;
import net.micode.notes.data.Notes.NoteColumns;

/**
 * 便签列表排序方式。负责排序偏好的读写和 SQL ORDER BY 的生成。
 *
 * <p>排序规则统一保证文件夹排在便签之前，便签内部置顶项优先。</p>
 */
public final class NoteSortOrder {
    /** 按修改时间倒序，默认排序。 */
    public static final int MODIFIED_DESC = 0;

    /** 按创建时间倒序。 */
    public static final int CREATED_DESC = 1;

    /** 按标题（摘要首行）升序。 */
    public static final int TITLE_ASC = 2;

    private static final String PREFERENCE_SORT_ORDER = "note_sort_order";

    private static final int DEFAULT_SORT_ORDER = MODIFIED_DESC;

    private NoteSortOrder() {
    }

    /**
     * 读取用户选择的排序方式；取值非法时回退到默认值。
     */
    public static int get(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int order = prefs.getInt(PREFERENCE_SORT_ORDER, DEFAULT_SORT_ORDER);
        return isValid(order) ? order : DEFAULT_SORT_ORDER;
    }

    /**
     * 保存用户选择的排序方式，非法值会被忽略。
     */
    public static void set(Context context, int order) {
        if (!isValid(order)) {
            return;
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putInt(PREFERENCE_SORT_ORDER, order).apply();
    }

    /**
     * 判断排序方式是否合法。
     */
    public static boolean isValid(int order) {
        return order == MODIFIED_DESC || order == CREATED_DESC || order == TITLE_ASC;
    }

    /**
     * 获取排序方式对应的菜单标题资源。
     */
    public static int getTitleRes(int order) {
        switch (order) {
            case CREATED_DESC:
                return R.string.menu_sort_created;
            case TITLE_ASC:
                return R.string.menu_sort_title;
            case MODIFIED_DESC:
            default:
                return R.string.menu_sort_modified;
        }
    }

    /**
     * 生成便签列表查询的 ORDER BY 子句。
     *
     * <p>排序优先级：文件夹/系统文件夹 > 便签，便签内 > 置顶 > 用户选择的排序方式。
     * 标题排序使用 TRIM + NOCASE，忽略首尾空白和大小写差异。</p>
     */
    public static String buildOrderBy(Context context) {
        String orderBy;
        switch (get(context)) {
            case CREATED_DESC:
                orderBy = NoteColumns.CREATED_DATE + " DESC";
                break;
            case TITLE_ASC:
                orderBy = "TRIM(" + NoteColumns.SNIPPET + ") COLLATE NOCASE ASC";
                break;
            case MODIFIED_DESC:
            default:
                orderBy = NoteColumns.MODIFIED_DATE + " DESC";
                break;
        }
        // 修改时间作为稳定的兜底排序，避免相同排序键下列表顺序频繁变化。
        return NoteColumns.TYPE + " DESC, " + NoteColumns.PINNED + " DESC, " + orderBy
                + ", " + NoteColumns.MODIFIED_DATE + " DESC";
    }
}

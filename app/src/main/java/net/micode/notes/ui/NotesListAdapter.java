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

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import net.micode.notes.data.Notes;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * 便签列表适配器。把便签游标数据绑定为可复用的列表项视图。
 */
public class NotesListAdapter extends CursorAdapter {
    private static final String TAG = "NotesListAdapter";
    private Context mContext;
    private HashMap<Integer, Boolean> mSelectedIndex;
    private int mNotesCount;
    private boolean mChoiceMode;

    /**
     * 桌面小部件属性。保存小部件 ID 和类型。
     */
    public static class AppWidgetAttribute {
        public int widgetId;
        public int widgetType;
    };

    /**
     * 构造 NotesListAdapter 对象，并初始化内部状态。
     */
    public NotesListAdapter(Context context) {
        super(context, null);
        mSelectedIndex = new HashMap<Integer, Boolean>();
        mContext = context;
        mNotesCount = 0;
    }

    /**
     * 创建便签列表项视图。
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return new NotesListItem(context);
    }

    /**
     * 把便签数据绑定到列表项。
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // 列表项视图统一使用 NotesListItem 展示便签数据。
        if (view instanceof NotesListItem) {
            NoteItemData itemData = new NoteItemData(context, cursor);
            ((NotesListItem) view).bind(context, itemData, mChoiceMode,
                    isSelectedItem(cursor.getPosition()));
        }
    }

    /**
     * 设置指定列表项的选中状态。
     */
    public void setCheckedItem(final int position, final boolean checked) {
        if (position < 0 || position >= getCount()) {
            Log.w(TAG, "Invalid checked position:" + position);
            return;
        }
        mSelectedIndex.put(position, checked);
        notifyDataSetChanged();
    }

    /**
     * 判断列表是否处于选择模式。
     */
    public boolean isInChoiceMode() {
        return mChoiceMode;
    }

    /**
     * 设置列表选择模式。
     */
    public void setChoiceMode(boolean mode) {
        mSelectedIndex.clear();
        mChoiceMode = mode;
        notifyDataSetChanged();
    }

    /**
     * 全选或取消全选。
     */
    public void selectAll(boolean checked) {
        Cursor cursor = getCursor();
        if (cursor == null) {
            return;
        }
        for (int i = 0; i < getCount(); i++) {
            if (cursor.moveToPosition(i)) {
                if (NoteItemData.getNoteType(cursor) == Notes.TYPE_NOTE) {
                    setCheckedItem(i, checked);
                }
            }
        }
    }

    /**
     * 获取所有选中便签的 ID。
     */
    public HashSet<Long> getSelectedItemIds() {
        HashSet<Long> itemSet = new HashSet<Long>();
        for (Integer position : mSelectedIndex.keySet()) {
            Boolean selected = mSelectedIndex.get(position);
            if (selected != null && selected && position >= 0 && position < getCount()) {
                long id = getItemId(position);
                if (id == Notes.ID_ROOT_FOLDER) {
                    Log.d(TAG, "Wrong item id, should not happen");
                } else if (id > 0) {
                    itemSet.add(id);
                }
            }
        }

        return itemSet;
    }

    /**
     * 获取选中项对应的小部件属性。
     */
    public HashSet<AppWidgetAttribute> getSelectedWidget() {
        HashSet<AppWidgetAttribute> itemSet = new HashSet<AppWidgetAttribute>();
        for (Integer position : mSelectedIndex.keySet()) {
            Boolean selected = mSelectedIndex.get(position);
            if (selected != null && selected && position >= 0 && position < getCount()) {
                Object item = getItem(position);
                if (item instanceof Cursor) {
                    AppWidgetAttribute widget = new AppWidgetAttribute();
                    NoteItemData itemData = new NoteItemData(mContext, (Cursor) item);
                    widget.widgetId = itemData.getWidgetId();
                    widget.widgetType = itemData.getWidgetType();
                    itemSet.add(widget);
                } else {
                    Log.e(TAG, "Invalid cursor");
                    return null;
                }
            }
        }
        return itemSet;
    }

    /**
     * 判断当前选中的便签是否全部已置顶。
     *
     * <p>没有任何选中项时返回 false，调用方据此决定显示“置顶”还是“取消置顶”。</p>
     */
    public boolean areAllSelectedPinned() {
        int selectedCount = 0;
        for (Integer position : mSelectedIndex.keySet()) {
            Boolean selected = mSelectedIndex.get(position);
            if (selected == null || !selected || position < 0 || position >= getCount()) {
                continue;
            }
            Object item = getItem(position);
            if (item instanceof Cursor
                    && new NoteItemData(mContext, (Cursor) item).isPinned()) {
                selectedCount++;
            }
        }
        return selectedCount > 0 && selectedCount == getSelectedCount();
    }

    /**
     * 获取选中项数量。
     */
    public int getSelectedCount() {
        Collection<Boolean> values = mSelectedIndex.values();
        if (null == values) {
            return 0;
        }
        Iterator<Boolean> iter = values.iterator();
        int count = 0;
        while (iter.hasNext()) {
            if (Boolean.TRUE.equals(iter.next())) {
                count++;
            }
        }
        return count;
    }

    /**
     * 判断是否所有项都已选中。
     */
    public boolean isAllSelected() {
        int checkedCount = getSelectedCount();
        return (checkedCount != 0 && checkedCount == mNotesCount);
    }

    /**
     * 判断指定位置是否被选中。
     */
    public boolean isSelectedItem(final int position) {
        if (null == mSelectedIndex.get(position)) {
            return false;
        }
        return Boolean.TRUE.equals(mSelectedIndex.get(position));
    }

    /**
     * 处理内容变化。
     */
    @Override
    protected void onContentChanged() {
        super.onContentChanged();
        calcNotesCount();
    }

    /**
     * 切换列表游标并刷新选中状态。
     */
    @Override
    public void changeCursor(Cursor cursor) {
        super.changeCursor(cursor);
        calcNotesCount();
    }

    /**
     * 统计便签数量。
     */
    private void calcNotesCount() {
        mNotesCount = 0;
        Cursor cursor = getCursor();
        if (cursor == null) {
            return;
        }
        for (int i = 0; i < getCount(); i++) {
            if (cursor.moveToPosition(i)
                    && NoteItemData.getNoteType(cursor) == Notes.TYPE_NOTE) {
                mNotesCount++;
            }
        }
    }
}

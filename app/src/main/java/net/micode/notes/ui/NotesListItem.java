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
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.tool.DataUtils;
import net.micode.notes.tool.ResourceParser.NoteItemBgResources;

/**
 * 便签列表项视图。负责展示便签标题、摘要、时间和背景样式。
 */
public class NotesListItem extends LinearLayout {
    private ImageView mAlert;
    private ImageView mPin;
    private TextView mTitle;
    private TextView mTime;
    private TextView mTags;
    private TextView mCallName;
    private NoteItemData mItemData;
    private CheckBox mCheckBox;

    /**
     * 构造 NotesListItem 对象，并初始化内部状态。
     */
    public NotesListItem(Context context) {
        super(context);
        inflate(context, R.layout.note_item, this);
        mAlert = (ImageView) findViewById(R.id.iv_alert_icon);
        mPin = (ImageView) findViewById(R.id.iv_pin_icon);
        mTitle = (TextView) findViewById(R.id.tv_title);
        mTime = (TextView) findViewById(R.id.tv_time);
        mTags = (TextView) findViewById(R.id.tv_tags);
        mCallName = (TextView) findViewById(R.id.tv_name);
        mCheckBox = (CheckBox) findViewById(android.R.id.checkbox);
    }

    /**
     * 根据便签数据设置标题、摘要、时间和背景。
     */
    public void bind(Context context, NoteItemData data, boolean choiceMode, boolean checked) {
        if (data == null) {
            return;
        }

        // 多选模式下显示复选框，普通模式下隐藏。
        if (choiceMode && data.getType() == Notes.TYPE_NOTE) {
            mCheckBox.setVisibility(View.VISIBLE);
            mCheckBox.setChecked(checked);
        } else {
            mCheckBox.setVisibility(View.GONE);
        }

        mItemData = data;
        // 置顶标记只对普通便签展示，文件夹和通话记录保持原有样式。
        if (data.getType() == Notes.TYPE_NOTE && data.isPinned()) {
            mPin.setVisibility(View.VISIBLE);
        } else {
            mPin.setVisibility(View.GONE);
        }

        // 通话记录文件夹、通话便签和普通便签使用不同的展示样式。
        if (data.getId() == Notes.ID_CALL_RECORD_FOLDER) {
            mCallName.setVisibility(View.GONE);
            mAlert.setVisibility(View.VISIBLE);
            mTitle.setTextAppearance(context, R.style.TextAppearancePrimaryItem);
            mTitle.setText(context.getString(R.string.call_record_folder_name)
                    + context.getString(R.string.format_folder_files_count, data.getNotesCount()));
            mAlert.setImageResource(R.drawable.call_record);
        } else if (data.getParentId() == Notes.ID_CALL_RECORD_FOLDER) {
            mCallName.setVisibility(View.VISIBLE);
            mCallName.setText(data.getCallName());
            mTitle.setTextAppearance(context,R.style.TextAppearanceSecondaryItem);
            mTitle.setText(DataUtils.getFormattedSnippet(data.getSnippet()));
            if (data.hasAlert()) {
                mAlert.setImageResource(R.drawable.clock);
                mAlert.setVisibility(View.VISIBLE);
            } else {
                mAlert.setVisibility(View.GONE);
            }
        } else {
            mCallName.setVisibility(View.GONE);
            mTitle.setTextAppearance(context, R.style.TextAppearancePrimaryItem);

            if (data.getType() == Notes.TYPE_FOLDER) {
                mTitle.setText(data.getSnippet()
                        + context.getString(R.string.format_folder_files_count,
                                data.getNotesCount()));
                mAlert.setVisibility(View.GONE);
            } else {
                mTitle.setText(DataUtils.getFormattedSnippet(data.getSnippet()));
                if (data.hasAlert()) {
                    mAlert.setImageResource(R.drawable.clock);
                    mAlert.setVisibility(View.VISIBLE);
                } else {
                    mAlert.setVisibility(View.GONE);
                }
            }
        }

        // 标签行只在普通便签存在标签时展示，文件夹和通话便签保持单行高度。
        if (data.getType() == Notes.TYPE_NOTE
                && data.getParentId() != Notes.ID_CALL_RECORD_FOLDER) {
            String tags = DataUtils.formatTagsForDisplay(data.getTags());
            if (TextUtils.isEmpty(tags)) {
                mTags.setVisibility(View.GONE);
            } else {
                mTags.setText(tags);
                mTags.setVisibility(View.VISIBLE);
            }
        } else {
            mTags.setVisibility(View.GONE);
        }

        mTime.setText(DateUtils.getRelativeTimeSpanString(data.getModifiedDate()));

        setBackground(data);
    }

    /**
     * 设置背景。
     */
    private void setBackground(NoteItemData data) {
        int id = data.getBgColorId();
        if (data.getType() == Notes.TYPE_NOTE) {
            if (data.isSingle() || data.isOneFollowingFolder()) {
                setBackgroundResource(NoteItemBgResources.getNoteBgSingleRes(id));
            } else if (data.isLast()) {
                setBackgroundResource(NoteItemBgResources.getNoteBgLastRes(id));
            } else if (data.isFirst() || data.isMultiFollowingFolder()) {
                setBackgroundResource(NoteItemBgResources.getNoteBgFirstRes(id));
            } else {
                setBackgroundResource(NoteItemBgResources.getNoteBgNormalRes(id));
            }
        } else {
            setBackgroundResource(NoteItemBgResources.getFolderBgRes());
        }
    }

    /**
     * 获取列表项数据。
     */
    public NoteItemData getItemData() {
        return mItemData;
    }
}

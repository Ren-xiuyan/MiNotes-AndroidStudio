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

package net.micode.notes.gtask.data;

import android.database.Cursor;

import org.json.JSONObject;

/**
 * Google Tasks 同步节点的抽象基类。定义同步动作常量以及 GID、名称、修改时间、删除标记等公共字段。
 */
public abstract class Node {
    public static final int SYNC_ACTION_NONE = 0;

    public static final int SYNC_ACTION_ADD_REMOTE = 1;

    public static final int SYNC_ACTION_ADD_LOCAL = 2;

    public static final int SYNC_ACTION_DEL_REMOTE = 3;

    public static final int SYNC_ACTION_DEL_LOCAL = 4;

    public static final int SYNC_ACTION_UPDATE_REMOTE = 5;

    public static final int SYNC_ACTION_UPDATE_LOCAL = 6;

    public static final int SYNC_ACTION_UPDATE_CONFLICT = 7;

    public static final int SYNC_ACTION_ERROR = 8;

    private String mGid;

    private String mName;

    private long mLastModified;

    private boolean mDeleted;

    /**
     * 构造 Node 对象，并初始化内部状态。
     */
    public Node() {
        mGid = null;
        mName = "";
        mLastModified = 0;
        mDeleted = false;
    }

    /**
     * 生成创建节点的 JSON 请求体，由子类实现。
     */
    public abstract JSONObject getCreateAction(int actionId);

    /**
     * 生成更新节点的 JSON 请求体，由子类实现。
     */
    public abstract JSONObject getUpdateAction(int actionId);

    /**
     * 从远程 JSON 恢复节点内容，由子类实现。
     */
    public abstract void setContentByRemoteJSON(JSONObject js);

    /**
     * 从本地 JSON 恢复节点内容，由子类实现。
     */
    public abstract void setContentByLocalJSON(JSONObject js);

    /**
     * 把本地节点内容转换为 JSON，由子类实现。
     */
    public abstract JSONObject getLocalJSONFromContent();

    /**
     * 计算当前节点应执行的同步动作，由子类实现。
     */
    public abstract int getSyncAction(Cursor c);

    /**
     * 设置远程节点的全局 ID。
     */
    public void setGid(String gid) {
        this.mGid = gid;
    }

    /**
     * 设置节点名称。
     */
    public void setName(String name) {
        this.mName = name;
    }

    /**
     * 设置远程节点的最后修改时间。
     */
    public void setLastModified(long lastModified) {
        this.mLastModified = lastModified;
    }

    /**
     * 设置节点的删除标记。
     */
    public void setDeleted(boolean deleted) {
        this.mDeleted = deleted;
    }

    /**
     * 获取远程节点的全局 ID。
     */
    public String getGid() {
        return this.mGid;
    }

    /**
     * 获取节点名称。
     */
    public String getName() {
        return this.mName;
    }

    /**
     * 获取远程节点的最后修改时间。
     */
    public long getLastModified() {
        return this.mLastModified;
    }

    /**
     * 获取节点的删除标记。
     */
    public boolean getDeleted() {
        return this.mDeleted;
    }

}

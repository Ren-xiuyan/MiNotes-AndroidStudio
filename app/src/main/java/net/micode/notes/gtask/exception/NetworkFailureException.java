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

package net.micode.notes.gtask.exception;

/**
 * 网络请求失败异常。在 Google Tasks 登录或请求过程中发生网络错误时抛出。
 */
public class NetworkFailureException extends Exception {
    private static final long serialVersionUID = 2107610287180234136L;

    /**
     * 构造 NetworkFailureException 对象，并初始化内部状态。
     */
    public NetworkFailureException() {
        super();
    }

    /**
     * 构造 NetworkFailureException 对象，并初始化内部状态。
     */
    public NetworkFailureException(String paramString) {
        super(paramString);
    }

    /**
     * 构造 NetworkFailureException 对象，并初始化内部状态。
     */
    public NetworkFailureException(String paramString, Throwable paramThrowable) {
        super(paramString, paramThrowable);
    }
}

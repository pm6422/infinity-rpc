/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.infinity.rpc.core.exception.impl;

import org.infinity.rpc.core.exception.RpcAbstractException;
import org.infinity.rpc.core.exception.RpcErrorConstants;

public class RpcBizException extends RpcAbstractException {
    private static final long serialVersionUID = -3491276058323309898L;

    public RpcBizException() {
        super(RpcErrorConstants.BIZ_DEFAULT_EXCEPTION);
    }

    public RpcBizException(RpcErrorMsg rpcErrorMsg) {
        super(rpcErrorMsg);
    }

    public RpcBizException(String message) {
        super(message, RpcErrorConstants.BIZ_DEFAULT_EXCEPTION);
    }

    public RpcBizException(String message, RpcErrorMsg rpcErrorMsg) {
        super(message, rpcErrorMsg);
    }

    public RpcBizException(String message, Throwable cause) {
        super(message, cause, RpcErrorConstants.BIZ_DEFAULT_EXCEPTION);
    }

    public RpcBizException(String message, Throwable cause, RpcErrorMsg rpcErrorMsg) {
        super(message, cause, rpcErrorMsg);
    }

    public RpcBizException(Throwable cause) {
        super(cause, RpcErrorConstants.BIZ_DEFAULT_EXCEPTION);
    }

    public RpcBizException(Throwable cause, RpcErrorMsg rpcErrorMsg) {
        super(cause, rpcErrorMsg);
    }
}

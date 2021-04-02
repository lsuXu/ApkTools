package com.company;

import java.io.InputStream;

/**
 * 脚本执行回调
 */
public interface IExecuteCallback {

    void onError(InputStream errorStream);

    void onInfo(InputStream infoStream);
}

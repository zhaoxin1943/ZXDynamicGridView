package com.zx.zxdynamicgridviewlib;

/**
 * Created by zhaoxin on 15/3/16.
 * QQ:343986392
 * https://github.com/zhaoxin1943
 */
public interface IZXDynamicGridViewAdapter {

    int getColumnCount();
    boolean canReorder(int position);
    void reorderItems(int originalPosition, int newPosition);
}

package com.zx.zxdynamicgridviewlib;

import android.view.View;

import java.util.List;

/**
 * Created by zhaoxin on 15/3/16.
 * QQ:343986392
 * https://github.com/zhaoxin1943
 */
public class ZXDynamicGridUtils {

    public static float getViewX(View view) {
        return Math.abs((view.getRight() - view.getLeft()) / 2);
    }

    public static float getViewY(View view) {
        return Math.abs((view.getBottom() - view.getTop()) / 2);
    }
    public static void reorder(List list, int indexFrom, int indexTwo) {
        Object obj = list.remove(indexFrom);
        list.add(indexTwo, obj);
    }
}

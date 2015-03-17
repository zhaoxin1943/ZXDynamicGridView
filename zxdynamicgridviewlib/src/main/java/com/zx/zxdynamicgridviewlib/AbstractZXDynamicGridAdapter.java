package com.zx.zxdynamicgridviewlib;

import android.widget.BaseAdapter;

import java.util.HashMap;
import java.util.List;

/**
 * Created by zhaoxin on 15/3/17.
 * QQ:343986392
 * https://github.com/zhaoxin1943
 */
public abstract class AbstractZXDynamicGridAdapter<T> extends BaseAdapter implements IZXDynamicGridViewAdapter {

    public static final int INVALID_ID = -1;
    private int nextStableId = 0;
    private HashMap<T, Integer> mIdMap = new HashMap<>();

    /**
     * 这个方法就是判断item的id是否稳定，如果有自己的id也就是true，那就是稳定，否则不稳定，则根据item位置来确定id
     * 因为会根据ID来得到view
     *
     * @return
     */
    @Override
    public final boolean hasStableIds() {
        return true;
    }

    protected void addStableId(T item) {
        mIdMap.put(item, nextStableId++);
    }

    protected void addAllStableId(List<T> items) {
        for (T item : items) {
            addStableId(item);
        }
    }

    @Override
    public long getItemId(int position) {
        if (position < 0 || position >= mIdMap.size()) {
            return INVALID_ID;
        }
        T item = (T) getItem(position);
        return mIdMap.get(item);
    }

    protected void clearStableIdMap() {
        mIdMap.clear();
    }

    protected void removeStableID(T item) {
        mIdMap.remove(item);
    }
}

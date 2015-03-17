package com.zx.zxdynamicgridviewlib;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhaoxin on 15/3/17.
 * QQ:343986392
 * https://github.com/zhaoxin1943
 */
public abstract class ZXBaseDynamicGridAdapter<T> extends AbstractZXDynamicGridAdapter<T> {

    private Context mContext;
    private ArrayList<T> mItems = new ArrayList<>();
    private int mColumnCount;

    protected ZXBaseDynamicGridAdapter(Context context, int columnCount) {
        this.mContext = context;
        this.mColumnCount = columnCount;
    }

    public ZXBaseDynamicGridAdapter(Context context, List<T> items, int columnCount) {
        mContext = context;
        mColumnCount = columnCount;
        init(items);
    }

    private void init(List<T> items) {
        addAllStableId(items);
        this.mItems.addAll(items);
    }

    private void set(List<T> items) {
        mItems.clear();
        init(items);
        notifyDataSetChanged();
    }

    private void clear() {
        clearStableIdMap();
        mItems.clear();
        notifyDataSetChanged();
    }

    public void add(T item) {
        addStableId(item);
        mItems.add(item);
        notifyDataSetChanged();
    }

    public void add(int position, T item) {
        addStableId(item);
        mItems.add(position, item);
        notifyDataSetChanged();
    }

    public void add(List<T> items) {
        addAllStableId(items);
        this.mItems.addAll(items);
        notifyDataSetChanged();
    }

    public void remove(T item) {
        mItems.remove(item);
        removeStableID(item);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public T getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public int getColumnCount() {
        return mColumnCount;
    }

    public void setColumnCount(int columnCount) {
        this.mColumnCount = columnCount;
        notifyDataSetChanged();
    }

    @Override
    public boolean canReorder(int position) {
        return true;
    }

    protected Context getContext() {
        return mContext;
    }

    @Override
    public void reorderItems(int originalPosition, int newPosition) {
        if (newPosition < getCount()) {
            ZXDynamicGridUtils.reorder(mItems, originalPosition, newPosition);
            notifyDataSetChanged();
        }
    }

}

package com.zx.zxdynamicgridview;

import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.zx.zxdynamicgridviewlib.IZXDynamicGridViewAdapter;
import com.zx.zxdynamicgridviewlib.ZXBaseDynamicGridAdapter;
import com.zx.zxdynamicgridviewlib.ZXDynamicGridView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends ActionBarActivity {

    private ZXDynamicGridView dynamic_grid_view;
    private Integer[] picIds = {R.mipmap.pic1, R.mipmap.pic2, R.mipmap.pic3, R.mipmap.pic4,
            R.mipmap.pic5, R.mipmap.pic6, R.mipmap.pic7, R.mipmap.pic8,
            R.mipmap.pic1, R.mipmap.pic2, R.mipmap.pic3, R.mipmap.pic4,
            R.mipmap.pic5, R.mipmap.pic6, R.mipmap.pic7, R.mipmap.pic8,
            R.mipmap.pic1, R.mipmap.pic2, R.mipmap.pic3, R.mipmap.pic4,
            R.mipmap.pic5, R.mipmap.pic6, R.mipmap.pic7, R.mipmap.pic8,
            R.mipmap.pic1, R.mipmap.pic2, R.mipmap.pic3, R.mipmap.pic4,
            R.mipmap.pic5, R.mipmap.pic6, R.mipmap.pic7, R.mipmap.pic8};
    private List<Integer> picIdList = Arrays.asList(picIds);
    private TestAdapter testAdapter;

    private int imageViewSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dynamic_grid_view = (ZXDynamicGridView) findViewById(R.id.dynamic_grid_view);
        dynamic_grid_view.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                dynamic_grid_view.startEditMode(position);
                return true;
            }
        });
        testAdapter = new TestAdapter(this, picIdList, 3);
        dynamic_grid_view.setAdapter(testAdapter);
        imageViewSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, getResources().getDisplayMetrics());
    }


    class TestAdapter extends ZXBaseDynamicGridAdapter<Integer> {


        public TestAdapter(Context context, List<Integer> items, int columnCount) {
            super(context, items, columnCount);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {  // if it's not recycled, initialize some attributes
                imageView = new ImageView(MainActivity.this);
                imageView.setLayoutParams(new GridView.LayoutParams(imageViewSize, imageViewSize));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setPadding(8, 8, 8, 8);
            } else {
                imageView = (ImageView) convertView;
            }

            imageView.setImageResource(getItem(position));
            return imageView;
        }

    }

}

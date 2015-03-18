package com.zx.zxdynamicgridview.example;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;


import com.zx.zxdynamicgridview.R;
import com.zx.zxdynamicgridviewlib.ZXDynamicGridView;

import java.util.ArrayList;
import java.util.Arrays;

public class GridActivity extends Activity {

    private static final String TAG = GridActivity.class.getName();

    private ZXDynamicGridView gridView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grid);
        gridView = (ZXDynamicGridView) findViewById(R.id.dynamic_grid);
        gridView.setAdapter(new CheeseDynamicAdapter(this,
                new ArrayList<>(Arrays.asList(Cheeses.sCheeseStrings)),
                3));
//        add callback to stop edit mode if needed
//        gridView.setOnDropListener(new DynamicGridView.OnDropListener()
//        {
//            @Override
//            public void onActionDrop()
//            {
//                gridView.stopEditMode();
//            }
//        });
        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                gridView.startEditMode(position);
                return true;
            }
        });

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(GridActivity.this, parent.getAdapter().getItem(position).toString(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

}

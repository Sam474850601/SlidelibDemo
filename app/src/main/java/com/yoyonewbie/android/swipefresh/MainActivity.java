package com.yoyonewbie.android.swipefresh;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

public class MainActivity extends AppCompatActivity {


    private ListView listView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(new MyAdapter(getApplicationContext()));
    }

    private static class MyAdapter extends BaseAdapter
    {
        private Context context;
        private MyAdapter(Context context)
        {
            this.context = context;
        }

        @Override
        public int getCount() {
            return 20;
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(null == convertView)
            {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_text, null);
            }
            return convertView;
        }
    }
}

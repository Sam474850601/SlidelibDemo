package com.yoyonewbie.android.swipefresh;

import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.yoyonewbie.android.lib.swipefreshlib.SwipeHideOrShowHeaderLayout;
import com.yoyonewbie.android.lib.swipefreshlib.SwipeItemDisappearLayout;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

       SwipeItemDisappearLayout swipeRefreshLayout = (SwipeItemDisappearLayout) findViewById(R.id.swipeDismissItem);
        swipeRefreshLayout.setRepeat(true);
        swipeRefreshLayout.setOnDisapperListener(new SwipeItemDisappearLayout.OnDisapperListener() {
            @Override
            public void onDisppeared() {

            }
        });
        SwipeHideOrShowHeaderLayout swipeHideOrShowHeaderLayout = (SwipeHideOrShowHeaderLayout) findViewById(R.id.swipeHideOrShowHeaderLayout);
        swipeHideOrShowHeaderLayout.setOnOperatalbeListener(new SwipeHideOrShowHeaderLayout.OnOperatalbeListener() {
            @Override
            public boolean enableShowHeader() {

                return true;
            }

            @Override
            public boolean enableHideHeader() {

                return true;
            }

            @Override
            public void startAnim(boolean isShow) {

            }

            @Override
            public void animRunning(boolean isShow) {

            }

            @Override
            public void endAnim(boolean isShow) {

            }
        });

    }
}

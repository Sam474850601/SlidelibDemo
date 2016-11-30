package com.yoyonewbie.android.swipefreshlib;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * The list slide option disappears from the container
 * @author Sam
 */
public class SwipeItemDisappearLayout extends ViewGroup {
    private final static String LOG_TAG = SwipeItemDisappearLayout.class.getSimpleName();
    int mWith;
    int mHeight;
    public SwipeItemDisappearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    View mTarget;
    int paddingLeft;
    int paddingRight;
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWith = getMeasuredWidth();
        mHeight = getMeasuredHeight();
        paddingLeft = getPaddingLeft();
        paddingRight = getPaddingRight();

        if(1 != getChildCount())
            throw new  RuntimeException("There can be only one child view or layout!");
        mTarget = getChildAt(0);
        mTarget.measure(MeasureSpec.makeMeasureSpec(
                getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(
                0, MeasureSpec.UNSPECIFIED));
        mHeight = Math.min(mHeight, mTarget.getMeasuredHeight()+getPaddingTop()+getPaddingBottom());
        setMeasuredDimension(mWith, mHeight);
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int childLeft = getPaddingLeft();
        final int childTop = getPaddingTop();
        final int childWidth = mWith - getPaddingLeft() - getPaddingRight();
        final int childHeight = mTarget.getMeasuredHeight();
        mTarget.layout(childLeft-scrollX, childTop, childLeft + childWidth-scrollX,childTop +childHeight );
    }




    private int scrollX;
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction())
        {
            case MotionEvent.ACTION_DOWN:
            {
                return true;
            }
            case MotionEvent.ACTION_MOVE:
            {

                if(INVALID_POINTER == mActivePointerId)
                {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
                    return false;
                }
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }
                scrollX = (int) ( downX -ev.getX(pointerIndex));
                if(scrollX<0)
                    return false;
                return true;
            }
            case MotionEvent.ACTION_UP:
            {
                Log.e(LOG_TAG, "ACTION_UP ");

            }break;
            case MotionEvent.ACTION_CANCEL:
            {
                mActivePointerId = INVALID_POINTER;
            }break;

        }
        return super.onTouchEvent(ev);
    }

    int downX = 0;

    int mActivePointerId;

    int pointerIndex;


     final static int INVALID_POINTER = -1;


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        switch (ev.getAction())
        {
            case MotionEvent.ACTION_DOWN:
            {
                scrollX = 0;
                Log.e(LOG_TAG, "onInterceptTouchEvent ACTION_DOWN");
                //SwipeRefreshLayout
                mActivePointerId = ev.getPointerId(0);
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                downX = (int) ev.getX(pointerIndex);
                getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
            case MotionEvent.ACTION_MOVE:
            {

                if(INVALID_POINTER == mActivePointerId)
                {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
                    return false;
                }
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                boolean allowToMove =  downX - (int) ev.getX(pointerIndex)>0;
                Log.e(LOG_TAG, "onInterceptTouchEvent ACTION_MOVE "+allowToMove);
                return allowToMove;
            }
            case MotionEvent.ACTION_UP:
            {
            }break;
            case MotionEvent.ACTION_CANCEL:
            {
                mActivePointerId = INVALID_POINTER;
            }break;
        }
        return super.onInterceptTouchEvent(ev);
    }



}

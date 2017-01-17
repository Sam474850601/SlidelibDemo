package com.yoyonewbie.android.lib.swipefreshlib;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Scroller;

/**
 * A layout that can be removed  when user swipe-left to operation
 * @author Sam
 */

public class SwipeItemDisappearLayout extends ViewGroup {

    Scroller mScroller;
    private   final  static int INVALID_POINTER = -1;
    int mActivePointerId;
    int pointerIndex;
    float  mInitialDownX;
    float moveValue;
    private  OnDisapperListener onDisapperListener;
    private OnRecoveriedListener onRecoveriedListener;
    private  final static int STATE_NORMAL = 1;
    private  final static int STATE_DISAPPEAR = 0x2;
    private  final static int STATE_DISAPPEAR_COMPLETED = 0x4;
    private  final static int STATE_RECOVER = 3;
    private int  state = STATE_NORMAL;
    private ValueAnimator disappearAnimator;
    int mWith;

    int mHeight;

    int paddingLeft;
    int paddingRight;
    boolean repeat = true;

    View mTargetView;
    public SwipeItemDisappearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mScroller = new Scroller(context);
    }





    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWith = getMeasuredWidth();
        paddingLeft = getPaddingLeft();
        paddingRight = getPaddingRight();

        if(1 != getChildCount())
            throw new  RuntimeException("There can be only one child view or layout!");
        mTargetView = getChildAt(0);
        mTargetView.measure(MeasureSpec.makeMeasureSpec(
                getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(
                0,  MeasureSpec.UNSPECIFIED));
        if(STATE_NORMAL == state)
        {
            mHeight = mTargetView.getMeasuredHeight()+getPaddingTop()+getPaddingBottom();
        }
        setMeasuredDimension(mWith, mHeight);
    }

    @Override
    protected void onLayout(boolean b, int i, int i1, int i2, int i3) {
        final int childLeft = getPaddingLeft();
        final int childTop = getPaddingTop();
        final int childWidth = mWith - getPaddingLeft() - getPaddingRight();
        final int childHeight = mTargetView.getMeasuredHeight();
        mTargetView.layout(childLeft, childTop, childLeft + childWidth,childTop +childHeight );

    }

    final ValueAnimator.AnimatorUpdateListener   updateListener =   new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            int value = (Integer) valueAnimator.getAnimatedValue();
            _setHeight(value);
        }
    };


    private void _setHeight(int height)
    {
        mHeight = height;
        requestLayout();
    }

    final  ValueAnimator.AnimatorListener animatorListener = new ValueAnimator.AnimatorListener()
    {

        @Override
        public void onAnimationStart(Animator animator) {

        }

        @Override
        public void onAnimationEnd(Animator animator) {
            if(null != onDisapperListener)
                onDisapperListener.onDisppeared();
            state |= STATE_DISAPPEAR_COMPLETED;
            if(repeat)
            {
                recover();
            }
            moveValue  = 0;
        }

        @Override
        public void onAnimationCancel(Animator animator) {

        }

        @Override
        public void onAnimationRepeat(Animator animator) {

        }
    };


    private void _disappear()
    {
        if(isRunning())
            return;
        disappearAnimator = ValueAnimator.ofInt(mTargetView.getMeasuredHeight(), 0);
        disappearAnimator.setDuration(300);
        disappearAnimator.addUpdateListener(updateListener);
        disappearAnimator.addListener(animatorListener);
        disappearAnimator.start();
    }

    public boolean isRunning()
    {
        return null != disappearAnimator && disappearAnimator.isRunning();
    }



    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if(isRunning())
            return false;
        switch (ev.getAction())
        {
            case MotionEvent.ACTION_DOWN:
            {
                mActivePointerId = ev.getPointerId(0);
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                mInitialDownX = ev.getX(pointerIndex);
                moveValue = 0;
                super.onTouchEvent(ev);
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            }
            case MotionEvent.ACTION_MOVE:
            {
                if(STATE_DISAPPEAR == state)
                    return false ;
                if(INVALID_POINTER == mActivePointerId)
                    return false;
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e("SwipeDisappearLayout", "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }
                float currentX = ev.getX(pointerIndex);
                float moveValue = currentX - mInitialDownX;
                if(moveValue<0)
                {
                    moveValue = moveValue*0.4f;
                    if(this.moveValue>moveValue)
                    {
                        this.moveValue = moveValue;
                        if(!mScroller.isFinished())
                            mScroller.forceFinished(true);
                        scrollTo((int) -moveValue, 0);
                    }
                }
                return true;
            }
            case MotionEvent.ACTION_CANCEL:
                mActivePointerId = INVALID_POINTER;
            case MotionEvent.ACTION_UP:
            {
                if(isRunning())
                    break;
                Log.e("SwipeDisappearLayout", "ACTION_UP");

                Log.e("SwipeDisappearLayout", "moveValue:"+moveValue);
                if(moveValue<-getMeasuredWidth()/10f)
                {

                    Log.e("SwipeDisappearLayout", "disappear");
                    state = STATE_DISAPPEAR;
                    if(null != onDisapperListener)
                        onDisapperListener.onStarted();
                    mScroller.startScroll(getScrollX(),0 , (int) (getMeasuredWidth()-Math.abs(moveValue)), 0, 1500);
                    invalidate();
                }
                else if(moveValue<0)
                {
                    int recoverValue = (int) moveValue;
                    state = STATE_RECOVER;
                    Log.e("SwipeDisappearLayout", "recoverValue:"+recoverValue);
                    mScroller.startScroll(getScrollX(), 0 ,recoverValue , 0, 1000);
                    invalidate();
                }
                getParent().requestDisallowInterceptTouchEvent(false);
            }break;

        }
        return super.onTouchEvent(ev);
    }


    public void recover()
    {
        scrollTo(0, 0);
        state = STATE_NORMAL;
        requestLayout();
        if(null != onRecoveriedListener)
            onRecoveriedListener.onRecoveried();
    }



    @Override
    public void computeScroll() {
        if(mScroller.computeScrollOffset())
        {
            scrollTo(mScroller.getCurrX(), 0);
            invalidate();
        }
        else if(STATE_RECOVER == state)
        {
            if(null != onRecoveriedListener)
                onRecoveriedListener.onRecoveried();
            state = STATE_NORMAL;
            moveValue= 0;

        }
        else if(STATE_DISAPPEAR == state && (0 ==( state& STATE_DISAPPEAR_COMPLETED)))
        {
            _disappear();
        }
    }

    public void disappear()
    {

        if(0 != ( state& STATE_DISAPPEAR_COMPLETED))
            return;
        state = STATE_DISAPPEAR;
        if(null != onDisapperListener)
            onDisapperListener.onStarted();
        mScroller.startScroll(getScrollX(),0 , getMeasuredWidth(), 0, 1500);
        invalidate();
    }


    public interface OnDisapperListener
    {
        void onStarted();
        void onDisppeared();
    }


    public interface OnRecoveriedListener
    {
        void onRecoveried();
    }

    public void setOnDisapperListener(OnDisapperListener onDisapperListener) {
        this.onDisapperListener = onDisapperListener;
    }


    public void setOnRecoveriedListener(OnRecoveriedListener onRecoveriedListener) {
        this.onRecoveriedListener = onRecoveriedListener;
    }

    public void setRepeat(boolean repeat)
    {
        this.repeat = repeat;
    }
}

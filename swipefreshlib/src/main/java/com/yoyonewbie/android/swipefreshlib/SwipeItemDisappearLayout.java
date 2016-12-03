package com.yoyonewbie.android.swipefreshlib;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;


import java.lang.ref.WeakReference;

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
    boolean isFirstonMeasure= true;
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWith = getMeasuredWidth();
        paddingLeft = getPaddingLeft();
        paddingRight = getPaddingRight();

        if(1 != getChildCount())
            throw new  RuntimeException("There can be only one child view or layout!");
        mTarget = getChildAt(0);
        mTarget.measure(MeasureSpec.makeMeasureSpec(
                getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(
                0,  MeasureSpec.UNSPECIFIED));
        if(isFirstonMeasure)
        {
            isFirstonMeasure = false;
            mHeight = mTarget.getMeasuredHeight()+getPaddingTop()+getPaddingBottom();
        }
        setMeasuredDimension(mWith, mHeight);
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int childLeft = getPaddingLeft();
        final int childTop = getPaddingTop();
        final int childWidth = mWith - getPaddingLeft() - getPaddingRight();
        final int childHeight = mTarget.getMeasuredHeight();
        mTarget.layout(childLeft+scrollX, childTop, childLeft + childWidth+scrollX,childTop +childHeight );
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
                scrollX = (int) ( ev.getX(pointerIndex) -downX);
                if(scrollX>0)
                {
                    getParent().requestDisallowInterceptTouchEvent(false);
                    return false;
                }
                requestLayout();
                return true;
            }
            case MotionEvent.ACTION_CANCEL:
                mActivePointerId = INVALID_POINTER;
            case MotionEvent.ACTION_UP:
            {
                if(scrollX<0)
                {
                    int  type = scrollX>-mWith/8?TYPE_RECOVERY:TYPE_DISAPPEAR;
                    _startScrollXAnim(type, scrollX );
                }
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
            case MotionEvent.ACTION_CANCEL:
                mActivePointerId = INVALID_POINTER;
            case MotionEvent.ACTION_UP:
            {

            }break;

        }
        return super.onInterceptTouchEvent(ev);
    }

    private void _scrollBy(int x)
    {
        this.scrollX = x;
        requestLayout();
    }


    private ValueAnimator scrollAnimator;


    final static int TYPE_DISAPPEAR = 1;

    final static int TYPE_RECOVERY = 2;


    private void _startScrollXAnim(int type, int scrollX)
    {
        if(_isAnimRunninng())
            return;
        WeakReference<SwipeItemDisappearLayout> swipeItemDisappearLayoutWeakReference =   new WeakReference<SwipeItemDisappearLayout>(this);
        if(TYPE_RECOVERY == type)
        {
            scrollAnimator = ValueAnimator.ofInt(scrollX, 0);
            scrollAnimator.setInterpolator(new DecelerateInterpolator(2f));
            scrollAnimator.setDuration(1500);
        }
        else if  (TYPE_DISAPPEAR == type)
        {
            scrollAnimator = ValueAnimator.ofInt(scrollX, -mWith);
            scrollAnimator.setInterpolator(new AccelerateInterpolator());
            scrollAnimator.setDuration(300);
        }
        if(null  == scrollAnimator)
            return;
        scrollAnimator.addListener(new ScrollAnimListener(type, swipeItemDisappearLayoutWeakReference));
        scrollAnimator.addUpdateListener(new ScrollValueAnimatorUpdateListener(swipeItemDisappearLayoutWeakReference));
        scrollAnimator.start();
    }


    private static  class ScrollValueAnimatorUpdateListener  implements ValueAnimator.AnimatorUpdateListener
    {
        WeakReference<SwipeItemDisappearLayout> swipeItemDisappearLayoutWeakReference;
        private  ScrollValueAnimatorUpdateListener(WeakReference<SwipeItemDisappearLayout> swipeItemDisappearLayoutWeakReference)
        {
            this.swipeItemDisappearLayoutWeakReference = swipeItemDisappearLayoutWeakReference;
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            if(null != swipeItemDisappearLayoutWeakReference)
            {
                SwipeItemDisappearLayout swipeItemDisappearLayout =  swipeItemDisappearLayoutWeakReference.get();
                if(null != swipeItemDisappearLayout)
                {
                    swipeItemDisappearLayout._scrollBy((Integer) animation.getAnimatedValue());
                }
            }
        }
    }

    private static  class  DisappearValueAnimatorUpdateListener implements ValueAnimator.AnimatorUpdateListener
    {
        WeakReference<SwipeItemDisappearLayout> swipeItemDisappearLayoutWeakReference;
        private DisappearValueAnimatorUpdateListener (WeakReference<SwipeItemDisappearLayout> swipeItemDisappearLayoutWeakReference)
        {
            this.swipeItemDisappearLayoutWeakReference = swipeItemDisappearLayoutWeakReference;
        }
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            if(null != swipeItemDisappearLayoutWeakReference)
            {
                SwipeItemDisappearLayout layout =  swipeItemDisappearLayoutWeakReference.get();
                if(null != layout)
                {
                    layout._disappearHeight((Integer) animation.getAnimatedValue());
                }
            }

        }
    }

    private void _disappearHeight(int height)
    {
        mHeight = height;
        requestLayout();

    }




    //if anim is running
    private boolean _isAnimRunninng()
    {
        return null != scrollAnimator && scrollAnimator.isRunning();
    }


    private class ScrollAnimListener implements Animator.AnimatorListener {
        WeakReference<SwipeItemDisappearLayout> swipeItemDisappearLayoutWeakReferenc;
        int type;
        public ScrollAnimListener(int type, WeakReference<SwipeItemDisappearLayout> swipeItemDisappearLayoutWeakReference) {
            this.swipeItemDisappearLayoutWeakReferenc = swipeItemDisappearLayoutWeakReference;
            this.type =type;
        }

        @Override
        public void onAnimationStart(Animator animation) {

        }

        private ValueAnimator disappearValueAnimato;
        @Override
        public void onAnimationEnd(Animator animation) {
            if(TYPE_RECOVERY == type)
            {
                if(null != swipeItemDisappearLayoutWeakReferenc)
                    swipeItemDisappearLayoutWeakReferenc.clear();
            }
            else  if(TYPE_DISAPPEAR == type)
            {
                if(null != swipeItemDisappearLayoutWeakReferenc)
                {
                    SwipeItemDisappearLayout layout =  swipeItemDisappearLayoutWeakReferenc.get();
                    if(null != layout)
                    {
                        Log.e(LOG_TAG, "ScrollAnimListener onAnimationEnd TYPE_DISAPPEAR " );
                        disappearValueAnimato = ValueAnimator.ofInt(layout.mHeight, 0);
                        disappearValueAnimato.setDuration(500);
                        disappearValueAnimato.addUpdateListener(new DisappearValueAnimatorUpdateListener(swipeItemDisappearLayoutWeakReferenc));
                        disappearValueAnimato.addListener(new DisappearAnimatorListener(swipeItemDisappearLayoutWeakReferenc));
                        disappearValueAnimato.start();
                    }
                }
            }

        }



        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    }


    private static class  DisappearAnimatorListener implements Animator.AnimatorListener
    {
        WeakReference<SwipeItemDisappearLayout> swipeItemDisappearLayoutWeakReferenc;
        public DisappearAnimatorListener(WeakReference<SwipeItemDisappearLayout> swipeItemDisappearLayoutWeakReferenc) {
            this.swipeItemDisappearLayoutWeakReferenc = swipeItemDisappearLayoutWeakReferenc;
        }

        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if(null != swipeItemDisappearLayoutWeakReferenc)
            {
                SwipeItemDisappearLayout swipeItemDisappearLayout =   swipeItemDisappearLayoutWeakReferenc.get();
                if(null != swipeItemDisappearLayout)
                {
                    OnDismissLinstener onDismissLinstener = swipeItemDisappearLayout.onDismissLinstener;
                    if(null != onDismissLinstener)
                    {
                        onDismissLinstener.onDismiss();
                    }
                    swipeItemDisappearLayoutWeakReferenc.clear();
                }
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    }

    public void disappear()
    {
        if(!_isAnimRunninng())
        {
            _startScrollXAnim(TYPE_DISAPPEAR, 0 );
        }
    }

    private OnDismissLinstener onDismissLinstener;

    public void setOnDismissLinstener(OnDismissLinstener onDismissLinstener) {
        this.onDismissLinstener = onDismissLinstener;
    }

    public interface OnDismissLinstener
    {
        void onDismiss();
    }

}
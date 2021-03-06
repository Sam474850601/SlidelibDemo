package com.yoyonewbie.android.lib.swipefreshlib;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Scroller;

/**
 * A layout that can be removed  when user swipe-left to operation
 *
 * @author Sam
 */

public class SwipeItemDisappearLayout extends ViewGroup {

    Scroller mScroller;
    private final static int INVALID_POINTER = -1;
    int mActivePointerId;
    int pointerIndex;
    float mInitialDownX;
    float moveValue;
    private OnDisapperListener onDisapperListener;
    private OnRecoveriedListener onRecoveriedListener;
    private ValueAnimator scroollOutTheLayoutValueAnimator;
    private final static int STATE_NORMAL = 1;
    private final static int STATE_DISAPPEAR = 0x2;
    private final static int STATE_DISAPPEAR_COMPLETED = 0x4;
    private final static int STATE_RECOVER = 3;
    private int state = STATE_NORMAL;
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

        if (1 != getChildCount())
            throw new RuntimeException("There can be only one child view or layout!");
        mTargetView = getChildAt(0);
        mTargetView.measure(MeasureSpec.makeMeasureSpec(
                getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(
                0, MeasureSpec.UNSPECIFIED));
        if (STATE_NORMAL == state) {
            mHeight = mTargetView.getMeasuredHeight() + getPaddingTop() + getPaddingBottom();
        }
        setMeasuredDimension(mWith, mHeight);
    }

    private int scrollerLeft;

    @Override
    protected void onLayout(boolean b, int i, int i1, int i2, int i3) {
        final int childLeft = getPaddingLeft();
        final int childTop = getPaddingTop();
        final int childWidth = mWith - getPaddingLeft() - getPaddingRight();
        final int childHeight = mTargetView.getMeasuredHeight();
        mTargetView.layout(childLeft + scrollerLeft, childTop, scrollerLeft + childLeft + childWidth, childTop + childHeight);

    }

    Animator.AnimatorListener scroollOutTheLayoutAnimatorListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {
            scrollTo(0, 0);
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            _disappear();
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    };


    ValueAnimator.AnimatorUpdateListener scroollOutTheLayoutAnimatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            scrollerLeft = (int) animation.getAnimatedValue();
            requestLayout();
        }
    };

    private int scrollOutDuration = 500;

    public void setScrollOutDuration(int duration) {
        this.scrollOutDuration = duration;
    }

    private int disappearOutDuration = 200;

    public void setDisappearOutDuration(int duration) {
        this.disappearOutDuration = duration;
    }


    private void _scrollOutTheLayout(int scrollValue) {
        scroollOutTheLayoutValueAnimator = ValueAnimator.ofInt(scrollValue, -getMeasuredWidth());
        scroollOutTheLayoutValueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        scroollOutTheLayoutValueAnimator.setDuration(scrollOutDuration);
        scroollOutTheLayoutValueAnimator.addUpdateListener(scroollOutTheLayoutAnimatorUpdateListener);
        scroollOutTheLayoutValueAnimator.addListener(scroollOutTheLayoutAnimatorListener);
        scroollOutTheLayoutValueAnimator.start();
    }


    final ValueAnimator.AnimatorUpdateListener updateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            int value = (Integer) valueAnimator.getAnimatedValue();
            _setHeight(value);
        }
    };


    private void _setHeight(int height) {
        mHeight = height;
        requestLayout();
    }

    final ValueAnimator.AnimatorListener animatorListener = new ValueAnimator.AnimatorListener() {

        @Override
        public void onAnimationStart(Animator animator) {
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            if (null != onDisapperListener)
                onDisapperListener.onDisppeared();
            state |= STATE_DISAPPEAR_COMPLETED;
            if (repeat) {
                recover();
            }
            moveValue = 0;
        }

        @Override
        public void onAnimationCancel(Animator animator) {

        }

        @Override
        public void onAnimationRepeat(Animator animator) {

        }
    };


    private void _disappear() {
        if (isRunning())
            return;
        if (null == disappearAnimator) {
            disappearAnimator = ValueAnimator.ofInt(mTargetView.getMeasuredHeight(), 0);
            disappearAnimator.setDuration(disappearOutDuration);
            disappearAnimator.addUpdateListener(updateListener);
            disappearAnimator.addListener(animatorListener);
            disappearAnimator.setInterpolator(new LinearInterpolator());
        }
        disappearAnimator.start();
    }

    public boolean isRunning() {
        return null != disappearAnimator && disappearAnimator.isRunning() || (null != scroollOutTheLayoutValueAnimator && scroollOutTheLayoutValueAnimator.isRunning());
    }


    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (isRunning())
            return false;
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN: {
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
            case MotionEvent.ACTION_MOVE: {
                if (STATE_DISAPPEAR == state)
                    return false;
                if (INVALID_POINTER == mActivePointerId)
                    return false;
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e("SwipeDisappearLayout", "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }
                float currentX = ev.getX(pointerIndex);
                float moveValue = currentX - mInitialDownX;
                if (moveValue < 0) {
                    moveValue = moveValue * 0.4f;
                    if (this.moveValue > moveValue) {
                        this.moveValue = moveValue;
                        if (!mScroller.isFinished())
                            mScroller.forceFinished(true);
                        scrollTo((int) -moveValue, 0);
                    }
                }
                return true;
            }
            case MotionEvent.ACTION_CANCEL:
                mActivePointerId = INVALID_POINTER;
            case MotionEvent.ACTION_UP: {
                if (isRunning())
                    break;
                if (moveValue < -getMeasuredWidth() / 8f) {

                    state = STATE_DISAPPEAR;
                    if (null != onDisapperListener)
                        onDisapperListener.onStarted();
//                    mScroller.startScroll(getScrollX(), 0, (int) (getMeasuredWidth() - Math.abs(moveValue)), 0, 1300);
//                    invalidate();
                    _scrollOutTheLayout((int) moveValue);
                } else if (moveValue < 0) {
                    int recoverValue = (int) moveValue;
                    state = STATE_RECOVER;
                    mScroller.startScroll(getScrollX(), 0, recoverValue, 0, 1200);
                    invalidate();
                }
                try {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                catch (Exception ex)
                {

                }

            }
            break;

        }
        return super.onTouchEvent(ev);
    }


    public void recover() {
        state = STATE_NORMAL;
        scrollerLeft = 0;
        requestLayout();
        scrollTo(0, 0);
        if (null != onRecoveriedListener)
            onRecoveriedListener.onRecoveried();
    }


    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), 0);
            invalidate();
        }

        if (mScroller.isFinished()) {
            if (STATE_RECOVER == state) {
                if (null != onRecoveriedListener)
                    onRecoveriedListener.onRecoveried();
                state = STATE_NORMAL;
                moveValue = 0;

            } else if (STATE_DISAPPEAR == state && (0 == (state & STATE_DISAPPEAR_COMPLETED))) {
                _disappear();
            }
        }


    }

    public void disappear() {
        if (isRunning())
            return;
        if (0 != (state & STATE_DISAPPEAR_COMPLETED))
            return;
        state = STATE_DISAPPEAR;
        if (null != onDisapperListener)
            onDisapperListener.onStarted();
//        mScroller.startScroll(getScrollX(), 0, getMeasuredWidth(), 0, 1300);
//        invalidate();
        _scrollOutTheLayout((int) moveValue);
    }


    public interface OnDisapperListener {
        void onStarted();

        void onDisppeared();
    }


    public interface OnRecoveriedListener {
        void onRecoveried();
    }

    public void setOnDisapperListener(OnDisapperListener onDisapperListener) {
        this.onDisapperListener = onDisapperListener;
    }


    public void setOnRecoveriedListener(OnRecoveriedListener onRecoveriedListener) {
        this.onRecoveriedListener = onRecoveriedListener;
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }


}

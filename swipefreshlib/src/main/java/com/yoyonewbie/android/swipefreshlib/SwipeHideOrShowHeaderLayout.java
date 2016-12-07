package com.yoyonewbie.android.swipefreshlib;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;


/**
 * Swipe to hide or show the head layout
 *
 * @author Sam
 */

public class SwipeHideOrShowHeaderLayout extends ViewGroup {

    int mWith;
    int mHeight;
    View headerView;
    View convertView;
    int headerViewHeight;

    Rect allowSwpieRect = new Rect();


    final static int VERTICAL_SWIPE_SIZE = 100;

    final static int HERTICAL_SWIPE_SIZE = 100;

    int suspendedAreaHeight = 0;
    int suspendedAreaId;

    final static String LOG_TAG = SwipeHideOrShowHeaderLayout.class.getSimpleName();

    public boolean isShowing() {
        return Math.abs(headerView.getTop()) < suspendedAreaHeight;
    }

    public interface OnOperatalbeListener {
        boolean enableShowHeader();

        boolean enableHideHeader();

        void startAnim();

        void animRunning();

        void endAnim();
    }

    private OnOperatalbeListener onOperatalbeListener;

    public void setOnOperatalbeListener(OnOperatalbeListener onOperatalbeListener) {
        this.onOperatalbeListener = onOperatalbeListener;
    }

    public SwipeHideOrShowHeaderLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SwipeHideOrShowHeaderLayout);
        try {
            suspendedAreaId = typedArray.getResourceId(R.styleable.SwipeHideOrShowHeaderLayout_suspendedArea, -1);
        } finally {
            typedArray.recycle();
        }
    }


    @Override
    public boolean onInterceptHoverEvent(MotionEvent event) {
        return _isAnimRunning();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);


        mWith = getMeasuredWidth();
        mHeight = getMeasuredHeight();
        allowSwpieRect.set(-HERTICAL_SWIPE_SIZE, VERTICAL_SWIPE_SIZE, HERTICAL_SWIPE_SIZE, mHeight - VERTICAL_SWIPE_SIZE);
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();

        if (2 != getChildCount())
            throw new RuntimeException("There must be 2 childViews(layout)!");
        headerView = getChildAt(0);
        convertView = getChildAt(1);
        if (headerView instanceof ViewGroup && suspendedAreaId > 0) {
            View suspendedView = headerView.findViewById(suspendedAreaId);
            suspendedAreaHeight = suspendedView.getMeasuredHeight();
        }
        int childViewWidthMeasureSpec = MeasureSpec.makeMeasureSpec(mWith - paddingLeft - paddingRight, MeasureSpec.EXACTLY);
        int childViewHeightMeasureSpec = MeasureSpec.makeMeasureSpec(mHeight - paddingTop - paddingBottom, MeasureSpec.EXACTLY);
        measureChild(headerView, childViewWidthMeasureSpec, childViewHeightMeasureSpec);
        measureChild(convertView, childViewWidthMeasureSpec, childViewHeightMeasureSpec);
        int convertHeight = convertView.getMeasuredHeight();
        headerViewHeight = headerView.getMeasuredHeight();
        setMeasuredDimension(mWith, paddingTop + paddingBottom + headerViewHeight + convertHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();
        int headerShowHeight = headerViewHeight + paddingTop + scrollY;
        headerView.layout(paddingLeft, paddingTop + scrollY, headerView.getMeasuredWidth() - paddingRight, headerShowHeight);
        convertView.layout(paddingLeft, headerShowHeight, convertView.getMeasuredWidth() - paddingRight, mHeight - paddingBottom);
    }


    final static int INVALID_POINTER = -1;

    int scrollY;
    int mActivePointerId;
    int pointerIndex;
    int downY;

    int FLAG_PROCESSD = 0X2;
    int FLAG_FIRST_PROCESSD = 0X4;
    int FLAG_HIDE = 0X8;
    int FLAG_SHOW = 0X10;
    int flag = 0;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        return super.onInterceptTouchEvent(ev);
    }

    private int downX;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (_isAnimRunning()) {
            return false;
        }

        try {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    if (0 != (FLAG_PROCESSD & flag))
                        flag &= ~FLAG_PROCESSD;
                    scrollY = 0;
                    Log.e(LOG_TAG, "dispatchTouchEvent ACTION_DOWN");
                    //SwipeRefreshLayout
                    mActivePointerId = ev.getPointerId(0);
                    pointerIndex = ev.findPointerIndex(mActivePointerId);
                    downY = (int) ev.getY(pointerIndex);
                    downX = (int) ev.getX(mActivePointerId);
                    getParent().requestDisallowInterceptTouchEvent(true);
                    super.dispatchTouchEvent(ev);
                    return true;
                }
                case MotionEvent.ACTION_MOVE: {

                    if (INVALID_POINTER == mActivePointerId) {
                        Log.e(LOG_TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
                        getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                    }
                    pointerIndex = ev.findPointerIndex(mActivePointerId);
                    if (pointerIndex < 0) {
                        getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                    }
                    int moveY = downY - (int) ev.getY(pointerIndex);
                    int moveX = downX - (int) ev.getX(pointerIndex);
                    boolean isSwipeDown = moveY < 0;
                    Log.e(LOG_TAG, "dispatchTouchEvent ACTION_MOVE isSwipeUp " + isSwipeDown);
                    if (0 == (flag & FLAG_PROCESSD) && !_isAnimRunning() && allowSwpieRect.contains(Math.abs(moveX), Math.abs(moveY))) {
                        boolean isShowing = isShowing();
                        Log.e(LOG_TAG, "dispatchTouchEvent ACTION_MOVE isShowing " + isShowing);
                        if (!isSwipeDown && _enableHideHeaderView() && isShowing) {
                            Log.e(LOG_TAG, "_enableHideHeaderView ACTION_MOVE");
                            flag |= FLAG_PROCESSD;
                            _hideHeader();
                        } else if (allowSwpieRect.contains(Math.abs(moveX), Math.abs(moveY)) && isSwipeDown && _enableShowHeaderView() && !isShowing) {
                            Log.e(LOG_TAG, "_enableShowHeaderView ACTION_MOVE");
                            flag |= FLAG_PROCESSD;
                            _showHeader();
                        }

                    }
                    super.dispatchTouchEvent(ev);
                    return true;
                }
                case MotionEvent.ACTION_UP: {
                    Log.e(LOG_TAG, "dispatchTouchEvent ACTION_UP isShowing " );
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                break;
                case MotionEvent.ACTION_CANCEL: {
                    getParent().requestDisallowInterceptTouchEvent(false);
                    mActivePointerId = INVALID_POINTER;
                }
                break;
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return super.dispatchTouchEvent(ev);
    }

    private void _hideHeader() {
        _showAnim(false);
    }


    private boolean _enableShowHeaderView() {
        if (null == onOperatalbeListener)
            return true;
        return onOperatalbeListener.enableShowHeader();
    }


    private boolean _enableHideHeaderView() {
        if (null == onOperatalbeListener)
            return true;
        return onOperatalbeListener.enableHideHeader();
    }

    ValueAnimator valueAnimator;


    private boolean _isAnimRunning() {
        return null != valueAnimator && valueAnimator.isRunning();
    }


    ValueAnimator.AnimatorUpdateListener updateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            scrollY = (int) animation.getAnimatedValue();
            requestLayout();
            if (null != onOperatalbeListener)
                onOperatalbeListener.animRunning();
        }
    };


    private void _showHeader() {
        _showAnim(true);
    }

    private Interpolator showHeaderInterpolator;

    public void setShowHeaderViewInterpolator(Interpolator showHeaderInterpolator)
    {
        this.showHeaderInterpolator = showHeaderInterpolator;
    }

    private Interpolator hideHeaderInterpolator;

    public void setHideHeaderViewInterpolator(Interpolator hideHeaderInterpolator)
    {
        this.hideHeaderInterpolator = hideHeaderInterpolator;
    }

    private long durationTime=0;
    public void setDuration(long time)
    {
        this.durationTime = time;
    }

    private void _showAnim(final boolean isshowAnim) {
        if (_isAnimRunning())
            valueAnimator.cancel();
        valueAnimator = isshowAnim ? ValueAnimator.ofInt(-headerViewHeight + suspendedAreaHeight, 0) : ValueAnimator.ofInt(0, -headerViewHeight + suspendedAreaHeight);
        valueAnimator.addUpdateListener(updateListener);
        valueAnimator.setDuration(0== durationTime ?500:durationTime);
        if(isshowAnim)
            valueAnimator.setInterpolator(null == showHeaderInterpolator?new DecelerateInterpolator():showHeaderInterpolator);
        else
            valueAnimator.setInterpolator(null == hideHeaderInterpolator?new LinearInterpolator():hideHeaderInterpolator);
        valueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (null != onOperatalbeListener)
                    onOperatalbeListener.startAnim();
            }

            @Override
            public void onAnimationEnd(Animator animation) {

                requestLayout();
                if (null != onOperatalbeListener)
                    onOperatalbeListener.endAnim();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        valueAnimator.start();
    }


}

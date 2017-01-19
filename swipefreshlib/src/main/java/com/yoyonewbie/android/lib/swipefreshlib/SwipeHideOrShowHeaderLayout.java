package com.yoyonewbie.android.lib.swipefreshlib;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import com.yoyonewbie.android.swipefreshlib.R;


/**
 * Swipe to hide or show the head layout
 *
 * @author Sam
 */

public class SwipeHideOrShowHeaderLayout extends ViewGroup {

    int mWith;
    int mHeight;
    View headerView;
    View mTarget;
    int headerViewHeight;
    int hideContentHeight;

    Rect allowSwpieRect = new Rect();


    //do nothing rect
    Rect donothingRect = new Rect();

    Rect notAllowSwpieLeftRect = new Rect();

    Rect notAllowSwpieRightRect = new Rect();


    final static int VERTICAL_SWIPE_SIZE = 50;

    final static int HERTICAL_SWIPE_SIZE = 50;

    int suspendedAreaHeight = 0;
    int suspendedAreaId;

    final static String LOG_TAG = SwipeHideOrShowHeaderLayout.class.getSimpleName();

    public boolean isShowing() {
        return Math.abs(scrollY) == 0;
    }

    public interface OnOperatalbeListener {
        boolean enableShowHeader();

        boolean enableHideHeader();

        void startAnim(boolean isShow);

        void animRunning(boolean isShow);

        void endAnim(boolean isShow);
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
        notAllowSwpieLeftRect.set(-mWith, -VERTICAL_SWIPE_SIZE, -HERTICAL_SWIPE_SIZE, VERTICAL_SWIPE_SIZE);
        notAllowSwpieRightRect.set(HERTICAL_SWIPE_SIZE, -VERTICAL_SWIPE_SIZE, mWith, VERTICAL_SWIPE_SIZE);
        donothingRect.set(-HERTICAL_SWIPE_SIZE, 0, HERTICAL_SWIPE_SIZE, VERTICAL_SWIPE_SIZE);
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();

        if (2 != getChildCount())
            throw new RuntimeException("There must be 2 childViews(layout)!");
        headerView = getChildAt(0);
        mTarget = getChildAt(1);
        if (headerView instanceof ViewGroup && suspendedAreaId > 0) {
            View suspendedView = headerView.findViewById(suspendedAreaId);
            suspendedAreaHeight = suspendedView.getMeasuredHeight();
        }
        int childViewWidthMeasureSpec = MeasureSpec.makeMeasureSpec(mWith - paddingLeft - paddingRight, MeasureSpec.EXACTLY);
        int childViewHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        measureChild(headerView, childViewWidthMeasureSpec, childViewHeightMeasureSpec);
        headerViewHeight = headerView.getMeasuredHeight();
        measureChild(mTarget, childViewWidthMeasureSpec, childViewHeightMeasureSpec);
        int convertHeight = mTarget.getMeasuredHeight();

        if (0 == convertHeight) {
            childViewHeightMeasureSpec = MeasureSpec.makeMeasureSpec(mHeight - paddingTop - paddingBottom-suspendedAreaHeight, MeasureSpec.EXACTLY);
            measureChild(mTarget, childViewWidthMeasureSpec, childViewHeightMeasureSpec);
        }

        hideContentHeight  = headerViewHeight - suspendedAreaHeight;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();
        int headerShowHeight = headerViewHeight + paddingTop + scrollY;
        headerView.layout(paddingLeft, paddingTop + scrollY, headerView.getMeasuredWidth() - paddingRight, headerShowHeight);
        mTarget.layout(paddingLeft, headerShowHeight, mTarget.getMeasuredWidth() - paddingRight, mHeight - paddingBottom);
    }


    final static int INVALID_POINTER = -1;

    int scrollY;
    int mActivePointerId;
    int pointerIndex;
    int downY;

    int FLAG_PROCESSD =2;
//    int FLAG_FIRST_PROCESSD = 0X4;
//    int FLAG_HIDE = 0X8;
//    int FLAG_SHOW = 0X10;
    int flag = 0;



    private int downX;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (_isAnimRunning()||0==hideContentHeight) {
            return super.dispatchTouchEvent(ev);
        }

        try {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    if (0 != (FLAG_PROCESSD & flag))
                        flag &= ~FLAG_PROCESSD;


                    //SwipeRefreshLayout
                    mActivePointerId = ev.getPointerId(0);
                    pointerIndex = ev.findPointerIndex(mActivePointerId);
                    downY = (int) ev.getY(pointerIndex);
                    downX = (int) ev.getX(mActivePointerId);
                    super.dispatchTouchEvent(ev);
                    return true;
                }
                case MotionEvent.ACTION_MOVE: {

                    if (INVALID_POINTER == mActivePointerId) {
                        Log.e(LOG_TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
                        break;
                    }
                    pointerIndex = ev.findPointerIndex(mActivePointerId);
                    if (pointerIndex < 0) {
                        break;
                    }
                    int moveY = downY - (int) ev.getY(pointerIndex);
                    int moveX = downX - (int) ev.getX(pointerIndex);

                    boolean isSwipeDown = moveY < 0;
                    boolean isShowing = isShowing();
                    if (0 == (flag & FLAG_PROCESSD) && !_isAnimRunning() && allowSwpieRect.contains(Math.abs(moveX), Math.abs(moveY))) {


                        if (!isSwipeDown && _enableHideHeaderView() && isShowing) {
                            flag |= FLAG_PROCESSD;

                            _hideHeader();

                        } else if (allowSwpieRect.contains(Math.abs(moveX), Math.abs(moveY)) && isSwipeDown && _enableShowHeaderView() && !isShowing) {
                            flag |= FLAG_PROCESSD;
                            if(enableShowHeader)
                            {
                                _showHeader();
                            }

                        }
                    }
                    if(!isShowing || isSwipeDown||(isShowing && notAllowSwpieLeftRect.contains(moveX,moveY )|| notAllowSwpieRightRect.contains(moveX,moveY )))
                    {
                        super.dispatchTouchEvent(ev);
                    }
                    return true;
                }
                case MotionEvent.ACTION_UP: {
                }
                break;
                case MotionEvent.ACTION_CANCEL: {
                    mActivePointerId = INVALID_POINTER;
                }
                break;
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
       boolean isProcess =  true;
        try {
            super.dispatchTouchEvent(ev);
        }
        catch (Exception ex)
        {
        }
        return isProcess;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if(_isAnimRunning())
            return true;
        return super.onInterceptTouchEvent(ev);
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


    UpdateListener updateListener = new UpdateListener();

    private class UpdateListener implements ValueAnimator.AnimatorUpdateListener {

        private boolean isShowing;

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            scrollY = (int) animation.getAnimatedValue();
            requestLayout();
            if (null != onOperatalbeListener)
                onOperatalbeListener.animRunning(isShowing);
        }
    }


    public boolean isRunning()
    {
        return _isAnimRunning();
    }




    private void _showHeader() {
        _showAnim(true);
    }


    private void _showAnim(final boolean isshowAnim) {
        if (_isAnimRunning())
            valueAnimator.cancel();
        valueAnimator = isshowAnim ? ValueAnimator.ofInt(-headerViewHeight + suspendedAreaHeight, 0) : ValueAnimator.ofInt(0, -headerViewHeight + suspendedAreaHeight);
        updateListener.isShowing = isshowAnim;
        valueAnimator.addUpdateListener(updateListener);
        if(isshowAnim)
        {
            valueAnimator.setDuration(500);
            valueAnimator.setInterpolator(new DecelerateInterpolator(2f));
        }
        else
        {
            valueAnimator.setInterpolator(new OvershootInterpolator(1.5f));
            valueAnimator.setDuration(300);
        }

        valueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (null != onOperatalbeListener)
                    onOperatalbeListener.startAnim(isshowAnim);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                scrollY = isshowAnim?0:-headerViewHeight + suspendedAreaHeight;
                if (null != onOperatalbeListener)
                    onOperatalbeListener.endAnim(isshowAnim);
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


    public boolean isEnableShowHeader() {
        return enableShowHeader;
    }

    private boolean enableShowHeader=true;


    public void showHeader()
    {
        _showHeader();
    }

    public void hideHeader()
    {
        _hideHeader();
    }


    public void setEnableShowHeader(boolean enable)
    {
        this.enableShowHeader = enable;
    }

}

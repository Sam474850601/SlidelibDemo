package com.yoyonewbie.android.swipefreshlib;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import java.lang.ref.WeakReference;

/**
 *  下拉刷新基类。可扩展
 */

public abstract class BaseSwipeRefreshLayout extends ViewGroup {


    /**
     * 提供下拉头部
     */
    protected abstract View getHeaderView(Context context);

    /**
     * 是否允许下拉了。
     */
    protected abstract boolean enablePulldown();

    /**
     * 下拉时，校验是否允许释放去刷新
     */
    protected abstract void allowedToReleaseBeforePulledDownSetting();

    /**
     * 没有到达下拉释放条件
     */
    protected abstract void notEnoughPullReadyToReleasePulledDownSetting();


    /**
     * 下拉后，触发 释放立即刷新
     *
     * @return
     */
    protected abstract void readyToReleasePulledDownSetting();


    /**
     * 释放下拉后，触发刷新中。。。
     *
     * @return
     */
    protected abstract void freshingPulledDownSetting();

    /**
     * 下拉成功后执行
     */
    protected abstract void freshingCompletedPulledDownSetting();

    public BaseSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        screenWith = display.getWidth();
        screenHeight = display.getHeight();
        headerView = getHeaderView(context);
        addView(headerView);
        mFlag |= FLAG_PULL_DOWN_NORMAL;
    }


    final static int LIMITE_MOVING_Y_MIN = 50;

    final static int LIMITE_MOVING_X_INNER_ABS = 50;


    private int screenWith = 0;

    private int screenHeight = 0;

    View headerView;

    private int headerMarinTop;

    private View childView;

    int mWith;

    int mHeight;

    int headerViewHeight;

    int childViewHeight;


    int downX;
    int downY;
    int moveY;


    public final static int FLAG_PULL_DOWN_NORMAL = 2;

    public final static int FLAG_PULL_DOWN_DRAGGING = 4;

    public final static int FLAG_PULL_DOWN_READY_RELEASE = 8;

    public final static int FLAG_PULL_DOWN_RELEASE = 16;


    private int mFlag = 0;

    //允许手势拖动区间
    Rect allowablePullingRange = new Rect();


    private ValueAnimator valueAnimator;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWith = getSize(screenWith, widthMeasureSpec);
        mHeight = getSize(screenHeight, heightMeasureSpec);
        setMeasuredDimension(mWith, mHeight);
        childView = getChildAt(1);
        measureChild(headerView, widthMeasureSpec, heightMeasureSpec);
        measureChild(childView, widthMeasureSpec, heightMeasureSpec);
        headerViewHeight = headerView.getMeasuredHeight();
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int paddingTop = getPaddingTop();
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();
        int magrinTop = -headerViewHeight + paddingTop + headerMarinTop;
        int headerBottom = paddingTop + headerMarinTop + headerViewHeight - headerViewHeight;
        headerView.layout(paddingLeft, magrinTop, mWith - paddingRight, headerBottom);
        childView.layout(paddingLeft, headerBottom, mWith - paddingRight, mHeight - paddingBottom);
    }


    protected void move(int move) {
        //限制下拉最大距离不再触发下拉
        int pullMovingHeightOfLimite = mHeight / 6;
        if (move < 0)
            return;
        if(move>pullMovingHeightOfLimite)
            move = pullMovingHeightOfLimite;
        //实际最大移动距离
        int maxMoving = headerViewHeight * 2;
        headerMarinTop = move * maxMoving / pullMovingHeightOfLimite;
        if(headerMarinTop >=headerViewHeight )
        {
            mFlag &=~FLAG_PULL_DOWN_DRAGGING;
            mFlag |= FLAG_PULL_DOWN_READY_RELEASE;
            readyToReleasePulledDownSetting();
        }
        requestLayout();
    }




    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
            }break;
            case MotionEvent.ACTION_MOVE: {
                int currentMoveY = (int) (event.getY() - downY);
                moveY = currentMoveY;
                int realMoving = _getRealYMoving(currentMoveY);
                move(realMoving);
            }
            break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                getParent().requestDisallowInterceptTouchEvent(false);
                if(enablePulldown())
                {
                    if(0 == (mFlag&FLAG_PULL_DOWN_RELEASE) && ( mFlag & FLAG_PULL_DOWN_READY_RELEASE) !=0  )
                    {
                        mFlag |= FLAG_PULL_DOWN_RELEASE;
                       _startMovingAnim(BackMovingAnimatorListener.TYPE_PULLDOWN_FRESHING, _getRealYMoving(moveY));
                    }
                    else if(0 != (mFlag & FLAG_PULL_DOWN_DRAGGING))
                    {

                        _startMovingAnim(BackMovingAnimatorListener.TYPE_PULLDOWN_NOT_ENOUNGH_PULL_READY_TO_RELEASE, _getRealYMoving(moveY));
                    }
                    _log("dispatchTouchEvent", "ACTION_UP");
                }
            }
            break;
        }
        return super.onTouchEvent(event);
    }

    private boolean _isAnimRunning()
    {
        return null != valueAnimator && valueAnimator.isRunning();
    }

    private void _startMovingAnim(int type, int move)
    {
        if(_isAnimRunning())
            valueAnimator.cancel();
        if(BackMovingAnimatorListener.TYPE_PULLDOWN_FRESHING == type )
        {
            valueAnimator = ValueAnimator.ofInt(move, headerViewHeight);
            valueAnimator.setInterpolator(new LinearInterpolator());
            valueAnimator.setDuration(500);
        }
        else if(BackMovingAnimatorListener.TYPE_PULLDOWN_NOT_ENOUNGH_PULL_READY_TO_RELEASE == type ||BackMovingAnimatorListener.TYPE_PULLDOWN_FRESHING_OK == type)
        {
            valueAnimator = ValueAnimator.ofInt(move, -headerViewHeight);
            valueAnimator.setInterpolator(new DecelerateInterpolator());
            valueAnimator.setDuration(1500);
        }
        if(null != valueAnimator)
        {
            valueAnimator.addUpdateListener(new MovingAnimatorUpdateListener(this));
            valueAnimator.addListener(new BackMovingAnimatorListener(type, this));
            valueAnimator.start();
        }

    }


    private static  class MovingAnimatorUpdateListener implements  ValueAnimator.AnimatorUpdateListener
    {
        private WeakReference<BaseSwipeRefreshLayout> wrf;
        public MovingAnimatorUpdateListener(BaseSwipeRefreshLayout basePullToRefreshLayout) {
            wrf = new WeakReference<BaseSwipeRefreshLayout>(basePullToRefreshLayout);
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            BaseSwipeRefreshLayout refreshLayout =  wrf.get();
            if(null != refreshLayout)
                refreshLayout.move((Integer) animation.getAnimatedValue());
        }
    }


    private static class  BackMovingAnimatorListener  implements Animator.AnimatorListener
    {
        static final  int TYPE_PULLDOWN_FRESHING = 1;

        static final int TYPE_PULLDOWN_NOT_ENOUNGH_PULL_READY_TO_RELEASE = 2;

        static final int TYPE_PULLDOWN_FRESHING_OK = 3;

        int type;
        WeakReference<BaseSwipeRefreshLayout> wrf;
        BackMovingAnimatorListener(int type, BaseSwipeRefreshLayout basePullToRefreshLayout)
        {
            this.type  = type;
            this.wrf = new WeakReference<BaseSwipeRefreshLayout>(basePullToRefreshLayout);
        }

        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {
            BaseSwipeRefreshLayout basePullToRefreshLayout =  wrf.get();
            if(null== basePullToRefreshLayout)
                return;
            if(TYPE_PULLDOWN_FRESHING == type)
            {
                basePullToRefreshLayout.freshingPulledDownSetting();
                OnRefreshListener onRefreshListenr = basePullToRefreshLayout.onRefreshListener;
                if(null != onRefreshListenr)
                    onRefreshListenr.onRefresh();

            }
            else if(TYPE_PULLDOWN_NOT_ENOUNGH_PULL_READY_TO_RELEASE == type)
            {
                basePullToRefreshLayout.mFlag = 0;
                basePullToRefreshLayout.mFlag |= FLAG_PULL_DOWN_NORMAL;
                basePullToRefreshLayout.notEnoughPullReadyToReleasePulledDownSetting();
            }
            else if(TYPE_PULLDOWN_FRESHING_OK == type)
            {
                basePullToRefreshLayout.mFlag = 0;
                basePullToRefreshLayout.mFlag |= FLAG_PULL_DOWN_NORMAL;
                basePullToRefreshLayout.freshingCompletedPulledDownSetting();
            }
            wrf.clear();
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    }




    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                //清除之前保存的滑动记录
                moveY = 0;
                downY = (int) event.getY();
                downX = (int) event.getX();

                _log("dispatchTouchEvent", "ACTION_DOWN");
                if((mFlag & FLAG_PULL_DOWN_NORMAL) !=0 && enablePulldown()&&!_isAnimRunning())
                {
                    mFlag &=~FLAG_PULL_DOWN_NORMAL;
                    mFlag |= FLAG_PULL_DOWN_DRAGGING;
                    getParent().requestDisallowInterceptTouchEvent(true);
                    allowedToReleaseBeforePulledDownSetting();
                    if (allowablePullingRange.isEmpty()) {
                        allowablePullingRange.set(-LIMITE_MOVING_X_INNER_ABS, LIMITE_MOVING_Y_MIN, LIMITE_MOVING_X_INNER_ABS, mHeight);
                    }
                }
                return false;
            }
            case MotionEvent.ACTION_MOVE: {
                if( 0 == (mFlag&FLAG_PULL_DOWN_RELEASE)  && ((mFlag & FLAG_PULL_DOWN_DRAGGING)!= 0 || (FLAG_PULL_DOWN_READY_RELEASE &mFlag) !=0) && enablePulldown()&&!_isAnimRunning()   )
                {
                    int currentMoveY = (int) (event.getY() - downY);
                    int currentMoveX = (int) (event.getX() - downX);
                    if (currentMoveY < moveY) {
                        break;
                    }
                    moveY = currentMoveY;
                    if (moveY>LIMITE_MOVING_Y_MIN) {
                        if(_whetherInAllowablePullingRange(currentMoveX, currentMoveY))
                        {
                            _log("onInterceptTouchEvent", "ACTION_MOVE", "currentMove", currentMoveY);
                            return true;
                        }
                    }
                }
            }break;
            case MotionEvent.ACTION_UP: {
                _log("onInterceptTouchEvent", "ACTION_UP");
                boolean disallowInterceptTouchEvent =  moveY>0&&enablePulldown()&&!_isAnimRunning();
                return disallowInterceptTouchEvent;
            }
        }
        return super.onInterceptTouchEvent(event);
    }

    public void freshingCompleted()
    {
        if((mFlag & FLAG_PULL_DOWN_RELEASE) != 0)
        {

          _startMovingAnim( BackMovingAnimatorListener.TYPE_PULLDOWN_FRESHING_OK, headerViewHeight);
        }
    }

    public interface  OnRefreshListener
    {
        void onRefresh();
    }

    private OnRefreshListener onRefreshListener;

    public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
        this.onRefreshListener = onRefreshListener;
    }

    private int _getRealYMoving(int currentMoveY) {
        return currentMoveY - LIMITE_MOVING_Y_MIN;
    }


    /**
     * 防止与父容器滑动冲突,判断是否在滑动区间范围
     */
    private boolean _whetherInAllowablePullingRange(int moveX, int moveY) {
        return allowablePullingRange.contains(moveX, moveY);
    }


    protected void _log(Object... values) {
        String strs = "";
        for (Object value : values) {
            strs += value + ",";
        }
        Log.e("PullToRefreshLayout", "" + strs.substring(0, strs.length() - 1));
    }


    public int getSize(int size, int measureSpec) {
        int result = size;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        switch (specMode) {
            case MeasureSpec.UNSPECIFIED:
                result = size;
                break;
            case MeasureSpec.AT_MOST:
                result = Math.min(size, specSize);
                break;
            case MeasureSpec.EXACTLY:
                result = specSize;
                break;
        }
        return result;
    }
}

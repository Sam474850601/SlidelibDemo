package com.yoyonewbie.android.lib.swipefreshlib;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.Parcelable;
import android.support.annotation.RequiresApi;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Scroller;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.Date;

/**
 *  An extensible swipe down refreshes the view, but needs to inherit it and implement its header view
 *  @author  Sam
 */

public abstract class BaseSwipeRefreshLayout extends ViewGroup {

       String LOG_TAG = getClass().getSimpleName();
    private Scroller mScroller;
    /**
     * Provides a swipe-down header view
     */
    protected abstract View getHeaderView(Context context);

    /**
     * Whether to allow swipe-down for each swiping
     */
    protected  boolean enableSwipedown()
    {
        if(null != swipeOperatableAdapter)
            return swipeOperatableAdapter.enableSwipeDown();
        return true;
    }

    /**
     * Whether to allow swipe-down for each swiping
     */
    protected  boolean enableSwipeUp()
    {
        if(null != swipeOperatableAdapter)
            return swipeOperatableAdapter.enableSwipeUp();
        return true;
    }

    private  SwipeOperatableAdapter swipeOperatableAdapter;

    public SwipeOperatableAdapter getSwipeOperatableAdapter() {
        return swipeOperatableAdapter;
    }

    public void setSwipeOperatableAdapter(SwipeOperatableAdapter swipeOperatableAdapter) {
        this.swipeOperatableAdapter = swipeOperatableAdapter;
    }

    public interface SwipeOperatableAdapter
    {
        boolean enableSwipeDown();
        boolean enableSwipeUp();
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        if (mHeaderViewIndex < 0) {
            return i;
        } else if (i == childCount - 1) {
            // Draw the selected child last
            return mHeaderViewIndex;
        } else if (i >= mHeaderViewIndex) {
            // Move the children after the selected child earlier one
            return i + 1;
        } else {
            // Keep the children before the selected child the same
            return i;
        }
    }

    /**
     * This method will be called when the dropdown does not reach the refresh before
     */
    protected abstract void allowedToReleaseBeforePulledDownSetting();


    /**
     * This method will be called when moving before allow to fresh
     * @param persent
     */
    protected abstract void allowedToReleaseBeforePulledDownSetting(float persent);

    /**
     * This method will be called  in each release of the gesture and did not reach the refresh time
     */
    protected abstract void notEnoughPullReadyToReleasePulledDownSetting();


    /**
     * This method will be called every time you can just slide to trigger the refresh effect
     *
     * @return
     */
    protected abstract void readyToReleasePulledDownSetting();




    /**
     * This method will be called after the release of gestures to refresh the animation
     *
     * @return
     */
    protected abstract void freshingPulledDownSetting();

    /**
     * This method will be called when the animation is ready to be flushed
     */
    protected abstract void freshingCompletedPulledDownSetting();

    public BaseSwipeRefreshLayout(Context context) {
        super(context);
        init(context);
    }

    public BaseSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);

    }

    public BaseSwipeRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public BaseSwipeRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }




    protected void init(Context context)
    {
        mScroller = new Scroller(context);
        setChildrenDrawingOrderEnabled(true);
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

    private View mTarget;

    int mWith;

    int mHeight;

    int headerViewHeight;

    int childViewHeight;


    int downX;
    int downY;
    int moveY;
    int moveY2;

    public final static int FLAG_PULL_DOWN_NORMAL = 0x2;

    public final static int FLAG_PULL_DOWN_DRAGGING = 0x4;

    public final static int FLAG_PULL_DOWN_READY_RELEASE = 0x8;

    public final static int FLAG_PULL_DOWN_RELEASE = 0x10;

    public  final static  int FLAG_FRESHING_NO_COMPLETION = 0x40;


    private int mFlag = 0;


    public final static int GRAVITY_TOP = 1;

    public final static int GRAVITY_INNER = 0;

    private int gravity = GRAVITY_INNER;

    //Allow gestures to drag intervals
    Rect allowablePullingRange = new Rect();


    Rect allowableSwipeUpRange = new Rect();


    private ValueAnimator valueAnimator;



    public void setGravity(int gravity)
    {
        this.gravity = gravity;
    }


    private  int mHeaderViewIndex=-1;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if(getChildCount()>2)
            throw new RuntimeException("There is must be only one childView !");

        mWith = getMeasuredWidth();
        mHeight = getMeasuredHeight();
        mTarget = getChildAt(1);

        mTarget.measure(MeasureSpec.makeMeasureSpec(
                getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(
                getMeasuredHeight() - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY));

        int headerViewWithMeasureSpec = MeasureSpec.makeMeasureSpec(mWith-getPaddingLeft()-getPaddingRight(), MeasureSpec.EXACTLY);
        int headerViewHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        headerView.measure(headerViewWithMeasureSpec, headerViewHeightMeasureSpec);
        headerViewHeight = headerView.getMeasuredHeight();

        mHeaderViewIndex = -1;
        // Get the index of the circleview.
        for (int index = 0; index < getChildCount(); index++) {
            if (getChildAt(index) == headerView) {
                mHeaderViewIndex = index;
                break;
            }
        }
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int paddingTop = getPaddingTop();
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();
        int magrinTop = -headerViewHeight + paddingTop + headerMarinTop;
        int headerBottom = paddingTop + headerMarinTop + headerViewHeight - headerViewHeight;
        if(GRAVITY_INNER ==gravity)
        {
            mTarget.layout(paddingLeft, headerBottom, mWith - paddingRight, mHeight - paddingBottom-paddingTop);
            headerView.layout(paddingLeft, magrinTop, mWith - paddingRight, headerBottom);
        }
        else if(GRAVITY_TOP ==gravity)
        {
            mTarget.layout(paddingLeft, paddingTop, mWith - paddingRight, mHeight - paddingBottom-paddingTop);
            headerView.layout(paddingLeft, magrinTop, mWith - paddingRight, headerBottom);
        }
        else
        {
            throw new RuntimeException("you must provide  a valid  value that is gravity");
        }
    }






    boolean allowMovingSetting= true;

    protected   void move(int move) {
        //Limit the maximum amount of glide
        int pullMovingHeightOfLimite = mHeight / 6;
        if (move < 0)
            return;
        move = (int)(move*0.5f);
        if(move>pullMovingHeightOfLimite)
            move = pullMovingHeightOfLimite;
        //The actual animation moves the maximum value
        int maxMoving = headerViewHeight * 2;
       // headerMarinTop = move * maxMoving / pullMovingHeightOfLimite;
        headerMarinTop =  move;
        if(headerMarinTop >=headerViewHeight && 0 != (mFlag&FLAG_PULL_DOWN_DRAGGING))
        {
            mFlag &=~FLAG_PULL_DOWN_DRAGGING;
            mFlag |= FLAG_PULL_DOWN_READY_RELEASE;
            readyToReleasePulledDownSetting();
        }

        int persentMoving = headerMarinTop;

        if(persentMoving>=headerViewHeight )
        {
            persentMoving = headerViewHeight;
        }


        if(allowMovingSetting)
        {
            BigDecimal moveDecimal  = new BigDecimal(persentMoving);
            BigDecimal maxDecimal  = new BigDecimal(headerViewHeight);
            BigDecimal resultDecimal =  moveDecimal.divide(maxDecimal,2, BigDecimal.ROUND_HALF_UP);
            allowedToReleaseBeforePulledDownSetting(resultDecimal.floatValue());
        }

        if(persentMoving>=headerViewHeight)
        {
            allowMovingSetting = false;
        }

        _move(headerMarinTop);
    }




    private void _move(int headerMarinTop)
    {
        if (headerMarinTop < 0)
            return;
        this.headerMarinTop = headerMarinTop;
        requestLayout();
    }


    private boolean isChildResumeNoEvent;


    float dispatchDownY;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if(0 == (mFlag&FLAG_FRESHING_NO_COMPLETION)&&0!=(mFlag&FLAG_PULL_DOWN_RELEASE))
        {
            switch (ev.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                {
                    dispatchDownY = ev.getY();

                }break;
                case MotionEvent.ACTION_MOVE:
                {
                    if(dispatchDownY-ev.getY()>0)
                    {
                        this.mFlag |= FLAG_FRESHING_NO_COMPLETION;
                        _startMovingAnim(BackMovingAnimatorListener.TYPE_FRESHING_NO_COMPLETION, headerViewHeight);
                    }
                }break;

            }
        }
        return super.dispatchTouchEvent(ev);
    }

    boolean scrollerIsFinished = true;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    isChildResumeNoEvent = true;
                    allowMovingSetting = true;
                    scrollerIsFinished = mScroller.isFinished();
                    return !_isAnimRunning();
                }
                case MotionEvent.ACTION_MOVE: {
                    if (INVALID_POINTER == mActivePointerId) {
                        Log.e(LOG_TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
                        break;
                    }
                    pointerIndex = event.findPointerIndex(mActivePointerId);
                    if (pointerIndex < 0) {
                        break;
                    }

                    int currentMoveY = (int) (event.getY(mActivePointerId) - downY);

                    if(currentMoveY>0 && scrollerIsFinished)
                    {
                         if( (enableSwipeDown && 0 != (mFlag&FLAG_FRESHING_NO_COMPLETION) &&!_isAnimRunning())||(enableSwipeDown && 0 == (mFlag&FLAG_PULL_DOWN_RELEASE)  && ((mFlag & FLAG_PULL_DOWN_DRAGGING)!= 0 || (FLAG_PULL_DOWN_READY_RELEASE &mFlag) !=0)&&!_isAnimRunning()  ))
                        {

                            if (currentMoveY < moveY) {
                                break;
                            }
                            moveY = currentMoveY;
                            int realMoving = _getRealYMoving(currentMoveY);
                            move(realMoving);
                        }
                    }
                    else if(enableSwipeUp())
                    {
                        if (currentMoveY > moveY2) {
                            break;
                        }
                        moveY2 = currentMoveY;
                        Log.e("isChildResumeNoEvent",isChildResumeNoEvent+" isChildResumeNoEvent" );
                        if(!isChildResumeNoEvent)
                            moveY2= moveY2+LIMITE_MOVING_Y_MIN;
                        if(!mScroller.isFinished())
                            mScroller.abortAnimation();
                        if(moveY2<-500)
                            moveY2= -500;

                        scrollTo(0, -moveY2);
                        invalidate();
                    }
                }
                break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP: {
                    int scrollY = getScrollY();
                    if(scrollY>0 &&moveY2<0)
                    {
                        _log("moveY2", moveY2);
                        mScroller.startScroll(0, getScrollY(), 0, moveY2, 800);
                        invalidate();
                    }
                    int realMoving = _getRealYMoving(moveY);

                    if(realMoving>0)
                    {
                        if(0 != (mFlag&FLAG_FRESHING_NO_COMPLETION) )
                        {
                            _startMovingAnim(BackMovingAnimatorListener.TYPE_FRESHING_NO_COMPLETION, headerMarinTop);
                        }
                       else   if( 0 == (mFlag&FLAG_PULL_DOWN_RELEASE) && ( mFlag & FLAG_PULL_DOWN_READY_RELEASE) !=0  )
                        {

                            mFlag |= FLAG_PULL_DOWN_RELEASE;
                            _startMovingAnim(BackMovingAnimatorListener.TYPE_PULLDOWN_FRESHING, headerMarinTop);
                        }
                        else
                        {

                            _startMovingAnim(BackMovingAnimatorListener.TYPE_PULLDOWN_NOT_ENOUNGH_PULL_READY_TO_RELEASE, headerMarinTop);
                        }

                    }

                }
                break;
            }
        }
        catch (IllegalArgumentException ex)
        {
            ex.printStackTrace();
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if(mScroller.computeScrollOffset())
        {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            invalidate();
        }
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
        else if(BackMovingAnimatorListener.TYPE_FRESHING_NO_COMPLETION == type ||BackMovingAnimatorListener.TYPE_PULLDOWN_NOT_ENOUNGH_PULL_READY_TO_RELEASE == type || BackMovingAnimatorListener.TYPE_PULLDOWN_FRESHING_OK == type)
        {
            valueAnimator = ValueAnimator.ofInt(move, 0);
            valueAnimator.setInterpolator(new DecelerateInterpolator());
            valueAnimator.setDuration(200);
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
                refreshLayout._move((Integer) animation.getAnimatedValue());
        }
    }


    private static class  BackMovingAnimatorListener  implements Animator.AnimatorListener
    {
        static final  int TYPE_PULLDOWN_FRESHING = 1;

        static final int TYPE_PULLDOWN_NOT_ENOUNGH_PULL_READY_TO_RELEASE = 2;

        static final int TYPE_PULLDOWN_FRESHING_OK = 3;

        static final int TYPE_FRESHING_NO_COMPLETION = 4;

        int type;
        WeakReference<BaseSwipeRefreshLayout> wrf;
        BackMovingAnimatorListener(int type, BaseSwipeRefreshLayout basePullToRefreshLayout)
        {
            this.type  = type;
            this.wrf = new WeakReference<BaseSwipeRefreshLayout>(basePullToRefreshLayout);
        }

        @Override
        public void onAnimationStart(Animator animation) {
            BaseSwipeRefreshLayout basePullToRefreshLayout =  wrf.get();
            if(null== basePullToRefreshLayout)
                return;
            if(TYPE_PULLDOWN_FRESHING_OK == type)
            {
                basePullToRefreshLayout.mFlag = 0;
                basePullToRefreshLayout.mFlag |= FLAG_PULL_DOWN_NORMAL;
                basePullToRefreshLayout.freshingCompletedPulledDownSetting();
            }
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
            wrf.clear();
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    }



    boolean enableSwipeDown;
    int mActivePointerId;
    int pointerIndex;
    final static  int INVALID_POINTER = -1;
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        try {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {

                    allowMovingSetting = true;
                    isChildResumeNoEvent= false;
                    mActivePointerId = event.getPointerId(0);
                    pointerIndex = event.findPointerIndex(mActivePointerId);
                    downY = (int) event.getY(mActivePointerId);
                    downX = (int) event.getX(mActivePointerId);
                    moveY = 0;
                    moveY2=0;
                    if (allowablePullingRange.isEmpty()) {
                        allowablePullingRange.set(-LIMITE_MOVING_X_INNER_ABS, LIMITE_MOVING_Y_MIN, LIMITE_MOVING_X_INNER_ABS, mHeight);
                    }
                    if(enableSwipeDown = enableSwipedown())
                    {
                        boolean isNormal = (mFlag & FLAG_PULL_DOWN_NORMAL) !=0;
                        if( isNormal&&enableSwipeDown&&!_isAnimRunning())
                        {
                            mFlag &=~FLAG_PULL_DOWN_NORMAL;
                            mFlag |= FLAG_PULL_DOWN_DRAGGING;
                            allowedToReleaseBeforePulledDownSetting();
                        }
                    }
                   break;
                }
                case MotionEvent.ACTION_MOVE: {

                    if (INVALID_POINTER == mActivePointerId) {
                        Log.e(LOG_TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
                        break;
                    }
                    pointerIndex = event.findPointerIndex(mActivePointerId);
                    if (pointerIndex < 0) {
                        break;
                    }

                    int currentMoveY = (int) (event.getY(mActivePointerId) - downY);
                    int currentMoveX = (int) (event.getX(mActivePointerId) - downX);
                        if(currentMoveY<0&&enableSwipeUp()&&!_isAnimRunning())
                    {
                        return _whetherInAllowablePullingRange(currentMoveX, -currentMoveY);
                    }
                    else
                    {
                        if(enableSwipeDown && currentMoveY>0)
                        {
                            if(0 != (mFlag&FLAG_FRESHING_NO_COMPLETION))
                                return _whetherInAllowablePullingRange(currentMoveX, currentMoveY);
                            if( 0 == (mFlag&FLAG_PULL_DOWN_RELEASE)  && ((mFlag & FLAG_PULL_DOWN_DRAGGING)!= 0 || (FLAG_PULL_DOWN_READY_RELEASE &mFlag) !=0)&&!_isAnimRunning()   )
                            {
                                if (currentMoveY < moveY) {
                                    break;
                                }
                                moveY = currentMoveY;
                                if (moveY>LIMITE_MOVING_Y_MIN) {
                                   boolean isAllowSwipeDown =  _whetherInAllowablePullingRange(currentMoveX, currentMoveY);
                                    if(isAllowSwipeDown)
                                    {
                                        scrollerIsFinished = mScroller.isFinished();
                                    }
                                    return isAllowSwipeDown;
                                }
                            }
                        }

                    }



                }break;
                case MotionEvent.ACTION_UP: {

                    boolean disallowInterceptTouchEvent = moveY > 0 && !_isAnimRunning();
                    return disallowInterceptTouchEvent && enableSwipeDown;
                }
                case MotionEvent.ACTION_CANCEL:
                {
                    mActivePointerId = INVALID_POINTER;
                }

            }
            return super.onInterceptTouchEvent(event);
        }catch (IllegalArgumentException ex)
        {

        }
        return false;
    }

    public void freshingCompleted()
    {
        if((mFlag & FLAG_PULL_DOWN_RELEASE) != 0)
        {
            if(0 ==( mFlag&FLAG_FRESHING_NO_COMPLETION ))
            {
                _startMovingAnim( BackMovingAnimatorListener.TYPE_PULLDOWN_FRESHING_OK, headerViewHeight);
            }
            else
            {
                mFlag = 0;
                mFlag |= FLAG_PULL_DOWN_NORMAL;
                if(headerMarinTop>0)
                    _startMovingAnim( BackMovingAnimatorListener.TYPE_PULLDOWN_FRESHING_OK, headerMarinTop);
                freshingCompletedPulledDownSetting();
            }

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
     *To prevent the collision with the parent container, to determine whether the scope of the swipe range
     */
    private boolean _whetherInAllowablePullingRange(int moveX, int moveY) {
        return allowablePullingRange.contains(moveX, moveY);
    }


    protected void _log(Object... values) {
        String strs = "";
        for (Object value : values) {
            strs += value + ",";
        }
        Log.e(getClass().getSimpleName(), "" + strs.substring(0, strs.length() - 1));
    }


    /**
     * Automatically enter the refresh state
     */
    public void autoSwipeDownFresh()
    {
        if(_isAnimRunning()||0!=(mFlag&FLAG_PULL_DOWN_RELEASE))
            return;
       this.post(new Runnable() {
           @Override
           public void run() {
               _autoSwipeDownFresh();
           }
       });
    }


    private void _autoSwipeDownFresh()
    {
        mFlag = 0;
        mFlag |= FLAG_PULL_DOWN_READY_RELEASE;
        mFlag |= FLAG_PULL_DOWN_RELEASE;
        freshingPulledDownSetting();
        _startMovingAnim(BackMovingAnimatorListener.TYPE_PULLDOWN_FRESHING, 0);
    }

    /**
     * Automatically enter the refresh state, you can set the delay time
     */
    public void autoSwipeDownFresh(long delayedTime)
    {
        if(_isAnimRunning()||0!=(mFlag&FLAG_PULL_DOWN_RELEASE))
            return;
        this.postDelayed(new Runnable() {
            @Override
            public void run() {
                _autoSwipeDownFresh();
            }
        }, delayedTime);
    }

    /**
     * the swipelayout  is touching or its anim running
     */
    public boolean isRunning()
    {
        return _isAnimRunning()||0!=(mFlag&FLAG_PULL_DOWN_DRAGGING)||0!=(mFlag&FLAG_PULL_DOWN_READY_RELEASE)||
                0!=(mFlag&FLAG_PULL_DOWN_RELEASE);
    }


    /**
     * recover fresh state, and show the header
     */
    public void recoveryFreshState()
    {
        this.post(new Runnable() {
            @Override
            public void run() {
                int headerViewWithMeasureSpec = MeasureSpec.makeMeasureSpec(mWith-getPaddingLeft()-getPaddingRight(), MeasureSpec.EXACTLY);
                int headerViewHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
                headerView.measure(headerViewWithMeasureSpec, headerViewHeightMeasureSpec);
                int headerViewHeight = headerView.getMeasuredHeight();
                _move(headerViewHeight);
                freshingPulledDownSetting();
            }
        });
    }


    /**
     * whether it is freshing
     * @return
     */
    public boolean isRefreshing()
    {
        return 0!=(mFlag&FLAG_PULL_DOWN_RELEASE);
    }




}

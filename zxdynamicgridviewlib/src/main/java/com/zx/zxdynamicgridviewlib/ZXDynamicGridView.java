package com.zx.zxdynamicgridviewlib;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.GridView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhaoxin on 15/3/12.
 * QQ:343986392
 * https://github.com/zhaoxin1943
 */
public class ZXDynamicGridView extends GridView {

    private static final int INVALID_ID = -1;
    private BitmapDrawable mHoverCell;
    private Rect mHoverCellCurrentBounds;
    private Rect mHoverCellOriginalBounds;
    private static final int MOVE_DURATION = 300;
    private int mDownX;
    private int mDownY;
    private int mActivePointerId;
    private int mLastEventY, mLastEventX;
    private boolean mCellIsMobile;

    private int mTotalOffsetX = 0;
    private int mTotalOffsetY = 0;

    private View mMobileView;
    private long mMobileItemId = INVALID_ID;
    private List<Long> idList = new ArrayList<>();
    //用来区分直线还是对角线交换
    private int mOverlapIfSwitchStraightLine;
    private boolean mHoverAnimation;
    private boolean mReorderAnimation;
    private boolean mIsMobileScrolling;
    private boolean mIsWaitingForScrollFinish = false;
    private int mScrollState = OnScrollListener.SCROLL_STATE_IDLE;
    private static final int SMOOTH_SCROLL_AMOUNT_AT_EDGE = 8;
    private int mSmoothScrollAmountAtEdge = 0;

    public ZXDynamicGridView(Context context) {
        super(context);
        init();
    }

    public ZXDynamicGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public ZXDynamicGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        super.setOnScrollListener(mScrollListener);
        mOverlapIfSwitchStraightLine = getResources().getDimensionPixelSize(R.dimen.dgv_overlap_if_switch_straight_line);
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        mSmoothScrollAmountAtEdge = (int) (SMOOTH_SCROLL_AMOUNT_AT_EDGE * metrics.density + 0.5f);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mHoverCell != null) {
            mHoverCell.draw(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        int action = MotionEventCompat.getActionMasked(ev);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mDownX = (int) ev.getX();
                mDownY = (int) ev.getY();
                mActivePointerId = ev.getPointerId(0);
                break;
            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_ID) {
                    break;
                }

                int pointerIndex = ev.findPointerIndex(mActivePointerId);

                mLastEventY = (int) ev.getY(pointerIndex);
                mLastEventX = (int) ev.getX(pointerIndex);
                int deltaY = mLastEventY - mDownY;
                int deltaX = mLastEventX - mDownX;

                if (mCellIsMobile) {
                    mHoverCellCurrentBounds.offsetTo(mHoverCellOriginalBounds.left + deltaX + mTotalOffsetX,
                            mHoverCellOriginalBounds.top + deltaY + mTotalOffsetY);
                    mHoverCell.setBounds(mHoverCellCurrentBounds);
                    invalidate();
                    handleCellSwitch();
                    mIsMobileScrolling = false;
                    handleMobileCellScroll();
                    return false;
                }
                break;
            case MotionEvent.ACTION_UP:
                touchEventsEnded();
                break;
            case MotionEvent.ACTION_CANCEL:
                touchEventsCancelled();
                break;
            case MotionEvent.ACTION_POINTER_UP:
                pointerIndex = MotionEventCompat.getActionIndex(ev);
                int pointerId = ev.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId) {
                    touchEventsEnded();
                }
                break;

        }
        return super.onTouchEvent(ev);
    }

    private void handleCellSwitch() {
        final int deltaY = mLastEventY - mDownY;
        final int deltaX = mLastEventX - mDownX;
        final int deltaYTotal = mHoverCellOriginalBounds.centerY() + mTotalOffsetY + deltaY;
        final int deltaXTotal = mHoverCellOriginalBounds.centerX() + mTotalOffsetX + deltaX;
        mMobileView = getViewForId(mMobileItemId);
        View targetView = null;
        float vX = 0;
        float vY = 0;
        Point mobileColumnRowPair = getColumnAndRowForView(mMobileView);
        for (Long id : idList) {
            View view = getViewForId(id);
            if (view != null) {
                Point targetColumnRowPair = getColumnAndRowForView(view);
                if ((aboveRight(targetColumnRowPair, mobileColumnRowPair)
                        && deltaYTotal < view.getBottom() && deltaXTotal > view.getLeft()
                        || aboveLeft(targetColumnRowPair, mobileColumnRowPair)
                        && deltaYTotal < view.getBottom() && deltaXTotal < view.getRight()
                        || belowRight(targetColumnRowPair, mobileColumnRowPair)
                        && deltaYTotal > view.getTop() && deltaXTotal > view.getLeft()
                        || belowLeft(targetColumnRowPair, mobileColumnRowPair)
                        && deltaYTotal > view.getTop() && deltaXTotal < view.getRight()
                        || above(targetColumnRowPair, mobileColumnRowPair)
                        && deltaYTotal < view.getBottom() - mOverlapIfSwitchStraightLine
                        || below(targetColumnRowPair, mobileColumnRowPair)
                        && deltaYTotal > view.getTop() + mOverlapIfSwitchStraightLine
                        || right(targetColumnRowPair, mobileColumnRowPair)
                        && deltaXTotal > view.getLeft() + mOverlapIfSwitchStraightLine
                        || left(targetColumnRowPair, mobileColumnRowPair)
                        && deltaXTotal < view.getRight() - mOverlapIfSwitchStraightLine)) {
                    float xDiff = Math.abs(ZXDynamicGridUtils.getViewX(view) - ZXDynamicGridUtils.getViewX(mMobileView));
                    float yDiff = Math.abs(ZXDynamicGridUtils.getViewY(view) - ZXDynamicGridUtils.getViewY(mMobileView));
                    if (xDiff >= vX && yDiff >= vY) {
                        vX = xDiff;
                        vY = yDiff;
                        targetView = view;
                    }
                }
            }
        }
        if (targetView != null) {
            final int originalPosition = getPositionForView(mMobileView);
            final int targetPosition = getPositionForView(targetView);
            final IZXDynamicGridViewAdapter izxDynamicGridViewAdapter = getAdapterInterface();
            if (targetPosition == INVALID_POSITION || !izxDynamicGridViewAdapter.canReorder(originalPosition)
                    || !izxDynamicGridViewAdapter.canReorder(targetPosition)) {
                updateNeighborViewsForId(mMobileItemId);
                return;
            }

            reorderElements(originalPosition, targetPosition);

            mDownY = mLastEventY;
            mDownX = mLastEventX;

            SwitchCellAnimator switchCellAnimator = new LSwitchCellAnimator(deltaX, deltaY);
            updateNeighborViewsForId(mMobileItemId);
            switchCellAnimator.animateSwitchCell(originalPosition, targetPosition);

        }

    }

    private void reorderElements(int originalPosition, int targetPosition) {
        getAdapterInterface().reorderItems(originalPosition, targetPosition);
    }

    private View getViewForId(long id) {
        int firstVisiblePosition = getFirstVisiblePosition();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            long tempId = getAdapter().getItemId(firstVisiblePosition + i);
            if (tempId == id) {
                return getChildAt(i);
            }
        }
        return null;
    }


    private void touchEventsEnded() {

        final View mobileView = getViewForId(mMobileItemId);
        if (mobileView != null && (mCellIsMobile || mIsWaitingForScrollFinish)) {
            mCellIsMobile = false;
            mIsWaitingForScrollFinish = false;
            mIsMobileScrolling = false;
            mActivePointerId = INVALID_ID;

            if (mScrollState != OnScrollListener.SCROLL_STATE_IDLE) {
                mIsWaitingForScrollFinish = true;
                return;
            }

            mHoverCellCurrentBounds.offsetTo(mobileView.getLeft(), mobileView.getTop());
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
                animateBounds(mobileView);
            } else {
                mHoverCell.setBounds(mHoverCellCurrentBounds);
                invalidate();
                reset(mobileView);
            }

        } else {
            touchEventsCancelled();
        }

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void animateBounds(final View mobileView) {
        //TypeEvaluator定义了如何计算属性的值
        //它是一个计算器接口，它允许你创建你自己的计算器。如果你正在计算一个对象属性并不是int,float或者颜色值类型的，
        //那么你必须实现TypeEvaluator接口去指定如何去计算对象的属性值。
        TypeEvaluator<Rect> sBoundEvaluator = new TypeEvaluator<Rect>() {
            @Override
            public Rect evaluate(float fraction, Rect startValue, Rect endValue) {
                return new Rect(interpolate(startValue.left, endValue.left, fraction),
                        interpolate(startValue.top, endValue.top, fraction),
                        interpolate(startValue.right, endValue.right, fraction),
                        interpolate(startValue.bottom, endValue.bottom, fraction));
            }

            public int interpolate(int start, int end, float fraction) {
                return (int) (start + fraction * (end - start));
            }
        };
        //已经调用过mHoverCellCurrentBounds.offsetTo(mobileView.getLeft(), mobileView.getTop());
        ObjectAnimator objectAnimator = ObjectAnimator.ofObject(mHoverCell, "bounds", sBoundEvaluator, mHoverCellCurrentBounds);
        objectAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                invalidate();
            }
        });
        objectAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mHoverAnimation = true;
                updateEnableState();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mHoverAnimation = false;
                updateEnableState();
                reset(mobileView);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        objectAnimator.start();
    }

    private void touchEventsCancelled() {

        View mobileView = getViewForId(mMobileItemId);
        if (mCellIsMobile) {
            reset(mobileView);
        }

        mCellIsMobile = false;
        mIsMobileScrolling = false;
        mActivePointerId = INVALID_ID;

    }

    private void reset(View view) {
        idList.clear();
        mMobileItemId = INVALID_ID;
        view.setVisibility(View.VISIBLE);
        mHoverCell = null;

        for (int i = 0; i < getLastVisiblePosition() - getFirstVisiblePosition(); i++) {
            View child = getChildAt(i);
            if (child != null) {
                child.setVisibility(View.VISIBLE);
            }
        }

    }

    private void startDragAtPosition(int position) {

        mTotalOffsetY = mTotalOffsetX = 0;
        int itemNum = position - getFirstVisiblePosition();
        View selectedView = getChildAt(itemNum);
        if (selectedView != null) {
            mMobileItemId = getAdapter().getItemId(position);
            mHoverCell = getAndAddHoverView(selectedView);
            selectedView.setVisibility(View.INVISIBLE);
            if (isPostHoneycomb())
                selectedView.setVisibility(View.INVISIBLE);
            mCellIsMobile = true;
            updateNeighborViewsForId(mMobileItemId);
        }

    }

    private void updateNeighborViewsForId(long mMobileItemId) {
        idList.clear();
        int draggedPos = getPositionForId(mMobileItemId);
        for (int pos = getFirstVisiblePosition(); pos <= getLastVisiblePosition(); pos++) {
            if (pos != draggedPos && getAdapterInterface().canReorder(pos)) {
                idList.add(getItemId(pos));
            }
        }

    }

    private Long getItemId(int pos) {
        return getAdapter().getItemId(pos);
    }

    private int getPositionForId(long itemId) {
        View v = getViewForId(itemId);
        if (v == null) {
            return -1;
        } else {
            return getPositionForView(v);
        }
    }

    private BitmapDrawable getAndAddHoverView(View v) {
        int w = v.getWidth();
        int h = v.getHeight();
        int top = v.getTop();
        int left = v.getLeft();

        Bitmap b = getBitmapFromView(v);
        BitmapDrawable drawable = new BitmapDrawable(getResources(), b);
        mHoverCellOriginalBounds = new Rect(left, top, left + w, top + h);
        mHoverCellCurrentBounds = new Rect(mHoverCellOriginalBounds);
        drawable.setBounds(mHoverCellCurrentBounds);
        return drawable;
    }

    private Bitmap getBitmapFromView(View v) {
        Bitmap bitmap = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        v.draw(canvas);
        return bitmap;
    }

    public void startEditMode(int position) {
        requestDisallowInterceptTouchEvent(true);
        if (position != -1) {
            startDragAtPosition(position);
        }
    }


    private Point getColumnAndRowForView(View view) {
        //得到该View在位置(data set里面的位置)
        int pos = getPositionForView(view);
        int columns = getColumnCount();
        int row = pos / columns;
        int col = pos % columns;
        return new Point(col, row);
    }

    private int getColumnCount() {
        return getAdapterInterface().getColumnCount();
    }

    private IZXDynamicGridViewAdapter getAdapterInterface() {
        return ((IZXDynamicGridViewAdapter) getAdapter());
    }

    /**
     * 这里所有的方位都是相对于mMobileView，比如左下，就是指targetView在mMobileView的左下
     *
     * @param targetColumnRowPair
     * @param mobileColumnRowPair
     * @return
     */
    private boolean belowLeft(Point targetColumnRowPair, Point mobileColumnRowPair) {
        return targetColumnRowPair.y > mobileColumnRowPair.y && targetColumnRowPair.x < mobileColumnRowPair.x;
    }

    private boolean belowRight(Point targetColumnRowPair, Point mobileColumnRowPair) {
        return targetColumnRowPair.y > mobileColumnRowPair.y && targetColumnRowPair.x > mobileColumnRowPair.x;
    }

    private boolean aboveLeft(Point targetColumnRowPair, Point mobileColumnRowPair) {
        return targetColumnRowPair.y < mobileColumnRowPair.y && targetColumnRowPair.x < mobileColumnRowPair.x;
    }

    private boolean aboveRight(Point targetColumnRowPair, Point mobileColumnRowPair) {
        return targetColumnRowPair.y < mobileColumnRowPair.y && targetColumnRowPair.x > mobileColumnRowPair.x;
    }

    private boolean above(Point targetColumnRowPair, Point mobileColumnRowPair) {
        return targetColumnRowPair.y < mobileColumnRowPair.y && targetColumnRowPair.x == mobileColumnRowPair.x;
    }

    private boolean below(Point targetColumnRowPair, Point mobileColumnRowPair) {
        return targetColumnRowPair.y > mobileColumnRowPair.y && targetColumnRowPair.x == mobileColumnRowPair.x;
    }

    private boolean right(Point targetColumnRowPair, Point mobileColumnRowPair) {
        return targetColumnRowPair.y == mobileColumnRowPair.y && targetColumnRowPair.x > mobileColumnRowPair.x;
    }

    private boolean left(Point targetColumnRowPair, Point mobileColumnRowPair) {
        return targetColumnRowPair.y == mobileColumnRowPair.y && targetColumnRowPair.x < mobileColumnRowPair.x;
    }

    private interface SwitchCellAnimator {
        void animateSwitchCell(final int originalPosition, final int targetPosition);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void animateReorder(final int oldPosition, final int newPosition) {

        boolean isForward = newPosition > oldPosition;
        ArrayList<Animator> animatorArrayList = new ArrayList<>();
        if (isForward) {
            //往后移动
            for (int pos = oldPosition; pos < newPosition; pos++) {
                View view = getViewForId(getItemId(pos));
                if ((pos + 1) % getColumnCount() == 0) {
                    //如果是最后一列的元素，那么动画效果是从下面一行最左边移动到上面一行最右边，那么动画的参数就是如下，也就是动画开始的位置是在下面一行最左，动画结束位置是在上面一行最优
                    //startX和startY分别表示动画开始时的偏移量，也就是下面一行最左
                    //这样设置参数的目的是动画结束后其实是没有变位置的
                    animatorArrayList.add(createTranslationAnimations(view, -view.getWidth() * (getColumnCount() - 1), 0,
                            view.getHeight(), 0));
                } else {
                    animatorArrayList.add(createTranslationAnimations(view, view.getWidth(), 0, 0, 0));
                }
            }
        } else {
            for (int pos = oldPosition; pos > newPosition; pos--) {
                View view = getViewForId(getItemId(pos));
                //当view是第一列的元素时，动画的效果就是往左下移动，这样写的目的也是动画结束后实际上没有变位置
                if ((pos + getColumnCount()) % getColumnCount() == 0) {
                    animatorArrayList.add(createTranslationAnimations(view, view.getWidth() * (getColumnCount() - 1), 0,
                            -view.getHeight(), 0));
                } else {
                    animatorArrayList.add(createTranslationAnimations(view, -view.getWidth(), 0, 0, 0));
                }
            }
        }
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(animatorArrayList);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.setDuration(MOVE_DURATION);
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mReorderAnimation = true;
                updateEnableState();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mReorderAnimation = false;
                updateEnableState();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animatorSet.start();
    }

    private void updateEnableState() {
        setEnabled(!mHoverAnimation && !mReorderAnimation);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private Animator createTranslationAnimations(View view, float startX, float endX, float startY, float endY) {
        ObjectAnimator animX = ObjectAnimator.ofFloat(view, "translationX", startX, endX);
        ObjectAnimator animY = ObjectAnimator.ofFloat(view, "translationY", startY, endY);
        AnimatorSet animSetXY = new AnimatorSet();
        animSetXY.playTogether(animX, animY);
        return animSetXY;
    }

    private class LSwitchCellAnimator implements SwitchCellAnimator {

        private int mDeltaY;
        private int mDeltaX;

        public LSwitchCellAnimator(int deltaX, int deltaY) {
            mDeltaX = deltaX;
            mDeltaY = deltaY;
        }

        @Override
        public void animateSwitchCell(final int originalPosition, final int targetPosition) {
            getViewTreeObserver().addOnPreDrawListener(new AnimateSwitchViewOnPreDrawListener(originalPosition, targetPosition));
        }

        private class AnimateSwitchViewOnPreDrawListener implements ViewTreeObserver.OnPreDrawListener {
            private final int mOriginalPosition;
            private final int mTargetPosition;

            AnimateSwitchViewOnPreDrawListener(final int originalPosition, final int targetPosition) {
                mOriginalPosition = originalPosition;
                mTargetPosition = targetPosition;
            }

            @Override
            public boolean onPreDraw() {
                getViewTreeObserver().removeOnPreDrawListener(this);
                mTotalOffsetY += mDeltaY;
                mTotalOffsetX += mDeltaX;
                animateReorder(mOriginalPosition, mTargetPosition);
                //因为是hasStableIds()方法为true,mMobileItemId不变但对应的mMobileView变了
                mMobileView.setVisibility(View.VISIBLE);
                mMobileView = getViewForId(mMobileItemId);
                mMobileView.setVisibility(View.INVISIBLE);
                return true;
            }
        }
    }

    private void handleMobileCellScroll() {
        mIsMobileScrolling = handleMobileCellScroll(mHoverCellCurrentBounds);
    }

    private boolean handleMobileCellScroll(Rect rect) {
        //滚动条的长度
        int extent = computeVerticalScrollExtent();
        //上下滚动的范围
        int range = computeVerticalScrollRange();
        //滚动的偏移量
        int offset = computeVerticalScrollOffset();

        int top = rect.top;
        int hoverHeight = rect.height();

        int height = getHeight();

        if (top < 0 && offset > 0) {
            smoothScrollBy(-mSmoothScrollAmountAtEdge, 0);
            return true;
        }

        if (top + hoverHeight > height && (offset + extent < range)) {
            smoothScrollBy(mSmoothScrollAmountAtEdge, 0);
            return true;
        }
        return false;
    }

    /**
     * 该listener用来处理当cell滑动到边界引起滚动时的Item交换
     */
    private OnScrollListener mScrollListener = new OnScrollListener() {

        private int mPreviousFirstVisibleItem = -1;
        private int mPreviousVisibleItemCount = -1;
        private int mCurrentFirstVisibleItem;
        private int mCurrentVisibleItemCount;
        private int mCurrentScrollState;

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            mCurrentScrollState = scrollState;
            mScrollState = scrollState;
            isScrollCompleted();
        }

        private void isScrollCompleted() {
            if (mCurrentVisibleItemCount > 0 && mCurrentScrollState == SCROLL_STATE_IDLE) {
                if (mCellIsMobile && mIsMobileScrolling) {
                    handleMobileCellScroll();
                } else if (mIsWaitingForScrollFinish) {
                    touchEventsEnded();
                }
            }
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            mCurrentFirstVisibleItem = firstVisibleItem;
            mCurrentVisibleItemCount = visibleItemCount;

            mPreviousFirstVisibleItem = (mPreviousFirstVisibleItem == -1) ? mCurrentFirstVisibleItem
                    : mPreviousFirstVisibleItem;
            mPreviousVisibleItemCount = (mPreviousVisibleItemCount == -1) ? mCurrentVisibleItemCount
                    : mPreviousVisibleItemCount;

            checkAndHandleFirstVisibleCellChange();
            checkAndHandleLastVisibleCellChange();

            mPreviousFirstVisibleItem = mCurrentFirstVisibleItem;
            mPreviousVisibleItemCount = mCurrentVisibleItemCount;
        }

        //因为滑动时gridview的position什么的都变了，所以需要重新调用updateNeighborViewsForId方法
        public void checkAndHandleFirstVisibleCellChange() {
            if (mCurrentFirstVisibleItem != mPreviousFirstVisibleItem) {
                if (mCellIsMobile && mMobileItemId != INVALID_ID) {
                    updateNeighborViewsForId(mMobileItemId);
                    handleCellSwitch();
                }
            }
        }

        public void checkAndHandleLastVisibleCellChange() {
            int currentLastVisibleItem = mCurrentFirstVisibleItem + mCurrentVisibleItemCount;
            int previousLastVisibleItem = mPreviousFirstVisibleItem + mPreviousVisibleItemCount;
            if (currentLastVisibleItem != previousLastVisibleItem) {
                if (mCellIsMobile && mMobileItemId != INVALID_ID) {
                    updateNeighborViewsForId(mMobileItemId);
                    handleCellSwitch();
                }
            }
        }
    };

    private boolean isPostHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }
}

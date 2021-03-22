package com.example.control_panel;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Scroller;

public class ModeSwitchScrollView extends LinearLayout {
    private static String TAG = ModeSwitchScrollView.class.getSimpleName();
    private static final int INVALID_POINTER = -1;
    private Context mContext;
    private Scroller mScroller;
    private int mSelectedIndex = 0;
    private int mTouchSlop;
    private int mLastMotionX;
    private int mLastMotionY;
    private int mActivePointerId = INVALID_POINTER;
    private boolean mIsDoAction = false;
    private SelectListener mSelectListener;
    private Integer[] mWidths;
    boolean mLayoutSuccess = false;
    private ModeSwitchAdapter mAdapter;
    private ImageView mModeSwitchBarPoint;
    private boolean mIsEnableSwitchMode;

    public interface SelectListener {
        void onSelect(int beforePosition, int position);
    }

    public ModeSwitchScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public void setSelectListener(SelectListener selectListener) {
        this.mSelectListener = selectListener;
    }

    public void setAdapter(ModeSwitchAdapter adapter, ImageView modeSwitchBarPoint) {
        this.mAdapter = adapter;
        this.mModeSwitchBarPoint = modeSwitchBarPoint;
        if (this.mAdapter == null) {
            return;
        }
        mWidths = new Integer[this.mAdapter.getCount()];
        addViews();
    }

    public void setEnableSwitchMode(boolean enable) {
        this.mIsEnableSwitchMode = enable;
    }

    private void init(Context context) {
        mContext = context;
        mScroller = new Scroller(context, new DecelerateInterpolator());
        setOrientation(LinearLayout.HORIZONTAL);
        final ViewConfiguration configuration = ViewConfiguration.get(mContext);
        mTouchSlop = configuration.getScaledTouchSlop();
    }

    private void addViews() {
        if (mAdapter == null) {
            return;
        }
        for (int i = 0; i < mAdapter.getCount(); i++) {
            View view = mAdapter.getPositionView(i, this, LayoutInflater.from(mContext));
            final int index = i;
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    moveToPoint(index);
                }
            });
            addView(view);
        }
    }

    public void setDefaultSelectedIndex(int selectedIndex) {
        this.mSelectedIndex = selectedIndex;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return scrollEvent(ev) || super.dispatchTouchEvent(ev);
    }

    private boolean scrollEvent(MotionEvent ev) {
        if (!mIsEnableSwitchMode) {
            mActivePointerId = INVALID_POINTER;
        }
        final int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionX = (int) ev.getX();
                mLastMotionY = (int) ev.getY();
                mActivePointerId = ev.getPointerId(0);
                mIsDoAction = false;
                setPointAnimation(mModeSwitchBarPoint, true);
                return !super.dispatchTouchEvent(ev);
            case MotionEvent.ACTION_MOVE:
                final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
                if (activePointerIndex == -1) {
                    break;
                }

                final int x = (int) ev.getX(activePointerIndex);
                final int y = (int) ev.getY(activePointerIndex);
                int deltaX = mLastMotionX - x;
                int deltaY = mLastMotionY - y;
                int absDeltaX = Math.abs(deltaX);
                int absDeltaY = Math.abs(deltaY);
                if (!mIsDoAction && absDeltaX > mTouchSlop && absDeltaX > absDeltaY) {
                    mIsDoAction = true;
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                    if (deltaX > 0) {
                        moveRight();
                    } else {
                        moveLeft();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                setPointAnimation(mModeSwitchBarPoint, false);
                break;
            case MotionEvent.ACTION_CANCEL:
                mActivePointerId = INVALID_POINTER;
                break;
        }
        return mIsDoAction;
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            invalidate();
        }
        super.computeScroll();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (mLayoutSuccess) {
            return;
        }
        mLayoutSuccess = true;
        int childCount = getChildCount();
        int childLeft;
        int childRight;
        int selectedMode = mSelectedIndex;
        int widthOffset = 0;
        for (int i = 0; i < childCount; i++) {
            View childView = getChildAt(i);
            if (i < selectedMode) {
                widthOffset += childView.getMeasuredWidth();
            }
        }
        for (int i = 0; i < childCount; i++) {
            View childView = getChildAt(i);
            mWidths[i] = childView.getMeasuredWidth();
            if (i != 0) {
                View preView = getChildAt(i - 1);
                childLeft = preView.getRight();
                childRight = childLeft + childView.getMeasuredWidth();
            } else {
                childLeft = (getWidth() - getChildAt(selectedMode).getMeasuredWidth()) / 2 - widthOffset;
                childRight = childLeft + childView.getMeasuredWidth();
            }
            childView.layout(childLeft, childView.getTop(), childRight, childView.getMeasuredHeight());
            initChildView(childView);
        }

        selectView(getChildAt(selectedMode));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(MeasureSpec
                .getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
    }

    public void moveLeft() {
        moveToPoint(mSelectedIndex - 1);
    }

    public void moveRight() {
        moveToPoint(mSelectedIndex + 1);
    }

    // TODO: Shyam Fix move logic to be generic
    private void moveToPoint(int index) {
        if (!mIsEnableSwitchMode) {
            Log.d(TAG, "In current state, can not switch mode");
            return;
        }
        if (mAdapter == null) {
            return;
        }
        if (index < 0 || index >= mAdapter.getCount() || index == mSelectedIndex) {
            return;
        }
        int mBeforeIndex = mSelectedIndex;
        View toView = getChildAt(index);
        int[] screens = new int[2];
        toView.getLocationOnScreen(screens);
        int[] left = new int[2];
        getLocationOnScreen(left);
        int mLeft = left[0];

        int moveSize = 0;
        int scrollX = 0;
        View childView = getChildAt(0);
        int moveBaseSize = childView.getWidth();
        if (mBeforeIndex > index) {
            // to right -value
            if (mBeforeIndex == 0){
                scrollX = -moveBaseSize * 2;
                if (index ==1) {
                    moveSize = moveBaseSize;
                } else if (index == 2) {
                    moveSize = moveBaseSize * 2;
                } else if (index == 3) {
                    moveSize = moveBaseSize * 3;
                } else if (index == 4) {
                    moveSize = moveBaseSize * 4;
                }
            } else if (mBeforeIndex == 1) {
                scrollX = -moveBaseSize;
                if (index ==0) {
                    moveSize = -moveBaseSize;
                } else if (index == 2) {
                    moveSize = moveBaseSize;
                } else if (index == 3) {
                    moveSize = moveBaseSize * 2;
                } else if (index == 4) {
                    moveSize = moveBaseSize * 3;
                }
            } else if (mBeforeIndex == 2) {
                scrollX =  0;
                if (index ==0) {
                    moveSize = -moveBaseSize * 2;
                } else if (index == 1) {
                    moveSize = -moveBaseSize;
                } else if (index == 3) {
                    moveSize = moveBaseSize;
                } else if (index == 4) {
                    moveSize = moveBaseSize * 2;
                }
            } else if (mBeforeIndex == 3) {
                scrollX = moveBaseSize;
                if (index ==0) {
                    moveSize = -moveBaseSize * 3;
                } else if (index == 1) {
                    moveSize = -moveBaseSize * 2;
                } else if (index == 2) {
                    moveSize = -moveBaseSize;
                } else if (index == 4) {
                    moveSize = moveBaseSize;
                }
            } else if (mBeforeIndex == 4) {
                scrollX = moveBaseSize * 2;
                if (index ==0) {
                    moveSize = -moveBaseSize * 4;
                } else if (index == 1) {
                    moveSize = -moveBaseSize * 3;
                } else if (index == 2) {
                    moveSize = -moveBaseSize * 2;
                } else if (index == 3) {
                    moveSize = -moveBaseSize;
                }
            }
        } else {
            // to left +value
            if (mBeforeIndex == 0){
                scrollX = -moveBaseSize * 2;
                if (index ==1) {
                    moveSize = moveBaseSize;
                } else if (index == 2) {
                    moveSize = moveBaseSize * 2;
                } else if (index == 3) {
                    moveSize = moveBaseSize * 3;
                } else if (index == 4) {
                    moveSize = moveBaseSize * 4;
                }
            } else if (mBeforeIndex == 1) {
                scrollX = -moveBaseSize;
                if (index ==0) {
                    moveSize = -moveBaseSize;
                } else if (index == 2) {
                    moveSize = moveBaseSize;
                } else if (index == 3) {
                    moveSize = moveBaseSize * 2;
                } else if (index == 4) {
                    moveSize = moveBaseSize * 3;
                }
            } else if (mBeforeIndex == 2) {
                scrollX = 0;
                if (index ==0) {
                    moveSize = -moveBaseSize * 2;
                } else if (index == 1) {
                    moveSize = -moveBaseSize;
                } else if (index == 3) {
                    moveSize = moveBaseSize;
                } else if (index == 4) {
                    moveSize = moveBaseSize * 2;
                }
            } else if (mBeforeIndex == 3) {
                scrollX = moveBaseSize;
                if (index ==0) {
                    moveSize = -moveBaseSize * 3;
                } else if (index == 1) {
                    moveSize = -moveBaseSize * 1;
                } else if (index == 2) {
                    moveSize = -moveBaseSize;
                } else if (index == 4) {
                    moveSize = moveBaseSize;
                }
            } else if (mBeforeIndex == 4) {
                scrollX = moveBaseSize * 2;
                if (index ==0) {
                    moveSize = -moveBaseSize * 4;
                } else if (index == 1) {
                    moveSize = -moveBaseSize * 3;
                } else if (index == 2) {
                    moveSize = -moveBaseSize * 2;
                } else if (index == 3) {
                    moveSize = -moveBaseSize;
                }
            }
        }

        Log.d(TAG, "moveSize: " + moveSize);
        Log.d(TAG, "scrollX: " + getScrollX());
        int mDuration = 320;
        mScroller.startScroll(scrollX, 0, moveSize, 0, mDuration);
        scrollToNext(mBeforeIndex, index);
        mSelectedIndex = index;
        invalidate();
    }

    private void scrollToNext(int lastIndex, int selectIndex) {
        if (mAdapter == null) {
            return;
        }
        Log.d(TAG, "lastIndex: "+lastIndex+", selectIndex: "+selectIndex);
        View preView = getChildAt(lastIndex);
        if (preView != null) {
            mAdapter.preView(preView);
        }
        View selectView = getChildAt(selectIndex);
        if (selectView != null) {
            mAdapter.selectView(selectView);
        }
        if (mSelectListener != null) {
            mSelectListener.onSelect(lastIndex, selectIndex);
        }
    }

    private void selectView(View view) {
        if (mAdapter == null || view == null) {
            return;
        }
        mAdapter.selectView(view);
    }

    private void initChildView(View view) {
        if (mAdapter == null || view == null) {
            return;
        }
        mAdapter.initView(view);
    }

    public void runModeSwitchPointAnimation() {
        setPointAnimation(mModeSwitchBarPoint, false);
    }

    private void setPointAnimation(View view, boolean isSelect) {
        int fromAlpha;
        int toAlpha;
        int animationTime = 1000;

        AnimationSet animationSet = new AnimationSet(true);
        animationSet.setRepeatMode(Animation.RESTART);
        animationSet.setRepeatCount(1);
        animationSet.setFillAfter(true);
        if (isSelect) {
            fromAlpha = 1;
            toAlpha = -1;
        } else {
            fromAlpha = -1;
            toAlpha = 1;
        }
        Animation alphaAnimation = new AlphaAnimation(fromAlpha, toAlpha);
        alphaAnimation.setDuration(animationTime);
        animationSet.addAnimation(alphaAnimation);
        view.startAnimation(animationSet);
    }

}
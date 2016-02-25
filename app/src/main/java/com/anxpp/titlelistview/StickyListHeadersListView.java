package com.anxpp.titlelistview;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.SectionIndexer;

import com.anxpp.titlelistview.WrapperViewList.LifeCycleListener;

/**
 * 尽管这是一个FrameLayout类,我们仍认为这是一个ListView。
 * 原因如下：
 *   1. 它的行为像ListView。
 *   2. 它曾经是一个ListView子类和重构的名字会引起兼容性错误。
 *
 * @author anxpp.com
 */
public class StickyListHeadersListView extends FrameLayout {

    /* 子视图 */
    //带标题的列表
    private WrapperViewList wrapperViewList;
    //头布局
    private View mHeader;

    /* --- Header 的一些参数 --- */
    //id
    private Long mHeaderId;
    //位置
    private Integer mHeaderPosition;
    //偏移量
    private Integer mHeaderOffset;

    /* --- 成员 --- */
    private AdapterWrapper mAdapter;

    /* --- 设置 --- */
    //是否固定到顶部
    private boolean mAreHeadersSticky = true;
    private boolean mClippingToPadding = true;
    private boolean mIsDrawingListUnderStickyHeader = true;
    private int mPaddingLeft = 0;
    private int mPaddingTop = 0;
    private int mPaddingRight = 0;
    private int mPaddingBottom = 0;

    private AdapterWrapperDataSetObserver mDataSetObserver;

    public StickyListHeadersListView(Context context) {
        this(context, null);
    }

    public StickyListHeadersListView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.stickyListHeadersListViewStyle);
    }

    public StickyListHeadersListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // 初始化封装的列表视图
        wrapperViewList = new WrapperViewList(context);

        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs,R.styleable.StickyListHeadersListView, defStyle, 0);

            try {
                // -- 配置的视图属性 --
                int padding = a.getDimensionPixelSize(R.styleable.StickyListHeadersListView_android_padding, 0);
                mPaddingLeft = a.getDimensionPixelSize(R.styleable.StickyListHeadersListView_android_paddingLeft, padding);
                mPaddingTop = a.getDimensionPixelSize(R.styleable.StickyListHeadersListView_android_paddingTop, padding);
                mPaddingRight = a.getDimensionPixelSize(R.styleable.StickyListHeadersListView_android_paddingRight, padding);
                mPaddingBottom = a.getDimensionPixelSize(R.styleable.StickyListHeadersListView_android_paddingBottom, padding);

                setPadding(mPaddingLeft, mPaddingTop, mPaddingRight, mPaddingBottom);

                // Set clip to padding on the list and reset value to default on
                // wrapper
                mClippingToPadding = a.getBoolean(R.styleable.StickyListHeadersListView_android_clipToPadding, true);
                super.setClipToPadding(true);
                wrapperViewList.setClipToPadding(mClippingToPadding);

                // 滚动条
                final int scrollBars = a.getInt(R.styleable.StickyListHeadersListView_android_scrollbars, 0x00000200);
                wrapperViewList.setVerticalScrollBarEnabled((scrollBars & 0x00000200) != 0);
                wrapperViewList.setHorizontalScrollBarEnabled((scrollBars & 0x00000100) != 0);

                // overscroll
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    wrapperViewList.setOverScrollMode(a.getInt(R.styleable.StickyListHeadersListView_android_overScrollMode, 0));
                }

                // -- ListView的属性 --
                wrapperViewList.setFadingEdgeLength(a.getDimensionPixelSize(R.styleable.StickyListHeadersListView_android_fadingEdgeLength,
                        wrapperViewList.getVerticalFadingEdgeLength()));
                final int fadingEdge = a.getInt(R.styleable.StickyListHeadersListView_android_requiresFadingEdge, 0);
                if (fadingEdge == 0x00001000) {
                    wrapperViewList.setVerticalFadingEdgeEnabled(false);
                    wrapperViewList.setHorizontalFadingEdgeEnabled(true);
                } else if (fadingEdge == 0x00002000) {
                    wrapperViewList.setVerticalFadingEdgeEnabled(true);
                    wrapperViewList.setHorizontalFadingEdgeEnabled(false);
                } else {
                    wrapperViewList.setVerticalFadingEdgeEnabled(false);
                    wrapperViewList.setHorizontalFadingEdgeEnabled(false);
                }
                wrapperViewList.setCacheColorHint(a
                        .getColor(R.styleable.StickyListHeadersListView_android_cacheColorHint, wrapperViewList.getCacheColorHint()));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    wrapperViewList.setChoiceMode(a.getInt(R.styleable.StickyListHeadersListView_android_choiceMode,
                            wrapperViewList.getChoiceMode()));
                }
                wrapperViewList.setDrawSelectorOnTop(a.getBoolean(R.styleable.StickyListHeadersListView_android_drawSelectorOnTop, false));
                wrapperViewList.setFastScrollEnabled(a.getBoolean(R.styleable.StickyListHeadersListView_android_fastScrollEnabled,
                        wrapperViewList.isFastScrollEnabled()));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    wrapperViewList.setFastScrollAlwaysVisible(a.getBoolean(
                            R.styleable.StickyListHeadersListView_android_fastScrollAlwaysVisible,
                            wrapperViewList.isFastScrollAlwaysVisible()));
                }

                //设置滚动条无边框
//                wrapperViewList.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
//                wrapperViewList.setSmoothScrollbarEnabled(false);

                if (a.hasValue(R.styleable.StickyListHeadersListView_android_listSelector)) {
                    wrapperViewList.setSelector(a.getDrawable(R.styleable.StickyListHeadersListView_android_listSelector));
                }

                wrapperViewList.setScrollingCacheEnabled(a.getBoolean(R.styleable.StickyListHeadersListView_android_scrollingCache,
                        wrapperViewList.isScrollingCacheEnabled()));
                
                wrapperViewList.setStackFromBottom(a.getBoolean(R.styleable.StickyListHeadersListView_android_stackFromBottom, false));

                wrapperViewList.setTranscriptMode(a.getInt(R.styleable.StickyListHeadersListView_android_transcriptMode,
                        ListView.TRANSCRIPT_MODE_DISABLED));

                // -- StickyListHeaders attributes --
                mAreHeadersSticky = a.getBoolean(R.styleable.StickyListHeadersListView_hasStickyHeaders, true);
                mIsDrawingListUnderStickyHeader = a.getBoolean(R.styleable.StickyListHeadersListView_isDrawingListUnderStickyHeader, true);
            } finally {
                a.recycle();
            }
        }

        // attach some listeners to the wrapped list
        wrapperViewList.setLifeCycleListener(new WrapperViewListLifeCycleListener());

        addView(wrapperViewList);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measureHeader(mHeader);
    }

    private void ensureHeaderHasCorrectLayoutParams(View header) {
        ViewGroup.LayoutParams lp = header.getLayoutParams();
        if (lp == null) {
            lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            header.setLayoutParams(lp);
        } else if (lp.height == LayoutParams.MATCH_PARENT || lp.width == LayoutParams.WRAP_CONTENT) {
            lp.height = LayoutParams.WRAP_CONTENT;
            lp.width = LayoutParams.MATCH_PARENT;
            header.setLayoutParams(lp);
        }
    }

    private void measureHeader(View header) {
        if (header != null) {
            final int width = getMeasuredWidth() - mPaddingLeft - mPaddingRight;
            final int parentWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
            final int parentHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            measureChild(header, parentWidthMeasureSpec, parentHeightMeasureSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        wrapperViewList.layout(0, 0, wrapperViewList.getMeasuredWidth(), getHeight());
        if (mHeader != null) {
            MarginLayoutParams layoutParams = (MarginLayoutParams) mHeader.getLayoutParams();
            int headerTop = layoutParams.topMargin;
            mHeader.layout(mPaddingLeft, headerTop, mHeader.getMeasuredWidth() + mPaddingLeft, headerTop + mHeader.getMeasuredHeight());
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        // Only draw the list here.
        // The header should be drawn right after the lists children are drawn.
        // This is done so that the header is above the list items
        // but below the list decorators (scroll bars etc).
        if (wrapperViewList.getVisibility() == VISIBLE || wrapperViewList.getAnimation() != null)
            drawChild(canvas, wrapperViewList, 0);
    }

    // Reset values tied the header. also remove header form layout
    // This is called in response to the data set or the adapter changing
    private void clearHeader() {
        if (mHeader != null) {
            removeView(mHeader);
            mHeader = null;
            mHeaderId = null;
            mHeaderPosition = null;
            mHeaderOffset = null;

            // reset the top clipping length
            wrapperViewList.setTopClippingLength(0);
            updateHeaderVisibilities();
        }
    }

    private void updateOrClearHeader(int firstVisiblePosition) {
        final int adapterCount = mAdapter == null ? 0 : mAdapter.getCount();
        if (adapterCount == 0 || !mAreHeadersSticky) {
            return;
        }

        final int headerViewCount = wrapperViewList.getHeaderViewsCount();
        int headerPosition = firstVisiblePosition - headerViewCount;
        if (wrapperViewList.getChildCount() > 0) {
            View firstItem = wrapperViewList.getChildAt(0);
            if (firstItem.getBottom() < stickyHeaderTop()) {
                headerPosition++;
            }
        }

        // It is not a mistake to call getFirstVisiblePosition() here.
        // Most of the time getFixedFirstVisibleItem() should be called
        // but that does not work great together with getChildAt()
        final boolean doesListHaveChildren = wrapperViewList.getChildCount() != 0;
        final boolean isFirstViewBelowTop = doesListHaveChildren
                && wrapperViewList.getFirstVisiblePosition() == 0
                && wrapperViewList.getChildAt(0).getTop() >= stickyHeaderTop();
        final boolean isHeaderPositionOutsideAdapterRange = headerPosition > adapterCount - 1
                || headerPosition < 0;
        if (!doesListHaveChildren || isHeaderPositionOutsideAdapterRange || isFirstViewBelowTop) {
            clearHeader();
            return;
        }

        updateHeader(headerPosition);
    }

    private void updateHeader(int headerPosition) {

        // check if there is a new header should be sticky
        if (mHeaderPosition == null || mHeaderPosition != headerPosition) {
            mHeaderPosition = headerPosition;
            final long headerId = mAdapter.getHeaderId(headerPosition);
            if (mHeaderId == null || mHeaderId != headerId) {
                mHeaderId = headerId;
                final View header = mAdapter.getHeaderView(mHeaderPosition, mHeader, this);
                if (mHeader != header) {
                    if (header == null) {
                        throw new NullPointerException("header may not be null");
                    }
                    swapHeader(header);
                }
                ensureHeaderHasCorrectLayoutParams(mHeader);
                measureHeader(mHeader);
                // Reset mHeaderOffset to null ensuring
                // that it will be set on the header and
                // not skipped for performance reasons.
                mHeaderOffset = null;
            }
        }

        int headerOffset = stickyHeaderTop();

        // Calculate new header offset
        // Skip looking at the first view. it never matters because it always
        // results in a headerOffset = 0
        for (int i = 0; i < wrapperViewList.getChildCount(); i++) {
            final View child = wrapperViewList.getChildAt(i);
            final boolean doesChildHaveHeader = child instanceof WrapperView && ((WrapperView) child).hasHeader();
            final boolean isChildFooter = wrapperViewList.containsFooterView(child);
            if (child.getTop() >= stickyHeaderTop() && (doesChildHaveHeader || isChildFooter)) {
                headerOffset = Math.min(child.getTop() - mHeader.getMeasuredHeight(), headerOffset);
                break;
            }
        }

        setHeaderOffet(headerOffset);

        if (!mIsDrawingListUnderStickyHeader) {
            wrapperViewList.setTopClippingLength(mHeader.getMeasuredHeight()
                    + mHeaderOffset);
        }

        updateHeaderVisibilities();
    }

    private void swapHeader(View newHeader) {
        if (mHeader != null) {
            removeView(mHeader);
        }
        mHeader = newHeader;
        addView(mHeader);
        mHeader.setClickable(true);
    }

    // hides the headers in the list under the sticky header.
    // Makes sure the other ones are showing
    private void updateHeaderVisibilities() {
        int top = stickyHeaderTop();
        int childCount = wrapperViewList.getChildCount();
        for (int i = 0; i < childCount; i++) {

            // ensure child is a wrapper view
            View child = wrapperViewList.getChildAt(i);
            if (!(child instanceof WrapperView)) {
                continue;
            }

            // ensure wrapper view child has a header
            WrapperView wrapperViewChild = (WrapperView) child;
            if (!wrapperViewChild.hasHeader()) {
                continue;
            }

            // update header views visibility
            View childHeader = wrapperViewChild.mHeader;
            if (wrapperViewChild.getTop() < top) {
                if (childHeader.getVisibility() != View.INVISIBLE) {
                    childHeader.setVisibility(View.INVISIBLE);
                }
            } else {
                if (childHeader.getVisibility() != View.VISIBLE) {
                    childHeader.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    // Wrapper around setting the header offset in different ways depending on
    // the API version
    @SuppressLint("NewApi")
    private void setHeaderOffet(int offset) {
        if (mHeaderOffset == null || mHeaderOffset != offset) {
            mHeaderOffset = offset;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                mHeader.setTranslationY(mHeaderOffset);
            } else {
                MarginLayoutParams params = (MarginLayoutParams) mHeader.getLayoutParams();
                params.topMargin = mHeaderOffset;
                mHeader.setLayoutParams(params);
            }
        }
    }

    private class AdapterWrapperDataSetObserver extends DataSetObserver {

        @Override
        public void onChanged() {
            clearHeader();
        }

        @Override
        public void onInvalidated() {
            clearHeader();
        }

    }

    //生命周期监听
    private class WrapperViewListLifeCycleListener implements LifeCycleListener {
        @Override
        public void onDispatchDrawOccurred(Canvas canvas) {
            // onScroll is not called often at all before froyo
            // therefor we need to update the header here as well.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
                updateOrClearHeader(wrapperViewList.getFixedFirstVisibleItem());
            }
            if (mHeader != null) {
                if (mClippingToPadding) {
                    canvas.save();
                    canvas.clipRect(0, mPaddingTop, getRight(), getBottom());
                    drawChild(canvas, mHeader, 0);
                    canvas.restore();
                } else {
                    drawChild(canvas, mHeader, 0);
                }
            }
        }
    }

    private int stickyHeaderTop() {
        int mStickyHeaderTopOffset = 0;
        return mStickyHeaderTopOffset + (mClippingToPadding ? mPaddingTop : 0);
    }

    /**
     * Use the method with extreme caution!! Changing any values on the
     * underlying ListView might break everything.
     *
     * @return the ListView backing this view.
     */
    private boolean requireSdkVersion(int versionCode) {
        if (Build.VERSION.SDK_INT < versionCode) {
            Log.e("StickyListHeaders", "Api lvl must be at least "+versionCode+" to call this method");
            return false;
        }
        return true;
    }

	/* ---------- 下面是ListView的代理方法 ---------- */
    //没有用到的已经注释掉了，若要使用，取消注释即可！
    public void setAdapter(StickyListHeadersAdapter adapter) {
        if (adapter == null) {
            if (mAdapter != null) {
                mAdapter.stickyListHeadersAdapter = null;
            }
            wrapperViewList.setAdapter(null);
            clearHeader();
            return;
        }
        if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
        }
        if (adapter instanceof SectionIndexer) {
            mAdapter = new SectionIndexerAdapterWrapper(getContext(), adapter);
        } else {
            mAdapter = new AdapterWrapper(getContext(), adapter);
        }
        mDataSetObserver = new AdapterWrapperDataSetObserver();
        mAdapter.registerDataSetObserver(mDataSetObserver);
        wrapperViewList.setAdapter(mAdapter);
        clearHeader();
    }

    @Override
    public void setOnTouchListener(final OnTouchListener onTouchListener) {
        if (onTouchListener != null) {
            wrapperViewList.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return onTouchListener.onTouch(StickyListHeadersListView.this, event);
                }
            });
        } else {
            wrapperViewList.setOnTouchListener(null);
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public int getOverScrollMode() {
        if (requireSdkVersion(Build.VERSION_CODES.GINGERBREAD)) {
            return wrapperViewList.getOverScrollMode();
        }
        return 0;
    }
    @Override
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public void setOverScrollMode(int mode) {
        if (requireSdkVersion(Build.VERSION_CODES.GINGERBREAD)) {
            if (wrapperViewList != null) {
                wrapperViewList.setOverScrollMode(mode);
            }
        }
    }

    @Override
    public void setOnCreateContextMenuListener(OnCreateContextMenuListener l) {
        wrapperViewList.setOnCreateContextMenuListener(l);
    }
    @Override
    public boolean showContextMenu() {
        return wrapperViewList.showContextMenu();
    }

    @Override
    public void setClipToPadding(boolean clipToPadding) {
        if (wrapperViewList != null) {
            wrapperViewList.setClipToPadding(clipToPadding);
        }
        mClippingToPadding = clipToPadding;
    }
    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        mPaddingLeft = left;
        mPaddingTop = top;
        mPaddingRight = right;
        mPaddingBottom = bottom;

        if (wrapperViewList != null) {
            wrapperViewList.setPadding(left, top, right, bottom);
        }
        super.setPadding(0, 0, 0, 0);
        requestLayout();
    }


    @Override
    public int getPaddingLeft() {
        return mPaddingLeft;
    }
    @Override
    public int getPaddingTop() {
        return mPaddingTop;
    }
    @Override
    public int getPaddingRight() {
        return mPaddingRight;
    }
    @Override
    public int getPaddingBottom() {
        return mPaddingBottom;
    }

    public void setScrollBarStyle(int style) {
        wrapperViewList.setScrollBarStyle(style);
    }
    public int getScrollBarStyle() {
        return wrapperViewList.getScrollBarStyle();
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        if (superState != BaseSavedState.EMPTY_STATE) {
          throw new IllegalStateException("Handling non empty state of parent class is not implemented");
        }
        return wrapperViewList.onSaveInstanceState();
    }
    @Override
    public void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(BaseSavedState.EMPTY_STATE);
        wrapperViewList.onRestoreInstanceState(state);
    }
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public boolean canScrollVertically(int direction) {
        return wrapperViewList.canScrollVertically(direction);
    }
//    public void setTranscriptMode (int mode) {
//        wrapperViewList.setTranscriptMode(mode);
//    }
//    public void setBlockLayoutChildren(boolean blockLayoutChildren) {
//        wrapperViewList.setBlockLayoutChildren(blockLayoutChildren);
//    }
//    public void setStackFromBottom(boolean stackFromBottom) {
//    	wrapperViewList.setStackFromBottom(stackFromBottom);
//    }
//    public boolean isStackFromBottom() {
//    	return wrapperViewList.isStackFromBottom();
//    }
}

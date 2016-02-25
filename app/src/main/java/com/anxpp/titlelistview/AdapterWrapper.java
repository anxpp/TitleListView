package com.anxpp.titlelistview;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.LinkedList;
import java.util.List;

/**
 */
class AdapterWrapper extends BaseAdapter implements StickyListHeadersAdapter {

	StickyListHeadersAdapter stickyListHeadersAdapter;
	private final List<View> mHeaderCache = new LinkedList<>();
	private final Context mContext;

	AdapterWrapper(Context context,
				   StickyListHeadersAdapter delegate) {
		this.mContext = context;
		this.stickyListHeadersAdapter = delegate;
		DataSetObserver mDataSetObserver = new DataSetObserver() {

			@Override
			public void onInvalidated() {
				mHeaderCache.clear();
				AdapterWrapper.super.notifyDataSetInvalidated();
			}

			@Override
			public void onChanged() {
				AdapterWrapper.super.notifyDataSetChanged();
			}
		};
		delegate.registerDataSetObserver(mDataSetObserver);
	}

	@Override
	public boolean areAllItemsEnabled() {
		return stickyListHeadersAdapter.areAllItemsEnabled();
	}

	@Override
	public boolean isEnabled(int position) {
		return stickyListHeadersAdapter.isEnabled(position);
	}

	@Override
	public int getCount() {
		return stickyListHeadersAdapter.getCount();
	}

	@Override
	public Object getItem(int position) {
		return stickyListHeadersAdapter.getItem(position);
	}

	@Override
	public long getItemId(int position) {
		return stickyListHeadersAdapter.getItemId(position);
	}

	@Override
	public boolean hasStableIds() {
		return stickyListHeadersAdapter.hasStableIds();
	}

	@Override
	public int getItemViewType(int position) {
		return stickyListHeadersAdapter.getItemViewType(position);
	}

	@Override
	public int getViewTypeCount() {
		return stickyListHeadersAdapter.getViewTypeCount();
	}

	@Override
	public boolean isEmpty() {
		return stickyListHeadersAdapter.isEmpty();
	}

	/**
	 * Will recycle header from {@link WrapperView} if it exists
	 */
	private void recycleHeaderIfExists(WrapperView wv) {
		View header = wv.mHeader;
		if (header != null) {
			// reset the headers visibility when adding it to the cache
			header.setVisibility(View.VISIBLE);
			mHeaderCache.add(header);
		}
	}

	/**
	 * Get a header view. This optionally pulls a header from the supplied
	 * {@link WrapperView} and will also recycle the divider if it exists.
	 */
	private View configureHeader(WrapperView wv, final int position) {
		View header = wv.mHeader == null ? popHeader() : wv.mHeader;
		header = stickyListHeadersAdapter.getHeaderView(position, header, wv);
		if (header == null) {
			throw new NullPointerException("Header view must not be null.");
		}
		return header;
	}

	private View popHeader() {
		if(mHeaderCache.size() > 0) {
			return mHeaderCache.remove(0);
		}
		return null;
	}

	/** Returns {@code true} if the previous position has the same header ID. */
	private boolean previousPositionHasSameHeader(int position) {
		return position != 0
				&& stickyListHeadersAdapter.getHeaderId(position) == stickyListHeadersAdapter
						.getHeaderId(position - 1);
	}

	@Override
	public WrapperView getView(int position, View convertView, ViewGroup parent) {
		WrapperView wrapperView = (convertView == null) ? new WrapperView(mContext) : (WrapperView) convertView;
		View item = stickyListHeadersAdapter.getView(position, wrapperView.mItem, parent);
		View header = null;
		if (previousPositionHasSameHeader(position)) {
			recycleHeaderIfExists(wrapperView);
		} else {
			header = configureHeader(wrapperView, position);
		}
		wrapperView.update(item, header);
		return wrapperView;
	}
	@Override
	public boolean equals(Object object) {
		return stickyListHeadersAdapter.equals(object);
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		return ((BaseAdapter) stickyListHeadersAdapter).getDropDownView(position, convertView, parent);
	}

	@Override
	public int hashCode() {
		return stickyListHeadersAdapter.hashCode();
	}

	@Override
	public void notifyDataSetChanged() {
		((BaseAdapter) stickyListHeadersAdapter).notifyDataSetChanged();
	}

	@Override
	public void notifyDataSetInvalidated() {
		((BaseAdapter) stickyListHeadersAdapter).notifyDataSetInvalidated();
	}

	@Override
	public String toString() {
		return stickyListHeadersAdapter.toString();
	}

	@Override
	public View getHeaderView(int position, View convertView, ViewGroup parent) {
		return stickyListHeadersAdapter.getHeaderView(position, convertView, parent);
	}

	@Override
	public long getHeaderId(int position) {
		return stickyListHeadersAdapter.getHeaderId(position);
	}

}

package com.anxpp.titlelistview;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

/**
 * 首节点适配器接口
 *
 * @author anxpp.com
 */
public interface StickyListHeadersAdapter extends ListAdapter {

	View getHeaderView(int position, View convertView, ViewGroup parent);

	long getHeaderId(int position);
}

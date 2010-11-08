package com.anoshenko.android.background;

import java.util.ArrayList;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

public class GradientListAdapter implements ListAdapter {

	private Background mBackground;
	private final Context mContext;

	public GradientListAdapter(Context context) {
		mContext = context;
	}

	public void setBackground(Background background) {
		mBackground = background;
	}

	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}

	@Override
	public boolean isEnabled(int position) {
		return true;
	}

	@Override
	public int getCount() {
		int count = GradientType.values().length;
		return count;
	}

	@Override
	public Object getItem(int position) {
		return GradientType.values()[position];
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		GradientView view = null;

		if (convertView == null) {
			view = new GradientView(mContext);
		} else
			view = (GradientView) convertView;

		view.setGradient(mBackground.getColor1(), mBackground.getColor2(),
				GradientType.values()[position], position == mBackground.getGradient().Id);

		return view;
	}

	@Override
	public int getItemViewType(int position) {
		return 0;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	private ArrayList<DataSetObserver> DataSetObserverList = null;

	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		if (DataSetObserverList == null)
			DataSetObserverList = new ArrayList<DataSetObserver>();

		DataSetObserverList.add(observer);
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		if (DataSetObserverList != null) {
			int index = DataSetObserverList.indexOf(observer);

			if (index >= 0 && index < DataSetObserverList.size())
				DataSetObserverList.remove(index);
		}
	}
}

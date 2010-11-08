package com.anoshenko.android.mahjongg;

import java.util.List;
import java.util.Vector;

import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class GameListAdapter implements ListAdapter, OnItemClickListener, OnItemLongClickListener {

	private Vector<DataSetObserver> mDataSetObserverList = new Vector<DataSetObserver>();
	private final List<MahjonggData> mList;
	private Bitmap mDefaultBitmap;
	private final SelectActivity mActivity;

	//--------------------------------------------------------------------------
	public GameListAdapter(SelectActivity activity, List<MahjonggData> list,
			Bitmap default_bitmap, ListView list_view) {
		mActivity = activity;
		mList = list;
		mDefaultBitmap = default_bitmap;

		if (list_view != null) {
			list_view.setAdapter(this);
			list_view.setOnItemClickListener(this);
			list_view.setOnItemLongClickListener(this);
		}
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean isEnabled(int position) {
		return true;
	}

	//--------------------------------------------------------------------------
	@Override
	public int getCount() {
		return mList.size();
	}

	//--------------------------------------------------------------------------
	@Override
	public Object getItem(int position) {
		return mList.get(position);
	}

	//--------------------------------------------------------------------------
	@Override
	public long getItemId(int position) {
		return position;
	}

	//--------------------------------------------------------------------------
	@Override
	public int getViewTypeCount() {
		return 1;
	}

	//--------------------------------------------------------------------------
	@Override
	public int getItemViewType(int position) {
		return 0;
	}

	//--------------------------------------------------------------------------
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = LayoutInflater.from(mActivity).inflate(R.layout.list_item, null);
		}

		MahjonggData data = mList.get(position);
		Bitmap preview_image = data.getPreview();

		ImageView preview = (ImageView) convertView.findViewById(R.id.PreviewImage);

		preview.setImageBitmap(preview_image == null ? mDefaultBitmap : preview_image);

		TextView text = (TextView) convertView.findViewById(R.id.GameName);
		text.setText(data.getName() == null ? mActivity.getString(R.string.nameless) : data.getName());

		return convertView;
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean hasStableIds() {
		return false;
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean isEmpty() {
		return getCount() == 0;
	}

	//--------------------------------------------------------------------------
	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		mDataSetObserverList.add(observer);
	}

	//--------------------------------------------------------------------------
	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		mDataSetObserverList.remove(observer);
	}

	//--------------------------------------------------------------------------
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		mActivity.onItemClick(mList.get(position));
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		mActivity.onItemLongClick(mList.get(position));
		return true;
	}

}

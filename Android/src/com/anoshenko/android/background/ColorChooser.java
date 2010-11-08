package com.anoshenko.android.background;

import java.util.ArrayList;

import com.anoshenko.android.mahjongg.R;

import android.app.Dialog;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListAdapter;

public class ColorChooser implements ListAdapter, AdapterView.OnItemClickListener {
	private ArrayList<DataSetObserver> DataSetObserverList = new ArrayList<DataSetObserver>();
	private Dialog dialog = null;

	private final Context mContext;
	private ColorChoiceResult mResult;
	private final int[] colors = {
		Color.BLACK, Color.DKGRAY, Color.GRAY, Color.WHITE, 0xFF7F0000,
		0xFFFF0000, 0xFFFF7F7F, 0xFFFF6A00, 0xFFFFE97F, 0xFFFFD800,
		0xFF7F6A00, 0xFFB6FF00, 0xFF4CFF00, 0xFF00FF00, 0xFF007F00,
		0xFF00FF90, 0xFF00FFFF, 0xFF007F7F, 0xFF7FC9FF, 0xFF0094FF,
		0xFF0000FF, 0xFF00007F, 0xFF7F92FF, 0xFFA17FFF, 0xFFB200FF,
		0xFF7F006E, 0xFFFF00DC, 0xFFFF00DC, 0xFFFF006E, 0xFF7F0037
	};

	//--------------------------------------------------------------------------
	private ColorChooser(Context context, ColorChoiceResult result) {
		mContext = context;
		mResult = result;
	}

	void setDialog(Dialog dialog) { this.dialog = dialog;}

	//--------------------------------------------------------------------------
	public static void show(Context context, ColorChoiceResult result) {
		ColorChooser chooser = new ColorChooser(context, result);

		Dialog dialog = new Dialog(context);

		dialog.setTitle(context.getResources().getString(R.string.background_title));
		dialog.setContentView(R.layout.color_view);

		GridView view = (GridView) dialog.findViewById(R.id.ColorGrid);
		view.setAdapter(chooser);
		view.setOnItemClickListener(chooser);
		chooser.setDialog(dialog);

		dialog.show();
	}

	//--------------------------------------------------------------------------
	@Override
	public int getCount() {
		return colors.length;
	}

	//--------------------------------------------------------------------------
	@Override
	public Object getItem(int position) {
		return colors[position];
	}

	//--------------------------------------------------------------------------
	@Override
	public long getItemId(int position) {
		return position;
	}

	//--------------------------------------------------------------------------
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ColorView view = null;

		if (convertView == null) {
			view = new ColorView(mContext);
		} else
			view = (ColorView) convertView;

		view.setColor(colors[position]);

		return view;
	}

	//--------------------------------------------------------------------------
	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		DataSetObserverList.add(observer);
	}

	//--------------------------------------------------------------------------
	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		int index = DataSetObserverList.indexOf(observer);

		if (index >= 0 && index < DataSetObserverList.size())
			DataSetObserverList.remove(index);
	}

	//--------------------------------------------------------------------------
	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		if (mResult != null) {
			mResult.setColor(colors[position]);
			dialog.dismiss();
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
	public int getItemViewType(int position) {
		return 0;
	}

	//--------------------------------------------------------------------------
	@Override
	public int getViewTypeCount() {
		return 1;
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean hasStableIds() {
		return true;
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean isEmpty() {
		return false;
	}
}

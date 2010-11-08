package com.anoshenko.android.mahjongg;

import java.util.Vector;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class PopupMenu implements ListAdapter, OnItemClickListener {

	//--------------------------------------------------------------------------
	public interface Listener {
		public void onPopupMenuSelect(int command);
	}

	//--------------------------------------------------------------------------
	private class PopupMenuItem {
		final int Command;
		final String Text;
		final Bitmap Icon;
		final boolean Enabled;

		PopupMenuItem(int command, int text_id, int icon_id, boolean enabled) {
			Resources res = mContext.getResources();

			Command = command;
			Text = res.getString(text_id);
			Icon = BitmapFactory.decodeResource(res, icon_id);
			Enabled = enabled;
		}
	}

	//--------------------------------------------------------------------------
	private final Context mContext;
	private final Listener mListener;
	private Vector<PopupMenuItem> mItems = new Vector<PopupMenuItem>();
	private AlertDialog mDialog;
	private String mTitle;
	private boolean mHasDisabled = false;

	//--------------------------------------------------------------------------
	public PopupMenu(Context context, Listener listener) {
		mContext = context;
		mListener = listener;
	}

	//--------------------------------------------------------------------------
	public void addItem(int command, int text_id, int icon_id) {
		mItems.add(new PopupMenuItem(command, text_id, icon_id, true));
	}

	//--------------------------------------------------------------------------
	public void addItem(int command, int text_id, int icon_id, boolean enabled) {
		mItems.add(new PopupMenuItem(command, text_id, icon_id, enabled));
		if (!enabled)
			mHasDisabled = true;
	}

	//--------------------------------------------------------------------------
	public void setTitle(String title) {
		mTitle = title;
	}

	//--------------------------------------------------------------------------
	public void setTitle(int title_id) {
		mTitle = mContext.getResources().getString(title_id);
	}

	//--------------------------------------------------------------------------
	public void show() {
		AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
		dialog.setCancelable(true);

		ListView list = new ListView(mContext);
		list.setLayoutParams(new LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		list.setAdapter(this);
		list.setOnItemClickListener(this);
		dialog.setView(list);

		if (mTitle != null)
			dialog.setTitle(mTitle);

		mDialog = dialog.show();
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean areAllItemsEnabled() {
		return !mHasDisabled;
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean isEnabled(int position) {
		return mItems.get(position).Enabled;
	}

	//--------------------------------------------------------------------------
	@Override
	public int getCount() {
		return mItems.size();
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean isEmpty() {
		return getCount() == 0;
	}

	//--------------------------------------------------------------------------
	@Override
	public Object getItem(int position) {
		return mItems.get(position);
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
	public View getView(int position, View view, ViewGroup group) {
		if (view == null) {
			view = LayoutInflater.from(mContext).inflate(R.layout.menu_item, null);
		}

		PopupMenuItem item = mItems.get(position);

		ImageView icon = (ImageView) view.findViewById(R.id.MenuItemIcon);
		icon.setImageBitmap(item.Icon);

		TextView text = (TextView) view.findViewById(R.id.MenuItemText);
		text.setText(item.Text);
		text.setTextColor(item.Enabled ? 0xFFFFFFFF : 0xFF808080);

		return view;
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean hasStableIds() {
		return true;
	}

	//--------------------------------------------------------------------------
	private Vector<DataSetObserver> mDataSetObservers = new Vector<DataSetObserver>();

	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		mDataSetObservers.add(observer);
	}

	//--------------------------------------------------------------------------
	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		mDataSetObservers.remove(observer);
	}

	//--------------------------------------------------------------------------
	@Override
	public void onItemClick(AdapterView<?>  parent, View  view, int position, long id) {
		mDialog.dismiss();
		if (mListener != null)
			mListener.onPopupMenuSelect(mItems.get(position).Command);
	}
}

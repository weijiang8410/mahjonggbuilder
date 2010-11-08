package com.anoshenko.android.background;

import java.util.ArrayList;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

public class BuildinImageAdapter implements ListAdapter {
	private ArrayList<DataSetObserver> DataSetObserverList = new ArrayList<DataSetObserver>();
	private Bitmap[] samples;

	private final static int SAMPLE_WIDTH = 90, SAMPLE_HEIGHT = 80;
	private final Context mContext;
	private final int[] BuildinImageId;
	private final Background mBackground;

	//--------------------------------------------------------------------------
	public BuildinImageAdapter(Context context, int[] image_ids, Background background) {
		mContext = context;
		BuildinImageId = image_ids;
		mBackground = background;

		samples = new Bitmap[Background.BUILDIN_IDS.length];

		for (int j = 0; j < samples.length; j++) {
			samples[j] = Bitmap.createBitmap(SAMPLE_WIDTH, SAMPLE_HEIGHT,
					Bitmap.Config.ARGB_8888);

			Canvas g = new Canvas(samples[j]);
			Bitmap image = BitmapFactory.decodeResource(context.getResources(), BuildinImageId[j]);

			int ih = image.getHeight();
			int iw = image.getWidth();
			Rect src = new Rect();
			Rect dst = new Rect();

			Paint paint = new Paint();

			for (int i = 0; i < SAMPLE_HEIGHT; i += ih) {
				if (i + ih > SAMPLE_HEIGHT)
					ih = SAMPLE_HEIGHT - i;

				for (int k = 0; k < SAMPLE_WIDTH; k += iw) {
					src.set(0, 0, k + iw <= SAMPLE_WIDTH ? iw : SAMPLE_WIDTH - k, ih);
					dst.set(k, i, k + src.width(), i + ih);
					g.drawBitmap(image, src, dst, paint);
				}
			}
		}
	}

	//--------------------------------------------------------------------------
	@Override
	public int getCount() {
		return samples.length;
	}

	//--------------------------------------------------------------------------
	@Override
	public Object getItem(int position) {
		return null;
	}

	//--------------------------------------------------------------------------
	@Override
	public long getItemId(int position) {
		return position;
	}

	//--------------------------------------------------------------------------
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		SelectedImageView view = null;

		if (convertView == null) {
			view = new SelectedImageView(mContext);
		} else
			view = (SelectedImageView) convertView;

		view.setImageBitmap(samples[position]);
		view.setSelected(mBackground.getImageNumber() == position);

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

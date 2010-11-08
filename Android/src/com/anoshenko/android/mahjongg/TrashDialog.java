package com.anoshenko.android.mahjongg;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;

public class TrashDialog {

	public final static void show(PlayActivity activity, int[] trash) {
		DisplayMetrics dm = Utils.getDisplayMetrics(activity);
		int width = dm.widthPixels * 5 / 6;
		int side_wall = Math.min(dm.heightPixels, dm.widthPixels) >= 480 ? 4 : 3;
		int die_width = width / (dm.heightPixels < dm.widthPixels ? 14 : 9);
		int die_height = die_width * activity.mDieImage[0].getHeight() /
				activity.mDieImage[0].getWidth();
		Bitmap die = Utils.createDieImage(die_width, die_height, side_wall);
		int height = die_height + side_wall;
		int x = 0, y, w, count;

		for (int i=0; i<trash.length; i++) {
			w = 2*die_width + side_wall;
			if (i+1 < trash.length && trash[i+1] == trash[i]) {
				w += 2*die_width;
				i++;
			}

			if (x > 0)
				x += side_wall;

			if (x + w > width) {
				height += die_height + 2*side_wall;
				x = w;
			} else {
				x += w;
			}
		}

		Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas g = new Canvas(image);
		Paint paint = new Paint();

		paint.setAntiAlias(true);
		paint.setFilterBitmap(true);
		paint.setDither(true);
		g.drawARGB(0, 0, 0, 0);

		Rect image_rect = new Rect(0, 0, activity.mDieImage[0].getWidth(),
				activity.mDieImage[0].getHeight());
		Rect dst_rect = new Rect();

		x = y = 0;

		for (int i=0; i<trash.length; i++) {
			if (i+1 < trash.length && trash[i+1] == trash[i]) {
				count = 4;
				i++;
			} else {
				count = 2;
			}

			w = count*die_width + side_wall;

			if (x > 0)
				x += side_wall;

			if (x + w > width) {
				y += die_height + 2*side_wall;
				x = 0;
			}

			for (int k=count-1; k>=0; k--) {
				g.drawBitmap(die, x + k*die_width, y, paint);
				dst_rect.set(x + k*die_width + side_wall, y + 1,
						x + (k+1)*die_width + side_wall - 1, y + die_height);
				g.drawBitmap(activity.mDieImage[trash[i]], image_rect, dst_rect, paint);
			}

			x += w;
		}

		AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
		View view = activity.getLayoutInflater().inflate(R.layout.trash_view, null);
		ImageView image_view = (ImageView)view.findViewById(R.id.TrashImageView);
		image_view.setImageBitmap(image);

		dialog.setView(view);
		dialog.setNeutralButton(android.R.string.ok, null);
		dialog.show();
	}
}

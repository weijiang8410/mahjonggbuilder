package com.anoshenko.android.mahjongg;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class Utils {

	public final static int DIE_COUNT = 36;

	private final static int DIE_FILL_COLOR		= 0xFFE0E0E0;
	private final static int DIE_BORDER_COLOR	= 0xFF808080;
	private final static int DIE_SIDE_COLOR		= 0xFFB0B0B0;
	private final static int DIE_ANGLE_COLOR	= 0xFFC0C0C0;

	//--------------------------------------------------------------------------
	static public Bitmap createDieImage(int width, int height, int wall_size) {
		Bitmap bitmap = Bitmap.createBitmap(width + wall_size, height + wall_size, Bitmap.Config.ARGB_8888);
		Canvas g = new Canvas(bitmap);
		Paint paint = new Paint();
		int right = width + wall_size - 1;
		int bottom = height + wall_size - 1;

		paint.setAntiAlias(false);
		paint.setStrokeWidth(1);

		g.drawARGB(0, 0, 0, 0);

		paint.setStyle(Paint.Style.FILL);
		paint.setColor(DIE_FILL_COLOR);
		g.drawRect(wall_size, 1, right, height, paint);

		paint.setStyle(Paint.Style.STROKE);
		paint.setColor(DIE_ANGLE_COLOR);

		g.drawLine(wall_size-1, 1, wall_size-1, height, paint);
		g.drawLine(wall_size-1, height, right-1, height, paint);
		g.drawPoint(wall_size, 1, paint);
		g.drawPoint(wall_size, height-1, paint);
		g.drawPoint(right-1, 1, paint);
		g.drawPoint(right-1, height-1, paint);

		paint.setColor(DIE_SIDE_COLOR);

		for (int n=1; n<wall_size-1; n++) {
			g.drawLine(n, wall_size-n, n, bottom, paint);
			g.drawLine(1, bottom-n, right-wall_size+n, bottom-n, paint);
		}

		paint.setColor(DIE_BORDER_COLOR);

		g.drawLine(0, wall_size, 0, bottom, paint);
		g.drawLine(1, bottom, width, bottom, paint);

		g.drawLine(0, wall_size+1, wall_size+1, 0, paint);
		g.drawLine(width, bottom, right, height, paint);
		g.drawLine(1, bottom, wall_size, height, paint);
		g.drawLine(wall_size, 0, right, 0, paint);
		g.drawLine(right, 1, right, height, paint);

		return bitmap;
	}

	//--------------------------------------------------------------------------
	static public DisplayMetrics getDisplayMetrics(Context context) {
		DisplayMetrics metrics = new DisplayMetrics();
		WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
		wm.getDefaultDisplay().getMetrics(metrics);

		return metrics;
	}

	//--------------------------------------------------------------------------
	final static void Deal(int[] result) {
		int[] work = new int[DIE_COUNT * 4];
		int i, k, n = 0;

		for (i = 0; i < DIE_COUNT; i++)
			for (k = 0; k < 4; k++) {
				work[n] = i;
				n++;
			}

		n = DIE_COUNT * 4;
		while (n > 1) {
			k = (int) (Math.random() * n); // TODO
			if (k < n) {
				result[DIE_COUNT * 4 - n] = work[k];
				for (i = k + 1; i < work.length; i++)
					work[i - 1] = work[i];

				n--;
			}
		}

		result[DIE_COUNT * 4 - 1] = work[0];
	}

	//--------------------------------------------------------------------------
	final static AlertDialog Question(Context context, int message_id,
			DialogInterface.OnClickListener yes_listener) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setMessage(message_id);
		dialog.setPositiveButton(R.string.yes, yes_listener);
		dialog.setNegativeButton(R.string.no, null);
		dialog.setCancelable(true);

		return dialog.show();
	}

	//--------------------------------------------------------------------------
	final static AlertDialog Question(Context context, String message,
			DialogInterface.OnClickListener yes_listener) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setMessage(message);
		dialog.setPositiveButton(R.string.yes, yes_listener);
		dialog.setNegativeButton(R.string.no, null);
		dialog.setCancelable(true);

		return dialog.show();
	}

	//--------------------------------------------------------------------------
	final static AlertDialog Note(Context context, int message_id) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setMessage(message_id);
		dialog.setNeutralButton(android.R.string.ok, null);
		dialog.setCancelable(true);
		return dialog.show();
	}

	//--------------------------------------------------------------------------
	final static AlertDialog Note(Context context, String message) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setMessage(message);
		dialog.setNeutralButton(android.R.string.ok, null);
		dialog.setCancelable(true);
		return dialog.show();
	}

	//--------------------------------------------------------------------------
	final static void createMenu(Context context, Menu menu, boolean builder_item) {
		if (builder_item) {
			menu.add(Menu.NONE, Command.BUILDER, 1, R.string.builder_item)
				.setIcon(R.drawable.icon_builder);
		}

		menu.add(Menu.NONE, Command.BACKGROUND, 2, R.string.background_item)
			.setIcon(R.drawable.icon_background);

		menu.add(Menu.NONE, Command.SETTINGS, 2, R.string.settings_item)
			.setIcon(R.drawable.icon_settings);

		menu.add(Menu.NONE, Command.ABOUT, 3, R.string.about_item)
			.setIcon(R.drawable.icon_info);

	}

	//--------------------------------------------------------------------------
	final static AlertDialog showAbout(Context context) {
		//PackageManager pm = context.getPackageManager();

		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		View view = LayoutInflater.from(context).inflate(R.layout.about_view, null);

		PackageManager manager = context.getPackageManager();
        PackageInfo info;
		try {
			info = manager.getPackageInfo(context.getPackageName(), 0);
			TextView version = (TextView) view.findViewById(R.id.VersionNumber);
			version.setText(info.versionName);
		} catch (NameNotFoundException e) {
		}

		dialog.setView(view);
		dialog.setNeutralButton(android.R.string.ok, null);
		dialog.setCancelable(true);
		return dialog.show();
	}

	//--------------------------------------------------------------------------
	final static void setOrientation(Activity activity) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);

		int new_orientation;
		String value = prefs.getString(activity.getString(R.string.pref_orientation_key), "3");
		switch(Integer.parseInt(value))
		{
		case 1:
			new_orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
			break;

		case 2:
			new_orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
			break;

		case 3:
			new_orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
			break;

		default:
			new_orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
		}

		if (activity.getRequestedOrientation() != new_orientation) {
			activity.setRequestedOrientation(new_orientation);
		}
	}
}

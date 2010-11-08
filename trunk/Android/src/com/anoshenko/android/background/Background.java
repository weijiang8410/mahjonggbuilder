package com.anoshenko.android.background;

import java.util.Scanner;

import com.anoshenko.android.mahjongg.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader.TileMode;
import android.preference.PreferenceManager;

public class Background {

	static final public int[] BUILDIN_IDS = { R.drawable.background00,
		R.drawable.background01, R.drawable.background02,
		R.drawable.background03, R.drawable.background04,
		R.drawable.background05, R.drawable.background06,
		R.drawable.background07, R.drawable.background08,
		R.drawable.background09, R.drawable.background10,
		R.drawable.background11 };

	private final String PREFS_KEY = "Background";
	private final Context mContext;

	private BackgroundType mType;
	private GradientType mGradient = GradientType.VERTICAL;
	private int mColor1 = 0xFF007F00, mColor2 = 0xFF000000;
	private int mImageNumber;
	private Bitmap mImage;

	//--------------------------------------------------------------------------
	public Background(Context context) {
		mContext = context;

		if (!Load())
			setGradient(GradientType.VERTICAL, 0xFF007F00, 0xFF000000);
	}

	//--------------------------------------------------------------------------
	public boolean Load() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		String value = prefs.getString(PREFS_KEY, null);

		if (value != null) {
			Scanner scanner = new Scanner(value);
			scanner.useDelimiter(";");
			if (scanner.hasNext()){
				int type = Integer.parseInt(scanner.next());
				if(type == BackgroundType.GRADIENT.Id) {
					if(scanner.hasNext()) {
						type = Integer.parseInt(scanner.next());

						for(GradientType gradient : GradientType.values())
							if(gradient.Id == type) {
								if(scanner.hasNext()) {
									int color1 = Integer.parseInt(scanner.next());
									if(scanner.hasNext()) {
										int color2 = Integer.parseInt(scanner.next());
										setGradient(gradient, color1, color2);
										return true;
									}
								}
								break;
							}
					}
				} else if(type == BackgroundType.BUILDIN.Id) {
					if(scanner.hasNext()) {
						if(setBuildin(Integer.parseInt(scanner.next()))) {
							return true;
						}
					}
				} /*else if(type == BackgroundType.EXTERN.Id) {
				}*/
			}
		}

		return false;
	}

	//--------------------------------------------------------------------------
	public void Store() {
		String value;

		switch(mType) {
		case GRADIENT:
			value = String.format("%d;%d;%d;%d", mType.Id, mGradient.Id, mColor1, mColor2);
			break;
		case BUILDIN:
			value = String.format("%d;%d", mType.Id, mImageNumber);
			break;
		//case EXTERN:
			//break;
		default:
			return;
		}

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(PREFS_KEY, value);
		editor.commit();
	}

	//--------------------------------------------------------------------------
	public BackgroundType getType() { return mType;}
	public GradientType getGradient() { return mGradient;}
	public int getColor1() { return mColor1;}
	public int getColor2() { return mColor2;}
	public int getImageNumber() { return mImageNumber;}

	//--------------------------------------------------------------------------
	public void setGradient(GradientType type, int color1, int color2) {
		mType = BackgroundType.GRADIENT;
		mGradient = type;
		mColor1 = color1;
		mColor2 = color2;
		mImage = null;
	}

	//--------------------------------------------------------------------------
	public boolean setBuildin(int number) {
		if(number >= 0 && number < BUILDIN_IDS.length) {
			mType = BackgroundType.BUILDIN;
			mImageNumber = number;
			return true;
		}
		return false;
	}

	//--------------------------------------------------------------------------
	public void setColor1(int color) {
		mColor1 = color;
	}

	//--------------------------------------------------------------------------
	public void setColor2(int color) {
		mColor2 = color;
	}

	//--------------------------------------------------------------------------
	public void draw(Canvas g, Rect rect, int width, int height) {
		Paint paint = new Paint();

		switch(mType) {
		case GRADIENT:
			paint.setShader(mGradient.getShader(width, height, mColor1, mColor2));
			g.drawRect(rect, paint);
			break;

		case BUILDIN:
			if (mImage == null) {
				updateImage(width, height);
				if (mType != BackgroundType.BUILDIN) {
					draw(g, rect, width, height);
					break;
				}
			}
			paint.setShader(new BitmapShader(mImage, TileMode.REPEAT, TileMode.REPEAT));
			g.drawRect(rect, paint);
			break;

		//case EXTERN:
		//	break;
		}
	}

	//--------------------------------------------------------------------------
	public void updateImage(int width, int height) {
		switch(mType) {
		case GRADIENT:
			break;

		case BUILDIN:
			mImage = BitmapFactory.decodeResource(mContext.getResources(), BUILDIN_IDS[mImageNumber]);
			if (mImage == null) {
				mType = BackgroundType.GRADIENT;
			}
			break;

		//case EXTERN:
		//	break;
		}
	}
}

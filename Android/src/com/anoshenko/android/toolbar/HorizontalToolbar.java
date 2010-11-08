package com.anoshenko.android.toolbar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;

public class HorizontalToolbar extends Toolbar {

	//-------------------------------------------------------------------------
	public HorizontalToolbar(Context context) {
		super(context);
	}

	//-------------------------------------------------------------------------
	public HorizontalToolbar(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	//-------------------------------------------------------------------------
	public HorizontalToolbar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	//-------------------------------------------------------------------------
	@Override
	protected void updateButtonRect() {
		int w = getWidth(), h = getHeight();
		int bw = w / mButtonRect.length;

		mButtonRect[0].left   = 0;
		mButtonRect[0].top    = 0;
		mButtonRect[0].right  = bw + (w % mButtonRect.length) / 2;
		mButtonRect[0].bottom = h;

		for(int i=1; i<mButtonRect.length; i++) {
			mButtonRect[i].left   = mButtonRect[i-1].right;
			mButtonRect[i].top    = 0;
			mButtonRect[i].right  = mButtonRect[i].left + bw;
			mButtonRect[i].bottom = h;
		}

		mButtonRect[mButtonRect.length-1].right = w;
	}

	//-------------------------------------------------------------------------
	@Override
	protected void drawButtonBackground(Canvas g, int number) {
		Paint paint = new Paint();
		int color0, color1;
		Rect rect;

		if (number < 0) {
			rect = new Rect(0, 0, getWidth(), getHeight());
			color0 = 0xFF808080;
			color1 = 0xFF000000;
		} else {
			rect = mButtonRect[number];
			color0 = 0xFF000000;
			color1 = 0xFF808080;
		}

		paint.setShader(new LinearGradient(rect.left, rect.top, rect.left, rect.bottom,
				color0, color1, Shader.TileMode.CLAMP));
		g.drawRect(rect, paint);
		paint.setShader(null);
	}
}

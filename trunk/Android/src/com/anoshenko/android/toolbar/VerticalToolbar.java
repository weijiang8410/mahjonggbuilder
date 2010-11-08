package com.anoshenko.android.toolbar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;

public class VerticalToolbar extends Toolbar {

	//-------------------------------------------------------------------------
	public VerticalToolbar(Context context) {
		super(context);
	}

	//-------------------------------------------------------------------------
	public VerticalToolbar(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	//-------------------------------------------------------------------------
	public VerticalToolbar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	//-------------------------------------------------------------------------
	@Override
	protected void updateButtonRect() {
		int w = getWidth(), h = getHeight();
		int bh = h / mButtonRect.length;

		mButtonRect[0].left   = 0;
		mButtonRect[0].top    = 0;
		mButtonRect[0].right  = w;
		mButtonRect[0].bottom = bh + (h % mButtonRect.length) / 2;

		for(int i=1; i<mButtonRect.length; i++) {
			mButtonRect[i].left   = 0;
			mButtonRect[i].top    = mButtonRect[i-1].bottom;
			mButtonRect[i].right  = w;
			mButtonRect[i].bottom = mButtonRect[i].top + bh;
		}

		mButtonRect[mButtonRect.length-1].bottom = h;
	}

	//-------------------------------------------------------------------------
	@Override
	protected void drawButtonBackground(Canvas g, int number) {
		Paint paint = new Paint();
		int color0, color1;
		Rect rect;

		if (number < 0) {
			rect = new Rect(0, 0, getWidth(), getHeight());
			color0 = 0xFF000000;
			color1 = 0xFF808080;
		} else {
			rect = mButtonRect[number];
			color0 = 0xFF808080;
			color1 = 0xFF000000;
		}

		paint.setShader(new LinearGradient(rect.left, rect.top, rect.right/2, rect.top,
				color0, color1, Shader.TileMode.MIRROR));
		g.drawRect(rect, paint);
		paint.setShader(null);
	}
}

package com.anoshenko.android.background;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class GradientView extends View {

	private int mColor0, mColor1;
	private GradientType mType;
	private boolean mCurrent;

	public GradientView(Context context) {
		super(context);
		setMinimumWidth(80);
		setMinimumHeight(80);
	}

	public GradientView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setMinimumWidth(80);
		setMinimumHeight(80);
	}

	public GradientView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setMinimumWidth(80);
		setMinimumHeight(80);
	}

	public final void setGradient(int color0, int color1, GradientType type, boolean current) {
		mColor0 = color0;
		mColor1 = color1;
		mType = type;
		mCurrent = current;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {

	}

	@Override
	protected void onDraw(Canvas canvas) {
		Rect rect = new Rect(0, 0, getWidth(), getHeight());
		Paint paint = new Paint();

		paint.setShader(mType.getShader(getWidth(), getHeight(), mColor0, mColor1));
		paint.setStyle(Paint.Style.FILL);
		canvas.drawRect(rect, paint);

		if(mCurrent) {
			paint.reset();
			paint.setStyle(Paint.Style.STROKE);
			paint.setColor(Color.RED);
			paint.setStrokeWidth(10);
			canvas.drawRect(rect, paint);
		}
	}

}

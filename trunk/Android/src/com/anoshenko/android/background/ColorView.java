package com.anoshenko.android.background;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class ColorView extends View {

	private int color = Color.BLACK;

	public ColorView(Context context) {
		super(context);
		setMinimumWidth(50);
		setMinimumHeight(50);
	}

	public ColorView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setMinimumWidth(50);
		setMinimumHeight(50);
	}

	public ColorView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setMinimumWidth(50);
		setMinimumHeight(50);
	}

	public void setColor(int color) {
		this.color = color;
	}

	protected void onDraw(Canvas canvas) {
		Rect rect = new Rect(0, 0, getWidth(), getHeight());
		Paint paint = new Paint();

		paint.setColor(color);
		paint.setStyle(Paint.Style.FILL);
		canvas.drawRect(rect, paint);

		paint.setColor(Color.WHITE);
		paint.setStyle(Paint.Style.STROKE);
		canvas.drawRect(rect, paint);
	}

}

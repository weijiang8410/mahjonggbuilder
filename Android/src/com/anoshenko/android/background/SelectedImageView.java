package com.anoshenko.android.background;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ImageView;

public class SelectedImageView extends ImageView {

	private boolean mSelected = false;

	public SelectedImageView(Context context) {
		super(context);
	}

	public SelectedImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SelectedImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void setSelected(boolean selected) {
		mSelected = selected;
	}

	@Override
	protected void  onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (mSelected) {
			Rect rect = new Rect(0, 0, getWidth(), getHeight());
			Paint paint = new Paint();

			paint.setStyle(Paint.Style.STROKE);
			paint.setColor(Color.RED);
			paint.setStrokeWidth(10);
			canvas.drawRect(rect, paint);
		}
	}
}

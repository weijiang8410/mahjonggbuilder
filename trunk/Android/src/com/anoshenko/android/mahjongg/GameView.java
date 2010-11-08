package com.anoshenko.android.mahjongg;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

public class GameView extends View {

	private Bitmap mZBuffer;
	private BaseActivity mActivity;
	private int mOffsetX, mOffsetY;

	//--------------------------------------------------------------------------
	public GameView(Context context) {
		super(context);
		init(context);
	}

	//--------------------------------------------------------------------------
	public GameView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	//--------------------------------------------------------------------------
	public GameView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	//--------------------------------------------------------------------------
	private void init(Context context) {
		mActivity = (BaseActivity)context;
		mOffsetX = mOffsetY = 0;
	}

	//--------------------------------------------------------------------------
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		mActivity.updateZBuffer(w, h);
	}

	//--------------------------------------------------------------------------
	@Override
	protected void onDraw(Canvas g) {
		if (mZBuffer != null) {
			int width = getWidth();
			int height = getHeight();
			Paint paint = new Paint();

			if (width == mZBuffer.getWidth() && height == mZBuffer.getHeight()) {
				g.drawBitmap(mZBuffer, 0, 0, paint);
			} else {
				g.drawBitmap(mZBuffer,
						new Rect(mOffsetX, mOffsetY, mOffsetX + width, mOffsetY + height),
						new Rect(0, 0, width, height), paint);
			}
		}
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return mActivity.KeyDown(keyCode, event);
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		return mActivity.KeyUp(keyCode, event);
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mActivity.PenDown((int)event.getX(), (int)event.getY());
			break;

		case MotionEvent.ACTION_MOVE:
			mActivity.PenMove((int)event.getX(), (int)event.getY());
			break;

		case MotionEvent.ACTION_UP:
			mActivity.PenUp((int)event.getX(), (int)event.getY());
			break;
		}
		return true;
	}

	//--------------------------------------------------------------------------
	void setZBuffer(Bitmap z_buffer) {
		mZBuffer = z_buffer;
		invalidate();
	}

	//--------------------------------------------------------------------------
	void setOffset(int x, int y) {
		mOffsetX = x;
		mOffsetY = y;
	}
}

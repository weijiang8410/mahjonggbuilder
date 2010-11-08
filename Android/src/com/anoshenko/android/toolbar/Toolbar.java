package com.anoshenko.android.toolbar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public abstract class Toolbar extends View {

	protected OnToolbarListener mListener;
	protected Rect[] mButtonRect;
	protected ToolbarButton[] mButtons;

	private int mPushedButton = -1;
	private boolean mPushed = false;

	//-------------------------------------------------------------------------
	public Toolbar(Context context) {
		super(context);
	}

	//-------------------------------------------------------------------------
	public Toolbar(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	//-------------------------------------------------------------------------
	public Toolbar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	//-------------------------------------------------------------------------
	public void setButtons(ToolbarButton[] buttons) {
		mButtons = buttons;

		if (mButtonRect == null || mButtonRect.length != buttons.length) {
			mButtonRect = new Rect[buttons.length];

			for(int i=0; i<mButtonRect.length; i++)
				mButtonRect[i] = new Rect();

			updateButtonRect();
		}

		invalidate();
	}

	//-------------------------------------------------------------------------
	public void setListener(OnToolbarListener listener) {
		mListener = listener;
	}

	//-------------------------------------------------------------------------
	protected abstract void updateButtonRect();

	//-------------------------------------------------------------------------
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		if (mButtonRect != null)
			updateButtonRect();
	}

	//-------------------------------------------------------------------------
	public final boolean isCapturePen() {
		return mPushedButton >= 0;
	}

	//-------------------------------------------------------------------------
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mButtons == null)
			return true;

		int number;

		switch(event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			number = getButtonNumber((int)event.getX(), (int)event.getY());
			if (number >= 0 && mButtons[number].mEnabled) {
				mPushedButton = number;
				mPushed = true;
				invalidate();
			}
			break;

		case MotionEvent.ACTION_UP:
			number = getButtonNumber((int)event.getX(), (int)event.getY());
			if (mButtons != null && mListener != null &&
					number == mPushedButton && number >= 0) {
				mListener.onToolbarButtonClick(mButtons[number].mCommand);
			}

			mPushedButton = -1;
			mPushed = false;
			invalidate();
			break;

		case MotionEvent.ACTION_MOVE:
			if (mPushedButton >= 0) {
				number = getButtonNumber((int)event.getX(), (int)event.getY());
				if (mPushed) {
					if (number != mPushedButton) {
						mPushed = false;
						invalidate();
					}
				} else if (number == mPushedButton) {
					mPushed = true;
					invalidate();
				}
			}
			break;

		case MotionEvent.ACTION_CANCEL:
			mPushedButton = -1;
			if (mPushed) {
				mPushed = false;
				invalidate();
			}
			break;
		}

		return true;
	}

	//-------------------------------------------------------------------------
	protected int getButtonNumber(int x, int y) {
		if (mButtons != null)
			for(int i=0; i<mButtonRect.length; i++) {
				if(mButtonRect[i].contains(x, y)) {
					return i;
				}
			}

		return -1;
	}

	//-------------------------------------------------------------------------
	@Override
	public void onDraw(Canvas g) {
		if (mButtons == null) {
			return;
		}

		Paint paint = new Paint();
		paint.setTypeface(Typeface.DEFAULT_BOLD);

		paint.setAntiAlias(true);

		Paint.FontMetrics fm = paint.getFontMetrics();
		int font_height = (int)(fm.descent - fm.ascent);
		Bitmap icon;

		/*if (ButtonText == null) {
			ButtonText = new String[mToolbarData.getToolbarButtonCount()];
			Resources res = getContext().getResources();

			for(int i=0; i<ButtonText.length; i++)
				ButtonText[i] = res.getString(mToolbarData.getToolbarButtonText(i));
		}*/

		drawButtonBackground(g, -1);

		for(int i=0; i<mButtonRect.length; i++) {
			if (i == mPushedButton && mPushed)
				drawButtonBackground(g, i);

			if (mButtons[i].mEnabled) {
				icon = mButtons[i].mIcon;
				paint.setColor(0xFFFFFFFF);
			} else {
				icon = mButtons[i].mIconDisabled;
				paint.setColor(0xFFC0C0C0);
			}

			int y = (mButtonRect[i].bottom + mButtonRect[i].top -
					icon.getHeight() - font_height - 1) / 2;
			int x = (mButtonRect[i].right + mButtonRect[i].left - icon.getWidth()) / 2;

			g.drawBitmap(icon, x, y, paint);

			if (mButtons[i].mText != null) {
				y += icon.getHeight() + 1 - (int)fm.ascent;
				x = (mButtonRect[i].right + mButtonRect[i].left -
						(int)paint.measureText(mButtons[i].mText)) / 2;

				g.drawText(mButtons[i].mText, x, y, paint);
			}
		}
	}

	//-------------------------------------------------------------------------
	abstract protected void drawButtonBackground(Canvas g, int number);
}

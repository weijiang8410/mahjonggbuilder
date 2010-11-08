package com.anoshenko.android.mahjongg;

import com.anoshenko.android.background.Background;
import com.anoshenko.android.background.BackgroundActivity;
import com.anoshenko.android.toolbar.OnToolbarListener;
import com.anoshenko.android.toolbar.Toolbar;
import com.anoshenko.android.toolbar.ToolbarButton;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;

public abstract class BaseActivity extends Activity implements OnToolbarListener, PopupMenu.Listener {

	final static String GAME_ID_KEY = "GAME_ID";

	final static int FREE_PLACE = -4;

	protected Handler mHandler = new Handler();

	protected Background mBackground;
	protected Toolbar mToolbar;

	protected Bitmap mZBuffer, mDieBase;
	Bitmap[] mDieImage = new Bitmap[Utils.DIE_COUNT];
	protected int mSideWallSize, mDieWidth, mDieHeight, mMarkedBorder = 2;
	protected int mXOffset, mYOffset;

	int[][][] mLayer;
	protected int[] mStartDies = new int[144];

	//--------------------------------------------------------------------------
	protected abstract int getLayoutId();

	//--------------------------------------------------------------------------
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		Utils.setOrientation(this);

		super.onCreate(savedInstanceState);

		setContentView(getLayoutId());
		setResult(Command.NONE);

		mToolbar = (Toolbar)findViewById(getToolbarId());
		mToolbar.setListener(this);

		mBackground = new Background(this);

		Resources res = getResources();

		for (int i=0; i<Utils.DIE_COUNT; i++)
			mDieImage[i] = BitmapFactory.decodeResource(res, R.drawable.die00 + i);
	}

	//--------------------------------------------------------------------------
	abstract void PenDown(int x, int y);
	abstract void PenMove(int x, int y);
	abstract void PenUp(int x, int y);
	abstract boolean KeyDown(int keyCode, KeyEvent event);
	abstract boolean KeyUp(int keyCode, KeyEvent event);
	abstract protected void invalidateArrea();
	abstract protected void startLayerDraw(Canvas g, int layer);

	abstract int getLayerCount();
	abstract int getLayerWidth();
	abstract int getLayerHeight();
	abstract int getLeftLayerCount();
	abstract int getTopLayerCount();

	abstract protected boolean isMarked(int layer, int row, int collumn);
	abstract protected boolean isLayerVisible(int layer);

	abstract boolean updateZBuffer(int width, int height);

	//--------------------------------------------------------------------------
	protected boolean updateZBuffer(int width, int height, int min_width, int min_height) {

		if (width <= 0 || height <= 0)
			return false;

		DisplayMetrics dm = Utils.getDisplayMetrics(this);

		if (Math.min(dm.heightPixels, dm.widthPixels) >= 480) {
			mSideWallSize = 4;
			mMarkedBorder = 3;
		} else {
			mSideWallSize = 3;
		}

		final int layer_width		= getLayerWidth();
		final int layer_height		= getLayerHeight();
		final int left_layer_count	= getLeftLayerCount();
		final int top_layer_count	= getTopLayerCount();

		int cell_width	= (width - left_layer_count * mSideWallSize) / layer_width;
		int cell_height	= (height - top_layer_count * mSideWallSize) / layer_height;

		mDieWidth = cell_width * 2;

		if (dm.heightPixels > dm.widthPixels) {
			mDieHeight = (mDieWidth * mDieImage[0].getHeight() / mDieImage[0].getWidth() + 2) & 0xFFFE;

			if (cell_height < mDieHeight / 2)
				mDieHeight = cell_height * 2;
			else
				cell_height = mDieHeight / 2;
		} else {
			mDieHeight = cell_height * 2;

			if (mDieWidth > mDieHeight) {
				mDieWidth = mDieHeight;
				cell_width = cell_height;
			}
		}

		mDieBase = Utils.createDieImage(mDieWidth, mDieHeight, mSideWallSize);

		mXOffset = (width - left_layer_count * mSideWallSize - layer_width * cell_width) / 2;
		mYOffset = (height - top_layer_count * mSideWallSize - layer_height * cell_height) / 2;

		if (min_width < width) {
			if (mXOffset > mYOffset) {
				width -= 2 * (mXOffset - mYOffset);
				if (width < min_width) {
					width = min_width;
					mXOffset = (width - left_layer_count * mSideWallSize - layer_width * cell_width) / 2;
				} else {
					mXOffset = mYOffset;
				}
			} else if (mXOffset < mYOffset) {
				height -= 2 * (mYOffset - mXOffset);
				if (height < min_height) {
					height = min_height;
					mYOffset = (height - top_layer_count * mSideWallSize - layer_height * cell_height) / 2;
				} else {
					mYOffset = mXOffset;
				}
			}
		}

		mYOffset += (top_layer_count - 1) * mSideWallSize;

		if (mZBuffer == null || mZBuffer.getWidth() != width || mZBuffer.getHeight() != height) {
			mBackground.updateImage(width, height);
			mZBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			RedrawZBuffer();
			return true;
		}

		return false;
	}

	//--------------------------------------------------------------------------
	private final boolean isDieVisible(int layer, int row, int collumn) {
		if (layer == getLayerCount()-1)
			return true;

		if (mLayer[layer+1][row][collumn] >= 0)
			return false;

		boolean inner_row = row > 0 && row < getLayerHeight() - 2;

		if (inner_row &&
				mLayer[layer][row-1][collumn] >= 0 &&
				mLayer[layer][row+1][collumn] >= 0)
			return false;

		if (collumn > 0 && collumn < getLayerWidth() - 2) {
			if (mLayer[layer][row][collumn-1] >= 0 &&
				mLayer[layer][row][collumn+1] >= 0)
				return false;

			if (inner_row &&
					mLayer[layer][row-1][collumn-1] >= 0 &&
					mLayer[layer][row-1][collumn+1] >= 0 &&
					mLayer[layer][row+1][collumn-1] >= 0 &&
					mLayer[layer][row+1][collumn+1] >= 0)
				return false;
		}

		return true;
	}

	//--------------------------------------------------------------------------
	void RedrawZBuffer() {
		if (mZBuffer != null) {
			Canvas g = new Canvas(mZBuffer);
			Paint paint = new Paint();

			paint.setAntiAlias(true);
			paint.setFilterBitmap(true);
			paint.setDither(true);

			int width	= mZBuffer.getWidth();
			int height	= mZBuffer.getHeight();

			mBackground.draw(g, new Rect(0, 0, width, height), width, height);
			//g.drawARGB(0xFF, 0, 128, 0);

			final int layer_count	= getLayerCount();
			final int layer_width	= getLayerWidth();
			final int layer_height	= getLayerHeight();
			final int cell_width	= mDieWidth / 2;
			final int cell_height	= mDieHeight / 2;
			int x, y, i, j, k;

			Rect src_rect = new Rect(mSideWallSize, 0, mSideWallSize + mDieWidth, mDieHeight);
			Rect image_rect = new Rect(0, 0, mDieImage[0].getWidth(), mDieImage[0].getHeight());
			Rect dst_rect = new Rect();
			int image_width = (mDieHeight - 1) * mDieImage[0].getWidth() / mDieImage[0].getHeight();
			int image_x_off = mSideWallSize + (mDieWidth - image_width) / 2;

			Paint marked_paint;
			marked_paint = new Paint();
			marked_paint.setStyle(Paint.Style.STROKE);
			marked_paint.setColor(0xFF0000FF);
			marked_paint.setStrokeWidth(mMarkedBorder);

			for (i=0; i<layer_count; i++)
				if (isLayerVisible(i)) {
					startLayerDraw(g, i);

					int[][] layer = mLayer[i];

					for (j=0, y=mYOffset-i*mSideWallSize; j<layer_height; j++, y+=cell_height)
						for (k=0, x=mXOffset+i*mSideWallSize; k<layer_width; k++, x+=cell_width)
							if (layer[j][k] >= 0) {
								g.drawBitmap(mDieBase, x, y, paint);
							}

					for (j=0, y=mYOffset-i*mSideWallSize; j<layer_height; j++, y+=cell_height)
						for (k=0, x=mXOffset+i*mSideWallSize; k<layer_width; k++, x+=cell_width)
							if (layer[j][k] >= 0 && isDieVisible(i, j, k)) {
								dst_rect.set(x + mSideWallSize, y,
										x + mSideWallSize + mDieWidth, y + mDieHeight);
								g.drawBitmap(mDieBase, src_rect, dst_rect, paint);

								dst_rect.set(x + image_x_off, y + 1,
										x + image_x_off + image_width, y + mDieHeight);
								g.drawBitmap(mDieImage[layer[j][k]], image_rect, dst_rect, paint);

								if (isMarked(i, j, k)) {
									dst_rect.set(x + mSideWallSize + 1, y + 1,
											x + mSideWallSize + mDieWidth - 1, y + mDieHeight - 1);

									g.drawRect(dst_rect, marked_paint);
								}
							}
			}
		}
	}

	//--------------------------------------------------------------------------
	protected void doCommand(int command) {
		switch (command) {

		case Command.ABOUT:
			Utils.showAbout(this);
			break;

		case Command.BACKGROUND:
			startActivityForResult(new Intent(this, BackgroundActivity.class), Command.BACKGROUND_ACTIVITY);
			break;

		case Command.SETTINGS:
			startActivityForResult(new Intent(this, SettingsActivity.class), Command.SETTINGS_ACTIVITY);
			break;

		case Command.SOLITAIRES:
			Utils.installSolitaires(this);
			break;
		}
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		doCommand(item.getItemId());
		return true;
	}

	//--------------------------------------------------------------------------
	@Override
	public void onToolbarButtonClick(int command) {
		doCommand(command);
	}

	//--------------------------------------------------------------------------
	@Override
	public void onPopupMenuSelect(int command) {
		doCommand(command);
	}

	//--------------------------------------------------------------------------
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent  data) {
		switch (requestCode) {
		case Command.BACKGROUND_ACTIVITY:
			mBackground.Load();
			mBackground.updateImage(mZBuffer.getWidth(), mZBuffer.getHeight());
			RedrawZBuffer();
			invalidateArrea();
			break;

		case Command.SETTINGS_ACTIVITY:
			Utils.setOrientation(this);
			updateToolbar();
			break;
		}
	}

	//--------------------------------------------------------------------------
	protected abstract int getToolbarId();
	protected abstract int getToolbarRightId();
	protected abstract ToolbarButton[] getToolbarButtons();

	protected void updateToolbar() {
		DisplayMetrics dm = Utils.getDisplayMetrics(this);

		if (dm.widthPixels > dm.heightPixels) {
			String value = getPreferenceValue(R.string.pref_landscape_toolbar_key, "0");
			Toolbar leftToolbar = (Toolbar)findViewById(getToolbarId());
			Toolbar rightToolbar = (Toolbar)findViewById(getToolbarRightId());

			if (value.equals("0")) {
				if (rightToolbar != null)
					rightToolbar.setVisibility(View.GONE);

				leftToolbar.setVisibility(View.VISIBLE);
				mToolbar = leftToolbar;
			} else {
				if (rightToolbar != null) {
					leftToolbar.setVisibility(View.GONE);
					rightToolbar.setVisibility(View.VISIBLE);
					mToolbar = rightToolbar;
				} else {
					mToolbar = leftToolbar;
				}
			}

			mToolbar.setListener(this);
			mToolbar.setButtons(getToolbarButtons());
		}
	}

	//--------------------------------------------------------------------------
	protected String getPreferenceValue(int key_id, String default_value) {
		return getPreferenceValue(getString(key_id), default_value);
	}

	//--------------------------------------------------------------------------
	protected String getPreferenceValue(String key, String default_value) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		return key == null ? default_value : prefs.getString(key, default_value);
	}
}

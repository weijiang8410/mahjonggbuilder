package com.anoshenko.android.mahjongg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Vector;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.anoshenko.android.toolbar.ToolbarButton;

public class BuilderActivity extends BaseActivity {

	private final static int ACTION_BUTTON	= 0;
	private final static int LAYER_BUTTON	= 1;
	private final static int SCALE_BUTTON	= 2;
	private final static int MORE_BUTTON	= 3;
	private final static int BUTTON_COUNT	= 4;

	private final static int ACTION_NONE		= -1;
	private final static int ACTION_ADD_SERIES	= 0;
	private final static int ACTION_ADD_SINGLE	= 1;
	private final static int ACTION_REMOVE		= 2;
	private final static int ACTION_MOVE_LAST	= 3;
	private final static int ACTION_MOVE_LAYER	= 4;
	private final static int ACTION_MOVE_ALL	= 5;
	private final static int ACTION_SCROLL		= 6;

	private final static int LAYER_COUNT		= 8;
	private final static int LAYER_HEIGHT		= 18;
	private final static int LAYER_WIDTH		= 30;

	private final static String LEFT_FREE_KEY		= "LEFT_FREE_KEY";
	private final static String TOP_FREE_KEY		= "TOP_FREE_KEY";
	private final static String LAYOUT_KEY			= "LAYOUT_KEY_KEY";
	private final static String START_DIES_KEY		= "START_DIES_KEY";
	private final static String CURRENT_LAYER_KEY	= "CURRENT_LAYER_KEY";
	private final static String SCALE_KEY			= "SCALE_KEY";
	private final static String ACTION_KEY			= "ACTION_KEY";
	private final static String SCROLL_X_KEY		= "SCROLL_X_KEY";
	private final static String SCROLL_Y_KEY		= "SCROLL_Y_KEY";
	private final static String UNDO_KEY			= "UNDO_KEY";

	//--------------------------------------------------------------------------
	private class DieLocation {
		final int Row, Collumn;

		DieLocation(int row, int collumn) {
			Row = row;
			Collumn = collumn;
		}
	}

	//--------------------------------------------------------------------------
	private class Action {
		final int Action, Layer, Row, Collumn, RowCount, CollumnCount;
		final Vector<DieLocation> Series;

		//--------------------------------------------------------------------------
		Action(int action, int layer, int row, int collumn, int row_count,
				int collumn_count, Vector<DieLocation> series) {
			Action			= action;
			Layer			= layer;
			Row				= row;
			Collumn			= collumn;
			RowCount		= row_count;
			CollumnCount	= collumn_count;
			Series = series != null ? new Vector<DieLocation>(series) : new Vector<DieLocation>();

			if (series == null && Action == ACTION_ADD_SINGLE)
				Series.add(new DieLocation(row, collumn));
		}

		//--------------------------------------------------------------------------
		Action(int action, int layer, int row, int collumn) {
			this(action, layer, row, collumn, 2, 2, null);
		}

		//--------------------------------------------------------------------------
		Action(String data) {
			Action			= data.charAt(0) - 'A';
			Layer			= data.charAt(1) - 'A';
			Row				= data.charAt(2) - 'A';
			Collumn			= data.charAt(3) - 'A';
			RowCount		= data.charAt(4) - 'A';
			CollumnCount	= data.charAt(5) - 'A';
			Series			= new Vector<DieLocation>();

			for (int i=6; i<data.length()-1; i+=2) {
				int row = data.charAt(i) - 'A';
				int collumn = data.charAt(i+1) - 'A';
				DieLocation die_location = new DieLocation(row, collumn);
				try {
					Series.add(die_location);
				} catch (OutOfMemoryError ex) {
					System.gc();
					Series.add(die_location);
				}
			}
		}

		//--------------------------------------------------------------------------
		void store(StringBuilder builder) {
			builder.append((char)('A' + Action));
			builder.append((char)('A' + Layer));
			builder.append((char)('A' + Row));
			builder.append((char)('A' + Collumn));
			builder.append((char)('A' + RowCount));
			builder.append((char)('A' + CollumnCount));
			for (DieLocation die : Series) {
				builder.append((char)('A' + die.Row));
				builder.append((char)('A' + die.Collumn));
			}
			builder.append('\n');
		}
	}

	//--------------------------------------------------------------------------
	private final ToolbarButton[] mToolbarButton = new ToolbarButton[BUTTON_COUNT];
	private GameView mBuilderView;

	MahjonggData mData;

	private int mAction = ACTION_ADD_SERIES;
	private int mDragAction = ACTION_NONE;
	private int mCurrentLayer = 0;
	private int mScale = 1;
	private int mLayerCount = 1;
	private int mLeftDie = 144;

	private int mDownX, mDownY, mLastX, mLastY;
	private int mScrollX, mScrollY;

	private Vector<Action> mUndoMemory = new Vector<Action>();

	//--------------------------------------------------------------------------
	private class AddSeries {
		final int StartRow, StartCollumn;
		int Row, Collumn;
		Vector<DieLocation> Series = new Vector<DieLocation>();

		AddSeries(int row, int column) {
			Row = StartRow = row;
			Collumn = StartCollumn = column;
		}
	}

	private AddSeries mAddSeries = null;

	//--------------------------------------------------------------------------
	private class DragFragment {
		final int StartRow, StartCollumn, RowCount, CollumnCount;
		int Row, Collumn;
		Vector<DieLocation> Series;

		DragFragment(Action action) {
			Row = StartRow = action.Row;
			Collumn = StartCollumn = action.Collumn;
			RowCount = action.RowCount;
			CollumnCount = action.CollumnCount;
			Series = new Vector<DieLocation>(action.Series);
		}
	}

	private DragFragment mDragFragment = null;

	//--------------------------------------------------------------------------
	private class DragLayer {
		final int StartLeft, StartTop, Width, Height;
		int Left, Top;

		DragLayer(int left_free, int top_free, int right_free, int bottom_free) {
			Left	= StartLeft	= left_free;
			Top 	= StartTop	= top_free;
			Width	= LAYER_WIDTH - left_free - right_free;
			Height	= LAYER_HEIGHT - top_free - bottom_free;
		}
	}

	private DragLayer mDragLayer = null;

	//--------------------------------------------------------------------------
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mBuilderView = (GameView) findViewById(R.id.BuilderArrea);
		mLayer = new int[LAYER_COUNT][LAYER_HEIGHT][LAYER_WIDTH];

		for (int i=0; i<LAYER_COUNT; i++)
			for (int j=0; j<LAYER_HEIGHT; j++)
				for (int k=0; k<LAYER_WIDTH; k++)
					mLayer[i][j][k] = FREE_PLACE;

		if (savedInstanceState != null) {
			String data = savedInstanceState.getString(START_DIES_KEY);
			if (data != null) {
				for (int i=0; i<mStartDies.length; i++)
					mStartDies[i] = data.charAt(i) - '0';
			} else {
				Utils.Deal(mStartDies);
			}

			mAction = savedInstanceState.getInt(ACTION_KEY, 0);
			mScale = savedInstanceState.getInt(SCALE_KEY, 1);
			mCurrentLayer = savedInstanceState.getInt(CURRENT_LAYER_KEY, 0);
			if (mScale > 1) {
				mScrollX = savedInstanceState.getInt(SCROLL_X_KEY, 0);
				mScrollY = savedInstanceState.getInt(SCROLL_Y_KEY, 0);
			}

			data = savedInstanceState.getString(UNDO_KEY);
			if (data != null) {
				BufferedReader reader = new BufferedReader(new StringReader(data));
				try {
					String line = reader.readLine();
					while (line != null) {
						mUndoMemory.add(new Action(data));
						line = reader.readLine();
					}
				} catch (IOException e) {
				} finally {
					try {
						reader.close();
					} catch (IOException e) {
					}
				}
			}
		} else {
			Utils.Deal(mStartDies);
		}

		int left_free = 0, top_free = 0;

		try {
			if (savedInstanceState != null) {
				String data = savedInstanceState.getString(LAYOUT_KEY);
				if (data != null) {
					mData = new MahjonggData(this, data);

					left_free = savedInstanceState.getInt(LEFT_FREE_KEY, 0);
					top_free = savedInstanceState.getInt(TOP_FREE_KEY, 0);
				}
			} else {
				int id = getIntent().getIntExtra(GAME_ID_KEY, -1);
				if (id >= 0) {
					mData = new MahjonggData(this, id);
					top_free = (LAYER_HEIGHT - mData.getLayerHeight()) / 2;
					left_free = (LAYER_WIDTH - mData.getLayerWidth()) / 2;
				}
			}
		} catch (MahjonggData.LoadExeption e) {
			e.printStackTrace();
		}

		mLeftDie = 144;

		if (mData == null)
			mData = new MahjonggData(this);
		else {
			for (int i=0; i<mData.getLayerCount() && mLeftDie>0; i++)
				for (int j=0; j<mData.getLayerHeight() && mLeftDie>0; j++)
					for (int k=0; k<mData.getLayerWidth() && mLeftDie>0; k++)
						if (mData.isPlace(i, j, k)) {
							mLeftDie--;
							mLayer[i][j+top_free][k+left_free] = mStartDies[mLeftDie];
						}
		}

		Resources res = getResources();
		int scale_icon; // action_icon,

		/*switch (mAction) {
		case ACTION_ADD_SERIES:
			action_icon = R.drawable.icon_action_add_series;
			break;

		case ACTION_ADD_SINGLE:
			action_icon = R.drawable.icon_action_add;
			break;

		case ACTION_REMOVE:
			action_icon = R.drawable.icon_action_remove;
			break;

		case ACTION_MOVE_LAST:
			action_icon = R.drawable.icon_move_last;
			break;

		case ACTION_MOVE_LAYER:
			action_icon = R.drawable.icon_move_layer;
			break;

		case ACTION_MOVE_ALL:
			action_icon = R.drawable.icon_move_all;
			break;

		case ACTION_SCROLL:
			action_icon = R.drawable.icon_cursor;
			break;

		default:
			mAction = ACTION_ADD_SERIES;
			action_icon = R.string.add_group_item;
			break;
		}*/

		switch (mScale) {
		case 1:
			scale_icon =R.drawable.icon_1x;
			break;

		case 2:
			scale_icon =R.drawable.icon_2x;
			break;

		case 3:
			scale_icon =R.drawable.icon_3x;
			break;

		default:
			scale_icon =R.drawable.icon_1x;
			mScale = 1;
			break;
		}

		mToolbarButton[ACTION_BUTTON] = new ToolbarButton(res, Command.ACTION_MENU,
				R.string.action_button, R.drawable.icon_action_add_series, -1);

		mToolbarButton[LAYER_BUTTON] = new ToolbarButton(res, Command.LAYER_MENU,
				R.string.layer_button, R.drawable.icon_layer1 + mCurrentLayer, -1);

		mToolbarButton[SCALE_BUTTON] = new ToolbarButton(res, Command.SCALE_MENU,
				R.string.scale_button, scale_icon, -1);

		mToolbarButton[MORE_BUTTON] = new ToolbarButton(res, Command.MORE_MENU,
				R.string.more_button, R.drawable.icon_start, -1);

		for (int i=0; i<mToolbarButton.length; i++)
			mToolbarButton[i].loadName(this);

		mToolbar.setButtons(mToolbarButton);
		updateToolbar();
		updateLayerCount();
		updateLeftCounter();

		setCurrentAction(mAction);
	}

	//--------------------------------------------------------------------------
	@Override
	protected void onDestroy() {
		int free_left	= getLeftFree();
		int free_top	= getTopFree();
		int free_right	= getRightFree();
		int free_bottom	= getBottomFree();
		int layer_count	= mLayerCount;

		if (isLayerEmpty(layer_count-1))
			layer_count--;

		if (layer_count > 0) {
			mData.setData(mLayer, layer_count, free_left, free_top, free_right, free_bottom);
			mData.store();
			setResult(mData.getId());
		} else if (mData.getId() >= 0) {
			mData.store();
			setResult(mData.getId());
		} else {
			setResult(-1);
		}

		super.onDestroy();
	}

	@Override
	//--------------------------------------------------------------------------
	protected void onSaveInstanceState(Bundle outState) {
		int free_left	= getLeftFree();
		int free_top	= getTopFree();
		int free_right	= getRightFree();
		int free_bottom	= getBottomFree();
		int layer_count	= mLayerCount;

		if (isLayerEmpty(layer_count-1))
			layer_count--;

		if (layer_count > 0) {
			mData.setData(mLayer, layer_count, free_left, free_top, free_right, free_bottom);
			outState.putString(LAYOUT_KEY, mData.getStoreData());
			outState.putInt(TOP_FREE_KEY, free_top);
			outState.putInt(LEFT_FREE_KEY, free_left);

			int left = mLeftDie;
			for (int i=layer_count-1; i>=0 && left < mStartDies.length; i--)
				for (int j=LAYER_HEIGHT-1; j>=0 && left < mStartDies.length; j--) {
					int[] row = mLayer[i][j];
					for (int k=LAYER_WIDTH-1; k>=0 && left < mStartDies.length; k--)
						if (row[k] >= 0) {
							mStartDies[left] = row[k];
							left++;
						}
				}
		}

		StringBuilder builder = new StringBuilder();
		for (int i=0; i<mStartDies.length; i++)
			builder.append((char)('0' + mStartDies[i]));

		outState.putString(START_DIES_KEY, builder.toString());
		outState.putInt(ACTION_KEY, mAction);
		outState.putInt(CURRENT_LAYER_KEY, mCurrentLayer);
		outState.putInt(SCALE_KEY, mScale);
		if (mScale > 1) {
			outState.putInt(SCROLL_X_KEY, mScrollX);
			outState.putInt(SCROLL_Y_KEY, mScrollY);
		}

		if (mUndoMemory.size() > 0) {
			builder.setLength(0);
			for (Action action : mUndoMemory)
				action.store(builder);

			outState.putString(UNDO_KEY, builder.toString());
		}
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getLayoutId() {
		return R.layout.builder_view;
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getToolbarId() {
		return R.id.BuilderToolbar;
	}

	//--------------------------------------------------------------------------
	@Override
	protected int getToolbarRightId() {
		return R.id.BuilderToolbarRight;
	}

	//--------------------------------------------------------------------------
	@Override
	protected ToolbarButton[] getToolbarButtons() {
		return mToolbarButton;
	}

	//--------------------------------------------------------------------------
	@Override
	protected void invalidateArrea() {
		mBuilderView.invalidate();
	}

	//--------------------------------------------------------------------------
	@Override
	protected void doCommand(int command) {
		PopupMenu menu;
		TextView text_view;

		switch (command) {

		case Command.ACTION_MENU:
			menu = new PopupMenu(this, this);
			menu.addItem(Command.ADD_SERIES, R.string.add_group_item, R.drawable.icon_action_add_series);
			menu.addItem(Command.ADD_SINGLE, R.string.add_single_item, R.drawable.icon_action_add);
			menu.addItem(Command.REMOVE, R.string.remove_item, R.drawable.icon_action_remove, mLeftDie < 144);
			menu.addItem(Command.MOVE_LAST, R.string.move_last_item, R.drawable.icon_move_last,
					getLastFragment() != null);
			menu.addItem(Command.MOVE_LAYER, R.string.move_layer_item, R.drawable.icon_move_layer,
					getLayerTopFree(mCurrentLayer) > 0 || getLayerBottomFree(mCurrentLayer) > 0 ||
					getLayerLeftFree(mCurrentLayer) > 0 || getLayerRightFree(mCurrentLayer) > 0);
			menu.addItem(Command.MOVE_ALL, R.string.move_all_item, R.drawable.icon_move_all,
					getTopFree() > 0 || getBottomFree() > 0 || getLeftFree() > 0 || getRightFree() > 0);
			menu.addItem(Command.SCROLL, R.string.scroll_item, R.drawable.icon_cursor, mScale > 1);
			menu.show();
			break;

		case Command.LAYER_MENU:
			menu = new PopupMenu(this, this);
			for (int i=0; i<LAYER_COUNT; i++)
				menu.addItem(Command.LAYER1 + i, R.string.layer1_item + i, R.drawable.icon_layer1 + i, i < mLayerCount);

			menu.show();
			break;

		case Command.SCALE_MENU:
			menu = new PopupMenu(this, this);
			for (int i=0; i<3; i++)
				menu.addItem(Command.SCALE_1X + i, R.string.scale_1x_item + i, R.drawable.icon_1x + i);

			menu.show();
			break;

		case Command.MORE_MENU:
			menu = new PopupMenu(this, this);
			menu.addItem(Command.UNDO, R.string.undo, R.drawable.icon_undo, mUndoMemory.size() > 0);
			menu.addItem(Command.RENAME, R.string.name_and_info_item, R.drawable.icon_edit);
			//menu.addItem(Command.SAVE, R.string.save_item, R.drawable.icon_save);
			menu.show();
			break;

		case Command.ADD_SERIES:
			setCurrentAction(ACTION_ADD_SERIES);
			break;

		case Command.ADD_SINGLE:
			setCurrentAction(ACTION_ADD_SINGLE);
			break;

		case Command.REMOVE:
			setCurrentAction(ACTION_REMOVE);
			break;

		case Command.MOVE_LAST:
			setCurrentAction(ACTION_MOVE_LAST);
			break;

		case Command.MOVE_LAYER:
			setCurrentAction(ACTION_MOVE_LAYER);
			break;

		case Command.MOVE_ALL:
			setCurrentAction(ACTION_MOVE_ALL);
			break;

		case Command.SCROLL:
			setCurrentAction(ACTION_SCROLL);
			break;

		case Command.LAYER1:
		case Command.LAYER2:
		case Command.LAYER3:
		case Command.LAYER4:
		case Command.LAYER5:
		case Command.LAYER6:
		case Command.LAYER7:
		case Command.LAYER8:
			mCurrentLayer = command - Command.LAYER1;
			mToolbarButton[LAYER_BUTTON].setIcon(this, R.drawable.icon_layer1 + mCurrentLayer);
			mToolbar.invalidate();

			text_view = (TextView) findViewById(R.id.BuilderLayer);
			if (text_view != null)
				text_view.setText(Integer.toString(mCurrentLayer + 1));

			RedrawZBuffer();
			invalidateArrea();
			break;

		case Command.SCALE_1X:
			if (changeScale(1)) {
				mToolbarButton[SCALE_BUTTON].setIcon(this, R.drawable.icon_1x);
				mToolbar.invalidate();
			}
			break;

		case Command.SCALE_2X:
			if (changeScale(2)) {
				mToolbarButton[SCALE_BUTTON].setIcon(this, R.drawable.icon_2x);
				mToolbar.invalidate();
			}
			break;

		case Command.SCALE_3X:
			if (changeScale(3)) {
				mToolbarButton[SCALE_BUTTON].setIcon(this, R.drawable.icon_3x);
				mToolbar.invalidate();
			}
			break;

		case Command.UNDO:
			Undo();
			break;

		case Command.RENAME:
			GameInfoDialog.show(this);
			break;

		case Command.SAVE:
			// TODO
			break;

		default:
			super.doCommand(command);
		}
	}

	//--------------------------------------------------------------------------
	private void setCurrentAction(int action) {
		int text_id;
		int icon_id;

		mAction = action;

		switch(action) {
		case ACTION_ADD_SERIES:
			icon_id = R.drawable.icon_action_add_series;
			text_id = R.string.add_group_item;
			break;

		case ACTION_ADD_SINGLE:
			icon_id = R.drawable.icon_action_add;
			text_id = R.string.add_single_item;
			break;

		case ACTION_REMOVE:
			icon_id = R.drawable.icon_action_remove;
			text_id = R.string.remove_item;
			break;

		case ACTION_MOVE_LAST:
			icon_id = R.drawable.icon_move_last;
			text_id = R.string.move_last_item;
			break;

		case ACTION_MOVE_LAYER:
			icon_id = R.drawable.icon_move_layer;
			text_id = R.string.move_layer_item;
			break;

		case ACTION_MOVE_ALL:
			icon_id = R.drawable.icon_move_all;
			text_id = R.string.move_all_item;
			break;

		case ACTION_SCROLL:
			icon_id = R.drawable.icon_cursor;
			text_id = R.string.scroll_item;
			break;

		default:
			return;
		}

		mToolbarButton[ACTION_BUTTON].setIcon(this, icon_id);
		mToolbar.invalidate();

		TextView action_title = (TextView)findViewById(R.id.BuilderTitleText);
		if (action_title != null)
			action_title.setText(text_id);
	}

	//--------------------------------------------------------------------------
	private boolean changeScale(int new_scale) {
		if (mScale == new_scale)
			return false;

		int width = mBuilderView.getWidth();
		int height = mBuilderView.getHeight();
		int scale_width = width * new_scale;
		int scale_height = height * new_scale;

		mScale = new_scale;
		updateZBuffer(width, height);

		if (mScrollX + width > scale_width)
			mScrollX = scale_width - width;

		if (mScrollY + height > scale_height)
			mScrollY = scale_height - height;

		mBuilderView.setOffset(mScrollX, mScrollY);
		mBuilderView.invalidate();
		return true;
	}

	//--------------------------------------------------------------------------
	private void removeSeries(Vector<DieLocation> series) {
		for (int i=series.size()-1; i>=0; i--) {
			try {
				DieLocation die = series.get(i);
				mStartDies[mLeftDie] = mLayer[mCurrentLayer][die.Row][die.Collumn];
				mLeftDie++;
				mLayer[mCurrentLayer][die.Row][die.Collumn] = FREE_PLACE;
			} catch (ArrayIndexOutOfBoundsException ex) {
				ex.printStackTrace();
			}
		}
	}

	//--------------------------------------------------------------------------
	private void setSeries(Vector<DieLocation> result, int row, int collumn, int row_count, int collumn_count) {
		for (int i=0; i<row_count && mLeftDie > 0; i+=2)
			for (int k=0; k<collumn_count && mLeftDie > 0; k+=2) {
				if (isPlaceFree(mCurrentLayer, row + i, collumn + k)) {
					result.add(new DieLocation(row + i, collumn + k));
					mLeftDie--;
					mLayer[mCurrentLayer][row + i][collumn + k] = mStartDies[mLeftDie];
				}
			}
	}

	//--------------------------------------------------------------------------
	private void Undo() {
		Action action;

		if (mUndoMemory.size() > 0) {
			action = mUndoMemory.remove(mUndoMemory.size()-1);
		} else {
			return;
		}

		switch (action.Action) {
		case ACTION_ADD_SERIES:
			removeSeries(action.Series);
			updateLayerCount();
			RedrawAndUpdate();
			break;

		case ACTION_ADD_SINGLE:
			mStartDies[mLeftDie] = mLayer[mCurrentLayer][action.Row][action.Collumn];
			mLeftDie++;
			mLayer[mCurrentLayer][action.Row][action.Collumn] = FREE_PLACE;

			updateLayerCount();
			RedrawAndUpdate();
			break;

		case ACTION_REMOVE:
			for (DieLocation die : action.Series) {
				mLeftDie--;
				mLayer[mCurrentLayer][die.Row][die.Collumn] = mStartDies[mLeftDie];
			}

			updateLayerCount();
			RedrawAndUpdate();
			break;

		case ACTION_MOVE_LAST:
			removeSeries(action.Series);

			action = mUndoMemory.lastElement();
			if (action != null) {
				for (DieLocation die : action.Series)
					if (mLeftDie == 0) {
						break;
					} else if (isPlaceFree(mCurrentLayer, die.Row, die.Collumn)) {
						mLeftDie--;
						mLayer[mCurrentLayer][die.Row][die.Collumn] = mStartDies[mLeftDie];
					}
			}

			RedrawAndUpdate();
			break;

		case ACTION_MOVE_LAYER:
			shiftLayer(action.Layer, -action.Row, -action.Collumn);
			RedrawAndUpdate();
			break;

		case ACTION_MOVE_ALL:
			for (int i=0; i<mLayerCount; i++)
				shiftLayer(action.Layer, -action.Row, -action.Collumn);

			RedrawAndUpdate();
			break;

		case ACTION_SCROLL:
			break;
		}
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (KeyDown(keyCode, event))
			return true;

		return super.onKeyDown(keyCode, event);
	}

	//--------------------------------------------------------------------------
	private void store() {
		int free_left	= getLeftFree();
		int free_top	= getTopFree();
		int free_right	= getRightFree();
		int free_bottom	= getBottomFree();
		int layer_count	= mLayerCount;

		if (isLayerEmpty(layer_count-1))
			layer_count--;

		if (layer_count > 0) {
			mData.setData(mLayer, layer_count, free_left, free_top, free_right, free_bottom);
			mData.store();
			setResult(mData.getId());
		}
	}

	//--------------------------------------------------------------------------
	class ExitQuestionDialog extends Dialog {

		public ExitQuestionDialog(Context context) {
			super(context);

			View view = LayoutInflater.from(BuilderActivity.this).inflate(R.layout.exit_question, null);

			Button button = (Button)view.findViewById(R.id.SaveAndExitButton);
			button.setOnClickListener(mSaveAndExit);

			button = (Button)view.findViewById(R.id.ExitWithoutSaveButton);
			button.setOnClickListener(mExitWithoutSave);

			button = (Button)view.findViewById(R.id.ContinueEditingButton);
			button.setOnClickListener(mContinueEditing);

			setContentView(view);
			setTitle(R.string.warning);
		}

		//----------------------------------------------------------------------
		private final View.OnClickListener mSaveAndExit = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				store();
				finish();
				dismiss();
			}
		};

		//----------------------------------------------------------------------
		private final View.OnClickListener mExitWithoutSave = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setResult(-1);
				finish();
				dismiss();
			}
		};

		//----------------------------------------------------------------------
		private final View.OnClickListener mContinueEditing = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		};
	}

	//--------------------------------------------------------------------------
	@Override
	boolean KeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			if (mLeftDie == 144 && mData.getId() < 0)
				return false;

			if (mLeftDie > 0) {
				ExitQuestionDialog dialog = new ExitQuestionDialog(this);
				dialog.show();
				return true;
			}

			if (mData.getName() == null) {
				AlertDialog.Builder dialog = new AlertDialog.Builder(this);
				dialog.setMessage(R.string.no_name);
				dialog.setCancelable(true);
				dialog.setNegativeButton(android.R.string.no, null);
				dialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						store();
						finish();
					}
				});
				dialog.setNeutralButton(R.string.set_name, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						GameInfoDialog.show(BuilderActivity.this);
					}
				});
				dialog.show();
			}
			return false;
		}

		return false;
	}

	//--------------------------------------------------------------------------
	@Override
	boolean KeyUp(int keyCode, KeyEvent event) {
		// TODO
		return false;
	}

	//--------------------------------------------------------------------------
	private void updateLeftCounter() {
		TextView text_view = (TextView) findViewById(R.id.BuilderLeft);
		if (text_view != null)
			text_view.setText(Integer.toString(mLeftDie));
	}

	//--------------------------------------------------------------------------
	private boolean isPlaceFree(int layer, int row, int collumn) {
		int[][] layer_data = mLayer[mCurrentLayer];

		if (row >= LAYER_HEIGHT-1 || collumn >= LAYER_WIDTH-1 ||
			layer_data[row][collumn] >= 0 ||
			layer_data[row+1][collumn] >= 0 ||
			layer_data[row][collumn+1] >= 0 ||
			layer_data[row+1][collumn+1] >= 0)
			return false;

		if (row > 0) {
			if (layer_data[row-1][collumn] >= 0 ||
				layer_data[row-1][collumn+1] >= 0 ||
				(collumn > 0 && layer_data[row-1][collumn-1] >= 0))
				return false;
		}

		if (collumn > 0)
			if (layer_data[row][collumn-1] >= 0 ||
				layer_data[row+1][collumn-1] >= 0)
				return false;

		return true;
	}

	//--------------------------------------------------------------------------
	private Action getLastFragment() {
		Action action;

		for (int i=mUndoMemory.size()-1; i>=0; i--) {
			action = mUndoMemory.get(i);

			switch (action.Action) {
			case ACTION_ADD_SERIES:
			case ACTION_ADD_SINGLE:
			case ACTION_MOVE_LAST:
				return action.Layer == mCurrentLayer ? action : null;

			case ACTION_REMOVE:
				break;
			}
		}

		return null;
	}

	//--------------------------------------------------------------------------
	private void RedrawAndUpdate() {
		RedrawZBuffer();
		invalidateArrea();
		updateLeftCounter();
	}

	//--------------------------------------------------------------------------
	private void shiftLayer(int layer_number, int vertical, int horizontal) {
		
		try {
			int[][] layer = mLayer[layer_number];
	
			if (horizontal < 0) {
	
				for (int i=0; i<LAYER_HEIGHT; i++)
					for (int k=0; k<LAYER_WIDTH+horizontal; k++)
						layer[i][k] = layer[i][k-horizontal];
	
				for (int i=0; i<LAYER_HEIGHT; i++)
					for (int k=LAYER_WIDTH+horizontal; k<LAYER_WIDTH; k++)
						layer[i][k] = FREE_PLACE;
	
			} else if (horizontal > 0) {
	
				for (int i=0; i<LAYER_HEIGHT; i++)
					for (int k=LAYER_WIDTH-1; k>=horizontal; k--)
						layer[i][k] = layer[i][k-horizontal];
	
				for (int i=0; i<LAYER_HEIGHT; i++)
					for (int k=0; k<horizontal; k++)
						layer[i][k] = FREE_PLACE;
			}
	
			if (vertical < 0) {
	
				for (int i=0; i<LAYER_HEIGHT+vertical; i++)
					for (int k=0; k<LAYER_WIDTH; k++)
						layer[i][k] = layer[i-vertical][k];
	
				for (int i=LAYER_HEIGHT+vertical; i<LAYER_HEIGHT; i++)
					for (int k=0; k<LAYER_WIDTH; k++)
						layer[i][k] = FREE_PLACE;
	
			} else if (vertical > 0) {
	
				for (int i=LAYER_HEIGHT-1; i>=vertical; i--)
					for (int k=0; k<LAYER_WIDTH; k++)
						layer[i][k] = layer[i-vertical][k];
	
				for (int i=0; i<vertical; i++)
					for (int k=0; k<LAYER_WIDTH; k++)
						layer[i][k] = FREE_PLACE;
			}
			
		} catch (ArrayIndexOutOfBoundsException ex) {
			
		}
	}

	//--------------------------------------------------------------------------
	@Override
	void PenDown(int x, int y) {

		mDragAction = ACTION_NONE;
		mLastX = mDownX = x;
		mLastY = mDownY = y;

		switch (mAction) {
		case ACTION_ADD_SERIES:
			if (mLeftDie > 0) {
				x += mScrollX;
				y += mScrollY;

				int x1 = mXOffset + mCurrentLayer * mSideWallSize;
				int y1 = mYOffset - (mCurrentLayer - 1)*mSideWallSize;

				if (x >= x1 && y >= y1) {
					int row = (y - y1) / (mDieHeight / 2);
					int collumn = (x - x1) / (mDieWidth / 2);

					if (row < LAYER_HEIGHT-1 && collumn < LAYER_WIDTH-1) {
						mAddSeries = new AddSeries(row, collumn);

						if (isPlaceFree(mCurrentLayer, row, collumn)) {
							mAddSeries.Series.add(new DieLocation(row, collumn));
							mLeftDie--;
							mLayer[mCurrentLayer][row][collumn] = mStartDies[mLeftDie];
							mDragAction = ACTION_ADD_SERIES;
							RedrawAndUpdate();

							mDownX -= (x - x1) % (mDieWidth / 2);
							mDownY -= (y - y1) % (mDieHeight / 2);
						}
					}
				}
			}
			break;

		case ACTION_ADD_SINGLE:
			if (mLeftDie > 0) {
				x += mScrollX;
				y += mScrollY;

				int x1 = mXOffset + mCurrentLayer * mSideWallSize;
				int y1 = mYOffset - (mCurrentLayer - 1)*mSideWallSize;

				if (x >= x1 && y >= y1) {
					int row = (y - y1) / (mDieHeight / 2);
					int collumn = (x - x1) / (mDieWidth / 2);

					if (row < LAYER_HEIGHT-1 && collumn < LAYER_WIDTH-1 &&
							isPlaceFree(mCurrentLayer, row, collumn)) {
						mLeftDie--;
						mLayer[mCurrentLayer][row][collumn] = mStartDies[mLeftDie];
						if (mLayerCount == mCurrentLayer+1 && mLayerCount < LAYER_COUNT)
							mLayerCount++;

						updateLayerCount();
						RedrawAndUpdate();
						mUndoMemory.add(new Action(ACTION_ADD_SINGLE, mCurrentLayer, row, collumn));
					}
				}
			}
			break;

		case ACTION_REMOVE: {
			x += mScrollX;
			y += mScrollY;

			int x1 = mXOffset + mCurrentLayer * mSideWallSize;
			int y1 = mYOffset - (mCurrentLayer - 1)*mSideWallSize;

			if (x >= x1 && y >= y1) {
				int row = (y - y1) / (mDieHeight / 2);
				int collumn = (x - x1) / (mDieWidth / 2);

				if (row < LAYER_HEIGHT && collumn < LAYER_WIDTH) {
					int[][] layer = mLayer[mCurrentLayer];
					int remove_row = -1, remove_collumn = -1;

					if (layer[row][collumn] >= 0) {
						remove_row = row;
						remove_collumn = collumn;
					} else {
						if (row > 0) {
							if (layer[row-1][collumn] >= 0) {
								remove_row = row-1;
								remove_collumn = collumn;
							} else if (collumn > 0 && layer[row-1][collumn-1] >= 0) {
								remove_row = row-1;
								remove_collumn = collumn-1;
							} else if (collumn < LAYER_WIDTH-1 && layer[row-1][collumn+1] >= 0) {
								remove_row = row-1;
								remove_collumn = collumn+1;
							}
						}

						if (remove_row < 0 && row < LAYER_HEIGHT-1) {
							if (layer[row+1][collumn] >= 0) {
								remove_row = row+1;
								remove_collumn = collumn;
							} else if (collumn > 0 && layer[row+1][collumn-1] >= 0) {
								remove_row = row+1;
								remove_collumn = collumn-1;
							} else if (collumn < LAYER_WIDTH-1 && layer[row+1][collumn+1] >= 0) {
								remove_row = row+1;
								remove_collumn = collumn+1;
							}
						}

						if (remove_row < 0 && collumn > 0 && layer[row][collumn-1] >= 0) {
							remove_row = row;
							remove_collumn = collumn-1;
						}

						if (remove_row < 0 && collumn < LAYER_WIDTH-1 && layer[row][collumn+1] >= 0) {
							remove_row = row;
							remove_collumn = collumn-1;
						}
					}

					if (remove_row >= 0 && remove_row < layer.length && 
						remove_collumn >= 0 && remove_collumn < layer[remove_row].length &&
						mLeftDie < mStartDies.length) {

						mStartDies[mLeftDie] = layer[remove_row][remove_collumn];
						layer[remove_row][remove_collumn] = FREE_PLACE;
						mLeftDie++;

						if (isLayerEmpty(mCurrentLayer) && mLayerCount == mCurrentLayer+1)
							mLayerCount--;

						updateLayerCount();
						RedrawAndUpdate();
						mUndoMemory.add(new Action(ACTION_REMOVE, mCurrentLayer, remove_row, remove_collumn));
					}
				}
			}
			break;
		}
		case ACTION_MOVE_LAST: {
			Action last_fragment = getLastFragment();
			if (last_fragment != null) {
				mDragFragment = new DragFragment(last_fragment);
				mDragAction = ACTION_MOVE_LAST;
				RedrawAndUpdate();
			}
			break;
		}
		case ACTION_MOVE_LAYER: {
			int left_free	= getLayerLeftFree(mCurrentLayer);
			int top_free	= getLayerTopFree(mCurrentLayer);
			int right_free	= getLayerRightFree(mCurrentLayer) - 1;
			int bottom_free	= getLayerBottomFree(mCurrentLayer) - 1;

			if (left_free > 0 || top_free > 0 || right_free > 0 || bottom_free > 0) {
				mDragLayer = new DragLayer(left_free, top_free, right_free, bottom_free);
				mDragAction = ACTION_MOVE_LAYER;
			}
			break;
		}
		case ACTION_MOVE_ALL: {
			int left_free	= getLeftFree();
			int top_free	= getTopFree();
			int right_free	= getRightFree() - 1;
			int bottom_free	= getBottomFree() - 1;

			if (left_free > 0 || top_free > 0 || right_free > 0 || bottom_free > 0) {
				mDragLayer = new DragLayer(left_free, top_free, right_free, bottom_free);
				mDragAction = ACTION_MOVE_ALL;
			}
			break;
		}
		case ACTION_SCROLL:
			if (mScale > 1) {
				mDragAction = ACTION_SCROLL;
			}
			break;
		}
	}

	//--------------------------------------------------------------------------
	@Override
	void PenMove(int x, int y) {
		switch (mDragAction) {
		case ACTION_ADD_SERIES: {
			int row, collumn, row2, collumn2;

			if (y < mDownY) {
				row = mAddSeries.StartRow - 2 * ((mDownY - y + mDieHeight) / mDieHeight);
				while (row < 0) row += 2;
			} else {
				row = mAddSeries.StartRow + 2 * ((y - mDownY) / mDieHeight);
				while (row >= LAYER_HEIGHT-1) row -= 2;
			}

			if (x < mDownX) {
				collumn = mAddSeries.StartCollumn - 2 * ((mDownX - x + mDieWidth) / mDieWidth);
				while (collumn < 0) collumn += 2;
			} else {
				collumn = mAddSeries.StartCollumn + 2 * ((x - mDownX) / mDieWidth);
				while (collumn >= LAYER_WIDTH-1) collumn -= 2;
			}

			if (row != mAddSeries.Row || collumn != mAddSeries.Collumn) {
				removeSeries(mAddSeries.Series);
				mAddSeries.Series.clear();

				mAddSeries.Row = row;
				mAddSeries.Collumn = collumn;

				if (row <= mAddSeries.StartRow) {
					row2 = mAddSeries.StartRow;
				} else {
					row2 = row; row = mAddSeries.StartRow;
				}

				if (collumn <= mAddSeries.StartCollumn) {
					collumn2 = mAddSeries.StartCollumn;
				} else {
					collumn2 = collumn; collumn = mAddSeries.StartCollumn;
				}

				setSeries(mAddSeries.Series, row, collumn, row2 - row + 2, collumn2 - collumn + 2);
				RedrawAndUpdate();
			}
			break;
		}

		case ACTION_MOVE_LAST: {
			int row = mDragFragment.StartRow + (y - mDownY) / (mDieHeight / 2);
			int collumn = mDragFragment.StartCollumn + (x - mDownX) / (mDieWidth / 2);

			if (row < 0)
				row = 0;
			else if (row + mDragFragment.RowCount > LAYER_HEIGHT)
				row = LAYER_HEIGHT - mDragFragment.RowCount;

			if (collumn < 0)
				collumn = 0;
			else if (collumn + mDragFragment.CollumnCount > LAYER_WIDTH)
				collumn = LAYER_WIDTH - mDragFragment.CollumnCount;

			if (row != mDragFragment.Row || collumn != mDragFragment.Collumn) {
				removeSeries(mDragFragment.Series);
				mDragFragment.Series.clear();

				mDragFragment.Row = row;
				mDragFragment.Collumn = collumn;

				setSeries(mDragFragment.Series, row, collumn, mDragFragment.RowCount, mDragFragment.CollumnCount);
				RedrawAndUpdate();
			}
			break;
		}
		case ACTION_MOVE_LAYER: {
			int top = mDragLayer.StartTop + (y - mDownY) / (mDieHeight / 2);
			int left = mDragLayer.StartLeft + (x - mDownX) / (mDieWidth / 2);

			if (top < 0)
				top = 0;
			else if (top + mDragLayer.Height > LAYER_HEIGHT)
				top = LAYER_HEIGHT - mDragLayer.Height;

			if (left < 0)
				left = 0;
			else if (left + mDragLayer.Width > LAYER_WIDTH)
				left = LAYER_WIDTH - mDragLayer.Width;

			if (mDragLayer.Top != top || mDragLayer.Left != left) {
				shiftLayer(mCurrentLayer, top - mDragLayer.Top, left - mDragLayer.Left);
				mDragLayer.Top = top;
				mDragLayer.Left = left;
				RedrawAndUpdate();
			}
			break;
		}
		case ACTION_MOVE_ALL: {
			int top = mDragLayer.StartTop + (y - mDownY) / (mDieHeight / 2);
			int left = mDragLayer.StartLeft + (x - mDownX) / (mDieWidth / 2);

			if (top < 0)
				top = 0;
			else if (top + mDragLayer.Height > LAYER_HEIGHT)
				top = LAYER_HEIGHT - mDragLayer.Height;

			if (left < 0)
				left = 0;
			else if (left + mDragLayer.Width > LAYER_WIDTH)
				left = LAYER_WIDTH - mDragLayer.Width;

			if (mDragLayer.Top != top || mDragLayer.Left != left) {
				for (int i=0; i<mLayerCount; i++)
					shiftLayer(i, top - mDragLayer.Top, left - mDragLayer.Left);

				mDragLayer.Top = top;
				mDragLayer.Left = left;
				RedrawAndUpdate();
			}
			break;
		}
		case ACTION_SCROLL: {
			int scroll_x		= mScrollX - x + mLastX;
			int scroll_y		= mScrollY - y + mLastY;
			int width			= mBuilderView.getWidth();
			int height			= mBuilderView.getHeight();
			int scale_width		= mZBuffer.getWidth();
			int scale_height	= mZBuffer.getHeight();

			if (scroll_x < 0)
				scroll_x = 0;
			else if (scroll_x + width > scale_width)
				scroll_x = scale_width - width;

			if (scroll_y < 0)
				scroll_y = 0;
			else if (scroll_y + height > scale_height)
				scroll_y = scale_height - height;

			if (scroll_x != mScrollX || scroll_y != mScrollY) {
				mScrollX = scroll_x;
				mScrollY = scroll_y;
				mBuilderView.setOffset(mScrollX, mScrollY);
				mBuilderView.invalidate();
			}
			break;
		}
		}

		mLastX = x;
		mLastY = y;
	}

	//--------------------------------------------------------------------------
	@Override
	void PenUp(int x, int y) {
		switch (mDragAction) {
		case ACTION_ADD_SERIES:
			if (mAddSeries.Series.size() > 0) {
				int row, collumn, row2, collumn2;

				if (mAddSeries.Row <= mAddSeries.StartRow) {
					row = mAddSeries.Row;
					row2 = mAddSeries.StartRow;
				} else {
					row2 = mAddSeries.Row;
					row = mAddSeries.StartRow;
				}

				if (mAddSeries.Collumn <= mAddSeries.StartCollumn) {
					collumn = mAddSeries.Collumn;
					collumn2 = mAddSeries.StartCollumn;
				} else {
					collumn2 = mAddSeries.Collumn;
					collumn = mAddSeries.StartCollumn;
				}

				updateLayerCount();
				mUndoMemory.add(new Action(ACTION_ADD_SERIES, mCurrentLayer, row, collumn,
						row2 - row + 2, collumn2 - collumn + 2, mAddSeries.Series));
			}
			mAddSeries = null;
			break;

		case ACTION_MOVE_LAST:
			mUndoMemory.add(new Action(ACTION_MOVE_LAST, mCurrentLayer,
					mDragFragment.Row, mDragFragment.Collumn, mDragFragment.RowCount,
					mDragFragment.CollumnCount, mDragFragment.Series));
			mDragFragment = null;
			RedrawAndUpdate();
			break;

		case ACTION_MOVE_LAYER:
			if (mDragLayer.StartLeft != mDragLayer.Left || mDragLayer.StartTop != mDragLayer.Top) {
				mUndoMemory.add(new Action(ACTION_MOVE_LAYER, mCurrentLayer,
						mDragLayer.Top - mDragLayer.StartTop,
						mDragLayer.Left - mDragLayer.StartLeft));
			}
			mDragFragment = null;
			break;

		case ACTION_MOVE_ALL:
			if (mDragLayer.StartLeft != mDragLayer.Left || mDragLayer.StartTop != mDragLayer.Top) {
				mUndoMemory.add(new Action(ACTION_MOVE_ALL, -1,
						mDragLayer.Top - mDragLayer.StartTop,
						mDragLayer.Left - mDragLayer.StartLeft));
			}
			mDragFragment = null;
			break;

		case ACTION_SCROLL:
			break;
		}

		mDragAction = ACTION_NONE;
	}

	//--------------------------------------------------------------------------
	@Override
	boolean updateZBuffer(int width, int height) {

		if (updateZBuffer(width * mScale, height * mScale, width, height)) {
			mBuilderView.setZBuffer(mZBuffer);
			return true;
		}

		return false;
	}

	//--------------------------------------------------------------------------
	@Override
	int getLayerHeight() {
		return LAYER_HEIGHT;
	}

	//--------------------------------------------------------------------------
	@Override
	int getLayerWidth() {
		return LAYER_WIDTH;
	}

	//--------------------------------------------------------------------------
	@Override
	int getLeftLayerCount() {
		return LAYER_COUNT;
	}

	//--------------------------------------------------------------------------
	@Override
	int getTopLayerCount() {
		return LAYER_COUNT;
	}

	//--------------------------------------------------------------------------
	@Override
	int getLayerCount() {
		return mLayerCount;
	}

	//--------------------------------------------------------------------------
	@Override
	protected boolean isLayerVisible(int layer) {
		return layer <= mCurrentLayer;
	}

	//--------------------------------------------------------------------------
	@Override
	protected boolean isMarked(int layer, int row, int collumn) {
		if (mDragFragment != null && layer == mCurrentLayer)
			for (DieLocation die : mDragFragment.Series)
				if (die.Row == row && die.Collumn == collumn)
					return true;

		return false;
	}

	//--------------------------------------------------------------------------
	@Override
	protected void startLayerDraw(Canvas g, int layer) {

		if (layer != mCurrentLayer)
			return;

		Paint paint = new Paint();
		int x1 = mXOffset + layer*mSideWallSize;
		int y1 = mYOffset - (layer - 1)*mSideWallSize;
		int x2 = x1 + LAYER_WIDTH * mDieWidth / 2;
		int y2 = y1 + LAYER_HEIGHT * mDieHeight / 2;

		paint.setColor(0xA0000000);
		paint.setStyle(Paint.Style.FILL);

		g.drawRect(0, 0, mZBuffer.getWidth(), mZBuffer.getHeight(), paint);

		paint.setColor(0xFFFFFFFF);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(1);

		for (int x = x1; x <= x2; x += mDieWidth)
			g.drawLine(x, y1, x, y2, paint);

		for (int y = y1; y <= y2; y += mDieHeight)
			g.drawLine(x1, y, x2, y, paint);

		paint.setPathEffect(new DashPathEffect(new float[]{4.0f, 4.0f}, 0));

		for (int x = x1 + mDieWidth/2; x < x2; x += mDieWidth)
			g.drawLine(x, y1, x, y2, paint);

		for (int y = y1 + mDieHeight / 2; y < y2; y += mDieHeight)
			g.drawLine(x1, y, x2, y, paint);
	}

	//--------------------------------------------------------------------------
	private void updateLayerCount() {
		int count = 0;

		for (int i=LAYER_COUNT-1; i>=0; i--)
			if (getLayerTopFree(i) < LAYER_HEIGHT) {
				count = i + 1;
				break;
			}

		if (count < LAYER_COUNT-1)
			count++;

		mLayerCount = count;
	}

	//--------------------------------------------------------------------------
	private boolean isLayerEmpty(int layer) {
		return getLayerTopFree(layer) == LAYER_HEIGHT;
	}

	//--------------------------------------------------------------------------
	private int getLayerLeftFree(int layer) {
		int[][] layer_data = mLayer[layer];
		int left = LAYER_WIDTH - 1;

		for (int i=0; i<LAYER_HEIGHT; i++)
			for (int k=0; k<left; k++)
				if (layer_data[i][k] >= 0) {
					left = k;
					break;
				}

		return left;
	}

	//--------------------------------------------------------------------------
	private int getLeftFree() {
		int left = getLayerLeftFree(0), left_i;

		for (int i=1; i<LAYER_COUNT && left > 0; i++) {
			left_i = getLayerLeftFree(i);
			if (left_i < left)
				left = left_i;
		}

		return left;
	}

	//--------------------------------------------------------------------------
	private int getLayerTopFree(int layer) {
		int[][] layer_data = mLayer[layer];

		for (int i=0; i<LAYER_HEIGHT; i++)
			for (int k=0; k<LAYER_WIDTH; k++)
				if (layer_data[i][k] >= 0) {
					return i;
				}

		return LAYER_HEIGHT;
	}

	//--------------------------------------------------------------------------
	private int getTopFree() {
		int top = getLayerTopFree(0), top_i;

		for (int i=1; i<LAYER_COUNT && top > 0; i++) {
			top_i = getLayerTopFree(i);
			if (top_i < top)
				top = top_i;
		}

		return top;
	}

	//--------------------------------------------------------------------------
	private int getLayerRightFree(int layer) {
		int[][] layer_data = mLayer[layer];
		int right = 0;

		for (int i=0; i<LAYER_HEIGHT; i++)
			for (int k=LAYER_WIDTH - 1; k>right; k--)
				if (layer_data[i][k] >= 0) {
					right = k;
					break;
				}

		return LAYER_WIDTH - 1 - right;
	}

	//--------------------------------------------------------------------------
	private int getRightFree() {
		int right = getLayerRightFree(0), right_i;

		for (int i=1; i<LAYER_COUNT && right > 0; i++) {
			right_i = getLayerRightFree(i);
			if (right_i < right)
				right = right_i;
		}

		return right;
	}

	//--------------------------------------------------------------------------
	private int getLayerBottomFree(int layer) {
		int[][] layer_data = mLayer[layer];

		for (int i=LAYER_HEIGHT-1; i>=0; i--)
			for (int k=0; k<LAYER_WIDTH; k++)
				if (layer_data[i][k] >= 0) {
					return LAYER_HEIGHT - 1 - i;
				}

		return LAYER_HEIGHT;
	}

	//--------------------------------------------------------------------------
	private int getBottomFree() {
		int bottom = getLayerBottomFree(0), bottom_i;

		for (int i=1; i<LAYER_COUNT && bottom > 0; i++) {
			bottom_i = getLayerBottomFree(i);
			if (bottom_i < bottom)
				bottom = bottom_i;
		}

		return bottom;
	}
}

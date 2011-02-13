package com.anoshenko.android.mahjongg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Scanner;
import java.util.Vector;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

final class MahjonggData implements Comparable<MahjonggData> {

	private final static byte[] BYTE_MASK = {(byte)0x80, 0x40, 0x20, 0x10, 8, 4, 2, 1};
	private final static int SIDE_WALL_SIZE = 2;

	final static int USER_GAME_ID = 0x1000000;

	private final static String UNEXPECTED_EOF = "Unexpected end of file";
	private final static String INVALID_DATA = "Invalid user game data";
	private final static String NOT_FOUND = "Game not found";

	private final static String USER_GAME_KEY 		= "USER_GAME_";
	private final static String USER_GAME_LIST_KEY	= "USER_GAME_LIST";
	private final static String STATISTICS_KEY		= "STATISTICS";

	private final static String ID_KEY			= "ID";
	private final static String NAME_KEY		= "Name";
	private final static String AUTHOR_KEY		= "Author";
	private final static String COMMENT_KEY		= "Comment";
	private final static String LAYERCOUNT_KEY	= "LayerCount";
	private final static String HEIGHT_KEY		= "Height";
	private final static String WIDTH_KEY		= "Width";
	private final static String DATA_KEY		= "Data";

	private int mID;
	private Context mContext;
	private String mName, mAuthor, mComment;
	private Bitmap mPreview;

	private boolean[] mLayout;
	private int mLayerCount;
	private int mLayerHeight;
	private int mLayerWidth;
	private boolean mUnfinished = false;

	private int mWins, mLosses, mBestTime,
		mAvgTotalGames, mAvgTotalTime, mAvgUndos, mAvgShuffles;

	//--------------------------------------------------------------------------
	@SuppressWarnings("serial")
	class LoadExeption extends Exception {
		public LoadExeption(String message) {
			super(message);
		}
	}

	//--------------------------------------------------------------------------
	MahjonggData(Context context, int id) throws LoadExeption {
		mContext = context;
		mID = id;

		if (id < USER_GAME_ID) {
			InputStream list_stream = mContext.getResources().openRawResource(R.raw.games_list);
			try {
				while (list_stream.available() > 0) {
					id = readUint24(list_stream);
					if (id == mID) {
						load(readUint24(list_stream));
						return;
					} else {
						list_stream.skip(3);
					}
				}
			} catch (IOException e) {
				throw new LoadExeption(e.getMessage());
			} finally {
				try {
					list_stream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			throw new LoadExeption(NOT_FOUND);
		} else {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			String data = prefs.getString(USER_GAME_KEY + id, null);
			if (data == null)
				throw new LoadExeption(NOT_FOUND);

			parse(data, false);
		}
	}

	//--------------------------------------------------------------------------
	public MahjonggData(Context context, String data) throws LoadExeption {

		mContext = context;
		mID = -1;
		parse(data, true);
	}

	//--------------------------------------------------------------------------
	MahjonggData(Context context, InputStream list_stream) throws LoadExeption {

		mContext = context;
		mID = readUint24(list_stream);
		load(readUint24(list_stream));
	}

	//--------------------------------------------------------------------------
	MahjonggData(Context context) {

		mContext = context;
		mID = -1;
	}

	//--------------------------------------------------------------------------
	private int readUint24(InputStream stream) throws LoadExeption {
		int result = 0, d;

		for (int i=0; i<3; i++) {
			result <<= 8;
			try {
				d = stream.read();
				if (d > 0)
					result += d;
			} catch (IOException e) {
				e.printStackTrace();
				throw new LoadExeption(e.getMessage());
			}
		}

		return result;
	}

	//--------------------------------------------------------------------------
	private void load(int pos) throws LoadExeption {
		try {
			InputStream stream = mContext.getResources().openRawResource(R.raw.games_data);
			stream.skip(pos);

			int name_len = stream.read();
			if (name_len < 0)
				throw new LoadExeption(UNEXPECTED_EOF);

			byte[] name = new byte[name_len];
			stream.read(name);

			mName = new String(name);

			name_len = stream.read();
			if (name_len < 0)
				throw new LoadExeption(UNEXPECTED_EOF);

			if (name_len > 0) {
				name = new byte[name_len];
				stream.read(name);

				mAuthor = new String(name);
			}

			int win_chance = stream.read();
			int layer_count  = stream.read();
			int layer_width  = stream.read();
			int layer_height = stream.read();

			if (win_chance < 0 || layer_count < 0 || layer_width < 0 || layer_height < 0)
				throw new LoadExeption(UNEXPECTED_EOF);

			mLayerCount  = layer_count;
			mLayerHeight = layer_height;
			mLayerWidth  = layer_width;
			mLayout = new boolean[mLayerCount*mLayerHeight*mLayerWidth];

			int len = (layer_width + 7) / 8;

			byte[] buffer = new byte[len];
			int i, j, k, off;
			int layer_size = mLayerHeight * mLayerWidth;

			for (i = 0; i < layer_count; i++) {
				off = i * layer_size;

				for (k = 0; k < layer_height; k++) {
					stream.read(buffer);
					for (j = 0; j < layer_width; j++)
						mLayout[off + j] = ((buffer[j >> 3] & BYTE_MASK[j & 7]) != 0);

					off += mLayerWidth;
				}
			}
		} catch (IOException e) {
			throw new LoadExeption(e.getMessage());
		}
	}

	//--------------------------------------------------------------------------
	private void parse(String data, boolean parse_id) throws LoadExeption {
		BufferedReader reader = new BufferedReader(new StringReader(data));
		mUnfinished = true;

		try {
			String line = reader.readLine();
			String layers_data = null;

			while (line != null) {
				int pos = line.indexOf('=');
				if (pos > 0) {
					String key = line.substring(0, pos);
					String value = line.substring(pos+1);

					if (ID_KEY.equalsIgnoreCase(key)) {
						if (parse_id)
							mID = Integer.parseInt(value);
					} else if (NAME_KEY.equalsIgnoreCase(key)) {
						mName = value;
					} else if (AUTHOR_KEY.equalsIgnoreCase(key)) {
						mAuthor = value;
					} else if (COMMENT_KEY.equalsIgnoreCase(key)) {
						mComment = value;
					} else if (LAYERCOUNT_KEY.equalsIgnoreCase(key)) {
						mLayerCount = Integer.parseInt(value);
					} else if (WIDTH_KEY.equalsIgnoreCase(key)) {
						mLayerWidth = Integer.parseInt(value);
					} else if (HEIGHT_KEY.equalsIgnoreCase(key)) {
						mLayerHeight = Integer.parseInt(value);
					} else if (DATA_KEY.equalsIgnoreCase(key)) {
						layers_data = value;
					}
				}

				line = reader.readLine();
			}

			if (mLayerCount == 0 || mLayerWidth == 0 || mLayerHeight == 0 ||
					layers_data == null || layers_data.length() % 3 != 0)
				throw new LoadExeption(INVALID_DATA);

			mUnfinished = (layers_data.length() / 3 != 144);
			mLayout = new boolean[mLayerCount * mLayerHeight * mLayerWidth];

			for (int i=0; i<layers_data.length(); i+=3) {
				int layer	= layers_data.charAt(i) - 'A';
				int row		= layers_data.charAt(i+1) - 'A';
				int collumn	= layers_data.charAt(i+2) - 'A';

				if (layer < 0 || layer >= mLayerCount ||
					row < 0 || row >= mLayerHeight ||
					collumn < 0 || collumn >= mLayerWidth) {
					throw new LoadExeption(INVALID_DATA);
				}

				mLayout[layer * mLayerHeight * mLayerWidth + row * mLayerWidth + collumn] = true;
			}

		} catch (IOException e) {
			throw new LoadExeption(e.getMessage());
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
			}
		}
	}

	//--------------------------------------------------------------------------
	int getId() {
		return mID;
	}

	//--------------------------------------------------------------------------
	boolean isUnfinished() {
		return mUnfinished;
	}

	//--------------------------------------------------------------------------
	String getName() {
		return mName;
	}

	//--------------------------------------------------------------------------
	String getAuthor() {
		return mAuthor;
	}

	//--------------------------------------------------------------------------
	String getComment() {
		return mComment;
	}

	//--------------------------------------------------------------------------
	void setInfo(String name, String author, String comment) {
		mName = name;
		mAuthor = author;
		mComment = comment;
	}

	//--------------------------------------------------------------------------
	int getLayerCount() {
		return mLayerCount;
	}

	//--------------------------------------------------------------------------
	int getLayerHeight() {
		return mLayerHeight + 1;
	}

	//--------------------------------------------------------------------------
	int getLayerWidth() {
		return mLayerWidth + 1;
	}

	//--------------------------------------------------------------------------
	boolean isPlace(int layer, int y, int x) {
		if (y < mLayerHeight && x < mLayerWidth)
			return mLayout[layer * mLayerHeight * mLayerWidth + y * mLayerWidth + x];

		return false;
	}

	//--------------------------------------------------------------------------
	final Bitmap getPreview() {
		return mPreview;
	}

	//--------------------------------------------------------------------------
	void createPreview(final int width, final int height) {
		mPreview = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas g = new Canvas(mPreview);
		g.drawARGB(255, 0, 128, 0);

		int layer_width = this.mLayerWidth + 1;
		int layer_height = this.mLayerHeight + 1;

		final int left_count = getLeftLayerCount();
		final int top_count = getTopLayerCount();

		int die_width_2 = width / layer_width;
		if (width - die_width_2 * layer_width < SIDE_WALL_SIZE * left_count)
			die_width_2 = (width - SIDE_WALL_SIZE * left_count) / layer_width;

		int die_height_2 = height / layer_height;
		if (height - die_height_2 * layer_height < SIDE_WALL_SIZE * top_count)
			die_height_2 = (height - SIDE_WALL_SIZE * top_count) / layer_height;

		if (die_width_2 - die_height_2 > 2)
			die_width_2 = die_height_2 + 2;

		int die_width = 2 * die_width_2;
		int die_height = 2 * die_height_2;

		int off_x = (width - die_width_2 * layer_width - left_count * SIDE_WALL_SIZE) / 2;
		int off_y = (height - die_height_2 * layer_height - top_count * SIDE_WALL_SIZE) / 2 +
				(top_count - 1) * SIDE_WALL_SIZE;

		Paint border_paint = new Paint();
		border_paint.setAntiAlias(false);
		border_paint.setStyle(Paint.Style.STROKE);
		border_paint.setStrokeWidth(1);
		border_paint.setColor(0xFF909090);

		Paint fill_paint = new Paint();
		fill_paint.setAntiAlias(false);
		fill_paint.setStyle(Paint.Style.FILL);
		fill_paint.setColor(0xFFE0E0E0);

		Paint side_wall_paint = new Paint();
		side_wall_paint.setAntiAlias(false);
		side_wall_paint.setStyle(Paint.Style.STROKE);
		side_wall_paint.setStrokeWidth(1);
		side_wall_paint.setColor(0xFFAFAFAF);

		int i, j, k, x, y, n, x2, y2;

		for (i=0; i<mLayerCount; i++) {
			for (k=0, y=off_y; k<mLayerHeight; k++, y += die_height_2)
				for (j=0, x=off_x; j<mLayerWidth; j++, x += die_width_2)
					if (isPlace(i, k, j)) {
						x2 = x + die_width + SIDE_WALL_SIZE - 1;
						y2 = y + die_height + SIDE_WALL_SIZE - 1;

						g.drawLine(x, y + SIDE_WALL_SIZE, x, y2, border_paint);
						g.drawLine(x + 1, y2, x2 - SIDE_WALL_SIZE + 1, y2, border_paint);

						for (n=1; n<SIDE_WALL_SIZE; n++) {
							g.drawLine(x + n, y + SIDE_WALL_SIZE - n, x + n, y2, side_wall_paint);
							g.drawLine(x + 1, y2 - n, x2 - SIDE_WALL_SIZE + n + 1, y2 - n, side_wall_paint);
						}
					}

			for (k=0, y=off_y; k<mLayerHeight; k++, y+=die_height_2)
				for (j=0, x=off_x + SIDE_WALL_SIZE; j<mLayerWidth; j++, x += die_width_2)
					if (isPlace(i, k, j)) {
						x2 = x + die_width - 1;

						g.drawRect(x, y + 1, x2, y + die_height, fill_paint);
						g.drawLine(x, y, x2, y, border_paint);
						g.drawLine(x2, y + 1, x2, y + die_height, border_paint);
					}

			off_x += SIDE_WALL_SIZE;
			off_y -= SIDE_WALL_SIZE;
		}

		if (mUnfinished) {
			Bitmap icon = BitmapFactory.decodeResource(mContext.getResources(),
					R.drawable.icon_unfinished);

			x = (width - icon.getWidth()) / 2;
			y = (height - icon.getHeight()) / 2;
			g.drawBitmap(icon, x, y, new Paint());
		}
	}

	//--------------------------------------------------------------------------
	int getTopLayerCount() {
		int i, k;

		for (i = mLayerCount-1; i >= 0; i--)
			for (k = 0; k < mLayerWidth-1; k++)
				if (isPlace(i, 0, k))
					return i+1;

		return 0;
	}

	//--------------------------------------------------------------------------
	int getLeftLayerCount() {
		int i, k, left = mLayerWidth - 1;

		for (i = mLayerCount-1; i >= 0; i--)
			for (k = 0; k < mLayerHeight; k++)
				if (isPlace(i, k, left))
					return i+1;

		return 0;
	}

	//--------------------------------------------------------------------------
	final int getWins() {
		return mWins;
	}

	//--------------------------------------------------------------------------
	final void increaseWins() {
		mWins++;
	}

	//--------------------------------------------------------------------------
	final int getLosses() {
		return mLosses;
	}

	//--------------------------------------------------------------------------
	final void increaseLosses() {
		mLosses++;
	}

	//--------------------------------------------------------------------------
	final int getBestTime() {
		return mBestTime;
	}

	//--------------------------------------------------------------------------
	final int getAvgTotalGames() {
		return mAvgTotalGames;
	}
	//--------------------------------------------------------------------------
	final int getAvgTotalTime() {
		return mAvgTotalTime;
	}
	//--------------------------------------------------------------------------
	final int getAvgUndos() {
		return mAvgUndos;
	}
	//--------------------------------------------------------------------------
	final int getAvgShuffles() {
		return mAvgShuffles;
	}

	//--------------------------------------------------------------------------
	final int updateBestTime(int time, int undos, int shuffles) {
		int mPrevBestTime = mBestTime; 
		if (mBestTime == 0 || time < mBestTime)
			mBestTime = time;
		mAvgTotalGames++;
		mAvgTotalTime += time;
		mAvgUndos += undos;
		mAvgShuffles += shuffles;
		return mPrevBestTime;
	}

	//--------------------------------------------------------------------------
	private final static char SEPARATOR = ';';

	void loadStatistics() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		String key = STATISTICS_KEY + mID;
		String value = prefs.getString(key, null);

		if (value != null) {
			String[] fields = value.split(";");
			mWins = (fields.length > 0) ? Integer.parseInt(fields[0]) : 0;
			mLosses = (fields.length > 1) ? Integer.parseInt(fields[1]) : 0;
			mBestTime = (fields.length > 2) ? Integer.parseInt(fields[2]) : 0;
			mAvgTotalGames = (fields.length > 3) ? Integer.parseInt(fields[3]) : 0;
			mAvgTotalTime = (fields.length > 4) ? Integer.parseInt(fields[4]) : 0;
			mAvgUndos = (fields.length > 5) ? Integer.parseInt(fields[5]) : 0;
			mAvgShuffles = (fields.length > 6) ? Integer.parseInt(fields[6]) : 0;
		} else {
			mLosses = 0;
			mBestTime = 0;
			mAvgTotalGames = 0;
			mAvgTotalTime = 0;
			mAvgUndos = 0;
			mAvgShuffles = 0;
		}
	}

	//--------------------------------------------------------------------------
	void storeStatistics() {
		if (mWins + mLosses == 0)
			return;

		String key = STATISTICS_KEY + mID;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		SharedPreferences.Editor editor = prefs.edit();

		StringBuilder builder = new StringBuilder();
		builder.append(mWins);
		builder.append(SEPARATOR);
		builder.append(mLosses);
		builder.append(SEPARATOR);
		builder.append(mBestTime);
		builder.append(SEPARATOR);
		builder.append(mAvgTotalGames);
		builder.append(SEPARATOR);
		builder.append(mAvgTotalTime);
		builder.append(SEPARATOR);
		builder.append(mAvgUndos);
		builder.append(SEPARATOR);
		builder.append(mAvgShuffles);

		editor.putString(key, builder.toString());
		editor.commit();
	}

	//--------------------------------------------------------------------------
	void clearStatistics() {
		mWins = mLosses = mBestTime =
			mAvgTotalGames = mAvgTotalTime =
			mAvgUndos = mAvgShuffles = 0;
	}

	//--------------------------------------------------------------------------
	@Override
	public int compareTo(MahjonggData another) {
		if (mName == null)
			return another.getName() != null ? 0xFF : 0;

		if (another.getName() == null)
			return -0xFF;

		return mName.compareTo(another.getName());
	}

	//--------------------------------------------------------------------------
	void setData(int[][][] layers, int layer_count, int free_left, int free_top,
			int free_right, int free_bottom) {

		mLayerCount		= layer_count;
		mLayerHeight	= layers[0].length - free_top - free_bottom;
		mLayerWidth		= layers[0][0].length - free_left - free_right;

		mLayout = new boolean[mLayerCount * mLayerHeight * mLayerWidth];

		int n = 0;

		for (int i=0; i<layer_count; i++)
			for (int j=0; j<mLayerHeight; j++)
				for (int k=0; k<mLayerWidth; k++) {
					if (layers[i][free_top+j][free_left+k] >= 0) {
						mLayout[n] = true;
					}
					n++;
				}
	}

	//--------------------------------------------------------------------------
	static int[] getUserGameIds(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String user_list = prefs.getString(USER_GAME_LIST_KEY, null);
		if (user_list != null) {
			Vector<Integer> list = new Vector<Integer>();

			Scanner scanner = new Scanner(user_list);
			scanner.useDelimiter(";");

			while (scanner.hasNext()) {
				try {
					list.add(new Integer(scanner.next()));
				} catch (NumberFormatException ex) {
				}
			}

			int[] result = new int[list.size()];
			for (int i=0; i<result.length; i++) {
				result[i] = list.get(i).intValue();
			}

			return result;
		}

		return new int[0];
	}

	//--------------------------------------------------------------------------
	private void updateID() {
		if (mID < 0) {
			int[] id_list = getUserGameIds(mContext);
			boolean exists;

			mID = USER_GAME_ID;
			do {
				mID++;
				exists = false;

				for (int i=0; i<id_list.length; i++)
					if (id_list[i] == mID) {
						exists = true;
						break;
					}
			} while (exists);

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
			SharedPreferences.Editor editor = prefs.edit();
			String user_list = prefs.getString(USER_GAME_LIST_KEY, null);

			if (user_list == null)
				user_list = Integer.toString(mID);
			else
				user_list += ";" + mID;

			editor.putString(USER_GAME_LIST_KEY, user_list);
			editor.commit();
		}
	}

	//--------------------------------------------------------------------------
	void store() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		SharedPreferences.Editor editor = prefs.edit();

		if (mID < 0)
			updateID();

		editor.putString(USER_GAME_KEY + mID, getStoreData());
		editor.commit();
	}

	//--------------------------------------------------------------------------
	String getStoreData() {
		StringBuilder builder = new StringBuilder();

		if (mID > 0)
			builder.append(ID_KEY + '=').append(mID).append('\n');

		if (mName != null)
			builder.append(NAME_KEY + '=').append(mName).append('\n');

		if (mAuthor != null)
			builder.append(AUTHOR_KEY + '=').append(mAuthor).append('\n');

		if (mComment != null)
			builder.append(COMMENT_KEY + '=').append(mComment).append('\n');

		builder.append(LAYERCOUNT_KEY + '=').append(mLayerCount).append('\n');
		builder.append(HEIGHT_KEY + '=').append(mLayerHeight).append('\n');
		builder.append(WIDTH_KEY + '=').append(mLayerWidth).append('\n');

		int n = 0;

		builder.append(DATA_KEY + '=');

		for (int i=0; i<mLayerCount; i++)
			for (int j=0; j<mLayerHeight; j++)
				for (int k=0; k<mLayerWidth; k++) {
					if (mLayout[n]) {
						builder.append((char)('A' + i));
						builder.append((char)('A' + j));
						builder.append((char)('A' + k));
					}
					n++;
				}

		builder.append('\n');

		return builder.toString();
	}

	//--------------------------------------------------------------------------
	static boolean deleteGame(Context context, int id) {
		int[] ids = getUserGameIds(context);

		for (int i=0; i<ids.length; i++)
			if (ids[i] == id) {
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
				SharedPreferences.Editor editor = prefs.edit();

				editor.remove(USER_GAME_KEY + id);
				editor.remove(STATISTICS_KEY + id);

				if (ids.length > 1) {
					StringBuilder builder = new StringBuilder();

					for (int k=0; k<ids.length; k++)
						if (k != i) {
							if (builder.length() > 0)
								builder.append(';');

							builder.append(Integer.toString(ids[k]));
						}
				} else {
					editor.remove(USER_GAME_LIST_KEY);
				}

				editor.commit();
				return true;
			}

		return false;
	}

	//--------------------------------------------------------------------------
	void showInfo(Context context) {

		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle(mName);

		View view = LayoutInflater.from(context).inflate(R.layout.game_info, null);

		if (mAuthor != null) {
			TextView text = (TextView)view.findViewById(R.id.game_info_author);
			text.setText(mAuthor);
		} else {
			view.findViewById(R.id.game_info_author_layout).setVisibility(View.GONE);
		}

		if (mComment != null) {
			TextView text = (TextView)view.findViewById(R.id.game_info_comment);
			text.setText(mComment);
		} else {
			view.findViewById(R.id.game_info_comment_layout).setVisibility(View.GONE);
		}

		dialog.setView(view);
		dialog.setCancelable(true);
		dialog.setPositiveButton(android.R.string.ok, null);
		dialog.show();
	}
}

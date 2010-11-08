package com.anoshenko.android.mahjongg;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Scanner;
import java.util.Vector;

import com.anoshenko.android.background.BackgroundActivity;
import com.anoshenko.android.mahjongg.MahjonggData.LoadExeption;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TabHost;

public class SelectActivity extends TabActivity implements PopupMenu.Listener {

	private final static String FAVORITES_TAG = "FAVORITES";
	private final static String GAME_LIST_TAG = "GAME_LIST";

	private final static String FAVORITES_KEY = "FAVORITES";
	private final static char FAVORITE_SEPARATOR = ';';

	private final static String SOLITAIRES_KEY = "SOLITAIRES";
	private final static String CURRENT_PAGE_KEY = "CURRENT_PAGE";

	private final Vector<MahjonggData> mFavorites = new Vector<MahjonggData>();
	private final Vector<MahjonggData> mAllGames = new Vector<MahjonggData>();

	@SuppressWarnings("unused")
	private GameListAdapter mFavoritesAdapter, mAllGamesAdapter;

	private int mPreviewWidth, mPreviewHeight;
	private TabHost mTabHost;
	public final Handler mHandler = new Handler();

	//--------------------------------------------------------------------------
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Utils.setOrientation(this);

		super.onCreate(savedInstanceState);

		mTabHost = getTabHost();

		LayoutInflater.from(this).inflate(R.layout.game_select,
				mTabHost.getTabContentView(), true);

		Resources res = getResources();
		TabHost.TabSpec tab;
		Bitmap icon;

		icon = BitmapFactory.decodeResource(res, R.drawable.icon_favorites);
		tab = mTabHost.newTabSpec(FAVORITES_TAG);
		tab.setIndicator("Favorites", new BitmapDrawable(icon));
		tab.setContent(R.id.FavoritesList);
		mTabHost.addTab(tab);

		icon = BitmapFactory.decodeResource(res, R.drawable.icon_all_games);
		tab = mTabHost.newTabSpec(GAME_LIST_TAG);
		tab.setIndicator("All games", new BitmapDrawable(icon));
		tab.setContent(R.id.AllGamesList);
		mTabHost.addTab(tab);

		createGameList();

		Bitmap bitmap = Bitmap.createBitmap(mPreviewWidth, mPreviewHeight, Bitmap.Config.ARGB_8888);
		Canvas g = new Canvas(bitmap);
		g.drawARGB(255, 0, 128, 0);

		mFavoritesAdapter = new GameListAdapter(this, mFavorites, bitmap,
				(ListView)findViewById(R.id.FavoritesList));

		mAllGamesAdapter = new GameListAdapter(this, mAllGames, bitmap,
				(ListView)findViewById(R.id.AllGamesList));

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		int current = Integer.parseInt(prefs.getString(getString(R.string.pref_start_tab_key), "0"));
		if (savedInstanceState != null)
			current = savedInstanceState.getInt(CURRENT_PAGE_KEY, current);

		mTabHost.setCurrentTab(current);

		(new Thread(new GameLoader())).start();

		boolean solitaires = prefs.getBoolean(SOLITAIRES_KEY, false);
		if (!solitaires) {
			if (!Utils.isSolitairesInstalled(this)) {
				AlertDialog.Builder dialog = new AlertDialog.Builder(this);

				dialog.setMessage("I present my new free program \"250+ Solitaire Collection\".");
				dialog.setPositiveButton("Install", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Utils.installSolitaires(SelectActivity.this);
					}
				});
				dialog.setNegativeButton("Close", null);
				dialog.setCancelable(true);
				dialog.show();
			}

			SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean(SOLITAIRES_KEY, true);
			editor.commit();
		}
	}

	//--------------------------------------------------------------------------
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(CURRENT_PAGE_KEY, mTabHost.getCurrentTab());
	}

	//--------------------------------------------------------------------------
	@Override
	public void onDestroy() {
		storeFavoritesList();
		super.onDestroy();
	}

	//--------------------------------------------------------------------------
	private class FavoritesListUpdater implements Runnable {
		@Override
		public void run() {
			((ListView)findViewById(R.id.FavoritesList)).invalidateViews();
		}
	}

	//--------------------------------------------------------------------------
	private class AllGameListUpdater implements Runnable {
		@Override
		public void run() {
			((ListView)findViewById(R.id.AllGamesList)).invalidateViews();
		}
	}

	//--------------------------------------------------------------------------
	private class GameLoader implements Runnable {
		@Override
		public void run() {
			for (MahjonggData mahjongg : mAllGames) {
				mahjongg.createPreview(mPreviewWidth, mPreviewHeight);

				if (mTabHost.getCurrentTab() == 0) {
					if (mFavorites.contains(mahjongg))
						mHandler.post(new FavoritesListUpdater());
				} else {
					mHandler.post(new AllGameListUpdater());
				}
			}
		}
	}

	//--------------------------------------------------------------------------
	private void createGameList() {
		DisplayMetrics dm = Utils.getDisplayMetrics(this);

		int x = Math.min(dm.heightPixels, dm.widthPixels);
		mPreviewWidth = x * 2 / 5;
		mPreviewHeight = mPreviewWidth * 3 / 5;

		mFavorites.clear();
		mAllGames.clear();

		InputStream list_stream = getResources().openRawResource(R.raw.games_list);
		try {
			while (list_stream.available() > 0) {
				try {
					mAllGames.add(new MahjonggData(this, list_stream));
				} catch (LoadExeption e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		int[] ids = MahjonggData.getUserGameIds(this);
		for (int i=0; i<ids.length; i++)
			try {
				mAllGames.add(new MahjonggData(this, ids[i]));
			} catch (LoadExeption e) {
				e.printStackTrace();
			}

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String favorites = prefs.getString(FAVORITES_KEY, null);

		if (favorites == null) {
			for (MahjonggData data : mAllGames)
				if (data.getId() == 7) {
					mFavorites.add(data);
					break;
				}
		} else {
			Scanner scanner = new Scanner(favorites);
			scanner.useDelimiter("" + FAVORITE_SEPARATOR);
			while (scanner.hasNext()) {
				int id = Integer.parseInt(scanner.next());
				for (MahjonggData data : mAllGames)
					if (data.getId() == id) {
						mFavorites.add(data);
						break;
					}
			}
		}
		Collections.sort(mFavorites);
	}

	//--------------------------------------------------------------------------
	public void onItemClick(MahjonggData mahjongg) {
		if (mahjongg.isUnfinished()) {
			Utils.Note(this, R.string.disable_play);
		} else {
			Intent intent = new Intent(this, PlayActivity.class);
			intent.putExtra(BaseActivity.GAME_ID_KEY, mahjongg.getId());
			startActivityForResult(intent, Command.PLAY_ACTIVITY);
		}
	}

	//--------------------------------------------------------------------------
	private MahjonggData mLastMahjongg;

	public void onItemLongClick(MahjonggData mahjongg) {
		mLastMahjongg = mahjongg;

		PopupMenu menu = new PopupMenu(this, this);

		menu.setTitle(mahjongg.getName());

		menu.addItem(Command.PLAY, R.string.play_item, R.drawable.icon_start,
				mahjongg.getId() < MahjonggData.USER_GAME_ID || !mahjongg.isUnfinished());

		if (mTabHost.getCurrentTab() == 0)
			menu.addItem(Command.REMOVE_FAVORITE, R.string.remove_favorite_item, R.drawable.icon_favorite_remove);
		else
			menu.addItem(Command.ADD_FAVORITE, R.string.add_to_favorites_item, R.drawable.icon_favorite_add);

		menu.addItem(Command.STATISTICS, R.string.statistics_item, R.drawable.icon_statistics);

		if (mahjongg.getId() >= MahjonggData.USER_GAME_ID) {
			menu.addItem(Command.EDIT_USER_GAME, R.string.edit_item, R.drawable.icon_builder);
			menu.addItem(Command.DELETE_USER_GAME, R.string.delete_item, R.drawable.icon_delete);
			menu.addItem(Command.USER_GAME_INFO, R.string.info_item, R.drawable.icon_info,
					mahjongg.getAuthor() != null || mahjongg.getComment() != null);

			//if (!mahjongg.isUnfinished())
			//	menu.addItem(Command.PUBLISH_USER_GAME, R.string.publish_item, R.drawable.icon_publish);
		}

		menu.show();
	}

	//--------------------------------------------------------------------------
	@Override
	public void onPopupMenuSelect(int command) {
		if (mLastMahjongg != null) {
			switch (command) {
			case Command.PLAY:
				onItemClick(mLastMahjongg);
				break;

			case Command.ADD_FAVORITE:
				addToFavorites(mLastMahjongg);
				break;

			case Command.REMOVE_FAVORITE:
				removeFromFavorites(mLastMahjongg);
				break;

			case Command.STATISTICS:
				mLastMahjongg.loadStatistics();
				StatisticsDialog.show(this, mLastMahjongg);
				break;

			case Command.EDIT_USER_GAME:
				startBuilder(mLastMahjongg.getId());
				break;

			case Command.DELETE_USER_GAME:
				Utils.Question(this,
						getString(R.string.delete_game, mLastMahjongg.getName() == null ?
								getString(R.string.nameless) : mLastMahjongg.getName()),
						new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (MahjonggData.deleteGame(SelectActivity.this, mLastMahjongg.getId())) {
							mAllGames.remove(mLastMahjongg);
							((ListView)findViewById(R.id.AllGamesList)).invalidateViews();
							if (mFavorites.remove(mLastMahjongg))
								((ListView)findViewById(R.id.FavoritesList)).invalidateViews();
						}
					}});
				break;

			case Command.PUBLISH_USER_GAME:
				// TODO
				break;

			case Command.USER_GAME_INFO:
				mLastMahjongg.showInfo(this);
				break;
			}
		}
	}

	//--------------------------------------------------------------------------
	private void removeFromFavorites(final MahjonggData game) {
		String message = getString(R.string.remove_from_favorites, game.getName());
		Utils.Question(this, message, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				for (MahjonggData data : mFavorites)
					if (data.getId() == game.getId()) {
						mFavorites.remove(data);
						Collections.sort(mFavorites);
						storeFavoritesList();
						((ListView)findViewById(R.id.FavoritesList)).invalidateViews();
						return;
					}
			}
		});
	}

	//--------------------------------------------------------------------------
	private void addToFavorites(final MahjonggData game) {
		for (MahjonggData data : mFavorites)
			if (data.getId() == game.getId()) {
				String message = getString(R.string.already_in_favorites, game.getName());
				Utils.Note(this, message);
				return;
			}

		mFavorites.add(game);
		Collections.sort(mFavorites);
		storeFavoritesList();
		((ListView)findViewById(R.id.FavoritesList)).invalidateViews();

		String message = getString(R.string.added_to_favorites, game.getName());
		Utils.Note(this, message);
	}

	//--------------------------------------------------------------------------
	private void storeFavoritesList() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = prefs.edit();

		StringBuilder builder = new StringBuilder();
		for (MahjonggData data : mFavorites) {
			builder.append(data.getId());
			builder.append(FAVORITE_SEPARATOR);
		}

		editor.putString(FAVORITES_KEY, builder.toString());
		editor.commit();
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Utils.createMenu(this, menu, true);
		return true;
	}

	//--------------------------------------------------------------------------
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {

		switch (item.getItemId()) {
		case Command.ABOUT:
			Utils.showAbout(this);
			return true;

		case Command.BACKGROUND:
			startActivityForResult(new Intent(this, BackgroundActivity.class), Command.BACKGROUND_ACTIVITY);
			return true;

		case Command.SETTINGS:
			startActivityForResult(new Intent(this, SettingsActivity.class), Command.SETTINGS_ACTIVITY);
			return true;

		case Command.BUILDER:
			startBuilder(-1);
			return true;

		case Command.SOLITAIRES:
			Utils.installSolitaires(this);
			return true;
		}

		return false;
	}

	//--------------------------------------------------------------------------
	private void startBuilder(int edit_id) {
		Intent intent = new Intent(this, BuilderActivity.class);
		intent.putExtra(BaseActivity.GAME_ID_KEY, edit_id);
		startActivityForResult(intent, Command.BUILDER_ACTIVITY);
	}

	//--------------------------------------------------------------------------
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case Command.PLAY_ACTIVITY:
			if (resultCode == Command.BUILDER) {
				startBuilder(-1);
			}
			break;

		case Command.BACKGROUND_ACTIVITY:
			break;

		case Command.SETTINGS_ACTIVITY:
			Utils.setOrientation(this);
			break;

		case Command.BUILDER_ACTIVITY:
			if (resultCode >= 0) {
				boolean favorite = false;

				for (MahjonggData game : mAllGames)
					if (game.getId() == resultCode) {
						mAllGames.remove(game);
						if (mFavorites.indexOf(game) >= 0) {
							mFavorites.remove(game);
							favorite = true;
						}
						break;
					}

				try {
					MahjonggData game = new MahjonggData(this, resultCode);
					game.createPreview(mPreviewWidth, mPreviewHeight);

					mAllGames.add(game);
					Collections.sort(mAllGames);
					((ListView)findViewById(R.id.AllGamesList)).invalidateViews();

					if (favorite) {
						mFavorites.add(game);
						Collections.sort(mFavorites);
						((ListView)findViewById(R.id.FavoritesList)).invalidateViews();
					}

				} catch (LoadExeption e) {
					e.printStackTrace();
				}
			}
			break;
		}
	}
}

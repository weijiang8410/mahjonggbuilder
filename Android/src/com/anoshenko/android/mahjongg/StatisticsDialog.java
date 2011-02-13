package com.anoshenko.android.mahjongg;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TableRow;
import android.widget.TextView;

class StatisticsDialog {

	static StatisticsDialog Instance;
	private final Context mContext;
	private final MahjonggData mData;
	private View view;
	private Runnable mStartRun, mExitRun;

	//--------------------------------------------------------------------------
	private StatisticsDialog(Context context, MahjonggData data,
			Runnable start_run, Runnable exit_run) {

		mData = data;
		mContext = context;
		mStartRun = start_run;
		mExitRun = exit_run;
	}

	//--------------------------------------------------------------------------
	private StatisticsDialog(Context context, MahjonggData data) {

		mData = data;
		mContext = context;
	}

	//--------------------------------------------------------------------------
	public static void show(Context context, MahjonggData data, int current_time, int prev_best_time,
			Runnable start_run, Runnable exit_run) {
		Instance = new StatisticsDialog(context, data, start_run, exit_run);
		Instance.show(true, current_time, prev_best_time);
	}

	//--------------------------------------------------------------------------
	public static void show(Context context, MahjonggData data) {
		Instance = new StatisticsDialog(context, data);
		Instance.show(false, -1, -1);
	}

	//--------------------------------------------------------------------------
	private void show(boolean f_win_message, int current_time, int prev_best_time) {

		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		view = LayoutInflater.from(mContext).inflate(R.layout.statistics_view, null);

		TextView text_view = (TextView)view.findViewById(R.id.StatisticsGameName);
		text_view.setText(f_win_message ? R.string.win_message : R.string.statistics_title);

		int total = mData.getWins() + mData.getLosses();
		if(total > 0)
		{
			text_view = (TextView)view.findViewById(R.id.StatisticsTotal);
			text_view.setText(Integer.toString(total));

			String wins_text, losses_text;
			if(mData.getWins() == 0) {
				wins_text = "0 (0.00%)";
				losses_text = String.format("%d (100.00%%)", mData.getLosses());
			} else if(mData.getLosses() == 0) {
				wins_text = String.format("%d (100.00%%)", mData.getWins());
				losses_text = "0 (0.00%)";
			} else {
				int percent = mData.getWins() * 10000 / total;
				int fraction = percent % 100;
				percent /= 100;

				wins_text = String.format("%d (%d.%02d%%)", mData.getWins(), percent, fraction);

				percent = 100 - percent;
				if(fraction > 0) {
					percent--;
					fraction = 100 - fraction;
				}

				losses_text = String.format("%d (%d.%02d%%)", mData.getLosses(), percent, fraction);
			}

			text_view = (TextView)view.findViewById(R.id.StatisticsWins);
			text_view.setText(wins_text);

			text_view = (TextView)view.findViewById(R.id.StatisticsLosses);
			text_view.setText(losses_text);
		}

		if(mData.getBestTime() > 0) {
			text_view = (TextView)view.findViewById(R.id.StatisticsBestTimeName);
			text_view.setText(current_time == mData.getBestTime() ? R.string.highscore_best_time_text : R.string.best_time_text);
		    text_view = (TextView)view.findViewById(R.id.StatisticsBestTime);
			text_view.setText(String.format("%d:%02d", mData.getBestTime() / 60, mData.getBestTime() % 60));
		}

		if (current_time >= 0) {
			if (current_time == mData.getBestTime()) {
				text_view = (TextView)view.findViewById(R.id.StatisticsCurrentTimeName);
				text_view.setText(R.string.highscore_current_time_text);
				text_view = (TextView)view.findViewById(R.id.StatisticsCurrentTime);
				text_view.setText(String.format("%d:%02d", prev_best_time / 60, prev_best_time % 60));
			} else {
				text_view = (TextView)view.findViewById(R.id.StatisticsCurrentTimeName);
				text_view.setText(R.string.current_time_text);
				text_view = (TextView)view.findViewById(R.id.StatisticsCurrentTime);
				text_view.setText(String.format("%d:%02d", current_time / 60, current_time % 60));
			}
		} else {
			TableRow row = (TableRow)view.findViewById(R.id.StatisticsCurrentTimeRow);
			row.setVisibility(View.GONE);
		}

		if (mData.getAvgTotalGames() > 0) {
			text_view = (TextView)view.findViewById(R.id.StatisticsAvg);
			text_view.setText(String.format(mContext.getResources().getString(R.string.statistics_average_value_text),
					(mData.getAvgTotalTime() / mData.getAvgTotalGames()) / 60,
					(mData.getAvgTotalTime() / mData.getAvgTotalGames()) % 60,
					(float) mData.getAvgUndos() / mData.getAvgTotalGames(),
					(float) mData.getAvgShuffles() / mData.getAvgTotalGames(),
					mData.getAvgTotalGames()));
		} else {
			TableRow row = (TableRow)view.findViewById(R.id.StatisticsAvgRow);
			row.setVisibility(View.GONE);
		}

	builder.setView(view);
		builder.setCancelable(true);
		builder.setTitle(mData.getName());

		if(f_win_message) {
			builder.setPositiveButton(R.string.start_button, mStart);
			builder.setNegativeButton(R.string.exit_button, mExit);
			builder.setOnCancelListener(mExitBack);
		} else {
			builder.setPositiveButton(R.string.clear_button, mClear);
			builder.setNegativeButton(R.string.close_button, mClose);
			builder.setOnCancelListener(mBack);
		}

		builder.show();
	}

	//--------------------------------------------------------------------------
	private DialogInterface.OnClickListener mClear = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int arg1) {
			if (mData.getWins() + mData.getLosses() > 0) {
				mData.clearStatistics();
				mData.storeStatistics();

				TextView text_view = (TextView)view.findViewById(R.id.StatisticsWins);
				text_view.setText("0 (0.00%)");

				text_view = (TextView)view.findViewById(R.id.StatisticsLosses);
				text_view.setText("0 (0.00%)");

				text_view = (TextView)view.findViewById(R.id.StatisticsTotal);
				text_view.setText("0");

				text_view = (TextView)view.findViewById(R.id.StatisticsBestTime);
				text_view.setText("0:00");

				show(false, -1, -1);
			}
		}
	};

	//--------------------------------------------------------------------------
	private DialogInterface.OnClickListener mClose = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int arg1) {
			dialog.dismiss();
			Instance = null;
		}
	};

	//--------------------------------------------------------------------------
	private DialogInterface.OnCancelListener mBack = new DialogInterface.OnCancelListener() {
		@Override
		public void onCancel(DialogInterface dialog) {
			dialog.dismiss();
			Instance = null;
		}
	};

	//--------------------------------------------------------------------------
	private DialogInterface.OnClickListener mStart = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int arg1) {
			dialog.dismiss();
			Instance = null;
			if (mStartRun != null) {
				(new Handler()).post(mStartRun);
			}
		}
	};

	//--------------------------------------------------------------------------
	private DialogInterface.OnClickListener mExit = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int arg1) {
			dialog.dismiss();
			Instance = null;
			if (mExitRun != null) {
				(new Handler()).post(mExitRun);
			}
		}
	};

	//--------------------------------------------------------------------------
	private DialogInterface.OnCancelListener mExitBack = new DialogInterface.OnCancelListener() {
		@Override
		public void onCancel(DialogInterface dialog) {
			dialog.dismiss();
			Instance = null;
			if (mExitRun != null) {
				(new Handler()).post(mExitRun);
			}
		}
	};
}
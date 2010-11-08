package com.anoshenko.android.mahjongg;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

class GameInfoDialog implements DialogInterface.OnClickListener {

	private final BuilderActivity mActivity;
	private AlertDialog mDialog;

	//--------------------------------------------------------------------------
	private GameInfoDialog(BuilderActivity activity) {
		mActivity = activity;
	}

	//--------------------------------------------------------------------------
	static void show(BuilderActivity activity) {
		GameInfoDialog dialog = new GameInfoDialog(activity);
		dialog.show();
	}

	//--------------------------------------------------------------------------
	private void show() {
		AlertDialog.Builder dialog = new AlertDialog.Builder(mActivity);

		View view = LayoutInflater.from(mActivity).inflate(R.layout.game_name, null);
		EditText editor;
		MahjonggData data = mActivity.mData;

		if (data.getName() != null) {
			editor = (EditText)view.findViewById(R.id.GameNameEdit);
			editor.setText(data.getName());
		}

		if (data.getAuthor() != null) {
			editor = (EditText)view.findViewById(R.id.GameAuthorEdit);
			editor.setText(data.getAuthor());
		}

		if (data.getComment() != null) {
			editor = (EditText)view.findViewById(R.id.GameCommentEdit);
			editor.setText(data.getComment());
		}

		dialog.setView(view);

		dialog.setCancelable(true);
		dialog.setNegativeButton(R.string.cancel_button, null);
		dialog.setPositiveButton(android.R.string.ok, this);

		mDialog = dialog.show();
	}

	//--------------------------------------------------------------------------
	@Override
	public void onClick(DialogInterface dialog, int which) {
		EditText editor = (EditText)mDialog.findViewById(R.id.GameNameEdit);
		String name = editor.getEditableText().toString().trim();
		if (name.length() == 0) name = null;

		editor = (EditText)mDialog.findViewById(R.id.GameAuthorEdit);
		String author = editor.getEditableText().toString().trim();
		if (author.length() == 0) author = null;

		editor = (EditText)mDialog.findViewById(R.id.GameCommentEdit);
		String comment = editor.getEditableText().toString().trim();
		if (comment.length() == 0) comment = null;

		mActivity.mData.setInfo(name, author, comment);
	}
}

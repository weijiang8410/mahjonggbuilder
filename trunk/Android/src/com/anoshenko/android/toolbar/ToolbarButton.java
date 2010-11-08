package com.anoshenko.android.toolbar;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class ToolbarButton {

	public final int mCommand;
	private int mTextId;
	public Bitmap mIcon, mIconDisabled;
	public String mText;
	public boolean mEnabled;

	public ToolbarButton(Resources res, int command, int text, int icon, int icon_disabled) {
		mCommand = command;
		mTextId = text;
		mIcon = BitmapFactory.decodeResource(res, icon);
		mEnabled = true;
		if (icon_disabled >= 0)
			mIconDisabled = BitmapFactory.decodeResource(res, icon_disabled);
		else
			mIconDisabled = mIcon;
	}

	public void loadName(Context context) {
		mText = context.getString(mTextId);
	}

	public void setIcon(Context context, int icon_id) {
		mIcon = BitmapFactory.decodeResource(context.getResources(), icon_id);
	}

	public void setText(Context context, int text_id) {
		mTextId = text_id;
		loadName(context);
	}
}

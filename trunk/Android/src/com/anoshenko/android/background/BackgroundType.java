package com.anoshenko.android.background;

public enum BackgroundType {
	GRADIENT(0), BUILDIN(1); //, EXTERN(2);

	public final int Id;

	BackgroundType(int id) {
		Id = id;
	}
}

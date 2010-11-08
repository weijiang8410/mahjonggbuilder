package com.anoshenko.android.background;

import android.graphics.LinearGradient;
import android.graphics.RadialGradient;
import android.graphics.Shader;

public enum GradientType {
	VERTICAL(0), HORIZONTAL(1), DIAGONAL(2), DIAGONAL2(3), CENTER(4);

	public final int Id;

	GradientType(int id) {
		Id = id;
	}

	public Shader getShader(int width, int height, int color0, int color1) {
		Shader shader = null;

		switch(this) {
		case VERTICAL:
			shader = new LinearGradient(0, 0, 0, height, color0, color1, Shader.TileMode.CLAMP);
			break;

		case HORIZONTAL:
			shader = new LinearGradient(0, 0, width, 0, color0, color1, Shader.TileMode.CLAMP);
			break;

		case DIAGONAL:
			shader = new LinearGradient(0, 0, width, height, color0, color1, Shader.TileMode.CLAMP);
			break;

		case DIAGONAL2:
			shader = new LinearGradient(width, 0, 0, height, color0, color1, Shader.TileMode.CLAMP);
			break;

		case CENTER:
			shader = new RadialGradient(width/2, height/2, Math.max(width, height)/2,
					color0, color1, Shader.TileMode.CLAMP);
			break;
		}

		return shader;
	}
}

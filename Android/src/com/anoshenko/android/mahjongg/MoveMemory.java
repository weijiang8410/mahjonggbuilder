package com.anoshenko.android.mahjongg;

import java.util.Arrays;

class MoveMemory {

	private class Move {
		final int die1, layer1, x1, y1, die2, layer2, x2, y2;

		//----------------------------------------------------------------------
		Move(int die1, int layer1, int x1, int y1, int die2, int layer2, int x2, int y2) {
			this.die1	= die1;
			this.layer1	= layer1;
			this.x1		= x1;
			this.y1		= y1;
			this.die2	= die2;
			this.layer2	= layer2;
			this.x2		= x2;
			this.y2		= y2;
		}

		//----------------------------------------------------------------------
		void Undo() {
			mGame.mLayer[layer1][y1][x1] = die1;
			mGame.mLayer[layer2][y2][x2] = die2;
		}

		//----------------------------------------------------------------------
		void Redo() {
			mGame.mLayer[layer1][y1][x1] = -4;
			mGame.mLayer[layer2][y2][x2] = -4;
		}

		//----------------------------------------------------------------------
		void Store(StringBuilder builder) {
			builder.append((char)(die1 + 0x20));
			builder.append((char)(layer1 + 0x20));
			builder.append((char)(x1 + 0x20));
			builder.append((char)(y1 + 0x20));
			builder.append((char)(die2 + 0x20));
			builder.append((char)(layer2 + 0x20));
			builder.append((char)(x2 + 0x20));
			builder.append((char)(y2 + 0x20));
		}
	}

	private final PlayActivity mGame;
	private Move[] mMemory = new Move[72];
	private int mUndoCount = 0, mRedoCount = 0, mBookmarkCount = 0;
	private int[] mBookmark = new int[10];

	//--------------------------------------------------------------------------
	MoveMemory(PlayActivity game) {
		mGame = game;
	}

	//--------------------------------------------------------------------------
	boolean isUndoAvailable() {
		return mUndoCount > 0;
	}

	//--------------------------------------------------------------------------
	boolean isRedoAvailable() {
		return mRedoCount > 0;
	}

	//--------------------------------------------------------------------------
	boolean isBookmarkExist() {
		return mBookmarkCount > 0;
	}

	//--------------------------------------------------------------------------
	void Reset() {
		mUndoCount = mRedoCount = mBookmarkCount = 0;

		for (int i=0; i<mMemory.length; i++)
			mMemory[i] = null;

		mGame.EnableRedoCommand(false);
		mGame.EnableUndoCommand(false);
	}

	//--------------------------------------------------------------------------
	void add(int die1, int layer1, int x1, int y1, int die2, int layer2, int x2, int y2) {

		mMemory[mUndoCount] = new Move(die1, layer1, x1, y1, die2, layer2, x2, y2);

		if (mUndoCount == 0)
			mGame.EnableUndoCommand(true);

		mUndoCount++;

		if (mRedoCount > 0) {
			mGame.EnableRedoCommand(false);
			mRedoCount = 0;
		}
	}

	//--------------------------------------------------------------------------
	void Undo() {
		if (mUndoCount > 0) {

			mGame.Unmark();

			mUndoCount--;
			mMemory[mUndoCount].Undo();

			if (mUndoCount == 0)
				mGame.EnableUndoCommand(false);

			if (mRedoCount == 0)
				mGame.EnableRedoCommand(true);

			mRedoCount++;

			if (mBookmarkCount > 0) {
				while (mBookmarkCount > 0
						&& mBookmark[mBookmarkCount - 1] > mUndoCount)
					mBookmarkCount--;
			}

			mGame.ResumeMove();
		}
	}

	//--------------------------------------------------------------------------
	void Redo() {
		if (mRedoCount > 0) {

			mGame.Unmark();

			mMemory[mUndoCount].Redo();
			mRedoCount--;

			if (mRedoCount == 0)
				mGame.EnableRedoCommand(false);

			if (mUndoCount == 0)
				mGame.EnableUndoCommand(true);

			mUndoCount++;

			mGame.ResumeMove();
		}
	}

	//--------------------------------------------------------------------------
	void setBookmark() {
		if ((mBookmarkCount > 0) && (mBookmark[mBookmarkCount - 1] == mUndoCount))
			return;

		if (mBookmarkCount == mBookmark.length) {
			int[] old = mBookmark;

			mBookmark = new int[old.length * 2];
			for (int i = 0; i < old.length; i++)
				mBookmark[i] = old[i];
		}

		mBookmark[mBookmarkCount] = mUndoCount;
		mBookmarkCount++;
	}

	//--------------------------------------------------------------------------
	void backToBookmark() {
		if ((mBookmarkCount > 0) && (mBookmark[mBookmarkCount - 1] == mUndoCount)) {
			mBookmarkCount--;
		}

		if (mBookmarkCount > 0) {

			mBookmarkCount--;

			mGame.Unmark();

			while (mBookmark[mBookmarkCount] < mUndoCount) {
				mUndoCount--;
				mRedoCount++;
				mMemory[mUndoCount].Undo();
			}

			if (mUndoCount == 0)
				mGame.EnableUndoCommand(false);

			if (mRedoCount > 0)
				mGame.EnableRedoCommand(true);

			mGame.ResumeMove();
		}
	}

	//--------------------------------------------------------------------------
	void Store(StringBuilder builder) {
		builder.append((char)(0x20 + mUndoCount));
		builder.append((char)(0x20 + mRedoCount));
		builder.append((char)(0x20 + mBookmarkCount));

		int i;
		for (i = 0; i < (mUndoCount + mRedoCount); i++)
			mMemory[i].Store(builder);

		for (i = 0; i < mBookmarkCount; i++)
			builder.append((char)(0x20 + mBookmark[i]));
	}

	//--------------------------------------------------------------------------
	void Load(String data) {
		mUndoCount = (int) data.charAt(0) - 0x20;
		mRedoCount = (int) data.charAt(1) - 0x20;
		mBookmarkCount = (int) data.charAt(2) - 0x20;

		int i, n = 3;
		for (i = 0; i < (mUndoCount + mRedoCount); i++) {
			mMemory[i] = new Move(
					(int) data.charAt(n) - 0x20,
					(int) data.charAt(n + 1) - 0x20,
					(int) data.charAt(n + 2) - 0x20,
					(int) data.charAt(n + 3) - 0x20,
					(int) data.charAt(n + 4) - 0x20,
					(int) data.charAt(n + 5) - 0x20,
					(int) data.charAt(n + 6) - 0x20,
					(int) data.charAt(n + 7) - 0x20);
			n += 8;
		}

		for (i = 0; i < mBookmarkCount; i++) {
			mBookmark[i] = (int) data.charAt(n) - 0x20;
			n++;
		}

		mGame.EnableRedoCommand(mRedoCount > 0);
		mGame.EnableUndoCommand(mUndoCount > 0);
	}

	//--------------------------------------------------------------------------
	int[] getTrash() {
		if (mUndoCount == 0)
			return null;

		int[] result = new int[mUndoCount];
		int n=0;

		for (int i=0; i < mUndoCount; i++) {
			result[n] = mMemory[i].die1; n++;
		}

		Arrays.sort(result);
		return result;
	}

	//--------------------------------------------------------------------------
	boolean isFull() {
		return mUndoCount == mMemory.length;
	}
}

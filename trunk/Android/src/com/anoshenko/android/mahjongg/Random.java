package com.anoshenko.android.mahjongg;

public class Random {

	private long rand;

	//--------------------------------------------------------------------------
	public Random() {
		rand = System.currentTimeMillis() & 0x7FFFFFFFL;	
	}
	
	//--------------------------------------------------------------------------
	int nextInt(int n) {
		rand = rand * 0x48C27395L & 0x7FFFFFFFL;
		long result = (rand * (long)n / 0x80000000L);
		return (int)result;
	}
}

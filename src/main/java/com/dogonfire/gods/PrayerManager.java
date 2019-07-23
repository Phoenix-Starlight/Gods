package com.dogonfire.gods;

import java.util.Random;

public class PrayerManager
{
	private Gods plugin = null;
	private Random random = new Random();

	PrayerManager(Gods p)
	{
		this.plugin = p;
	}

	public boolean isPrayer()
	{
		return false;
	}
}

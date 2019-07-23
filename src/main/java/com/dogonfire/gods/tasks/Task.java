package com.dogonfire.gods.tasks;

import com.dogonfire.gods.Gods;

public abstract class Task implements Runnable {

	protected Gods getPlugin() {
		return Gods.get();
	}

	@Override
	public abstract void run();

}
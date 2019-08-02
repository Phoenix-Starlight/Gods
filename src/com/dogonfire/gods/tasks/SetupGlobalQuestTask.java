package com.dogonfire.gods.tasks;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.managers.QuestManager;


public class SetupGlobalQuestTask implements Runnable
{
	private Gods	plugin;

	public SetupGlobalQuestTask(Gods instance)
	{
		this.plugin = instance;
	}

	public void run()
	{
		QuestManager.instance().GodSayNewGlobalQuest();
	}
}

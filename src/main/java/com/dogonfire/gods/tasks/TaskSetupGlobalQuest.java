package com.dogonfire.gods.tasks;

import com.dogonfire.gods.managers.QuestManager;

public class TaskSetupGlobalQuest extends Task {
	@Override
	public void run() {
		QuestManager.instance().GodSayNewGlobalQuest();
	}
}

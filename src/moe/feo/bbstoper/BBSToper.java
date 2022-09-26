package moe.feo.bbstoper;

import moe.feo.bbstoper.gui.GUI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import moe.feo.bbstoper.gui.GUIManager;
import moe.feo.bbstoper.sql.SQLManager;

public class BBSToper extends JavaPlugin {
	private static BBSToper bbstoper;

	public static BBSToper getInstance() {
		return bbstoper;
	}

	@Override
	public void onEnable() {
		bbstoper = this;
		this.saveDefaultConfig();
		Option.load();
		Message.saveDefaultConfig();
		Message.load();
		SQLManager.initializeSQLer();
		this.getCommand("bbstoper").setExecutor(CLI.getInstance());
		this.getCommand("bbstoper").setTabCompleter(CLI.getInstance());
		new Reminder(this);
		new GUIManager(this);
		SQLManager.startTimingReconnect();
		Util.startAutoReward();
		if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
			new PAPIExpansion().register();
		}
	}

	@Override
	public void onDisable() {
		Bukkit.getOnlinePlayers().forEach(player -> {
			if (player.getOpenInventory().getTopInventory().getHolder() instanceof GUI.BBSToperGUIHolder) {
				player.closeInventory();
			}
		});

		Bukkit.getScheduler().cancelTasks(bbstoper);
		Thread thread = new Thread(new Runnable(){
			@Override
			public void run() {
				Util.waitForAllTask();// 此方法会阻塞
				SQLManager.closeSQLer();
				bbstoper = null;
			}
		});
		thread.setDaemon(true);
		thread.start();
	}

}

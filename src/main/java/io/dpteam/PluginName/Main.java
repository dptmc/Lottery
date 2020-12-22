package io.dpteam.PluginName;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin{
	public void onEnable() {
		System.out.println("PluginName Enabled");
	}

	@Override
	public void onDisable() {
		System.out.println("PluginName Disabled");
	}
}
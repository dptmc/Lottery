package io.dpteam.Lottery;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Runnable {
	Main plugin = this;
	File configFile;
	FileConfiguration config;
	DecimalFormat format = new DecimalFormat("$###,###");
	int delay = 600;
	int duration = 600;
	long timestamp;
	boolean lotteryRun = false;
	Random rand = new Random();
	List tickets = new ArrayList();
	public static Economy economy = null;
	String header;

	public Main() {
		super();
		this.header = "" + ChatColor.DARK_GRAY + ChatColor.BOLD + "[" + ChatColor.RED + "Lottery" + ChatColor.DARK_GRAY + ChatColor.BOLD + "] " + ChatColor.GRAY;
	}

	public void run() {
		this.lotteryRun = !this.lotteryRun;
		if (this.lotteryRun) {
			Bukkit.broadcastMessage(this.format(this.config.getString("config.lotteryBegin")));
			Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Main.Delay(), (long)(20 * this.duration - 1200));
			this.timestamp = System.currentTimeMillis() / 1000L + (long)this.duration;
		} else {
			if (this.tickets.size() != 0) {
				String winner = (String)this.tickets.get(this.rand.nextInt(this.tickets.size()));
				Bukkit.broadcastMessage(this.format(this.config.getString("config.winBroadcast").replace("{PLAYER}", winner).replace("{MONEY}", this.format.format((long)(this.tickets.size() * this.config.getInt("config.ticketPrice"))))));
				economy.depositPlayer(winner, (double)(this.tickets.size() * this.config.getInt("config.ticketPrice")));
				this.tickets.clear();
			}

			Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, this, (long)(20 * this.delay));
			this.timestamp = System.currentTimeMillis() / 1000L + (long)this.delay;
		}

	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!cmd.getName().equalsIgnoreCase("lottery")) {
			return false;
		} else {
			if (!(sender instanceof Player)) {
				sender.sendMessage("This command can only be run by a player.");
			} else {
				Player player = (Player)sender;
				if (player.hasPermission("lottery.buy")) {
					if (args.length == 1) {
						boolean var6 = false;

						int ticketsInput;
						try {
							ticketsInput = Integer.parseInt(args[0]);
						} catch (Exception var10) {
							if (!args[0].equalsIgnoreCase("pot") && !args[0].equalsIgnoreCase("bal") && !args[0].equalsIgnoreCase("money")) {
								player.sendMessage(this.header + "Please enter an integer!");
							} else {
								player.sendMessage(this.header + "Lottery total: " + this.format.format((long)(this.tickets.size() * this.config.getInt("config.ticketPrice"))));
							}

							return true;
						}

						if (ticketsInput > 0) {
							if (this.lotteryRun) {
								if (this.getCount(player) < this.config.getInt("config.maxTickets")) {
									int ticketsToBuy = ticketsInput;
									if (ticketsInput + this.getCount(player) > this.config.getInt("config.maxTickets")) {
										ticketsToBuy = this.config.getInt("config.maxTickets") - this.getCount(player);
									}

									if (ticketsToBuy > 0) {
										if (economy.getBalance(player.getName()) >= (double)(ticketsToBuy * this.config.getInt("config.ticketPrice"))) {
											economy.withdrawPlayer(player.getName(), (double)(ticketsToBuy * this.config.getInt("config.ticketPrice")));

											for(int i = 0; i < ticketsToBuy; ++i) {
												this.tickets.add(player.getName());
											}

											if (ticketsToBuy > 2) {
												Bukkit.broadcastMessage(this.format(this.config.getString("config.ticketBuy").replace("{PLAYER}", player.getName()).replace("{MONEY}", this.format.format((long)(ticketsToBuy * this.config.getInt("config.ticketPrice")))).replace("{TICKETS}", "" + ticketsToBuy)));
											} else {
												player.sendMessage(this.header + "You have bought: " + ticketsToBuy + " tickets.");
											}
										} else {
											player.sendMessage(this.header + "You do not have enough money!");
										}
									}
								} else {
									player.sendMessage(this.header + "You may only buy " + this.config.getInt("config.maxTickets") + ", per round!");
								}
							} else {
								player.sendMessage(this.header + "There is no lottery going at this time!");
							}
						} else {
							player.sendMessage(this.header + "You must buy a positive number of tickets!");
						}
					} else {
						this.help(player);
					}
				} else {
					player.sendMessage(this.header + "You do not have permission!");
				}
			}

			return true;
		}
	}

	private void help(Player player) {
		player.sendMessage(this.header + "Lottery total: " + this.format.format((long)(this.tickets.size() * this.config.getInt("config.ticketPrice"))));
		player.sendMessage(this.header + "You have bought: " + this.getCount(player) + ", tickets!");
		if (this.lotteryRun) {
			player.sendMessage(this.header + "Lottery will end in: " + this.getTime(this.timestamp));
		} else {
			player.sendMessage(this.header + "Lottery will start in: " + this.getTime(this.timestamp));
		}

		player.sendMessage(this.header + "/lottery <TicketsToBuy>");
		player.sendMessage(this.header + "/lottery pot | Check current lottery balance!");
	}

	private int getCount(Player player) {
		int count = 0;
		Iterator var4 = this.tickets.iterator();

		while(var4.hasNext()) {
			String name = (String)var4.next();
			if (player.getName().equals(name)) {
				++count;
			}
		}

		return count;
	}

	private String getTime(long time) {
		long currentTime = time - System.currentTimeMillis() / 1000L;
		String msg = "";
		if ((int)(currentTime / 3600L) > 0) {
			msg = msg + currentTime / 3600L + " hours, ";
			currentTime -= currentTime / 3600L * 60L * 60L;
		}

		if (currentTime / 60L > 0L) {
			msg = msg + currentTime / 60L + " minutes, ";
			currentTime -= currentTime / 60L * 60L;
		}

		msg = msg + currentTime + " seconds ";
		return msg;
	}

	public void onEnable() {
		this.getServer().getLogger().info("[Lottery] Loading, please wait...");
		this.configFile = new File(this.getDataFolder(), "config.yml");

		try {
			this.firstRun();
		} catch (Exception var2) {
			var2.printStackTrace();
		}

		this.config = new YamlConfiguration();
		this.loadYamls();
		this.init();
		if (!this.setupEconomy()) {
			Bukkit.getConsoleSender().sendMessage(this.header + "Lottery plugin disabled. Vault dependency not found!");
			this.getServer().getPluginManager().disablePlugin(this);
		} else {
			Bukkit.getScheduler().scheduleSyncDelayedTask(this, this, (long)(20 * this.delay));
			this.timestamp = System.currentTimeMillis() / 1000L + (long)this.delay;
			Bukkit.getConsoleSender().sendMessage(this.header + "Plugin loaded and enabled");
		}

	}

	private boolean setupEconomy() {
		if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		} else {
			RegisteredServiceProvider rsp = this.getServer().getServicesManager().getRegistration(Economy.class);
			if (rsp == null) {
				return false;
			} else {
				economy = (Economy)rsp.getProvider();
				return economy != null;
			}
		}
	}

	private void init() {
		this.delay = this.config.getInt("config.lotteryDelay");
		this.duration = this.config.getInt("config.lotteryDuration");
	}

	private void firstRun() throws Exception {
		if (!this.configFile.exists()) {
			this.configFile.getParentFile().mkdirs();
			this.copy(this.getResource("config.yml"), this.configFile);
		}
	}

	private void copy(InputStream in, File file) {
		try {
			OutputStream out = new FileOutputStream(file);
			byte[] buf = new byte[1024];

			int len;
			while((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}

			out.close();
			in.close();
		} catch (Exception var6) {
			var6.printStackTrace();
		}
	}

	private void loadYamls() {
		try {
			this.config.load(this.configFile);
		} catch (Exception var2) {
			var2.printStackTrace();
		}

	}

	@Override
	public void onDisable() {
		this.getServer().getLogger().info("[Lottery] Plugin unloaded and disabled");
	}
	
	private String format(String message) {
		return message.replaceAll("(?i)&([a-z0-9])", "ยง$1");
	}

	public class Delay implements Runnable {
		public Delay() {
			super();
		}

		public void run() {
			Bukkit.broadcastMessage(Main.this.format(Main.this.config.getString("config.timeBroadcast")));
			Bukkit.getScheduler().scheduleSyncDelayedTask(Main.this.plugin, Main.this.plugin, 1200L);
		}
	}
}

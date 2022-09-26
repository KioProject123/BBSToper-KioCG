package moe.feo.bbstoper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import moe.feo.bbstoper.gui.GUI;
import moe.feo.bbstoper.gui.IDListener;
import moe.feo.bbstoper.sql.SQLManager;
import moe.feo.bbstoper.sql.SQLer;

public class CLI implements TabExecutor {

	private static SQLer sql;
	private Map<String, String> cache = new HashMap<>();// 这个map是为了暂存玩家的绑定信息的
	private Map<UUID, Long> queryrecord = new HashMap<>();// 这个map是用于储存玩家上次查询顶贴记录的时间

	private static CLI cli = new CLI();

	private CLI() {

	}

	public static CLI getInstance() {
		return cli;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length == 1) {
			List<String> list = new ArrayList<String>();
			String arg = args[0].toLowerCase();
			if ("help".startsWith(arg)) list.add("help");
			if ("reward".startsWith(arg) && sender.hasPermission("bbstoper.reward")) {
				list.add("reward");
			}
			if ("testreward".startsWith(arg) && sender.hasPermission("bbstoper.testreward")) {
				list.add("testreward");
			}
			if ("binding".startsWith(arg) && sender.hasPermission("bbstoper.binding")) {
				list.add("binding");
			}
			if ("list".startsWith(arg) && sender.hasPermission("bbstoper.list")) {
				list.add("list");
			}
			if ("top".startsWith(arg) && sender.hasPermission("bbstoper.top")) {
				list.add("top");
			}
			if ("check".startsWith(arg) && sender.hasPermission("bbstoper.check")) {
				list.add("check");
			}
			if ("delete".startsWith(arg) && sender.hasPermission("bbstoper.delete")) {
				list.add("delete");
			}
			if ("reload".startsWith(arg) && sender.hasPermission("bbstoper.reload")) {
				list.add("reload");
			}
			return list;
		}
		if (args.length == 2) {
			if (args[0].equalsIgnoreCase("check")) {
				List<String> list = new ArrayList<String>();
				if (sender.hasPermission("bbstoper.check")) {
					list.add("bbsid");
					list.add("player");
				}
				return list;
			}
		}
		return null;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		new BukkitRunnable() {

			@Override
			public void run() {
				Util.addRunningTaskID(this.getTaskId());
				task();
				Util.removeRunningTaskID(this.getTaskId());
			}
			
			public void task() {
				if (args.length == 0) {// 没有带参数
					if (sender instanceof Player) {
						Player player = (Player) sender;
						new GUI(player);
					} else {
						String[] args = { "help" };
						onCommand(sender, cmd, label, args);
					}
					return;
				}
				Crawler crawler;// 爬虫
				switch (args[0].toLowerCase()) {
				case "help": {
					sender.sendMessage(Message.PREFIX.getString() + Message.HELP_TITLE.getString());
					if (sender.hasPermission("bbstoper.testreward")) {
						sender.sendMessage(Message.PREFIX.getString() + Message.HELP_TESTREWARD.getString());
					}
					if (sender.hasPermission("bbstoper.binding")) {
						sender.sendMessage(Message.PREFIX.getString() + Message.HELP_BINDING.getString());
					}
					if (sender.hasPermission("bbstoper.list")) {
						sender.sendMessage(Message.PREFIX.getString() + Message.HELP_LIST.getString());
					}
					if (sender.hasPermission("bbstoper.top")) {
						sender.sendMessage(Message.PREFIX.getString() + Message.HELP_TOP.getString());
					}
					if (sender.hasPermission("bbstoper.check")) {
						sender.sendMessage(Message.PREFIX.getString() + Message.HELP_CHECK.getString());
					}
					if (sender.hasPermission("bbstoper.delete")) {
						sender.sendMessage(Message.PREFIX.getString() + Message.HELP_DELETE.getString());
					}
					if (sender.hasPermission("bbstoper.reload")) {
						sender.sendMessage(Message.PREFIX.getString() + Message.HELP_RELOAD.getString());
					}
					return;
				}
				case "binding": {
					if (!(sender instanceof Player)) {
						sender.sendMessage(Message.PLAYERCMD.getString());
						sender.sendMessage(Message.HELP_HELP.getString());
						return;
					}
					if (!(sender.hasPermission("bbstoper.binding"))) {
						sender.sendMessage(Message.PREFIX.getString() + Message.NOPERMISSION.getString());
						IDListener.unregister(sender);
						return;
					}
					if (args.length == 2) {
						Player player = Bukkit.getPlayer(sender.getName());
						String uuid = player.getUniqueId().toString();
						Poster poster = sql.getPoster(uuid);
						boolean isrecording = true;
						if (poster != null) {
							sender.sendMessage(Message.PREFIX.getString() + Message.ALREADYBIND.getString());
							IDListener.unregister(sender);
							return;
						} else {
							poster = new Poster();
							isrecording = false;
						}
						String ownersuuid = sql.bbsNameCheck(args[1]);
						if (ownersuuid == null) {// 没有人绑定过这个论坛id
							if (cache.get(uuid) != null && cache.get(uuid).equals(args[1])) {
								poster.setUuid(uuid);
								poster.setName(sender.getName());
								poster.setBbsname(args[1]);
								poster.setBinddate(System.currentTimeMillis());
								if (isrecording) {
									sql.updatePoster(poster);
								} else {
									sql.addPoster(poster);
								}
								cache.put(uuid, null);// 绑定成功, 清理这个键
								sender.sendMessage(Message.PREFIX.getString() + Message.BINDINGSUCCESS.getString());
								IDListener.unregister(sender);
							} else if (cache.get(uuid) == null) {
								cache.put(uuid, args[1]);
								sender.sendMessage(Message.PREFIX.getString() + Message.REPEAT.getString());
							} else {
								sender.sendMessage(Message.PREFIX.getString() + Message.NOTSAME.getString());
								cache.put(uuid, null);
								IDListener.unregister(sender);
							}
							return;
						} else if (ownersuuid.equals(uuid)) {// 自己绑定了这个论坛id
							sender.sendMessage(Message.PREFIX.getString() + Message.OWNSAMEBIND.getString());
							IDListener.unregister(sender);
							return;
						} else {
							sender.sendMessage(Message.PREFIX.getString() + Message.SAMEBIND.getString());
							IDListener.unregister(sender);
							return;
						}
					} else {
						sender.sendMessage(Message.PREFIX.getString() + Message.INVALID.getString());
						sender.sendMessage(Message.PREFIX.getString() + Message.HELP_BINDING.getString());
						IDListener.unregister(sender);
						return;
					}
				}

				case "testreward": {
					if (!(sender instanceof Player)) {
						sender.sendMessage(Message.PLAYERCMD.getString());
						sender.sendMessage(Message.HELP_HELP.getString());
						return;
					}
					if (!sender.hasPermission("bbstoper.testreward")) {
						sender.sendMessage(Message.PREFIX.getString() + Message.NOPERMISSION.getString());
						return;
					}
					String type;
					if (args.length == 1) {
						type = "NORMAL";
					} else if (args.length == 2) {
						type = args[1].toUpperCase();
						if (!(type.equals("NORMAL") || type.equals("INCENTIVE") || type.equals("OFFDAY"))) {
							sender.sendMessage(Message.PREFIX.getString() + Message.INVALID.getString());
							sender.sendMessage(Message.PREFIX.getString() + Message.HELP_TESTREWARD.getString());
							return;
						}
					} else {
						sender.sendMessage(Message.PREFIX.getString() + Message.INVALID.getString());
						sender.sendMessage(Message.PREFIX.getString() + Message.HELP_TESTREWARD.getString());
						return;
					}
					Player player = Bukkit.getPlayer(sender.getName());
					new Reward(player, null, 0).testAward(type);
					sender.sendMessage(Message.PREFIX.getString() + Message.REWARDGIVED.getString());
					break;
				}

				case "list": {
					if (!sender.hasPermission("bbstoper.list")) {
						sender.sendMessage(Message.PREFIX.getString() + Message.NOPERMISSION.getString());
						return;
					}
					if (sender instanceof Player && !sender.hasPermission("bbstoper.bypassquerycooldown")) {
						double cooldown = getQueryCooldown(((Player) sender).getUniqueId());
						if (cooldown > 0) {
							sender.sendMessage(Message.PREFIX.getString() + Message.QUERYCOOLDOWN.getString()
									.replaceAll("%COOLDOWN%", String.valueOf((int) cooldown)));
							return;
						} else {
							queryrecord.put(((Player) sender).getUniqueId(), System.currentTimeMillis());
						}
					}
					int page = 1;
					if (args.length == 2) {
						for (char c : args[1].toCharArray()) {// 判断参数是否为数字
							if (!Character.isDigit(c)) {
								sender.sendMessage(Message.PREFIX.getString() + Message.INVALID.getString());
								sender.sendMessage(Message.PREFIX.getString() + Message.HELP_TOP.getString());
								return;
							}
						}
						try {
							page = Integer.parseInt(args[1]);
						} catch (NumberFormatException e) {
							sender.sendMessage(Message.PREFIX.getString() + Message.INVALIDNUM.getString());
							return;
						}

					} else if (args.length > 2) {
						sender.sendMessage(Message.PREFIX.getString() + Message.INVALID.getString());
						sender.sendMessage(Message.PREFIX.getString() + Message.HELP_LIST.getString());
						return;
					}
					crawler = new Crawler();
					if (!crawler.visible) {
						if (sender instanceof Player) {
							sender.sendMessage(Message.PREFIX.getString() + Message.PAGENOTVISIBLE.getString());
							return;
						} else {
							return;
						}
					}
					int totalpage = (int) Math.ceil((double) crawler.ID.size() / Option.MCBBS_PAGESIZE.getInt());
					if (page > totalpage) {
						sender.sendMessage(Message.PREFIX.getString() + Message.OVERPAGE.getString());
						return;
					}
					List<String> msglist = new ArrayList<String>();
					msglist.add(Message.PREFIX.getString() + Message.POSTERNUM.getString() + crawler.ID.size());
					for (int i = (page - 1) * Option.MCBBS_PAGESIZE.getInt(); i < page
							* Option.MCBBS_PAGESIZE.getInt(); i++) {
						if (i >= crawler.ID.size())
							break;// 当i不再小于顶贴人数，该停了
						msglist.add(Message.POSTERID.getString() + crawler.ID.get(i).replace("\u202E", "") + " "
								+ Message.POSTERTIME.getString() + crawler.Time.get(i));
					}
					if (msglist.size() == 1)
						msglist.add(Message.NOPOSTER.getString());
					String pageinfo = Message.PAGEINFO.getString();
					pageinfo = pageinfo.replaceAll("%PAGE%", Integer.toString(page));
					pageinfo = pageinfo.replaceAll("%TOTALPAGE%", Integer.toString(totalpage));
					msglist.add(Message.PREFIX.getString() + pageinfo);
					for (int i = 0; i < msglist.size(); i++) {
						sender.sendMessage(msglist.get(i));
					}
					break;
				}
				case "top": {
					if (!sender.hasPermission("bbstoper.top")) {
						sender.sendMessage(Message.PREFIX.getString() + Message.NOPERMISSION.getString());
						return;
					}
					if (sender instanceof Player && !sender.hasPermission("bbstoper.bypassquerycooldown")) {
						double cooldown = getQueryCooldown(((Player) sender).getUniqueId());
						if (cooldown > 0) {
							sender.sendMessage(Message.PREFIX.getString() + Message.QUERYCOOLDOWN.getString()
									.replaceAll("%COOLDOWN%", String.valueOf((int) cooldown)));
							return;
						} else {
							queryrecord.put(((Player) sender).getUniqueId(), System.currentTimeMillis());
						}
					}
					int page = 1;
					if (args.length == 2) {
						for (char c : args[1].toCharArray()) {// 判断参数是否为数字
							if (!Character.isDigit(c)) {
								sender.sendMessage(Message.PREFIX.getString() + Message.INVALID.getString());
								sender.sendMessage(Message.PREFIX.getString() + Message.HELP_TOP.getString());
								return;
							}
						}
						try {
							page = Integer.parseInt(args[1]);
						} catch (NumberFormatException e) {
							sender.sendMessage(Message.PREFIX.getString() + Message.INVALIDNUM.getString());
							return;
						}
					} else if (args.length > 2) {
						sender.sendMessage(Message.PREFIX.getString() + Message.INVALID.getString());
						sender.sendMessage(Message.PREFIX.getString() + Message.HELP_TOP.getString());
						return;
					}
					List<Poster> posterlist = sql.getTopPosters();
					posterlist.addAll(sql.getNoCountPosters());
					int totalpage = (int) Math.ceil((double) posterlist.size() / Option.MCBBS_PAGESIZE.getInt());
					if (page > totalpage) {
						sender.sendMessage(Message.PREFIX.getString() + Message.OVERPAGE.getString());
						return;
					}
					List<String> msglist = new ArrayList<String>();
					msglist.add(Message.PREFIX.getString() + Message.POSTERTOTAL.getString() + posterlist.size());
					for (int i = (page - 1) * Option.MCBBS_PAGESIZE.getInt(); i < page
							* Option.MCBBS_PAGESIZE.getInt(); i++) {
						if (i >= posterlist.size())
							break;// 当i不再小于顶贴人数，该停了
						Poster poster = posterlist.get(i);
						msglist.add(Message.POSTERPLAYER.getString() + poster.getName() + " "
								+ Message.POSTERID.getString() + poster.getBbsname().replace("\u202E", "") + " "
								+ Message.POSTERNUM.getString() + poster.getCount());
					}
					if (msglist.size() == 1)
						msglist.add(Message.NOPLAYER.getString());
					String pageinfo = Message.PAGEINFOTOP.getString();
					pageinfo = pageinfo.replaceAll("%PAGE%", Integer.toString(page));
					pageinfo = pageinfo.replaceAll("%TOTALPAGE%", Integer.toString(totalpage));
					msglist.add(Message.PREFIX.getString() + pageinfo);
					for (int i = 0; i < msglist.size(); i++) {
						sender.sendMessage(msglist.get(i));
					}
					break;
				}
				case "reload": {
					if (!(sender.hasPermission("bbstoper.reload"))) {
						sender.sendMessage(Message.PREFIX.getString() + Message.NOPERMISSION.getString());
						return;
					}
					BBSToper.getInstance().saveDefaultConfig();
					Option.load();
					Message.saveDefaultConfig();
					Message.load();
					SQLManager.initializeSQLer();
					SQLManager.startTimingReconnect();
					Util.startAutoReward();
					sender.sendMessage(Message.PREFIX.getString() + Message.RELOAD.getString());
					break;
				}
				case "check": {
					if (!(sender.hasPermission("bbstoper.check"))) {
						sender.sendMessage(Message.PREFIX.getString() + Message.NOPERMISSION.getString());
						return;
					}
					if (args.length != 3) {
						sender.sendMessage(Message.PREFIX.getString() + Message.INVALID.getString());
						sender.sendMessage(Message.PREFIX.getString() + Message.HELP_CHECK.getString());
						return;
					}
					switch (args[1].toLowerCase()) {
					case "bbsid": {
						String owneruuid = sql.bbsNameCheck(args[2]);
						if (owneruuid == null) {
							sender.sendMessage(Message.PREFIX.getString() + Message.IDNOTFOUND.getString());
							return;
						}
						OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(owneruuid));
						String ownername = owner.getName();
						sender.sendMessage(Message.PREFIX.getString() + Message.IDOWNER.getString()
								.replaceAll("%PLAYER%", ownername).replaceAll("%UUID%", owneruuid));
						return;
					}
					case "player": {
						@SuppressWarnings("deprecation")
						UUID owneruuid = Bukkit.getOfflinePlayer(args[2]).getUniqueId();
						Poster poster = sql.getPoster(owneruuid.toString());
						if (poster == null) {
							sender.sendMessage(Message.PREFIX.getString() + Message.OWNERNOTFOUND.getString());
							return;
						}
						String mcbbsname = poster.getBbsname();
						sender.sendMessage(
								Message.PREFIX.getString() + Message.OWNERID.getString().replaceAll("%ID%", mcbbsname));
						return;
					}
					}
				}
				case "delete": {
					if (!(sender.hasPermission("bbstoper.delete"))) {
						sender.sendMessage(Message.PREFIX.getString() + Message.NOPERMISSION.getString());
						return;
					}
					if (args.length != 2) {
						sender.sendMessage(Message.PREFIX.getString() + Message.INVALID.getString());
						sender.sendMessage(Message.PREFIX.getString() + Message.HELP_DELETE.getString());
						return;
					}
					@SuppressWarnings("deprecation")
					UUID uuid = Bukkit.getOfflinePlayer(args[1]).getUniqueId();
					Poster poster = sql.getPoster(uuid.toString());
					if (poster == null) {
						sender.sendMessage(Message.PREFIX.getString() + Message.OWNERNOTFOUND.getString());
						return;
					}
					sql.deletePoster(uuid.toString());
					sender.sendMessage(Message.PREFIX.getString() + Message.DELETESUCCESS.getString());
					return;
				}
				default: {
					sender.sendMessage(Message.PREFIX.getString() + Message.INVALID.getString());
					sender.sendMessage(Message.PREFIX.getString() + Message.HELP_HELP.getString());
					return;
				}
				}
			}
		}.runTaskAsynchronously(BBSToper.getInstance());
		return true;
	}

	public static void setSQLer(SQLer sql) {
		CLI.sql = sql;
	}

	public Map<String, String> getCache() {
		return this.cache;
	}

	public void recordQuery(UUID uuid, Long time) {
		queryrecord.put(uuid, time);
	}

	public double getQueryCooldown(UUID uuid) {
		int cooldown = Option.MCBBS_QUERYCOOLDOWN.getInt() * 1000;
		long now = System.currentTimeMillis();
		Long before = queryrecord.get(uuid);
		if (before == null) {
			before = 0l;
		}
		return (cooldown - (now - before)) / (double) 1000;
	}
}

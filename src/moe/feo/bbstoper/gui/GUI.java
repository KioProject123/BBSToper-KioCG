package moe.feo.bbstoper.gui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import moe.feo.bbstoper.BBSToper;
import moe.feo.bbstoper.Crawler;
import moe.feo.bbstoper.Message;
import moe.feo.bbstoper.Option;
import moe.feo.bbstoper.Poster;
import moe.feo.bbstoper.Util;
import moe.feo.bbstoper.sql.SQLer;

public class GUI {

	private static SQLer sql;
	private Inventory inv;

	// 添加返回主菜单按钮
	private static final ItemStack menuIcon = new ItemStack(Material.WRITABLE_BOOK);
	// 添加空间分割线物品
	private static final ItemStack fillIcon = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);

	static {
		menuIcon.editMeta(itemMeta -> itemMeta.setDisplayName("§e↩ §c返回主菜单"));
		fillIcon.editMeta(itemMeta -> itemMeta.setDisplayName(Message.GUI_FRAME.getString()));
	}

	public static String getTitle() {// 获取插件的gui标题必须用此方法，因为用户可能会修改gui标题
		String title = Message.GUI_TITLE.getString().replace("%PREFIX%", Message.PREFIX.getString());
		return title;
	}

	public GUI(Player player) {
		createGui(player);
		Bukkit.getScheduler().runTask(BBSToper.getInstance(), () -> player.openInventory(inv));
	}
	
	public class BBSToperGUIHolder implements InventoryHolder {// 定义一个Holder用于识别此插件的GUI
		@Override
		public Inventory getInventory() {
			return getGui();
		}
	}

	@SuppressWarnings("deprecation")
	public void createGui(Player player) {
		InventoryHolder holder = new BBSToperGUIHolder();
		this.setGui(Bukkit.createInventory(holder, 6 * 9, getTitle()));

		// 添加返回主菜单按钮
		inv.setItem(inv.getSize() - 5, menuIcon);

		ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1, (short) SkullType.PLAYER.ordinal());
		SkullMeta skullmeta = (SkullMeta) skull.getItemMeta();// 玩家头颅
		if (Option.GUI_DISPLAYHEADSKIN.getBoolean()) {// 如果开启了头颅显示，才会设置头颅的所有者
			try {
				skullmeta.setOwningPlayer(player);
			} catch (NoSuchMethodError e) {// 这里为了照顾低版本
				skullmeta.setOwner(player.getName());
			}
		}
		skullmeta.setDisplayName(Message.GUI_SKULL.getString().replace("%PLAYER%", player.getName()));
		List<String> skulllores = new ArrayList<String>();
		Poster poster = sql.getPoster(player.getUniqueId().toString());
		if (poster == null) {
			skulllores.add(Message.GUI_NOTBOUND.getString());
			skulllores.add(Message.GUI_CLICKBOUND.getString());
		} else {
			skulllores.add(Message.GUI_BBSID.getString().replace("%BBSID%", poster.getBbsname()));
			skulllores.add(Message.GUI_POSTTIMES.getString().replace("%TIMES%", "" + poster.getTopStates().size()));
		}
		skullmeta.setLore(skulllores);
		skull.setItemMeta(skullmeta);
		inv.setItem(20, skull);
		ItemStack sunflower = new ItemStack(Material.SUNFLOWER);
		ItemMeta sunflowermeta = sunflower.getItemMeta();
		sunflowermeta.setDisplayName(Message.GUI_REWARDS.getString());
		List<String> sunflowerlores = new ArrayList<String>(Message.GUI_REWARDSINFO.getStringList());// 自定义奖励信息
		if (sunflowerlores.isEmpty()) {// 如果没有自定义奖励信息
			sunflowerlores.addAll(Option.REWARD_COMMANDS.getStringList());// 直接显示命令
			if (Option.REWARD_INCENTIVEREWARD_ENABLE.getBoolean()) {
				sunflowerlores.addAll(Option.REWARD_INCENTIVEREWARD_COMMANDS.getStringList());// 激励奖励命令
			}
			if (Option.REWARD_OFFDAYREWARD_ENABLE.getBoolean()) {
				sunflowerlores.addAll(Option.REWARD_OFFDAYREWARD_COMMANDS.getStringList()); // 休息日奖励命令
			}
		}
		sunflowermeta.setLore(sunflowerlores);
		sunflower.setItemMeta(sunflowermeta);
		inv.setItem(23, sunflower);
		ItemStack star = new ItemStack(Material.NETHER_STAR);
		ItemMeta starmeta = star.getItemMeta();
		starmeta.setDisplayName(Message.GUI_TOPS.getString());
		List<String> starlores = new ArrayList<String>();
		List<Poster> listposter = sql.getTopPosters();
		for (int i = 0; i < listposter.size(); i++) {
			if (i >= Option.GUI_TOPPLAYERS.getInt())
				break;
			starlores.add(Message.POSTERPLAYER.getString() + listposter.get(i).getName() + " "
					+ Message.POSTERID.getString() + listposter.get(i).getBbsname().replace("\u202E", "") + " "
					+ Message.POSTERNUM.getString() + listposter.get(i).getCount());
		}
		starmeta.setLore(starlores);
		star.setItemMeta(starmeta);
		inv.setItem(24, star);
		ItemStack compass = new ItemStack(Material.COMPASS);
		ItemMeta compassmeta = compass.getItemMeta();
		compassmeta.setDisplayName(Message.GUI_PAGESTATE.getString());
		List<String> compasslores = new ArrayList<String>();
		compasslores.add(Message.GUI_PAGEID.getString().replace("%PAGEID%", Option.MCBBS_URL.getString()));
		Crawler crawler = new Crawler();
		if (crawler.visible) {// 如果帖子可视，就获取帖子最近一次顶贴
			if (crawler.Time.size() > 0) { // 如果从没有人顶帖，就以“----”代替上次顶帖时间(原来不加判断直接get会报索引范围错误)
				compasslores.add(Message.GUI_LASTPOST.getString().replace("%TIME%", crawler.Time.get(0)));
			} else {
				compasslores.add(Message.GUI_LASTPOST.getString().replace("%TIME%", "----"));
			}
		} else {
			compasslores.add(Message.GUI_PAGENOTVISIBLE.getString());
		}
		compasslores.add(Message.GUI_CLICKOPEN.getString());
		compassmeta.setLore(compasslores);
		compass.setItemMeta(compassmeta);
		inv.setItem(21, compass);

		int[] slots = {0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,49,50,51,52,53};
		for (int i: slots) {// 设置边框
			if (inv.getItem(i) == null) {
				inv.setItem(i, fillIcon);
			}
		}
	}
	
	public Inventory getGui() {
		return inv;
	}

	public void setGui(Inventory inv) {
		this.inv = inv;
	}
	
	public static void setSQLer(SQLer sql) {
		GUI.sql = sql;
	}
}

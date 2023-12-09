package fr.skytasul.quests.api.editors.parsers;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class WorldParser implements AbstractParser<World> {

	public World parse(Player p, String msg) throws Throwable {
		World world = Bukkit.getWorld(msg);
		if (world == null) p.sendMessage("§cThis world does not exist.");
		return world;
	}

}

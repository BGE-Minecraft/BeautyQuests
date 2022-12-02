package fr.skytasul.quests.utils.compatibility.maps;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerIcon;
import org.dynmap.markers.MarkerSet;
import fr.skytasul.quests.BeautyQuests;
import fr.skytasul.quests.QuestsConfiguration;
import fr.skytasul.quests.structure.Quest;
import fr.skytasul.quests.utils.DebugUtils;
import net.md_5.bungee.api.ChatColor;

public class BQDynmap extends AbstractMapIntegration {

	private MarkerIcon icon;
	private MarkerSet markers;
	
	@Override
	public boolean isEnabled() {
		return QuestsConfiguration.dynmapMarkerIcon() != null && !QuestsConfiguration.dynmapMarkerIcon().isEmpty();
	}

	@Override
	protected void initializeMarkers(Runnable initializeQuests) {
		DynmapAPI dynmap = (DynmapAPI) Bukkit.getPluginManager().getPlugin("dynmap");
		MarkerAPI api = dynmap.getMarkerAPI();
		icon = api.getMarkerIcon(QuestsConfiguration.dynmapMarkerIcon());
		
		markers = api.getMarkerSet("beautyquests.markerset");
		if (markers == null){
			markers = api.createMarkerSet("beautyquests.markerset", QuestsConfiguration.dynmapSetName(), null, false);
		}else markers.setMarkerSetLabel(QuestsConfiguration.dynmapSetName());
		
		markers.setMinZoom(QuestsConfiguration.dynmapMinimumZoom());
		markers.setHideByDefault(false);
		markers.setDefaultMarkerIcon(icon);
		
		initializeQuests.run();
	}
	
	@Override
	public void unload() {
		if (markers != null){
			markers.deleteMarkerSet();
			markers = null;
		}
	}
	
	@Override
	public void addMarker(Quest quest, Location lc) {
		if (markers == null) return;
		
		Marker marker = markers.createMarker("qu_" + quest.getID(), ChatColor.stripColor(quest.getName()), lc.getWorld().getName(), lc.getX(), lc.getBlockY(), lc.getBlockZ(), icon, false);
		
		if (marker == null) {
			BeautyQuests.logger.severe("Problem when creating marker for quest " + quest.getID());
		}else DebugUtils.logMessage("Marker " + marker.getMarkerID() + " created");
	}
	
	@Override
	public void removeMarker(Quest quest) {
		if (markers == null) return;
		
		Marker marker = markers.findMarker("qu_" + quest.getID());
		if (marker == null) {
			BeautyQuests.logger.warning("Unable to find marker for quest " + quest.getID());
		}else marker.deleteMarker();
	}
	
}
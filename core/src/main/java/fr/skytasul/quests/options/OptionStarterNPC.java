package fr.skytasul.quests.options;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import com.cryptomorin.xseries.XMaterial;
import fr.skytasul.quests.api.QuestsAPI;
import fr.skytasul.quests.api.gui.ItemUtils;
import fr.skytasul.quests.api.localization.Lang;
import fr.skytasul.quests.api.npcs.BQNPC;
import fr.skytasul.quests.api.options.OptionSet;
import fr.skytasul.quests.api.options.QuestOption;
import fr.skytasul.quests.api.quests.creation.QuestCreationGuiClickEvent;
import fr.skytasul.quests.gui.npc.NpcSelectGUI;

public class OptionStarterNPC extends QuestOption<BQNPC> {
	
	public OptionStarterNPC() {
		super(OptionQuestPool.class);
	}
	
	@Override
	public Object save() {
		return getValue().getId();
	}
	
	@Override
	public void load(ConfigurationSection config, String key) {
		setValue(QuestsAPI.getAPI().getNPCsManager().getById(config.getInt(key)));
	}
	
	@Override
	public BQNPC cloneValue(BQNPC value) {
		return value;
	}
	
	private List<String> getLore(OptionSet options) {
		List<String> lore = new ArrayList<>(4);
		lore.add(formatDescription(Lang.questStarterSelectLore.toString()));
		lore.add(null);
		if (options != null && options.hasOption(OptionQuestPool.class) && options.getOption(OptionQuestPool.class).hasCustomValue()) lore.add(Lang.questStarterSelectPool.toString());
		lore.add(getValue() == null ? Lang.NotSet.toString() : "§7" + getValue().getName() + " §8(" + getValue().getId() + ")");
		return lore;
	}
	
	@Override
	public ItemStack getItemStack(OptionSet options) {
		return ItemUtils.item(XMaterial.VILLAGER_SPAWN_EGG, Lang.questStarterSelect.toString(), getLore(options));
	}

	@Override
	public void click(QuestCreationGuiClickEvent event) {
		NpcSelectGUI.selectNullable(event::reopen, npc -> {
			setValue(npc);
			ItemUtils.lore(event.getClicked(), getLore(event.getGui().getOptionSet()));
			event.reopen();
		}).open(event.getPlayer());
	}
	
	@Override
	public void onDependenciesUpdated(OptionSet options) {
		super.onDependenciesUpdated(options);
		// ItemUtils.lore(item, getLore(options));
		// TODO wtf ?
	}

}

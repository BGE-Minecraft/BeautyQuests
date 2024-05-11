package fr.skytasul.quests.integrations;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import fr.skytasul.quests.api.AbstractHolograms;
import fr.skytasul.quests.api.QuestsAPI;
import fr.skytasul.quests.api.QuestsPlugin;
import fr.skytasul.quests.api.gui.ItemUtils;
import fr.skytasul.quests.api.localization.Lang;
import fr.skytasul.quests.api.requirements.RequirementCreator;
import fr.skytasul.quests.api.rewards.RewardCreator;
import fr.skytasul.quests.api.utils.IntegrationManager;
import fr.skytasul.quests.api.utils.IntegrationManager.BQDependency;
import fr.skytasul.quests.api.utils.XMaterial;
import fr.skytasul.quests.integrations.mobs.*;
import fr.skytasul.quests.integrations.npcs.*;
import fr.skytasul.quests.integrations.placeholders.PapiMessageProcessor;
import fr.skytasul.quests.integrations.placeholders.PlaceholderRequirement;
import fr.skytasul.quests.integrations.placeholders.QuestsPlaceholders;
import fr.skytasul.quests.integrations.vault.economy.MoneyRequirement;
import fr.skytasul.quests.integrations.vault.economy.MoneyReward;
import fr.skytasul.quests.integrations.vault.permission.PermissionReward;
import fr.skytasul.quests.integrations.worldguard.BQWorldGuard;

public class IntegrationsLoader {

	private static IntegrationsLoader instance;

	public static IntegrationsLoader getInstance() {
		return instance;
	}

	private IntegrationsConfiguration config;

	public IntegrationsLoader() {
		instance = this;

		config = new IntegrationsConfiguration(QuestsPlugin.getPlugin().getConfig());
		config.load();

		IntegrationManager manager = QuestsPlugin.getPlugin().getIntegrationManager();

		// NPCS
		manager.addDependency(new BQDependency("ServersNPC",
				() -> QuestsAPI.getAPI().addNpcFactory("znpcs", new BQServerNPCs()), null, this::isZnpcsVersionValid));

		manager.addDependency(new BQDependency("ZNPCsPlus", this::registerZnpcsPlus));

		manager.addDependency(new BQDependency("Citizens", () -> {
			QuestsAPI.getAPI().addNpcFactory("citizens", new BQCitizens());
			QuestsAPI.getAPI().registerMobFactory(new CitizensFactory());
		}));


		// MOBS
		manager.addDependency(new BQDependency("MythicMobs", this::registerMythicMobs));

	


		// REWARDS / REQUIREMENTS
		manager.addDependency(new BQDependency("Vault", this::registerVault));


		// MAPS
		


		// HOLOGRAMS
				manager.addDependency(
				new BQDependency("DecentHolograms", () -> QuestsAPI.getAPI().setHologramsManager(new BQDecentHolograms())));


		// OTHERS
		manager.addDependency(new BQDependency("PlaceholderAPI", this::registerPapi));
		manager.addDependency(new BQDependency("WorldGuard", BQWorldGuard::initialize, BQWorldGuard::unload));
		manager.addDependency(new BQDependency("Sentinel", BQSentinel::initialize));
	
	}

	private void registerPapi() {
		QuestsPlaceholders.registerPlaceholders(
				QuestsPlugin.getPlugin().getConfig().getConfigurationSection("startedQuestsPlaceholder"));
		QuestsAPI.getAPI().getRequirements()
				.register(new RequirementCreator("placeholderRequired", PlaceholderRequirement.class,
						ItemUtils.item(XMaterial.NAME_TAG, Lang.RPlaceholder.toString()), PlaceholderRequirement::new));
		QuestsAPI.getAPI().registerMessageProcessor("placeholderapi_replace", 5, new PapiMessageProcessor());
	}

	private void registerVault() {
		QuestsAPI.getAPI().getRewards().register(new RewardCreator("moneyReward", MoneyReward.class,
				ItemUtils.item(XMaterial.EMERALD, Lang.rewardMoney.toString()), MoneyReward::new));
		QuestsAPI.getAPI().getRewards().register(new RewardCreator("permReward", PermissionReward.class,
				ItemUtils.item(XMaterial.REDSTONE_TORCH, Lang.rewardPerm.toString()), PermissionReward::new));
		QuestsAPI.getAPI().getRequirements().register(new RequirementCreator("moneyRequired", MoneyRequirement.class,
				ItemUtils.item(XMaterial.EMERALD, Lang.RMoney.toString()), MoneyRequirement::new));
	}



	
	private void registerMythicMobs() {
		try {
			Class.forName("io.lumine.mythic.api.MythicPlugin");
			QuestsAPI.getAPI().registerMobFactory(new MythicMobs5());
		} catch (ClassNotFoundException ex) {
			QuestsAPI.getAPI().registerMobFactory(new MythicMobs());
		}
	}

	private boolean isZnpcsVersionValid(Plugin plugin) {
		if (plugin.getClass().getName().equals("io.github.gonalez.znpcs.ServersNPC")) // NOSONAR
			return true;

		QuestsPlugin.getPlugin().getLoggerExpanded().warning("Your version of znpcs ("
				+ plugin.getDescription().getVersion() + ") is not supported by BeautyQuests.");
		return false;
	}

	private void registerZnpcsPlus() {
		try {
			Class.forName("lol.pyr.znpcsplus.api.NpcApiProvider");
			QuestsAPI.getAPI().addNpcFactory("znpcsplus", new BQZNPCsPlus());
		} catch (ClassNotFoundException ex) {
			QuestsAPI.getAPI().addNpcFactory("znpcsplus", new BQZNPCsPlusOld()); // TODO remove, old version of znpcs+

			QuestsPlugin.getPlugin().getLoggerExpanded()
					.warning("Your version of ZNPCsPlus will soon not be supported by BeautyQuests.");
		}
	}

	public IntegrationsConfiguration getConfig() {
		return config;
	}

}

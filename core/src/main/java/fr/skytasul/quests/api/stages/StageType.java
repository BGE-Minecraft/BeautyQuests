package fr.skytasul.quests.api.stages;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import fr.skytasul.quests.BeautyQuests;
import fr.skytasul.quests.api.serializable.SerializableCreator;
import fr.skytasul.quests.api.serializable.SerializableRegistry;
import fr.skytasul.quests.api.stages.options.StageOption;
import fr.skytasul.quests.gui.creation.stages.Line;
import fr.skytasul.quests.structure.QuestBranch;

public class StageType<T extends AbstractStage> {
	
	public final String id;
	public final Class<T> clazz;
	public final String name;
	public final StageLoader<T> loader;
	public final ItemStack item;
	public final StageCreationSupplier<T> creationSupplier;
	public final String[] dependencies;
	
	private final SerializableRegistry<StageOption<T>, SerializableCreator<StageOption<T>>> optionsRegistry = new SerializableRegistry<>();
	
	public StageType(String id, Class<T> clazz, String name, StageLoader<T> loader, ItemStack item, StageCreationSupplier<T> creationSupplier) {
		this(id, clazz, name, loader, item, creationSupplier, new String[0]);
	}
	
	@Deprecated
	public StageType(String id, Class<T> clazz, String name, StageDeserializationSupplier<T> deserializationSupplier, ItemStack item, StageCreationSupplier<T> creationSupplier, String... dependencies) {
		this(id, clazz, name, (StageLoader<T>) deserializationSupplier, item, creationSupplier, dependencies);
	}
	
	@Deprecated
	public StageType(String id, Class<T> clazz, String name, StageLoader<T> loader, ItemStack item, StageCreationSupplier<T> creationSupplier, String... dependencies) {
		this.id = id;
		this.clazz = clazz;
		this.name = name;
		this.item = item;
		this.loader = loader;
		this.creationSupplier = creationSupplier;
		this.dependencies = dependencies;
		if (dependencies.length != 0) BeautyQuests.logger.warning("Nag author of the " + id + " stage type about its use of the deprecated \"dependencies\" feature.");
	}
	
	public SerializableRegistry<StageOption<T>, SerializableCreator<StageOption<T>>> getOptionsRegistry() {
		return optionsRegistry;
	}
	
	@Deprecated
	public boolean isValid() {
		for (String depend : dependencies) {
			if (!Bukkit.getPluginManager().isPluginEnabled(depend)) return false;
		}
		return true;
	}
	
	@FunctionalInterface
	public static interface StageCreationSupplier<T extends AbstractStage> {
		
		StageCreation<T> supply(Line line, boolean endingStage);
		
	}
	
	@FunctionalInterface
	@Deprecated
	public static interface StageDeserializationSupplier<T extends AbstractStage> extends StageLoader<T> {
		
		/**
		 * @deprecated for removal, {@link StageLoader#supply(ConfigurationSection, QuestBranch)} should be used instead.
		 */
		@Deprecated
		T supply(Map<String, Object> serializedDatas, QuestBranch branch);
		
		@Override
		default T supply(ConfigurationSection section, QuestBranch branch) {
			return supply(section.getValues(false), branch);
		}
		
	}
	
	@FunctionalInterface
	public static interface StageLoader<T extends AbstractStage> {
		
		T supply(ConfigurationSection section, QuestBranch branch);
		
	}
	
}

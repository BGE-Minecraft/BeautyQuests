package fr.skytasul.quests.api.stages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import fr.skytasul.quests.BeautyQuests;
import fr.skytasul.quests.QuestsConfiguration;
import fr.skytasul.quests.api.QuestsAPI;
import fr.skytasul.quests.api.objects.QuestObject;
import fr.skytasul.quests.api.requirements.AbstractRequirement;
import fr.skytasul.quests.api.rewards.AbstractReward;
import fr.skytasul.quests.api.serializable.SerializableObject;
import fr.skytasul.quests.players.PlayerAccount;
import fr.skytasul.quests.players.PlayersManager;
import fr.skytasul.quests.players.events.PlayerAccountJoinEvent;
import fr.skytasul.quests.players.events.PlayerAccountLeaveEvent;
import fr.skytasul.quests.structure.QuestBranch;
import fr.skytasul.quests.structure.QuestBranch.Source;
import fr.skytasul.quests.utils.Utils;

public abstract class AbstractStage implements Listener{
	
	private final StageType<?> type;
	protected boolean asyncEnd = false;
	
	protected final QuestBranch branch;
	
	private String startMessage = null;
	private String customText = null;
	private List<AbstractReward> rewards = new ArrayList<>();
	private List<AbstractRequirement> validationRequirements = new ArrayList<>();
	
	protected AbstractStage(QuestBranch branch) {
		this.branch = branch;
		this.type = QuestsAPI.getStageType(getClass());
		
		Bukkit.getPluginManager().registerEvents(this, BeautyQuests.getInstance());
	}
	
	public QuestBranch getQuestBranch(){
		return branch;
	}
	
	public void setStartMessage(String text){
		this.startMessage = text;
	}
	
	public String getStartMessage(){
		return startMessage;
	}
	
	public List<AbstractReward> getRewards(){
		return rewards;
	}
	
	public void setRewards(List<AbstractReward> rewards){
		this.rewards = rewards;
		rewards.forEach(reward -> reward.attach(branch.getQuest()));
		checkAsync();
	}

	public List<AbstractRequirement> getValidationRequirements() {
		return validationRequirements;
	}

	public void setValidationRequirements(List<AbstractRequirement> validationRequirements) {
		this.validationRequirements = validationRequirements;
		validationRequirements.forEach(requirement -> requirement.attach(branch.getQuest()));
	}

	public String getCustomText(){
		return customText;
	}
	
	public void setCustomText(String message) {
		this.customText = message;
	}
	
	public boolean sendStartMessage(){
		return startMessage == null && QuestsConfiguration.sendStageStartMessage();
	}
	
	public StageType<?> getType() {
		return type;
	}
	
	public boolean hasAsyncEnd(){
		return asyncEnd;
	}
	
	private void checkAsync(){
		for(AbstractReward rew : rewards){
			if (rew.isAsync()) {
				asyncEnd = true;
				break;
			}
		}
	}
	
	public int getID(){
		return branch.getID(this);
	}
	
	public int getStoredID(){
		if (branch.isRegularStage(this)) {
			return 0;
		}
		int index = 0;
		for (AbstractStage stage : branch.getEndingStages().keySet()) {
			if (stage == this) break;
			index++;
		}
		return index;
	}
	
	protected boolean canUpdate(Player p) {
		return canUpdate(p, false);
	}

	protected boolean canUpdate(Player p, boolean msg) {
		for (AbstractRequirement requirement : validationRequirements) {
			if (!requirement.test(p)) {
				if (msg) requirement.sendReason(p);
				return false;
			}
		}
		return true;
	}
	
	public String debugName() {
		return "quest " + branch.getQuest().getID() + ", branch " + branch.getID() + ", stage " + getID() + "(" + type.id + ")";
	}

	/**
	 * Called internally when a player finish stage's objectives
	 * @param p Player who finish the stage
	 */
	protected final void finishStage(Player p) {
		branch.finishStage(p, this);
	}
	
	/**
	 * Called internally to test if a player has the stage started
	 * @param p Player to test
	 * @see QuestBranch#hasStageLaunched(PlayerAccount, AbstractStage)
	 */
	protected final boolean hasStarted(Player p){
		return branch.hasStageLaunched(PlayersManager.getPlayerAccount(p), this);
	}
	
	/**
	 * Called when the stage starts (player can be offline)
	 * @param acc PlayerAccount for which the stage starts
	 */
	public void start(PlayerAccount acc) {
		if (acc.isCurrent()) Utils.sendOffMessage(acc.getPlayer(), startMessage);
		Map<String, Object> datas = new HashMap<>();
		initPlayerDatas(acc, datas);
		acc.getQuestDatas(branch.getQuest()).setStageDatas(getStoredID(), datas);
		QuestsAPI.propagateQuestsHandlers(handler -> handler.stageStart(acc, this));
	}
	
	protected void initPlayerDatas(PlayerAccount acc, Map<String, Object> datas) {}

	/**
	 * Called when the stage ends (player can be offline)
	 * @param acc PlayerAccount for which the stage ends
	 */
	public void end(PlayerAccount acc) {
		acc.getQuestDatas(branch.getQuest()).setStageDatas(getStoredID(), null);
		QuestsAPI.propagateQuestsHandlers(handler -> handler.stageEnd(acc, this));
	}
	
	/**
	 * Called when an account with this stage launched joins
	 * @param acc PlayerAccount which just joined
	 */
	public void joins(PlayerAccount acc, Player p) {
		QuestsAPI.propagateQuestsHandlers(handler -> handler.stageJoin(acc, p, this));
	}
	
	/**
	 * Called when an account with this stage launched leaves
	 * @param acc PlayerAccount which just left
	 */
	public void leaves(PlayerAccount acc, Player p) {
		QuestsAPI.propagateQuestsHandlers(handler -> handler.stageLeave(acc, p, this));
	}
	
	public final String getDescriptionLine(PlayerAccount acc, Source source){
		if (customText != null) return "§e" + Utils.format(customText, descriptionFormat(acc, source));
		try{
			return descriptionLine(acc, source);
		}catch (Exception ex){
			BeautyQuests.logger.severe("An error occurred while getting the description line for player " + acc.getName() + " in " + debugName(), ex);
			return "§a" + type.name;
		}
	}
	
	/**
	 * @param acc PlayerAccount who has the stage in progress
	 * @param source source of the description request
	 * @return the progress of the stage for the player
	 */
	protected abstract String descriptionLine(PlayerAccount acc, Source source);
	
	/**
	 * Will be called only if there is a {@link #customText}
	 * @param acc PlayerAccount who has the stage in progress
	 * @param source source of the description request
	 * @return all strings that can be used to format the custom description text
	 */
	protected Object[] descriptionFormat(PlayerAccount acc, Source source) {return null;}
	
	public void updateObjective(PlayerAccount acc, Player p, String dataKey, Object dataValue) {
		Map<String, Object> datas = acc.getQuestDatas(branch.getQuest()).getStageDatas(getStoredID());
		Validate.notNull(datas, "Account " + acc.debugName() + " does not have datas for " + debugName());
		datas.put(dataKey, dataValue);
		acc.getQuestDatas(branch.getQuest()).setStageDatas(getStoredID(), datas);
		branch.getBranchesManager().objectiveUpdated(p, acc);
	}

	protected <T> T getData(PlayerAccount acc, String dataKey) {
		Map<String, Object> stageDatas = acc.getQuestDatas(branch.getQuest()).getStageDatas(getStoredID());
		return stageDatas == null ? null : (T) stageDatas.get(dataKey);
	}

	/**
	 * Called when the stage has to be unloaded
	 */
	public void unload(){
		QuestsAPI.propagateQuestsHandlers(handler -> handler.stageUnload(this));
        HandlerList.unregisterAll(this);
		rewards.forEach(AbstractReward::detach);
		validationRequirements.forEach(AbstractRequirement::detach);
	}
	
	/**
	 * Called when the stage loads
	 */
	public void load() {
		QuestsAPI.propagateQuestsHandlers(handler -> handler.stageLoad(this));
	}
	
	@EventHandler
	public void onJoin(PlayerAccountJoinEvent e) {
		if (branch.hasStageLaunched(e.getPlayerAccount(), this)) {
			joins(e.getPlayerAccount(), e.getPlayer());
		}
	}
	
	@EventHandler
	public void onLeave(PlayerAccountLeaveEvent e) {
		if (branch.hasStageLaunched(e.getPlayerAccount(), this)) {
			leaves(e.getPlayerAccount(), e.getPlayer());
		}
	}
	
	/**
	 * @deprecated for removal, {@link #serialize(ConfigurationSection)} should be used instead.
	 */
	@Deprecated
	protected void serialize(Map<String, Object> map) {}
	
	protected void serialize(ConfigurationSection section) {
		Map<String, Object> map = new HashMap<>();
		serialize(map);
		map.forEach(section::set);
	}
	
	public final void save(ConfigurationSection section) {
		serialize(section);
		
		section.set("stageType", type.id);
		section.set("customText", customText);
		if (startMessage != null) section.set("text", startMessage);
		
		if (!rewards.isEmpty()) section.set("rewards", SerializableObject.serializeList(rewards));
		if (!validationRequirements.isEmpty()) section.set("requirements", SerializableObject.serializeList(validationRequirements));
	}
	
	public static AbstractStage deserialize(ConfigurationSection section, QuestBranch branch) {
		String typeID = section.getString("stageType");
		
		Optional<StageType<?>> stageTypeOptional = QuestsAPI.stages.stream().filter(type -> type.id.equals(typeID)).findAny();
		if (!stageTypeOptional.isPresent()) {
			BeautyQuests.getInstance().getLogger().severe("Unknown stage type : " + typeID);
			return null;
		}
		
		StageType<?> stageType = stageTypeOptional.get();
		if (!stageType.isValid()) {
			BeautyQuests.getInstance().getLogger().severe("The stage " + typeID + " requires not enabled dependencies: " + Arrays.toString(stageType.dependencies));
			return null;
		}

		AbstractStage st = stageType.loader.supply(section, branch);
		if (section.contains("text")) st.startMessage = section.getString("text");
		if (section.contains("customText")) st.customText = section.getString("customText");
		if (section.contains("rewards")) st.setRewards(QuestObject.deserializeList(section.getMapList("rewards"), AbstractReward::deserialize));
		if (section.contains("requirements")) st.setValidationRequirements(QuestObject.deserializeList(section.getMapList("requirements"), AbstractRequirement::deserialize));
		
		return st;
	}
}

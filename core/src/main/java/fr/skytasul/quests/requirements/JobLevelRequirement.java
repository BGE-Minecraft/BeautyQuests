package fr.skytasul.quests.requirements;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import fr.skytasul.quests.api.objects.QuestObjectClickEvent;
import fr.skytasul.quests.api.objects.QuestObjectLoreBuilder;
import fr.skytasul.quests.api.requirements.AbstractRequirement;
import fr.skytasul.quests.api.requirements.TargetNumberRequirement;
import fr.skytasul.quests.editors.TextEditor;
import fr.skytasul.quests.utils.ComparisonMethod;
import fr.skytasul.quests.utils.Lang;
import fr.skytasul.quests.utils.compatibility.Jobs;

public class JobLevelRequirement extends TargetNumberRequirement {

	public String jobName;
	
	public JobLevelRequirement() {
		this(null, null, null, 0, ComparisonMethod.GREATER_OR_EQUAL);
	}
	
	public JobLevelRequirement(String customDescription, String customReason, String jobName, double target,
			ComparisonMethod comparison) {
		super(customDescription, customReason, target, comparison);
		this.jobName = jobName;
	}

	@Override
	public double getPlayerTarget(Player p) {
		return Jobs.getLevel(p, jobName);
	}
	
	@Override
	public Class<? extends Number> numberClass() {
		return Integer.class;
	}
	
	@Override
	public void sendHelpString(Player p) {
		Lang.CHOOSE_XP_REQUIRED.send(p);
	}
	
	@Override
	protected String getDefaultReason(Player player) {
		return Lang.REQUIREMENT_JOB.format(getFormattedValue(), jobName);
	}
	
	@Override
	public String getDefaultDescription(Player p) {
		return Lang.RDJobLevel.format(Integer.toString((int) target), jobName);
	}

	@Override
	public AbstractRequirement clone() {
		return new JobLevelRequirement(getCustomDescription(), getCustomReason(), jobName, target, comparison);
	}
	
	@Override
	protected void addLore(QuestObjectLoreBuilder loreBuilder) {
		super.addLore(loreBuilder);
		loreBuilder.addDescription("§8Job name: §7" + jobName);
	}
	
	@Override
	public void itemClick(QuestObjectClickEvent event) {
		Lang.CHOOSE_JOB_REQUIRED.send(event.getPlayer());
		new TextEditor<String>(event.getPlayer(), event::cancel, obj -> {
			jobName = obj;
			super.itemClick(event);
		}).useStrippedMessage().enter();
	}
	
	@Override
	public void save(ConfigurationSection section) {
		super.save(section);
		section.set("jobName", jobName);
	}

	@Override
	public void load(ConfigurationSection section) {
		super.load(section);
		jobName = section.getString("jobName");
	}
	
}
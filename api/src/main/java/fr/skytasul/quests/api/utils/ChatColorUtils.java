package fr.skytasul.quests.api.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Various utility methods for handling Minecraft colors in strings.
 *
 * @author SkytAsul
 */
public final class ChatColorUtils {

	private ChatColorUtils() {}

	private static final Pattern START_WITH_COLORS = Pattern.compile("(?i)^(" + ChatColor.COLOR_CHAR + "[0-9A-FK-ORX])+");
	private static final Pattern COLOR = Pattern.compile("(?i)" + ChatColor.COLOR_CHAR + "[0-9A-FK-ORX]");
	private static final Pattern HEX_COLOR = Pattern.compile("(?i)[&§]#([A-F0-9]{6})");
	private static final Pattern NEWLINE_PATTERN = Pattern.compile("\\\\n|\\{nl\\}");

	/**
	 * Breaks a raw string up into a series of lines. Words are wrapped using
	 * spaces as decimeters and the newline character is respected.
	 *
	 * @param rawString The raw string to break.
	 * @param lineLength The length of a line of text.
	 * @return An array of word-wrapped lines.
	 */
	public static @NotNull List<@NotNull String> wordWrap(@Nullable String rawString, int lineLength) {
		return wordWrap(rawString, lineLength, lineLength);
	}

	public static @NotNull List<@NotNull String> wordWrap(@Nullable String rawString, int maxLineLength,
			int criticalLineLength) {
		if (maxLineLength > criticalLineLength) maxLineLength = criticalLineLength;
		// A null string is a single line
		if (rawString == null) {
			return Arrays.asList("");
		}

		rawString = NEWLINE_PATTERN.matcher(rawString).replaceAll("\n");

		// A string shorter than the lineWidth is a single line
		if (rawString.length() <= maxLineLength && !rawString.contains("\n")) {
			return Arrays.asList(rawString);
		}

		try {
			char[] rawChars = (rawString + ' ').toCharArray(); // add a trailing space to trigger pagination
			StringBuilder word = new StringBuilder();
			StringBuilder line = new StringBuilder(maxLineLength);
			String colorsWord = "";
			String colors = "";
			boolean first = false;
			List<String> lines = new LinkedList<>();
			int wordLength = 0;
			int lineLength = 0;

			for (int i = 0; i < rawChars.length; i++) {
				char c = rawChars[i];

				// skip chat color modifiers
				if (c == ChatColor.COLOR_CHAR) {
					Matcher matcher = START_WITH_COLORS.matcher(rawString.substring(i));
					if (matcher.find()) {
						String rawColors = matcher.group();
						String oldColors = colors;
						colors = appendRawColorString(colors, rawColors);
						String toAdd = getColorDifference(oldColors, colors);
						//System.out.println("new colors: " + colors + " | to add: " + toAdd);

						if (colors.length() >= criticalLineLength) { // weird case : the formatting and word is longer than a line
							colors = "";
						}else {
							word.append(toAdd);
						}
						i += rawColors.length() - 1; // eat all color characters minus 1 (first § already processed)
					}else {
						word.append(c); // unknown color code: we copy it
					}
				}else if (c == ' ' || c == '\n') {
					if (lineLength == 0 && (wordLength > maxLineLength || word.length() > criticalLineLength)) { // special case: extremely long word begins a line
						//System.out.println("long word : " + word);
						lines.addAll(splitColoredWord(word.toString(), wordLength, maxLineLength, criticalLineLength, colorsWord));
					}else {
						int totalLength = (lineLength == 0 ? 0 : lineLength + 1) + wordLength;
						int totalLengthC = (lineLength == 0 ? 0 : line.length() + 1) + word.length();
						//System.out.println("so far: " + line.toString() + " | " + word.toString() + " | total length " + totalLength + " | total COMPLETE " + totalLengthC);
						if ((totalLength == maxLineLength && totalLengthC <= criticalLineLength) || (totalLengthC == criticalLineLength && totalLength <= maxLineLength)) { // Line exactly the correct length...newline
							if (lineLength > 0) line.append(' ');
							line.append(word);
							lines.add(line.toString());
							//System.out.println("append exact " + line.toString());
							line = new StringBuilder(maxLineLength);
							lineLength = 0;
							first = true;
						}else if (totalLength > maxLineLength || totalLengthC > criticalLineLength) { // Line too long...break the line
							//System.out.println("too long " + line.toString() + " | plus : " + word.toString() + " | linelength: " + lineLength + " | wordlength: " + wordLength);
							lines.add(line.toString());
							if (word.length() >= maxLineLength || word.length() >= criticalLineLength) {
								//System.out.println("entire word too long. split: " + word.toString() + " length " + wordLength + " colors " + colorsWord);
								lines.addAll(splitColoredWord(colorsWord + word.toString(), wordLength, maxLineLength, criticalLineLength, colorsWord));
								lineLength = 0;
								//System.out.println("lines: " + lines);
								line = new StringBuilder(maxLineLength);
							}else {
								int textIndex = getFirstTextIndex(word.toString());
								line = new StringBuilder(maxLineLength); // start the line with the word
								line.append(word.substring(textIndex));
								lineLength = wordLength;
								line.insert(0, appendRawColorString(colorsWord, word.substring(0, textIndex)));
								//System.out.println("line: " + line.toString());
							}
						}else {
							if (line.length() > 0) {
								if (!first) {
									line.append(' ');
									lineLength++;
								}
							}
							first = false;
							line.append(word);
							lineLength += wordLength;
							//System.out.println("append word " + word);
						}
					}
					colorsWord = colors;
					word = lineLength == 0 ? new StringBuilder(colors) : new StringBuilder();
					wordLength = 0;

					if (c == '\n') { // Newline forces the line to flush
						////System.out.println(lastColors.replace('§', '&') + " LINE " + line.toString());
						lines.add(line.toString());
						line = new StringBuilder(maxLineLength);
						lineLength = 0;
						word.append(colors);
						first = true;
					}
				}else {
					word.append(c);
					wordLength++;
				}
			}

			if (line.length() > 0) { // Only add the last line if there is anything to add
				lines.add(line.toString());
			}

			return lines;
		}catch (Exception ex) {
			new RuntimeException("An exception occurred while trying to split the string " + rawString + " with max length " + maxLineLength + " and critical length " + criticalLineLength, ex).printStackTrace();
			rawString = "§4ERROR " + rawString;
			return Arrays.asList(rawString.split("(?<=\\G.{4})"));
		}
	}

	public static @NotNull String appendRawColorString(@NotNull String original, @NotNull String appended) {
		StringBuilder builder = new StringBuilder(original);
		StringBuilder hexBuilder = null;
		for (int colorIndex = 1; colorIndex < appended.length(); colorIndex += 2) {
			char cc = appended.charAt(colorIndex);
			if (hexBuilder != null) {
				hexBuilder.append('§').append(cc);
				if (hexBuilder.length() == 14) { // end of the color
					builder = hexBuilder; // as it is a color, previous formatting is lost
					hexBuilder = null;
				}
			}else if (cc == 'x') {
				if (colorIndex + 2 * 6 < appended.length()) {
					hexBuilder = new StringBuilder("§x");
				}
			}else {
				ChatColor sub = ChatColor.getByChar(cc);
				if (sub != null) {
					if (sub == ChatColor.RESET) {
						builder.setLength(0); // reset -> empty previous format and do not copy the reset code
						continue;
					}else if (sub.isColor()) {
						builder.setLength(0); // color -> empty previous format and copy color
					}
					builder.append(sub.toString());
				}else builder.append('§').append(cc); // unknown color character
			}
		}
		return builder.toString();
	}

	private static @NotNull String getColorDifference(@NotNull String oldColors, @NotNull String newColors) {
		return newColors.startsWith(oldColors) ? newColors.substring(oldColors.length()) : newColors;
	}

	private static @NotNull List<@NotNull String> splitColoredWord(@NotNull String string, int stringLength, int maxLength,
			int criticalLength, @NotNull String startColors) {
		final String original = string;
		int loops = 0;
		List<String> split = new ArrayList<>();
		while (stringLength >= maxLength || string.length() >= criticalLength) {
			if (loops++ > 1000) throw new RuntimeException("Failed to split a colored string: " + original);
			//System.out.println("Colored string too long: " + string + " | length " + stringLength + " | COMPLETE " + string.length());
			int colorIndex = string.indexOf('§');
			if (colorIndex > maxLength || colorIndex == -1) { // before color -> only text
				split.add(startColors + string.substring(0, maxLength));
				//System.out.println("Colors too long: " + split + " | color index " + colorIndex);
				string = string.substring(maxLength);
				stringLength -= maxLength;
			}else {
				int length = 0;
				int previousIndex = -2;
				String colors = startColors;
				//System.out.println("Matching " + string);
				Matcher matcher = COLOR.matcher(string);
				while (matcher.find()) {
					if (loops++ > 1000) throw new RuntimeException("Failed to split a colored string: " + original);
					length += matcher.start() - previousIndex - 2;
					//System.out.println("Matched " + matcher.group() + " | Length " + length);
					if (length > maxLength || matcher.start() > criticalLength) {
						split.add(string.substring(0, previousIndex));
						//System.out.println("Added part: " + split);
						string = startColors + string.substring(previousIndex);
						startColors = colors;
						matcher = COLOR.matcher(string);
						stringLength -= length;
						length = 0;
						previousIndex = -2;
						continue;
					}
					colors = appendRawColorString(colors, matcher.group());
					previousIndex = matcher.start();
				}
				length += string.length() - (previousIndex + 2);
				int truncated = 0;
				if (length >= maxLength) {
					truncated = maxLength + startColors.length();
					if (string.charAt(truncated - 1) == '§') truncated--;
					split.add(string.substring(0, truncated));
					//System.out.println("Truncated with length: " + split);
					string = startColors + string.substring(truncated);
				}else if (string.length() >= criticalLength) {
					truncated = criticalLength - startColors.length();
					split.add(string.substring(0, truncated));
					//System.out.println("Truncated with critical length: " + split);
					string = startColors + string.substring(truncated);
				}
				stringLength -= truncated;
				startColors = colors;
			}
		}
		split.add(string);
		return split;
	}

	private static int getFirstTextIndex(@NotNull String string) {
		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			if (i % 2 == 0) {
				if (c != ChatColor.COLOR_CHAR) return i;
			}else {
				if (net.md_5.bungee.api.ChatColor.ALL_CODES.indexOf(c) == -1) return i - 1;
			}
		}
		return string.length();
	}

	public static @NotNull String translateHexColorCodes(@NotNull String message) {
		Matcher matcher = HEX_COLOR.matcher(message);
		StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);
		while (matcher.find()) {
			String group = matcher.group(1);
			matcher.appendReplacement(buffer, ChatColor.COLOR_CHAR + "x"
					+ ChatColor.COLOR_CHAR + group.charAt(0) + ChatColor.COLOR_CHAR + group.charAt(1)
					+ ChatColor.COLOR_CHAR + group.charAt(2) + ChatColor.COLOR_CHAR + group.charAt(3)
					+ ChatColor.COLOR_CHAR + group.charAt(4) + ChatColor.COLOR_CHAR + group.charAt(5));
		}
		return matcher.appendTail(buffer).toString();
	}

	public static @NotNull String getLastColors(@NotNull String originalColors, @Nullable String appended) {
		if (appended == null || appended.length() == 0) return originalColors;
		StringBuilder builder = originalColors == null ? new StringBuilder() : new StringBuilder(originalColors);
		StringBuilder hexBuilder = null;
		boolean color = false;
		for (int colorIndex = 0; colorIndex < appended.length(); colorIndex++) {
			char cc = Character.toLowerCase(appended.charAt(colorIndex));
			if (cc == ChatColor.COLOR_CHAR) {
				color = true;
			}else if (color) {
				color = false;
				if (hexBuilder != null) {
					if ((cc >= 48 && cc <= 57) || (cc >= 97 && cc <= 102)) { // hex character
						hexBuilder.append('§').append(cc);
						if (hexBuilder.length() == 14) { // end of the color
							builder = hexBuilder; // as it is a color, previous formatting is lost
							hexBuilder = null;
						}
					}else {
						hexBuilder = null;
					}
				}else if (cc == 'x') {
					if (colorIndex + 2 * 6 < appended.length()) {
						hexBuilder = new StringBuilder("§x");
					}
				}else {
					ChatColor sub = ChatColor.getByChar(cc);
					if (sub != null) {
						if (sub == ChatColor.RESET) {
							builder.setLength(0); // reset -> empty previous format and do not copy the reset code
							continue;
						}else if (sub.isColor()) {
							builder.setLength(0); // color -> empty previous format and copy color
						}
						builder.append(sub.toString());
					}else builder.append('§').append(cc); // unknown color character
				}
			}else if (hexBuilder != null) {
				hexBuilder = null;
			}
		}
		return builder.toString();
	}

}

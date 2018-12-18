package com.hgames.rhogue.text;

import java.util.List;

import squidpony.panel.IColoredString;
import squidpony.panel.IMarkup;

/**
 * Stuff about {@link IColoredString}.
 * 
 * @author smelC
 */
public class IColoredStrings {

	/**
	 * @param text
	 * @param markup
	 * @param joiner
	 * @return {@code text[0].presentWithMarkup(markup) + joiner + ...}
	 */
	public static <T> String applyMarkup(List<IColoredString<T>> text, IMarkup<T> markup, String joiner) {
		final StringBuilder result = new StringBuilder();
		final int sz = text.size();
		for (int i = 0; i < sz; i++) {
			final IColoredString<T> ics = text.get(i);
			if (ics == null) {
				assert false;
			} else {
				result.append(ics.presentWithMarkup(markup));
				if (i < sz - 1)
					result.append(joiner);
			}
		}
		return result.toString();
	}

	/**
	 * @param text
	 * @param markup
	 * @param default_
	 * @param highlight
	 * @param searcheds
	 * @return {@code text} where the first occurence of a member of
	 *         {@code searcheds} is highlight with {@code highlight}.
	 */
	public static <T> String colorizeFirstCharIn(String text, IMarkup<T> markup, T default_, T highlight,
			char... searcheds) {
		int idx = -1;
		for (char searched : searcheds) {
			idx = text.indexOf(searched);
			if (0 <= idx)
				/* found */
				break;
		}
		if (idx < 0) {
			/* 'searcheds' not found */
			return typeset(text, markup, default_);
		}
		final int len = text.length();
		final String start = idx == 0 ? "" : typeset(text.substring(0, idx - 1), markup, default_);
		final String hl = typeset(String.valueOf(text.charAt(idx)), markup, highlight);
		final String end = idx == len - 1 ? "" : typeset(text.substring(idx + 1, len), markup, default_);
		return start + hl + end;
	}

	/**
	 * @param i
	 * @param markup
	 * @param color
	 * @return The application of {@code markup} on {@code i + color}
	 */
	public static <T> String typeset(int i, IMarkup<T> markup, T color) {
		return typeset(String.valueOf(i), markup, color);
	}

	/**
	 * @param text
	 * @param markup
	 * @param color
	 * @return The application of {@code markup} on {@code text + color}
	 */
	public static <T> String typeset(String text, IMarkup<T> markup, T color) {
		return text.length() == 0 ? "" : markup.getMarkup(color) + text + markup.closeMarkup();
	}

}

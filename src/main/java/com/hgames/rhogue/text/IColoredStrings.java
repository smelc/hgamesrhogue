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
			if (ics == null)
				assert false;
			else {
				result.append(ics.presentWithMarkup(markup));
				if (i < sz - 1)
					result.append(joiner);
			}
		}
		return result.toString();
	}

}

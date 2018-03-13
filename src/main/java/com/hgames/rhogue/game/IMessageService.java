package com.hgames.rhogue.game;

import java.util.List;

import com.hgames.lib.color.IColor;

import squidpony.panel.IColoredString;

/**
 * Services provided by {@link IGame} about messages.
 * 
 * @author smelC
 */
public interface IMessageService {

	/**
	 * Writes {@code text} to the UI.
	 * 
	 * @param text
	 */
	public void write(/* @Nullable */ String text);

	/**
	 * Writes {@code text} to the UI.
	 * 
	 * @param text
	 * @param color
	 *            {@code text}'s color.
	 */
	public void write(/* @Nullable */ String text, /* @Nullable */ IColor color);

	/**
	 * Writes {@code text} to the UI.
	 * 
	 * @param text
	 */
	public void write(IColoredString<IColor> text);

	/**
	 * Writes {@code text} to the UI.
	 * 
	 * @param text
	 */
	public void write(List<? extends IColoredString<IColor>> text);

}

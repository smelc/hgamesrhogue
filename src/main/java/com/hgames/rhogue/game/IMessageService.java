package com.hgames.rhogue.game;

import java.util.List;

import com.hgames.lib.color.IColor;

import squidpony.panel.IColoredString;
import squidpony.squidmath.Coord;

/**
 * Services provided by {@link IGame} about messages.
 * 
 * @author smelC
 */
public interface IMessageService {

	/** Removes all existing messages */
	public void clear();

	/**
	 * Call done when the map's viewport changes
	 * 
	 * @param deltax
	 *            The change, in number of cells
	 * @param deltay
	 *            The change, in number of cells
	 */
	public void handleMapViewPortChange(int deltax, int deltay);

	/**
	 * @param where
	 *            Where the text should be written (mapwise)
	 * @param text
	 *            The text to display
	 * @param color
	 *            The color to use
	 */
	public void write(/* @Nullable */ Coord where, /* @Nullable */ String text, /* @Nullable */ IColor color);

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

}

package com.hgames.rhogue.generation.map.draw;

/**
 * Class to convert a dungeon to a {@link String}.
 * 
 * @author churlin
 * @param <T>
 */
public abstract class Generic2DArrayDrawer<T> {

	protected final String lineSeparator;

	/**
	 * @param lineSeparator
	 *            The separator to use. {@code System#getProperty("line.separator")}
	 *            isn't used, because it isn't GWT-compatible.
	 */
	public Generic2DArrayDrawer(String lineSeparator) {
		this.lineSeparator = lineSeparator;
	}

	/**
	 * @param array
	 * @return {@code array} drawn with {@link #draw(Object)}.
	 */
	public String draw(T[][] array) {
		final int width = array.length;
		final int height = width == 0 ? 0 : array[0].length;
		final StringBuilder result = new StringBuilder(width * height);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				final T sym = array[x][y];
				result.append(draw(sym));
			}
			result.append(lineSeparator);
		}
		return result.toString();
	}

	protected abstract char draw(T t);

}

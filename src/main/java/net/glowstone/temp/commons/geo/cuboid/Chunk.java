package net.glowstone.temp.commons.geo.cuboid;

import net.glowstone.temp.commons.geo.World;
import net.glowstone.temp.commons.geo.point.Point;

/**
 * Represents a cube containing 16x16x16 Blocks
 */
public class Chunk extends Cube {
	
	private final static double EDGE = 16.0;
	
	public Chunk(World world, double x, double y, double z) {
		super(new Point(world, x, y, z), EDGE);
	}

}

/*
 * This file is part of Spout.
 *
 * Copyright (c) 2011 Spout LLC <http://www.spout.org/>
 * Spout is licensed under the Spout License Version 1.
 *
 * Spout is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * In addition, 180 days after any changes are published, you can use the
 * software, incorporating those changes, under the terms of the MIT license,
 * as described in the Spout License Version 1.
 *
 * Spout is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Lesser General Public License,
 * the MIT license and the Spout License Version 1 along with this program.
 * If not, see <http://www.gnu.org/licenses/> for the GNU Lesser General Public
 * License and see <http://spout.in/licensev1> for the full license, including
 * the MIT license.
 */
package org.spout.api.event.server.protection;

import org.spout.api.entity.Entity;
import org.spout.api.event.Cancellable;
import org.spout.api.event.HandlerList;
import org.spout.api.event.entity.AbstractEntityEvent;
import org.spout.api.geo.discrete.Point;
import org.spout.api.material.BlockMaterial;

/**
 * This {@link org.spout.api.event.entity.AbstractEntityEvent} is designed to be fired by plugins that wish to check if an entity can place the given block at the current {@link Point}. Protection
 * plugins should utilize this event to deny entities building in specific locations.
 */
public class EntityCanBuildEvent extends AbstractEntityEvent implements Cancellable {
	private static HandlerList handlers = new HandlerList();
	private final Point point;
	private final BlockMaterial material;

	public EntityCanBuildEvent(Entity entity, Point point, BlockMaterial bm) {
		super(entity);
		this.point = point;
		this.material = bm;
	}

	/**
	 * The Material data for the Block that is being placed.
	 *
	 * @return the material
	 */
	public BlockMaterial getMaterial() {
		return material;
	}

	/**
	 * The Point where the player is attempting to place a block.
	 *
	 * @return the point
	 */
	public Point getPoint() {
		return point;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		super.setCancelled(cancelled);
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}

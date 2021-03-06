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
package org.spout.engine.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.spout.api.Platform;
import org.spout.api.Spout;
import org.spout.api.component.entity.PlayerNetworkComponent;
import org.spout.api.entity.Entity;
import org.spout.api.entity.Player;
import org.spout.api.protocol.event.EntitySyncEvent;
import org.spout.engine.component.entity.SpoutPhysicsComponent;
import org.spout.engine.util.thread.snapshotable.SnapshotManager;
import org.spout.engine.util.thread.snapshotable.SnapshotableHashMap;
import org.spout.engine.world.SpoutChunk;
import org.spout.engine.world.SpoutRegion;

/**
 * A class which manages all of the entities within a world.
 */
public class EntityManager {
	/**
	 * The snapshot manager
	 */
	protected final SnapshotManager snapshotManager = new SnapshotManager();
	/**
	 * A map of all the entity ids to the corresponding entities.
	 */
	private final SnapshotableHashMap<Integer, SpoutEntity> entities = new SnapshotableHashMap<>(snapshotManager);
	/**
	 * The next id to check.
	 */
	private final static AtomicInteger nextId = new AtomicInteger(1);
	/**
	 * The region with entities this manager manages.
	 */
	private final SpoutRegion region;
	/**
	 * Player listings plus listings of sync'd entities per player
	 */
	private final SnapshotableHashMap<Player, ArrayList<SpoutEntity>> players = new SnapshotableHashMap<>(snapshotManager);

	public EntityManager(SpoutRegion region) {
		if (region == null) {
			throw new NullPointerException("Region can not be null!");
		}
		this.region = region;
	}

	/**
	 * Gets all entities.
	 *
	 * @return A collection of entities.
	 */
	public Collection<SpoutEntity> getAll() {
		return entities.get().values();
	}

	/**
	 * Gets all the entities that are in a live state (not the snapshot).
	 *
	 * @return A collection of entities
	 */
	public Collection<SpoutEntity> getAllLive() {
		return entities.getLive().values();
	}

	/**
	 * Gets all the players currently in the engine.
	 *
	 * @return The list of players.
	 */
	public List<Player> getPlayers() {
		return new ArrayList<>(players.get().keySet());
	}

	/**
	 * Gets an entity by its id.
	 *
	 * @param id The id.
	 * @return The entity, or {@code null} if it could not be found.
	 */
	public SpoutEntity getEntity(int id) {
		return entities.get().get(id);
	}

	/**
	 * Adds an entity to the manager.
	 *
	 * @param entity The entity
	 */
	public void addEntity(SpoutEntity entity) {
		int currentId = entity.getId();
		if (currentId == SpoutEntity.NOTSPAWNEDID) {
			currentId = getNextId();
			entity.setId(currentId);
		}
		entities.put(currentId, entity);
		if (entity instanceof Player) {
			players.put((Player) entity, new ArrayList<SpoutEntity>());
		}
	}

	private static int getNextId() {
		int id = nextId.getAndIncrement();
		if (id == -2) {
			throw new IllegalStateException("Entity id space exhausted");
		}
		return id;
	}

	public boolean isSpawnable(SpoutEntity entity) {
		if (entity.getId() == SpoutEntity.NOTSPAWNEDID) {
			return true;
		}
		return false;
	}

	/**
	 * Removes an entity from the manager.
	 *
	 * @param entity The entity
	 */
	public void removeEntity(SpoutEntity entity) {
		entities.remove(entity.getId());
		if (entity instanceof Player) {
			players.remove((Player) entity);
		}
	}

	/**
	 * Finalizes the manager at the FINALIZERUN tick stage
	 */
	public void finalizeRun() {
		for (SpoutEntity e : entities.get().values()) {
			e.finalizeRun();
		}
	}

	/**
	 * Finalizes the manager at the FINALIZERUN tick stage
	 */
	public void preSnapshotRun() {
		for (SpoutEntity e : entities.get().values()) {
			e.preSnapshotRun();
		}
	}

	/**
	 * Snapshots the manager and all the entities managed in the SNAPSHOT tickstage.
	 */
	public void copyAllSnapshots() {
		for (SpoutEntity e : entities.get().values()) {
			e.copySnapshot();
		}
		snapshotManager.copyAllSnapshots();

		// We want one more tick with for the removed Entities
		// The next tick works with the snapshotted values which contains has all removed entities with idRemoved true
		for (SpoutEntity e : entities.get().values()) {
			if (e.isRemoved()) {
				removeEntity(e);
			}
		}
	}

	/**
	 * The region this entity manager oversees
	 *
	 * @return region
	 */
	public SpoutRegion getRegion() {
		return region;
	}

	/**
	 * Syncs all entities/observers in this region
	 */
	public void syncEntities() {
		if (!(Spout.getPlatform() == Platform.SERVER)) {
			throw new UnsupportedOperationException("Must be in server mode to sync entities");
		}
		for (Entity observed : getAll()) {
			if (observed.getId() == SpoutEntity.NOTSPAWNEDID) {
				throw new IllegalStateException("Attempt to sync entity with not spawned id.");
			}
			if (observed.getChunk() == null) {
				continue;
			}
			//Players observing the chunk this entity is in
			Set<? extends Entity> observers = observed.getChunk().getObservers();
			syncEntity(observed, observers, false);

			//TODO: Why do we need this...?
			Set<? extends Entity> expiredObservers = ((SpoutChunk) observed.getChunk()).getExpiredObservers();
			syncEntity(observed, expiredObservers, true);
		}
	}

	private void syncEntity(Entity observed, Set<? extends Entity> observers, boolean forceDestroy) {
		for (Entity observer : observers) {
			//Non-players have no synchronizer, ignore
			if (!(observer instanceof Player)) {
				continue;
			}
			Player player = (Player) observer;
			//Grab the NetworkSynchronizer of the player
			PlayerNetworkComponent network = player.getNetwork();
			//Grab player's sync distance
			int syncDistance = network.getSyncDistance();
			/*
			 * Just because a player can see a chunk doesn't mean the entity is within sync-range, do the math and sync based on the result.
			 *
			 * Following variables hold sync status
			 */
			boolean add, sync, remove;
			add = sync = remove = false;
			//Entity is out of range of the player's view distance, destroy
			final SpoutPhysicsComponent physics = (SpoutPhysicsComponent) observed.getPhysics();
			if (forceDestroy || observed.isRemoved() || physics.getTransformLive().getPosition().distanceSquared(player.getPhysics().getPosition()) > syncDistance * syncDistance || player.isInvisible(observed)) {
				remove = true;
			} else if (network.hasSpawned(observed)) {
				sync = true;
			} else {
				add = true;
			}
			observed.getEngine().getEventManager().callEvent(new EntitySyncEvent(observed, ((SpoutPhysicsComponent) observed.getPhysics()).getTransformLive(), add, sync, remove));
		}
	}
}

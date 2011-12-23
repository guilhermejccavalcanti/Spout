package org.getspout.unchecked.server.io.entity.monsters;

import org.getspout.api.util.nbt.CompoundTag;
import org.getspout.unchecked.server.SpoutServer;
import org.getspout.unchecked.server.SpoutWorld;
import org.getspout.unchecked.server.entity.monsters.SpoutCreeper;

public class CreeperStore extends MonsterStore<SpoutCreeper> {
	public CreeperStore() {
		super(SpoutCreeper.class, "Creeper");
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public CreeperStore(Class clazz, String id) {
		super(clazz, id);
	}

	@Override
	public SpoutCreeper load(SpoutServer server, SpoutWorld world, CompoundTag compound) {

		SpoutCreeper entity = new SpoutCreeper(server, world);

		super.load(entity, compound);

		return entity;
	}
}
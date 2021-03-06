package net.imprex.orebfuscator.cache;

import java.util.HashSet;
import java.util.Set;

import net.imprex.orebfuscator.util.BlockCoords;

public class ChunkCacheEntry {

	private final byte[] hash;
	private final byte[] data;

	private final Set<BlockCoords> proximityBlocks;
	private final Set<BlockCoords> removedTileEntities;

	public ChunkCacheEntry(byte[] hash, byte[] data) {
		this(hash, data, new HashSet<>(), new HashSet<>());
	}

	public ChunkCacheEntry(byte[] hash, byte[] data, Set<BlockCoords> proximityBlocks,
			Set<BlockCoords> removedTileEntities) {
		this.hash = hash;
		this.data = data;
		this.proximityBlocks = proximityBlocks;
		this.removedTileEntities = removedTileEntities;
	}

	public byte[] getHash() {
		return this.hash;
	}

	public byte[] getData() {
		return this.data;
	}

	public Set<BlockCoords> getProximityBlocks() {
		return this.proximityBlocks;
	}

	public Set<BlockCoords> getRemovedTileEntities() {
		return this.removedTileEntities;
	}
}

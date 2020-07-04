package net.imprex.orebfuscator.nms.v1_9_R2;

import java.util.BitSet;
import java.util.Iterator;
import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_9_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_9_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_9_R2.util.CraftMagicNumbers;
import org.bukkit.entity.Player;

import net.imprex.orebfuscator.config.CacheConfig;
import net.imprex.orebfuscator.config.Config;
import net.imprex.orebfuscator.nms.AbstractBlockState;
import net.imprex.orebfuscator.nms.AbstractNmsManager;
import net.imprex.orebfuscator.nms.AbstractRegionFileCache;
import net.imprex.orebfuscator.util.BlockCoords;
import net.minecraft.server.v1_9_R2.Block;
import net.minecraft.server.v1_9_R2.BlockPosition;
import net.minecraft.server.v1_9_R2.Chunk;
import net.minecraft.server.v1_9_R2.ChunkProviderServer;
import net.minecraft.server.v1_9_R2.EntityPlayer;
import net.minecraft.server.v1_9_R2.IBlockData;
import net.minecraft.server.v1_9_R2.MathHelper;
import net.minecraft.server.v1_9_R2.MinecraftKey;
import net.minecraft.server.v1_9_R2.Packet;
import net.minecraft.server.v1_9_R2.PacketPlayOutBlockChange;
import net.minecraft.server.v1_9_R2.TileEntity;
import net.minecraft.server.v1_9_R2.WorldServer;

public class NmsManager extends AbstractNmsManager {

	private static WorldServer world(World world) {
		return ((CraftWorld) world).getHandle();
	}

	private static EntityPlayer player(Player player) {
		return ((CraftPlayer) player).getHandle();
	}

	private static boolean isChunkLoaded(WorldServer world, int chunkX, int chunkZ) {
		return world.getChunkProviderServer().isLoaded(chunkX, chunkZ);
	}

	private static IBlockData getBlockData(World world, int x, int y, int z, boolean loadChunk) {
		WorldServer worldServer = world(world);
		ChunkProviderServer chunkProviderServer = worldServer.getChunkProviderServer();

		if (isChunkLoaded(worldServer, x >> 4, z >> 4) || loadChunk) {
			// will load chunk if not loaded already
			Chunk chunk = chunkProviderServer.getOrLoadChunkAt(x >> 4, z >> 4);
			return chunk != null ? chunk.getBlockData(new BlockPosition(x, y, z)) : null;
		}
		return null;
	}

	static int getBlockId(IBlockData blockData) {
		if (blockData == null) {
			return 0;
		} else {
			int id = Block.REGISTRY_ID.getId(blockData);
			return id == -1 ? 0 : id;
		}
	}

	private final int blockIdCaveAir;
	private final BitSet blockAir;

	public NmsManager(Config config) {
		super(config);

		for (Iterator<IBlockData> iterator = Block.REGISTRY_ID.iterator(); iterator.hasNext();) {
			IBlockData blockData = iterator.next();
			Material material = CraftMagicNumbers.getMaterial(blockData.getBlock());
			this.registerMaterialId(material, getBlockId(blockData));
		}

		this.blockIdCaveAir = this.getMaterialIds(Material.AIR).iterator().next();
		this.blockAir = this.materialsToBitSet(Material.AIR);
	}

	@Override
	protected AbstractRegionFileCache<?> createRegionFileCache(CacheConfig cacheConfig) {
		return new RegionFileCache(cacheConfig);
	}

	@Override
	public int getBitsPerBlock() {
		return MathHelper.d(Block.REGISTRY_ID.a());
	}

	@Override
	public int getMaterialSize() {
		return Block.REGISTRY_ID.a();
	}

	@Override
	public Optional<Material> getMaterialByName(String name) {
		MinecraftKey minecraftKey = new MinecraftKey(name);
		if (Block.REGISTRY.d(minecraftKey)) {
			return Optional.ofNullable(CraftMagicNumbers.getMaterial(Block.REGISTRY.get(minecraftKey)));
		}
		return Optional.empty();
	}

	@Override
	public int getCaveAirBlockId() {
		return this.blockIdCaveAir;
	}

	@Override
	public boolean isHoe(Material material) {
		switch (material) {
		case WOOD_HOE:
		case STONE_HOE:
		case IRON_HOE:
		case GOLD_HOE:
		case DIAMOND_HOE:
			return true;

		default:
			return false;
		}
	}

	@Override
	public boolean isAir(int blockId) {
		return this.blockAir.get(blockId);
	}

	@Override
	public boolean isTileEntity(int blockId) {
		return Block.getByCombinedId(blockId).getBlock().isTileEntity();
	}

	@Override
	public boolean canApplyPhysics(Material material) {
		switch (material) {
		case AIR:
		case FIRE:
		case WATER:
		case LAVA:
			return true;

		default:
			return false;
		}
	}

	@Override
	public void updateBlockTileEntity(Player player, BlockCoords blockCoord) {
		EntityPlayer entityPlayer = player(player);
		net.minecraft.server.v1_9_R2.World world = entityPlayer.getWorld();

		TileEntity tileEntity = world.getTileEntity(new BlockPosition(blockCoord.x, blockCoord.y, blockCoord.z));
		if (tileEntity == null) {
			return;
		}

		Packet<?> packet = tileEntity.getUpdatePacket();
		if (packet != null) {
			entityPlayer.playerConnection.sendPacket(packet);
		}
	}

	@Override
	public int getBlockLightLevel(World world, int x, int y, int z) {
		return world(world).getLightLevel(new BlockPosition(x, y, z));
	}

	@Override
	public AbstractBlockState<?> getBlockState(World world, int x, int y, int z) {
		IBlockData blockData = getBlockData(world, x, y, z, false);
		return blockData != null ? new BlockState(x, y, z, world, blockData) : null;
	}

	@Override
	public int loadChunkAndGetBlockId(World world, int x, int y, int z) {
		IBlockData blockData = getBlockData(world, x, y, z, true);
		return blockData != null ? getBlockId(blockData) : -1;
	}

	@Override
	public boolean sendBlockChange(Player player, BlockCoords blockCoord) {
		WorldServer world = world(player.getWorld());
		if (!isChunkLoaded(world, blockCoord.x >> 4, blockCoord.z >> 4)) {
			return false;
		}

		BlockPosition position = new BlockPosition(blockCoord.x, blockCoord.y, blockCoord.z);
		PacketPlayOutBlockChange packet = new PacketPlayOutBlockChange(world, position);
		player(player).playerConnection.sendPacket(packet);

		return true;
	}
}

package butterdungeons;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.logging.Level;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.item.ItemDoor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.world.World;
import cpw.mods.fml.common.FMLLog;
import flammpfeil.oneusespawner.TileEntityOneUseMobSpawner;

public class DungeonMap {

	public char[][][] map;

	void loadMap(File cfg_file) throws FileNotFoundException, IOException {
		File file = new File(cfg_file.getParent(), cfg_file.getName().replace(".cfg", ".map"));
		FileReader fr = new FileReader(file);
		BufferedReader br = new BufferedReader(fr);
		String s = "";
		int x = 0;
		int y = 0;
		int z = 0;
		int z_tmp = 0;
		Queue<String> q = new LinkedList();
		while(true) {
			s = br.readLine();
			if(s == null) {
				break;
			}
			q.add(s);
			x = Math.max(x, s.length());
			if(s.startsWith("-")) {
				y++;
				z = Math.max(z, z_tmp);
				z_tmp = 0;
			}
			else {
				z_tmp++;
			}
		}
		z = Math.max(z, z_tmp);
		map = new char[x][y][z];
		String line = "";
		for(int j = 0; j < y; ++j) {
			for(int k = 0; k < z; ++k) {
				line = q.peek();
				if(line == null || line.startsWith("-")) {
					if(k == 0) {
						// skip to next level
						if(line != null) {
							q.remove();
						}
					}
					else {
						for(int i = 0; i < x; ++i) {
							map[i][j][k] = ' ';
						}
						continue;
					}
				}

				if(line == null) {
					line = "";
				}
				else {
					line = q.remove();
				}
				for(int i = 0; i < x; ++i) {
					if(i < line.length()) {
						map[i][j][k] = line.charAt(i);
					}
					else {
						map[i][j][k] = ' ';
					}
				}
			}
		}
		br.close();
		fr.close();
	}

	public void buildDungeon(World world, Random random, int x, int y, int z, DungeonConfig cfg) {
		int xd = map.length;
		int yd = map[0].length;
		int zd = map[0][0].length;

		boolean[][][] r = new boolean[xd][yd][zd];
		for(int j = 0 ; j < yd; ++j) {
			for(int i = 0; i < xd; ++i) {
				for(int k = 0; k < zd; ++k) {
					char c = map[i][yd - j - 1][k];
					boolean b = true;
					try {
						b = setBlock(cfg.settings, world, random, x + i, y + j, z + k, c);
					}
					catch(Exception e) {
						FMLLog.log(Level.WARNING, e, "ButterDungeons setBlock exception %c", c);
					}
					r[i][j][k] = b;
				}
			}
		}

		// retry
		for(int j = 0 ; j < yd; ++j) {
			for(int i = 0; i < xd; ++i) {
				for(int k = 0; k < zd; ++k) {
					if(r[i][j][k] == false) {
						char c = map[i][yd - j - 1][k];
						boolean b = true;
						try {
							b = setBlock(cfg.settings, world, random, x + i, y + j, z + k, c);
						}
						catch(Exception e) {
							FMLLog.log(Level.WARNING, e, "ButterDungeons setBlock exception %c", c);
						}
					}
				}
			}
		}
		FMLLog.log(Level.INFO, "ButterDungeons generateDungeon %s in %d, %d, %d", cfg.getName(), x, y, z);
	}

	public boolean setBlock(DungeonSettings settings, World world, Random random, int i, int j, int k, char c) {

		if(c == ' ') {
			return world.setBlock(i, j, k, 0);
		}

		if(c == '_') {
			return true;
		}

		if(settings.mobMap.containsKey(c)) {
			FMLLog.log(Level.WARNING, "ButterDungeons %c as Mob not implemented in this version", c);
			return false;
		}
		if(settings.blockIdMap.containsKey(c) == false) {
			FMLLog.log(Level.WARNING, "ButterDungeons %c is unknown character", c);
			return world.setBlock(i, j, k, 0);
		}
		int blockId = settings.blockIdMap.get(c);
		Block block = Block.blocksList[blockId];
		if(block == null) {
			FMLLog.log(Level.WARNING, "ButterDungeons %c blockID is %d, block not found", c, blockId);
			return false;
		}/*
		if(block instanceof BlockBed) {
			int west = world.getBlockId(i - 1, j, k);
			int west_meta = world.getBlockMetadata(i - 1, j, k);
			int north = world.getBlockId(i, j, k - 1);
			int north_meta = world.getBlockMetadata(i, j, k - 1);
			if(west == Block.bed.blockID && (west_meta & 0x8) == 1) {
				world.setBlockMetadata(i - 1, j, k, 9);
				world.setBlockAndMetadata(i, j, k, Block.bed.blockID, 0);
			}else if(north == Block.bed.blockID && (north_meta & 0x8) == 1) {
				world.setBlockMetadata(i, j, k - 1, 10);
				world.setBlockAndMetadata(i, j, k, Block.bed.blockID, 2);
			}
		}*/
		if(block instanceof BlockDoor) {
			if(settings.blockMetadataMap.containsKey(c)) {
				int meta = settings.blockMetadataMap.get(c);
				if(meta < 8) {
					world.setBlock(i, j + 1, k, 0);
					ItemDoor.placeDoorBlock(world, i, j, k, meta, block);
					return true;
				}
				else {
					return true;
				}
			}
		}
		if(block.canPlaceBlockAt(world, i, j, k) == false) {
			return false;
		}
		if(settings.blockMetadataMap.containsKey(c)) {
			if(world.setBlockAndMetadata(i, j, k, blockId, settings.blockMetadataMap.get(c)) == false) {
				return false;
			}
		}
		else {
			if(world.setBlock(i, j, k, blockId) == false) {
				return false;
			}
		}

		if(blockId == Block.chest.blockID) {
			if(settings.chestMap.containsKey(c)) {
				String s = settings.chestMap.get(c);
				List<ItemStack> itemStacks = settings.parseChestItemsString(s);
				TileEntityChest tile = (TileEntityChest) world.getBlockTileEntity(i, j, k);
				if (tile != null) {
					int n = 0;
					for(ItemStack itemStack : itemStacks) {
						for(; n < tile.getSizeInventory(); ++n) {
							if(tile.getStackInSlot(n) == null) {
								tile.setInventorySlotContents(n, itemStack);
								break;
							}
						}
					}
					return true;
				}
			}
			else if(settings.randomChestMap.containsKey(c)) {
				String s = settings.randomChestMap.get(c);
				List<ItemStack> itemStacks = settings.parseChestItemsString(s);
				TileEntityChest tile = (TileEntityChest) world.getBlockTileEntity(i, j, k);
				if (tile != null) {
					int n = 0;
					for(ItemStack itemStack : itemStacks) {
						if(random.nextInt(7) == 1) {
							for(; n < tile.getSizeInventory(); ++n) {
								if(tile.getStackInSlot(n) == null) {
									tile.setInventorySlotContents(n, itemStack);
									break;
								}
							}
						}
					}
					return true;
				}
			}
		}
		if(blockId == Config.blockOneUseSpawnerId) {
			if(world.isRemote) {
				return true;
			}
			TileEntity tile = world.getBlockTileEntity(i, j, k);
			if(tile != null) {
				String mobname = parseMobsString(settings.mobSpawnerMap.get(c), random);
				if(mobname != null) {
					Entity e = EntityList.createEntityByName(mobname, world);
					if(e == null) {
						FMLLog.log(Level.WARNING, "ButterDungeons %c is OneUseSpawner, but related Mob %s not found", c, settings.mobSpawnerMap.get(c));
						return false;
					}
					TileEntityOneUseMobSpawner tileSpawner = (TileEntityOneUseMobSpawner) tile;
					NBTTagCompound tag = new NBTTagCompound();
					NBTTagCompound itemTag = new NBTTagCompound();
					e.addEntityID(tag);
					itemTag.setCompoundTag("MobNBT", tag);
					tileSpawner.mobNBT = tag;
					tileSpawner.spawnCount = 1;
					return true;
				}
				FMLLog.log(Level.WARNING, "ButterDungeons %c is OneUseSpawner, but related Mob %s not found", c, settings.mobSpawnerMap.get(c));
				return false;

			}
			/*
			if(world.setBlock(i, j, k, blockId) == false) {
				return false;
			}
			Block b = Block.blocksList[blockId];
			if(b == null) {
				return false;
			}
			if(b.getClass().getName() == "flammpfeil.oneusespawner.BlockOneUseMobSpawner") {

			}*/
			return true;
		}
		if(blockId == Block.mobSpawner.blockID) {
			if(settings.mobSpawnerMap.containsKey(c)) {
				TileEntityMobSpawner tile = (TileEntityMobSpawner) world.getBlockTileEntity(i, j, k);
				if (tile != null) {
					String mobname = parseMobsString(settings.mobSpawnerMap.get(c), random);
					if(mobname != null) {
						tile.setMobID(mobname);
						return true;
					}
					FMLLog.log(Level.WARNING, "ButterDungeons %c is Spawner, but related Mob %s not found", c, settings.mobSpawnerMap.get(c));
					return false;
				}
			}
			FMLLog.log(Level.WARNING, "ButterDungeons %c is Spawner, but related Mob not found", c);
			return false;
		}
		return true;
	}

	String parseMobsString(String par1, Random random) {

		List<String> names = new ArrayList<String>();
		for(String token : par1.split(",")) {
			if(token != null && token.trim().isEmpty() == false) {
				String name = token.trim();
				if(name.compareToIgnoreCase("MOB") == 0) {
					names.add(parseMobsString(Config.random_mob, random));
				}
				else if(name.compareToIgnoreCase("BOSS") == 0) {
					names.add(parseMobsString(Config.random_boss, random));
				}
				else {
					Map m = EntityList.stringToClassMapping;
					Iterator it = m.keySet().iterator();
					while (it.hasNext()) {
						String mobname = (String) it.next();
						if (mobname.compareToIgnoreCase(name) == 0) {
							names.add(name);
							break;
						}
					}
				}
			}
		}

		if(names.size() == 0) {
			return null;
		}
		return names.get(random.nextInt(names.size()));
	}
}

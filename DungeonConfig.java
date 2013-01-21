package butterdungeons;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Level;

import cpw.mods.fml.common.FMLLog;
import flammpfeil.oneusespawner.TileEntityOneUseMobSpawner;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockDoor;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDoor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.Configuration;

public class DungeonConfig {

	public Set<String> allow_biomes = new LinkedHashSet();
	public int floor_level_min = 60;
	public int floor_level_max = 70;
	public int replace_block_id = 0;
	public int replace_block_percentage = 90;
	public int generate_frequency = 100;
	public int generate_num_limit = -1;
	public Set<Integer> allow_dimentions = new LinkedHashSet();

	public char[][][] map;
	public Map<Character, Integer> blockIdMap = new LinkedHashMap();
	public Map<Character, Integer> blockMetadataMap = new LinkedHashMap();
	public Map<Character, String> mobSpawnerMap = new LinkedHashMap();
	public Map<Character, String> chestMap = new LinkedHashMap();
	public Map<Character, String> randomChestMap = new LinkedHashMap();
	public Map<Character, String> mobMap = new LinkedHashMap();

	public String dungeon_name;

	public int skip = 0;
	public int num = 0;
	public boolean generating = false;

	public void load(File cfg_file) throws IOException {

		// load cfg file
		Configuration cfg = new Configuration(cfg_file);
		cfg.load();

		floor_level_min = cfg.get(Configuration.CATEGORY_GENERAL, "floor_level_min", floor_level_min).getInt();
		floor_level_max = cfg.get(Configuration.CATEGORY_GENERAL, "floor_level_max", floor_level_max).getInt();
		replace_block_id = cfg.get(Configuration.CATEGORY_GENERAL, "replace_block_id", replace_block_id).getInt();
		replace_block_percentage = cfg.get(Configuration.CATEGORY_GENERAL, "replace_block_percentage", replace_block_percentage).getInt();
		generate_frequency = cfg.get(Configuration.CATEGORY_GENERAL, "generate_frequency", generate_frequency).getInt();
		generate_num_limit = cfg.get(Configuration.CATEGORY_GENERAL, "generate_num_limit", generate_num_limit).getInt();
		dungeon_name = cfg.get(Configuration.CATEGORY_GENERAL, "dungeon_name", "").value;
		String cfg_allow_dimentions = cfg.get(Configuration.CATEGORY_GENERAL, "allow_dimentions", "0").value;
		for(String token : cfg_allow_dimentions.split(",")) {
			try {
				int dim = Integer.parseInt(token.trim());
				allow_dimentions.add(dim);
			}
			catch(NumberFormatException e) {
				FMLLog.log(Level.WARNING, e, "ButterDungeons allow_dimentions invalid: %s", token);
			}
		}

		String cfg_allow_biomes = cfg.get(Configuration.CATEGORY_GENERAL, "allow_biomes", "all").value;
		for(String token : cfg_allow_biomes.split(",")) {
			String biome = token.trim();
			if(biome.isEmpty()) {
				continue;
			}
			allow_biomes.add(biome);
		}

		// load setting file
		{
			File file = new File(cfg_file.getParent(), cfg_file.getName().replace(".cfg", ".settings"));
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			String line = "";
			int linenum = 0;
			while(true) {
				line = br.readLine();
				linenum++;
				if(line == null) {
					break;
				}
				if(line.contains("=")) {
					String[] tmp =line.split("=");
					if(tmp.length != 2) {
						continue;
					}
					String key = tmp[0].trim();
					if(key.length() == 0) {
						continue;
					}
					char c = key.charAt(0);
					String value = tmp[1].trim();
					if(value.length() == 0) {
						continue;
					}

					// c = CHEST stone * 1, ingotIron * 1, cloth:4 * 1, 14 * 2, potion+
					if(value.trim().startsWith("CHEST")) {
						chestMap.put(c, value.replaceFirst("CHEST", "").trim());
						blockIdMap.put(c, Block.chest.blockID);
					}
					// c = RANDOMCHEST stone * 1, ingotIron * 1, cloth:4 * 1, 14 * 2, potion+
					else if(value.trim().startsWith("RANDOMCHEST")) {
						randomChestMap.put(c, value.replaceFirst("RANDOMCHEST", "").trim());
						blockIdMap.put(c, Block.chest.blockID);
					}
					// c = SPAWNER pig
					else if(value.trim().startsWith("SPAWNER")) {
						String[] mobnames = value.replaceFirst("SPAWNER", "").trim().split(" ");
						Random rand = new Random();
						mobSpawnerMap.put(c, mobnames[rand.nextInt(mobnames.length)]);
						blockIdMap.put(c, Block.mobSpawner.blockID);
					}
					// c = ONEUSESPAWNER pig
					else if(value.trim().startsWith("ONEUSESPAWNER")) {
						String[] mobnames = value.replaceFirst("ONEUSESPAWNER", "").trim().split(" ");
						Random rand = new Random();
						mobSpawnerMap.put(c, mobnames[rand.nextInt(mobnames.length)]);
						blockIdMap.put(c, Config.blockOneUseSpawnerId);
					}
					// c = cloth:4
					else if(value.contains(":")) {
						String[] a = value.split(":");
						if(a.length == 2) {
							int id = getBlockId(a[0]);
							try {
								if(id != -1) {
									int meta = Integer.parseInt(a[1].trim());
									blockIdMap.put(c, id);
									blockMetadataMap.put(c, meta);
								}
								else {
									FMLLog.log(Level.WARNING, "ButterDungeons %c format error, %s", c, a[0]);
								}
							}
							catch(NumberFormatException e) {
								FMLLog.log(Level.WARNING, e, "ButterDungeons setting invalid: %d line", linenum);
							}
						}
					}
					// c = 4 or c = stone
					else {
						int id = getBlockId(value);
						if(id != -1) {
							blockIdMap.put(c, id);
						}
						else {
							FMLLog.log(Level.WARNING, "ButterDungeons %c format error, %s", c, value);
						}
					}
				}
			}
			br.close();
			fr.close();
		}

		// load map file
		{
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
	}

	public boolean setBlock(World world, Random random, int i, int j, int k, char c) {

		if(c == ' ') {
			return world.setBlock(i, j, k, 0);
		}

		if(c == '_') {
			return true;
		}

		if(mobMap.containsKey(c)) {
			FMLLog.log(Level.WARNING, "ButterDungeons %c as Mob not implemented in this version", c);
			return false;
		}
		if(blockIdMap.containsKey(c) == false) {
			FMLLog.log(Level.WARNING, "ButterDungeons %c is unknown character", c);
			return world.setBlock(i, j, k, 0);
		}
		int blockId = blockIdMap.get(c);
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
			if(blockMetadataMap.containsKey(c)) {
				int meta = blockMetadataMap.get(c);
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
		if(blockMetadataMap.containsKey(c)) {
			if(world.setBlockAndMetadata(i, j, k, blockId, blockMetadataMap.get(c)) == false) {
				return false;
			}
		}
		else {
			if(world.setBlock(i, j, k, blockId) == false) {
				return false;
			}
		}

		if(blockId == Block.chest.blockID) {
			if(chestMap.containsKey(c)) {
				String s = chestMap.get(c);
				List<ItemStack> itemStacks = parseChestItemsString(s);
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
			else if(randomChestMap.containsKey(c)) {
				String s = randomChestMap.get(c);
				List<ItemStack> itemStacks = parseChestItemsString(s);
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

				Map m = EntityList.stringToClassMapping;
				Iterator it = m.keySet().iterator();
				String name = mobSpawnerMap.get(c);
				if(name.compareToIgnoreCase("MOB") == 0) {
					String[] mobnames = Config.random_mob.split(",");
					name = mobnames[random.nextInt(mobnames.length)].trim();
				}
				else if(name.compareToIgnoreCase("BOSS") == 0) {
					String[] mobnames = Config.random_boss.split(",");
					name = mobnames[random.nextInt(mobnames.length)].trim();
				}
				while (it.hasNext()) {
					String mobname = (String) it.next();
					if (mobname.compareToIgnoreCase(name) == 0) {
						Entity e = EntityList.createEntityByName(mobname, world);
						if(e == null) {
							continue;
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
				}
				FMLLog.log(Level.WARNING, "ButterDungeons %c is OneUseSpawner, but related Mob %s not found", c, mobSpawnerMap.get(c));
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
			if(mobSpawnerMap.containsKey(c)) {
				TileEntityMobSpawner tile = (TileEntityMobSpawner) world.getBlockTileEntity(i, j, k);
				if (tile != null) {
					Map m = EntityList.stringToClassMapping;
					Iterator it = m.keySet().iterator();
					String name = mobSpawnerMap.get(c);
					if(name.compareToIgnoreCase("MOB") == 0) {
						String[] mobnames = Config.random_mob.split(",");
						name = mobnames[random.nextInt(mobnames.length)].trim();
					}
					else if(name.compareToIgnoreCase("BOSS") == 0) {
						String[] mobnames = Config.random_boss.split(",");
						name = mobnames[random.nextInt(mobnames.length)].trim();
					}
					while (it.hasNext()) {
						String mobname = (String) it.next();
						if (mobname.compareToIgnoreCase(name) == 0) {
							tile.setMobID(mobname);
							return true;
						}
					}
					FMLLog.log(Level.WARNING, "ButterDungeons %c is Spawner, but related Mob %s not found", c, mobSpawnerMap.get(c));
					return false;
				}
			}
			FMLLog.log(Level.WARNING, "ButterDungeons %c is Spawner, but related Mob not found", c);
			return false;
		}
		return true;
	}

	private int getBlockId(String name) {

		int id = -1;
		try {
			int i = Integer.parseInt(name.trim());
			Block b = Block.blocksList[i];
			if(b != null) {
				id = b.blockID;
			}
		}
		catch(NumberFormatException e) {
			for(Block b : Block.blocksList) {
				if(b == null) {
					continue;
				}
				if(b.getBlockName() == null) {
					continue;
				}
				if(name.compareToIgnoreCase(b.getBlockName()) == 0 || name.compareToIgnoreCase(b.getBlockName().replaceFirst("tile.", "")) == 0) {
					id = b.blockID;
					break;
				}
			}
		}

		return id;
	}

	public String getName() {
		return dungeon_name;
	}

	// sword+efficiency*1, stone*1, cloth:4*2
	private List<ItemStack> parseChestItemsString(String str) {
		List<ItemStack> itemStacks = new LinkedList();

		for(String token : str.split(",")) {
			if(token.trim().isEmpty()) {
				continue;
			}

			if(token.trim().compareToIgnoreCase("RANDOM_TREASURE") == 0) {
				List<ItemStack> treasures = parseChestItemsString(token);
				for(ItemStack item : treasures) {
					itemStacks.add(item);
				}
				continue;
			}
			String rest = "";

			// get stack size (*3)
			int stackSize = 1;
			{
				String[] a = token.split("\\*");
				if(a.length > 2) {
					FMLLog.log(Level.WARNING, "ButterDungeons %s format error", token);
					continue;
				}
				try {
					if(a.length == 2) {
						stackSize = Integer.parseInt(a[1].trim());
					}
				}
				catch(NumberFormatException e) {
					FMLLog.log(Level.WARNING, e, "ButterDungeons %s format error", token);
					continue;
				}
				rest = a[0].trim();
			}

			// get enchants (+efficiency^3+silktouch^2)
			Map<Enchantment, Integer> enchants = new HashMap();
			{
				String[] a = rest.split("\\+");
				rest = a[0].trim();
				for(int i = 1; i < a.length; ++i) {
					String[] b = a[i].trim().split("\\^");
					int level = 1;
					if(b.length > 1) {
						try {
							level = Integer.parseInt(b[1].trim());
						}
						catch(NumberFormatException e) {
							FMLLog.log(Level.WARNING, e, "ButterDungeons %s format error", token);
							continue;
						}
					}
					for(Enchantment e : Enchantment.enchantmentsList) {
						if(e == null || e.getName() == null) {
							continue;
						}
						if(e.getName().replaceFirst("enchantment.", "").compareToIgnoreCase(b[0].trim()) == 0) {
							enchants.put(e, level);
						}
					}
				}
			}

			// get metadata (:5)
			int metadata = 0;
			{
				String[] a = rest.split(":");
				rest = a[0].trim();
				if(a.length > 1) {
					try {
						metadata = Integer.parseInt(a[1].trim());
					}
					catch(NumberFormatException e) {
						FMLLog.log(Level.WARNING, e, "ButterDungeons %s format error", token);
						continue;
					}
				}
			}

			int id = -1;
			try {
				id = Integer.parseInt(rest.trim());
			}
			catch(NumberFormatException e) {
				for(Block b : Block.blocksList) {
					if(b == null || b.getBlockName() == null) {
						continue;
					}
					if(b.getBlockName().replaceFirst("tile.", "").compareToIgnoreCase(rest.trim()) == 0) {
						id = b.blockID;
						break;
					}
				}
				if(id == -1) {
					for(Item item : Item.itemsList) {
						if(item == null || item.getItemName() == null) {
							continue;
						}
						if(item.getItemName().replaceFirst("item.", "").compareToIgnoreCase(rest.trim()) == 0) {
							id = item.shiftedIndex;
							break;
						}
					}
				}
			}

			if(id == -1) {
				FMLLog.log(Level.WARNING, "ButterDungeons %s format error", token);
				continue;
			}
			ItemStack itemStack = new ItemStack(id, stackSize, metadata);
			itemStacks.add(itemStack);
			for(Map.Entry<Enchantment, Integer> e : enchants.entrySet()) {
				itemStack.addEnchantment(e.getKey(), e.getValue());
			}
		}
		return itemStacks;
	}

	public void parse(File src) throws IOException {

		File f1 = new File(Minecraft.getMinecraftDir(), Config.better_dungeons_path);
		File f2 = new File(Minecraft.getMinecraftDir(), Config.root_path);
		String t = src.getAbsolutePath().replace(f1.getAbsolutePath(), f2.getAbsolutePath());
		{
			File file = new File( t + ".settings");
			File parent = new File(file.getParent());
			if(file.exists()) {
				return;
			}
		}

		char map[][][] = null;
		Map<Integer, Character> blockMap = new LinkedHashMap();
		{
			FileReader fr = new FileReader(src);
			BufferedReader br = new BufferedReader(fr);
			String line;
			line = br.readLine();

			int x = 0, y = 0, z = 0;
			line = br.readLine();
			x = Integer.parseInt(line.trim());
			line = br.readLine();
			y = Integer.parseInt(line.trim());
			line = br.readLine();
			z = Integer.parseInt(line.trim());

			map = new char[y][z][x];
			blockMap.put(0, ' ');
			blockMap.put(-100, '_');
			char c = 'A';
			char end_char = '`';

			Set<Integer> set = new LinkedHashSet();
			Map<Integer, Integer> freq = new TreeMap();

			for(int i = 0; i < x; ++i) {
				for(int j = 0; j < y; ++j) {
					line = br.readLine();
					String[] tokens = line.split(" ");
					for(int k = 0; k < Math.min(tokens.length, z); ++k) {
						if(tokens[k] == null) {
							System.out.println(line);
						}
						if(tokens[k].isEmpty()) {
							continue;
						}
						float n = Float.parseFloat(tokens[k]);
						int a = (int) (n * 100.0F);
						// mob
						if(30000 <= a && a < 30100) {
							a = -200;
						}
						// treasure
						if(30100 <= a && a < 30200) {
							a = Block.chest.blockID * 100;
						}
						// boss
						if(30200 <= a && a < 30300) {
							a = -300;
						}
						if(Block.chest.blockID * 100 <= a && a < (Block.chest.blockID + 1) * 100) {
							a = Block.chest.blockID * 100;
						}
						if(Block.torchWood.blockID * 100 <= a && a < (Block.torchWood.blockID + 1) * 100) {
							a = Block.torchWood.blockID * 100;
						}
						if(Block.bed.blockID * 100 <= a && a < (Block.bed.blockID + 1) * 100) {
							a = Block.bed.blockID * 100;
						}
						if(Block.cauldron.blockID * 100 <= a && a < (Block.cauldron.blockID + 1) * 100) {
							a = Block.cauldron.blockID * 100;
						}
						if(Block.stoneOvenIdle.blockID * 100 <= a && a < (Block.stoneOvenIdle.blockID + 1) * 100) {
							a = Block.stoneOvenIdle.blockID * 100;
						}
						if(Block.signPost.blockID * 100 <= a && a < (Block.signPost.blockID + 1) * 100) {
							//a = 0;
						}
						if(Block.doorWood.blockID * 100 + 8 <= a && a < (Block.doorWood.blockID + 1) * 100) {
							a = -100;
						}
						if(Block.doorSteel.blockID * 100 + 8 <= a && a < (Block.doorSteel.blockID + 1) * 100) {
							a = -100;
						}
						set.add(a);
						if(freq.containsKey(a)) {
							freq.put(a, freq.get(a) + 1);
						}
						else {
							freq.put(a, 1);
						}
						if(blockMap.containsKey(a) == false) {
							if(c == end_char) {
								map[j][k][i] = ' ';
								continue;
							}
							blockMap.put(a, c);
							map[j][k][i] = c;
							if(c == 'Z') {
								c = 'a';
							}
							else if(c == 'z') {
								c = '0';
							}
							else if(c == '@') {
								c = '!';
							}
							else if(c == '/') {
								c = '[';
							}
							else {
								++c;
							}
							if(c == ' ' || c == '_' || c == '-') {
								++c;
							}
						}
						else {
							map[j][k][i] = blockMap.get(a);
						}
					}
				}
			}
			br.close();
			fr.close();
			System.out.print(src.getAbsolutePath());
			System.out.print(" block type num = ");
			System.out.println(set.size());
			if(c == end_char) {
				for(Map.Entry<Integer, Integer> e : freq.entrySet()) {
					int n = e.getKey();
					if(n <= 0) {
						continue;
					}
					int id = n / 100;
					int meta = n % 100;
					Block b = Block.blocksList[id];
					if(b == null) {
						System.out.print(id);
					}
					else {
						if(b.getBlockName() != null && b.getBlockName().isEmpty() == false) {
							System.out.print(b.getBlockName());
						}
						else {
							System.out.print(id);
						}
					}
					System.out.print(":");
					System.out.print(meta);
					System.out.print(" = ");
					System.out.println(e.getValue());
				}
				FMLLog.log(Level.WARNING, "ButterDungeon parse map too many blocks(%d) in " + src.getAbsolutePath(), set.size());
				return;
			}
		}
		{
			File file = new File( t + ".settings");
			File parent = new File(file.getParent());
			parent.mkdirs();

			FileWriter fw = new FileWriter(file);
			BufferedWriter bw = new BufferedWriter(fw);

			for(Map.Entry<Integer, Character> e : blockMap.entrySet()) {
				int a = e.getKey();
				int id = a / 100;
				if(id == 0 || id == -1) {
					continue;
				}

				String s = "";
				// mob
				if(id == -2) {
					char cc = e.getValue();
					s += cc;
					s += " = ONEUSESPAWNER MOB";
				}
				// boss
				else if(id == -3) {
					char cc = e.getValue();
					s += cc;
					s += " = ONEUSESPAWNER BOSS";
				}
				else if(id == Block.chest.blockID) {
					char cc = e.getValue();
					s += cc;
					s += " = RANDOMCHEST RANDOM_TREASURE";
				}
				else {
					int metadata = a % 100;
					char cc = e.getValue();
					s += cc;
					s += " = ";

					Block block = Block.blocksList[id];
					if(block != null) {
						String name = block.getBlockName();
						if(name != null && name.isEmpty() == false) {
							s += name.replaceFirst("tile.", "");
						}
						else {
							s += String.valueOf(id);
						}
					}
					else {
						s += String.valueOf(id);
					}
					if(metadata != 0) {
						s += ":";
						s += String.valueOf(metadata);
					}
				}
				s += "\r\n";
				bw.write(s);
			}
			bw.close();
			fw.close();
		}
		{
			File file = new File(t + ".map");
			FileWriter fw = new FileWriter(file);
			BufferedWriter bw = new BufferedWriter(fw);
			for(int y = map.length -1 ; y >= 0; --y) {
				for(int z = 0; z < map[0].length; ++z) {
					String s = new String(map[y][z]);
					bw.write(s + "\r\n");
				}
				bw.write("-\r\n");
			}
			bw.close();
			fw.close();
		}
	}
}

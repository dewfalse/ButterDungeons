package butterdungeons;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.common.FMLLog;

public class DungeonSettings {

	public Map<Character, Integer> blockIdMap = new LinkedHashMap();
	public Map<Character, Integer> blockMetadataMap = new LinkedHashMap();
	public Map<Character, String> mobSpawnerMap = new LinkedHashMap();
	public Map<Character, String> chestMap = new LinkedHashMap();
	public Map<Character, String> randomChestMap = new LinkedHashMap();
	public Map<Character, String> mobMap = new LinkedHashMap();

	void loadSettings(File cfg_file) throws FileNotFoundException, IOException {
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

	// sword+efficiency*1, stone*1, cloth:4*2
	List<ItemStack> parseChestItemsString(String str) {
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

}

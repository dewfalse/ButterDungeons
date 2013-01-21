package butterdungeons;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;

import cpw.mods.fml.common.FMLLog;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.Configuration;

public class Dungeons {

	public static Set<DungeonConfig> dungeons = new LinkedHashSet();

	private static FileFilter getConfigFilter() {
		return new FileFilter() {
			public boolean accept(File file) {
				return file.isFile() && file.getName().endsWith(".cfg");
			}
		};
	}

	private static FileFilter getSubDirFilter() {
		return new FileFilter() {
			public boolean accept(File file) {
				return file.isDirectory();
			}
		};
	}

	public static void init() {

		File better_root = new File(Minecraft.getMinecraftDir(), Config.better_dungeons_path);
		parse_better_dungeons_config(better_root);

		File root = new File(Minecraft.getMinecraftDir(), Config.root_path);

		if(root.isDirectory()) {
			File[] files = root.listFiles(getConfigFilter());
			for(File f : files) {
				loadDungeonConfig(f);
			}
		}

		if(root.isDirectory()) {
			File[] subdirs = root.listFiles(getSubDirFilter());
			for(File d : subdirs) {
				File[] files = d.listFiles(getConfigFilter());
				for(File f : files) {
					loadDungeonConfig(f);
				}
			}
		}
	}

	private static void parse_better_dungeons_config(File root) {
		if(root.isDirectory()) {
			File[] files = root.listFiles();
			for(File f : files) {
				if(f.isDirectory()) {
					parse_better_dungeons_config(f);
				}
				else if(f.isFile()) {
					try {
						DungeonConfig c = new DungeonConfig();
						c.parse(f);
					}
					catch (Exception e) {
					    FMLLog.log(Level.WARNING, e, "ButterDungeon parse map exception in " + f.getAbsolutePath());
					}

				}
			}
		}

	}

	static void loadDungeonConfig(File f) {
		try {
			DungeonConfig c = new DungeonConfig();
			c.load(f);
			dungeons.add(c);
		}
		catch (Exception e) {
		    FMLLog.log(Level.WARNING, e, "ButterDungeon load config exception in " + f.getName());
		}
	}

}

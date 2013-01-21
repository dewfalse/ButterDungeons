package butterdungeons;

import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Level;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.IWorldGenerator;

public class DungeonGenerator implements IWorldGenerator {

	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world,
			IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {


		for(DungeonConfig cfg : Dungeons.dungeons) {
			if(cfg.allow_dimentions.contains(world.provider.dimensionId) == false) {
				continue;
			}
			if(cfg.allow_biomes.contains("all") == false) {
				BiomeGenBase b = world.getBiomeGenForCoords(chunkX << 4, chunkZ << 4);
				if(cfg.allow_biomes.contains(b.biomeName) == false && cfg.allow_biomes.contains(String.valueOf(b.biomeID)) == false ) {
					continue;
				}
			}
			if(cfg.generate_num_limit == -1 || cfg.num < cfg.generate_num_limit) {
				if(cfg.skip++ >= cfg.generate_frequency) {
					int xs = 0, zs = 0;
/*
					int xd = cfg.map.length >> 4;
					int zd = cfg.map[0][0].length >> 4;

					xd++;
					zd++;
					System.out.print("chunk =");
					System.out.print(chunkX);
					System.out.print(", ");
					System.out.println(chunkZ);

					int n = 0;
					boolean exist = false;
					for(int x = -xd; x <= 0; x++) {
						for(int z = -zd; z <= 0; z++) {
							if(chunkProvider.chunkExists(chunkX + x, chunkZ + z)) {
								n++;
							}
						}
					}
					if(n > xd * zd - 1) {
						xs = -xd;
						zs = -zd;
						exist = true;
					}

					n = 0;
					for(int x = -xd; x <= 0; x++) {
						for(int z = 0; z <= zd; z++) {
							if(chunkProvider.chunkExists(chunkX + x, chunkZ + z)) {
								n++;
							}
						}
					}
					if(n > xd * zd - 1) {
						xs = -xd;
						zs = 0;
						exist = true;
					}

					n = 0;
					for(int x = 0; x <= xd; x++) {
						for(int z = -zd; z <= 0; z++) {
							if(chunkProvider.chunkExists(chunkX + x, chunkZ + z)) {
								n++;
							}
						}
					}
					if(n > xd * zd - 1) {
						xs = 0;
						zs = -zd;
						exist = true;
					}

					n = 0;
					for(int x = 0; x <= xd; x++) {
						for(int z = 0; z <= zd; z++) {
							if(chunkProvider.chunkExists(chunkX + x, chunkZ + z)) {
								n++;
							}
						}
					}
					if(n > xd * zd - 1) {
						xs = 0;
						zs = 0;
						exist = true;
					}

					if(exist) {*/
						if(generateDungeon(world, random, (chunkX + xs) << 4, (chunkZ + zs) << 4, cfg)) {
							cfg.skip = - random.nextInt(cfg.generate_frequency / 2);
							cfg.num += 1;
						}
						break;
					//}
				}
			}
		}

	}

	private boolean generateDungeon(World world, Random random, int chunkX, int chunkZ, DungeonConfig cfg) {

		if(cfg.generating) {
			return false;
		}
		cfg.generating = true;

		int xd = cfg.map.length;
		int yd = cfg.map[0].length;
		int zd = cfg.map[0][0].length;

		int xs = random.nextInt(16);
		int zs = random.nextInt(16);

		Vector<Integer> v = new Vector();
		for(int y = cfg.floor_level_min; y < cfg.floor_level_max + yd; ++y) {
			int n = 0;
			for(int x = 0; x < xd; ++x) {
				for(int z = 0; z < zd; ++z) {

					int blockId = world.getBlockId(chunkX + x, y, chunkZ + z);
					Block b = Block.blocksList[blockId];
					if(b != null && 0.0F > b.getBlockHardness(world, chunkX + x, y, chunkZ + z)) {
						// found unbreakable block
						cfg.generating = false;
						return false;
					}
					if(blockId == cfg.replace_block_id) {
						n++;
					}
				}
			}
			v.add(n);
		}
		int n = 0;
		for(int y = 0; y < yd; ++y) {
			n += v.get(y);
		}
		if(n * 100 >= xd * yd * zd *cfg.replace_block_percentage) {
			buildDungeon(world, random, chunkX, cfg.floor_level_min, chunkZ, cfg);
			cfg.generating = false;
			return true;
		}
		for(int y = 1; y < cfg.floor_level_max - cfg.floor_level_min; ++y) {
			n -= v.get(y);
			n += v.get(yd + y);
			if(n * 100 >= xd * yd * zd *cfg.replace_block_percentage) {
				buildDungeon(world, random, chunkX, cfg.floor_level_min + y, chunkZ, cfg);
				cfg.generating = false;
				return true;
			}
		}
		cfg.generating = false;
		return false;
	}

	public void buildDungeon(World world, Random random, int x, int y, int z, DungeonConfig cfg) {
		int xd = cfg.map.length;
		int yd = cfg.map[0].length;
		int zd = cfg.map[0][0].length;

		boolean[][][] r = new boolean[xd][yd][zd];
		for(int j = 0 ; j < yd; ++j) {
			for(int i = 0; i < xd; ++i) {
				for(int k = 0; k < zd; ++k) {
					char c = cfg.map[i][yd - j - 1][k];
					boolean b = true;
					try {
						b = cfg.setBlock(world, random, x + i, y + j, z + k, c);
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
						char c = cfg.map[i][yd - j - 1][k];
						boolean b = true;
						try {
							b = cfg.setBlock(world, random, x + i, y + j, z + k, c);
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

}

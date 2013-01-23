package butterdungeons;

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
					int xs = random.nextInt(16);
					int zs = random.nextInt(16);
					if(generateDungeon(world, random, (chunkX << 4) + xs, (chunkZ << 4) + zs, cfg)) {
						cfg.skip = - random.nextInt(cfg.generate_frequency / 2);
						cfg.num += 1;
					}
					break;
				}
			}
		}

	}

	private boolean generateDungeon(World world, Random random, int chunkX, int chunkZ, DungeonConfig cfg) {

		if(cfg.generating) {
			return false;
		}
		cfg.generating = true;

		int xd = cfg.map.map.length;
		int yd = cfg.map.map[0].length;
		int zd = cfg.map.map[0][0].length;

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
			cfg.map.buildDungeon(world, random, chunkX, cfg.floor_level_min, chunkZ, cfg);
			cfg.generating = false;
			return true;
		}
		for(int y = 1; y < cfg.floor_level_max - cfg.floor_level_min; ++y) {
			n -= v.get(y);
			n += v.get(yd + y);
			if(n * 100 >= xd * yd * zd *cfg.replace_block_percentage) {
				cfg.map.buildDungeon(world, random, chunkX, cfg.floor_level_min + y, chunkZ, cfg);
				cfg.generating = false;
				return true;
			}
		}
		cfg.generating = false;
		return false;
	}

}

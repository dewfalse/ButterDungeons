package butterdungeons;

import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;

public class PopulateChunkEventHook {

	DungeonGenerator generator = new DungeonGenerator();
	@ForgeSubscribe
	public void generateDungeons(PopulateChunkEvent.Post event) {
		generator.generate(event.rand, event.chunkX, event.chunkZ, event.world, event.chunkProvider, event.chunkProvider);
	}
}

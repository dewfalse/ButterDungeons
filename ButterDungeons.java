package butterdungeons;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.Property;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.PostInit;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;

@Mod(modid = "ButterDungeons", name = "ButterDungeons", version = "1.0")
@NetworkMod(clientSideRequired = false, serverSideRequired = true, channels = { "butter" }, packetHandler = PacketHandler.class, connectionHandler = ConnectionHandler.class, versionBounds = "[1.0]")
public class ButterDungeons {
	@SidedProxy(clientSide = "butterdungeons.ClientProxy", serverSide = "butterdungeons.CommonProxy")
	public static CommonProxy proxy;

	@Instance("ButterDungeons")
	public static ButterDungeons instance;

	public static Logger logger = Logger.getLogger("Minecraft");

	public static boolean debug = false;

	//public static final CreativeTabs creativeTab = new CreativeTabButterDungeons("ButterDungeons");


	@Mod.Init
	public void load(FMLInitializationEvent event) {
		//GameRegistry.registerWorldGenerator(new DungeonGenerator());
		NetworkRegistry.instance().registerGuiHandler(instance,  proxy);
		MinecraftForge.EVENT_BUS.register(new PopulateChunkEventHook());
	}

	@PostInit
	public void postInit(FMLPostInitializationEvent event) {
		//Blocks.init();
		Dungeons.init();
	}
	@PreInit
	public void preInit(FMLPreInitializationEvent event) {
		Configuration cfg = new Configuration(event.getSuggestedConfigurationFile());

		try {
			cfg.load();
			Config.root_path = cfg.get(Configuration.CATEGORY_GENERAL, "dungeon_config_root_path", Config.root_path).value;
			Config.better_dungeons_path = cfg.get(Configuration.CATEGORY_GENERAL, "better_dungeons_path", Config.better_dungeons_path).value;
			Config.random_treasure = cfg.get(Configuration.CATEGORY_GENERAL, "random_treasure", Config.random_treasure).value;
			Config.random_mob = cfg.get(Configuration.CATEGORY_GENERAL, "random_mob", Config.random_mob).value;
			Config.random_boss = cfg.get(Configuration.CATEGORY_GENERAL, "random_boss", Config.random_boss).value;
			Config.blockOneUseSpawnerId = cfg.get(Configuration.CATEGORY_BLOCK, "blockOneUseSpawnerId", Config.blockOneUseSpawnerId).getInt();

			cfg.save();

			Property debug = cfg.get(Configuration.CATEGORY_GENERAL, "debug", false);
			this.debug = debug.getBoolean(false);
		} catch (Exception e) {
			FMLLog.log(Level.SEVERE, e, "ButterDungeons load config exception");
		} finally {
			cfg.save();
		}
	}

		@Mod.ServerStarting
		public void serverStarting(FMLServerStartingEvent event){
			event.registerServerCommand(new CommandCut());
			event.registerServerCommand(new CommandPaste());
		}

}

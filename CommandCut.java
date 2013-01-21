package butterdungeons;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import cpw.mods.fml.common.FMLLog;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

public class CommandCut extends  CommandBase  {

	Map<String, int[]> posMap = new LinkedHashMap();

	@Override
	public String getCommandName() {
		return "buttercut";
	}

	@Override
	public void processCommand(ICommandSender var1, String[] var2) {

		String username = var1.getCommandSenderName();
		EntityPlayerMP player = getCommandSenderAsPlayer(var1);
		if(username == null || username.isEmpty()) {
			return;
		}

		if(player == null) {
			return;
		}

		if(player.capabilities.isCreativeMode == false) {
			return;
		}

		if(var2.length < 1) {
			return;
		}
		try {
			if(var2[0].compareToIgnoreCase("start") == 0) {
				int[] pos = {(int) player.posX,(int) player.posY,(int) player.posZ};
				posMap.put(username, pos);
				player.sendChatToPlayer("buttercut start : " + String.valueOf(pos[0]) + ", " + String.valueOf(pos[1]) + ", " + String.valueOf(pos[2]));
			}
			else if(var2[0].compareToIgnoreCase("end") == 0) {
				int[] pos = posMap.get(username);
				if(pos == null) {
					return;
				}

				cut(player, pos[0], pos[1], pos[2], (int) player.posX,(int) player.posY,(int) player.posZ);
				posMap.remove(username);

			}
			else if(var2.length < 6) {
				return;
			}
			else {
				int i = 0;
				int x1 = Integer.parseInt(var2[i++]);
				int y1 = Math.min(255, Math.max(Integer.parseInt(var2[i++]), 0));
				int z1 = Integer.parseInt(var2[i++]);
				int x2 = Integer.parseInt(var2[i++]);
				int y2 = Math.min(255, Math.max(Integer.parseInt(var2[i++]), 0));
				int z2 = Integer.parseInt(var2[i++]);
				cut(player, x1, y1, z1, x2, y2, z2);
			}


		}
		catch(NumberFormatException e) {
			player.sendChatToPlayer("buttercut fail : format error");

		} catch (IOException e) {
			e.printStackTrace();
			FMLLog.log(Level.WARNING, e, "ButterDungeons save error");
		}
	}

	private void cut(EntityPlayerMP player, int x1, int y1, int z1, int x2,
			int y2, int z2) throws IOException {
		player.sendChatToPlayer("buttercut range : " + String.valueOf(x1) + ", " + String.valueOf(y1) + ", " + String.valueOf(z1) + " - " + String.valueOf(x2) + ", " + String.valueOf(y2) + ", " + String.valueOf(z2));

		Map<Integer, Character> blockMap = new LinkedHashMap();
		blockMap.put(0, ' ');
		Set<Integer> set = new LinkedHashSet();
		char c = 'A';

		int xn = Math.abs(Math.max(x1, x2) - Math.min(x1, x2)) + 1;
		int yn = Math.max(y1, y2) - Math.min(y1, y2) + 1 ;
		int zn = Math.abs(Math.max(z1, z2) - Math.min(z1, z2)) + 1;
		char[][][] map = new char[yn][zn][xn];
		int xs = Math.min(x1, x2);
		int ys = Math.min(y1, y2);
		int zs = Math.min(z1, z2);
		for(int x = 0; x < xn; ++x) {
			for(int y = 0; y < yn; ++y) {
				for(int z = 0; z < zn; ++z) {
					int blockId = player.worldObj.getBlockId(xs + x, ys + y, zs + z);
					set.add(blockId);
					int metadata = 0;
					if(blockId != 0) {
						metadata = player.worldObj.getBlockMetadata(xs + x, ys + y, zs + z);
					}
					int a = blockId * 100 + metadata;
					if(blockMap.containsKey(a) == false) {
						if(c == 'z') {
							return;
						}
						blockMap.put(a, c);
						map[y][z][x] = c;
						++c;
						if(c == 'Z') {
							c = 'a';
						}
					}
					else {
						map[y][z][x] = blockMap.get(a);
					}
				}
			}
		}

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
		String t = sdf.format(new java.util.Date());
		File root = new File(Minecraft.getMinecraftDir(), Config.root_path);
		{
			File file = new File(root.getPath(), t + ".settings");
			FileWriter fw = new FileWriter(file);
			BufferedWriter bw = new BufferedWriter(fw);

			for(Map.Entry<Integer, Character> e : blockMap.entrySet()) {
				int n = e.getKey();
				int id = n / 100;
				if(id == 0) {
					continue;
				}
				int metadata = n % 100;
				char cc = e.getValue();
				String s = "";
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
				s += "\r\n";
				bw.write(s);
			}
			bw.close();
			fw.close();
		}
		{
			File file = new File(root.getPath(), t + ".map");
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
		{
			File file = new File(root.getPath(), t + ".cfg");
			DungeonConfig cfg = new DungeonConfig();
			cfg.generate_num_limit = 0;
			cfg.dungeon_name = t;
			cfg.save(file);
			Dungeons.loadDungeonConfig(file);
		}
		player.sendChatToPlayer("buttercut save to " + t);
	}

}

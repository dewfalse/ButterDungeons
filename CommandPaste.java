package butterdungeons;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

public class CommandPaste extends CommandBase implements ICommand {

	@Override
	public String getCommandName() {
		return "butterpaste";
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

		if(var2.length < 1) {
			return;
		}
		for(DungeonConfig cfg : Dungeons.dungeons) {
			if(var2[0].compareToIgnoreCase(cfg.dungeon_name) == 0) {
				DungeonGenerator g = new DungeonGenerator();
				g.buildDungeon(player.worldObj, player.worldObj.rand, (int) player.posX,(int) player.posY,(int) player.posZ, cfg);
				player.sendChatToPlayer("butterpaste : " + cfg.dungeon_name);
			}
		}
	}

}

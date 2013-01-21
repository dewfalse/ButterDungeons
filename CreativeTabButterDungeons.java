package butterdungeons;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class CreativeTabButterDungeons extends CreativeTabs {

	public CreativeTabButterDungeons(String label) {
		super(label);
	}

	@Override
	public String getTranslatedTabLabel() {
		return "ButterDungeons";
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getTabIconItemIndex() {
		return Block.obsidian.blockID;
	}

}

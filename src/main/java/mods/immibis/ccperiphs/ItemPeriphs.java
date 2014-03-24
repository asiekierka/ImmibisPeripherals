package mods.immibis.ccperiphs;

import mods.immibis.core.ItemCombined;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class ItemPeriphs extends ItemCombined {

	public ItemPeriphs(Block block) {
		super(block, "immibis_peripherals", getUnlocalizedSuffixes());
	}

	private static String[] getUnlocalizedSuffixes() {
		String[] r = new String[EnumPeriphs.VALUES.length];
		for(EnumPeriphs e : EnumPeriphs.VALUES)
			r[e.ordinal()] = e.name;
		return r;
	}

	@Override
	public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int metadata) {
		if(super.placeBlockAt(stack, player, world, x, y, z, side, hitX, hitY, hitZ, metadata)) {
			TilePeriphs te = (TilePeriphs)world.getTileEntity(x, y, z);
			te.onPlacedOnSide(side);
			
			return true;
		}
		return false;
	}
}

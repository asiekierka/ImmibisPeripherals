package mods.immibis.ccperiphs.rfid;

import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

public class ItemCardBase extends Item {
	// Colours are stored as item metadata, using wool data values
	
	private String cardType;
	
	private IIcon[] icons = new IIcon[16];
	
	public ItemCardBase(String type) {
		super();
		cardType = type;
		setUnlocalizedName("immibis_peripherals."+type);
		setMaxStackSize(1);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister reg) {
		for(int k = 0; k < 16; k++)
			icons[k] = reg.registerIcon("immibis_peripherals:"+cardType+k);
	}
	
	@Override
	public boolean getShareTag() {return true;}
	
	@Override
	public IIcon getIconFromDamage(int damage) {
		if(damage < 0 || damage > 15)
			return null;
		return icons[damage];
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void addInformation(ItemStack stack, EntityPlayer ply, List list, boolean par4) {
		if(stack.stackTagCompound == null || !stack.stackTagCompound.hasKey("data"))
			return;
		
		String line1 = stack.stackTagCompound.getString("line1");
		//String line2 = stack.stackTagCompound.getString("line2");
		//String line3 = stack.stackTagCompound.getString("line3");
		
		if(!line1.equals("")) list.add(line1);
		//if(!line2.equals("")) list.add(line2);
		//if(!line3.equals("")) list.add(line3);
	}

}

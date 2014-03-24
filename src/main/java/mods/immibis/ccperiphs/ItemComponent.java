package mods.immibis.ccperiphs;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemComponent extends Item {

	public static final int META_CPU_CORE = 0;
	public static final int META_CPU_CORE_8 = 1;
	public static final int META_CPU_CORE_64 = 2;
	
	private static final String[] unlocalizedNames = {
		"core",
		"corex8",
		"corex64"
	};
	
	private IIcon[] icons = new IIcon[3];
	
	public ItemComponent() {
		super();
		
		setMaxDamage(0);
		setHasSubtypes(true);
		setCreativeTab(CreativeTabs.tabMisc);
	}
	
	@Override
	public String getUnlocalizedName(ItemStack par1ItemStack) {
		int damage = par1ItemStack.getItemDamage();
		if(damage < 0 || damage >= unlocalizedNames.length)
			return "";
		else
			return "item.immibis_peripherals." + unlocalizedNames[damage];
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIconFromDamage(int par1) {
		if(par1 < 0 || par1 >= icons.length)
			return null;
		else
			return icons[par1];
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister reg) {
		icons[META_CPU_CORE] = reg.registerIcon("immibis_peripherals:mcore1");
		icons[META_CPU_CORE_8] = reg.registerIcon("immibis_peripherals:mcore8");
		icons[META_CPU_CORE_64] = reg.registerIcon("immibis_peripherals:mcore64");
	}
}

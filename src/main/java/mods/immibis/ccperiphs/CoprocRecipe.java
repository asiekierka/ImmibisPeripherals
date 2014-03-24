package mods.immibis.ccperiphs;

import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.oredict.ShapedOreRecipe;

// a shaped recipe, but requires 64 of the middle item instead of 1
// also they can ONLY be crafted on normal workbenches due to BC being fail
public class CoprocRecipe extends ShapedOreRecipe {

	public CoprocRecipe(ItemStack result, Object... recipe) {
		super(result, recipe);
	}
	
	private boolean isWorkbench(InventoryCrafting inv) {
		return inv.getSizeInventory() == 9
			&& inv.getClass() == InventoryCrafting.class
			&& ReflectionHelper.getPrivateValue(InventoryCrafting.class, inv, 2).getClass() == ContainerWorkbench.class;
	}
	
	@Override
	public boolean matches(InventoryCrafting inv, World world) {
		return super.matches(inv, world) && isWorkbench(inv) && inv.getStackInSlot(4).stackSize >= 64;
	}
	
	@Override
	public ItemStack getCraftingResult(InventoryCrafting var1) {
		if(!isWorkbench(var1))
			return null;
		
		ItemStack rv = super.getCraftingResult(var1);
		ItemStack middleStack = var1.getStackInSlot(4);
		if(middleStack == null || middleStack.stackSize < 64)
			return null;
		return rv;
	}

}

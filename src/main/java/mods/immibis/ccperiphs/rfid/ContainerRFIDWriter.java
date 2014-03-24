package mods.immibis.ccperiphs.rfid;

import mods.immibis.core.api.util.BaseContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerRFIDWriter extends BaseContainer<TileRFIDWriter> {
	
	public TileRFIDWriter tile;

	public ContainerRFIDWriter(EntityPlayer player, TileRFIDWriter inv) {
		super(player, inv);
		this.tile = (TileRFIDWriter)inv;
		
		addSlotToContainer(new Slot(inv, 0, 85, 22));
		
		for(int x = 0; x < 9; x++) {
			for(int y = 0; y < 3; y++)
				addSlotToContainer(new Slot(player.inventory, x + y*9 + 9, 13 + 18*x, 66 + 18*y));
			addSlotToContainer(new Slot(player.inventory, x, 13 + 18*x, 124));
		}
	}
	
	@Override
	public ItemStack transferStackInSlot(int n) {return null;}
	
	private int lastProgress = -1;
	
	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();
		
		if(tile.progress != lastProgress) {
			sendProgressBarUpdate(0, tile.progress);
			
			lastProgress = tile.progress;
		}
	}
	
	@Override
	public void updateProgressBar(int i, int j) {
		if(i == 0)
			tile.progress = j;
	}

}

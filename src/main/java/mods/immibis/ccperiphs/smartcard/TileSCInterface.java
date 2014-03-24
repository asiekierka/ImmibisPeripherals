package mods.immibis.ccperiphs.smartcard;


/*import java.util.HashMap;
import java.util.Map;

import mods.immibis.ccperiphs.ImmibisPeripherals;
import mods.immibis.ccperiphs.TilePeriphs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import dan200.computer.api.IComputerAccess;
import dan200.computer.api.IPeripheral;

public class TileSCInterface extends TilePeriphs implements IPeripheral, IInventory {
	
	// computer -> side
	private Map<IComputerAccess, String> computers = new HashMap<IComputerAccess, String>();
	
	@Override
	public String getType() {
		return "smartcard reader";
	}
	
	private static String[] METHOD_NAMES = {
		"send",
		"sendDebug",
	};

	@Override
	public String[] getMethodNames() {
		return METHOD_NAMES;
	}

	@Override
	public Object[] callMethod(IComputerAccess computer, int method, Object[] arguments) throws Exception {
		switch(method) {
		case 0:
			// send
			for(Object o : arguments)
				if(!(o instanceof String))
					return new Object[] {"All arguments must be strings"};
			if(arguments.length == 0)
				return new Object[] {"Need at least one argument"};
			if(card == null)
				return new Object[] {"No card inserted"};
			if(cardComputerAccess == null)
				return new Object[] {"No IComputerAccess (should not happen)"};
			cardComputerAccess.queueEvent("sc_event", arguments);
			break;
		case 1:
			// sendDebug
			if(arguments.length != 1)
				return new Object[] {"Need exactly one argument"};
			if(card == null)
				return new Object[] {"No card inserted"};
			if(cardComputerAccess == null)
				return new Object[] {"No IComputerAccess (should not happen)"};
			cardComputerAccess.queueEvent("sc_debug_input", arguments);
			break;
		}
		return null;
	}

	@Override
	public boolean canAttachToSide(int side) {
		return true;
	}

	@Override
	public void attach(IComputerAccess computer) {
		computers.put(computer, computer.getAttachmentName());
	}

	@Override
	public void detach(IComputerAccess computer) {
		computers.remove(computer);
	}

	@Override
	public int getSizeInventory() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ItemStack getStackInSlot(int var1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ItemStack decrStackSize(int var1, int var2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int var1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setInventorySlotContents(int var1, ItemStack var2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getInvName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getInventoryStackLimit() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer var1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void openChest() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void closeChest() {
		// TODO Auto-generated method stub
		
	}
	
	
	
	@Override
	public boolean onBlockActivated(EntityPlayer ply) {
		if(worldObj.isRemote)
			return false;
		
		ItemStack h = ply.getCurrentEquippedItem();
		if(h != null && h.itemID == ImmibisPeripherals.itemSmartCard.itemID && contents == null) {
			contents = h;
			ply.destroyCurrentEquippedItem();
			onInsertItem();
			return true;
		}
		
		if(h == null && contents != null) {
			onRemoveItem();
			ply.inventory.setInventorySlotContents(ply.inventory.currentItem, contents);
			contents = null;
			return true;
		}
		
		return false;
	}
	
	@Override
	public void updateEntity() {
		super.updateEntity();
		
		if(worldObj.isRemote)
			return;
		
		if(contents != null && card == null && contents.itemID == ImmibisPeripherals.itemSmartCard.itemID) {
			onInsertItem();
		}
		
		if(card != null && card.computer.isOn())
			card.computer.advance(0.05);
	}
	
	
	
	ItemStack contents = null;
	private IComputerAccess cardComputerAccess;
	private SmartCardComputer card;
	private void onInsertItem() {
		if(contents.itemID == ImmibisPeripherals.itemSmartCard.itemID) {
			NBTTagCompound computerTag = (contents.stackTagCompound == null ? null : contents.stackTagCompound.getCompoundTag("computer"));
			card = new SmartCardComputer(worldObj, computerTag, this);
			try {
				card.computer.setPeripheral(0, new IPeripheral() {
					
					@Override
					public String getType() {
						return "smartcard fake peripheral";
					}
					
					@Override
					public String[] getMethodNames() {
						return new String[0];
					}
					
					@Override
					public void detach(IComputerAccess computer) {
					}
					
					@Override
					public boolean canAttachToSide(int side) {
						return true;
					}
					
					@Override
					public Object[] callMethod(IComputerAccess computer, int method, Object[] arguments) throws Exception {
						return null;
					}
					
					@Override
					public void attach(IComputerAccess computer) {
						cardComputerAccess = computer;
					}
				});
				card.computer.turnOn(null);
				card.computer.advance(0);
				System.out.println("Turned on card ID "+card.computer.getID());
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	private void onRemoveItem() {
		if(card != null) {
			System.out.println("Turning off card");
			card.computer.turnOff();
			
			updateCardNBT();
			
			card = null;
			cardComputerAccess = null;
		}
	}
	
	private void updateCardNBT() {
		// Precondition: contents != null
		// Precondition: computer is off
		// Postcondition: card NBT is updated
		
		NBTTagCompound computerTag = new NBTTagCompound(); 
		card.computer.writeToNBT(computerTag);
		
		if(contents.stackTagCompound == null)
			contents.stackTagCompound = new NBTTagCompound();
		contents.stackTagCompound.setCompoundTag("computer", computerTag);
		String label = card.getLabel(0);
		if(label != null && !label.isEmpty())
			contents.stackTagCompound.setString("line1", label);
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		
		if(contents != null) {
			if(card != null)
				updateCardNBT();
			
			NBTTagCompound itemTag = new NBTTagCompound();
			contents.writeToNBT(itemTag);
			tag.setCompoundTag("item", itemTag);
		}
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		
		if(tag.hasKey("item")) {
			NBTTagCompound itemTag = tag.getCompoundTag("item");
			contents = ItemStack.loadItemStackFromNBT(itemTag);
		}
	}

	void onDebugWrite(String s) {
		for(IComputerAccess comp : computers.keySet())
			comp.queueEvent("sc_output", new Object[] {s});
	}

	@Override
	public boolean isInvNameLocalized() {
		return false;
	}

	@Override
	public boolean isStackValidForSlot(int i, ItemStack itemstack) {
		return true;
	}

}
*/
package mods.immibis.ccperiphs.rfid;

import cpw.mods.fml.common.Optional;
import dan200.computer.api.IComputerAccess;
import dan200.computer.api.ILuaContext;
import dan200.computer.api.IPeripheral;
import mods.immibis.ccperiphs.ImmibisPeripherals;
import mods.immibis.ccperiphs.TilePeriphs;
import mods.immibis.core.api.util.Dir;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;

// encode(RFID data, label line 1, label line 2, label line 3) -> (true) or (false, error message)
// isPresent() -> true/false is there a card in the slot?
// getProgress() -> double (0 to 1)
// isCoded() -> true/false is card in slot coded?

public class TileRFIDWriter extends TilePeriphs implements IPeripheral, IInventory {
	
	
	public static final int TICKS_TO_WRITE = 600;
	
	public static final int MAX_DATA_LENGTH = 80; // characters
	public static final int MAX_LABEL_LENGTH = 20; // characters on each line
	
	public byte facing;
	
	volatile ItemStack contents = null;
	
	// Note: Progress etc are not saved in the TE's NBT data,
	// so the operation stops if the world is reloaded.
	// This is intentional as this causes the computer to reboot anyway.
	
	// 0 if not running
	// otherwise ticks remaining
	volatile int progress = 0;
	
	// for current writing operation
	private volatile IComputerAccess computerToNotify = null;
	private volatile String rfidData, label1/*, label2, label3*/;

	// visual effects, some of these are synced with the client
	static final int STATE_EMPTY = 0;
	static final int STATE_IDLE = 1;
	static final int STATE_RUN = 2;
	static final float LID_RATE = 0.04f;
	float lidClosedAmt;
	int state = 0;
	int visualProgress; // 0 to 11
	int heldCardColour = -1;
	
	private void setState(int i) {
		if(state != i) {
			state = i;
			worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
		}
	}
	
	private void setVisualProgress(int i) {
		if(visualProgress != i) {
			visualProgress = i;
			worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
		}
	}
	
	@Override
	public Packet getDescriptionPacket() {
		int i = facing | (state << 8) | (visualProgress << 16) | (heldCardColour << 24);
		NBTTagCompound tag = new NBTTagCompound();
		tag.setInteger("i", i);
		return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, tag);
	}
	
	@Override
	public void onDataPacket(S35PacketUpdateTileEntity p) {
		int i = p.func_148857_g().getInteger("i");
		facing = (byte)i;
		state = (byte)(i >> 8);
		visualProgress = (byte)(i >> 16);
		heldCardColour = (byte)(i >> 24);
	}
	
	
	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		
		if(contents != null)
		{
			NBTTagCompound itemTag = new NBTTagCompound();
			contents.writeToNBT(itemTag);
			tag.setTag("item", itemTag);
		}
		tag.setByte("facing", facing);
		tag.setFloat("lid", lidClosedAmt);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		if(tag.hasKey("item"))
			contents = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("item"));
		facing = tag.getByte("facing");
		lidClosedAmt = tag.getFloat("lid");
	}
	
	
	private boolean canRun() {
		if(contents == null || contents.getItem().equals(ImmibisPeripherals.itemRFID))
			return false;
		if(contents.stackTagCompound != null && contents.stackTagCompound.hasKey("data"))
			return false;
		return true;
	}
	
	@Override
	public void onPlaced(EntityLivingBase player, int look) {
		if(look == Dir.PY)
			facing = (byte)Dir.PY;
		else
			facing = (byte)(look ^ 1);
	}
	
	
	@Override
	public synchronized void updateEntity() {
		super.updateEntity();
		
		if(state == STATE_RUN) {
			lidClosedAmt = Math.min(lidClosedAmt + LID_RATE, 1);
		} else {
			lidClosedAmt = Math.max(lidClosedAmt - LID_RATE, 0);
		}
		
		if(worldObj.isRemote)
			return;

		if(progress > 0 && canRun()) {
			
			progress--;
			setState(STATE_RUN);
			
			if(progress == 0) {
				if(contents.stackTagCompound == null)
					contents.stackTagCompound = new NBTTagCompound();
				
				NBTTagCompound tag = contents.stackTagCompound;
				
				tag.setString("data", rfidData);
				tag.setString("line1", label1);
				//tag.setString("line2", label2);
				//tag.setString("line3", label3);
				
				if(computerToNotify != null) {
					computerToNotify.queueEvent("rfid_written", new Object[] {computerToNotify.getAttachmentName()});
					computerToNotify = null;
				}
			}
			
		} else {
			progress = 0;
			setState(canRun() ? STATE_IDLE : STATE_EMPTY);
		}
		
		if(progress == 0) {
			setVisualProgress(isHoldingCodedCard() ? 11 : 0);
		} else {
			setVisualProgress((int)(12 * getProgress()));
		}
		
		{
			int colour = getHeldCardColour();
			if(heldCardColour != colour) {
				heldCardColour = colour;
				worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
			}
		}
	}
	
	private synchronized boolean isHoldingCard() {
		return contents != null && contents.getItem().equals(ImmibisPeripherals.itemRFID);
	}
	
	private synchronized boolean isHoldingCodedCard() {
		return isHoldingCard() && contents.stackTagCompound != null && contents.stackTagCompound.hasKey("data");
	}
	
	public synchronized int getHeldCardColour() {
		if(!isHoldingCard())
			return -1;
		return contents.getItemDamage();
	}
	
	

	@Override
	public String getType() {
		return "rfid writer";
	}
	
	private static String[] methodNames = {
		"encode",
		"isPresent",
		"getProgress",
		"isCoded"
	};

	public String[] getMethodNames() {
		return methodNames;
	}

	@Override
	public synchronized Object[] callMethod(IComputerAccess computer, ILuaContext ctx, int method, Object[] arguments) throws Exception {
		switch(method) {
		case 0: // encode
			if(progress > 0)
				return new Object[] {false, "Already encoding"};
			if(contents == null)
				return new Object[] {false, "No card inserted"};
			if(!contents.getItem().equals(ImmibisPeripherals.itemRFID))
				return new Object[] {false, "Foreign object blocking slot"};
			if(contents.stackTagCompound != null && contents.stackTagCompound.hasKey("data"))
				return new Object[] {false, "Card already encoded"};
			if(arguments.length != 2 && arguments.length != 3)
				return new Object[] {false, "Expected 2 arguments"};
			for(int k = 0; k < arguments.length; k++)
				if(!(arguments[k] instanceof String))
					return new Object[] {false, "Expected string as argument "+k};
			
			rfidData = (String)arguments[0];
			label1 = (String)arguments[1];
			
			boolean adminMode = arguments.length == 3 && ImmibisPeripherals.adminPassword != null && ImmibisPeripherals.adminPassword.equals(arguments[2]);
			
			if(rfidData.length() > MAX_DATA_LENGTH)
				return new Object[] {false, "Maximum data length is "+MAX_DATA_LENGTH+" chars"};
			if(label1.length() > MAX_LABEL_LENGTH /*|| label2.length() > MAX_LABEL_LENGTH || label3.length() > MAX_LABEL_LENGTH*/)
				return new Object[] {false, "Maximum label length is "+MAX_LABEL_LENGTH+" chars/line"};
			
			computerToNotify = computer;
			progress = adminMode ? 1 : TICKS_TO_WRITE;
			
			return new Object[] {true};
		case 1: // isPresent
			return new Object[] {contents != null};
		case 2: // getProgress
			if(progress == 0)
				return new Object[] {-1.0};
			else
				return new Object[] {1 - (progress / (double)TICKS_TO_WRITE)};
		case 3: // isCoded
			if(contents == null || contents.getItem().equals(ImmibisPeripherals.itemRFID))
				return new Object[] {false};
			return new Object[] {contents.stackTagCompound != null && contents.stackTagCompound.hasKey("data")};
		}
		return new Object[0];
	}

	@Override
	public boolean canAttachToSide(int side) {
		return true;
	}

	@Override
	public void attach(IComputerAccess computer) {
		
	}

	@Override
	public synchronized void detach(IComputerAccess computer) {
		if(computer == computerToNotify) {
			progress = 0;
			computerToNotify = null;
		}
	}
	
	
	
	

	@Override
	public int getSizeInventory() {
		return 1;
	}

	@Override
	public synchronized ItemStack getStackInSlot(int var1) {
		if(var1 == 0)
			return contents;
		return null;
	}

	@Override
	public synchronized ItemStack decrStackSize(int var1, int var2) {
		if(var1 != 0)
			return null;
		
		if(contents == null || contents.stackSize <= var2) {
			ItemStack rv = contents;
			contents = null;
			return rv;
		}
		
		contents.stackSize -= var2;
		
		ItemStack rv = contents.copy();
		rv.stackSize = var2;
		return rv;
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int var1) {
		return null;
	}

	@Override
	public synchronized void setInventorySlotContents(int var1, ItemStack var2) {
		if(var1 == 0)
			contents = var2;
	}

	@Override
	public String getInventoryName() {
		return "RFID writer";
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer var1) {
		return var1.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) <= 64;
	}

	@Override
	public void openInventory() {
		
	}

	@Override
	public void closeInventory() {
		
	}
	
	
	
	
	@Override
	public synchronized boolean onBlockActivated(EntityPlayer ply) {
		/*ply.openGui(mod_ImmibisPeripherals.instance, mod_ImmibisPeripherals.GUI_RFID_WRITER, worldObj, xCoord, yCoord, zCoord);
		return true;*/
		
		// no isRemote check
		
		ItemStack h = ply.getCurrentEquippedItem();
		if(h != null && h.getItem().equals(ImmibisPeripherals.itemRFID) && contents == null) {
			contents = h;
			ply.destroyCurrentEquippedItem();
			return true;
		}
		
		if(h == null && contents != null && lidClosedAmt == 0) {
			ply.inventory.setInventorySlotContents(ply.inventory.currentItem, contents);
			contents = null;
			return true;
		}
		
		return false;
	}

	public synchronized float getProgress() {
		return 1 - (progress / (float)TICKS_TO_WRITE);
	}

	@Override
	public boolean isItemValidForSlot(int i, ItemStack itemstack) {
		return itemstack != null && itemstack.getItem().equals(ImmibisPeripherals.itemRFID);
	}

	@Override
	public boolean hasCustomInventoryName() {
		return false;
	}
}

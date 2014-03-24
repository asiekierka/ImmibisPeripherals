package mods.immibis.ccperiphs.rfid;


import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import cpw.mods.fml.common.Optional;
import dan200.computer.api.IComputerAccess;
import dan200.computer.api.ILuaContext;
import dan200.computer.api.IPeripheral;
import mods.immibis.ccperiphs.ImmibisPeripherals;
import mods.immibis.ccperiphs.TilePeriphs;
import mods.immibis.core.api.util.Dir;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;

public class TileMagStripe extends TilePeriphs implements IPeripheral {
	
	public static final int MAX_DATA_LENGTH = 100;
	public static final int MAX_LABEL_LENGTH = 20;
	
	private Set<IComputerAccess> computers = new HashSet<IComputerAccess>();
	
	public byte facing;
	
	// Not saved with NBT so this device resets when the attached computer does.
	private String writeData;
	private String writeLabel;
	
	// Visual effects
	public static final int STATE_OFF = 0; // no computer connected
	public static final int STATE_IDLE = 1; // computer connected
	public static final int STATE_READ_WAIT = 2; // computer connected, not writing, insert card now
	public static final int STATE_WRITE_WAIT = 3; // computer connected, writing, insert card now
	public static final int STATE_WRITE = 4; // computer connected, writing
	public int state;
	
	private AtomicBoolean stateUpdateRequired = new AtomicBoolean(true);
	
	private boolean insertCardLight;
	
	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setByte("facing", facing);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		facing = tag.getByte("facing");
	}
	
	private void updateState() {
		int newState;
		if(computers.size() == 0)
			newState = STATE_OFF;
		else {
			if(writeData != null)
				newState = insertCardLight ? STATE_WRITE_WAIT : STATE_WRITE;
			else
				newState = insertCardLight ? STATE_READ_WAIT : STATE_IDLE;
		}
		
		if(state != newState) {
			state = newState;
			worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
		}
	}
	
	@Override
	public Packet getDescriptionPacket() {
		return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, ((facing & 7) | (state << 4)), null);
	}
	
	@Override
	public void onDataPacket(S35PacketUpdateTileEntity p) {
		int actionType = p.func_148853_f();
		facing = (byte)(actionType & 7);
		state = (actionType >> 4) & 15;
		worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
	}

	@Override
	public String getType() {
		return "mag card reader";
	}
	
	private static String[] methodNames = {
		"beginWrite",
		"cancelWrite",
		"isWaiting",
		"setInsertCardLight"
	};

	public String[] getMethodNames() {
		return methodNames;
	}

	@Override
	public Object[] callMethod(IComputerAccess computer, ILuaContext ctx, int method, Object[] arguments) throws Exception {
		switch(method) {
		case 0: // beginWrite
			if(arguments.length != 2)
				return new Object[] {false, "Expected two arguments"};
			if(!(arguments[0] instanceof String && arguments[1] instanceof String))
				return new Object[] {false, "Expected string"};
			if(writeData != null)
				return new Object[] {false, "Already writing"};
			
			writeData = (String)arguments[0];
			writeLabel = (String)arguments[1];
			if(writeData.length() > MAX_DATA_LENGTH) {
				writeData = writeLabel = null;
				return new Object[] {false, "Max data length is "+MAX_DATA_LENGTH+" chars"};
			}
			if(writeLabel.length() > MAX_LABEL_LENGTH) {
				writeData = writeLabel = null;
				return new Object[] {false, "Max label length is "+MAX_LABEL_LENGTH+" chars"};
			}
			
			stateUpdateRequired.set(true);
			
			return new Object[] {true};
		case 1: // cancelWrite
			writeData = null;
			writeLabel = null;
			stateUpdateRequired.set(true);
			break;
		case 2: // isWaiting
			return new Object[] {writeData != null};
		case 3: // setInsertCardLight
			if(arguments.length == 1 && arguments[0] instanceof Boolean) {
				insertCardLight = (boolean)(Boolean)arguments[0];
				stateUpdateRequired.set(true);
			}
			break;
		}
		return new Object[0];
	}

	@Override
	public boolean canAttachToSide(int side) {
		return true;
	}

	@Override
	public void attach(IComputerAccess computer) {
		computers.add(computer);
		stateUpdateRequired.set(true);
	}

	@Override
	public void detach(IComputerAccess computer) {
		computers.remove(computer);
		
		if(computers.size() == 0)
			writeData = null;

		stateUpdateRequired.set(true);
	}
	
	@Override
	public boolean onBlockActivated(EntityPlayer ply) {
		ItemStack h = ply.getCurrentEquippedItem();
		if(h == null || !(h.getItem().equals(ImmibisPeripherals.itemMagStripe)))
			return false;
		
		if(worldObj.isRemote)
			return true;
		
		if(writeData != null) {
			String oldData = "";
			if(h.stackTagCompound != null)
				oldData = h.stackTagCompound.getString("data");
			
			if(h.stackTagCompound == null)
				h.stackTagCompound = new NBTTagCompound();
			
			h.stackTagCompound.setString("data", writeData);
			h.stackTagCompound.setString("line1", writeLabel);
			
			for(IComputerAccess c : computers)
				c.queueEvent("mag_write_done", new Object[] {c.getAttachmentName(), oldData});
			
			writeData = null;
			writeLabel = null;
			
			stateUpdateRequired.set(true);
			
		} else {
			String data = "";
			if(h.stackTagCompound != null)
				data = h.stackTagCompound.getString("data");
			
			for(IComputerAccess c : computers)
				c.queueEvent("mag_swipe", new Object[] {data, c.getAttachmentName()});
		}
		
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
	public void updateEntity() {
		super.updateEntity();
		
		if(!worldObj.isRemote && stateUpdateRequired.compareAndSet(true, false))
			updateState();
	}
}

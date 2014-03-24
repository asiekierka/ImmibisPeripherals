package mods.immibis.ccperiphs.lan;


import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import cpw.mods.fml.common.Optional;
import mods.immibis.ccperiphs.ImmibisPeripherals;
import mods.immibis.ccperiphs.TilePeriphs;
import mods.immibis.ccperiphs.lan.WorldNetworkData.CableNet;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import dan200.computer.api.IComputerAccess;
import dan200.computer.api.ILuaContext;
import dan200.computer.api.IPeripheral;

/*
 * send(string message)
 * sendChannel(int channel, string message)
 * sendFrom(int channel, int sender, string message)
 * setDefaultChannel(int channel)
 * getDefaultChannel() -> int channel
 * setPromiscuous(boolean promiscuous)
 * getPromiscuous() -> boolean promiscuous
 * setListening(int channel, boolean state)
 * getListening(int channel) -> boolean state
 */

public class TileNIC extends TilePeriphs implements IPeripheral {
	
	private static final int MAX_CHANNEL = 128;
	private static final int TICKS_PER_SEND = 2;
	private static final int MAX_SEND_QUEUE_LENGTH = 25;
	
	public byte facing;
	private int defaultSendChannel = 1;
	private boolean promiscuous;
	
	private long channelMask1 = 1, channelMask2 = 0;
	private boolean isListeningOn(int channel) {
		if(channel < 1)
			return promiscuous || (computer != null && channel == -computer.getID());
		if(channel > 128)
			return false;
		if(channel <= 64)
			return ((channelMask1 >> (channel - 1)) & 1) != 0;
		else
			return ((channelMask2 >> (channel - 65)) & 1) != 0;
	}
	private void setListeningOn(int channel, boolean state) {
		if(channel < 1 || channel > 128)
			return;
		if(channel <= 64)
			if(state)
				channelMask1 |= (1L << (channel - 1));
			else
				channelMask1 &= ~(1L << (channel - 1));
		else
			if(state)
				channelMask2 |= (1L << (channel - 65));
			else
				channelMask2 &= ~(1L << (channel - 65));
	}
	
	private int ticksUntilNextSend = 0;
	private IComputerAccess computer;
	private String computerSide;
	
	// Only valid on client, used for rendering
	public boolean isConnectedToComputer;
	
	private AtomicBoolean resendDescPacket = new AtomicBoolean();
		
	private static class SendQueueEntry {
		public final int channel, sender;
		public final String message;
		public SendQueueEntry(int channel, int sender, String message) {
			this.channel = channel;
			this.sender = sender;
			this.message = message;
		}
	}
	
	private final ConcurrentLinkedQueue<SendQueueEntry> sendQueue = new ConcurrentLinkedQueue<SendQueueEntry>();
	private final AtomicInteger approxSendQueueLength = new AtomicInteger();

	@Override
	public String getType() {
		return "LAN NIC";
	}
	
	private static final String[] METHOD_NAMES = {
		"send",
		"sendChannel",
		"setDefaultChannel",
		"getDefaultChannel",
		"setPromiscuous",
		"getPromiscuous",
		"sendFrom",
		"setListening",
		"getListening",
	};

	public String[] getMethodNames() {
		return METHOD_NAMES;
	}
	
	private CableNet getNet() {
		return WorldNetworkData.getForWorld(worldObj).getNet(xCoord, yCoord, zCoord);
	}
	
	private synchronized void sendNow(int channel, int sender, String message) {
		CableNet net = getNet();
		if(net == null) {
			if(computer != null)
				computer.queueEvent("lan_error", new Object[] {"Unknown network. Break and replace "+computerSide+" modem."});
			return;
		}
		
		Set<WorldNetworkData.XYZ> invalidNICs = null;
		
		for(WorldNetworkData.XYZ pos : net.nics) {
			if(worldObj.blockExists(pos.x, pos.y, pos.z)) {
				TileEntity te = worldObj.getTileEntity(pos.x, pos.y, pos.z);
				if(te instanceof TileNIC) {
					if(te != this)
						((TileNIC)te).receive(channel, sender, message);
				} else {
					if(invalidNICs == null)
						invalidNICs = new HashSet<WorldNetworkData.XYZ>();
					invalidNICs.add(pos);
				}
			}
		}
		
		if(invalidNICs != null) for(WorldNetworkData.XYZ pos : invalidNICs) {
			System.err.println("Immibis's Peripherals: Removing invalid NIC from network at "+pos.x+","+pos.y+","+pos.z);
			WorldNetworkData.getForWorld(worldObj).removeNIC(pos.x, pos.y, pos.z);
		}
	}
	
	private void receive(int channel, int sender, String message) {
		if(!isListeningOn(channel)) {
			if(WorldNetworkData.DEBUG)
				System.out.println((computer == null ? null : computer.getID())+": not listening to "+channel+" (mask "+channelMask1+","+channelMask2+", promisc "+promiscuous+")");
			return;
		}
		if(computer == null)
			return;
		
		if(WorldNetworkData.DEBUG)
			System.out.println(computer.getID()+": received message from "+sender+" on "+channel);
		
		computer.queueEvent("lan_message", new Object[] {computerSide, sender, channel, message});
	}
	
	@Override
	public void updateEntity() {
		super.updateEntity();
		
		if(worldObj.isRemote)
			return;
		
		if(ticksUntilNextSend == 0) {
			
			SendQueueEntry e = sendQueue.poll();
			if(e != null) {
				ticksUntilNextSend = TICKS_PER_SEND;
				approxSendQueueLength.decrementAndGet();
				
				sendNow(e.channel, e.sender, e.message);
			}
			
		} else
			ticksUntilNextSend--;
		
		if(resendDescPacket.getAndSet(false))
			worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
	}

	@Override
	public Object[] callMethod(IComputerAccess computer, ILuaContext ctx, int method, Object[] arguments) throws Exception {
		int ch;
		switch(method) {
		case 0: // send
			if(arguments.length < 1)
				throw new Exception("Need one argument");
			if(!(arguments[0] instanceof String))
				throw new Exception("Argument must be string");
			
			synchronized(this) {
				ch = defaultSendChannel;
			}
			if(approxSendQueueLength.get() >= MAX_SEND_QUEUE_LENGTH)
				throw new Exception("Buffer full");
			approxSendQueueLength.incrementAndGet();
			sendQueue.add(new SendQueueEntry(ch, computer.getID(), (String)arguments[0]));
			return null;
			
		case 1: // sendChannel
			if(arguments.length < 2)
				throw new Exception("Need two arguments");
			if(!(arguments[0] instanceof Double))
				throw new Exception("First argument must be number");
			if(!(arguments[1] instanceof String))
				throw new Exception("Second argument must be string");
			
			ch = (int)(double)(Double)arguments[0];
			if(ch > MAX_CHANNEL)
				throw new Exception("Channel out of range (must be "+MAX_CHANNEL+" or lower)");
			
			Object[] sendArgs = new Object[arguments.length - 1];
			System.arraycopy(arguments, 1, sendArgs, 0, sendArgs.length);
			
			if(approxSendQueueLength.get() >= MAX_SEND_QUEUE_LENGTH)
				throw new Exception("Buffer full");
			approxSendQueueLength.incrementAndGet();
			sendQueue.add(new SendQueueEntry(ch, computer.getID(), (String)arguments[1]));
			return null;
			
		case 2: // setDefaultChannel
			if(arguments.length < 1 || !(arguments[0] instanceof Double))
				throw new Exception("Argument must be number");
			
			ch = (int)(double)(Double)arguments[0];
			if(ch > MAX_CHANNEL)
				throw new Exception("Channel out of range (must be "+MAX_CHANNEL+" or lower)");
			
			synchronized(this) {
				defaultSendChannel = ch;
			}
			return null;
			
		case 3: // getDefaultChannel
			synchronized(this) {
				return new Object[] {defaultSendChannel};
			}
			
		case 4: // setPromiscuous
			if(arguments.length < 1 || !(arguments[0] instanceof Boolean))
				throw new Exception("Argument must be boolean");
			synchronized(this) {
				promiscuous = (Boolean)arguments[0];
			}
			return null;
			
		case 5: // getPromiscuous
			synchronized(this) {
				return new Object[] {promiscuous};
			}
			
		case 6: // sendFrom
			if(!ImmibisPeripherals.enableSendFrom)
				throw new Exception("sendFrom is disabled on this server");
			
			if(arguments.length < 3)
				throw new Exception("Need three arguments");
			if(!(arguments[0] instanceof Double) || !(arguments[1] instanceof Double))
				throw new Exception("First and second arguments must be numbers");
			if(!(arguments[2] instanceof String))
				throw new Exception("Third argument must be string");
			
			ch = (int)(double)(Double)arguments[0];
			if(ch > MAX_CHANNEL)
				throw new Exception("Channel out of range (must be "+MAX_CHANNEL+" or lower)");
			
			int sender = (int)(double)(Double)arguments[1];
			
			sendArgs = new Object[arguments.length - 2];
			System.arraycopy(arguments, 2, sendArgs, 0, sendArgs.length);
			
			if(approxSendQueueLength.get() >= MAX_SEND_QUEUE_LENGTH)
				throw new Exception("Buffer full");
			approxSendQueueLength.incrementAndGet();
			sendQueue.add(new SendQueueEntry(ch, sender, (String)arguments[2]));
			return null;
			
		case 7: // setListening
			if(arguments.length < 2)
				throw new Exception("Need two arguments");
			if(!(arguments[0] instanceof Double))
				throw new Exception("First argument must be number");
			if(!(arguments[1] instanceof Boolean))
				throw new Exception("Second argument must be boolean");
			
			ch = (int)(double)(Double)arguments[0];
			if(ch < 1 || ch > MAX_CHANNEL)
				throw new Exception("Channel out of range (must be between 1 and "+MAX_CHANNEL+" inclusive)");
			
			synchronized(this) {
				setListeningOn(ch, (Boolean)arguments[1]);
			}
			
		case 8: // getListening
			if(arguments.length < 1)
				throw new Exception("Need one argument");
			if(!(arguments[0] instanceof Double))
				throw new Exception("Argument must be number");
			
			ch = (int)(double)(Double)arguments[0];
			
			synchronized(this) {
				return new Object[] {isListeningOn(ch)};
			}
		}
		return null;
	}

	@Override
	public synchronized boolean canAttachToSide(int side) {
		return side == facing;
	}

	@Override
	public synchronized void attach(IComputerAccess computer) {
		if(this.computer == null) {
			this.computer = computer;
			this.computerSide = computer.getAttachmentName();
			resendDescPacket.set(true);
		}
	}

	@Override
	public synchronized void detach(IComputerAccess computer) {
		if(this.computer == computer) {
			this.computer = null;
			this.computerSide = null;
			
			channelMask1 = 1;
			channelMask2 = 0;
			defaultSendChannel = 1;
			promiscuous = false;
			
			resendDescPacket.set(true);
		}
	}
	
	
	@Override
	public synchronized void onPlacedOnSide(int side) {
		facing = (byte)(side ^ 1);
		notifyNeighbouringBlocks();
	}
	
	@Override
	public synchronized Packet getDescriptionPacket() {
		return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, facing | (computer != null ? 8 : 0), null);
	}
	
	@Override
	public synchronized void onDataPacket(S35PacketUpdateTileEntity packet) {
		int actionType = packet.func_148853_f();
		facing = (byte)(actionType & 7);
		isConnectedToComputer = (actionType & 8) != 0;
		worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
	}
	
	@Override
	public synchronized void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setByte("facing", facing);
	}
	
	@Override
	public synchronized void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		facing = tag.getByte("facing");
	}
	
	
	
	/* drop if not attached to a block - this would prevent use with turtles.
	
	@Override
	public synchronized void onBlockNeighbourChange() {
		
		ForgeDirection dir = ForgeDirection.VALID_DIRECTIONS[facing];
		if(!worldObj.isBlockSolidOnSide(xCoord + dir.offsetX, yCoord + dir.offsetY, zCoord + dir.offsetZ, dir.getOpposite(), true)) {
			ImmibisPeripherals.block.dropBlockAsItem(worldObj, xCoord, yCoord, zCoord, worldObj.getBlockMetadata(xCoord, yCoord, zCoord), 0);
			worldObj.setBlockWithNotify(xCoord, yCoord, zCoord, 0);
		}
	}
	*/
}

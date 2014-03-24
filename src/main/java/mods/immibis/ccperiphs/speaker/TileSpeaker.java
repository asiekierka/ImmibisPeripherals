package mods.immibis.ccperiphs.speaker;

import mods.immibis.ccperiphs.TilePeriphs;
import mods.immibis.core.api.net.IPacket;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dan200.computer.api.IComputerAccess;
import dan200.computer.api.ILuaContext;
import dan200.computer.api.IPeripheral;

public class TileSpeaker extends TilePeriphs implements IPeripheral {
	
	public static final byte OP_WRITE = 1;
	public static final byte OP_ON = 2;
	public static final byte OP_OFF = 3;
	public static final byte OP_EXECUTE = 4;
	public static final byte OP_ATTENUATE = 5;
	public static final byte OP_DEBUG_ON = 6;
	public static final byte OP_DEBUG_OFF = 7;
	public static final byte OP_START = 8;
	public static final byte OP_STOP = 9;
	
	public byte facing;
	
	public Stream stream = null;
	
	@SideOnly(Side.CLIENT)
	public ClientSpeaker client;
	
	@Override
	public Packet getDescriptionPacket() {
		return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, facing, null);
	}
	
	@Override
	public void onDataPacket(S35PacketUpdateTileEntity packet) {
		facing = (byte)packet.func_148853_f();
	}
	
	@Override
	public synchronized void invalidate() {
		stream.stop();
		super.invalidate();
	}
	
	public static final double MAX_DISTANCE = 50;
	public static final int MAX_BUFFER = 1024;
	public static final int MAX_RATE = 1024; // currently must == MAX_BUFFER or bugs
	
	public static final int MEMORY_SIZE = 16384; // measured in shorts
	
	public static final int NUM_CHANNELS = 8;
	
	public short[] serverMemory = new short[MEMORY_SIZE];
	public int serverFreqs[] = new int[NUM_CHANNELS];
	
	public boolean on = false;
	public byte attenuation = 20;
	
	@Override
	public synchronized void validate() {
		final int dimension = worldObj.provider.dimensionId;
		stream = new Stream(worldObj, xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, MAX_DISTANCE, MAX_BUFFER, MAX_RATE) {
			
			@Override
			public void onTick() {
			}
			
			@Override
			public void onOverflow() {
			}
			
			@Override
			public IPacket getStreamPacket(byte[] bytes) {
				PacketSpeakerStream p = new PacketSpeakerStream();
				p.data = bytes;
				p.dimension = dimension;
				p.x = xCoord;
				p.y = yCoord;
				p.z = zCoord;
				return p;
			}
			
			@Override
			public IPacket getStopPacket(EntityPlayer pl) {
				PacketSpeakerStop p = new PacketSpeakerStop();
				p.x = xCoord;
				p.y = yCoord;
				p.z = zCoord;
				p.dimension = dimension;
				return p;
			}
			
			@Override
			public IPacket getStartPacket(EntityPlayer pl) {
				PacketSpeakerStart p = new PacketSpeakerStart();
				p.x = xCoord;
				p.y = yCoord;
				p.z = zCoord;
				p.dimension = dimension;
				p.mem = serverMemory;
				p.youAreOwner = pl.getGameProfile().getName().equals(owner);
				p.attenuation = attenuation;
				p.isOn = on;
				p.serverFreqs = serverFreqs;
				return p;
			}
		};
	}

	@Override
	public String getType() {
		return "speaker";
	}

	public String[] getMethodNames() {
		return new String[] {
			"write",
			"reboot",
			"shutdown",
			"startup",
			"getMemorySize",
			"execute",
			"setAttenuation",
			"debugOn",
			"debugOff",
			"start",
			"stop",
		};
	}

	@Override
	public Object[] callMethod(IComputerAccess computer, ILuaContext ctx, int method, Object[] arguments) throws Exception {
		if(stream == null)
			throw new Exception("No stream (shouldn't happen)");
		
		switch(method) {
		case 0:
			if(arguments.length < 2)
				throw new Exception("Not enough arguments");
			if(arguments.length > 256)
				throw new Exception("Too many arguments");
			for(Object o : arguments) {
				if(!(o instanceof Double))
					throw new Exception("All arguments must be numbers");
			}
			
			for(int k = 1; k < arguments.length; k++) {
				int i = (int)(double)(Double)arguments[k];
				if(i < 0 || i > 65535)
					throw new Exception("Argument out of range (got "+i+", must be between 0 and 65535)");
			}
			
			int addr = (int)(double)(Double)arguments[0];
			if(addr < 0 || addr + arguments.length - 1 >= MEMORY_SIZE)
				throw new Exception("Address out of range (got "+addr+", must be between 0 and "+(MEMORY_SIZE-1)+")");
			
			if(!stream.hasCapacity(arguments.length * 2 + 3))
				throw new Exception("Buffer full");
			
			stream.add(OP_WRITE);
			stream.add((byte)(addr >> 8));
			stream.add((byte)addr);
			int n = arguments.length - 1;
			stream.add((byte)(n >> 8));
			stream.add((byte)n);
			for(int k = 1; k < arguments.length; k++) {
				short s = (short)(double)(Double)arguments[k];
				stream.add((byte)(s >> 8));
				stream.add((byte)s);
				serverMemory[addr + k - 1] = s;
			}
			
			break;
		case 1:
			if(!stream.hasCapacity(2))
				throw new Exception("Buffer full");
			if(on || isServerPlaying()) stream.add(OP_OFF);
			stream.add(OP_ON);
			on = true;
			break;
		case 2:
			if(!stream.hasCapacity(1))
				throw new Exception("Buffer full");
			if(on || isServerPlaying()) stream.add(OP_OFF);
			on = false;
			break;
		case 3:
			if(!stream.hasCapacity(1))
				throw new Exception("Buffer full");
			if(!on) stream.add(OP_ON);
			on = true;
			break;
		case 4:
			return new Object[] {MEMORY_SIZE};
		case 5:
			if(!stream.hasCapacity(3))
				throw new Exception("Buffer full");
			if(arguments.length != 1)
				throw new Exception("Wrong number of arguments");
			if(!(arguments[0] instanceof Double))
				throw new Exception("Argument must be number");
			addr = (int)(double)(Double)arguments[0];
			if(addr < 0 || addr >= MEMORY_SIZE)
				throw new Exception("Argument out of range");
			stream.add(OP_EXECUTE);
			stream.add((byte)(addr >> 8));
			stream.add((byte)addr);
			on = true;
			break;
		case 6:
			if(!stream.hasCapacity(2))
				throw new Exception("Buffer full");
			if(arguments.length != 1)
				throw new Exception("Wrong number of arguments");
			if(!(arguments[0] instanceof Double))
				throw new Exception("Argument must be number");
			
			attenuation = (byte)(int)(double)(Double)arguments[0];
			
			stream.add(OP_ATTENUATE);
			stream.add(attenuation);
			break;
		case 7:
			if(!stream.hasCapacity(1))
				throw new Exception("Buffer full");
			stream.add(OP_DEBUG_ON);
			break;
		case 8:
			if(!stream.hasCapacity(1))
				throw new Exception("Buffer full");
			stream.add(OP_DEBUG_OFF);
			break;
			
		case 9:
			if(!stream.hasCapacity(6))
				throw new Exception("Buffer full");
			if(arguments.length < 1 || !(arguments[0] instanceof Double))
				throw new Exception("Channel must be number");
			if(arguments.length < 2 || !(arguments[1] instanceof Double))
				throw new Exception("Frequency must be number");
			if(arguments.length >= 3 && !(arguments[2] instanceof Double))
				throw new Exception("Volume must be number, if specified at all");
			if(arguments.length >= 4 && !(arguments[3] instanceof Double))
				throw new Exception("Wave type must be number, if specified at all");
			int chan = (int)(double)(Double)arguments[0];
			int freq = (int)(double)(Double)arguments[1];
			//double vol = arguments.length >= 3 ? (double)(Double)arguments[2] : 0.0;
			//int wavetype = arguments.length >= 4 ? (int)(double)(Double)arguments[3] : 0;
			if(chan < 0 || chan >= serverFreqs.length)
				throw new Exception("Invalid channel");
			serverFreqs[chan] = freq;
			stream.add(OP_START);
			stream.add((byte)chan);
			stream.add((byte)(freq >> 8));
			stream.add((byte)freq);
			break;
			
		case 10:
			if(!stream.hasCapacity(2))
				throw new Exception("Buffer full");
			if(!(arguments[0] instanceof Double) || arguments.length < 1)
				throw new Exception("Channel must be number");
			chan = (int)(double)(Double)arguments[0];
			if(chan < 0 || chan >= serverFreqs.length)
				throw new Exception("Invalid channel");
			serverFreqs[chan] = 0;
			stream.add(OP_STOP);
			stream.add((byte)chan);
			break;
		}
		
		return null;
	}
	
	public boolean isServerPlaying() {
		for(int i : serverFreqs)
			if(i != 0)
				return true;
		return false;
	}

	@Override
	public boolean canAttachToSide(int side) {
		return true;
	}
	
	@Override
	public void attach(IComputerAccess arg0) {
	}

	@Override
	public void detach(IComputerAccess computer) {

	}
	
	@Override
	public void updateEntity() {
		super.updateEntity();
		stream.tick();
	}
	
	@Override
	public void onPlaced(EntityLivingBase player, int look) {
		if(player instanceof EntityPlayer)
			owner = ((EntityPlayer)player).getGameProfile().getName();
		else
			owner = null;
		
		facing = (byte)(look ^ 1);
	}
	
	private String owner;
	
	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		if(owner != null)
			nbt.setString("owner", owner);
		nbt.setByte("facing", facing);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		if(nbt.hasKey("owner"))
			owner = nbt.getString("owner");
		else
			owner = null;
		facing = nbt.getByte("facing");
	}

}

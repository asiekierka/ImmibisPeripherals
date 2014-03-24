package mods.immibis.ccperiphs.coproc;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import cpw.mods.fml.common.Optional;
import dan200.computer.api.IComputerAccess;
import dan200.computer.api.ILuaContext;
import dan200.computer.api.IPeripheral;
import mods.immibis.ccperiphs.TilePeriphs;
import mods.immibis.core.api.util.Dir;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.util.Vec3;

public abstract class TileCoprocBase extends TilePeriphs implements IPeripheral {

	public int facing = Dir.NX;
	public boolean isConnected = false;
	
	
	private static String getExpectedArgs(int nReq, Class<?>[] classes) {
		StringBuilder sb = new StringBuilder();
		sb.append("expected ");
		for(int k = 0; k < classes.length; k++) {
			if(k > 0)
				sb.append(", ");
			if(k >= nReq)
				sb.append('[');
			if(classes[k] == String.class)
				sb.append("string");
			else if(classes[k] == Double.class)
				sb.append("number");
			else if(classes[k] == Boolean.class)
				sb.append("boolean");
			else
				sb.append(classes[k].getSimpleName());
			if(k >= nReq)
				sb.append(']');
		}
		return sb.toString();
	}
	
	public static void checkArgs(Object[] args, Class<?>... classes) throws Exception {
		checkArgs(args, classes.length, classes);
	}
	
	public static void checkArgs(Object[] args, int nReq, Class<?>... classes) throws Exception {
		if(args.length < nReq)
			throw new Exception(getExpectedArgs(nReq, classes));
		for(int k = 0; k < args.length && k < classes.length; k++) {
			if(!(k >= nReq && args[k] == null) && !classes[k].isInstance(args[k]))
				throw new Exception(getExpectedArgs(nReq, classes));
		}
	}
	
	@Override
	public Packet getDescriptionPacket() {
		return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, facing | (isConnected ? 8 : 0), null);
	}
	
	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setInteger("facing", facing);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		facing = tag.getInteger("facing");
	}
	
	@Override
	public void onDataPacket(S35PacketUpdateTileEntity packet) {
		facing = (packet.func_148853_f() & 7) % 6;
		isConnected = (packet.func_148853_f() & 8) != 0;
		worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
	}
	
	public static enum LightState {
		RED(255, 0, 0),
		GREEN(0, 255, 0),
		BLUE(0, 0, 255),
		YELLOW(255, 255, 0),
		CYAN(0, 255, 255),
		MAGENTA(255, 0, 255),
		BLACK(0, 0, 0),
		WHITE(255, 255, 255),
		FLASHING(0, 0, 0) {
			public Random rand = new Random();
			@Override
			public void update() {
				r = rand.nextInt(256);
				g = rand.nextInt(256);
				b = rand.nextInt(256);
			}
		};
		
		public static LightState[] VALUES = values();
		
		private LightState(int r, int g, int b) {
			this.r = r;
			this.g = g;
			this.b = b;
		}
		
		public void update() {}
		
		public int r, g, b;
	}
	public LightState[] lightState = new LightState[20];
	private int lightUpdateTicks = 0;
	
	@Override
	public void updateEntity() {
		super.updateEntity();
		
		if(worldObj.isRemote && --lightUpdateTicks < 0) {
			lightUpdateTicks = 0;
			for(int k = 0; k < lightState.length; k++) {
				if(!isConnected)
					lightState[k] = LightState.BLACK;
				else if(lightState[k] == null || worldObj.rand.nextInt(40) == 0)
					lightState[k] = LightState.VALUES[worldObj.rand.nextInt(LightState.VALUES.length)];
			}
		}
	}
	
	private int nComputers;
	protected Set<IComputerAccess> computers = new HashSet<IComputerAccess>();

	@Override
	public void attach(IComputerAccess arg0) {
		nComputers++;
		if((nComputers > 0) != isConnected) {
			isConnected = nComputers > 0;
			worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
		}
		computers.add(arg0);
	}

	@Override
	public boolean canAttachToSide(int arg0) {
		return true;
	}

	@Override
	public void detach(IComputerAccess arg0) {
		nComputers--;
		if((nComputers > 0) != isConnected) {
			isConnected = nComputers > 0;
			worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
		}
		computers.remove(arg0);
	}
	
	@Override
	public void onPlaced(EntityLivingBase player, int _) {
		Vec3 look = player.getLook(1.0f);
		
        double absx = Math.abs(look.xCoord);
        double absz = Math.abs(look.zCoord);
        
        if(absx > absz) {
        	if(look.xCoord < 0)
        		facing = Dir.PX;
        	else
        		facing = Dir.NX;
        } else {
        	if(look.zCoord < 0)
        		facing = Dir.PZ;
        	else
        		facing = Dir.NZ;
        }
	}

	@Override
	public abstract Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) throws Exception;

	@Override
	public abstract String[] getMethodNames();

	@Override
	public abstract String getType();

}

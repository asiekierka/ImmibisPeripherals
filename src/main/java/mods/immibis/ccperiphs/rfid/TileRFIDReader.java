package mods.immibis.ccperiphs.rfid;


import java.util.List;

import cpw.mods.fml.common.Optional;
import dan200.computer.api.IComputerAccess;
import dan200.computer.api.ILuaContext;
import dan200.computer.api.IPeripheral;
import mods.immibis.ccperiphs.ImmibisPeripherals;
import mods.immibis.ccperiphs.TilePeriphs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;

public class TileRFIDReader extends TilePeriphs implements IPeripheral {
	public static final double EXTENDED_RADIUS = 16;
	public static final double RADIUS = 5; // normal radius. data won't be returned for cards farther than this.
	public static final double MIN_RADIUS = 1;
	public static int TICKS_PER_SCAN; // read from config
	
	// Not saved with NBT, so the device resets, the same way computers do.
	// Intentional as long as computers reboot on unload.
	private int ticksRemaining = 0, scanTicks = 0;
	private double scanRadius;
	private IComputerAccess scanningComputer = null;

	
	private void doScan(IComputerAccess comp, IInventory inv, Entity ent) {
		int size = inv.getSizeInventory();
		for(int k = 0; k < size; k++) {
			ItemStack st = inv.getStackInSlot(k);
			if(st != null && st.getItem().equals(ImmibisPeripherals.itemRFID) && st.stackTagCompound != null && st.stackTagCompound.hasKey("data")) {
				double distance = ent.getDistance(xCoord+0.5, yCoord+0.5, zCoord+0.5);
				
				String data = "";
				if(distance <= RADIUS)
					data = st.stackTagCompound.getString("data");
				
				comp.queueEvent("rfid_detected", new Object[] {data, distance, comp.getAttachmentName()});
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void doScan(IComputerAccess comp) {
		AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(xCoord + 0.5 - scanRadius, yCoord + 0.5 - scanRadius, zCoord + 0.5 - scanRadius, xCoord + 0.5 + scanRadius, yCoord + 0.5 + scanRadius, zCoord + 0.5 + scanRadius);

		List<Entity> players = worldObj.getEntitiesWithinAABB(EntityPlayer.class, aabb);
		players.addAll(worldObj.getEntitiesWithinAABB(EntityMinecart.class, aabb));
		
		double scanRadiusSq = scanRadius*scanRadius;
		for(Entity e : players) {
			if(e.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) > scanRadiusSq)
				continue;
			
			if(e instanceof EntityPlayer)
				doScan(comp, ((EntityPlayer)e).inventory, e);
			else if(e instanceof IInventory)
				doScan(comp, (IInventory)e, e);
		}
		
		comp.queueEvent("rfid_scan_done", new Object[] {comp.getAttachmentName()});
	}
	
	
	@Override
	public String getType() {
		return "rfid reader";
	}
	
	private static String[] methodNames = {
		"scan",
		"getProgress",
		"isScanning"
	};

	public String[] getMethodNames() {
		return methodNames;
	}
	
	@Override
	public void updateEntity() {
		super.updateEntity();
		
		if(ticksRemaining > 0) {
			ticksRemaining--;
			if(ticksRemaining == 0 && scanningComputer != null) {
				doScan(scanningComputer);
				scanningComputer = null;
			}
		}
	}

	@Override
	public Object[] callMethod(IComputerAccess computer, ILuaContext ctx, int method, Object[] arguments) throws Exception {
		switch(method) {
		case 0: // scan
			if(ticksRemaining > 0)
				return new Object[] {false, "Already scanning"};
			if(arguments.length == 0 || !(arguments[0] instanceof Number)) {
				scanRadius = RADIUS;
				scanTicks = TICKS_PER_SCAN;
			} else {
				scanRadius = ((Number)arguments[0]).doubleValue();
				if(scanRadius < MIN_RADIUS)
					return new Object[] {false, "Radius too low, minimum is "+MIN_RADIUS};
				if(scanRadius > EXTENDED_RADIUS)
					return new Object[] {false, "Radius too high, maximum is "+EXTENDED_RADIUS};
				scanTicks = (int)(TICKS_PER_SCAN * scanRadius / RADIUS);
			}
			ticksRemaining = scanTicks;
			scanningComputer = computer;
			return new Object[] {true};
		case 1: // getProgress
			if(ticksRemaining == 0)
				return new Object[] {-1};
			return new Object[] {1 - ticksRemaining / (double)scanTicks};
		case 2: // isScanning
			return new Object[] {ticksRemaining > 0};
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
	public void detach(IComputerAccess computer) {
		if(computer == scanningComputer) {
			scanningComputer = null;
			ticksRemaining = 0;
		}
	}
}

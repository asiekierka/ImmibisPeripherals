package mods.immibis.ccperiphs.lan;


import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import mods.immibis.core.api.util.Dir;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

// All access to these objects happens on the server thread.
public class WorldNetworkData extends WorldSavedData {
	public WorldNetworkData(String par1Str) {
		super(par1Str);
	}

	public final static class XYZ {
		public final int x, y, z;
		public XYZ(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
		@Override
		public boolean equals(Object o) {
			try {
				XYZ xyz = (XYZ)o;
				return x == xyz.x && y == xyz.y && z == xyz.z;
			} catch(ClassCastException e) {
				return false;
			}
		}
		
		@Override
		public int hashCode() {
			return ((x + 8192) + (z+8192) * 16903) * 256 + y;
		}
		
		@Override
		public String toString() {
			return "["+x+","+y+","+z+"]";
		}

		public XYZ step(int direction) {
			switch(direction) {
			case Dir.NX: return new XYZ(x - 1, y, z);
			case Dir.PX: return new XYZ(x + 1, y, z);
			case Dir.NY: return new XYZ(x, y - 1, z);
			case Dir.PY: return new XYZ(x, y + 1, z);
			case Dir.NZ: return new XYZ(x, y, z - 1);
			case Dir.PZ: return new XYZ(x, y, z + 1);
			default: throw new IllegalArgumentException("Invalid direction "+direction);
			}
		}
	}
	
	public static class CableNet {
		public final int cableType;
		public final Set<XYZ> cables = new HashSet<XYZ>();
		public final Set<XYZ> nics = new HashSet<XYZ>();
		
		private final int netID;
		private static int nextID = 0;
		
		public CableNet(int cableType) {
			this.cableType = cableType;
			netID = ++nextID;
		}
		
		@Override
		public String toString() {
			return String.valueOf(netID);
		}
	}
	
	
	// FIELDS
	
	private Map<XYZ, CableNet> cables = new HashMap<XYZ, CableNet>();

	
	
	// PERSISTENCE
	
	public static WorldNetworkData getForWorld(World worldObj) {
		final String DATA_NAME = "immibis's-peripherals-networks";
		WorldNetworkData rv = (WorldNetworkData)worldObj.perWorldStorage.loadData(WorldNetworkData.class, DATA_NAME);
		if(rv == null) {
			rv = new WorldNetworkData(DATA_NAME);
			worldObj.perWorldStorage.setData(DATA_NAME, rv);
		}
		return rv;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tag) {
		
		cables.clear();
		
		NBTTagList list = tag.getTagList("nets", 10);
		for(int k = 0; k < list.tagCount(); k++) {
			NBTTagCompound netTag = (NBTTagCompound)list.getCompoundTagAt(k);
			
			CableNet net = new CableNet(netTag.getByte("type"));
			
			NBTTagList cablesTag = netTag.getTagList("cables", 10);
			for(int i = 0; i < cablesTag.tagCount(); i++) {
				NBTTagCompound cableTag = (NBTTagCompound)cablesTag.getCompoundTagAt(i);
				
				XYZ pos = new XYZ(cableTag.getInteger("x"), cableTag.getInteger("y"), cableTag.getInteger("z"));
				
				net.cables.add(pos);
				cables.put(pos, net);
			}
			
			NBTTagList nicsTag = netTag.getTagList("nics", 10);
			for(int i = 0; i < nicsTag.tagCount(); i++) {
				NBTTagCompound nicTag = (NBTTagCompound)nicsTag.getCompoundTagAt(i);
				
				XYZ pos = new XYZ(nicTag.getInteger("x"), nicTag.getInteger("y"), nicTag.getInteger("z"));
				
				net.nics.add(pos);
			}
		}
		
		if(DEBUG) sanityCheck();
	}
	
	private NBTTagCompound xyzToNBT(XYZ xyz) {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setInteger("x", xyz.x);
		tag.setInteger("y", xyz.y);
		tag.setInteger("z", xyz.z);
		return tag;
	}
	
	@Override
	public void writeToNBT(NBTTagCompound root) {
		if(DEBUG) sanityCheck();
		
		NBTTagList netsTag = new NBTTagList();
		for(CableNet net : new HashSet<CableNet>(cables.values())) {
			NBTTagCompound netTag = new NBTTagCompound();
			
			NBTTagList cablesTag = new NBTTagList();
			NBTTagList nicsTag = new NBTTagList();
			
			for(XYZ pos : net.cables)
				cablesTag.appendTag(xyzToNBT(pos));
			
			for(XYZ pos : net.nics)
				nicsTag.appendTag(xyzToNBT(pos));
			
			netTag.setTag("cables", cablesTag);
			netTag.setTag("nics", nicsTag);
			
			netsTag.appendTag(netTag);
		}
		root.setTag("nets", netsTag);
		
	}
	
	
	
	// DEBUGGING
	static final boolean DEBUG = Block.class.getName().equals("net.minecraft.src.Block");
	
	private void sanityCheck() {
		for(Map.Entry<XYZ, CableNet> e : cables.entrySet()) {
			if(!e.getValue().cables.contains(e.getKey()))
				throw new AssertionError("Sanity check failed: Cable's net does not contain cable");
		}
		
		int numCables = 0;
		for(CableNet e : new HashSet<CableNet>(cables.values())) {
			numCables += e.cables.size();
		}
		
		if(numCables != cables.size())
			throw new AssertionError("Sanity check failed: Number of cables ("+cables.size()+") != total net size ("+numCables+")");
	}
	
	
	
	
	
	// NICs (all NICs are also cables)

	public void removeNIC(int x, int y, int z) {
		XYZ pos = new XYZ(x, y, z);
		CableNet net = cables.get(pos);
		if(net != null) {
			if(DEBUG)
				System.out.println("removeNIC: Remove NIC "+pos+" from "+net);
			net.nics.remove(pos);
		}
		
		removeCable(x, y, z);
	}

	public void addNIC(int x, int y, int z, int wireType) {
		addCable(x, y, z, wireType);
		
		XYZ pos = new XYZ(x, y, z);
		cables.get(pos).nics.add(pos);
		
		if(DEBUG)
			System.out.println("addNIC: Add NIC "+pos+" to "+cables.get(pos));
	}
	
	public CableNet getNet(int x, int y, int z) {
		CableNet net = cables.get(new XYZ(x, y, z));
		
		if(DEBUG) {
			System.out.println("Nets:");
			for(Map.Entry<XYZ, CableNet> e : cables.entrySet())
				System.out.println(e.getKey()+": "+e.getValue()+": cables="+e.getValue().cables+", nics="+e.getValue().nics);
		}
		
		return net;
	}
	
	
	
	// CABLE NETS
	
	public void addCable(int x, int y, int z, int type) {
		XYZ pos = new XYZ(x, y, z);
		
		// Find an adjacent net to add the new cable to
		CableNet net = null;
		for(int k = 0; k < 6; k++) {
			XYZ next = pos.step(k);
			CableNet net2 = cables.get(next);
			if(net2 != null && net2.cableType == type) {
				if(net == null) {
					if(DEBUG) System.out.println("Using existing net: "+net2);
					net = net2;
				} else
					// If two or more adjacent nets, merge them since they're now connected
					net = mergeNets(net, net2);
			}
		}
		
		// If there was no adjacent net, make a new one
		if(net == null)
			net = new CableNet(type);
		
		if(DEBUG) System.out.println("Adding cable "+pos+" to "+net);
		
		// add cable to net
		cables.put(pos, net);
		net.cables.add(pos);
		
		setDirty(true);
	}
	
	public void removeCable(int x, int y, int z) {
		
		XYZ pos = new XYZ(x, y, z);
		
		CableNet net = cables.remove(pos);
		if(net == null)
			return;
		net.cables.remove(pos);
		net.nics.remove(pos);
		
		splitNet(net, pos);
		
		setDirty(true);
	}
	
	
	/**
	 * Moves all cables from one net to the other. Returns the merged net.
	 */
	private CableNet mergeNets(CableNet net1, CableNet net2) {
		if(net1 == net2)
			return net1;
		
		if(net1.cables.size() < net2.cables.size())
			return mergeNets(net2, net1);
		
		if(DEBUG) System.out.println("Merge net "+net2+" and "+net1);
		
		// Merge net2 into net1
		for(XYZ pos : net2.cables) {
			cables.put(pos, net1);
			net1.cables.add(pos);
			if(DEBUG) System.out.println("  Move cable "+pos+" from "+net2+" to "+net1);
		}
		if(DEBUG) for(XYZ pos : net2.nics) System.out.println("  Move NIC "+pos+" from "+net2+" to "+net1);
		
		net1.nics.addAll(net2.nics);
		net2.nics.clear();
		net2.cables.clear();
		
		return net1;
	}

	/**
	 * Splits a net into multiple nets if necessary, after removing the wire at position "pos".
	 */
	private void splitNet(CableNet net, XYZ pos) {
		if(net == null)
			return;
		
		if(DEBUG) System.out.println("splitNet "+net+" at "+pos);
		
		// Find adjacent cable locations
		XYZ neighbours[] = new XYZ[6];
		for(int k = 0; k < 6; k++) {
			XYZ _new = pos.step(k);
			if(net.cables.contains(_new))
				neighbours[k] = _new;
		}
		
		// Build new nets, starting from those locations
		CableNet newNetsBySide[] = new CableNet[6];
		for(int k = 0; k < 6; k++) {
			if(neighbours[k] == null)
				continue;
			
			if(newNetsBySide[k] != null)
				continue;
			
			newNetsBySide[k] = new CableNet(net.cableType);
			addLinkedCablesToNet(newNetsBySide[k], neighbours[k]);
			
			for(int i = 0; i < 6; i++) {
				if(neighbours[i] == null)
					continue;
				
				if(newNetsBySide[k].cables.contains(neighbours[i])) {
					if(DEBUG) System.out.println("Side "+i+" is linked to "+k);
					newNetsBySide[i] = newNetsBySide[k];
				}
			}
		}
	}

	/**
	 * Finds all linked cables starting at the given position
	 * and moves them to the given net (including the one at start position).
	 * Ignores cables that are already in the given net.
	 */
	private void addLinkedCablesToNet(CableNet net, XYZ start) {
		Queue<XYZ> open = new LinkedList<XYZ>();
		
		if(DEBUG)
			System.out.println("addLinkedCablesToNet "+net+" "+start);
		
		if(net.cables.add(start)) {
			CableNet net2 = cables.get(start);
			if(net2 != null)
				net2.cables.remove(start);

			cables.put(start, net);
			open.add(start);
			
			if(DEBUG)
				System.out.println("Move cable "+start+" from "+net2+" to "+net);
			
			if(net2 != null && net2.nics.remove(start)) {
				if(DEBUG)
					System.out.println("Move NIC "+start+" from "+net2+" to "+net);
				net.nics.add(start);
			}
		}
		
		while(open.size() > 0) {
			XYZ next = open.poll();
			
			// add all neighbours with the same type which are not already in the net
			for(int k = 0; k < 6; k++) {
				XYZ next2 = next.step(k);
				CableNet net2 = cables.get(next2);
				if(net2 != null && net2.cableType == net.cableType) {
					if(net.cables.add(next2)) {
						net2.cables.remove(next2);
						cables.put(next2, net);
						open.add(next2);
						
						if(DEBUG)
							System.out.println("Move cable "+next2+" from "+net2+" to "+net);
						
						if(net2.nics.remove(next2)) {
							if(DEBUG)
								System.out.println("Move NIC "+next2+" from "+net2+" to "+net);
							net.nics.add(next2);
						}
					}
				}
			}
		}
	}
}

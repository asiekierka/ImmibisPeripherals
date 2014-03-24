package mods.immibis.ccperiphs.speaker;


import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mods.immibis.core.api.APILocator;
import mods.immibis.core.api.net.IPacket;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public abstract class Stream {
	
	private byte[] buffer;
	private int writePos = 0, readPos = 0;
	
	private synchronized byte read() {
		if(writePos == readPos)
			throw new IllegalStateException("Queue is empty");
		byte b = buffer[readPos];
		readPos = (readPos + 1) % buffer.length;
		return b;
	}
	
	private World world;
	private double x, y, z;
	private double dist_sq;
	private boolean overflow;
	private int bytesPerTick;
	
	private Set<EntityPlayer> players = new HashSet<EntityPlayer>();
	
	public Stream(World world, double x, double y, double z, double distance, int bufferSize, int bytesPerTick) {
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
		this.dist_sq = distance * distance;
		this.buffer = new byte[bufferSize];
		this.bytesPerTick = bytesPerTick;
	}
	
	public synchronized void add(byte b) {
		int nextPos = (writePos + 1) % buffer.length;
		if(nextPos == readPos) {
			overflow = true;
			return;
		}
		buffer[writePos] = b;
		writePos = nextPos;
	}
	
	public synchronized boolean hasCapacity(int bytes) {
		int used = (writePos - readPos + buffer.length) % buffer.length;
		
		return (used + bytes < buffer.length);
	}
	
	public abstract IPacket getStopPacket(EntityPlayer pl);
	public abstract IPacket getStartPacket(EntityPlayer pl);
	public abstract IPacket getStreamPacket(byte[] bytes);
	public abstract void onTick();
	public abstract void onOverflow();
	
	@SuppressWarnings("unchecked")
	public void tick() {
		if(world.isRemote)
			return;
		
		for(EntityPlayer pl : (List<EntityPlayer>)world.playerEntities) {
			double dx = pl.posX - x;
			double dy = pl.posY - y;
			double dz = pl.posZ - z;
			if(dx*dx + dy*dy + dz*dz > dist_sq) {
				if(players.remove(pl)) {
					APILocator.getNetManager().sendToClient(getStopPacket(pl), pl);
				}
			} else {
				if(players.add(pl)) {
					APILocator.getNetManager().sendToClient(getStartPacket(pl), pl);
				}
			}
		}
		
		onTick();
		
		byte[] data;
		synchronized(this) {
			if(writePos == readPos)
				return;
			
			if(overflow) {
				onOverflow();
				overflow = false;
			}
			
			int dataSize = (writePos - readPos + buffer.length) % buffer.length;
			dataSize = Math.min(bytesPerTick, dataSize);
			
			data = new byte[dataSize];
			int dataPos = 0;
			for(int k = dataSize - 1; k >= 0; k--)
				data[dataPos++] = read();
		}
		
		IPacket packet = getStreamPacket(data);
		for(EntityPlayer pl : players)
			APILocator.getNetManager().sendToClient(packet, pl);
	}

	public synchronized void stop() {
		for(EntityPlayer pl : players)
			APILocator.getNetManager().sendToClient(getStopPacket(pl), pl);
	}
}

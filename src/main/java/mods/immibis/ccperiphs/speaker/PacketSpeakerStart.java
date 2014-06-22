package mods.immibis.ccperiphs.speaker;


import io.netty.buffer.ByteBuf;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import mods.immibis.ccperiphs.ImmibisPeripherals;
import mods.immibis.core.api.net.IPacket;
import net.minecraft.entity.player.EntityPlayer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PacketSpeakerStart implements IPacket {

	public int x;
	public int y;
	public int z;
	public int dimension;
	public short[] mem;
	public boolean youAreOwner;
	public byte attenuation;
	public boolean isOn;
	public int[] serverFreqs;
	
	@Override
	public void read(DataInputStream in) throws IOException {
		x = in.readInt();
		y = in.readInt();
		z = in.readInt();
		dimension = in.readInt();
		youAreOwner = in.readBoolean();
		attenuation = in.readByte();
		isOn = in.readBoolean();
		
		int len = in.readInt();
		mem = new short[len];
		for(int k = 0; k < len; k++)
			mem[k] = in.readShort();
		
		len = in.readByte();
		serverFreqs = new int[len];
		for(int k = 0; k < len; k++)
			serverFreqs[k] = in.readShort();
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeInt(x);
		out.writeInt(y);
		out.writeInt(z);
		out.writeInt(dimension);
		out.writeBoolean(youAreOwner);
		out.writeByte(attenuation);
		out.writeBoolean(isOn);
		out.writeInt(mem.length);
		for(int k = 0; k < mem.length; k++)
			out.writeShort(mem[k]);
		out.writeByte((byte)serverFreqs.length);
		for(int k = 0; k < serverFreqs.length; k++)
			out.writeShort((short)serverFreqs[k]);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void onReceived(EntityPlayer source) {
		if(source != null)
			return; // can't receive on server
		
		ClientSpeaker.start(x, y, z, dimension, mem, youAreOwner, attenuation, isOn);
		
		ClientSpeaker cs = ClientSpeaker.get(x, y, z, dimension);
		if(cs != null) {
			for(int k = 0; k < serverFreqs.length; k++)
				if(serverFreqs[k] != 0)
					cs.startChannel(k, serverFreqs[k]);
		}
	}

	@Override
	public byte getID() {
		return 1;
	}

	@Override
	public String getChannel() {
		return "imm_per";
	}
}

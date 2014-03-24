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

public class PacketSpeakerStream implements IPacket {

	public int x;
	public int y;
	public int z;
	public int dimension;
	public byte[] data;
	
	@Override
	public void read(ByteBuf in) throws IOException {
		x = in.readInt();
		y = in.readInt();
		z = in.readInt();
		dimension = in.readInt();
		int len = in.readInt();
		data = new byte[len];
		in.readBytes(data);
	}

	@Override
	public void write(ByteBuf out) throws IOException {
		out.writeInt(x);
		out.writeInt(y);
		out.writeInt(z);
		out.writeInt(dimension);
		out.writeInt(data.length);
		out.writeBytes(data);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void onReceived(EntityPlayer source) {
		if(source != null)
			return; // can't receive on server
		
		ClientSpeaker.stream(x, y, z, dimension, data);
	}
}

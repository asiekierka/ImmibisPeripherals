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

public class PacketSpeakerStop implements IPacket {

	public int x;
	public int y;
	public int z;
	public int dimension;
	
	@Override
	public void read(DataInputStream in) throws IOException {
		x = in.readInt();
		y = in.readInt();
		z = in.readInt();
		dimension = in.readInt();
	}

	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeInt(x);
		out.writeInt(y);
		out.writeInt(z);
		out.writeInt(dimension);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void onReceived(EntityPlayer source) {
		if(source != null)
			return; // can't receive on server
		
		ClientSpeaker.stop(x, y, z, dimension);
	}

	@Override
	public byte getID() {
		return ImmibisPeripherals.PKT_SPEAKER_STOP;
	}

	@Override
	public String getChannel() {
		return ImmibisPeripherals.CHANNEL;
	}
}

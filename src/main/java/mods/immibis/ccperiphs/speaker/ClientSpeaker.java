package mods.immibis.ccperiphs.speaker;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.FloatControl.Type;
import javax.sound.sampled.LineUnavailableException;

import mods.immibis.ccperiphs.forth.ForthContext;
import mods.immibis.ccperiphs.forth.IJavaWord;
import mods.immibis.ccperiphs.forth.IOutputDevice;
import mods.immibis.ccperiphs.forth.JavaDictionary;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import cpw.mods.fml.common.ObfuscationReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientSpeaker implements IOutputDevice {
	
	// for rendering
	double r_amplitude;
	double r_phase;
	
	public static class XYZ {
		public int x, y, z, dim;
		@Override
		public int hashCode() {
			return x + y * 257 + z * 65537;
		}
		
		@Override
		public boolean equals(Object o) {
			if(o instanceof XYZ) {
				XYZ xyz = (XYZ)o;
				return x == xyz.x && y == xyz.y && z == xyz.z && dim == xyz.dim;
			}
			return false;
		}
		
		public XYZ(int x, int y, int z, int dim) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.dim = dim;
		}
	}
	
	private static int prevDimension = -1;
	private static Map<XYZ, ClientSpeaker> speakers = new HashMap<XYZ, ClientSpeaker>();
	public static void tickSystem() {
		EntityPlayer me = Minecraft.getMinecraft().thePlayer;
		if(me == null || me.worldObj == null) {
			if(prevDimension == -1)
				return;
			
			prevDimension = -1;
			synchronized(speakers) {
				for(ClientSpeaker cs : speakers.values())
					cs.stop();
				speakers.clear();
			}
			
			return;
		}
		
		int newDim = me.worldObj.provider.dimensionId;
		if(newDim != prevDimension) {
			prevDimension = newDim;
			
			Collection<XYZ> toRemove = new ArrayList<XYZ>();
			synchronized(speakers) {
				for(XYZ xyz : speakers.keySet())
					if(xyz.dim != prevDimension)
						toRemove.add(xyz);
				
				for(XYZ xyz : toRemove)
					speakers.remove(xyz).stop();
			}
		}
		
		for(ClientSpeaker speaker : speakers.values()) {
			speaker.tick();
		}
	}
	
	public static void stop(int x, int y, int z, int dim) {
		synchronized(speakers) {
			TileEntity te = Minecraft.getMinecraft().theWorld.getTileEntity(x, y, z);
			if(te instanceof TileSpeaker)
				((TileSpeaker)te).client = null;
			
			ClientSpeaker cs = speakers.remove(new XYZ(x, y, z, dim));
			if(cs == null) {
				System.out.println("Immibis's Peripherals: Tried to stop non-existent speaker at "+x+","+y+","+z+" dimension "+dim);
				return;
			}
			
			cs.stop();
		}
	}
	
	public static void stream(int x, int y, int z, int dim, byte[] data) {
		synchronized(speakers) {
			ClientSpeaker cs = speakers.get(new XYZ(x, y, z, dim));
			if(cs == null) {
				System.out.println("Immibis's Peripherals: Tried to stream non-existent speaker at "+x+","+y+","+z+" dimension "+dim);
				return;
			}
			
			cs.stream(data);
		}
	}
	
	public static ClientSpeaker get(int x, int y, int z, int dim) {
		return speakers.get(new XYZ(x, y, z, dim));
	}
	
	public static void start(int x, int y, int z, int dim, short[] mem, boolean isOwner, byte attenuation, boolean isOn) {
		XYZ pos = new XYZ(x, y, z, dim);
		synchronized(speakers) {
			if(speakers.containsKey(pos)) {
				System.out.println("Immibis Peripherals: Tried to start already-existent speaker at "+x+","+y+","+z+" dimension "+dim);
				return;
			}
			
			ClientSpeaker cs = new ClientSpeaker(pos);
			cs.isOwner = isOwner;
			cs.attenuation = attenuation;
			cs.active = isOn;
			speakers.put(pos, cs);
			cs.start(mem);
			
			TileEntity te = Minecraft.getMinecraft().theWorld.getTileEntity(x, y, z);
			if(te instanceof TileSpeaker)
				((TileSpeaker)te).client = cs;
		}
	}
	
	private class SpeakerJavaDict extends JavaDictionary {
		public SpeakerJavaDict() {
			addWord(1, "PLAY", new IJavaWord() {@Override public void execute(ForthContext context) {
				int ch = context.popInt();
				int freq = context.popInt();
				if(ch < channels.length && ch >= 0)
					startChannel(ch, freq);
			}});
			addWord(2, "STOP", new IJavaWord() {@Override public void execute(ForthContext context) {
				int ch = context.popInt();
				if(ch < channels.length && ch >= 0)
					stopChannel(ch);
			}});
		}
	};
	
	
	private ForthContext forth;
	private boolean active;
	
	private boolean isOwner;
	private boolean debugOn;
	
	private XYZ pos;
	
	public ClientSpeaker(XYZ pos) {
		this.pos = pos;
	}
	
	private Clip channels[] = new Clip[TileSpeaker.NUM_CHANNELS];
	
	private void stopChannel(int k) {
		if(channels[k] != null) {
			channels[k].stop();
			channels[k].flush();
			channels[k].close();
			channels[k] = null;
		}
	}
	
	private int attenuation;
	
	/*private float initialGain;
	
	private void setVolume(float fraction, FloatControl control) {
		if(fraction < 0) fraction = 0;
		if(fraction > 1) fraction = 1;
		
		//// adapted from paulscode
		//double var3 = control.getMinimum();
		//double var5 = control.getValue(); // initialGain
		//double var7 = initialGain * 0.5 - var3;
		//double var9 = Math.log(10) / 20;
		
		//float value = (float)(var3 + 1 / var9 * Math.log(1 + (Math.exp(var9 * var7) - 1) * fraction));
		//value -= attenuation;
		//if(value < var3) {value = (float)var3; System.out.println("hit minimum gain "+var3);}
		//control.setValue(value);
		
		control.setValue((float)(Math.log(fraction) / Math.log(10) * 20));
	}*/
	
	private static boolean warnedLineUnavailable = false;
	
	private static final int SAMPLE_RATE = 44100;
	private static final int MIN_FREQ = 40, MAX_FREQ = 8000;
	private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 8, 1, false, false);
	public void startChannel(int k, int freq) {
		stopChannel(k);
		
		r_amplitude = Math.min(0.25, r_amplitude + AMPLITUDE_STEP);
		
		if(freq < MIN_FREQ)
			freq = MIN_FREQ;
		if(freq > MAX_FREQ)
			freq = MAX_FREQ;
		int period = SAMPLE_RATE / freq;
		
		byte AMPLITUDE = 40;
		
		byte[] data = new byte[period];
		for(int i = 0; i < period/2; i++)
			data[i] = (byte)(0x80 - AMPLITUDE);
		for(int i = period/2; i < data.length; i++)
			data[i] = (byte)(0x80 + AMPLITUDE);
		
		Clip c = null;
		try {
			c = AudioSystem.getClip();
			c.open(FORMAT, data, 0, data.length);
		} catch(LineUnavailableException e) {
			if(c != null)
				c.close();
			
			if(!warnedLineUnavailable) {
				warnedLineUnavailable = true;
				e.printStackTrace();
				System.err.println("^ This is from Immibis's Peripherals and is NOT A CRITICAL ERROR. However it does mean you won't get sound from the speaker.");
				System.err.println("It's probably a Java or OS bug. Try updating Java, if possible.");
			}
			return;
		}
		c.setLoopPoints(0, -1);
		FloatControl volume = (FloatControl)c.getControl(Type.MASTER_GAIN);
		//initialGain = volume.getValue();
		if(volume_db >= volume.getMinimum()) {
			c.loop(Clip.LOOP_CONTINUOUSLY);
			volume.setValue(Math.min(volume.getMaximum(), volume_db));
		}
		
		channels[k] = c;
		
		
	}
	
	public void stop() {
		//System.out.println("speaker stop");
		
		for(int k = 0; k < channels.length; k++)
			stopChannel(k);
	}
	
	public static final float ANGULAR_VELOCITY = 1.0f; // radians per tick
	public static final float AMPLITUDE_STEP = 0.005f; // blocks per note started
	public static final float AMPLITUDE_KEEP = 0.001f; // blocks per playing note per tick
	
	public float getAngularVelocity() {
		//return (float)Math.sqrt(r_amplitude) * 3;
		//return (float)r_amplitude * 10;
		return ANGULAR_VELOCITY;
	}
	
	private int ticksSinceVolumeAdjust = 0;
	private float volume_db = 0;
	private void tick() {
		r_phase += getAngularVelocity();
		r_amplitude *= 0.9;
		
		for(Clip c : channels)
			if(c != null)
				r_amplitude += AMPLITUDE_KEEP;
		
		EntityPlayer ply = Minecraft.getMinecraft().thePlayer;
		double dx = ply.posX - (pos.x + 0.5);
		double dy = ply.posY - (pos.y + 0.5);
		double dz = ply.posZ - (pos.z + 0.5);
		volume_db = (float)Math.log(Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.MUSIC))/(float)Math.log(10)*10
				- (float)Math.max(0, Math.log(dx*dx + dy*dy + dz*dz) / Math.log(10) * 10)
				- attenuation;
		
		if(active && !(Boolean)ObfuscationReflectionHelper.getPrivateValue(Minecraft.class, Minecraft.getMinecraft(), "field_71445_n", "isGamePaused"))
			forth.tick();
		
		//System.out.println("volume: "+volume_db);
		
		ticksSinceVolumeAdjust++;
		if(ticksSinceVolumeAdjust >= 5) {
			ticksSinceVolumeAdjust = 0;
			for(Clip c : channels)
				if(c != null) {
					FloatControl volume = ((FloatControl)c.getControl(Type.MASTER_GAIN));
					if(volume_db >= volume.getMinimum()) {
						volume.setValue(Math.min(volume.getMaximum(), volume_db));
						if(!c.isActive())
							c.loop(Clip.LOOP_CONTINUOUSLY);
					} else if(c.isActive())
						c.stop();
				}
		}
	}
	
	private void start(short[] mem) {
		//System.out.println("speaker start");
		forth = new ForthContext(this, new SpeakerJavaDict(), 1000, 20000);
		forth.setMemoryContents(mem);
	}
	
	private void stream(byte[] data) {
		//System.out.println("speaker stream, "+Arrays.toString(data));
		int pos = 0;
		try {
			while(pos < data.length) {
				switch(data[pos++]) {
				case TileSpeaker.OP_OFF:
					active = false;
					for(int k = 0; k < channels.length; k++)
						stopChannel(k);
					break;
				case TileSpeaker.OP_ON:
					active = true;
					forth.reboot();
					forth.doCall((short)0);
					break;
				case TileSpeaker.OP_WRITE:
					int addr, n;
					addr = (data[pos++] & 0xFF) << 8;
					addr |= (data[pos++] & 0xFF);
					n = (data[pos++] & 0xFF) << 8;
					n |= (data[pos++] & 0xFF);
					for(int k = n - 1; k >= 0; k--) {
						int val = (data[pos++] & 0xFF) << 8;
						val |= (data[pos++] & 0xFF);
						forth.writeCode(addr++, (short)val);
					}
					break;
				case TileSpeaker.OP_EXECUTE:
					active = true;
					addr = (data[pos++] & 0xFF) << 8;
					addr |= (data[pos++] & 0xFF);
					forth.reboot();
					forth.doCall((short)addr);
					break;
				case TileSpeaker.OP_ATTENUATE:
					attenuation = Math.max(0, data[pos++]);
					break;
				case TileSpeaker.OP_DEBUG_OFF:
					if(isOwner && debugOn)
						Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("Debugging disabled for speaker at "+this.pos.x+","+this.pos.y+","+this.pos.z));
					debugOn = false;
					break;
				case TileSpeaker.OP_DEBUG_ON:
					if(isOwner && !debugOn)
						Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText("Debugging enabled for speaker at "+this.pos.x+","+this.pos.y+","+this.pos.z));
					debugOn = true;
					break;
				case TileSpeaker.OP_START:
					int chan = data[pos++];
					int freq = (data[pos++] & 0xFF) << 8;
					freq |= (data[pos++] & 0xFF);
					startChannel(chan, freq);
					break;
				case TileSpeaker.OP_STOP:
					stopChannel(data[pos++]);
					break;
				default:
					System.out.println("Immibis's Peripherals: Invalid speaker opcode "+data[pos - 1]);
				}
			}
		} catch(ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
		}
	}
	
	

	@Override
	public void write(String text) {
		if(debugOn && isOwner)
			Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(text));
	}
}

package mods.immibis.ccperiphs;

import static org.lwjgl.opengl.GL11.*;
import static mods.immibis.core.RenderUtilsIC.loadIcon;
import static mods.immibis.core.RenderUtilsIC.loadIconArray;

import java.util.Random;

import mods.immibis.ccperiphs.coproc.TileCoprocBase;
import mods.immibis.ccperiphs.lan.BlockLANWire;
import mods.immibis.ccperiphs.lan.TileNIC;
import mods.immibis.ccperiphs.rfid.TileMagStripe;
import mods.immibis.ccperiphs.rfid.TileRFIDWriter;
import mods.immibis.ccperiphs.speaker.ClientSpeaker;
import mods.immibis.ccperiphs.speaker.TileSpeaker;
import mods.immibis.core.api.util.Dir;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderUtils {
	private static float uMin, uMax, vMin, vMax;
	private static RenderBlocks render;
	private static Random random = new Random();
	private static void setTexture(IIcon i) {
		if(render.overrideBlockTexture != null)
			i = render.overrideBlockTexture;
		uMin = i.getMinU();
		uMax = i.getMaxU();
		vMin = i.getMinV();
		vMax = i.getMaxV();
	}

	private static RotatedTessellator rt = new RotatedTessellator();
	
	public static void renderBlockStatic(RenderBlocks render, int meta, TilePeriphs t, double x, double y, double z, Block block) {
		RenderUtils.render = render;
		
		if(block == ImmibisPeripherals.lanWire) {
			renderLANWire(block.getIcon(0,0), (int)x, (int)y, (int)z, meta, -1);
			return;
		}
		
		EnumPeriphs type = EnumPeriphs.VALUES[meta];
		
		if(t != null) {
			// rendering block, not item
			mods.immibis.core.RenderUtilsIC.setBrightness(render.blockAccess, (int)x, (int)y, (int)z);
			Tessellator.instance.setColorOpaque(255, 255, 255);
		}
		
		switch(type) {
		case RFID_WRITER:
			renderRFIDWriterStatic(x, y, z, t == null ? Dir.PY : ((TileRFIDWriter)t).facing);
			break;
		case RFID_READER:
			renderRFIDReaderStatic(x, y, z);
			break;
		case MAG_STRIPE:
			renderMagStripeStatic(x, y, z, t == null ? Dir.PY : ((TileMagStripe)t).facing, t == null ? TileMagStripe.STATE_OFF : ((TileMagStripe)t).state);
			break;
		case SPEAKER:
			renderSpeakerStatic(x, y, z, t == null ? Dir.PY : ((TileSpeaker)t).facing);
			break;
		case NIC:
			renderNICStatic(x, y, z, t == null ? Dir.NX : ((TileNIC)t).facing, t == null, t != null && ((TileNIC)t).isConnectedToComputer);
			if(t != null)
				renderLANWire(ImmibisPeripherals.lanWire.getIcon(0,0), (int)x, (int)y, (int)z, 0, ((TileNIC)t).facing);
			break;
		case COPROC_CRYPTO:
		case COPROC_ADVMAP:
			renderCoprocStatic(x, y, z, type, t == null ? Dir.PX : ((TileCoprocBase)t).facing);
			break;
		}
	}

	public static void renderInvBlock(RenderBlocks render, int meta, Block block) {
		RenderUtils.render = render;
		
		if(block == ImmibisPeripherals.lanWire) {
			setTexture(block.getIcon(0,0));
			rt.setup(-0.5, -0.5, -0.5, Dir.NY);
			Tessellator.instance.startDrawingQuads();
			renderLANWireVertical(0, 1, true, true);
			Tessellator.instance.draw();
			return;
		}
		
		EnumPeriphs type = EnumPeriphs.VALUES[meta];
		
		//glDisable(GL_LIGHTING);
		
		GL11.glPushMatrix();
		GL11.glTranslatef(-0.5f, -0.5f, -0.5f);
		
		switch(type) {
		case RFID_WRITER:
			renderRFIDWriterDynamic(Dir.PY, 1, 0, false, false, -1);
			break;
		case RFID_READER:
			break;
		case MAG_STRIPE:
			break;
		case SPEAKER:
			renderSpeakerDynamic(0, 0, 0, Dir.PY, 0, 0, 0, null);
			break;
		case NIC:
			break;
		case COPROC_CRYPTO:
		case COPROC_ADVMAP:
			break;
		}
		
		//glEnable(GL_LIGHTING);
		
		GL11.glPopMatrix();
	}

	public static void renderRFIDWriterDynamic(int facing, float closed, int progress, boolean on, boolean running, int cardColour) {
		
		// rotate the modelview matrix so +Y is the direction it's facing
		
		switch(facing) {
		case Dir.PY:
			break;
		case Dir.PZ:
			GL11.glRotatef(90, 1, 0, 0);
			GL11.glTranslatef(0, 0, -1);
			break;
		case Dir.NZ:
			GL11.glRotatef(-90, 1, 0, 0);
			GL11.glTranslatef(1, -1, 1);
			GL11.glRotatef(180, 0, 1, 0);
			break;
		case Dir.NX:
			GL11.glRotatef(90, 0, 0, 1);
			GL11.glTranslatef(1, -1, 0);
			GL11.glRotatef(-90, 0, 1, 0);
			break;
		case Dir.PX:
			GL11.glRotatef(-90, 0, 0, 1);
			GL11.glTranslatef(-1, 0, 1);
			GL11.glRotatef(90, 0, 1, 0);
			break;
		}
		
		IIcon runTex = (!on ? iRfwRunOff : (
			running
			? iRfwRunOn[random.nextInt(iRfwRunOn.length)]
			: iRfwRunHalt[random.nextInt(iRfwRunHalt.length)]
		));
		
		IIcon progTex = iRfwProgress[progress];
		
		//closed = 0.5f + 0.5f*(float)Math.cos(ModLoader.getMinecraftInstance().theWorld.getWorldTime() / 20.0);
		
		GL11.glTranslatef(0, 9/16.0f, 0);
		GL11.glScalef(1/16.0f, 1/16.0f, 1/16.0f);
		
		Tessellator t = Tessellator.instance;
		
		// depth func is already LEQUAL
		//GL11.glDepthFunc(GL11.GL_LEQUAL);
		
		Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.locationBlocksTexture);
		t.startDrawingQuads();
		
		setTexture(facing == Dir.PY ? iRfwFaceTop : iRfwFaceSide);
		t.setNormal(0, 1, 0);
		t.addVertexWithUV(0, 0, 0, uMin, vMin);
		t.addVertexWithUV(0, 0, 16, uMin, vMax);
		t.addVertexWithUV(16, 0, 16, uMax, vMax);
		t.addVertexWithUV(16, 0, 0, uMax, vMin);
		
		if(runTex != null) {
			setTexture(runTex);
			t.addVertexWithUV(0, 0, 0, uMin, vMin);
			t.addVertexWithUV(0, 0, 16, uMin, vMax);
			t.addVertexWithUV(16, 0, 16, uMax, vMax);
			t.addVertexWithUV(16, 0, 0, uMax, vMin);
		}
		
		if(progTex != null) {
			setTexture(progTex);
			t.addVertexWithUV(0, 0, 0, uMin, vMin);
			t.addVertexWithUV(0, 0, 16, uMin, vMax);
			t.addVertexWithUV(16, 0, 16, uMax, vMax);
			t.addVertexWithUV(16, 0, 0, uMax, vMin);
		}
		
		t.draw();
		
		if(cardColour >= 0) {
			Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.locationItemsTexture);
			
			t.startDrawingQuads();
			IIcon cardTex = ImmibisPeripherals.itemRFID.getIconFromDamage(cardColour);
			setTexture(cardTex);
			// 9, 6, 13, 11
			t.addVertexWithUV(9, 0.01, 6, uMax, vMin+(vMax-vMin)*3/16f);
			t.addVertexWithUV(9, 0.01, 11, uMin, vMin+(vMax-vMin)*3/16f);
			t.addVertexWithUV(13, 0.01, 11, uMin, vMin+(vMax-vMin)*13/16f);
			t.addVertexWithUV(13, 0.01, 6, uMax, vMin+(vMax-vMin)*13/16f);
			t.draw();
			
			Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.locationBlocksTexture);
		}
		
		
		
		//GL11.glDepthFunc(GL11.GL_LESS);
		
		GL11.glTranslatef(8, 0.01f, 5);
		
		// now 0,0,0 is top left of white square border
		// and coordinates are in pixels
				
		GL11.glRotatef(-90 + 90*closed, 1, 0, 0);
		
		t.startDrawingQuads();
		
		t.setColorRGBA(255, 255, 255, 192);
		GL11.glEnable(GL11.GL_BLEND);
		
		// Z sides
		setTexture(iRfwCoverShort);
		t.setNormal(0, 0, -1);
		t.addVertexWithUV(0, 0, 0, uMin, vMin);
		t.addVertexWithUV(6, 0, 0, uMax, vMin);
		t.addVertexWithUV(6, 5, 0, uMax, vMax);
		t.addVertexWithUV(0, 5, 0, uMin, vMax);

		t.setNormal(0, 0, 1);
		t.addVertexWithUV(0, 0, 7, uMin, vMin);
		t.addVertexWithUV(6, 0, 7, uMax, vMin);
		t.addVertexWithUV(6, 5, 7, uMax, vMax);
		t.addVertexWithUV(0, 5, 7, uMin, vMax);
		
		// X sides
		setTexture(iRfwCoverLong);
		t.setNormal(-1, 0, 0);
		t.addVertexWithUV(0, 0, 0, uMin, vMin);
		t.addVertexWithUV(0, 0, 7, uMin, vMax);
		t.addVertexWithUV(0, 5, 7, uMax, vMax);
		t.addVertexWithUV(0, 5, 0, uMax, vMin);
		
		t.setNormal(1, 0, 0);
		t.addVertexWithUV(6, 0, 0, uMin, vMin);
		t.addVertexWithUV(6, 0, 7, uMin, vMax);
		t.addVertexWithUV(6, 5, 7, uMax, vMax);
		t.addVertexWithUV(6, 5, 0, uMax, vMin);
		
		// Top
		setTexture(iRfwCoverTop);
		t.setNormal(0, 1, 0);
		t.addVertexWithUV(0, 5, 0, uMin, vMin);
		t.addVertexWithUV(0, 5, 7, uMin, vMax);
		t.addVertexWithUV(6, 5, 7, uMax, vMax);
		t.addVertexWithUV(6, 5, 0, uMax, vMin);
		
		t.draw();
		
		GL11.glDisable(GL11.GL_BLEND);
		
	}

	private static void renderRFIDWriterStatic(double x, double y, double z, byte facing) {
		Tessellator t = Tessellator.instance;
		
		final double thick = 9/16.0f; // thickness of the block
		
		switch(facing) {
		case Dir.PY:
			// bottom
			setTexture(iBottom);
			t.setNormal(0.0F, -1.0F, 0.0F);
	        t.addVertexWithUV(x  , y, z  , uMin, vMin);
			t.addVertexWithUV(x+1, y, z  , uMax, vMin);
			t.addVertexWithUV(x+1, y, z+1, uMax, vMax);
			t.addVertexWithUV(x  , y, z+1, uMin, vMax);
			
			// sides
			setTexture(iRfwSideUp);
			t.setNormal(0.0F, 0.0F, -1.0F);
			t.addVertexWithUV(x  , y        , z, uMax, vMax);
			t.addVertexWithUV(x  , y+thick  , z, uMax, vMin);
			t.addVertexWithUV(x+1, y+thick  , z, uMin, vMin);
			t.addVertexWithUV(x+1, y        , z, uMin, vMax);
			
			t.setNormal(0.0F, 0.0F, 1.0F);
	        t.addVertexWithUV(x+1, y+thick  , z+1, uMin, vMin);
			t.addVertexWithUV(x  , y+thick  , z+1, uMax, vMin);
			t.addVertexWithUV(x  , y        , z+1, uMax, vMax);
			t.addVertexWithUV(x+1, y        , z+1, uMin, vMax);
			
			t.setNormal(-1.0F, 0.0F, 0.0F);
	        t.addVertexWithUV(x, y+thick  , z  , uMin, vMin);
			t.addVertexWithUV(x, y        , z  , uMin, vMax);
			t.addVertexWithUV(x, y        , z+1, uMax, vMax);
			t.addVertexWithUV(x, y+thick  , z+1, uMax, vMin);
			
			t.setNormal(1.0F, 0.0F, 0.0F);
	        t.addVertexWithUV(x+1, y+thick  , z  , uMin, vMin);
			t.addVertexWithUV(x+1, y+thick  , z+1, uMax, vMin);
			t.addVertexWithUV(x+1, y        , z+1, uMax, vMax);
			t.addVertexWithUV(x+1, y        , z  , uMin, vMax);
			break;
		
		case Dir.PX:
			// bottom
			setTexture(iRfwBottomSmall);
			t.setNormal(0.0F, -1.0F, 0.0F);
	        t.addVertexWithUV(x      , y, z  , uMin, vMax);
			t.addVertexWithUV(x+thick, y, z  , uMin, vMin);
			t.addVertexWithUV(x+thick, y, z+1, uMax, vMin);
			t.addVertexWithUV(x      , y, z+1, uMax, vMax);
			
			// Z sides
			setTexture(iRfwSide);
			t.setNormal(0.0F, 0.0F, -1.0F);
			t.addVertexWithUV(x      , y  , z, uMin, vMax);
			t.addVertexWithUV(x      , y+1, z, uMin, vMin);
			t.addVertexWithUV(x+thick, y+1, z, uMax, vMin);
			t.addVertexWithUV(x+thick, y  , z, uMax, vMax);
			
			t.setNormal(0.0F, 0.0F, 1.0F);
			t.addVertexWithUV(x+thick, y  , z+1, uMax, vMax);
			t.addVertexWithUV(x+thick, y+1, z+1, uMax, vMin);
			t.addVertexWithUV(x      , y+1, z+1, uMin, vMin);
			t.addVertexWithUV(x      , y  , z+1, uMin, vMax);
			
			// Back
			setTexture(iSide);
			t.setNormal(-1.0F, 0.0F, 0.0F);
	        t.addVertexWithUV(x, y+1, z  , uMin, vMin);
			t.addVertexWithUV(x, y  , z  , uMin, vMax);
			t.addVertexWithUV(x, y  , z+1, uMax, vMax);
			t.addVertexWithUV(x, y+1, z+1, uMax, vMin);
			
			// top
			setTexture(iRfwBottomSmall);
			t.setNormal(0.0F, 1.0F, 0.0F);
			t.addVertexWithUV(x      , y+1, z+1, uMax, vMax);
			t.addVertexWithUV(x+thick, y+1, z+1, uMax, vMin);
			t.addVertexWithUV(x+thick, y+1, z  , uMin, vMin);
			t.addVertexWithUV(x      , y+1, z  , uMin, vMax);
			
			break;
		case Dir.NX:
			// bottom
			setTexture(iRfwBottomSmall);
			t.setNormal(0.0F, -1.0F, 0.0F);
	        t.addVertexWithUV(x+1-thick, y, z  , uMin, vMax);
			t.addVertexWithUV(x+1      , y, z  , uMin, vMin);
			t.addVertexWithUV(x+1      , y, z+1, uMax, vMin);
			t.addVertexWithUV(x+1-thick, y, z+1, uMax, vMax);
			
			// Z sides
			setTexture(iRfwSide);
			t.setNormal(0.0F, 0.0F, -1.0F);
			t.addVertexWithUV(x+1-thick, y  , z, uMin, vMax);
			t.addVertexWithUV(x+1-thick, y+1, z, uMin, vMin);
			t.addVertexWithUV(x+1      , y+1, z, uMax, vMin);
			t.addVertexWithUV(x+1      , y  , z, uMax, vMax);
			
			t.setNormal(0.0F, 0.0F, 1.0F);
			t.addVertexWithUV(x+1      , y  , z+1, uMax, vMax);
			t.addVertexWithUV(x+1      , y+1, z+1, uMax, vMin);
			t.addVertexWithUV(x+1-thick, y+1, z+1, uMin, vMin);
			t.addVertexWithUV(x+1-thick, y  , z+1, uMin, vMax);
			
			// Back
			setTexture(iSide);
			t.setNormal(1.0F, 0.0F, 0.0F);
			t.addVertexWithUV(x+1, y+1, z+1, uMax, vMin);
			t.addVertexWithUV(x+1, y  , z+1, uMax, vMax);
			t.addVertexWithUV(x+1, y  , z  , uMin, vMax);
			t.addVertexWithUV(x+1, y+1, z  , uMin, vMin);
			
			// top
			setTexture(iRfwBottomSmall);
			t.setNormal(0.0F, 1.0F, 0.0F);
			t.addVertexWithUV(x+1-thick, y+1, z+1, uMax, vMax);
			t.addVertexWithUV(x+1      , y+1, z+1, uMax, vMin);
			t.addVertexWithUV(x+1      , y+1, z  , uMin, vMin);
			t.addVertexWithUV(x+1-thick, y+1, z  , uMin, vMax);
			
			break;
		case Dir.NZ:
			// bottom
			setTexture(iRfwBottomSmall);
			t.setNormal(0.0F, -1.0F, 0.0F);
			t.addVertexWithUV(x+1, y, z+1-thick, uMax, vMax);
			t.addVertexWithUV(x+1, y, z+1      , uMax, vMin);
			t.addVertexWithUV(x  , y, z+1      , uMin, vMin);
			t.addVertexWithUV(x  , y, z+1-thick, uMin, vMax);
			
			// X sides
			setTexture(iRfwSide);
			t.setNormal(0.0F, 0.0F, -1.0F);
			t.addVertexWithUV(x, y  , z+1      , uMax, vMax);
			t.addVertexWithUV(x, y+1, z+1      , uMax, vMin);
			t.addVertexWithUV(x, y+1, z+1-thick, uMin, vMin);
			t.addVertexWithUV(x, y  , z+1-thick, uMin, vMax);
			
			t.setNormal(0.0F, 0.0F, 1.0F);
			t.addVertexWithUV(x+1, y  , z+1-thick, uMin, vMax);
			t.addVertexWithUV(x+1, y+1, z+1-thick, uMin, vMin);
			t.addVertexWithUV(x+1, y+1, z+1      , uMax, vMin);
			t.addVertexWithUV(x+1, y  , z+1      , uMax, vMax);
			
			// Back
			setTexture(iSide);
			t.setNormal(1.0F, 0.0F, 0.0F);
			t.addVertexWithUV(x  , y+1, z+1, uMin, vMin);
			t.addVertexWithUV(x  , y  , z+1, uMin, vMax);
			t.addVertexWithUV(x+1, y  , z+1, uMax, vMax);
			t.addVertexWithUV(x+1, y+1, z+1, uMax, vMin);
			
			// top
			setTexture(iRfwBottomSmall);
			t.setNormal(0.0F, 1.0F, 0.0F);
			t.addVertexWithUV(x  , y+1, z+1-thick, uMin, vMax);
			t.addVertexWithUV(x  , y+1, z+1, uMin, vMin);
			t.addVertexWithUV(x+1, y+1, z+1, uMax, vMin);
			t.addVertexWithUV(x+1, y+1, z+1-thick, uMax, vMax);
			
			break;
		
		case Dir.PZ:
			// bottom
			setTexture(iRfwBottomSmall);
			t.setNormal(0.0F, -1.0F, 0.0F);
			t.addVertexWithUV(x+1, y, z      , uMax, vMax);
			t.addVertexWithUV(x+1, y, z+thick, uMax, vMin);
			t.addVertexWithUV(x  , y, z+thick, uMin, vMin);
			t.addVertexWithUV(x  , y, z      , uMin, vMax);
			
			// Z sides
			setTexture(iRfwSide);
			t.setNormal(0.0F, 0.0F, -1.0F);
			t.addVertexWithUV(x, y  , z+thick, uMax, vMax);
			t.addVertexWithUV(x, y+1, z+thick, uMax, vMin);
			t.addVertexWithUV(x, y+1, z      , uMin, vMin);
			t.addVertexWithUV(x, y  , z      , uMin, vMax);
			
			t.setNormal(0.0F, 0.0F, 1.0F);
			t.addVertexWithUV(x+1, y  , z      , uMin, vMax);
			t.addVertexWithUV(x+1, y+1, z      , uMin, vMin);
			t.addVertexWithUV(x+1, y+1, z+thick, uMax, vMin);
			t.addVertexWithUV(x+1, y  , z+thick, uMax, vMax);
			
			// Back
			setTexture(iSide);
			t.setNormal(-1.0F, 0.0F, 0.0F);
			t.addVertexWithUV(x+1, y+1, z, uMax, vMin);
			t.addVertexWithUV(x+1, y  , z, uMax, vMax);
			t.addVertexWithUV(x  , y  , z, uMin, vMax);
			t.addVertexWithUV(x  , y+1, z, uMin, vMin);
			
			// top
			setTexture(iRfwBottomSmall);
			t.setNormal(0.0F, 1.0F, 0.0F);
			t.addVertexWithUV(x  , y+1, z      , uMin, vMax);
			t.addVertexWithUV(x  , y+1, z+thick, uMin, vMin);
			t.addVertexWithUV(x+1, y+1, z+thick, uMax, vMin);
			t.addVertexWithUV(x+1, y+1, z      , uMax, vMax);
			break;
		}
	}
	
	private static void renderLANWireVertical(double bottom, double top, boolean bottomcap, boolean topcap) {
		final double min = 6/16.0, max=10/16.0/*, du=4/256.0, dvt=top/16.0, dvb=bottom/16.0*/;
		
		final double vt = vMin + (vMax - vMin) * top;
		final double vb = vMin + (vMax - vMin) * bottom;
		
		rt.setNormal(0, 0, -1);
		rt.addVertexWithUV(min, top, min, uMin, vt);
		rt.addVertexWithUV(max, top, min, uMax, vt);
		rt.addVertexWithUV(max, bottom, min, uMax, vb);
		rt.addVertexWithUV(min, bottom, min, uMin, vb);
		
		rt.setNormal(0, 0, 1);
		rt.addVertexWithUV(min, bottom, max, uMin, vb);
		rt.addVertexWithUV(max, bottom, max, uMax, vb);
		rt.addVertexWithUV(max, top, max, uMax, vt);
		rt.addVertexWithUV(min, top, max, uMin, vt);
		
		rt.setNormal(-1, 0, 0);
		rt.addVertexWithUV(min, bottom, min, uMin, vb);
		rt.addVertexWithUV(min, bottom, max, uMax, vb);
		rt.addVertexWithUV(min, top, max, uMax, vt);
		rt.addVertexWithUV(min, top, min, uMin, vt);
		
		rt.setNormal(1, 0, 0);
		rt.addVertexWithUV(max, top, min, uMin, vt);
		rt.addVertexWithUV(max, top, max, uMax, vt);
		rt.addVertexWithUV(max, bottom, max, uMax, vb);
		rt.addVertexWithUV(max, bottom, min, uMin, vb);
		
		if(bottomcap) {
			double d1 = (6/16.0)*(vMax-vMin), d2=(10/16.0)*(vMax-vMin);
			
			rt.setNormal(-1, 0, 0);
			rt.addVertexWithUV(max, bottom, min, uMax, vMin+d1);
			rt.addVertexWithUV(max, bottom, max, uMax, vMin+d2);
			rt.addVertexWithUV(min, bottom, max, uMin, vMin+d2);
			rt.addVertexWithUV(min, bottom, min, uMin, vMin+d1);
			
		}
		
		if(topcap) {
			double d1 = (6/16.0)*(vMax-vMin), d2=(10/16.0)*(vMax-vMin);
			
			rt.setNormal(1, 0, 0);
			rt.addVertexWithUV(min, top, min, uMin, vMin+d1);
			rt.addVertexWithUV(min, top, max, uMin, vMin+d2);
			rt.addVertexWithUV(max, top, max, uMax, vMin+d2);
			rt.addVertexWithUV(max, top, min, uMax, vMin+d1);
			
		}
		
	}
	
	private static void renderLANWire(IIcon tex, int x, int y, int z, int meta, int forceDir) {
		setTexture(tex);
		
		mods.immibis.core.RenderUtilsIC.setBrightness(render.blockAccess, x, y, z);
		Tessellator.instance.setColorRGBA(255, 255, 255, 255);
		
		boolean nx = BlockLANWire.connects(render.blockAccess, x-1,y,z,meta) || forceDir == Dir.NX;
		boolean px = BlockLANWire.connects(render.blockAccess, x+1,y,z,meta) || forceDir == Dir.PX;
		boolean ny = BlockLANWire.connects(render.blockAccess, x,y-1,z,meta) || forceDir == Dir.NY;
		boolean py = BlockLANWire.connects(render.blockAccess, x,y+1,z,meta) || forceDir == Dir.PY;
		boolean nz = BlockLANWire.connects(render.blockAccess, x,y,z-1,meta) || forceDir == Dir.NZ;
		boolean pz = BlockLANWire.connects(render.blockAccess, x,y,z+1,meta) || forceDir == Dir.PZ;
		
		final double min = 6/16f, max=10/16f;
		
		if(!nx && !ny && !nz && !px && !py && !pz) {
			rt.setup(x, y, z, Dir.NY);
			renderLANWireVertical(min, max, true, true);
			return;
		}
		
		if(nx || px) {
			rt.setup(x, y, z, Dir.NX);
			renderLANWireVertical(nx?0:min, px?1:max, !nx && !ny && !py && !nz && !pz, !px && !ny && !py && !nz && !pz);
		}
		
		if(ny || py) {
			rt.setup(x, y, z, Dir.NY);
			renderLANWireVertical(ny?0:min, py?1:max, !ny && !px && !px && !nz && !pz, !py && !nx && !px && !nz && !pz);
		}
		
		if(nz || pz) {
			rt.setup(x, y, z, Dir.NZ);
			renderLANWireVertical(nz?0:min, pz?1:max, !nz && !ny && !py && !nx && !px, !pz && !ny && !py && !nx && !px);
		}
	}
	
	private static void renderNICStatic(double x, double y, double z, int facing, boolean item, boolean connectedToComputer) {
		
		rt.setup(x, y, z, facing);
		
		final double ins = 2/16.0f; // inset on +X/+Z/-X/-Z edges
		//final double duv = (1-ins*2)/16.0f;
		final double thick = 2/16.0f; // thickness of the block
		
		//final double duv2 = ((1 - thick) / 16.0f);
		
		if(item)
			rt.x += 0.5 - thick/2;
		
		// bottom
		setTexture(iNicCompSide);
		rt.setNormal(0.0F, -1.0F, 0.0F);
        rt.addVertexWithUV(ins  , 0, ins  , uMin, vMin);
		rt.addVertexWithUV(1-ins, 0, ins  , uMax, vMin);
		rt.addVertexWithUV(1-ins, 0, 1-ins, uMax, vMax);
		rt.addVertexWithUV(ins  , 0, 1-ins, uMin, vMax);
		
		// top
		setTexture(connectedToComputer ? iNicCableSide : iNicCableSideOff);
		rt.setNormal(0.0F, 1.0F, 0.0F);
		rt.addVertexWithUV(ins  , thick, 1-ins, uMin, vMax);
		rt.addVertexWithUV(1-ins, thick, 1-ins, uMax, vMax);
		rt.addVertexWithUV(1-ins, thick, ins  , uMax, vMin);
		rt.addVertexWithUV(ins  , thick, ins  , uMin, vMin);
		
		// sides
		setTexture(iNicThinSide);
		rt.setNormal(0.0F, 0.0F, -1.0F);
		rt.addVertexWithUV(ins  , 0      , ins, uMax, vMax);
		rt.addVertexWithUV(ins  , thick  , ins, uMax, vMin);
		rt.addVertexWithUV(1-ins, thick  , ins, uMin, vMin);
		rt.addVertexWithUV(1-ins, 0      , ins, uMin, vMax);
		
		rt.setNormal(0.0F, 0.0F, 1.0F);
        rt.addVertexWithUV(1-ins, thick  , 1-ins, uMin, vMin);
		rt.addVertexWithUV(ins  , thick  , 1-ins, uMax, vMin);
		rt.addVertexWithUV(ins  , 0      , 1-ins, uMax, vMax);
		rt.addVertexWithUV(1-ins, 0      , 1-ins, uMin, vMax);
		
		rt.setNormal(-1.0F, 0.0F, 0.0F);
        rt.addVertexWithUV(ins, thick  , ins  , uMin, vMin);
		rt.addVertexWithUV(ins, 0      , ins  , uMin, vMax);
		rt.addVertexWithUV(ins, 0      , 1-ins, uMax, vMax);
		rt.addVertexWithUV(ins, thick  , 1-ins, uMax, vMin);
		
		rt.setNormal(1.0F, 0.0F, 0.0F);
        rt.addVertexWithUV(1-ins, thick  , ins  , uMin, vMin);
		rt.addVertexWithUV(1-ins, thick  , 1-ins, uMax, vMin);
		rt.addVertexWithUV(1-ins, 0      , 1-ins, uMax, vMax);
		rt.addVertexWithUV(1-ins, 0      , ins  , uMin, vMax);
	}
	
	/*
	private static void renderSimpleCube(double x, double y, double z, int tex_px, int tex_nx, int tex_py, int tex_ny, int tex_pz, int tex_nz) {
		Tessellator t = Tessellator.instance;
		
		final float duv = 1/16.0f;
		final float thick = 16/16.0f; // height of the "base"
		final float corner = 0/16.0f; // insets at the top corners of the base
		
		// bottom
		setTexture(tex_ny);
		t.setNormal(0.0F, -1.0F, 0.0F);
		t.addVertexWithUV(x+1, y, z  , u+duv, v+duv);
		t.addVertexWithUV(x+1, y, z+1, u+duv, v);
		t.addVertexWithUV(x  , y, z+1, u, v);
		t.addVertexWithUV(x  , y, z  , u, v+duv);
		
		// sides
		setTexture(tex_nz);
		t.setNormal(0.0F, 0.0F, -1.0F);
		t.addVertexWithUV(x  , y        , z, u+duv, v+duv);
		t.addVertexWithUV(x+corner, y+thick  , z+corner, u+duv, v);
		t.addVertexWithUV(x+1-corner, y+thick  , z+corner, u, v);
		t.addVertexWithUV(x+1, y        , z, u, v+duv);
		
		setTexture(tex_pz);
		t.setNormal(0.0F, 0.0F, 1.0F);
        t.addVertexWithUV(x+1-corner, y+thick  , z+1-corner, u, v);
		t.addVertexWithUV(x+corner, y+thick  , z+1-corner, u+duv, v);
		t.addVertexWithUV(x  , y        , z+1, u+duv, v+duv);
		t.addVertexWithUV(x+1, y        , z+1, u, v+duv);
		
		setTexture(tex_nx);
		t.setNormal(-1.0F, 0.0F, 0.0F);
        t.addVertexWithUV(x+corner, y+thick  , z+corner, u, v);
		t.addVertexWithUV(x, y        , z, u, v+duv);
		t.addVertexWithUV(x, y        , z+1, u+duv, v+duv);
		t.addVertexWithUV(x+corner, y+thick  , z+1-corner, u+duv, v);
		
		setTexture(tex_px);
		t.setNormal(1.0F, 0.0F, 0.0F);
        t.addVertexWithUV(x+1-corner, y+thick  , z+corner, u, v);
		t.addVertexWithUV(x+1-corner, y+thick  , z+1-corner, u+duv, v);
		t.addVertexWithUV(x+1, y        , z+1, u+duv, v+duv);
		t.addVertexWithUV(x+1, y        , z  , u, v+duv);
		
		// top
		setTexture(tex_py);
		t.setNormal(0.0F, 1.0F, 0.0F);
		t.addVertexWithUV(x+corner, y+thick, z+corner, u, v+duv);
		t.addVertexWithUV(x+corner, y+thick, z+1-corner, u, v);
		t.addVertexWithUV(x+1-corner, y+thick, z+1-corner, u+duv, v);
		t.addVertexWithUV(x+1-corner, y+thick, z+corner, u+duv, v+duv);
	}
	*/
	
	private static void renderRFIDReaderStatic(double x, double y, double z) {
		Tessellator t = Tessellator.instance;
		
		//final float duv = 1/16.0f;
		final float thick = 16/16.0f; // height of the "base"
		final float corner = 0/16.0f; // insets at the top corners of the base
		
		// bottom
		setTexture(iBottom);
		t.setNormal(0.0F, -1.0F, 0.0F);
		t.addVertexWithUV(x+1, y, z  , uMax, vMax);
		t.addVertexWithUV(x+1, y, z+1, uMax, vMin);
		t.addVertexWithUV(x  , y, z+1, uMin, vMin);
		t.addVertexWithUV(x  , y, z  , uMin, vMax);
		
		// sides
		setTexture(iRfrSide);
		t.setNormal(0.0F, 0.0F, -1.0F);
		t.addVertexWithUV(x  , y        , z, uMax, vMax);
		t.addVertexWithUV(x+corner, y+thick  , z+corner, uMax, vMin);
		t.addVertexWithUV(x+1-corner, y+thick  , z+corner, uMin, vMin);
		t.addVertexWithUV(x+1, y        , z, uMin, vMax);
		
		t.setNormal(0.0F, 0.0F, 1.0F);
        t.addVertexWithUV(x+1-corner, y+thick  , z+1-corner, uMin, vMin);
		t.addVertexWithUV(x+corner, y+thick  , z+1-corner, uMax, vMin);
		t.addVertexWithUV(x  , y        , z+1, uMax, vMax);
		t.addVertexWithUV(x+1, y        , z+1, uMin, vMax);
		
		t.setNormal(-1.0F, 0.0F, 0.0F);
        t.addVertexWithUV(x+corner, y+thick  , z+corner, uMin, vMin);
		t.addVertexWithUV(x, y        , z, uMin, vMax);
		t.addVertexWithUV(x, y        , z+1, uMax, vMax);
		t.addVertexWithUV(x+corner, y+thick  , z+1-corner, uMax, vMin);
		
		t.setNormal(1.0F, 0.0F, 0.0F);
        t.addVertexWithUV(x+1-corner, y+thick  , z+corner, uMin, vMin);
		t.addVertexWithUV(x+1-corner, y+thick  , z+1-corner, uMax, vMin);
		t.addVertexWithUV(x+1, y        , z+1, uMax, vMax);
		t.addVertexWithUV(x+1, y        , z  , uMin, vMax);
		
		// top
		setTexture(iBottom);
		t.setNormal(0.0F, 1.0F, 0.0F);
		t.addVertexWithUV(x+corner, y+thick, z+corner, uMin, vMax);
		t.addVertexWithUV(x+corner, y+thick, z+1-corner, uMin, vMin);
		t.addVertexWithUV(x+1-corner, y+thick, z+1-corner, uMax, vMin);
		t.addVertexWithUV(x+1-corner, y+thick, z+corner, uMax, vMax);
	}
	
	private static void renderMagStripeStatic(double x, double y, double z, int facing, int state) {
		Tessellator t = Tessellator.instance;
		
		//final float duv = 1/16.0f;
		
		IIcon facingTex = (facing == Dir.PY ? iMcrTop : iMcrSide)[state];
		
		// bottom
		setTexture(iBottom);
		t.setNormal(0.0F, -1.0F, 0.0F);
		t.addVertexWithUV(x+1, y, z  , uMax, vMax);
		t.addVertexWithUV(x+1, y, z+1, uMax, vMin);
		t.addVertexWithUV(x  , y, z+1, uMin, vMin);
		t.addVertexWithUV(x  , y, z  , uMin, vMax);
		
		// sides
		setTexture(facing == Dir.NZ ? facingTex : iSide);
		t.setNormal(0.0F, 0.0F, -1.0F);
		t.addVertexWithUV(x  , y  , z, uMax, vMax);
		t.addVertexWithUV(x  , y+1, z, uMax, vMin);
		t.addVertexWithUV(x+1, y+1, z, uMin, vMin);
		t.addVertexWithUV(x+1, y  , z, uMin, vMax);
		
		setTexture(facing == Dir.PZ ? facingTex : iSide);
		t.setNormal(0.0F, 0.0F, 1.0F);
        t.addVertexWithUV(x+1, y+1  , z+1, uMax, vMin);
		t.addVertexWithUV(x, y+1  , z+1, uMin, vMin);
		t.addVertexWithUV(x  , y        , z+1, uMin, vMax);
		t.addVertexWithUV(x+1, y        , z+1, uMax, vMax);
		
		setTexture(facing == Dir.NX ? facingTex : iSide);
		t.setNormal(-1.0F, 0.0F, 0.0F);
        t.addVertexWithUV(x, y+1  , z, uMin, vMin);
		t.addVertexWithUV(x, y        , z, uMin, vMax);
		t.addVertexWithUV(x, y        , z+1, uMax, vMax);
		t.addVertexWithUV(x, y+1  , z+1, uMax, vMin);
		
		setTexture(facing == Dir.PX ? facingTex : iSide);
		t.setNormal(1.0F, 0.0F, 0.0F);
        t.addVertexWithUV(x+1, y+1  , z, uMax, vMin);
		t.addVertexWithUV(x+1, y+1  , z+1, uMin, vMin);
		t.addVertexWithUV(x+1, y        , z+1, uMin, vMax);
		t.addVertexWithUV(x+1, y        , z  , uMax, vMax);
		
		// top
		setTexture(facing == Dir.PY ? facingTex : iBottom);
		t.setNormal(0.0F, 1.0F, 0.0F);
		t.addVertexWithUV(x, y+1, z, uMin, vMax);
		t.addVertexWithUV(x, y+1, z+1, uMin, vMin);
		t.addVertexWithUV(x+1, y+1, z+1, uMax, vMin);
		t.addVertexWithUV(x+1, y+1, z, uMax, vMax);
	}
	
	private static void renderSpeakerStatic(double x, double y, double z, int facing) {
		Tessellator t = Tessellator.instance;
		
		//final float duv = 1/16.0f;
		
		IIcon facingTex = (facing == Dir.PY || facing == Dir.NY ? iSpeakerFrontY : iSpeakerFront);
		
		// bottom
		setTexture(facing == Dir.NY ? facingTex : iBottom);
		t.setNormal(0.0F, -1.0F, 0.0F);
		t.addVertexWithUV(x+1, y, z  , uMax, vMax);
		t.addVertexWithUV(x+1, y, z+1, uMax, vMin);
		t.addVertexWithUV(x  , y, z+1, uMin, vMin);
		t.addVertexWithUV(x  , y, z  , uMin, vMax);
		
		// sides
		setTexture(facing == Dir.NZ ? facingTex : iSide);
		t.setNormal(0.0F, 0.0F, -1.0F);
		t.addVertexWithUV(x  , y  , z, uMax, vMax);
		t.addVertexWithUV(x  , y+1, z, uMax, vMin);
		t.addVertexWithUV(x+1, y+1, z, uMin, vMin);
		t.addVertexWithUV(x+1, y  , z, uMin, vMax);
		
		setTexture(facing == Dir.PZ ? facingTex : iSide);
		t.setNormal(0.0F, 0.0F, 1.0F);
        t.addVertexWithUV(x+1, y+1  , z+1, uMax, vMin);
		t.addVertexWithUV(x, y+1  , z+1, uMin, vMin);
		t.addVertexWithUV(x  , y        , z+1, uMin, vMax);
		t.addVertexWithUV(x+1, y        , z+1, uMax, vMax);
		
		setTexture(facing == Dir.NX ? facingTex : iSide);
		t.setNormal(-1.0F, 0.0F, 0.0F);
        t.addVertexWithUV(x, y+1  , z, uMin, vMin);
		t.addVertexWithUV(x, y        , z, uMin, vMax);
		t.addVertexWithUV(x, y        , z+1, uMax, vMax);
		t.addVertexWithUV(x, y+1  , z+1, uMax, vMin);
		
		setTexture(facing == Dir.PX ? facingTex : iSide);
		t.setNormal(1.0F, 0.0F, 0.0F);
        t.addVertexWithUV(x+1, y+1  , z, uMax, vMin);
		t.addVertexWithUV(x+1, y+1  , z+1, uMin, vMin);
		t.addVertexWithUV(x+1, y        , z+1, uMin, vMax);
		t.addVertexWithUV(x+1, y        , z  , uMax, vMax);
		
		// top
		setTexture(facing == Dir.PY ? facingTex : iBottom);
		t.setNormal(0.0F, 1.0F, 0.0F);
		t.addVertexWithUV(x, y+1, z, uMin, vMax);
		t.addVertexWithUV(x, y+1, z+1, uMin, vMin);
		t.addVertexWithUV(x+1, y+1, z+1, uMax, vMin);
		t.addVertexWithUV(x+1, y+1, z, uMax, vMax);
	}
	
	private static float[][] coneOffsets = {
		{6, 3},
		{10, 3},
		{10, 4},
		{12, 4},
		{12, 6},
		{13, 6},
		{13, 10},
		{12, 10},
		{12, 12},
		{10, 12},
		{10, 13},
		{6, 13},
		{6, 12},
		{4, 12},
		{4, 10},
		{3, 10},
		{3, 6},
		{4, 6},
		{4, 4},
		{6, 4},
		{6, 3}
	};
	static {
		for(int k = 0; k < coneOffsets.length; k++) {
			float[] d = coneOffsets[k];
			coneOffsets[k] = new float[] {d[0] / 16, d[1] / 16, (0.5f + d[0] / 16) / 2, (0.5f + d[1] / 16) / 2};
		}
	}
	public static void renderSpeakerDynamic(float x, float y, float z, int dir, float ampl, double phase, float partialTick, ClientSpeaker cs) {
		Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.locationBlocksTexture);
		setTexture(iSpeakerCentre);
		
		float du = uMax - uMin;
		float dv = vMax - vMin;
		
		float inset = (float)Math.cos(phase + partialTick*(cs == null ? 0 : cs.getAngularVelocity())) * ampl + 0.25f;
		glColor3f(1, 1, 1);
		rt.setup(x, y, z, dir);
		rt.doBuffering = false;
		rt.base.startDrawing(GL_QUAD_STRIP);
		for(int k = coneOffsets.length - 1; k >= 0; k--) {
			rt.addVertexWithUV(coneOffsets[k][0], 0, coneOffsets[k][1], uMin + coneOffsets[k][0]*du, vMin + coneOffsets[k][1]*dv); 
			rt.addVertexWithUV(coneOffsets[k][2], inset, coneOffsets[k][3], uMin + coneOffsets[k][2]*du, vMin + coneOffsets[k][3]*dv);
		}
		rt.base.draw();
		
		rt.base.startDrawing(GL_POLYGON);
		for(int k = 0; k < coneOffsets.length; k++) {
			rt.addVertexWithUV(coneOffsets[k][2], inset, coneOffsets[k][3], uMin + coneOffsets[k][2]*du, vMin + coneOffsets[k][3]*dv);
		}
		rt.base.draw();
	
	}
	
	// call rt.setup before this with the x/y/z and *opposite* direction
	// and rt.base.setColorOpaque if you want a different colour
	public static void renderCoprocFrontPanel(IIcon texture, int start, int end) {
		for(int k = start; k < end; k++) {
			setTexture(texture);
			// server front panel
			rt.setNormal(0, 1, 0);
			rt.addVertexWithUV( 1/16.0, 1, (k*3+1)/16.0, uMin, vMin);
			rt.addVertexWithUV( 1/16.0, 1, (k*3+3)/16.0, uMin, vMax);
			rt.addVertexWithUV(15/16.0, 1, (k*3+3)/16.0, uMax, vMax);
			rt.addVertexWithUV(15/16.0, 1, (k*3+1)/16.0, uMax, vMin);
		}
	}
	
	private static void renderCoprocStatic(double x, double y, double z, EnumPeriphs type, int facing) {
		facing ^= 1; // rendering is backwards, whoops, this fixes it
		
		rt.setup(x, y, z, facing);
		
		//final double duv = 1/16.0;
		
		final double GAP = 0.001; // to avoid z-fighting
		
		// front
		setTexture(iCpFront);
		rt.setNormal(0, 1, 0);
        rt.addVertexWithUV(0, 1, 0, uMin, vMin);
		rt.addVertexWithUV(0, 1, 1, uMin, vMax);
		rt.addVertexWithUV(1, 1, 1, uMax, vMax);
		rt.addVertexWithUV(1, 1, 0, uMax, vMin);
		
		// back inside
		setTexture(iCpInside);
		rt.setNormal(0, 1, 0);
        rt.addVertexWithUV(0, GAP, 0, uMin, vMin);
		rt.addVertexWithUV(0, GAP, 1, uMin, vMax);
		rt.addVertexWithUV(1, GAP, 1, uMax, vMax);
		rt.addVertexWithUV(1, GAP, 0, uMax, vMin);
		
		// back outside
		setTexture(iCpSideDark);
		rt.setNormal(0, -1, 0);
		rt.addVertexWithUV(1, 0, 0, uMax, vMin);
		rt.addVertexWithUV(1, 0, 1, uMax, vMax);
		rt.addVertexWithUV(0, 0, 1, uMin, vMax);
		rt.addVertexWithUV(0, 0, 0, uMin, vMin);
		
		// left outside
		setTexture(iCpSideLight);
		rt.setNormal(-1, 0, 0);
		rt.addVertexWithUV(0, 0, 1, uMax, vMax);
		rt.addVertexWithUV(0, 1, 1, uMax, vMin);
		rt.addVertexWithUV(0, 1, 0, uMin, vMin);
		rt.addVertexWithUV(0, 0, 0, uMin, vMax);
		
		// right outside
		setTexture(iCpSideDark);
		rt.setNormal(1, 0, 0);
		rt.addVertexWithUV(1, 0, 0, uMin, vMax);
		rt.addVertexWithUV(1, 1, 0, uMin, vMin);
		rt.addVertexWithUV(1, 1, 1, uMax, vMin);
		rt.addVertexWithUV(1, 0, 1, uMax, vMax);
		
		// left inside
		setTexture(iCpInside);
		rt.setNormal(1, 0, 0);
		rt.addVertexWithUV(GAP, 0, 0, uMin, vMax);
		rt.addVertexWithUV(GAP, 1, 0, uMin, vMin);
		rt.addVertexWithUV(GAP, 1, 1, uMax, vMin);
		rt.addVertexWithUV(GAP, 0, 1, uMax, vMax);
		
		// right inside
		setTexture(iCpInside);
		rt.setNormal(-1, 0, 0);
		rt.addVertexWithUV(1-GAP, 0, 1, uMax, vMax);
		rt.addVertexWithUV(1-GAP, 1, 1, uMax, vMin);
		rt.addVertexWithUV(1-GAP, 1, 0, uMin, vMin);
		rt.addVertexWithUV(1-GAP, 0, 0, uMin, vMax);
		
		// bottom outside
		setTexture(iCpSideDark);
		rt.setNormal(0, 0, 1);
		rt.addVertexWithUV(1, 0, 1, uMax, vMax);
		rt.addVertexWithUV(1, 1, 1, uMax, vMin);
		rt.addVertexWithUV(0, 1, 1, uMin, vMin);
		rt.addVertexWithUV(0, 0, 1, uMin, vMax);
		
		// top inside
		setTexture(iCpInside);
		rt.setNormal(0, 0, 1);
		rt.addVertexWithUV(1, 0, GAP, uMax, vMax);
		rt.addVertexWithUV(1, 1, GAP, uMax, vMin);
		rt.addVertexWithUV(0, 1, GAP, uMin, vMin);
		rt.addVertexWithUV(0, 0, GAP, uMin, vMax);
		
		// bottom inside
		setTexture(iCpInside);
		rt.setNormal(0, 0, 1);
		rt.addVertexWithUV(0, 0, 1-GAP, uMin, vMax);
		rt.addVertexWithUV(0, 1, 1-GAP, uMin, vMin);
		rt.addVertexWithUV(1, 1, 1-GAP, uMax, vMin);
		rt.addVertexWithUV(1, 0, 1-GAP, uMax, vMax);
		
		// top outside
		setTexture(iCpSideLight);
		rt.setNormal(0, 0, 1);
		rt.addVertexWithUV(0, 0, 0, uMin, vMax);
		rt.addVertexWithUV(0, 1, 0, uMin, vMin);
		rt.addVertexWithUV(1, 1, 0, uMax, vMin);
		rt.addVertexWithUV(1, 0, 0, uMax, vMax);
		
		renderCoprocFrontPanel(iCpPanelBlank, 0, 5);
		
		//double du14 = 14 / 256.0;
		setTexture(iCpServerSide);
		for(int k = 0; k < 5; k++) {
			// server bottom
			rt.setNormal(0, 0, -1);
			rt.addVertexWithUV( 1/16.0, 1, (k*3+3)/16.0, uMin, vMin);
			rt.addVertexWithUV( 1/16.0, 0, (k*3+3)/16.0, uMin, vMax);
			rt.addVertexWithUV(15/16.0, 0, (k*3+3)/16.0, uMax, vMax);
			rt.addVertexWithUV(15/16.0, 1, (k*3+3)/16.0, uMax, vMin);
			
			// server top
			rt.setNormal(0, 0, 1);
			rt.addVertexWithUV(15/16.0, 1, (k*3+1)/16.0, uMax, vMin);
			rt.addVertexWithUV(15/16.0, 0, (k*3+1)/16.0, uMax, vMax);
			rt.addVertexWithUV( 1/16.0, 0, (k*3+1)/16.0, uMin, vMax);
			rt.addVertexWithUV( 1/16.0, 1, (k*3+1)/16.0, uMin, vMin);
			
		}
		
	}

	public static void renderCoprocDynamic(double x, double y, double z, EnumPeriphs type, int facing, TileCoprocBase tile) {
		Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.locationBlocksTexture);
		
		final double GAP = 0.001; // to avoid z-fighting
		switch(facing) {
		case Dir.NX: x -= GAP; break;
		case Dir.NY: y -= GAP; break;
		case Dir.NZ: z -= GAP; break;
		case Dir.PX: x += GAP; break;
		case Dir.PY: y += GAP; break;
		case Dir.PZ: z += GAP; break;
		}
		
		glDisable(GL_LIGHTING);
		
		rt.setup(x, y, z, facing ^ 1);
		rt.base.startDrawingQuads();
		rt.base.setBrightness(0x00F000F0);
		if(tile.isConnected)
			rt.base.setColorOpaque(0, 255, 0);
		else
			rt.base.setColorOpaque(255, 0, 0);
		renderCoprocFrontPanel(iCpPanelStatus, 0, 5);
		
		for(int i = 0; i < 5; i++) {
			for(int k = 0; k < 4; k++) {
				TileCoprocBase.LightState ls = tile.lightState[k + i*4];
				if(ls == null)
					continue;
				
				ls.update();
				rt.base.setColorOpaque(ls.r, ls.g, ls.b);
				renderCoprocFrontPanel(iCpPanelFlash[k], i, i+1);
			}
		}
		
		rt.base.draw();
		
		glEnable(GL_LIGHTING);
	}
	
	
	private static IIcon iRfwFaceTop, iRfwFaceSide, iRfwRunOff, iRfwSide, iRfwSideUp, iRfwBottomSmall;
	private static IIcon[] iRfwRunOn, iRfwRunHalt, iRfwProgress, iMcrTop, iMcrSide, iCpPanelFlash;
	private static IIcon iBottom, iSide;
	private static IIcon iNicCompSide, iNicCableSide, iNicThinSide, iNicCableSideOff;
	private static IIcon iRfrSide, iSpeakerFrontY, iSpeakerFront, iSpeakerCentre;
	private static IIcon iCpSideLight, iCpSideDark, iCpFront, iCpInside;
	private static IIcon iRfwCoverTop, iRfwCoverLong, iRfwCoverShort;
	private static IIcon iCpServerSide, iCpPanelBlank, iCpPanelStatus;

	public static void registerTextures(IIconRegister reg) {
		iRfwFaceTop = loadIcon(reg,"immibis_peripherals:rfw-face-top");
		iRfwFaceSide = loadIcon(reg,"immibis_peripherals:rfw-face-side");
		iRfwRunOff = loadIcon(reg,"immibis_peripherals:rfw-run-off");
		iRfwRunOn = loadIconArray(reg, "immibis_peripherals:rfw-run", 12);
		iRfwRunHalt = loadIconArray(reg, "immibis_peripherals:rfw-run-halt", 2);
		iRfwProgress = loadIconArray(reg, "immibis_peripherals:rfw-bar", 12);
		iRfwSide = loadIcon(reg,"immibis_peripherals:rfw-side");
		iRfwSideUp = loadIcon(reg,"immibis_peripherals:rfw-side-up");
		iRfwBottomSmall = loadIcon(reg,"immibis_peripherals:rfw-bottom-small");
		iSide = loadIcon(reg,"immibis_peripherals:side");
		iBottom = loadIcon(reg,"immibis_peripherals:bottom");
		iNicCompSide = loadIcon(reg,"immibis_peripherals:nic-compside");
		iNicCableSide = loadIcon(reg,"immibis_peripherals:nic-cableside");
		iNicCableSideOff = loadIcon(reg,"immibis_peripherals:nic-cableside-off");
		iNicThinSide = loadIcon(reg,"immibis_peripherals:nic-thinside");
		iRfrSide = loadIcon(reg,"immibis_peripherals:rfr-side");
		iMcrTop = loadIconArray(reg, "immibis_peripherals:mcr-top", 5);
		iMcrSide = loadIconArray(reg, "immibis_peripherals:mcr-side", 5);
		iSpeakerFront = loadIcon(reg,"immibis_peripherals:speaker-front");
		iSpeakerFrontY = loadIcon(reg,"immibis_peripherals:speaker-front-y");
		iCpSideLight = loadIcon(reg,"immibis_peripherals:cp-side-light");
		iCpSideDark = loadIcon(reg,"immibis_peripherals:cp-side-dark");
		iCpInside = loadIcon(reg,"immibis_peripherals:cp-inside");
		iCpFront = loadIcon(reg,"immibis_peripherals:cp-front");
		iRfwCoverTop = loadIcon(reg,"immibis_peripherals:rfw-cover-top");
		iRfwCoverLong = loadIcon(reg,"immibis_peripherals:rfw-cover-long");
		iRfwCoverShort = loadIcon(reg,"immibis_peripherals:rfw-cover-short");
		iCpServerSide = loadIcon(reg,"immibis_peripherals:cp-server-side");
		iCpPanelBlank = loadIcon(reg,"immibis_peripherals:cp-panel-blank");
		iCpPanelStatus = loadIcon(reg,"immibis_peripherals:cp-panel-status");
		iCpPanelFlash = loadIconArray(reg, "immibis_peripherals:cp-panel-flash", 4);
		iSpeakerCentre = loadIcon(reg, "immibis_peripherals:speaker-centre");
	}
}
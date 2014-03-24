package mods.immibis.ccperiphs.rfid;

import mods.immibis.ccperiphs.RenderUtils;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderTileRFIDWriter extends TileEntitySpecialRenderer {

	@Override
	public void renderTileEntityAt(TileEntity teRaw, double x, double y, double z, float partialTick) {
		mods.immibis.core.RenderUtilsIC.setBrightness(teRaw.getWorldObj(), teRaw.xCoord, teRaw.yCoord, teRaw.zCoord);
		
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		
		TileRFIDWriter te = (TileRFIDWriter)teRaw;
		
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_LIGHTING);
		
		float interpLid = te.lidClosedAmt + TileRFIDWriter.LID_RATE * partialTick * (te.state == TileRFIDWriter.STATE_RUN ? 1 : -1);
		interpLid = Math.min(Math.max(interpLid, 0), 1);
		
		RenderUtils.renderRFIDWriterDynamic(te.facing, interpLid, te.visualProgress, te.state != TileRFIDWriter.STATE_EMPTY, te.state == TileRFIDWriter.STATE_RUN, te.heldCardColour);
		GL11.glEnable(GL11.GL_CULL_FACE);
		
		GL11.glPopMatrix();
	}

}
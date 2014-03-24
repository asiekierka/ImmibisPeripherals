package mods.immibis.ccperiphs.speaker;

import mods.immibis.ccperiphs.RenderUtils;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderTileSpeaker extends TileEntitySpecialRenderer {
	
	@Override
	public void renderTileEntityAt(TileEntity var1, double x, double y, double z, float var8) {
		TileSpeaker ts = (TileSpeaker)var1;
		mods.immibis.core.RenderUtilsIC.setBrightness(var1.getWorldObj(), var1.xCoord, var1.yCoord, var1.zCoord);
		if(ts.client != null)
			RenderUtils.renderSpeakerDynamic((float)x, (float)y, (float)z, ts.facing, (float)ts.client.r_amplitude, ts.client.r_phase, var8, ts.client);
		else
			ts.client = ClientSpeaker.get(var1.xCoord, var1.yCoord, var1.zCoord, var1.getWorldObj().provider.dimensionId);
	}
}
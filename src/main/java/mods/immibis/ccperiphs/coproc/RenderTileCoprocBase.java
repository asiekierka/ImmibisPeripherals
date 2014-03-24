package mods.immibis.ccperiphs.coproc;

import mods.immibis.ccperiphs.EnumPeriphs;
import mods.immibis.ccperiphs.RenderUtils;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

public class RenderTileCoprocBase extends TileEntitySpecialRenderer {

	@Override
	public void renderTileEntityAt(TileEntity var1, double var2, double var4, double var6, float var8) {
		RenderUtils.renderCoprocDynamic(var2, var4, var6, EnumPeriphs.VALUES[var1.getBlockMetadata()], ((TileCoprocBase)var1).facing, (TileCoprocBase)var1);
	}

}

package mods.immibis.ccperiphs;

import mods.immibis.core.api.porting.PortableBlockRenderer;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.IBlockAccess;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class BlockRenderer implements PortableBlockRenderer {
	@Override
	public boolean renderWorldBlock(RenderBlocks render, IBlockAccess w, int x, int y, int z, Block block, int model) {
		TilePeriphs t = null;
		if(block == ImmibisPeripherals.block) {
			t = (TilePeriphs)w.getTileEntity(x, y, z);
			if(t == null)
				return false;
		}
		RenderUtils.renderBlockStatic(render, w.getBlockMetadata(x, y, z), t, x, y, z, block);
		return true;
	}
	
	@Override
	public void renderInvBlock(RenderBlocks render, Block block, int meta, int model) {
		if(block == ImmibisPeripherals.block) {
			Tessellator.instance.startDrawingQuads();
			RenderUtils.renderBlockStatic(render, meta, null, -0.5, -0.5, -0.5, block);
			Tessellator.instance.draw();
		}
		RenderUtils.renderInvBlock(render, meta, block);
	}
}
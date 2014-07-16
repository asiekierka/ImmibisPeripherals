package mods.immibis.ccperiphs.lan;


import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mods.immibis.ccperiphs.BlockPeriphs;
import mods.immibis.ccperiphs.EnumPeriphs;
import mods.immibis.ccperiphs.ImmibisPeripherals;
import mods.immibis.core.RenderUtilsIC;
import mods.immibis.core.api.util.Dir;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockLANWire extends Block {
	public static int renderType = -1;

	public BlockLANWire() {
		super(Material.circuits);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister reg) {
		blockIcon = RenderUtilsIC.loadIcon(reg, "immibis_peripherals:lanwire");
	}
	
	public static boolean connects(IBlockAccess w, int x, int y, int z, int fromMeta) {
		Block block = w.getBlock(x, y, z);
		int bMeta = w.getBlockMetadata(x, y, z);
		if(block instanceof BlockLANWire)
			return fromMeta == bMeta;
		if(block instanceof BlockPeriphs)
			return false;
		if(fromMeta == 0)
			return bMeta == EnumPeriphs.NIC.ordinal();
		return false;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void addCollisionBoxesToList(World par1World, int par2, int par3, int par4, AxisAlignedBB par5AxisAlignedBB, List par6List, Entity par7Entity) {
		addCollidingBlockToList((IBlockAccess)par1World, par2, par3, par4, par5AxisAlignedBB, par6List, par7Entity);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void addBB(AxisAlignedBB mask, List list, int x, int y, int z, AxisAlignedBB bb) {
		bb = bb.offset(x, y, z);
		if(mask == null || bb.intersectsWith(mask))
			list.add(bb);
	}
	
	@SuppressWarnings("rawtypes")
	public void addCollidingBlockToList(IBlockAccess blockAccess, int x, int y, int z, AxisAlignedBB mask, List list, Entity par7Entity) {
		Block block = blockAccess.getBlock(x, y, z);
		int meta = blockAccess.getBlockMetadata(x, y, z);
		int forceDir = -1;
		
		if(block != this) {
			if(meta == EnumPeriphs.NIC.ordinal()) {
				meta = 0;
				forceDir = ((TileNIC)blockAccess.getTileEntity(x, y, z)).facing;
			}
		}
		
		boolean nx = BlockLANWire.connects(blockAccess, x-1,y,z,meta) || forceDir == Dir.NX;
		boolean px = BlockLANWire.connects(blockAccess, x+1,y,z,meta) || forceDir == Dir.PX;
		boolean ny = BlockLANWire.connects(blockAccess, x,y-1,z,meta) || forceDir == Dir.NY;
		boolean py = BlockLANWire.connects(blockAccess, x,y+1,z,meta) || forceDir == Dir.PY;
		boolean nz = BlockLANWire.connects(blockAccess, x,y,z-1,meta) || forceDir == Dir.NZ;
		boolean pz = BlockLANWire.connects(blockAccess, x,y,z+1,meta) || forceDir == Dir.PZ;
		
		final double min = 6/16f, max=10/16f;
		
		if(!nx && !ny && !nz && !px && !py && !pz) {
			addBB(mask,list,x,y,z, AxisAlignedBB.getBoundingBox(min, min, min, max, max, max));
			return;
		}
		
		if(nx || px) {
			addBB(mask,list,x,y,z, AxisAlignedBB.getBoundingBox(nx?0:min, min, min, px?1:max, max, max));
		}
		
		if(ny || py) {
			addBB(mask,list,x,y,z, AxisAlignedBB.getBoundingBox(min, ny?0:min, min, max, py?1:max, max));
		}
		
		if(nz || pz) {
			addBB(mask,list,x,y,z, AxisAlignedBB.getBoundingBox(min, min, nz?0:min, max, max, pz?1:max));
		}
	}
	
	@Override
	public AxisAlignedBB getSelectedBoundingBoxFromPool(World w, int x, int y, int z) {
		final double min = 6/16f, max=10/16f;
		
		int meta = w.getBlockMetadata(x, y, z);
		
		double x1 = BlockLANWire.connects(w, x-1,y,z,meta) ? 0 : min;
		double x2 = BlockLANWire.connects(w, x+1,y,z,meta) ? 1 : max;
		double y1 = BlockLANWire.connects(w, x,y-1,z,meta) ? 0 : min;
		double y2 = BlockLANWire.connects(w, x,y+1,z,meta) ? 1 : max;
		double z1 = BlockLANWire.connects(w, x,y,z-1,meta) ? 0 : min;
		double z2 = BlockLANWire.connects(w, x,y,z+1,meta) ? 1 : max;
		
		return AxisAlignedBB.getBoundingBox(x+x1, y+y1, z+z1, x+x2, y+y2, z+z2);
	}
	
	@Override
	public MovingObjectPosition collisionRayTrace(World w, int x, int y, int z, Vec3 src, Vec3 dst) {
		List<AxisAlignedBB> list = new ArrayList<AxisAlignedBB>();
		addCollidingBlockToList(w, x, y, z, null, list, null);
		
		MovingObjectPosition best = null;
		double best_dist = 0;
		for(AxisAlignedBB bb : list) {
			MovingObjectPosition rt = bb.calculateIntercept(src, dst);
			if(rt == null)
				continue;
			double dist = rt.hitVec.distanceTo(src);
			if(best == null || dist < best_dist) {
				best = rt;
				best_dist = dist;
			}
		}
		
		return best == null ? null : new MovingObjectPosition(x, y, z, best.sideHit, best.hitVec);
	}
	
	@Override
	public boolean isOpaqueCube() {
		return false;
	}
	
	@Override
	public int getRenderType() {
		return renderType == -1 ? BlockPeriphs.model : renderType;
	}
	
	@Override
	public void breakBlock(World par1World, int par2, int par3, int par4, Block par5, int par6) {
		WorldNetworkData.getForWorld(par1World).removeCable(par2, par3, par4);
		super.breakBlock(par1World, par2, par3, par4, par5, par6);
	}
	
	@Override
	public void onBlockAdded(World par1World, int par2, int par3, int par4) {
		WorldNetworkData.getForWorld(par1World).addCable(par2, par3, par4, par1World.getBlockMetadata(par2, par3, par4));
		super.onBlockAdded(par1World, par2, par3, par4);
	}
}

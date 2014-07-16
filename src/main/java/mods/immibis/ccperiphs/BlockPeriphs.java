package mods.immibis.ccperiphs;


import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mods.immibis.ccperiphs.coproc.TileCoprocBase;
import mods.immibis.ccperiphs.lan.TileNIC;
import mods.immibis.ccperiphs.lan.WorldNetworkData;
import mods.immibis.ccperiphs.rfid.TileRFIDWriter;
import mods.immibis.ccperiphs.speaker.TileSpeaker;
import mods.immibis.core.BlockCombined;
import mods.immibis.core.api.util.Dir;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class BlockPeriphs extends BlockCombined {
	
	public static int model;

	public BlockPeriphs() {
		super(Material.iron);
	}
	
	@Override
	public MovingObjectPosition collisionRayTrace(World w, int x, int y, int z, Vec3 src, Vec3 dst) {
		List<AxisAlignedBB> list = new ArrayList<AxisAlignedBB>();
		addCollisionBoxesToList(w, x, y, z, AxisAlignedBB.getBoundingBox(x, y, z, x+1, y+1, z+1), list, null);
		
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
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister reg) {
		RenderUtils.registerTextures(reg);
	}
	
	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess w, int x, int y, int z) {
		if(w.getBlock(x, y, z) != this) {
			setBlockBounds(0, 0, 0, 1, 1, 1);
			return;
		}
		
		EnumPeriphs type = EnumPeriphs.VALUES[w.getBlockMetadata(x, y, z)];
		TileEntity te = w.getTileEntity(x, y, z);
		
		switch(type) {
		case RFID_WRITER:
			if(te == null) return;
			
			switch(((TileRFIDWriter)te).facing) {
			case Dir.PY: setBlockBounds(0, 0, 0, 1, 9/16.0f, 1); break;
			case Dir.PX: setBlockBounds(0, 0, 0, 9/16.0f, 1, 1); break;
			case Dir.NX: setBlockBounds(7/16.0f, 0, 0, 1, 1, 1); break;
			case Dir.PZ: setBlockBounds(0, 0, 0, 1, 1, 9/16.0f); break;
			case Dir.NZ: setBlockBounds(0, 0, 7/16.0f, 1, 1, 1); break;
			}
			break;
		case NIC:
			if(te == null) return;
			
			float ins = 2/16f;
			float thick = 2/16f;
			
			switch(((TileNIC)te).facing) {
			case Dir.PY: setBlockBounds(ins, 1-thick, ins, 1-ins, 1, 1-ins); break;
			case Dir.NY: setBlockBounds(ins, 0, ins, 1-ins, thick, 1-ins); break;
			case Dir.PX: setBlockBounds(1-thick, ins, ins, 1, 1-ins, 1-ins); break;
			case Dir.NX: setBlockBounds(0, ins, ins, thick, 1-ins, 1-ins); break;
			case Dir.PZ: setBlockBounds(ins, ins, 1-thick, 1-ins, 1-ins, 1); break;
			case Dir.NZ: setBlockBounds(ins, ins, 0, 1-ins, 1-ins, thick); break;
			}
			break;
		default:
			setBlockBounds(0, 0, 0, 1, 1, 1);
			break;
		}
	}

	@Override
	public IIcon getIcon(int side, int data) {return null;}
	
	// @Override // client only override
	/*public int getBlockTexture(IBlockAccess w, int x, int y, int z, int side) {
		TilePeriphs t;
		try {
			t = (TilePeriphs)w.getTileEntity(x, y, z);
		} catch(Exception e) {
			return 0;
		}
		
		if(t == null)
			return 0;
		
		return t.getTexture(side);
	}*/
	
	@Override
	public boolean isOpaqueCube() {
        return false;
    }
	
	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}
	
	@Override
	public boolean isBlockSolid(IBlockAccess par1IBlockAccess, int par2, int par3, int par4, int par5) {
		return false;
	}
	
	@Override
	public boolean isSideSolid(IBlockAccess world, int x, int y, int z, ForgeDirection side) {
		switch(EnumPeriphs.VALUES[world.getBlockMetadata(x, y, z)])
		{
		case COPROC_ADVMAP:
		case COPROC_CRYPTO:
			// all sides except front
			return side.ordinal() != ((TileCoprocBase)world.getTileEntity(x, y, z)).facing;
		case SPEAKER:
			// all sides except front
			return side.ordinal() != ((TileSpeaker)world.getTileEntity(x, y, z)).facing;
		case MAG_STRIPE:
		case RFID_READER:
			return true;
		case RFID_WRITER:
			// back only
			return side.ordinal() == (1 ^ ((TileRFIDWriter)world.getTileEntity(x, y, z)).facing);
		case NIC:
			return false;
		}
		return false;
	}
	
	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World par1World, int par2, int par3, int par4) {
		setBlockBoundsBasedOnState(par1World, par2, par3, par4);
		return super.getCollisionBoundingBoxFromPool(par1World, par2, par3, par4);
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void addCollisionBoxesToList(World par1World, int par2, int par3, int par4, AxisAlignedBB par5AxisAlignedBB, List par6List, Entity par7Entity) {
		int meta = par1World.getBlockMetadata(par2, par3, par4);
		if(meta == EnumPeriphs.NIC.ordinal() && ImmibisPeripherals.enableLANRegistration)
			ImmibisPeripherals.lanWire.addCollidingBlockToList(par1World, par2, par3, par4, par5AxisAlignedBB, par6List, par7Entity);
		super.addCollisionBoxesToList(par1World, par2, par3, par4, par5AxisAlignedBB, par6List, par7Entity);
	}
	
	@Override
	public AxisAlignedBB getSelectedBoundingBoxFromPool(World par1World, int par2, int par3, int par4) {
		setBlockBoundsBasedOnState(par1World, par2, par3, par4);
		return super.getSelectedBoundingBoxFromPool(par1World, par2, par3, par4);
	}
	
	@Override
	public int getRenderType() {
		return model;
	}
	
	@Override
	public TileEntity createNewTileEntity(World world, int data) {
		return getBlockEntity(data);
	}

	@Override
	public TileEntity getBlockEntity(int data) {
		return EnumPeriphs.VALUES[data].createTile();
	}

	@Override
	public void getCreativeItems(List<ItemStack> arraylist) {
		for(EnumPeriphs e : EnumPeriphs.VALUES) {
			if(!ImmibisPeripherals.allowAdventureMapInterface && e == EnumPeriphs.COPROC_ADVMAP)
				continue;
			arraylist.add(new ItemStack(this, 1, e.ordinal()));
		}
	}
	
	
	
	@Override
	public void breakBlock(World world, int x, int y, int z, Block par5, int par6) {
		if(world.getBlockMetadata(x, y, z) == EnumPeriphs.NIC.ordinal()) {
			WorldNetworkData.getForWorld(world).removeNIC(x, y, z);
		}
		
		super.breakBlock(world, x, y, z, par5, par6);
	}
	
	@Override
	public void onBlockAdded(World par1World, int par2, int par3, int par4) {
		int meta = par1World.getBlockMetadata(par2, par3, par4); 
		
		if(meta == EnumPeriphs.NIC.ordinal()) {
			WorldNetworkData.getForWorld(par1World).addNIC(par2, par3, par4, 0);
		}
		
		super.onBlockAdded(par1World, par2, par3, par4);
	}
}

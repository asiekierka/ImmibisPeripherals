package mods.immibis.ccperiphs.coproc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.ReflectionHelper;
import dan200.computer.api.IComputerAccess;
import dan200.computer.api.ILuaContext;
import dan200.computer.api.ILuaObject;
import mods.immibis.ccperiphs.ImmibisPeripherals;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.util.FoodStats;
import net.minecraft.util.Vec3;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;

public class TileCoprocAdvMap extends TileCoprocBase {
	
	// All NBT types:
	// compound, list, byte, byteArray, double, float, int, intArray, long, short, string
	
	// converts NBT tags -> lua-readable objects
	private static class LuaNBTObject {
		public String type;
		public Object object;
		
		public LuaNBTObject(NBTBase t) throws Exception {
			if(t instanceof NBTTagCompound) {
				object = new LuaNBTCompound((NBTTagCompound)t);
				type = "compound";
			}
			else if(t instanceof NBTTagList) {
				object = new LuaNBTList((NBTTagList)t);
				type = "list";
			}
			else if(t instanceof NBTTagByte) {
				object = ((NBTTagByte)t).func_150290_f();
				type = "byte";
			}
			else if(t instanceof NBTTagByteArray) {
				object = new LuaNBTArray(((NBTTagByteArray)t).func_150292_c());
				type = "byteArray";
			}
			else if(t instanceof NBTTagDouble) {
				object = ((NBTTagDouble)t).func_150286_g();
				type = "double";
			}
			else if(t instanceof NBTTagFloat) {
				object = ((NBTTagFloat)t).func_150288_h();
				type = "float";
			}
			else if(t instanceof NBTTagInt) {
				object = ((NBTTagInt)t).func_150287_d();
				type = "int";
			}
			else if(t instanceof NBTTagIntArray) {
				object = new LuaNBTArray(((NBTTagIntArray)t).func_150302_c());
				type = "intArray";
			}
			else if(t instanceof NBTTagLong) {
				long l = ((NBTTagLong)t).func_150291_c();
				long high = (l >> 32) & 0xFFFFFFFFL;
				long low = l & 0xFFFFFFFFL;
				// returns {high, low}, "long"
				object = ImmutableMap.<Integer, Double>builder().put(1, (double)high).put(2, (double)low).build();
				type = "long";
			}
			else if(t instanceof NBTTagShort) {
				object = ((NBTTagShort)t).func_150289_e();
				type = "short";
			}
			else if(t instanceof NBTTagString) {
				object = ((NBTTagString)t).func_150285_a_();
				type = "string";
			}
			else
				throw new Exception("unknown nbt tag type: "+t.getClass().getSimpleName());
		}
	}
	
	// lua argument lists -> NBT tags
	private static NBTBase convertArgumentsToTag(Object[] args) throws Exception {
		String type = (String)args[0];
		
		
		// compound, list, byte, byteArray, double, float, int, intArray, long, short, string
		if(type.equals("compound")) {
			return new NBTTagCompound();
			
		} else if(type.equals("list")) {
			return new NBTTagList();
			
		} else if(type.equals("byte")) {
			if(args.length < 2 || !(args[1] instanceof Double))
				throw new Exception("for byte: need a number");
			
			return new NBTTagByte((byte)(double)(Double)args[1]);
			
		} else if(type.equals("byteArray")) {
			if(args.length < 2 || !(args[1] instanceof Double))
				throw new Exception("for byteArray: need a number (size)");
			try {
				return new NBTTagByteArray( new byte[(int)(double)(Double)args[1]]);
			} catch(NegativeArraySizeException e) {
				throw new Exception("size cannot be negative");
			}
			
		} else if(type.equals("double")) {
			if(args.length < 2 || !(args[1] instanceof Double))
				throw new Exception("for double: need a number");
			
			return new NBTTagDouble((Double)args[1]);
			
		} else if(type.equals("float")) {
			if(args.length < 2 || !(args[1] instanceof Double))
				throw new Exception("for float: need a number");
			
			return new NBTTagFloat((float)(double)(Double)args[1]);
			
		} else if(type.equals("int")) {
			if(args.length < 2 || !(args[1] instanceof Double))
				throw new Exception("for int: need a number");
			
			return new NBTTagInt((int)(double)(Double)args[1]);
			
		} else if(type.equals("intArray")) {
			if(args.length < 2 || !(args[1] instanceof Double))
				throw new Exception("for intArray: need a number (size)");
			try {
				return new NBTTagIntArray( new int[(int)(double)(Double)args[1]]);
			} catch(NegativeArraySizeException e) {
				throw new Exception("size cannot be negative");
			}
			
		} else if(type.equals("long")) {
			if(args.length < 3 || !(args[1] instanceof Double) || !(args[2] instanceof Double))
				throw new Exception("for long: need two numbers, high 32 bits first, then low 32 bits");
			long high = (long)(double)(Double)args[1];
			long low = (long)(double)(Double)args[2];
			return new NBTTagLong( low | (high << 32));
		
		} else if(type.equals("short")) {
			if(args.length < 2 || !(args[1] instanceof Double))
				throw new Exception("for short: need a number");
			
			return new NBTTagShort((short)(double)(Double)args[1]);
		
		} else if(type.equals("string")) {
			if(args.length < 2 || !(args[1] instanceof String))
				throw new Exception("for string: need a string");
			
			return new NBTTagString((String)args[1]);
		
		} else {
			throw new Exception("invalid NBT type: "+type+". valid types are: compound, list, byte, byteArray, double, float, int, intArray, long, short, string");
		}
	}
	
	private static class LuaNBTArray implements ILuaObject {
		private final Object ar;
		private final int len;
		private final String type;
		
		public LuaNBTArray(byte[] ar) {
			this.ar = ar;
			len = ar.length;
			type = "byteArray";
		}
		
		public LuaNBTArray(int[] ar) {
			this.ar = ar;
			len = ar.length;
			type = "intArray";
		}
		
		public String[] getMethodNames() {
			return new String[] {
				"getType",
				"getLength",
				"get",
				"set"
			};
		}
		
		@Override
		public Object[] callMethod(ILuaContext ctx, int arg0, Object[] args) throws Exception {
			switch(arg0) {
			case 0:
				return new Object[] {type};
			case 1:
				return new Object[] {len};
			case 2:
				checkArgs(args, Double.class);
				try {
					if(ar instanceof byte[])
						return new Object[] {((byte[])ar)[(int)(double)(Double)args[0]]};
					else
						return new Object[] {((int[])ar)[(int)(double)(Double)args[0]]};
				} catch(ArrayIndexOutOfBoundsException e) {
					throw new Exception("index out of bounds");
				}
			case 3:
				checkArgs(args, Double.class, Double.class);
				try {
					if(ar instanceof byte[])
						((byte[])ar)[(int)(double)(Double)args[0]] = (byte)(double)(Double)args[1];
					else
						((int[])ar)[(int)(double)(Double)args[0]] = (int)(double)(Double)args[1];
				} catch(ArrayIndexOutOfBoundsException e) {
					throw new Exception("index out of bounds");
				}
				return null;
			}
			return null;
		}
	}
	
	private static class LuaNBTList implements ILuaObject {
		private final NBTTagList tag;
		public LuaNBTList(NBTTagList tag) {
			this.tag = tag;
		}
		
		public String[] getMethodNames() {
			return new String[] {
				"getSize",
				"get",
				"add",
				"remove",
				"getType"
			};
		}
		
		private List<NBTBase> getList() {
			return ReflectionHelper.<List<NBTBase>, NBTTagList>getPrivateValue(NBTTagList.class, tag, 0);
		}
		
		@Override
		public Object[] callMethod(ILuaContext ctx, int arg0, Object[] args) throws Exception {
			switch(arg0) {
			case 0: // getSize() -> int
				return new Object[] {tag.tagCount()};
			case 1:
				// get(int index) -> tag
				checkArgs(args, Double.class);
				try {
					return new Object[] { "TODO - Mojang severely broke the API in 1.7.2" };
					//LuaNBTObject o = new LuaNBTObject(tag.tagAt((int)(double)(Double)args[0]));
					//return new Object[] {o.type, o.object};
				} catch(IndexOutOfBoundsException e) {
					throw new Exception("index out of bounds");
				}
			case 2:
				// add(int index, string type, [variant value])
				{
					checkArgs(args, Double.class, String.class);
					
					int index = (int)(double)(Double)args[0];
					
					// strip first argument, convert remaining arguments to tag
					Object[] tagInfo = new Object[args.length - 1];
					System.arraycopy(args, 1, tagInfo, 0, tagInfo.length);
					try {
						getList().add(index, convertArgumentsToTag(tagInfo));
					} catch(IndexOutOfBoundsException e) {
						throw new Exception("index out of bounds");
					}
				}
				return null;
			case 3:
				// remove(int index)
				checkArgs(args, Double.class);
				try {
					getList().remove((int)(double)(Double)args[0]);
				} catch(IndexOutOfBoundsException e) {
					throw new Exception("index out of bounds");
				}
				return null;
			case 4:
				return new Object[] {"list"};
			}
			return null;
		}
	}
	
	private static class LuaNBTCompound implements ILuaObject {
		private final NBTTagCompound tag;
		
		public LuaNBTCompound(NBTTagCompound tag) {
			this.tag = tag;
		}
		
		private Map<String, NBTBase> getMap() {
			return ReflectionHelper.<Map<String, NBTBase>, NBTTagCompound>getPrivateValue(NBTTagCompound.class, tag, 0);
		}
		
		@Override
		public Object[] callMethod(ILuaContext ctx, int arg0, Object[] args) throws Exception {
			switch(arg0) {
			case 0:
				return new Object[] {"compound"};
			case 1:
				{
					checkArgs(args, String.class);
					String key = (String)args[0];
					NBTBase t = tag.getTag(key);
					
					LuaNBTObject o = new LuaNBTObject(t);
					return new Object[] {o.type, o.object};
				}
			case 2:
				// hasKey(string key) -> boolean
				checkArgs(args, String.class);
				return new Object[] {tag.hasKey((String)args[0])};
			case 3:
				// getKeys() -> array of keys
				{
					Map<Integer, String> rv = new HashMap<Integer, String>();
					int k = 1;
					for(String key : (Set<String>)getMap().keySet())
						rv.put(k++, key);
					return new Object[] {rv};
				}
			case 4:
				// setValue(string key, string type, [variant value])
				{
					checkArgs(args, String.class, String.class);
					
					String key = (String)args[0];
					
					// strip first argument, convert remaining arguments to tag
					Object[] tagInfo = new Object[args.length - 1];
					System.arraycopy(args, 1, tagInfo, 0, tagInfo.length);
					tag.setTag(key, convertArgumentsToTag(tagInfo));
				}
				return null;
			case 5:
				// remove(string key)
				checkArgs(args, String.class);
				tag.removeTag((String)args[0]);
				return null;
			}
			return null;
		}
		
		public String[] getMethodNames() {
			return new String[] {
				"getType",
				"getValue",
				"hasKey",
				"getKeys",
				"setValue",
				"remove"
			};
		}
		
		
	}
	
	private static class LuaTileEntity implements ILuaObject {

		private final TileEntity te;
		
		public LuaTileEntity(TileEntity te) {
			this.te = te;
		}
		
		private NBTTagCompound curNBT;
		
		@Override
		public Object[] callMethod(ILuaContext ctx, int arg0, Object[] arg1) throws Exception {
			switch(arg0) {
			case 0:
				return new Object[] {te.getClass().getName()};
			case 1:
				curNBT = new NBTTagCompound();
				te.writeToNBT(curNBT);
				return null;
			case 2:
				if(curNBT == null)
					throw new Exception("no NBT loaded");
				return new Object[] {new LuaNBTCompound(curNBT)};
			case 3:
				if(curNBT == null)
					throw new Exception("no NBT loaded");
				te.readFromNBT(curNBT);
				te.getWorldObj().markBlockForUpdate(te.xCoord, te.yCoord, te.zCoord);
				return null;
			}
			return null;
		}

		public String[] getMethodNames() {
			return new String[] {
				"getClass",
				"readNBT",
				"getNBT",
				"writeNBT"
			};
		}
		
	}
	
	private static class LuaEntity implements ILuaObject {
		
		private final Entity e;
		
		public LuaEntity(Entity e) {
			this.e = e;
		}

		@Override
		public Object[] callMethod(ILuaContext ctx, int arg0, Object[] args) throws Exception {
			try {
				switch(arg0) {
				case 0: // getPosition() -> x, y, z
					return new Object[] {e.posX, e.posY, e.posZ};
				
				case 1: // setPosition(x, y, z)
					checkArgs(args, Double.class, Double.class, Double.class);
					{
						double x = (Double)args[0];
						double y = (Double)args[1];
						double z = (Double)args[2];
						if(e instanceof EntityLivingBase)
							((EntityLivingBase)e).setPositionAndUpdate(x, y, z);
						else
							e.setPosition(x, y, z);
					}
					return null;
					
				case 2: // getWorldID() -> dimID
					return new Object[] {e.worldObj.provider.dimensionId};
					
				case 3: // getLooking() -> x, y, z
					Vec3 v = e.getLookVec();
					return new Object[] {v.xCoord, v.yCoord, v.zCoord};
				}
			} catch(Exception e) {
				//e.printStackTrace();
				throw e;
			}
			return null;
		}

		public String[] getMethodNames() {
			return new String[] {
				"getPosition",
				"setPosition",
				"getWorldID",
				"getLooking"
			};
		}
		
	}
	
	private static class LuaEntityPlayer implements ILuaObject {

		private final EntityPlayer pl;
		
		public LuaEntityPlayer(EntityPlayer pl) {
			this.pl = pl;
		}
		
		@Override
		public Object[] callMethod(ILuaContext ctx, int arg0, Object[] args) throws Exception {
			switch(arg0) {
			case 0: // getUsername() -> string
				return new Object[] {pl.getGameProfile().getName()};
			case 1: // asEntity() -> entity
				return new Object[] {new LuaEntity(pl)};
			case 2: // sendChat(string)
				checkArgs(args, String.class);
				pl.addChatMessage(new ChatComponentText((String)args[0]));
				return null;
			case 3: // getHealth() -> float
				return new Object[] {pl.getHealth()};
			case 4: // getHunger() -> int
				return new Object[] {pl.getFoodStats().getFoodLevel()};
			case 5: // getFoodSaturation() -> int
				return new Object[] {pl.getFoodStats().getSaturationLevel()};
			case 6: // setHunger(int amt)
				checkArgs(args, Double.class);
				ReflectionHelper.setPrivateValue(FoodStats.class, pl.getFoodStats(), (int)(double)(Double)args[0], 0);
				return null;
			case 7: // setFoodSaturation(int amt)
				checkArgs(args, Double.class);
				ReflectionHelper.setPrivateValue(FoodStats.class, pl.getFoodStats(), (float)(double)(Double)args[0], 1);
				return null;
			case 8: // heal(int amt)
				checkArgs(args, Double.class);
				pl.heal((int)(double)(Double)args[0]);
				return null;
			case 9: // damage(int amt) -> boolean success
				checkArgs(args, Double.class);
				return new Object[] {pl.attackEntityFrom(DamageSource.magic, (int)(double)(Double)args[0])};
			case 10: // getGamemode() -> int mode
				if(pl instanceof EntityPlayerMP)
					return new Object[] {((EntityPlayerMP)pl).theItemInWorldManager.getGameType().getID()};
				else
					return null;
			}
			return null;
		}

		public String[] getMethodNames() {
			return new String[] {
				"getUsername",
				"asEntity",
				"sendChat",
				"getHealth",
				"getHunger",
				"getFoodSaturation",
				"setHunger",
				"setFoodSaturation",
				"heal",
				"damage",
				"getGamemode"
			};
		}
		
	}
	
	private static class LuaWorld implements ILuaObject {
		
		private final WorldServer w;
		
		public LuaWorld(WorldServer world) {
			this.w = world;
		}
		
		public String[] getMethodNames() {
			return new String[] {
				"getBiome",
				"getBlockName",
				"getMetadata",
				"getBlockLight",
				"getSkyLight",
				"playSound",
				"explode",
				"getClosestPlayer",
				"isChunkLoaded",
				"setBlock",
				"setBlockWithoutNotify",
				"getTime",
				"setTime",
				"getTileEntity"
			};
		}
		
		@Override
		public Object[] callMethod(ILuaContext ctx, int arg0, Object[] args) throws Exception {
			switch(arg0) {
			
			case 0: // getBiome(int x, int z) -> string biomeName
				checkArgs(args, Double.class, Double.class);
				return new Object[] {w.getBiomeGenForCoords((int)(double)(Double)args[0], (int)(double)(Double)args[1]).biomeName};
				
			case 1: // getBlockName(int x, int y, int z) -> block name
				checkArgs(args, Double.class, Double.class, Double.class);
				return new Object[] {Block.blockRegistry.getNameForObject(w.getBlock((int)(double)(Double)args[0], (int)(double)(Double)args[1], (int)(double)(Double)args[2]))};
			
			case 2: // getMetadata(int x, int y, int z) -> int meta
				checkArgs(args, Double.class, Double.class, Double.class);
				return new Object[] {w.getBlockMetadata((int)(double)(Double)args[0], (int)(double)(Double)args[1], (int)(double)(Double)args[2])};
			
			case 3: // getBlockLight(int x, int y, int z) -> int light
				checkArgs(args, Double.class, Double.class, Double.class);
				return new Object[] {w.getSkyBlockTypeBrightness(EnumSkyBlock.Block, (int)(double)(Double)args[0], (int)(double)(Double)args[1], (int)(double)(Double)args[2])};
			
			case 4: // getSkyLight(int x, int y, int z) -> int light
				checkArgs(args, Double.class, Double.class, Double.class);
				return new Object[] {w.getSkyBlockTypeBrightness(EnumSkyBlock.Sky, (int)(double)(Double)args[0], (int)(double)(Double)args[1], (int)(double)(Double)args[2])};
			
			case 5: // playSound(string sound, double x, double y, double z, float volume, float pitch)
				checkArgs(args, String.class, Double.class, Double.class, Double.class, Double.class, Double.class);
				w.playSoundEffect((Double)args[1], (Double)args[2], (Double)args[3], (String)args[0], (float)(double)(Double)args[4], (float)(double)(Double)args[5]);
				return null;
			
			case 6: // explode(double x, double y, double z, float power, boolean fire, boolean blocks)
				checkArgs(args, Double.class, Double.class, Double.class, Double.class, Boolean.class, Boolean.class);
				w.newExplosion(null, (Double)args[0], (Double)args[1], (Double)args[2], (float)(double)(Double)args[3], (Boolean)args[4], (Boolean)args[5]);
				return null;
			
			case 7: // getClosestPlayer(double x, double y, double z, [double maxDist]) -> player
				checkArgs(args, 3, Double.class, Double.class, Double.class, Double.class);
				EntityPlayer pl = w.getClosestPlayer((Double)args[0], (Double)args[1], (Double)args[2], args.length >= 4 && args[3] != null ? (Double)args[3] : 0);
				if(pl != null)
					return new Object[] {new LuaEntityPlayer(pl)};
				else
					return null;
				
			case 8: // isChunkLoaded(int x, int z) -> boolean
				checkArgs(args, Double.class, Double.class);
				{
					int x = (int)(double)(Double)args[0];
					int z = (int)(double)(Double)args[1];
					return new Object[] {w.blockExists(x<<4, 64, z<<4)};
				}
			
			case 9: // setBlock(int x, int y, int z, String name, int meta)
				checkArgs(args, Double.class, Double.class, Double.class, String.class, Double.class);
				String blockName = (String)args[3];
				w.setBlock(
					(int)(double)(Double)args[0],
					(int)(double)(Double)args[1],
					(int)(double)(Double)args[2],
					Block.getBlockFromName(blockName),
					(int)(double)(Double)args[4] & 15,
					3);
				return null;
			
			case 10: // setBlockWithoutNotify(int x, int y, int z, int ID, int meta)
				checkArgs(args, Double.class, Double.class, Double.class, Double.class, Double.class);
				checkArgs(args, Double.class, Double.class, Double.class, String.class, Double.class);
				String blockName2 = (String)args[3];
				w.setBlock(
					(int)(double)(Double)args[0],
					(int)(double)(Double)args[1],
					(int)(double)(Double)args[2],
					Block.getBlockFromName(blockName2),
					(int)(double)(Double)args[4] & 15,
					2);
				return null;
			
			case 11: // getTime() -> int
				return new Object[] {w.getWorldTime()};
			
			case 12: // setTime(long time)
				checkArgs(args, Double.class);
				w.setWorldTime((long)(double)(Double)args[0]);
				return null;
			
			case 13: // getTileEntity(int x, int y, int z) -> tile entity
				checkArgs(args, Double.class, Double.class, Double.class);
				{
					TileEntity te = w.getTileEntity((int)(double)(Double)args[0], (int)(double)(Double)args[1], (int)(double)(Double)args[2]);
					if(te == null)
						return null;
					else
						return new Object[] {new LuaTileEntity(te)};
				}
			}
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object[] callMethod(IComputerAccess computer, ILuaContext ctx, int method, Object[] args) throws Exception {
		if(!ImmibisPeripherals.allowAdventureMapInterface)
			throw new Exception("Peripheral disabled in config");
		
		switch(method) {
		case 0:
			// getLoadedWorlds() -> list of dim IDs
			{
				int k = 1;
				Map<Integer, Integer> rv = new HashMap<Integer, Integer>();
				for(WorldServer ws : MinecraftServer.getServer().worldServers)
					rv.put(k++, ws.provider.dimensionId);
				return new Object[] {rv};
			}
			
		case 1:
			{
				// getWorld(int dimID) -> world
				checkArgs(args, Double.class);
				int dim = (int)(double)(Double)args[0];
				WorldServer ws = DimensionManager.getWorld(dim);
				if(ws == null)
					return null;
				else
					return new Object[] {new LuaWorld(ws)};
			}
		
		case 2:
			{
				// getOrLoadWorld(int dimID) -> world
				checkArgs(args, Double.class);
				int dim = (int)(double)(Double)args[0];
				WorldServer ws = MinecraftServer.getServer().worldServerForDimension(dim);
				if(ws == null)
					return null;
				else
					return new Object[] {new LuaWorld(ws)};
			}
			
		case 3:
			// getPeripheralPos() -> x, y, z
			return new Object[] {xCoord, yCoord, zCoord};
		
		case 4:
			// getPeripheralWorldID() -> dimID
			return new Object[] {worldObj.provider.dimensionId};
			
		case 5:
			{
				// getPlayerByName(string username) -> player/nil
				checkArgs(args, String.class);
				EntityPlayer pl = MinecraftServer.getServer().getConfigurationManager().getPlayerForUsername((String)args[0]);
				return new Object[] {new LuaEntityPlayer(pl)};
			}
			
		case 6:
			// getPlayerUsernames() -> array of usernames
			{
				Map<Integer, String> rvmap = new HashMap<Integer, String>();
				int k = 1;
				for(EntityPlayer pl2 : (List<EntityPlayer>)MinecraftServer.getServer().getConfigurationManager().playerEntityList)
					rvmap.put(k++, pl2.getGameProfile().getName());
				
				return new Object[] {rvmap};
			}
			
		case 7:
			{
				// getRegisteredWorlds() -> list of dim IDs.
				int k = 1;
				Map<Integer, Integer> rv = new HashMap<Integer, Integer>();
				for(int i : ((Map<Integer, Integer>)ReflectionHelper.getPrivateValue(DimensionManager.class, null, "dimensions")).keySet())
					rv.put(k++, i);
				return new Object[] {rv};
			}
		}
		
		return null;
	}

	private static Set<TileCoprocAdvMap> tiles = new HashSet<TileCoprocAdvMap>();
	@Override
	public void onChunkUnload() {
		tiles.remove(this);
		super.onChunkUnload();
	}
	
	@Override
	public void invalidate() {
		tiles.remove(this);
		super.invalidate();
	}
	
	@Override
	public void validate() {
		tiles.add(this);
		super.validate();
	}

	public String[] getMethodNames() {
		return new String[] {
			"getLoadedWorlds",
			"getWorld",
			"getOrLoadWorld",
			"getPeripheralPos",
			"getPeripheralWorldID",
			"getPlayerByName",
			"getPlayerUsernames",
			"getRegisteredWorlds",
		};
	}

	@Override
	public String getType() {
		return "adventure map interface";
	}

	@SubscribeEvent
	public void onChatEvent(EntityPlayer entityPlayer, String message) {
		queueEvent("chat_message", entityPlayer.getGameProfile().getName(), message);
	}
	
	private void queueEvent(String evt, Object... args) {
		if(!ImmibisPeripherals.allowAdventureMapInterface)
			return;
		for(IComputerAccess c : computers)
			c.queueEvent(evt, args);
	}
	
	public static class AdvMapEventHandler {
		@SubscribeEvent
		public void chatEvent(ServerChatEvent event) {
			if(event.player.worldObj.isRemote) return; // Client?
			for(TileCoprocAdvMap t : TileCoprocAdvMap.tiles)
				t.onChatEvent(event.player, event.message);
		}
		
		@SubscribeEvent
		public void playerRespawn(PlayerEvent.PlayerRespawnEvent event) {
			for(TileCoprocAdvMap t : TileCoprocAdvMap.tiles)
				t.queueEvent("player_respawn", event.player.getGameProfile().getName());
		}
		
		@SubscribeEvent
		public void playerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
			for(TileCoprocAdvMap t : TileCoprocAdvMap.tiles)
				t.queueEvent("player_logout", event.player.getGameProfile().getName());
		}
		
		@SubscribeEvent
		public void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
			for(TileCoprocAdvMap t : TileCoprocAdvMap.tiles)
				t.queueEvent("player_login", event.player.getGameProfile().getName());
		}
		
		@SubscribeEvent
		public void playerChangeDim(PlayerEvent.PlayerChangedDimensionEvent event) {
			for(TileCoprocAdvMap t : TileCoprocAdvMap.tiles)
				t.queueEvent("player_change_world", event.player.getGameProfile().getName());
		}
	}
	
	static {
		MinecraftForge.EVENT_BUS.register(new AdvMapEventHandler());
		FMLCommonHandler.instance().bus().register(new AdvMapEventHandler());
	}

}

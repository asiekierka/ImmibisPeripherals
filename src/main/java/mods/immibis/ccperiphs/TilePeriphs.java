package mods.immibis.ccperiphs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Optional;
import dan200.computer.api.IComputerAccess;
import dan200.computer.api.ILuaContext;
import dan200.computer.api.IMount;
import dan200.computer.api.IPeripheral;
import dan200.computer.api.IWritableMount;
import li.cil.oc.api.FileSystem;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.ManagedPeripheral;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import mods.immibis.core.TileCombined;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

@Optional.InterfaceList({
	@Optional.Interface(modid = "OpenComputers", iface = "li.cil.oc.api.network.Environment"),
	@Optional.Interface(modid = "OpenComputers", iface = "li.cil.oc.api.network.ManagedPeripheral")
})
public abstract class TilePeriphs extends TileCombined implements Environment, IPeripheral, ManagedPeripheral {
	protected final List<String> _methods;
	public final Map<String, FakeComputerAccess> accesses = new HashMap<String, FakeComputerAccess>();
    public Node node;
    protected boolean addedToNetwork = false;
    
	public TilePeriphs() {
		super();
        _methods = Arrays.asList(this.getMethodNames());
        if(Loader.isModLoaded("OpenComputers"))
        	initOC();
	}
	
	@Optional.Method(modid = "OpenComputers")
	public void initOC() {
        node = Network.newNode(this, Visibility.Network).withComponent(this.getType().replace(' ', '_'), Visibility.Network).create();
	}
	
	@Override
	public List<ItemStack> getInventoryDrops() {
		if(!(this instanceof IInventory))
			return super.getInventoryDrops();
		
		IInventory inv = (IInventory)this;
		
		ArrayList<ItemStack> rv = new ArrayList<ItemStack>(inv.getSizeInventory());
		for(int k = 0; k < inv.getSizeInventory(); k++) {
			ItemStack is = inv.getStackInSlot(k);
			if(is != null)
				rv.add(is);
		}
		return rv;
	}
	
	public int getTexture(int side) {return 0;}

	public void onPlacedOnSide(int side) {}
	
	// OpenComputers compatibility
	// Thanks to Sangar for help
    
    @Override
    public Node node() {
        return node;
    }
    
    @Override
    public String[] methods() {
        return this.getMethodNames();
    }

    @Override
    public Object[] invoke(final String method, final Context context, final Arguments args) throws Exception {
        final int index = _methods.indexOf(method);
        if (index < 0) {
            throw new NoSuchMethodException();
        }
        final Object[] argArray = Iterables.toArray(args, Object.class);
        for (int i = 0; i < argArray.length; ++i) {
            if (argArray[i] instanceof byte[]) {
                argArray[i] = new String((byte[]) argArray[i], "UTF-8");
            }
        }
        final FakeComputerAccess access;
        if (accesses.containsKey(context.node().address())) {
            access = accesses.get(context.node().address());
        } else {
            // The calling contexts is not visible to us, meaning we never got
            // an onConnect for it. Create a temporary access.
            access = new FakeComputerAccess(this, context);
        }
        return this.callMethod(access, UnsupportedLuaContext.instance(), index, argArray);
    }
    
    @Override
    public void onConnect(final Node node) {
        if (node.host() instanceof Context) {
            final FakeComputerAccess access = new FakeComputerAccess(this, (Context) node.host());
            accesses.put(node.address(), access);
            this.attach(access);
        }
    }

    @Override
    public void onDisconnect(final Node node) {
        if (node.host() instanceof Context) {
            final FakeComputerAccess access = accesses.remove(node.address());
            if (access != null) {
                this.detach(access);
            }
        } else if (node == this.node) {
            for (FakeComputerAccess access : accesses.values()) {
                this.detach(access);
                access.close();
            }
            accesses.clear();
        }
    }
    @Override
    public void onMessage(final Message message) {
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (!addedToNetwork) {
            addedToNetwork = true;
            Network.joinOrCreateNetwork(this);
        }
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        if (node != null) node.remove();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (node != null) node.remove();
    }

    // ----------------------------------------------------------------------- //

    @Override
    public void readFromNBT(final NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        // The host check may be superfluous for you. It's just there to allow
        // some special cases, where getNode() returns some node managed by
        // some other instance (for example when you have multiple internal
        // nodes in this tile entity).
        if (node != null && node.host() == this) {
            // This restores the node's address, which is required for networks
            // to continue working without interruption across loads. If the
            // node is a power connector this is also required to restore the
            // internal energy buffer of the node.
            node.load(nbt.getCompoundTag("oc:node"));
        }
    }

    @Override
    public void writeToNBT(final NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        // See readFromNBT() regarding host check.
        if (node != null && node.host() == this) {
            final NBTTagCompound nodeNbt = new NBTTagCompound();
            node.save(nodeNbt);
            nbt.setTag("oc:node", nodeNbt);
        }
    }
    
    private static class FakeComputerAccess implements IComputerAccess {
        protected final TilePeriphs owner;
        protected final Context context;
        protected final Map<String, li.cil.oc.api.network.ManagedEnvironment> fileSystems = new HashMap<String, li.cil.oc.api.network.ManagedEnvironment>();

        public FakeComputerAccess(final TilePeriphs owner, final Context context) {
            this.owner = owner;
            this.context = context;
        }

        public void close() {
            for (li.cil.oc.api.network.ManagedEnvironment fileSystem : fileSystems.values()) {
                fileSystem.node().remove();
            }
            fileSystems.clear();
        }

        @Override
        public String mount(final String desiredLocation, final IMount mount) {
            if (fileSystems.containsKey(desiredLocation)) {
                return null;
            }
            return mount(desiredLocation, FileSystem.asManagedEnvironment(FileSystem.fromComputerCraft(mount)));
        }

        @Override
        public String mountWritable(final String desiredLocation, final IWritableMount mount) {
            if (fileSystems.containsKey(desiredLocation)) {
                return null;
            }
            return mount(desiredLocation, FileSystem.asManagedEnvironment(FileSystem.fromComputerCraft(mount)));
        }

        private String mount(final String path, final li.cil.oc.api.network.ManagedEnvironment fileSystem) {
            fileSystems.put(path, fileSystem);
            context.node().connect(fileSystem.node());
            return path;
        }

        @Override
        public void unmount(final String location) {
            final li.cil.oc.api.network.ManagedEnvironment fileSystem = fileSystems.remove(location);
            if (fileSystem != null) {
                fileSystem.node().remove();
            }
        }

        @Override
        public int getID() {
            return context.node().address().hashCode();
        }

        @Override
        public void queueEvent(final String event, final Object[] arguments) {
            context.signal(event, arguments);
        }

        @Override
        public String getAttachmentName() {
            return owner.node().address();
        }
    }
    
    private final static class UnsupportedLuaContext implements ILuaContext {
        protected static final UnsupportedLuaContext Instance = new UnsupportedLuaContext();

        private UnsupportedLuaContext() {
        }

        public static UnsupportedLuaContext instance() {
            return Instance;
        }

        @Override
        public Object[] pullEvent(final String filter) throws Exception {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object[] pullEventRaw(final String filter) throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object[] yield(final Object[] arguments) throws InterruptedException {
            throw new UnsupportedOperationException();
        }
    }
}

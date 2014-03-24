package mods.immibis.ccperiphs.smartcard;


/*import java.io.File;

import mods.immibis.ccperiphs.ImmibisPeripherals;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class SmartCardComputer implements IComputerEnvironment {
	
	Computer computer;
	private World world;
	
	SmartCardComputer(World world, NBTTagCompound tag, TileSCInterface tile) {
		this.world = world;
		this.computer = new Computer(this, new SmartCardTerminal(tile));
		if(tag != null)
			computer.readFromNBT(tag);
	}

	@Override
	public long getComputerSpaceLimit() {
		return 65536;
	}

	@Override
	public String getDescription() {
		return "Smartcard";
	}

	@Override
	public String getLabel(int arg0) {
		return ItemComputer.getComputerLabelOnServer(computer.getID());
	}

	@Override
	public File getLoadingJar() {
		return null;
	}

	@Override
	public File getSaveDir() {
		return ComputerCraft.getWorldDir(world);
	}

	@Override
	public File getStaticDir() {
		return ImmibisPeripherals.scBaseDir;
	}

	@Override
	public double getTimeOfDay() {
		return ((world.getWorldTime() + 6000) % 24000) / 1000.0;
	}

	@Override
	public boolean isHTTPEnabled() {
		return false;
	}

	@Override
	public void setLabel(int arg0, String arg1) {
		ItemComputer.setComputerLabelOnServer(computer.getID(), arg1);
	}

	@Override
	public boolean isColour() {
		return false;
	}

	@Override
	public int getDay() {
		return 0;
	}

}*/
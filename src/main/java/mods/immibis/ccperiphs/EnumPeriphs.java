package mods.immibis.ccperiphs;


import java.lang.reflect.InvocationTargetException;
//import immibis.ccperiphs.smartcard.TileSCInterface;

import mods.immibis.ccperiphs.coproc.TileCoprocAdvMap;
import mods.immibis.ccperiphs.coproc.TileCoprocCrypto;
import mods.immibis.ccperiphs.lan.TileNIC;
import mods.immibis.ccperiphs.rfid.TileMagStripe;
import mods.immibis.ccperiphs.rfid.TileRFIDReader;
import mods.immibis.ccperiphs.rfid.TileRFIDWriter;
import mods.immibis.ccperiphs.speaker.TileSpeaker;

public enum EnumPeriphs {
	RFID_WRITER("rfidwriter", TileRFIDWriter.class),
	RFID_READER("rfidreader", TileRFIDReader.class),
	MAG_STRIPE("magreader", TileMagStripe.class),
	SPEAKER("speaker", TileSpeaker.class),
	NIC("nic", TileNIC.class),
	COPROC_CRYPTO("crypto", TileCoprocCrypto.class),
	COPROC_ADVMAP("ami", TileCoprocAdvMap.class)
	//TAPE_DRIVE("Tape drive", TileTapeDrive.class),
	//SC_WRITER("Smartcard reader", TileSCInterface.class),
	;
	
	private EnumPeriphs(String name, Class<? extends TilePeriphs> teclass) {
		this.name = name;
		this.teclass = teclass;
	}
	
	public final String name;
	public final Class<? extends TilePeriphs> teclass;
	
	public TilePeriphs createTile() {
		try {
			return teclass.getConstructor().newInstance();
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static final EnumPeriphs[] VALUES = values(); 
}

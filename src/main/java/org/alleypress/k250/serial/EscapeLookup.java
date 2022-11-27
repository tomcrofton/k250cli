package org.alleypress.k250.serial;

import java.util.HashMap;

public class EscapeLookup {
	private static HashMap<Byte,Byte> ESC_MAP = new HashMap<Byte,Byte>();
	private static HashMap<Byte,Byte> INV_MAP = new HashMap<Byte,Byte>();

	static {
		put((byte)0x10, (byte)0x10);
		put((byte)0x16, (byte)0x30);
		put((byte)0x2c, (byte)0x31);
		put((byte)0x58, (byte)0x32);
		put((byte)0xb0, (byte)0x33);
		put((byte)0x61, (byte)0x34);
		put((byte)0xc2, (byte)0x35);
		put((byte)0x85, (byte)0x36);
		put((byte)0x0b, (byte)0x37);
	}
	
    private static void put(Byte orig,Byte escaped) {
    	ESC_MAP.put(orig,escaped);
    	INV_MAP.put(escaped,orig);
    }

    public static byte getEscaped(Byte b) {
    	return ESC_MAP.get(b);
    }
    
    public static byte getOriginal(Byte b) {
    	return INV_MAP.get(b);
    }
    
    public static boolean needsEscaping(Byte b) {
    	return ESC_MAP.containsKey(b);
    }

}

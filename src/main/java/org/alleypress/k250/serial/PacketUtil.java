package org.alleypress.k250.serial;

import java.util.ArrayList;

import com.google.common.primitives.Bytes;

public class PacketUtil {
	
	public static int PACKET_DATA_SIZE_MAX = 512;
		
	public static RawPacket newLoopbackStartPacket(int size) {
		if (size<0 || size > PACKET_DATA_SIZE_MAX)
			size=PACKET_DATA_SIZE_MAX;

		//loop start is 4 bytes, last 2 are the size
		byte[] lbData = new byte[] {0x00,0x17,0x00,0x00};
		lbData[2] = (byte)(((size&0xff00)>>8)&0xff);
		lbData[3] = (byte)(size&0xff);	
		
		return buildPacket(lbData);
	}
	
	public static RawPacket buildPacket(byte[] ba) {
		if (ba.length > PACKET_DATA_SIZE_MAX)
			throw new IllegalArgumentException("byte array is too long");

		//always start with 10,02
		RawPacket result = new RawPacket();		
		result.addByte((byte)0x10);
		result.addByte((byte)0x02);

		int size = ba.length;
		byte hiB = (byte)(((size&0xff00)>>8)&0xff);
		byte loB = (byte)(size&0xff);
		
		addByteWithEscape(result,hiB);
		addByteWithEscape(result,loB);

		short checkSum=0;
		for (byte x:ba) {
			addByteWithEscape(result,x);
			checkSum+=(x&0x00FF);
		}

		hiB = (byte)(((checkSum&0xff00)>>8)&0xff);
		loB = (byte)(checkSum&0xff);
		
		addByteWithEscape(result,hiB);
		addByteWithEscape(result,loB);

		return result;
	}
	
	private static void addByteWithEscape(RawPacket p, byte b) {
		if (EscapeLookup.needsEscaping(b)) {
			p.addByte((byte)0x10);
			p.addByte(EscapeLookup.getEscaped(b));
		} else {
			p.addByte(b);
		}		
	}
	
	public static String bytesToHex(byte[] ba) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < ba.length; i++) {
			result.append(String.format("%02X ", ba[i]));
		}
		return result.toString();
	}
	
	public static boolean verifyCheckSum(RawPacket pkt) {
		short myCheckSum=0;
		short givenCheckSum=0;
		
		byte[] p = pkt.asByteArray();
		int index=2;

		int dataSize = 0;
		
		byte x = p[index++]; //size high
		if (x==0x10) {
			x = EscapeLookup.getOriginal(p[index++]);
		}
		dataSize=x&0xFF;

		x = p[index++]; //size low
		if (x==0x10) {
			x = EscapeLookup.getOriginal(p[index++]);
		}
	    dataSize = (dataSize<<8)+(x&0xFF);

	    while (dataSize>0) {
			x = p[index++]; 
			if (x==0x10) {
				x = EscapeLookup.getOriginal(p[index++]);
			}
			myCheckSum+=(x&0x00FF);
			dataSize--;
	    }
		x = p[index++]; //check sum size high
		if (x==0x10) {
			x = EscapeLookup.getOriginal(p[index++]);
		}
		givenCheckSum=(short)(x&0x00ff);
		givenCheckSum = (short)(givenCheckSum<<8);
		x = p[index++]; //check sum low5
		if (x==0x10) {
			x = EscapeLookup.getOriginal(p[index++]);
		}
		givenCheckSum = (short)(givenCheckSum+(x&0xff));
		return (myCheckSum==givenCheckSum);
	}
	
	public static byte[] unpack(RawPacket packet) {
		byte[] p = packet.asByteArray();
		ArrayList<Byte> result = new ArrayList<Byte>();
		int index=2;
		int dataSize = 0;
		
		byte x = p[index++]; //size high
		if (x==0x10) {
			x = EscapeLookup.getOriginal(p[index++]);
		}
		dataSize=x&0xFF;

		x = p[index++]; //size low
		if (x==0x10) {
			x = EscapeLookup.getOriginal(p[index++]);
		}
	    dataSize = (dataSize<<8)+(x&0xFF);

	    while (dataSize>0) {
			x = p[index++]; //size low
			if (x==0x10) {
				x = EscapeLookup.getOriginal(p[index++]);
			}
			result.add(x);
			dataSize--;
	    }
		
		return Bytes.toArray(result);
	}

	public static int getDataSize(RawPacket packet) {
		byte[] p = packet.asByteArray();
		int index=2;
		int dataSize = 0;
		
		byte x = p[index++]; //size high
		if (x==0x10) {
			x = EscapeLookup.getOriginal(p[index++]);
		}
		dataSize=x&0xFF;

		x = p[index++]; //size low
		if (x==0x10) {
			x = EscapeLookup.getOriginal(p[index++]);
		}
	    dataSize = (dataSize<<8)+(x&0xFF);
		
		return dataSize;
	}
}

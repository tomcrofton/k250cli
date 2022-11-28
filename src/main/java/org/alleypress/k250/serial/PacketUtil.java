package org.alleypress.k250.serial;

import java.util.ArrayList;
import java.util.List;

import com.google.common.primitives.Bytes;


public class PacketUtil {
	
	public static int PACKET_DATA_SIZE_MAX = 512;
	
	public static int getDataSize(RawPacket packet) {
		List<Byte> rawData = packet.asByteList();
		if (rawData.size()<4) return -1;
		
		int index=2;
		
		byte x = rawData.get(index++);
		if (x == 0x10) {
			x = rawData.get(index++);
			x = EscapeLookup.getOriginal(x);
		}
		int result=Byte.toUnsignedInt(x);
		x = rawData.get(index++);
		if (x == 0x10) {
			x = rawData.get(index++);
			x = EscapeLookup.getOriginal(x);
		}
		result=(result<<8)+Byte.toUnsignedInt(x);
		return result;
	}	
	
	
	public static RawPacket newLoopbackPacket(int size) {
		if (size<0 || size > PACKET_DATA_SIZE_MAX)
			size=PACKET_DATA_SIZE_MAX;
		
		ArrayList<Byte> block = new ArrayList<Byte>();
		byte x=0;
		for (int i=0;i<size;i++) {
			block.add(x++);
		}
		return buildPacket(Bytes.toArray(block));
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
	
	public static byte[] getDataBytes(RawPacket packet) {
		ArrayList<Byte> result = new ArrayList<Byte>();
		
		int bytesToRead=getDataSize(packet);
		int index=4;
		
		List<Byte> rawData = packet.asByteList();
		while (bytesToRead>0) {
			byte b = rawData.get(index++);
			if (b==0x10) {
				b = EscapeLookup.getOriginal(rawData.get(index++));
			}
			result.add(b);
			bytesToRead--;
		}
		return Bytes.toArray(result);
	}	

}

package org.alleypress.k250.serial;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.google.common.primitives.Bytes;

public class RawPacket implements Serializable {

	private static final long serialVersionUID = 1987L;

	private List<Byte> rawData= new ArrayList<Byte>();
	
	public RawPacket() {
	}

	public RawPacket(byte[] bytes) {
		for (byte b:bytes) {
			rawData.add(b);
		}
	}
	
	public void addByte(byte b) {
		rawData.add(b);
	}
	
	public void setBytes(List<Byte> data) {
		rawData=data;
	}
	
	public List<Byte> asByteList() {
		return new ArrayList<Byte>(rawData);
	}
	
	public byte[] asByteArray() {
		return Bytes.toArray(rawData);
	}
}

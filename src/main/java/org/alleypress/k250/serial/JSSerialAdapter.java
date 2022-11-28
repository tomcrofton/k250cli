package org.alleypress.k250.serial;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

import com.fazecast.jSerialComm.SerialPort;

public class JSSerialAdapter implements SerialAdapter {
	private SerialPort port=null;
	@Override
	public String[] getPortNames() {
		SerialPort[] ports = SerialPort.getCommPorts();
		ArrayList<String> list = new ArrayList<String>();		
        for (SerialPort port : ports) {
            list.add(port.getSystemPortName());
        }
        return list.toArray(new String[list.size()]);
	}

	@Override
	public void selectPort(String portName) throws SerialException {
		port=null;
		if (StringUtils.isBlank(portName)) {
			return;
		}
		SerialPort[] allPorts = SerialPort.getCommPorts();
		for (SerialPort p:allPorts) {
			if (portName.equals(p.getSystemPortName())) {
				port = p;
			}
		}
		if (port==null) {
			throw new SerialException("Unknown Port "+portName);
		}
        port.setBaudRate(115200);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 200, 0);
	}

	private void assertPortSelected() throws SerialException {
		if (port==null) 
			throw new SerialException("No Port Selected");
	}
	
	@Override
	public String getAdapterInfo() throws SerialException {
		assertPortSelected();

		port.openPort();
		DataInputStream ins = new DataInputStream(port.getInputStream());
		DataOutputStream outs = new DataOutputStream(port.getOutputStream());
		String result = null;

		try {
			outs.write(63); // '?'
			result = readForChar((char)0x0a,ins);
		} catch (IOException e) {
			throw new SerialException(e);
		}
		port.closePort();
		return result;
	}
	
	private String readForChar(char c,DataInputStream ins) throws IOException {
		StringBuilder bufr = new StringBuilder();
		boolean notDone = true;
		while (notDone) {// read all bytes
			char b = (char) ins.read();
			if (b == c)
				notDone = false;
			else
				bufr.append(b);
		}		
		return bufr.toString();
	}

	private void sendBegin(DataInputStream ins,DataOutputStream out) throws SerialException {
		sendChar(ins,out,'B');
	}

	private void sendPacket(DataInputStream ins,DataOutputStream out, RawPacket packet) throws SerialException {
		try {
			out.write('P');
			out.write(packet.asByteArray());
			String s = readForChar('<',ins);
			if (!"OK".equals(s)) {
				throw new SerialException("Expecting OK, received "+s);
			}
		} catch (IOException e) {
			throw new SerialException(e.getMessage());
		}
	}

	private RawPacket getPacket(DataInputStream ins,DataOutputStream out) throws SerialException {
    	//a packet is 0x10,0x02,SizeH,SizeL,databytes,ChkSumH,ChkSumL
    	RawPacket p = new RawPacket();
		try {
			out.write('G');
			byte b=(byte)ins.read(); //should be 0x10
			p.addByte(b);
			b=(byte)ins.read(); //should be 0x02
			p.addByte(b);
			
			int dataSize = 0;
	
			b=(byte)ins.read(); //size high
			p.addByte(b);
			if (b==0x10) {
				b=(byte)ins.read();
				p.addByte(b);
				b=EscapeLookup.getOriginal(b);
			}
			dataSize=b;

			b=(byte)ins.read();  //size low
			p.addByte(b);
			if (b==0x10) {
				b=(byte)ins.read();
				p.addByte(b);
				b=EscapeLookup.getOriginal(b);
			}
		    dataSize = (dataSize<<8)+b;		    
		    dataSize+=2; // add on checksum

		    if (dataSize>512) dataSize=10; //something is wrong

		    while (dataSize>0) {
				b=(byte)ins.read();
				p.addByte(b);
		        // don't count the escp char
				if (b==0x10) {
					b=(byte)ins.read();
					p.addByte(b);
				}
		        dataSize--;
		    }
		} catch (IOException e) {
			throw new SerialException(e.getMessage());
		}
		return p;
	}

	
	
	private void sendOK(DataInputStream ins,DataOutputStream out) throws SerialException {
		sendChar(ins,out,'K');
	}
	
	private void sendChar(DataInputStream ins,DataOutputStream out,char c) throws SerialException {
		try {
			out.write(c);
			String s = readForChar('<',ins);
			if (!"OK".equals(s)) {
				throw new SerialException("Expecting OK, received "+s);
			}
		} catch (IOException e) {
			throw new SerialException(e.getMessage());
		}	
	}
	
	@Override
	public void sendReset() throws SerialException {
		assertPortSelected();
		port.openPort();
		DataInputStream ins = new DataInputStream(port.getInputStream());
		DataOutputStream outs = new DataOutputStream(port.getOutputStream());			
		sendChar(ins,outs,'R');
		port.closePort();
	}
	

	@Override
	public String getConfig() throws SerialException {
		assertPortSelected();
		port.openPort();
		DataInputStream ins = new DataInputStream(port.getInputStream());
		DataOutputStream outs = new DataOutputStream(port.getOutputStream());
		sendBegin(ins,outs);
		sendPacket(ins, outs, K250Commands.GET_CONFIG);
		RawPacket rp = getPacket(ins, outs);
		sendOK(ins,outs);
		port.closePort();
		
		StringBuilder sb = new StringBuilder();
		for (byte b:rp.asByteArray()) {
			sb.append(String.format("%02x ", b));
		}
		return sb.toString();
	}

	@Override
	public String loopTest(int packets, int size) throws SerialException {
		assertPortSelected();
		StringBuilder sb = new StringBuilder();
		
		RawPacket loopPacket = PacketUtil.newLoopbackPacket(size);
		
		port.openPort();
		DataInputStream ins = new DataInputStream(port.getInputStream());
		DataOutputStream outs = new DataOutputStream(port.getOutputStream());
		sendBegin(ins,outs);

		for (int i=0;i<size;i++) {
			sendPacket(ins, outs, loopPacket);
			RawPacket rp = getPacket(ins, outs);
			sb.append(" P:").append(rp.asByteArray().length);
			sendOK(ins,outs);
		}

		sendChar(ins,outs,'E');

		port.closePort();
		sb.append(" total:").append(size);
		return sb.toString();
	}
}

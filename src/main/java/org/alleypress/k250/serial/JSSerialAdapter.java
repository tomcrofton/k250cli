package org.alleypress.k250.serial;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

import com.fazecast.jSerialComm.SerialPort;

public class JSSerialAdapter implements SerialAdapter {
	private SerialPort port=null;
	private DataInputStream ins = null;
	private DataOutputStream outs = null;
	private ProgressWatcher progress = null;
	
	@Override
	public void setProgressWatcher(ProgressWatcher watcher) {
		progress = watcher;
	}
	
	private void beginProgress() {
		if (progress!=null)
			progress.begin();
	}

	private void endProgress() {
		if (progress!=null)
			progress.end();
	}

	private void setProgress(double d) {
		if (progress!=null)
			progress.setProgress(d);
	}

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
		port = null;
		ins = null;
		outs = null;
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

	private void openPort() throws SerialException {
		if (port==null) 
			throw new SerialException("No Port Selected");
		port.openPort();
		ins = new DataInputStream(port.getInputStream());
		outs = new DataOutputStream(port.getOutputStream());			
	}
	
	private void closePort() {
		try {
			if (ins!=null) ins.close();
		} catch (IOException e) {
			//ignore
		}
		try {
			if (outs!=null) outs.close();
		} catch (IOException e) {
			//ignore
		}
		port.closePort();
	}

	private String readForChar(char c) throws IOException {
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
	
	@Override
	public String getAdapterInfo() throws SerialException {
		openPort();
		String result = null;
		try {
			outs.write(63); // '?'
			result = readForChar('<');
		} catch (IOException e) {
			throw new SerialException(e);
		}
		closePort();
		return result;
	}
	
	private void sendBegin() throws SerialException {
		sendChar('B');
	}

	private void sendPacket(RawPacket packet) throws SerialException {
		try {
			outs.write('P');
			outs.write(packet.asByteArray());
			String s = readForChar('<');
			if (!"OK".equals(s)) {
				throw new SerialException("Expecting OK, received "+s);
			}
		} catch (IOException e) {
			throw new SerialException(e.getMessage());
		}
	}

	private RawPacket getPacket() throws SerialException {
    	//a packet is 0x10,0x02,SizeH,SizeL,databytes,ChkSumH,ChkSumL
    	RawPacket p = new RawPacket();
		try {
			outs.write('G');
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
			dataSize=b&0xff;
			dataSize=dataSize<<8;

			b=(byte)ins.read();  //size low
			p.addByte(b);
			if (b==0x10) {
				b=(byte)ins.read();
				p.addByte(b);
				b=EscapeLookup.getOriginal(b);
			}
		    dataSize = dataSize+(b&0xff);		    
		    if (dataSize>512) {
		    	throw new SerialException("Received data size of "+dataSize);
		    }
		    dataSize+=2; // add on checksum

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
	
	private void sendChar(char c) throws SerialException {
		try {
			outs.write(c);
			String s = readForChar('<');
			if (!"OK".equals(s)) {
				throw new SerialException("Expecting OK, received "+s);
			}
		} catch (IOException e) {
			throw new SerialException(e.getMessage());
		}	
	}
	
	@Override
	public void sendReset() throws SerialException {
		openPort();
		sendChar('R');
		closePort();
	}
	
	@Override
	public byte[] getConfig() throws SerialException {
		openPort();
		sendBegin();
		sendPacket(K250Commands.GET_CONFIG);
		RawPacket rp = getPacket();
		closePort();
		return PacketUtil.unpack(rp);
	}

	@Override
	public String loopTest(int packets, int size) throws SerialException {
		openPort();		
		sendBegin();
		beginProgress();
		RawPacket loopStartPacket = PacketUtil.newLoopbackStartPacket(size);
		//System.out.print("Loopback Test.");
		sendPacket(loopStartPacket);

		for (int i=0;i<packets;i++) {
			RawPacket p = getPacket();
			sendPacket(p);
//			sendChar('l');
			setProgress(((double)i)/packets);
			delay(3);
		}
		endProgress();
		delay(5);
		sendChar('E');
		closePort();
		return "OK";
	}
	
	
	private static void delay(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void echoTest(int packetLength) throws SerialException {
		byte[] data = new byte[packetLength];
		for (int i=0;i<data.length;i++)
			data[i]=(byte)i;
		RawPacket rp = PacketUtil.buildPacket(data);
		openPort();
		try {
		outs.write('e');
		outs.write(rp.asByteArray());

		int i=3*rp.asByteArray().length;
		while (i-->0) {
			System.out.print((char)ins.read());
			if (i%90==0)
				System.out.println();
		}
		
		} catch (IOException e) {
			throw new SerialException(e);
		}
		closePort();
	}
	
	@Override
	public void saveDigitizer(File f) throws SerialException, IOException {
		openPort();
		sendBegin();
		sendPacket(K250Commands.GET_DIGI1);
		RawPacket rp = getPacket();
		byte[] data = PacketUtil.unpack(rp); // 4 bytes of a 32 bit unsigned int
		int dataSize = ((0xFF & data[0]) << 24) | ((0xFF & data[1]) << 16) |((0xFF & data[2]) << 8) | (0xFF & data[3]);		
		System.out.println("File Size: "+dataSize);
		if (dataSize<1) {
			System.err.println("No data available");
			return;
		}
		beginProgress();
		FileOutputStream fos = new FileOutputStream(f);
		ObjectOutputStream fout = new ObjectOutputStream(fos);
		
		fout.writeObject(K250File.DIGI);
		fout.writeInt(dataSize);

		int readSize=0;
		boolean keepGoing=true;
		while (keepGoing) {
			rp = getPacket();
			fout.writeObject(rp);
			readSize+=PacketUtil.getDataSize(rp);
			setProgress(((double)readSize)/dataSize);
			if (dataSize-readSize<512) {
				//should be zero, but have seen different 
				keepGoing=false;
			}
		}
		if (dataSize!=readSize) {
			System.err.println("WARN: dataSize-readSize="+(dataSize-readSize));
		}
		endProgress();
		System.out.println("Size at end="+readSize);
		fout.close();
		fos.close();
		closePort();
	}

	@Override
	public void loadDigitizer(File f) throws SerialException, IOException {
		FileInputStream fis = new FileInputStream(f);
		ObjectInputStream dataIn = new ObjectInputStream(fis);
		int dataSize=0;
		int readSize=0;
		try {
			K250File k2f = (K250File)dataIn.readObject();
			if (!K250File.DIGI.equals(k2f)) {
				System.err.println(f.getName()+" is not a digitizer file");
				return;
			}
			dataSize = dataIn.readInt();
			openPort();
			sendBegin();
			beginProgress();
		
			sendPacket(K250Commands.SET_DIGI1);
			RawPacket rp = getPacket(); //returns 4 bytes all zeros
			
			boolean keepGoing=true;
			while (keepGoing) {
				rp = (RawPacket)dataIn.readObject();
				sendPacket(rp);
				int packetSize=PacketUtil.getDataSize(rp);
				readSize+=packetSize;
				setProgress(((double)readSize)/dataSize);
				if (dataSize-readSize<512 && packetSize<512) {
					//should be zero, but have seen different 
					keepGoing=false;
				}
			}
			endProgress();
		} catch (ClassNotFoundException e) {
			throw new IOException(e);
		} finally {
			dataIn.close();
			fis.close();
		}
	}
}

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
		StringBuilder bufr = new StringBuilder();

		try {
			outs.write(63); // '?'

			boolean notDone = true;
			while (notDone) {// read all bytes
				char b = (char) ins.read();
				if (b == 0x0a)
					notDone = false;
				else
					bufr.append(b);
			}
		} catch (IOException e) {
			throw new SerialException(e);
		}
		port.closePort();
		return bufr.toString().trim();
	}

}

package org.alleypress.k250.cli;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.alleypress.k250.serial.ConsoleProgressWatcher;
import org.alleypress.k250.serial.JSSerialAdapter;
import org.alleypress.k250.serial.K250Commands;
import org.alleypress.k250.serial.K250File;
import org.alleypress.k250.serial.PacketUtil;
import org.alleypress.k250.serial.SerialAdapter;
import org.alleypress.k250.serial.SerialException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;

public class K250Cli {

	private Options options = new Options();
	private Set<K250File> fileTypes = EnumSet.allOf(K250File.class);
	
	public K250Cli() {
		options.addOption("h", "help", false, "Print this help screen.")
		.addOption("l", "list", false, "List serial ports")
		.addOption("p", "port", true, "Serial port to use")
		.addOption("i","info", false,"Show connected interface adapter info")
		.addOption("f","file", true,"input or output file name <filename>")
		.addOption("x","loop",true,"loopback test <packets,size>")
		.addOption("e","echo",true,"local echo <packet size>")
		.addOption("c","config", false,"Get Config")
		.addOption("g", "get", true, "Get File "+fileTypes.toString().toLowerCase())
		.addOption("s", "send", true, "Send File "+fileTypes.toString().toLowerCase());
	}
	
	public void process(String[] args) throws SerialException, IOException {
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		try { 
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.err.println(e.getMessage());
			return;
		}
		if (cmd.hasOption('h') || (cmd.getOptions().length==0)) {
			showHelp();
			return;
		}
		SerialAdapter sa = new JSSerialAdapter();
		sa.setProgressWatcher(new ConsoleProgressWatcher());

		if (cmd.hasOption('l')) {
			String[] names = sa.getPortNames();
			if (names.length==0) {
				System.out.println("No ports found");
			} else {
				for (String s:names) {
					System.out.println(s);
				}
			}
			return;
		}
		
		//from here out you need the serial port
		
		if (cmd.hasOption('p')) {
			String prt = cmd.getOptionValue('p');
			if (StringUtils.isBlank(prt)) {
				System.err.println("Port name not provided");
				return;
			}
			sa.selectPort(prt);
		} else {
			String[] names = sa.getPortNames();
			if (names.length==0) {
				System.err.println("No ports found");
				return;
			}
			if (names.length>1) {
				System.err.println("Need to specify serial port");
				return;
			}
			sa.selectPort(names[0]);
		}
		
		//from here on, a serial port is selected
		
		if (cmd.hasOption('i')) {
			System.out.println(sa.getAdapterInfo());
			return;
		}		
		
		if (cmd.hasOption('e')) {
			String vals = cmd.getOptionValue('e');
			if (StringUtils.isBlank(vals) || (!Pattern.compile("^\\d{1,3}$").matcher(vals).find())) {
				System.err.println("need to specify packet size (2-512)");
				return;
			}
			int v = Integer.valueOf(vals);
			if (v<2) v=2;
			if (v>512) v=512;
			sa.echoTest(v);
			return;
		}		
		
		
		if (cmd.hasOption('x')) {
			String vals = cmd.getOptionValue('x');
			if (StringUtils.isBlank(vals) || (!Pattern.compile("^\\d{1,2},\\d{2,3}$").matcher(vals).find())) {
				System.err.println("need to specify numberOfPackets,packetSize (1-99),(10-512)");
				return;
			}
			String[] data = vals.split(",");
			int numPackets=Integer.parseInt(data[0]);
			int packetSize=Integer.parseInt(data[1]);
			sa.loopTest(numPackets, packetSize);
			pause(800);
			sa.sendReset();
			return;
					
		}

		if (cmd.hasOption('c')) {
			System.out.println(PacketUtil.bytesToHex(sa.getConfig()));
			pause(800);
			sa.sendReset();
			return;
		}		

		if (cmd.hasOption('g')) {
			String oper = cmd.getOptionValue('g').toUpperCase();
			//make sure we know the operation
			K250File fileType;
			try {
				fileType = K250File.valueOf(oper.toUpperCase());
			} catch (IllegalArgumentException e) {
				System.err.println("Unknown file type "+oper);
				return;
			}
			
			//make sure we have a file for results
			if (!cmd.hasOption('f') || cmd.getOptionValue('f')==null) {
				System.err.println("Output file required");
				return;
			}
			String filename = cmd.getOptionValue('f');
			
			
			sa.saveDigitizer(new File(filename));
			
			
			pause(800);
			sa.sendReset();
			return;
		}		

		if (cmd.hasOption('s')) {
			String oper = cmd.getOptionValue('s').toUpperCase();
			//make sure we know the operation
			K250File fileType;
			try {
				fileType = K250File.valueOf(oper.toUpperCase());
			} catch (IllegalArgumentException e) {
				System.err.println("Unknown file type "+oper);
				return;
			}
			
			//make sure we have a file for input
			if (!cmd.hasOption('f') || cmd.getOptionValue('f')==null) {
				System.err.println("Input file required");
				return;
			}
			String filename = cmd.getOptionValue('f');
			
			
			sa.loadDigitizer(new File(filename));
			
			
			pause(800);
			sa.sendReset();
			return;
		}			
		
	}
	
	private static void pause(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
	}
	
	public void showHelp() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("K250Cli", options);				
	}
	
	public static void main(String[] args) {
		K250Cli cli = new K250Cli();
		try {
			cli.process(args);
		} catch (SerialException | IOException e) {
			System.err.println(e.getMessage());
		}
	}

}

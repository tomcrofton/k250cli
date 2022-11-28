package org.alleypress.k250.cli;

import java.util.regex.Pattern;

import org.alleypress.k250.serial.JSSerialAdapter;
import org.alleypress.k250.serial.K250Commands;
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

	public K250Cli() {
		options.addOption("h", "help", false, "Print this help screen.")
		.addOption("l", "list", false, "List serial ports")
		.addOption("p", "port", true, "Serial port to use")
		.addOption("i","info", false,"Show connected interface adapter info")
		.addOption("o","out", true,"output file name")
		.addOption("x","loop",true,"loopback test <packets,size>")
		.addOption("g", "get", true, "Get Operation "+K250Commands.getGetCommandList());
	}
	
	public void process(String[] args) throws SerialException {
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
		
		if (cmd.hasOption('i')) {
			System.out.println(sa.getAdapterInfo());
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
			
			System.out.println(sa.loopTest(numPackets,packetSize));		
			preResetPause();
			sa.sendReset();
			return;
					
		}
		
		if (cmd.hasOption('g')) {
			String oper = cmd.getOptionValue('g');
			if (!K250Commands.getGetCommandList().contains(oper)) {
				System.err.println("Get Operation not found, "+oper);
				return;
			}
			
			//begin temp
			System.out.println(sa.getConfig());
			preResetPause();
			sa.sendReset();
			//end temp
			return;
		}		

	}
	
	private static void preResetPause() {
		try {
			Thread.sleep(800);
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
		} catch (SerialException e) {
			System.err.println(e.getMessage());
		}
	}

}

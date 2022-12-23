#k250cli
project to communicate with a k250 using the serial interface board

there are some open questions, but it's currently able to download and upload a sounds from the digitizer

the distro folder contains a zip file that has the needed jar file, an example sound and a helper file for windows.
The jar and sample file are cross platform, so linux/mac users 

usage: K250Cli
 -c,--config       Get Config
 -e,--echo <arg>   local echo <packet size>
 -f,--file <arg>   input or output file name <filename>
 -g,--get <arg>    Get File [digi]
 -h,--help         Print this help screen.
 -i,--info         Show connected interface adapter info
 -l,--list         List serial ports
 -p,--port <arg>   Serial port to use
 -s,--send <arg>   Send File [digi]
 -x,--loop <arg>   loopback test <packets,size>

if the serial adapter board is the only serial device pluged into your computer you don't need to specify which serial port to use

example:
k250 -s digi -f test.k250

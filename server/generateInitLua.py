import random
import IOTIOMapping
import IOTNode
import sys
name = raw_input("Node name:")
name = name.replace(",","")
uid = ""
for i in range(0,16):
    uid += "%X"%(random.randint(0,16))

iomapping = IOTIOMapping.IOTIOMapping()
output_port_count = int(raw_input("Output port count:"))
output_gpio_mapping = {}
input_gpio_mapping = {}
for i in range(0,output_port_count):
    portid = int(raw_input("Port ID(Arbitrary, unique between input and output):"))
    gpio = int(raw_input("GPIO Pin:"))
    out_name = raw_input("Output name:")
    
    iomapping.outputs.append(IOTIOMapping.OutputPort(portid,out_name))
    output_gpio_mapping[portid] = gpio
input_port_count = int(raw_input("Digital input port count:"))
for i in range(0,input_port_count):
    portid = int(raw_input("Port ID(Arbitrary, unique between input and output):"))
    gpio = int(raw_input("GPIO Pin:"))
    in_name = raw_input("Input name:")
    
    iomapping.digitalinputs.append(IOTIOMapping.InputPort(portid,in_name))
    input_gpio_mapping[portid] = gpio

portconfig = iomapping.genConfigStr()

if sys.argv[1] != "emulate":
    outfile = open(sys.argv[1],"w")
    outfile.write("nome = \"%s\"\n"%(name))
    outfile.write("uid = \"%s\"\n"%(uid))
    outfile.write("portconfig = \"%s\"\n"%(portconfig))
    outfile.write("tmr.alarm(0, 1000, 1, function()\n   if wifi.sta.getip() == nil then\n      \n   else\n      print('IP: ',wifi.sta.getip())\n      tmr.stop(0)\n   end\nend)\n")

    for portid in output_gpio_mapping:
        outfile.write("gpio.mode(%d, gpio.OUTPUT)\n"%(output_gpio_mapping[portid]))
        outfile.write("state_%d = 0\n"%(portid))
    for portid in input_gpio_mapping:
        outfile.write("gpio.mode(%d, gpio.INPUT)\n"%(input_gpio_mapping[portid]))

    outfile.write("function udprecv(c,pl)\n")
    outfile.write("    if pl == \"A\" then\n") # Status request
    statestr = ""
    statestr_tokens = []
    for portid in output_gpio_mapping:
        statestr_tokens.append("\"%d:\"..state_%d"%(portid,portid))
    statestr = "..\";\"..".join(statestr_tokens)
    outfile.write("         c:send(uid..\",\"..nome..\",\"..%s..\",\"..portconfig)\n"%(statestr))
    outfile.write("   end\n")
    outfile.write("   if string.sub(pl,1,1) == \"B\" then\n")
    outfile.write("         newstatestr = string.sub(pl,2)\n")
    outfile.write("         for i in string.gmatch(newstatestr,\"[^;]+\") do\n")
    outfile.write("              tokens = {}\n")
    outfile.write("              index = 1\n")
    outfile.write("              for k in string.gmatch(i,\"[^:]+\") do\n")
    outfile.write("                   tokens[index] = k\n")
    outfile.write("                   index = index + 1\n")
    outfile.write("              end\n")
    outfile.write("              portid = tonumber(tokens[1])\n")
    outfile.write("              portnewstate = tonumber(tokens[2])\n")
    indent = "              "
    for portid in output_gpio_mapping:
        outfile.write(indent+"if portid == %d then\n"%(portid))
        outfile.write(indent+"   "*1+"if portnewstate == 1 then\n")
        outfile.write(indent+"   "*2+"gpio.write(%d, gpio.HIGH)\n"%(output_gpio_mapping[portid]))
        outfile.write(indent+"   "*1+"else\n")
        outfile.write(indent+"   "*2+"gpio.write(%d, gpio.LOW)\n"%(output_gpio_mapping[portid]))
        outfile.write(indent+"   "*1+"end\n")
        outfile.write(indent+"   "*1+"state_%d = portnewstate\n"%(portid))
        outfile.write(indent+"end\n")

    outfile.write("         end\n") #for i...
    outfile.write("   end\n") #if string.sub...
    outfile.write("   if pl == \"C\" then\n")
    digitalinstr = ""
    digitalinstr_tokens = []
    for portid in input_gpio_mapping:
        digitalinstr_tokens.append("\"%d:\"..gpio.read(%d)"%(portid,input_gpio_mapping[portid]))
    digitalinstr = "..\";\"...".join(digitalinstr_tokens)
    outfile.write("        c:send(%s)\n"%(digitalinstr))
    outfile.write("   end\n")
    outfile.write("end\n")
    outfile.write("srv=net.createServer(net.UDP)\nsrv:on(\"receive\",udprecv)\nsrv:listen(8000)\n")






    outfile.close()
else:
    node = IOTNode.IOTNode(uid,name,iomapping,{})
    node._th_emulate()
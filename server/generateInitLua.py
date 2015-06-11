import random
import IOTIOMapping
import IOTNode
import sys
name = raw_input("Node name:")
name = name.replace(",","")
serverhost = raw_input("Server:")
uid = ""
dht11temp = False
dht11humidity = False
if len(sys.argv) < 3:
    for i in range(0,16):
        uid += "%X"%(random.randint(0,16))
else:
    uid = sys.argv[2].upper()
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
analog_input_port_count = int(raw_input("Analog input port count:"))
for i in range(0,analog_input_port_count):
    portid = int(raw_input("Port ID(Arbitrary, unique between input and output):"))
    ain_name = raw_input("Input name:")
    atype = raw_input("Input type:\nA: Temperature C, B: Humidity%%, C: Temperature F, D: Temperature K, ZZ: General purpose float:")
    if atype == "A":
        dht11temp = True
    if atype == "B":
        dht11humidity = True
    iomapping.analoginputs.append(IOTIOMapping.AnalogInputPort(portid,ain_name,atype))
    
    
portconfig = iomapping.genConfigStr()

if sys.argv[1] != "emulate":
    outfile = open(sys.argv[1],"w")
    outfile.write("nome = \"%s\"\n"%(name))
    outfile.write("uid = \"%s\"\n"%(uid))
    outfile.write("portconfig = \"%s\"\n"%(portconfig))
    outfile.write("server = \"%s\"\n"%(serverhost))
    
    outfile.write("function postrequest(uri,data)\n")
    outfile.write("    conn=net.createConnection(net.TCP, 0)\n")
    outfile.write("    conn:connect(8081, server)\n")
    outfile.write("    conn:on(\"sent\", function(c) c:close() end)\n")
    outfile.write("    conn:on(\"connection\", function(sck)\n")
    outfile.write("    conn:send(\"POST \"..uri..\" HTTP/1.1\\r\\nHost: \"..server..\"\\r\\nConnection: close\\r\\nContent-Type: application/json\\r\\nContent-Length: \"..string.len(data)..\"\\r\\n\\r\\n\"..data)\n end)\n")
    outfile.write("end\n")
    
    outfile.write("function postrequestgetstatus(uri,data)\n")
    outfile.write("    conn=net.createConnection(net.TCP, 0)\n")
    outfile.write("    conn:connect(8081, server)\n")
    outfile.write("    conn:on(\"receive\", function(c, pl) \nprint(pl)\nc:close()\n end)")
    outfile.write("    conn:on(\"connection\", function(sck)\n")
    outfile.write("    conn:send(\"POST \"..uri..\" HTTP/1.1\\r\\nHost: \"..server..\"\\r\\nConnection: close\\r\\nContent-Type: application/json\\r\\n\")\n")
    outfile.write("    conn:send(\"Content-Length: \"..string.len(data)..\"\\r\\n\\r\\n\")\n")
    outfile.write("    conn:send(data)\n    end)\n")
    outfile.write("end\n")
    
    outfile.write("function announcePresence()\n")
    outfile.write("    postrequest(\"/announce\", \"{ \\\"uid\\\" : \\\"\"..uid..\"\\\", \\\"nome\\\" : \\\"\"..nome..\"\\\", \\\"portconfig\\\" : \\\"\"..portconfig..\"\\\" }\")\n")
    outfile.write("end\n")
    outfile.write("tmr.alarm(0, 30000, 1, function()\n   announcePresence()\nend)\n")
    if dht11humidity or dht11temp or input_port_count > 0 or output_port_count > 0:
        if dht11humidity or dht11temp:
            outfile.write("DHT = require(\"dht_lib\")\n")
        outfile.write("function reportInputAndSetOutput()\n")
        if dht11humidity or dht11temp:
            outfile.write("    DHT.read11(3)\n")
        instr = ""
        instr_tokens = []
        for portid in input_gpio_mapping:
            instr_tokens.append("\"%d:\"..gpio.read(%d)"%(portid,input_gpio_mapping[portid]))
        for port in iomapping.analoginputs:
            if port.valuetype == "A":
                instr_tokens.append("\"%d:\"..DHT.getTemperature()"%(port.id))
            if port.valuetype == "B":
                instr_tokens.append("\"%d:\"..DHT.getHumidity()"%(port.id))
        instr = "..\";\"..".join(instr_tokens)
        outfile.write("    postrequestgetstatus(\"/inputstatus\",\"{ \\\"uid\\\" : \\\"\"..uid..\"\\\", \\\"status\\\" : \\\"\".."+instr+"..\"\\\" }\")\n")
        outfile.write("end\n")
        outfile.write("tmr.alarm(1, 10000, 1, function()\n   reportInputAndSetOutput()\nend)\n")

    for portid in output_gpio_mapping:
        outfile.write("gpio.mode(%d, gpio.OUTPUT)\n"%(output_gpio_mapping[portid]))
        outfile.write("gpio.write(%d, gpio.LOW)\n"%(output_gpio_mapping[portid]))
        outfile.write("state_%d = 0\n"%(portid))
    for portid in input_gpio_mapping:
        outfile.write("gpio.mode(%d, gpio.INPUT)\n"%(input_gpio_mapping[portid]))

    #outfile.write("function udprecv(c,pl)\n")
    #outfile.write("    if pl == \"A\" then\n") # Status request
    #statestr = ""
    #statestr_tokens = []
    #for portid in output_gpio_mapping:
        #statestr_tokens.append("\"%d:\"..state_%d"%(portid,portid))
    #statestr = "..\";\"..".join(statestr_tokens)
    #if len(statestr) > 0:
        #outfile.write("         c:send(uid..\",\"..nome..\",\"..%s..\",\"..portconfig)\n"%(statestr))
    #else:
        #outfile.write("         c:send(uid..\",\"..nome..\",,\"..portconfig)\n")
    #outfile.write("   end\n")
    #outfile.write("   if string.sub(pl,1,1) == \"B\" then\n")
    #outfile.write("         newstatestr = string.sub(pl,2)\n")
    #outfile.write("         for i in string.gmatch(newstatestr,\"[^;]+\") do\n")
    #outfile.write("              tokens = {}\n")
    #outfile.write("              index = 1\n")
    #outfile.write("              for k in string.gmatch(i,\"[^:]+\") do\n")
    #outfile.write("                   tokens[index] = k\n")
    #outfile.write("                   index = index + 1\n")
    #outfile.write("              end\n")
    #outfile.write("              portid = tonumber(tokens[1])\n")
    #outfile.write("              portnewstate = tonumber(tokens[2])\n")
    #indent = "              "
    #for portid in output_gpio_mapping:
        #outfile.write(indent+"if portid == %d then\n"%(portid))
        #outfile.write(indent+"   "*1+"if portnewstate == 1 then\n")
        #outfile.write(indent+"   "*2+"gpio.write(%d, gpio.HIGH)\n"%(output_gpio_mapping[portid]))
        #outfile.write(indent+"   "*1+"else\n")
        #outfile.write(indent+"   "*2+"gpio.write(%d, gpio.LOW)\n"%(output_gpio_mapping[portid]))
        #outfile.write(indent+"   "*1+"end\n")
        #outfile.write(indent+"   "*1+"state_%d = portnewstate\n"%(portid))
        #outfile.write(indent+"end\n")

    #outfile.write("         end\n") #for i...
    #outfile.write("   end\n") #if string.sub...
    #outfile.write("   if pl == \"C\" then\n")
    
    #outfile.write("        c:send(%s)\n"%(instr))
    #outfile.write("   end\n")
    #outfile.write("end\n")







    outfile.close()
    print("\nNow do:")
    print("luatool.py -f %s -t init.lua"%sys.argv[1])
    print("luatool.py -f dht_lib.lua -t dht_lib.lua")
else:
    node = IOTNode.IOTNode(uid,name,iomapping,{})
    node._th_emulate()



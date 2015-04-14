import re
import time
import socket
import thread
import random
import IOTIOMapping
QUERY_TIMEOUT = 5
#Commands: A=Query status and static info, B=Set figital outputs, C=Read digital and analog inputs
class IOTNode:
    def __init__(self,uid,name,iomapping,digitalstate={},ipaddress=""):
        self.uid = uid
        self.name = name
        self.iomapping = iomapping
        self.digitalstate = digitalstate
        self.analoginputstate = {}
        self.digitalinputstate = {}
        self.ipaddress = ipaddress
        self.inputs_last_update = time.time()
        self.last_seen = time.time()
    def packDigitalState(self):
        ports = []
        for port in self.digitalstate:
            ports.append("%d:%d"%(port,self.digitalstate[port]))
        return ";".join(ports)
    def _th_query_analog_and_digital_inputs(self):
        self.analoginputstate = {}
        self.digitalinputstate = {}
        sock = socket.socket(socket.AF_INET,socket.SOCK_DGRAM)
        sock.sendto("C", (self.ipaddress,8000))
        sock.settimeout(QUERY_TIMEOUT)
        starttime = time.time()
        while True:
            try:
                data,addr = sock.recvfrom(1500)
                if time.time() - starttime > QUERY_TIMEOUT:
                    break
            except socket.timeout:
                break
            if addr[0] == self.ipaddress:
                # portid:value;
                data = data.split(";")
                for port in data:
                    port = port.split(":")
                    portid = int(port[0])
                    portvalue = port[1]
                   
                    if self.iomapping.isInputPortDigital(portid):
                        self.digitalinputstate[portid] = int(portvalue)
                    else:
                        self.analoginputstate[portid] = float(portvalue)
                print("Received new digital and analog input state: %s %s\n"%(str(self.digitalinputstate),str(self.analoginputstate)))
                self.inputs_last_update = time.time()
                break
            
        sock.close()
    def applyDigitalState(self):
        sock = socket.socket(socket.AF_INET,socket.SOCK_DGRAM)
        sock.sendto("B"+self.packDigitalState(), (self.ipaddress,8000))
        sock.close() #In the case the command got lost, it will be retried at the next discovery round
    def tryQueryValues(self):
        thread.start_new_thread(self._th_query_analog_and_digital_inputs,())
    def _th_emulate(self):
        sock = socket.socket(socket.AF_INET,socket.SOCK_DGRAM)
        sock.bind(("0.0.0.0",8000))
        while True:
            data,addr = sock.recvfrom(1500)
            if data == "A":
                digitalstatestr = ""
                tokens = []
                for x in self.iomapping.outputs:
                    if x.id in self.digitalstate:
                        tokens.append("%d:%d"%(x.id,self.digitalstate[x.id]))
                    else:
                        tokens.append("%d:0"%(x.id))
                digitalstatestr = ";".join(tokens);
                reply = "%s,%s,%s,%s"%(self.uid,self.name,digitalstatestr,self.iomapping.genConfigStr())
                print(reply)
                sock.sendto(reply, addr)
            if data.startswith("B"):
                newstate = unpackDigitalState(data[1:])
                self.digitalstate = newstate
                print("New state: %s\n",str(newstate))
            if data == "C":
                for x in self.iomapping.digitalinputs:
                    self.digitalinputstate[x.id] = random.randint(0,1)
                tokens = []
                for x in self.iomapping.digitalinputs:
                    tokens.append("%d:%d"%(x.id,self.digitalinputstate[x.id]))
                digitalinstr = ";".join(tokens)
                sock.sendto(digitalinstr, addr)
    def emulate(self):
        thread.start_new_thread(self._th_emulate,())
    
def unpackDigitalState(s):
    if len(s) == 0:
        return {}
    data = s.split(";")
    res = {}
    for port in data:
        port = port.split(":")
        portid = int(port[0])
        portvalue = int(port[1])
        res[portid] = portvalue
    return res
def createJSONReprFromNodeDict(d):
    result = {}
    for uid in d:
        node = d[uid]
        result[uid] = { "Name" : node.name, "IOMapping" : node.iomapping.genConfigStr(), "digitaloutstate" : node.digitalstate, "digitalinstate" : node.digitalinputstate, "analoginstate" : node.analoginputstate , "IPAddress" : node.ipaddress, "InputLastUpdate" : node.inputs_last_update }
    return result
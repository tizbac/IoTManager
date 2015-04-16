import re
import time
import socket
import thread
import random
import IOTIOMapping
import threading
import rrdtool
import os
import math
QUERY_TIMEOUT = 5
DATADIR = "."
COLORS = [ "#FF0000", "#00AA00", "#0000FF","#AA00AA"]
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
        self.rrdlock = threading.Lock()
        self.rrdpath = ""
        if len(self.iomapping.analoginputs) > 0:
            try:
                os.mkdir(os.path.join(DATADIR,"rrd"))
            except:
                pass
            self.rrdpath = os.path.join(DATADIR,"rrd",self.uid+".rrd")
            if not os.access(self.rrdpath, os.F_OK):
                ds = []
                for port in self.iomapping.analoginputs:
                    ds.append("DS:port%d:GAUGE:120:U:U"%(port.id))
                ds.append("RRA:AVERAGE:0.5:1:2880")
                ds.append("RRA:AVERAGE:0.5:7:2880")
                ds.append("RRA:AVERAGE:0.5:31:2880")
                ds.append("RRA:AVERAGE:0.5:365:2880")
                self.rrdlock.acquire()
                rrdtool.create(self.rrdpath, "--step","30","--start","now",*ds)
                self.rrdlock.release()
    def generateGraphImage(self,gtype='day',includeports=None):
        self.rrdlock.acquire()
        if gtype == 'day':
            start = "end-1d"
        elif gtype == 'week':
            start = "end-7d"
        elif gtype == 'month':
            start = "end-31d"
        elif gtype == 'year':
            start = "end-365d"
        elif gtype == 'hour':
            start = "end-1h"
        addargs = []
        for x in self.iomapping.analoginputs:
            if includeports != None and x.id not in includeports:
                continue
            addargs.append("DEF:inport%d=%s:port%d:AVERAGE"%(x.id,self.rrdpath,x.id))
        i = 0
        for x in self.iomapping.analoginputs:
            if includeports != None and x.id not in includeports:
                continue
            addargs.append("LINE1:inport%d%s:%s"%(x.id,COLORS[i],x.name))
            i += 1
        if i == 0:
            self.rrdlock.release()
            return None
        try:
            ret = rrdtool.graphv( "-" , "--end", "now", "--start" , start, "--height", "250", *addargs)
        except rrdtool.error:
            print("Graph error")
            pass
        
        self.rrdlock.release()
        
        return ret["image"]
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
                if len(self.rrdpath) > 0:
                    self.rrdlock.acquire()
                    u = []
                    for x in self.iomapping.analoginputs:
                        u.append(str(self.analoginputstate[x.id]))
                    updates = ":".join(u)
                    rrdtool.update(self.rrdpath,"N:" + updates)
                    self.rrdlock.release()
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
                for x in self.iomapping.analoginputs:
                    self.analoginputstate[x.id] = math.sin(time.time()/120.0+x.id)
                tokens = []
                for x in self.iomapping.digitalinputs:
                    tokens.append("%d:%d"%(x.id,self.digitalinputstate[x.id]))
                for x in self.iomapping.analoginputs:
                    tokens.append("%d:%f"%(x.id,self.analoginputstate[x.id]))
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
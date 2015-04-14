import re
import base64
import zlib
#Port_Type [A : Digital Out, B : Digital In, C : Analog In] \ Port_Id \ Port_Name \ {Analog type} | next...

class OutputPort:
    def __init__(self,id,name):
        self.id = id
        self.name = name
    def __str__(self):
        return "OutputPort(%s,%s)"%(self.id,self.name)
class InputPort:
    def __init__(self,id,name):
        print(id)
        self.id = id
        self.name = name
    def __str__(self):
        return "InputPort(%s,%s)"%(self.id,self.name)
class AnalogInputPort:
    def __init__(self,id,name,valuetype):
        self.id = id
        self.name = name
        self.valuetype = valuetype
    def __str__(self):
        return "AnalogInputPort(%s,%s,%s)"%(self.id,self.name,self.valuetype)
class IOTIOMapping:
    def __init__(self):
        self.outputs = []
        self.digitalinputs = []
        self.analoginputs = []
    def genConfigStr(self):
        ports = []
        for do in self.outputs:
            ports.append("A\\%d\\%s"%(do.id,do.name))
        for di in self.digitalinputs:
            ports.append("B\\%d\\%s"%(di.id,di.name))
        for ai in self.analoginputs:
            ports.append("C\\%d\\%s\\%s"%(ai.id,ai.name,ai.valuetype))
        configstr = "|".join(ports)
        sc = zlib.compress(configstr)
        return base64.b64encode(sc)
    def isInputPortDigital(self,id):
        for di in self.digitalinputs:
            #print(str(di.id)+" "+str(id))
            if di.id == id:
                return True
        return False
    def isInputPortAnalog(self,id):
        for ai in self.analoginputs:
            if ai.id == id:
                return True
        return False
    def getOutputPortIds(self):
        res = []
        for do in self.outputs:
            res.append(do.id)
        return res
    def __str__(self):
        return "DigitalOutput: %s , DigitalInput: %s, AnalogInput: %s"%(str(self.outputs),str(self.digitalinputs),str(self.analoginputs))
#def outputStateFromBitField(bits,ports):
#    res = {}
#    for p in ports:
#        res[p] = 1 if (bits >> p) & 0x1 else 0
#    return res
def getIOMappingFromConfigStr(s):
    configstrc = base64.b64decode(s)
    configstr = zlib.decompress(configstrc)
    res = IOTIOMapping()
    #print(configstr)
    for port in configstr.split("|"):
        fields = port.split("\\")
        if fields[0] == "A":
            res.outputs.append(OutputPort(int(fields[1]),fields[2]))
        elif fields[0] == "B":
            res.digitalinputs.append(InputPort(int(fields[1]),fields[2]))
        elif fields[0] == "C":
            res.analoginputs.append(AnalogInputPort(int(fields[1]),fields[2],fields[3]))
    
    return res
def getDefaultESP_01Config():
    x = IOTIOMapping()
    x.outputs.append(OutputPort(0,"LegacyOutput1"))
    x.outputs.append(OutputPort(1,"LegacyOutput2"))
    return x.genConfigStr()

if __name__ == "__main__":
    print(getDefaultESP_01Config())
    
    print(getIOMappingFromConfigStr(getDefaultESP_01Config()))
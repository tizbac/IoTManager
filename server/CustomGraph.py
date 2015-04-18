import IOTIOMapping
import IOTNode
import threading
import rrdtool
import StringIO
import os
import math
from PIL import Image, ImageDraw
COLORS = [ "#FF0000", "#00AA00", "#0000FF","#AA00AA", "#999900", "#009999", "#990099", "#999999"]
def generateCustomGraphPNG(ports,gtype):
    #Ports is a list of tuples (nodeobj,portid)
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
    locking = {}
    for p in ports:#Lock once all involved rrd
        if p[0] not in locking:
            locking[p[0]] = p[0].rrdlock
            p[0].rrdlock.acquire()
    addargs = []
    i = 0
    for p in ports:
        node = p[0]
        portid = p[1]
        addargs.append("DEF:inport%dnode%s=%s:port%d:AVERAGE"%(portid,node.uid,node.rrdpath,portid))
        portname = "UNDEF"
        for ai in node.iomapping.analoginputs:
            if ai.id == portid:
                portname = ai.name
        addargs.append("LINE1:inport%dnode%s%s:%s"%(portid,node.uid,COLORS[i],node.name+"->"+portname))
        
        i += 1
    
    try:
        ret = rrdtool.graphv( "-" , "--end", "now", "--start" , start, "--height", "250", *addargs)
    except rrdtool.error:
        print("Graph error")
        im = Image.new('RGB', (128,80), (255,255,255))
        draw = ImageDraw.Draw(im) 
        draw.line((0,0,128,80), fill=128)
        draw.line((128,0,0,80), fill=128)
        output = StringIO.StringIO()
        im.save(output,'PNG')
        pngdata = output.getvalue()
        output.close()
        ret = {"image" : pngdata}
        pass
    
    for node in locking:
        locking[node].release()
    
    return ret["image"]
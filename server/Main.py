import sys
import threading
import time
import json
import signal
import random
import io
import qrcode
import IOTIOMapping
import IOTNode
import re
import os.path
from OpenSSL import crypto

from socket import *
from twisted.web import server, resource
from twisted.internet import ssl, protocol, task, defer, reactor


import urllib2

def ip2int(ip):
  i = ip.split(".")
  return int(i[3]) | ( int(i[2]) << 8 ) | ( int(i[1]) << 16 ) | ( int(i[0]) << 24 )
def int2ip(i):
  return "%d.%d.%d.%d"%(( i >> 24 ) & 0xff,( i >> 16 ) & 0xff,( i >> 8 ) & 0xff,( i  ) & 0xff)

#7 Name
#8 State
#9 UID
#A Summary ( UID,Name,State )
nodes = {} # { UID, Name , State, IP }

start = ip2int(sys.argv[1].split("/")[0])
mask = int(sys.argv[1].split("/")[1])
ips = []

for x in range(start+1,start+2**(32-mask)-1):
  ips.append(int2ip(x))


disc_sock = socket(AF_INET,SOCK_DGRAM)
disc_sock.bind(("",9000)) 
disc_sock.setsockopt(SOL_SOCKET, SO_BROADCAST, 1)

#Only needed for testing , after that a service to retrieve the public ip address is required
#localip = [(s.connect(('8.8.8.8', 80)), s.getsockname()[0], s.close()) for s in [socket(AF_INET, SOCK_DGRAM)]][0][1]


localip = urllib2.urlopen('http://ip.42.pl/raw').read().strip(" \r\n\t")
print("Public IPv4 Address: "+localip)
if not os.path.isfile('privkey.pem') or not os.path.isfile('server.pem'):
  print("Generating SSL Self-Signed certificates")
  key = crypto.PKey()
  key.generate_key(crypto.TYPE_RSA,2048)
  cert = crypto.X509()
  cert.get_subject().C = 'IT'
  cert.get_subject().ST = 'Terni'
  cert.get_subject().L = 'Calvi dell\'Umbria'
  cert.get_subject().OU = 'IoT Manager'
  cert.get_subject().CN = localip
  cert.set_serial_number(1000)
  cert.gmtime_adj_notBefore(0)
  cert.gmtime_adj_notAfter(10 * 365 * 24 * 60 * 60) 
  cert.set_issuer(cert.get_subject())
  cert.set_pubkey(key)
  cert.sign(key, 'sha1')
  shahash = cert.digest("sha1").replace(":","").lower()
  f = open("server.pem","w") 
  f.write(crypto.dump_certificate(crypto.FILETYPE_PEM, cert))
  f.close()
  f = open("privkey.pem","w")
  f.write(crypto.dump_privatekey(crypto.FILETYPE_PEM, key))
  f.close()
else:
  print("Certificates already existing")
  cert = crypto.load_certificate(crypto.FILETYPE_PEM,open("server.pem").read())
  shahash = cert.digest("sha1").replace(":","").lower()

sslContext = ssl.DefaultOpenSSLContextFactory(
        'privkey.pem', 
        'server.pem',
)

key = ""
try:
  key = open("key.txt","r").read()
except:
  print("Generating new password")
  key = ""
  for i in range(0,32):
    x = random.randint(0,2)
    if x == 0:
      key += chr(ord('a')+random.randint(0,24))
    if x == 1:
      key += chr(ord('A')+random.randint(0,24))
    if x == 2:
      key += chr(ord('0')+random.randint(0,10))
  f = open("key.txt","w")
  f.write(key)
  f.close()
print("Key is: "+key)
def applyNodeState(node):
  node.applyDigitalState()
  
  


def discoveryThread():
  global disc_sock
  while True:
    
    
    starttime = time.time()
    print("Discovery round starting...")
    
    disc_sock.settimeout(10)
    for x in ips[1:len(ips)-1]:
      disc_sock.sendto("A",(x,8000))
      time.sleep(0.01)
    disc_sock.settimeout(0.1)
    while time.time() - starttime < 5.0:
      try:
        data,addr = disc_sock.recvfrom(1500)
        print(addr)
        data = data.split(",")
        uid = data[0]
        name = data[1]
        ipaddress = addr[0]
        
        if len(data) > 3:
            iomapping = data[3]
        else:
            iomapping = IOTIOMapping.getDefaultESP_01Config() # Default esp8266 module with 2 GPIOs
        
        digitalstate = IOTNode.unpackDigitalState(data[2])
        iomapping = IOTIOMapping.getIOMappingFromConfigStr(iomapping)
        
        if uid in nodes:
          nodes[uid].name = name
          nodes[uid].ipaddress = ipaddress
          if digitalstate != nodes[uid].digitalstate: #State is incoherent
            applyNodeState(nodes[uid])
        else:
          nodes[uid] = IOTNode.IOTNode(uid,name,iomapping,digitalstate,ipaddress)
        
        
      except timeout:
        pass
    print("Discovery round complete", nodes)
    time.sleep(10.0)
  


class Simple(resource.Resource):
  isLeaf = True
  def render_GET(self, request):
    global ips
    nodes_safe = dict(nodes)
    
    if request.getClientIP() in ips or request.getClientIP() == "127.0.0.1":
      # Output the auth QR-Code
      request.setHeader("Content-Type", "image/png")
      
      output = io.StringIO.StringIO()
      qrcode.make(localip+",8080,"+key+","+shahash).save(output,'PNG')
      s = output.getvalue()
      output.close()
      
      return s
    else:
      request.setHeader("Content-Type", "application/json")
      return json.dumps({"error" : "Access denied from "+request.getClientIP() , "result" : None })
      
  def render_POST(self, request):
    nodes_safe = dict(nodes)
    request.setHeader("Content-Type", "application/json")
    if not "key" in request.args or request.args["key"][0] != key:
      return json.dumps({"error" : "Access denied from "+request.getClientIP() , "result" : None })
    if request.uri.startswith("/list"):
      return json.dumps({"error" : None , "result" : nodes_safe})
    if request.uri.startswith("/getstate"):
      sl = request.uri.split("/")
      if len(sl) == 3:
        if not sl[2] in nodes_safe:
          return json.dumps({"error" : "No such device" , "result" : None })
        node = nodes_safe[sl[2]]
        return json.dumps({"error" : None , "result" : { "digitaloutstate" : node.digitalstate, "digitalinstate" : node.digitalinputstate, "analoginstate" : node.analoginputstate}})
      else:
        return json.dumps({"error" : "Invalid argument" , "result" : None })
    if request.uri.startswith("/setstate"):
      sl = request.uri.split("/")
      if len(sl) == 4:
        uid = sl[2]
        if uid in nodes_safe:
          nodes[uid].digitalstate = int(sl[3])
          applyNodeState(nodes[uid])
          return json.dumps({"error" : None, "result" : nodes[uid]})
        else:
          return json.dumps({"error" : "No such device" , "result" : None })
      else:
        return json.dumps({"error" : "Invalid argument" , "result" : None })
def onexit(signum, frame):
  print("Exiting...")
  reactor.stop()
  print("HTTP Server stopped")
  
signal.signal(signal.SIGINT, onexit)
signal.signal(signal.SIGTERM, onexit)
dth = threading.Thread(target=discoveryThread)
dth.setDaemon(True)
dth.start()




site = server.Site(Simple())
reactor.listenSSL(8080, site, contextFactory = sslContext)
print("Listening on 8080")
reactor.run()
print("Exited.")
sys.exit(0)



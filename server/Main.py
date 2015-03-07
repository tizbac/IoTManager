import sys
import threading
import time
from socket import *
from netaddr import *
from twisted.web import server, resource
from twisted.internet import reactor
#7 Name
#8 State
#9 UID
#A Summary ( UID,Name,State )
nodes = {} # { UID, Name , State, IP }

disc_sock = socket(AF_INET,SOCK_DGRAM)
disc_sock.bind(("",9000)) 
disc_sock.setsockopt(SOL_SOCKET, SO_BROADCAST, 1)

def applyNodeState(node):
  sock = socket(AF_INET,SOCK_DGRAM)
  sock.sendto(node["State"], (node["IP"],8000))
  sock.close()
  
  


def discoveryThread():
  global disc_sock
  while True:
    
    
    starttime = time.time()
    print "Discovery round starting..."
    ip = IPNetwork(sys.argv[1])
    ips = map(str,ip)
    disc_sock.settimeout(10)
    for x in ips[1:len(ips)-1]:
      disc_sock.sendto("A",(x,8000))
    disc_sock.settimeout(0.1)
    while time.time() - starttime < 5.0:
      try:
        data,addr = disc_sock.recvfrom(64)
        data = data.split(",")
        if data[0] in nodes:
          nodes[data[0]]["Name"] = data[1]
          nodes[data[0]]["IP"] = addr[0]
          if data[2] != nodes[data[0]]["State"]: #State is incoherent
            applyNodeState(nodes[data[0]])
        else:
          nodes[data[0]] = { "UID" : data[0] , "Name" : data[1], "State" : data[2], "IP" : addr[0] }
        
        
      except timeout:
        pass
    print "Discovery round complete", nodes
    time.sleep(10.0)
  


class Simple(resource.Resource):
  isLeaf = True
  def render_GET(self, request):
    nodes_safe = dict(nodes)
    if request.uri.startswith("/setstate"):
      sl = request.uri.split("/")
      if len(sl) == 4:
        uid = sl[2]
        if uid in nodes_safe:
          nodes[uid]["State"] = sl[3]
          applyNodeState(nodes[uid])
          
    s = "<html><head>IOT by Tiziano Bacocco</head><body>\n"
    
    s += "<table border=1><tr><th>Device ID</th><th>Device Name</th><th>Device state</th><th>Set state</th></tr>\n"
    
    for x in nodes_safe:
      n = nodes_safe[x]
      ss = "<a href=\"/setstate/"+n["UID"]+"/0\">--</a><br/>"
      ss += "<a href=\"/setstate/"+n["UID"]+"/1\">X-</a><br/>"
      ss += "<a href=\"/setstate/"+n["UID"]+"/2\">-X</a><br/>"
      ss += "<a href=\"/setstate/"+n["UID"]+"/3\">XX</a><br/>"
      s += "<tr><td>"+n["UID"]+"</td><td>"+n["Name"]+"</td><td>"+n["State"]+"</td><td>"+ss+"</td>\n"
    
    s += "</body></html>"

    return s
dth = threading.Thread(target=discoveryThread)
dth.start()


site = server.Site(Simple())
reactor.listenTCP(8080, site)
reactor.run()
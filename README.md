# IoTManager
Very simple nodemcu lua firmware + python ssl webserver + android app to create on-off nodes with esp8266 modules


Usage
=====

Flash esp8266/init.lua to one or more nodemcu devices ( add or set ssid and password if required )

Start the server on some low power device like a raspberry pi using 

python Main.py [network]

Where [network] is the network where the ESP8266s are, like 192.168.1.0/24

Then finally map the 8080 TCP port on your router to raspberry IP , connect to https://raspberryip:8080 and scan the QR Code with the android application


License
=======

The software of this repository is licensed under the GPLv3 license except where explicitly another license header is present

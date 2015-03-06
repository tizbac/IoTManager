package info.linuxehacking.iotmanager;

import java.io.Serializable;

/**
 * Created by tiziano on 06/03/15.
 */
public class Device implements Serializable {
    private String name;
    private String ipaddr;
    public Device(String name, String ipaddr)
    {
        this.name = name;
        this.ipaddr = ipaddr;
    }

    public String getIpaddr() {
        return ipaddr;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return (ipaddr+name).hashCode();
    }

    @Override
    public boolean equals(Object o) {
        Device other = (Device)o;
        return other.name.equals(name) && other.ipaddr.equals(ipaddr);
    }
}

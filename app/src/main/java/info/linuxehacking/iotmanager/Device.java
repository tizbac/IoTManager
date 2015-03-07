package info.linuxehacking.iotmanager;

import java.io.Serializable;

/**
 * Created by tiziano on 06/03/15.
 */
public class Device implements Serializable {
    private String name;
    private String ipaddr;
    private String uid;
    private String state;
    public Device(String name, String ipaddr, String uid, String state)
    {
        this.name = name;
        this.ipaddr = ipaddr;
        this.uid = uid;
        this.state = state;
    }

    public String getIpaddr() {
        return ipaddr;
    }

    public String getName() {
        return name;
    }

    public String getUid() {
        return uid;
    }

    public String getState() {
        return state;
    }

    @Override
    public int hashCode() {
        return uid.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        Device other = (Device)o;
        return other.uid.equals(uid);
    }
}

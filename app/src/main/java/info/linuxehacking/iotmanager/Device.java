package info.linuxehacking.iotmanager;

import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Created by tiziano on 06/03/15.
 */
public class Device implements Serializable {
    private String name;
    private String ipaddr;
    private String uid;
    private HashMap<Integer,Boolean> digital_state;
    private HashMap<Integer,String> digital_out_names;
    public Device(String name, String ipaddr, String uid, HashMap<Integer,Boolean> digital_state, String configstr)
    {
        this.name = name;
        this.ipaddr = ipaddr;
        this.uid = uid;
        this.digital_state = digital_state;
        this.digital_out_names = new HashMap<Integer,String>();
        byte configdataz[] = Base64.decode(configstr.getBytes(),Base64.DEFAULT);
        Compressor c = new Compressor();
        try {
            String configdecomp = c.decompressToString(configdataz);
            String[] ports = configdecomp.split("\\|");
            for ( int p = 0; p < ports.length; p++ )
            {
                String[] tokens = ports[p].split("\\\\");
                if ( tokens[0].equals("A") )
                {
                    digital_out_names.put(Integer.parseInt(tokens[1]),tokens[2]);

                }
            }
        } catch (DataFormatException e) {
            e.printStackTrace();
        }


    }
    public HashMap<Integer,String> getDigitalOutNames()
    {
        return digital_out_names;
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

    public HashMap<Integer,Boolean> getState() {
        return digital_state;
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

    public static HashMap<Integer,Boolean> getDigitalStateFromJson(JSONObject deviceobj) throws JSONException {
        HashMap<Integer,Boolean> res = new HashMap<Integer,Boolean>();
        JSONObject digitalout = deviceobj.getJSONObject("digitaloutstate");
        Iterator<String> ports = digitalout.keys();
        while ( ports.hasNext() )
        {
            String port = ports.next();
            String state = digitalout.getString(port);
            boolean stateb = state.equals("1") ? true : false;
            res.put(Integer.parseInt(port),stateb);


        }
        return res;
    }

}

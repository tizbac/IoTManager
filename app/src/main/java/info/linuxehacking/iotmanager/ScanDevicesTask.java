package info.linuxehacking.iotmanager;

import android.content.Context;
import android.os.AsyncTask;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by tiziano on 06/03/15.
 */
public class ScanDevicesTask extends AsyncTask<Context, ArrayList<Device>, ArrayList<Device> > {

    @Override
    protected ArrayList<Device> doInBackground(Context... params) {
        ArrayList<Device> res = new ArrayList<Device>();

        HashSet<Device> found = new HashSet<Device>();

        try {
            DatagramSocket s = new DatagramSocket(9000);
            s.setBroadcast(true);
            byte[] data = {'7'};
            DatagramPacket p = new DatagramPacket(data,1, IOTUtils.getBroadcastAddress(params[0]), 8000);

            for ( int i = 0; i < 5; i++ )
            {
                //Scan round
                s.setSoTimeout(150);
                s.send(p);

                long start = System.currentTimeMillis();
                while ( System.currentTimeMillis() - start < 5000 && !isCancelled()) // Try receive replies from ESP8266 devices for 5 seconds
                {
                    byte[] buffer = new byte[16];
                    DatagramPacket rp = new DatagramPacket(buffer, buffer.length);
                    try {
                        s.receive(rp);
                    } catch ( SocketTimeoutException e )
                    {
                        continue;
                    }
                    Device dev = new Device(new String(buffer,0,rp.getLength()),rp.getAddress().getHostAddress().toString());
                    found.add(dev);

                    res.clear();
                    res.addAll(found);

                    publishProgress(res);
                }



            }
            s.close();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }




        return res;
    }

}

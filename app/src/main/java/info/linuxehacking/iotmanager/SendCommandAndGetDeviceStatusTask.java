package info.linuxehacking.iotmanager;

import android.os.AsyncTask;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

/**
 * Created by tiziano on 06/03/15.
 */
public class SendCommandAndGetDeviceStatusTask extends AsyncTask<Object, Integer, ArrayList<Boolean>> {

    @Override
    protected ArrayList<Boolean> doInBackground(Object... params) {
        ArrayList<Boolean> res = new ArrayList<Boolean>();
        Device dev = (Device)params[0];
        byte command = (Byte)params[1];
        try {
            DatagramSocket s = new DatagramSocket(9000);
            byte[] data = {command};
            DatagramPacket p = new DatagramPacket(data,1, InetAddress.getByName(dev.getIpaddr()), 8000);
            for ( int i = 0; i < 5; i++ ) { //Try to send the command five times and wait for response 2 seconds
                s.setSoTimeout(2000);
                s.send(p);
                byte[] buffer = new byte[16];
                DatagramPacket rp = new DatagramPacket(buffer, buffer.length);
                try {
                    s.receive(rp);
                } catch ( SocketTimeoutException e )
                {
                    continue;
                }
                if ( rp.getLength() == 1 ) {
                    res.add(((buffer[0]-'0') & 0x1) != 0 ? true : false);
                    res.add(((buffer[0]-'0') & 0x2) != 0 ? true : false);
                    break;
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

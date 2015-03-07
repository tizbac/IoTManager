package info.linuxehacking.iotmanager;

import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by tiziano on 06/03/15.
 */
public class RetrieveStateTask extends AsyncTask<Object, Integer, ArrayList<Boolean>> {
    private String error = null;
    @Override
    protected ArrayList<Boolean> doInBackground(Object... params) {
        ArrayList<Boolean> res = new ArrayList<Boolean>();
        Device dev = (Device)params[0];

        Configuration conf = Configuration.get((android.content.Context) params[1]);
        try {
            URL website = new URL("https://"+conf.ipaddress+":"+conf.port+"/getstate/"+dev.getUid());
            URLConnection conn = website.openConnection();
            ( (HttpsURLConnection) conn ).setSSLSocketFactory( SHA1VerifyGenerator.generateFactory(conf.valid_certificate));
            ( (HttpsURLConnection) conn ).setHostnameVerifier( SHA1VerifyGenerator.getNullVerifier());
            conn.setDoOutput(true);
            String urlParameters = "key="+conf.key;
            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(urlParameters);
            writer.flush();


            StringBuilder sb = new StringBuilder();
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            while ((line = reader.readLine()) != null) {
                System.err.println(line);
                sb.append(line +"\n");
            }
            writer.close();
            reader.close();

            JSONObject result = new JSONObject(sb.toString());
            if ( ! result.isNull("error") )
            {
                error = result.getString("error");
                return res;
            }

            JSONObject item = result.getJSONObject("result");
            int state = item.getInt("State");

            res.add((state & 0x1) != 0 );
            res.add((state & 0x2) != 0 );

        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return res;
    }
}

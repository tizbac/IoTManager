package info.linuxehacking.iotmanager;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by tiziano on 06/03/15.
 */
public class ScanDevicesTask extends AsyncTask<Context, ArrayList<Device>, ArrayList<Device> > {

    private String error = null;
    @Override
    protected ArrayList<Device> doInBackground(Context... params) {
        ArrayList<Device> res = new ArrayList<Device>();
        Configuration conf = Configuration.get(params[0]);
        try {
            URL website = new URL("https://"+conf.ipaddress+":"+conf.port+"/list");
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
                sb.append(line +"\n");
            }
            writer.close();
            reader.close();
            Log.i("Iotparse", sb.toString());
            JSONObject result = new JSONObject(sb.toString());
            if ( ! result.isNull("error") )
            {
                error = result.getString("error");
                return res;
            }

            JSONObject items = result.getJSONObject("result");
            Iterator<String> it = items.keys();
            while ( it.hasNext() )
            {

                String UID = it.next();
                JSONObject jdevice = items.getJSONObject(UID);

                HashMap<Integer, Boolean> digital_state = Device.getDigitalStateFromJson(jdevice);

                Device dev = new Device(jdevice.getString("Name"),jdevice.getString("IPAddress"),UID,digital_state,jdevice.getString("IOMapping"));
                res.add(dev);
            }

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

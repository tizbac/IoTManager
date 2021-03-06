package info.linuxehacking.iotmanager;

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
 * Created by tiziano on 08/03/15.
 */
public class SetStateTask extends AsyncTask<Object, Integer, HashMap<Integer,Boolean>> {
    private String error = null;
    @Override
    protected HashMap<Integer,Boolean> doInBackground(Object... params) {
        HashMap<Integer,Boolean> res = null;
        Device dev = (Device)params[0];
        HashMap<Integer,Boolean> newState = (HashMap<Integer,Boolean>)params[2];
        Configuration conf = Configuration.get((android.content.Context) params[1]);

        StringBuilder newstatesb = new StringBuilder();

        Iterator<Integer> it = newState.keySet().iterator();
        while ( it.hasNext() )
        {
            int portid = it.next();
            int portstate = newState.get(portid) == true ? 1 : 0;
            newstatesb.append(String.format("%d:%d,",portid,portstate));

        }

        Log.i("setstate",newstatesb.toString());

        try {
            URL website = new URL("https://"+conf.ipaddress+":"+conf.port+"/setstate/"+dev.getUid()+"/"+newstatesb.toString());
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
            HashMap<Integer, Boolean> state = Device.getDigitalStateFromJson(item);

            res = state;

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
package info.linuxehacking.iotmanager;

import android.os.AsyncTask;

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
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by tiziano on 13/04/15.
 */
public class PollInputStateTask extends AsyncTask<Object, PollInputStateTaskResult, PollInputStateTaskResult> {
    @Override
    protected PollInputStateTaskResult doInBackground(Object... params) {
        Device dev = (Device)params[0];

        Configuration conf = Configuration.get((android.content.Context) params[1]);
        while (! isCancelled() )
        {
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
                    continue;
                }

                JSONObject item = result.getJSONObject("result");
                HashMap<Integer, Boolean> state = Device.getDigitalInStateFromJson(item);
                HashMap<Integer, Float> state_analog = Device.getAnalogInStateFromJson(item);

                PollInputStateTaskResult res = new PollInputStateTaskResult();
                res.analogInState = state_analog;
                res.digitalInState = state;
                publishProgress(res);



            } catch (IOException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (KeyManagementException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }


            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                break;
            }

        }
        return null;
    }
}

package info.linuxehacking.iotmanager;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by tiziano on 18/04/15.
 */
public class GraphView extends ImageView{
    private Configuration config;
    private Device dev;
    private String type;
    private Handler h;
    private boolean customMode;

    private ArrayList<CustomGraphAdapter.Port> customports;


    private Runnable startRefreshTask = new Runnable() {
        DownloadImageTask t = null;
        @Override
        public void run() {
            if ( t != null )
                t.cancel(true);
            t = new DownloadImageTask();
            t.execute(0);
            h.removeCallbacks(this);
            h.postDelayed(startRefreshTask,30000);
        }


    };
    public GraphView(Context context, Configuration config, Device dev, String type, boolean custom) {
        super(context);
        this.config = config;
        this.dev = dev;
        this.type = type;
        customMode = custom;
        customports = new ArrayList<CustomGraphAdapter.Port>();
        h = new Handler();


    }
    public GraphView(Context context) {
        super(context);
        this.config = null;
        this.dev = null;
        this.type = null;
        customMode = false;
        h = new Handler();


    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        h.removeCallbacks(startRefreshTask);
    }

    public void setCustomPorts(ArrayList<CustomGraphAdapter.Port> newports)
    {
        customports = newports;
        startRefreshTask.run();
    }

    @Override
    protected void onAttachedToWindow() {
        startRefreshTask.run();
    }

    private class DownloadImageTask extends AsyncTask<Object,Object,Bitmap> {

        @Override
        protected Bitmap doInBackground(Object[] params) {

            if ( config == null )
            {
                return BitmapFactory.decodeResource(getContext().getResources(),R.drawable.ic_launcher);
            }
            try {
                URL website = null;

                if ( !customMode )
                    website = new URL("https://"+config.ipaddress+":"+config.port+"/graph/"+dev.getUid()+"/"+type);
                else
                {
                    StringBuilder reqstr = new StringBuilder();
                    Iterator<CustomGraphAdapter.Port> ports = customports.iterator();
                    while ( ports.hasNext() )
                    {
                        CustomGraphAdapter.Port p = ports.next();
                        reqstr.append("/"+p.dev.getUid()+","+p.portid);
                    }
                    website = new URL("https://"+config.ipaddress+":"+config.port+"/customgraph/"+type+reqstr.toString());


                }
                URLConnection conn = website.openConnection();
                ( (HttpsURLConnection) conn ).setSSLSocketFactory( SHA1VerifyGenerator.generateFactory(config.valid_certificate));
                ( (HttpsURLConnection) conn ).setHostnameVerifier( SHA1VerifyGenerator.getNullVerifier());
                conn.setDoOutput(true);
                String urlParameters = "key="+config.key;
                OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
                writer.write(urlParameters);
                writer.flush();

                Bitmap b = BitmapFactory.decodeStream(conn.getInputStream());
                ((HttpsURLConnection) conn).disconnect();
                return b;

            } catch (IOException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (KeyManagementException e) {
                e.printStackTrace();
            }


            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            setImageBitmap(bitmap);
        }
    }


}

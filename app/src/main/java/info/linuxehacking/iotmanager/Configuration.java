package info.linuxehacking.iotmanager;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by tiziano on 07/03/15.
 */
public class Configuration {
    public String ipaddress;
    public String key;
    public String valid_certificate;
    public int port;
    private Configuration()
    {

    }
    public static Configuration get(Context ctx)
    {
        SharedPreferences settings = ctx.getSharedPreferences("IoTManager", 0);
        String[] tokens = settings.getString("server","").split(","); // IP,Port,Key,SHA1
        if ( tokens.length !=  4 )
            return null;
        Configuration config = new Configuration();
        config.ipaddress = tokens[0];
        config.port = Integer.parseInt(tokens[1]);
        config.key = tokens[2];
        config.valid_certificate = tokens[3];
        return config;
    }
}

package info.linuxehacking.iotmanager;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by tiziano on 06/03/15.
 */
public class DevicesAdatper extends ArrayAdapter<Device> {
    public DevicesAdatper(Context context, ArrayList<Device> devs){
        super(context, 0, devs);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Device dev = getItem(position);
        if ( convertView == null )
        {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.device_list_item, parent, false);
        }
        TextView tvName = (TextView) convertView.findViewById(R.id.txtdlist_name);
        TextView tvIP = (TextView) convertView.findViewById(R.id.txtdlist_ip);

        tvName.setText(dev.getName());
        tvIP.setText(dev.getIpaddr());

        return convertView;
    }
}

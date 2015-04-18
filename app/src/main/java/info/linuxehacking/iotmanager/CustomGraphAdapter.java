package info.linuxehacking.iotmanager;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by tiziano on 18/04/15.
 */
public class CustomGraphAdapter extends BaseAdapter{
    class Port
    {
        public String name;
        public Device dev;
        public int portid;
        public boolean selected = false;

    }

    private Context ctx;
    private ArrayList<Device> m_devices;
    private ArrayList<Port> m_ports;
    private OnSelectionChangedListener selchangelist = null;
    public interface OnSelectionChangedListener {
        public void changed(ArrayList<Port> ports);
    }
    private CompoundButton.OnCheckedChangeListener checkChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if ( buttonView.isPressed() ) {
                Port p = (Port) getItem((Integer) buttonView.getTag());
                p.selected = isChecked;
                if ( selchangelist != null )
                    selchangelist.changed(getSelectedPorts());
            }
        }
    };

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Port p = (Port) getItem((Integer)v.getTag());
            p.selected = !p.selected;
            CheckBox cb = (CheckBox) v.findViewById(R.id.customgraph_checkInclude);
            cb.setChecked(p.selected);
            if ( selchangelist != null )
                selchangelist.changed(getSelectedPorts());
        }
    };

    private ArrayList<Port> getSelectedPorts() {
        ArrayList<Port> res = new ArrayList<Port>();
        Iterator<Port> it = m_ports.iterator();
        while ( it.hasNext() )
        {
            Port p = it.next();
            if ( p.selected )
                res.add(p);

        }
        return res;
    }

    public void setSelChangeListener(OnSelectionChangedListener selchangelist) {
        this.selchangelist = selchangelist;
    }

    public CustomGraphAdapter(Context ctx, ArrayList<Device> devices)
    {
        m_devices = devices;
        m_ports = new ArrayList<Port>();
        this.ctx = ctx;
        Iterator<Device> devs = m_devices.iterator();
        while ( devs.hasNext() )
        {
            Device dev = devs.next();
            HashMap<Integer, String> analogports = dev.getAnalogInNames();
            Iterator<Integer> ports = analogports.keySet().iterator();
            while ( ports.hasNext() )
            {
                int portid = ports.next();
                String name = analogports.get(portid);
                Port p = new Port();
                p.name = name;
                p.dev = dev;
                p.portid = portid;

                m_ports.add(p);


            }
            
        }
    }

    @Override
    public int getCount() {
        return m_ports.size();
    }

    @Override
    public Object getItem(int position) {
        return m_ports.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Port p = (Port) getItem(position);
        if ( convertView == null )
        {
            convertView = LayoutInflater.from(ctx).inflate(R.layout.customgraph_portrow, parent, false);
        }

        TextView portName = (TextView) convertView.findViewById(R.id.customgraph_textPortName);
        TextView devName = (TextView) convertView.findViewById(R.id.customgraph_textDevName);
        TextView uid = (TextView) convertView.findViewById(R.id.customgraph_textUID);
        CheckBox cb = (CheckBox) convertView.findViewById(R.id.customgraph_checkInclude);
        portName.setText(p.name);
        devName.setText(p.dev.getName());
        uid.setText(p.dev.getUid());
        convertView.setTag(position);
        convertView.setOnClickListener(clickListener);
        cb.setTag(position);
        cb.setOnCheckedChangeListener(checkChangeListener);
        return convertView;
    }
}

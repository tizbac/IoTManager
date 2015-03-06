package info.linuxehacking.iotmanager;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;


public class DeviceList extends ActionBarActivity {

    private ListView lv;
    private DeviceListScanDevicesTask task = null;
    class DeviceListScanDevicesTask extends ScanDevicesTask {
        @Override
        protected void onProgressUpdate(ArrayList<Device>... values) {
            lv.setAdapter(new DevicesAdatper(DeviceList.this,values[0]));
        }

        @Override
        protected void onPostExecute(ArrayList<Device> devices) {
            lv.setAdapter(new DevicesAdatper(DeviceList.this, devices));
            task = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        lv = (ListView) findViewById(R.id.devicelist);


        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DevicesAdatper adpt = (DevicesAdatper) parent.getAdapter();
                Device dev = adpt.getItem(position);
                Intent i = new Intent(DeviceList.this,OutputControl.class);
                i.putExtra("device", dev);
                startActivity(i);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if ( task != null )
            task.cancel(true);
        task = new DeviceListScanDevicesTask();
        task.execute(this);
    }

    @Override
    protected void onStop() {

        if ( task != null )
            task.cancel(true);
        task = null;
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_device_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

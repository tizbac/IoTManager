package info.linuxehacking.iotmanager;

import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;


public class OutputControl extends ActionBarActivity {
    private Device dev;
    private ToggleButton tb1;
    private ToggleButton tb2;
    AsyncTask task = null;
    class OutputControlSetStateTask extends SetStateTask {
        @Override
        protected void onPreExecute() {
            tb1.setEnabled(false);
            tb2.setEnabled(false);
        }
        @Override
        protected void onPostExecute(ArrayList<Boolean> booleans) {
            super.onPostExecute(booleans);


            if ( booleans.size() == 2 ) {
                tb1.setEnabled(true);
                tb2.setEnabled(true);
                tb1.setChecked(booleans.get(0));
                tb2.setChecked(booleans.get(1));
            }

        }
    }
    class OutputControlRetriveStateTask extends RetrieveStateTask {

        @Override
        protected void onPreExecute() {
            tb1.setEnabled(false);
            tb2.setEnabled(false);
        }

        @Override
        protected void onPostExecute(ArrayList<Boolean> booleans) {
            super.onPostExecute(booleans);


            if ( booleans.size() == 2 ) {
                tb1.setEnabled(true);
                tb2.setEnabled(true);
                tb1.setChecked(booleans.get(0));
                tb2.setChecked(booleans.get(1));
            }

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_output_control);

        dev = (Device) getIntent().getSerializableExtra("device");

        TextView tvName = (TextView) findViewById(R.id.txt_out_title);
        tb1 = (ToggleButton) findViewById(R.id.toggle1);
        tb2 = (ToggleButton) findViewById(R.id.toggle2);
        tb1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                task = new SetStateTask();
                ((SetStateTask)task).execute(dev,OutputControl.this,(isChecked ? 1 : 0 ) | (tb2.isChecked() ? 2 : 0 ));
            }
        });
        tb2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                task = new OutputControlSetStateTask();
                ((OutputControlSetStateTask)task).execute(dev,OutputControl.this,(isChecked ? 2 : 0 ) | (tb1.isChecked() ? 1 : 0 ));
            }
        });
        tvName.setText(dev.getName());



    }

    @Override
    protected void onStart() {
        super.onStart();
        task = new OutputControlRetriveStateTask();
        ((OutputControlRetriveStateTask)task).execute(dev,this);
    }

    @Override
    protected void onStop() {
        if ( task != null )
            task.cancel(true);
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_output_control, menu);
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

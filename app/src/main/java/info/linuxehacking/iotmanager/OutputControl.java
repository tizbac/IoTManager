package info.linuxehacking.iotmanager;

import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


public class OutputControl extends ActionBarActivity {
    private Device dev;
    private HashMap<Integer,ToggleButton> toggles;
    HashMap<Integer,Boolean> curstate;
    HashMap<Integer,TextView> digitalin_views;
    AsyncTask task = null;
    private OutputControlPollInputStateTask polltask;

    void setAllButtonsDisabled()
    {
        Iterator<ToggleButton> it = toggles.values().iterator();
        while ( it.hasNext() )
            it.next().setEnabled(false);

    }
    void setAllButtonsEnabled()
    {
        Iterator<ToggleButton> it = toggles.values().iterator();
        while ( it.hasNext() )
            it.next().setEnabled(true);

    }
    void updateButtonState()
    {
        Iterator<Integer> it = toggles.keySet().iterator();
        while ( it.hasNext() ) {
            int portid = it.next();
            ToggleButton tb = toggles.get(portid);
            if ( curstate.containsKey(portid) )
                tb.setChecked(curstate.get(portid));
            else
                tb.setChecked(false);
        }

    }
    class OutputControlPollInputStateTask extends PollInputStateTask {
        @Override
        protected void onProgressUpdate(PollInputStateTaskResult... values) {
            HashMap<Integer, Boolean> a = values[0].digitalInState;
            Iterator<Integer> i = a.keySet().iterator();
            while ( i.hasNext() )
            {
                int portid = i.next();
                digitalin_views.get(portid).setText(""+a.get(portid));

            }
        }
    }
    class OutputControlSetStateTask extends SetStateTask {
        @Override
        protected void onPreExecute() {
            setAllButtonsDisabled();
        }
        @Override
        protected void onPostExecute(HashMap<Integer,Boolean> state) {
            super.onPostExecute(state);


            /*if ( booleans.size() == 2 ) {
                tb1.setEnabled(true);
                tb2.setEnabled(true);
                tb1.setChecked(booleans.get(0));
                tb2.setChecked(booleans.get(1));
            }*/
            curstate = state;
            updateButtonState();
            setAllButtonsEnabled();

        }
    }
    class OutputControlRetriveStateTask extends RetrieveStateTask {

        @Override
        protected void onPreExecute() {
            setAllButtonsDisabled();
        }

        @Override
        protected void onPostExecute(HashMap<Integer,Boolean> state) {
            super.onPostExecute(state);


            /*if ( booleans.size() == 2 ) {
                tb1.setEnabled(true);
                tb2.setEnabled(true);
                tb1.setChecked(booleans.get(0));
                tb2.setChecked(booleans.get(1));
            }*/
            curstate = state;
            updateButtonState();
            setAllButtonsEnabled();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_output_control);
        toggles = new HashMap<Integer,ToggleButton>();
        digitalin_views = new HashMap<Integer,TextView>();
        curstate = new HashMap<Integer,Boolean>();
        dev = (Device) getIntent().getSerializableExtra("device");

        TextView tvName = (TextView) findViewById(R.id.txt_out_title);

        LinearLayout ll = (LinearLayout)findViewById(R.id.portsScroll);

        Iterator<Integer> ports = dev.getDigitalOutNames().keySet().iterator();
        while ( ports.hasNext() )
        {
            final int portid = ports.next();
            String name = dev.getDigitalOutNames().get(portid);
            LinearLayout ll2 = new LinearLayout(this);
            ll2.setOrientation(LinearLayout.HORIZONTAL);
            TextView tv = new TextView(this);
            tv.setText(name);
            ll2.addView(tv);

            final ToggleButton tb = new ToggleButton(this);
            tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if ( tb.isPressed() )
                    {
                        Iterator<Integer> ports = dev.getDigitalOutNames().keySet().iterator();
                        HashMap<Integer,Boolean> newstate = new HashMap<Integer, Boolean>();
                        while ( ports.hasNext() ) {
                            int portid2 = ports.next();
                            newstate.put(portid2, toggles.get(portid2).isChecked());
                        }
                        task = new OutputControlSetStateTask();
                        ((OutputControlSetStateTask) task).execute(dev, OutputControl.this, newstate);

                    }
                }
            });
            ll2.addView(tb);

            ll.addView(ll2);

            toggles.put(portid, tb);

        }
        ports = dev.getDigitalInNames().keySet().iterator();
        while ( ports.hasNext() )
        {
            final int portid = ports.next();
            String name = dev.getDigitalInNames().get(portid);
            LinearLayout ll2 = new LinearLayout(this);
            ll2.setOrientation(LinearLayout.HORIZONTAL);
            TextView tv = new TextView(this);
            tv.setText(name);
            ll2.addView(tv);

            TextView outV = new TextView(this);
            ll2.addView(outV);
            outV.setText("AAA");
            digitalin_views.put(portid, outV);

            ll.addView(ll2);


        }

        /*tb1 = (ToggleButton) findViewById(R.id.toggle1);
        tb2 = (ToggleButton) findViewById(R.id.toggle2);
        tb1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if ( tb1.isPressed() ) {
                    task = new OutputControlSetStateTask();
                    ((OutputControlSetStateTask) task).execute(dev, OutputControl.this, (isChecked ? 1 : 0) | (tb2.isChecked() ? 2 : 0));
                }
            }
        });
        tb2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if ( tb2.isPressed() ) {
                    task = new OutputControlSetStateTask();
                    ((OutputControlSetStateTask) task).execute(dev, OutputControl.this, (isChecked ? 2 : 0) | (tb1.isChecked() ? 1 : 0));
                }
            }
        });*/
        tvName.setText(dev.getName());



    }

    @Override
    protected void onStart() {
        super.onStart();
        task = new OutputControlRetriveStateTask();
        ((OutputControlRetriveStateTask)task).execute(dev,this);
        polltask = new OutputControlPollInputStateTask();
        polltask.execute(dev,this);

    }

    @Override
    protected void onStop() {
        if ( task != null )
            task.cancel(true);
        if ( polltask != null )
        {
            polltask.cancel(true);
        }
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

package info.linuxehacking.iotmanager;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;


public class CustomGraphActivity extends ActionBarActivity {

    private ArrayList<Device> devices = null;
    private LinkedList<GraphView> graphviews = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_graph);
        devices = (ArrayList<Device>) getIntent().getExtras().getSerializable("devices");

        ListView portlist = (ListView) findViewById(R.id.list_customgraph);
        CustomGraphAdapter cga = new CustomGraphAdapter(this, devices);
        cga.setSelChangeListener(new CustomGraphAdapter.OnSelectionChangedListener() {
            @Override
            public void changed(ArrayList<CustomGraphAdapter.Port> ports) {
                Iterator<GraphView> git = graphviews.iterator();
                while ( git.hasNext() )
                {
                    GraphView gv = git.next();
                    gv.setCustomPorts(ports);

                }
            }
        });
        portlist.setAdapter(cga);


        graphviews = new LinkedList<GraphView>();
        LinearLayout ll = (LinearLayout)findViewById(R.id.ll_custom_graphs);

        String[] types = {"hour", "day", "week", "month", "year"};
        for ( int i = 0; i < types.length; i++ ) {
            TextView v1 = new TextView(this);
            v1.setText((types[i]+"ly"));
            GraphView gv = new GraphView(this, Configuration.get(this), null, types[i], true);
            gv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            gv.setAdjustViewBounds(true);
            ll.addView(v1, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            ll.addView(gv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            graphviews.add(gv);
        }



    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_custom_graph, menu);
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

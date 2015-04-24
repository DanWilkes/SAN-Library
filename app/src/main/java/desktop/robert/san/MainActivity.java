package desktop.robert.san;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import desktop.robert.simplifiedandroidnetworking.NetworkingServices;


public class MainActivity extends Activity {


    private ResponseReceiver receiver;
    private boolean isRegistered = false;
    //The inputMsg correspond to spinnerContents ordering. Very important that it remain so.
    //public enum inputMsg {getBroadcastAddress, receiveBroadcast, sendBroadcast,
        //getWifiIP, sendUDP, receiveUDP,
        //sendTCP, receiveTCP, startTCPServer,
        //discover, declare, failed}

    private String spinnerContents[] = {"Get Broadcast Address", "Receive Broadcast",
            "Send Broadcast", "Get Wifi IP", "Send UDP Packet", "Receive UDP Packet",
            "Send TCP Packet", "Receive TCP Packet", "Start TCP Server", "discover",
            "declare"};

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d("MainTag", "Reached onCreate Start");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Set up the spinner.
        Spinner s = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, spinnerContents);
        s.setAdapter(adapter);

        //Create and register the receiver for the messages
        IntentFilter filter = new IntentFilter(NetworkingServices.ACTION_RESP);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        receiver = new ResponseReceiver();
        registerReceiver(receiver, filter);
        //Boolean suggested by JunR. I assume it's redundant
        isRegistered = true;

        Log.d("MainTag", "Reached onCreate End");
    }


    //Receiver needs to be stopped to avoid memory leaks
    @Override
    protected void onPause()
    {
        Log.d("MainTag", "Reached onPause Started");
        if (isRegistered)
        {
            unregisterReceiver(receiver);
            isRegistered = false;
        }
        super.onPause();
        Log.d("MainTag", "Reached onPause End");
    }

    @Override
    protected void onStop()
    {
        Log.d("MainTag", "Reached onStop Started");
        if(isRegistered)
            unregisterReceiver(receiver);
        super.onStop();
        Log.d("MainTag", "Reached onStop End");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return (id == R.id.action_settings) || super.onOptionsItemSelected(item);
    }


    /**
     * This particular application uses a spinner to get the requested operation, but your
     * application will likely do things in a different way.
     * @param v The view is not used but appears to be required
     */
    public void onClickStartService(View v)
    {
        Log.d("Main Tag", "Reached onClickStartService Start");
        startNetwork(NetworkingServices.inputMsg.values()[((Spinner)findViewById(R.id.spinner)).getSelectedItemPosition()]);
        Log.d("Main Tag", "Reached onClickStartService End");
    }

    /**
     * Starts the Networking Service.
     * @param msg The action you want the service to perform.
     */
    private void startNetwork(NetworkingServices.inputMsg msg)
    {
        Log.d("Main Tag", "Reached startNetwork Start");
        Intent intent = new Intent(this, NetworkingServices.class);
        intent.putExtra(NetworkingServices.PARAM_IN_MSG, msg);

        String ip = ((TextView)findViewById(R.id.ip)).getText().toString();
        //No error checking to see if a valid IP is entered. They could technically enter a URL
        if(!ip.equals(""))
        {
            intent.putExtra(NetworkingServices.PARAM_IN_IP, ip);
        }
        //If a payload is being sent, then and only then include the payload, even if null
        if(msg.equals(NetworkingServices.inputMsg.sendBroadcast)
                || msg.equals(NetworkingServices.inputMsg.sendTCP)
                || msg.equals(NetworkingServices.inputMsg.sendUDP))
        {
            Log.d("Main Tag", "Building Intent: " + msg);
            intent.putExtra(NetworkingServices.PARAM_IN_PAYLOAD, ((TextView)findViewById(R.id.PayloadText)).getText().toString());
        }
        else
        {
            Log.d("Main Tag", "No Payload Sent: " + msg);
        }

        this.startService(intent);

        Log.d("Main Tag", "Reached startNetwork End");
    }


    /**
     * Receive Intent Broadcasts from a running service.
     */
    public class ResponseReceiver extends BroadcastReceiver
    {

        /**
         * @param context Not used, but appears to be required
         * @param intent The message is encoded in this intent
         */
        @Override
        public void onReceive(Context context, Intent intent)
        {
            Log.d("Response Receiver", "Reached RR Start");
            //Display the MSG to user in the PayloadText that sends messages
            ((TextView) findViewById(R.id.PayloadText)).setText(
                    intent.getStringExtra(NetworkingServices.PARAM_OUT_MSG));

            //Can be commented out, for debugging purposes on the device
            Toast.makeText(getBaseContext(), "Service Replied", Toast.LENGTH_SHORT).show();

            Log.d("Response Receiver", "Reached RR End");
        }
    }
}

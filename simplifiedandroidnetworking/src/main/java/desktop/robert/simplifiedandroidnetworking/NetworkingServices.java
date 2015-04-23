package desktop.robert.simplifiedandroidnetworking;

/**
 * Created by Robert on 2014-09-01.
 */

        import android.app.IntentService;
        import android.content.Context;
        import android.content.Intent;
        import android.net.DhcpInfo;
        import android.net.wifi.WifiManager;
        import android.util.Log;
        import android.widget.Toast;

        import java.io.BufferedReader;
        import java.io.BufferedWriter;
        import java.io.IOException;
        import java.io.InputStreamReader;
        import java.io.OutputStreamWriter;
        import java.io.PrintWriter;
        import java.math.BigInteger;
        import java.net.DatagramPacket;
        import java.net.DatagramSocket;
        import java.net.InetAddress;
        import java.net.InetSocketAddress;
        import java.net.ServerSocket;
        import java.net.Socket;
        import java.net.SocketException;
        import java.net.SocketTimeoutException;
        import java.net.UnknownHostException;
        import java.nio.ByteOrder;
        import java.util.ArrayList;


public class NetworkingServices extends IntentService
{

    //Intent control variables
    public static final String PARAM_IN_MSG = "imsg";
    public static final String PARAM_IN_PAYLOAD = "payload";
    public static final String PARAM_IN_PORT = "port";
    public static final String PARAM_IN_TIMEOUT = "timeout";
    public static final String PARAM_IN_IP = "ip";
    public static final String PARAM_OUT_MSG = "omsg";
    public static final String USER_MSG = "umsg";
    public static final String SERVER_MSG = "smsg";
    public static final String ACTION_RESP = "desktop.robert.simplifiedandroidnetworking";


    //Change this to be your own unique message, something such as the application name
    private String discoveryMessage = "Unique Message";
    //TODO Make get and set methods for this;
    private int defaultBufferSize = 500;
    private boolean connected = false;
    private ServerSocket servSock = null;
    private boolean discovering = false;

    public enum inputMsg {getBroadcastAddress, receiveBroadcast, sendBroadcast,
        getWifiIP, sendUDP, receiveUDP,
        sendTCP, receiveTCP, startTCPServer,
        discover, declare, failed}

    /**
     * I have no idea what the super constructor is doing in this case.
     */
    public NetworkingServices()
    {
        super("NetworkingServices");
    }


    /**
     * Intents are send back and forth to communicate between threads.
     *
     * @param ctrmsg The control message to determine which extra is being sent back
     * @param result The String message that you want to send back to the listening thread.
     */
    private void sendIntent(String ctrmsg, String result)
    {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(ACTION_RESP);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(ctrmsg, result);
        sendBroadcast(broadcastIntent);
    }


    /**
     * Intents are send back and forth to communicate between threads.
     *
     * @param result The String message that you want to send back to the listening thread.
     */
    private void sendIntent(String result)
    {
        sendIntent(PARAM_OUT_MSG, result);
    }


    /**
     * This is never called by the user. The user calls start(intent), this is called automatically.
     *
     * @param intent The intent that has been received.
     */
    @Override
    protected void onHandleIntent(Intent intent)
    {
        Log.d("ServiceTag", "onHandleIntent Start");
        inputMsg msg = inputMsg.failed;
        String payload = "";
        String ip = "";
        int port = 14251;
        int timeout = 12000; //12 second listen time

        //Check which extras have been included in this received intent.
        try
        {
            if (intent.hasExtra(PARAM_IN_MSG))
                msg = (inputMsg) intent.getSerializableExtra(PARAM_IN_MSG);
            if (intent.hasExtra(PARAM_IN_PAYLOAD))
                payload = intent.getStringExtra(PARAM_IN_PAYLOAD);
            if (intent.hasExtra(PARAM_IN_PORT))
                port = intent.getIntExtra(PARAM_IN_PORT, port);
            if (intent.hasExtra(PARAM_IN_IP))
                ip = intent.getStringExtra(PARAM_IN_IP);
            if (intent.hasExtra(PARAM_IN_TIMEOUT))
                timeout = intent.getIntExtra(PARAM_IN_TIMEOUT, timeout);
        }
        catch(ClassCastException e)
        {
            Log.d("ERROR", "Something went wrong with trying to get extra from message "
                    + e.getMessage());

        }

        //"Get Broadcast Address", "Receive Broadcast", "Send Broadcast",
        //"Get Wifi IP", "Send UDP Packet", "Receive UDP",
        //"Send TCP Packet", "Receive TCP Packet", "Start TCP Server"}
        switch (msg)
        {
            case getBroadcastAddress:
                Log.d("ServiceTag", "IMSG Found - getBroadcastAddr");
                try
                {
                    sendIntent(getBroadcastAddress().getHostAddress());
                } catch (UnknownHostException e)
                {
                    e.printStackTrace();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
                Log.d("ServiceTag", "IMSG Completed - getBroadcastAddr");
                break;
            case receiveBroadcast:
                Log.d("ServiceTag", "IMSG Found - getBroadcast");
                receiveBroadcast();
                Log.d("ServiceTag", "IMSG Completed - getBroadcast");
                break;
            case sendBroadcast:
                Log.d("ServiceTag", "IMSG Found - sendBroadcast");
                sendBroadcast(port, payload);
                Log.d("ServiceTag", "IMSG Completed - sendBroadcast");
                break;
            case getWifiIP:
                Log.d("ServiceTag", "IMSG Found - getIP");
                //TODO remove this and replace with something like activity.getContext()
                sendIntent(wifiIpAddress(this));//activity.getApplicationContext()));
                Log.d("ServiceTag", "IMSG Completed - getIP");
                break;
            case sendUDP:
                Log.d("ServiceTag", "IMSG Found - sendUDP");
                sendUDPPacket(ip, port, payload);
                Log.d("ServiceTag", "IMSG Completed - sendUDP");
                break;
            case receiveUDP:
                Log.d("ServiceTag", "IMSG Found - receiveUDP");
                receiveUDPPacket(timeout, port);
                Log.d("ServiceTag", "IMSG Completed - receiveUDP");
                break;
            case sendTCP:
                Log.d("ServiceTag", "IMSG Found - sendTCP");
                Log.d("ServiceTag", "IMSG Completed - sendTCP");
                break;
            case receiveTCP:
                Log.d("ServiceTag", "IMSG Found - receiveTCP");
                Log.d("ServiceTag", "IMSG Completed - receiveTCP");
                break;
            case startTCPServer:
                Log.d("ServiceTag", "IMSG Found - StartTCPServer");
                Log.d("ServiceTag", "IMSG Completed - StartTCPServer");
                break;
            case discover:
                Log.d("ServiceTag", "IMSG Found - discover");
                receiveBroadcast(port, discoveryMessage);
                Log.d("ServiceTag", "IMSG Completed - discover");
                break;
            case declare:
                Log.d("ServiceTag", "IMSG  - declare");
                sendBroadcast(port, discoveryMessage);
                Log.d("ServiceTag", "IMSG Completed - declare");
                break;
            case failed:
                Log.d("ServiceTag", "IMSG Found - Failed (*No Complete Message*)");
                //Intentional fall-through
            default: //The Param was either not included, or something went wrong
                Log.d("ServiceTag", "IMSG not found: " + msg);
                break;
        }
        Log.d("ServiceTag", "onHandleIntent End");
    }

    /**
     * @param context The current context.
     * @return Returns a String representation of the IPv4 address of this client
     */
    protected String wifiIpAddress(Context context)
    {
        Log.d("Debug Wifi", "Reached wifiIpAddress Start");
        WifiManager wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

        // Convert little-endian to big-endian if needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN))
            ipAddress = Integer.reverseBytes(ipAddress);

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try
        {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex)
        {
            ipAddressString = null;
        }

        Log.d("Debug Wifi", "Reached wifiIpAddress End");
        return ipAddressString;
    }

    /**
     * @return Returns the address that the local network uses for broadcast messages
     * @throws IOException I don't know why this throws an IOException. Possibly because
     *                     there is no guarantee that the WiFi is turned on.
     */
    private InetAddress getBroadcastAddress() throws IOException
    {
        Log.d("Debug Wifi", "Reached getBroadcastAddress Start");
        WifiManager wifi_service = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi_service.getDhcpInfo();
        if (dhcp == null)
        {
            Log.d("Debug Wifi", "Could not get DHCP info");
            return null;
        }

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);

        Log.d("Debug Wifi", "Reached getBroadcastAddress End");
        return InetAddress.getByAddress(quads);
    }


    /**
     * This is used to send an arbitrary message on the local broadcast channel. ex: 192.168.0.255
     *
     * @param port    The port on which the broadcast will be sent.
     * @param payload The message that is sent along with this UDP Packet.
     */
    private void sendBroadcast(int port, String payload)
    {
        Log.d("Debug Wifi", "Reached sendBroadcast Start");

        //TODO remove this and replace with something like MainActivity.getContext()
        if (payload.equals(""))
            payload = wifiIpAddress(this);
        //I don't know if getHostAddress() will work as expected. TODO Test this again
        try
        {
            sendUDPPacket(getBroadcastAddress().getHostAddress(), port, payload.getBytes());
        } catch (IOException e)
        {
            e.printStackTrace();
        }


        Log.d("Debug Wifi", "Reached sendBroadcast End");
    }

    /**
     * Calls receiveBroadcast(int, int) with a timeout of 5 seconds on port 14251
     * If another application is trying to use this port, behaviour of this code
     * is undefined.
     */
    public void receiveBroadcast()
    {
        receiveBroadcast(5000, 14251);
    }

    /**
     * Calls receiveBroadcast(int, int) with a timeout of 5 seconds.
     *
     * @param port The port on which the broadcast packets will be received.
     *             Using port 0 automatically selects the next available port.
     */
    public void receiveBroadcast(int port)
    {
        receiveBroadcast(5000, port);
    }

    /**
     * @param timeout The time to listen for incoming broadcast packets.
     * @param port    The port on which the broadcast packets will be received.
     *                Using port 0 automatically selects the next available port.
     */
    public void receiveBroadcast(int timeout, int port)
    {
        Log.d("Debug Wifi", "Reached receiveBroadcast Start");
        //Android filters out packets not explicitly addressed to it.
        //This disables that behaviour to allow those packets
        //These next 3 lines + release lock line is not my code. I can't remember when I found it.
        WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiManager.MulticastLock multicastLock = wm.createMulticastLock("mydebuginfo");
        multicastLock.acquire();

        receiveUDPPacket(timeout, port);

        //Release lock. Android will now filter out packets not explicitly addressed to it.
        //This saves power.
        multicastLock.release();
        Log.d("Debug Wifi", "Reached receiveBroadcast End");
    }


    /**
     * Gets a line delimited, using \n, list of devices which replied, with their message.
     * Usually their message will be their IP address.
     * @param port The port on which the packet will be received.
     * @param compare The message to ensure that this application does not receive packets
     *                intended for other application using the broadcast address.
     */
    public void receiveBroadcast(int port, String compare)
    {
        //discovering = true;
        byte[] buffer = new byte[defaultBufferSize];
        String deviceList = "";

        Log.d("ServiceTag", "ReceiveBroadcast buffer created, beginning discovery.");
        //while(discovering)
        //{
            receiveUDPPacket(10000, port, buffer, defaultBufferSize);
            String[] msg = (new String(buffer)).split("\n");
            try
            {
                if (msg[0].equals(compare))
                    deviceList += "\n" + msg[1];
            } catch(ArrayIndexOutOfBoundsException e)
            { e.printStackTrace(); }
            //TODO This is only for testing purposes.
            // As long as at 1 packet has been discovered, stop checking.
            //if(!deviceList.equals(""))
                //discovering = false;
        //}
        sendIntent(deviceList);
    }

    /**
     * WARNING: Does not support data types that can't be interpreted as Strings
     * Calls receiveUDPPacket(int, int, int) with timeout, port, and 1000 as the arguments.
     * I intend to change that later on. It should be fairly simple, since I am just
     * getting the byte array that the string is made up from and putting that in the packet.
     * Meaning that, in the future, any data that can't be interpreted as a Byte Array won't work.
     * @param timeout The time, in ms, before the device stops listening for a packet.
     * @param port The port on which the packet will be received.
     */
    public void receiveUDPPacket(int timeout, int port)
    {
        receiveUDPPacket(timeout, port, defaultBufferSize);
    }

    /**
     * Begins to receive UDP Packets.
     *
     * @param timeout The time, in ms, before the device stops listening for a packet.
     * @param port The port on which the packet will be received.
     * @param bufferSize The expected size of the packet.
     */
    public void receiveUDPPacket(int timeout, int port, int bufferSize)
    {
        receiveUDPPacket(timeout, port, new byte[bufferSize], bufferSize);
    }


    /**
     * WARNING: Does not support data types that can't be interpreted as Strings
     *
     * @param timeout The time, in ms, before the device stops listening for a packet.
     * @param port The port on which the packet will be received.
     * @param buffer The holder for the contents of the incoming packet
     * @param bufferSize The expected size of the packet. Packets smaller than the buffer appear to,
     *                   though I am not fully certain, simply trim the excess space.
     *                   This must be the same size as, or smaller than the buffer
     */
    public void receiveUDPPacket(int timeout, int port, byte[] buffer, int bufferSize)
    {
        Log.d("Debug Wifi", "Reached receiveUDPPacket Start");
        try
        {//Perhaps excessive debug messages are due to an error which was long ago corrected.
            Log.d("Debug RUDPP", "UDP Receive Try Started");
            DatagramPacket dataIn = new DatagramPacket(buffer, bufferSize);
            Log.d("Debug RUDPP", "UDP Packet Created");
            DatagramSocket sock = new DatagramSocket(null);
            Log.d("Debug RUDPP", "UDP Socket Created");
            sock.setReuseAddress(true);
            Log.d("Debug RUDPP", "UDP REUSEADDR");
            sock.bind(new InetSocketAddress(port));
            //sock.connect(getBroadcastAddress(), port); //This caused those errors
            Log.d("Debug RUDPP", "UDP Socket Bound");
            sock.setSoTimeout(timeout);
            Log.d("Debug RUDPP", "UDP Timeout Set");
            sock.receive(dataIn);
            Log.d("Debug RUDPP", "UDP Message received: " + new String(buffer));
            sock.close();
            Log.d("Debug RUDPP", "UDP Receive Try Ended");
        } catch (SocketTimeoutException e)
        {
            Log.d("Debug RUDPP", "Socket Timeout Exception");
            buffer = "No Packet Arrived - Socket Timed out.".getBytes();
        } catch (IOException e)
        {
            Log.d("Debug RUDPP", "IOException");
            e.printStackTrace();
        } catch (Exception e)
        {
            Log.d("Debug RB", "Generic Failure: " + e.getMessage());
        }

        //Convert the bytes in the packet to a String.
        if(!discovering)
            sendIntent(new String(buffer));
        Log.d("Debug Wifi", "Reached receiveUDPPacket End");
    }

    /**
     * Sets the Discovery Message for this application. The message cannot be "" or "null"..
     * @param discoveryMessage The message that the developer users to identify their application
     *                         to other devices using their application
     */
    public void setDiscoveryMessage(String discoveryMessage)
    {
        if (!discoveryMessage.equals("") && !discoveryMessage.equals("null"))
            this.discoveryMessage = discoveryMessage;
    }

    /**
     * Informs user what the currently used Discovery Message is.
     * @return The current Discovery Message
     */
    public String getDiscoveryMessage() { return discoveryMessage; }

    public void sendUDPPacket(String ip, String payload) { sendUDPPacket(ip, 0, payload.getBytes()); }
    public void sendUDPPacket(String ip, int port, String payload) { sendUDPPacket(ip, port, payload.getBytes()); }
    public void sendUDPPacket(String ip, int port, byte[] payload)
    {
        Log.d("Wifi Debug", "Reached sendUDPPacket Start");
        DatagramPacket dp = null;
        DatagramSocket sock;

        //Create the packet that is about to be sent
        try {
            dp = new DatagramPacket(payload, payload.length, new InetSocketAddress(ip, port));
            Log.d("Debug Wifi", "DP created"); }
        catch (IOException e) {
            e.printStackTrace(); }



        //Send some message on the new socket using the packet
        try {
            sock = new DatagramSocket(null);
            sock.setReuseAddress(true);
            sock.connect(new InetSocketAddress(ip, port));
            sock.send(dp);
            sock.close();
            Log.d("Debug Wifi", "DP Sent");
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("Debug Wifi", "IOExeception. " + e.getMessage());
        } catch (NullPointerException e) {
            e.printStackTrace();
            Log.d("Debug Wifi", "Null Pointer Exception. " + e.getMessage());
        } catch (IllegalStateException e) {
            e.printStackTrace();
            Log.d("Debug Wifi", "Illegal State Exception. " + e.getMessage()); }

        Log.d("Wifi Debug", "Reached sendUDPPacket End");
    }



    //This is some dumb code. I don't understand it at all. I do not enjoy TCP
    public void openTCPServer(int port) { openTCPServer(port, 120000); }
    public void openTCPServer(int port, int timeout)
    {
        Log.d("Debug Wifi", "Reached openTCPServer Start");
        while(true)
        {
            try
            {
                Log.d("Debug TCPS", "Opening and accepting Clients");
                servSock = new ServerSocket(port);
                Socket clientSock = servSock.accept();
                Log.d("Debug TCPS", "Client found and accepted");

                try
                {
                    Log.d("Debug TCPS", "Reading from Client");
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(clientSock.getInputStream()));
                    String line = null;
                    while((line = in.readLine()) != null)
                    {
                        //This is where I do any needed processing with this particular packet
                        //If I have multiple client requests from 1 process interconnected,
                        //will this cause issues, or is that even possible?
                        sendIntent(SERVER_MSG, line);
                        Log.d("Debug TCPS", "Client information returned to calling program");
                    }
                    break;
                } catch (IOException e)
                { e.printStackTrace(); }
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        Log.d("Debug Wifi", "Reached openTCPServer Start");
    }

    public void closeTCPServer()
    {
        try
        { servSock.close();
        } catch (IOException e)
        { e.printStackTrace(); }
    }

    public void TCPClient(String ip, int port, String payload)
    {

        try
        {
            InetAddress serverAddr = InetAddress.getByName(ip);
            Log.d("Debug Wifi", "C: Connecting...");
            Socket socket = new Socket(serverAddr, port);
            connected = true;
            while (connected)
            {
                try
                {
                    Log.d("Debug Wifi", "TCP Client Sending message.");
                    PrintWriter out = new PrintWriter(new BufferedWriter(
                            new OutputStreamWriter(socket.getOutputStream())), true);
                    //Where do I determine that I should shut down the connection?
                    out.println(payload);
                    Log.d("Debug Wifi", "TCP Client Sent.");
                } catch (Exception e) {
                    Log.d("Debug Wifi", "TCP Server Error");
                    e.printStackTrace();
                }
            }
            socket.close();
            Log.d("Debug Wifi", "TCP Client Closed.");
        } catch (Exception e) {
            Log.d("Debug Wifi", "TCP Client Error", e);
            connected = false;
        }


    }

}
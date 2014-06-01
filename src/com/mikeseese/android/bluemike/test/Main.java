package com.mikeseese.android.bluemike.test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;

import com.mikeseese.android.bluemike.Network;
import com.mikeseese.android.bluemike.BMConstants;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class Main extends ListActivity
{
	private Network mNetwork;
	private Context mContext;
	private final Handler mHandler;
	
	private int testno;
	
	public ArrayAdapter<String> btArrayAdapter;
	public ArrayList<BluetoothDevice> btDevices;
	
	public Main()
	{
		super();
		
		testno = 0;
		
		//Looper.prepare();
		mHandler = new Handler()
		{
            public void handleMessage(Message msg)
            {
        		if(msg.what == BMConstants.MESSAGE_READ)
        		{
                	int dataLen = msg.arg1;
                	byte[] data = (byte[]) msg.obj;
                	String message = new String(data).substring(0, dataLen); //substring needed to get rid of nasty null characters!
                	Toast.makeText(mContext, message.trim(), Toast.LENGTH_SHORT).show();
                	
                	String[] vars;
                	String delimiter = ",";
                	String deviceAddr;
                	vars = message.split(delimiter);
                	
                	if(vars.length > 0 && vars[0].equals("$NEWDEV"))
                	{
                		//new device! add it to the network
                		if(vars.length > 1)
                		{
                			deviceAddr = vars[1];
                			mNetwork.connectTo(deviceAddr);
                		}
                	}
                	else if(vars.length > 0 && vars[0].equals("$NUMDEV"))
                	{
                		//new device! add it to the network
                		if(vars.length > 1)
                		{
                			int i = new Integer(vars[1]);
                		}
                	}
        		}
        		if(msg.what == BMConstants.DEVICE_ADDED)
        		{
                	Toast.makeText(mContext, "Device Added.", Toast.LENGTH_SHORT).show();
        		}
        		if(msg.what == BMConstants.NETWORK_JOINED)
        		{
                	Toast.makeText(mContext, "Network Joined.", Toast.LENGTH_SHORT).show();
        		}
        		if(msg.what == BMConstants.NEW_DEVICE)
        		{
                	Toast.makeText(mContext, "New Device.", Toast.LENGTH_SHORT).show();
        		}
            }
		};
		//Looper.loop();
	}
	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.main);
		
		mContext = getApplicationContext();
		mNetwork = new Network(mHandler);
		
		ListView listDevicesFound = (ListView) findViewById(android.R.id.list);
        btArrayAdapter = new ArrayAdapter<String>(Main.this, android.R.layout.simple_list_item_1);
        listDevicesFound.setAdapter(btArrayAdapter);
        
        btDevices = new ArrayList<BluetoothDevice>();

		registerReceiver(ActionFoundReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
		registerReceiver(ActionFoundReceiver, new IntentFilter("android.bleutooth.device.action.UUID"));
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		switch(requestCode)
		{
			case 0:
				Toast.makeText(mContext, "BT enabled. Press button again.", 2).show();
				break;
			case 1:
				Toast.makeText(mContext, "Network created.", 2).show();
				mNetwork.CreateNetwork("fuckbitches");
				break;
			default:
				break;
		}
	}
	
	public void doServer(View v)
	{
		if (!mNetwork.BTIsEnabled())
		{
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBtIntent, 0);
		}
		else
		{
			Intent discoverableIntent = new
			Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivityForResult(discoverableIntent, 1);
		}
	}
	
	public void doClient(View v)
	{
		if (!mNetwork.BTIsEnabled())
		{
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBtIntent, 0);
		}
		else
		{
			//look for devices?
			mNetwork.FindNetworks();
		}
	}
	
	public void sendData(View v)
	{
		mNetwork.broadcast("test"+testno);
		testno++;
	}
	
	public void sdpSearch(BluetoothDevice device)
	{
		try
	    {
	        Class cl = Class.forName("android.bluetooth.BluetoothDevice");
	        Class[] par = {};
	        Object[] args = {};
	        Method method = cl.getMethod("fetchUuidsWithSdp", par);
	        method.invoke(device, args);
	    }
	    catch (Exception e)
	    {
	        e.printStackTrace();
	    }
	}
	
	public ParcelUuid[] servicesFromDevice(BluetoothDevice device)
	{
	    try
	    {
	        Class cl = Class.forName("android.bluetooth.BluetoothDevice");
	        Class[] par = {};
	        Object[] args = {};
	        Method method = cl.getMethod("getUuids", par);
	        ParcelUuid[] retval = (ParcelUuid[]) method.invoke(device, args);
	        return retval;
	    }
	    catch (Exception e)
	    {
	        e.printStackTrace();
	        return null;
	    }
	}
	
	private final BroadcastReceiver ActionFoundReceiver = new BroadcastReceiver()
	{
		  @Override
		  public void onReceive(Context context, Intent intent)
		  {
			  String action = intent.getAction();
			  if(BluetoothDevice.ACTION_FOUND.equals(action))
			  {
				  BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				  sdpSearch(device);
				  btDevices.add(device);
				  btArrayAdapter.add(device.getName() + "\n" + device.getAddress());
		          btArrayAdapter.notifyDataSetChanged();
		      }
			  else if(action.equals("android.bleutooth.device.action.UUID"))
			  {
				  BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				  ParcelUuid[] pUUID = servicesFromDevice(device);
				  if(pUUID != null)
				  {
					  for(int i = 0; i < pUUID.length; i++)
					  {
						  String name = device.getName();
						  UUID u = pUUID[i].getUuid();
						  String currentUUID = u.toString();
						  String mUUID = mNetwork.mUUID.toString();
						  if(currentUUID.equals(mUUID))
						  {
							  //btDevices.add(device);
							  btArrayAdapter.add(device.getName() + "\n" + device.getAddress());
					          btArrayAdapter.notifyDataSetChanged();
						  }
					  }
				  }
			  }
		  }
	};
	
	 @Override
	 protected void onListItemClick(ListView l, View v, int position, long id)
	 {
		 mNetwork.JoinNetwork(btDevices.get(position));
		 l.setVisibility(View.GONE);
	 }
}

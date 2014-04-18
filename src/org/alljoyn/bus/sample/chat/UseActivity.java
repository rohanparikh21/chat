/*
 * Copyright (c) 2011, AllSeen Alliance. All rights reserved.
 *
 *    Permission to use, copy, modify, and/or distribute this software for any
 *    purpose with or without fee is hereby granted, provided that the above
 *    copyright notice and this permission notice appear in all copies.
 *
 *    THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 *    WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 *    MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 *    ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 *    WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 *    ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 *    OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package org.alljoyn.bus.sample.chat;

import org.alljoyn.bus.sample.chat.ChatApplication;
import org.alljoyn.bus.sample.chat.Observable;
import org.alljoyn.bus.sample.chat.Observer;
import org.alljoyn.bus.sample.chat.DialogBuilder;

import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.os.SystemClock;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;

import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import android.util.Log;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class UseActivity extends Activity implements Observer {
    private static final String TAG = "chat.UseActivity";
    
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate()");
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.use);
        
        this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        mHistoryList = new ArrayAdapter<String>(this, android.R.layout.test_list_item);
        ListView hlv = (ListView) findViewById(R.id.useHistoryList);
        hlv.setAdapter(mHistoryList);
        
        EditText messageBox = (EditText)findViewById(R.id.useMessage);
        messageBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                	String message = view.getText().toString();
                    Log.i(TAG, "useMessage.onEditorAction(): got message " + message + ")");
    	            mChatApplication.newLocalUserMessage(message);
    	            view.setText("");
                }
                return true;
            }
        });
                
        mServiceAdvertisement = (Button) findViewById(R.id.serviceAdvertisement);
        mServiceAdvertisement.setOnClickListener(new View.OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				services = "Info "+batteryStatus;
		        mChatApplication.newLocalUserMessage(services);
		        mHandler.postDelayed(startAdvertising, time_interval);
		        mHandler.postDelayed(getArbitrator, time_interval2);
			}
        	
        });
        
        mJoinButton = (Button) findViewById(R.id.useJoin);
        mJoinButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				showDialog(DIALOG_JOIN_ID);
			}
		});
        
        mLeaveButton = (Button) findViewById(R.id.useLeave);
        mLeaveButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				showDialog(DIALOG_LEAVE_ID);
			}
		});
        
        mChannelName = (TextView)findViewById(R.id.useChannelName);
        mChannelStatus = (TextView)findViewById(R.id.useChannelStatus);
        
        /*
         * Keep a pointer to the Android Appliation class around.  We use this
         * as the Model for our MVC-based application.    Whenever we are started
         * we need to "check in" with the application so it can ensure that our
         * required services are running.
         */
        mChatApplication = (ChatApplication)getApplication();
        mChatApplication.checkin();
        
        /*
         * Call down into the model to get its current state.  Since the model
         * outlives its Activities, this may actually be a lot of state and not
         * just empty.
         */
        updateChannelState();
        updateHistory();
        /*
         * Now that we're all ready to go, we are ready to accept notifications
         * from other components.
         */
        mChatApplication.addObserver(this);

    }
    
    // Broadcast reciever for battery status info -- Rohan
    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context ctxt, Intent intent) {
          int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
          batteryStatus = (String.valueOf(level) + "%");
        }
      };
    
	public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        mChatApplication = (ChatApplication)getApplication();
        mChatApplication.deleteObserver(this);
    	super.onDestroy();
	}
    
    public static final int DIALOG_JOIN_ID = 0;
    public static final int DIALOG_LEAVE_ID = 1;
    public static final int DIALOG_ALLJOYN_ERROR_ID = 2;
    public String services;

    protected Dialog onCreateDialog(int id) {
    	Log.i(TAG, "onCreateDialog()");
        Dialog result = null;
        switch(id) {
        case DIALOG_JOIN_ID:
	        { 
	        	DialogBuilder builder = new DialogBuilder();
	        	result = builder.createUseJoinDialog(this, mChatApplication);
	        }        	
        	break;
        case DIALOG_LEAVE_ID:
	        { 
	        	DialogBuilder builder = new DialogBuilder();
	        	result = builder.createUseLeaveDialog(this, mChatApplication);
	        }
	        break;
        case DIALOG_ALLJOYN_ERROR_ID:
	        { 
	        	DialogBuilder builder = new DialogBuilder();
	        	result = builder.createAllJoynErrorDialog(this, mChatApplication);
	        }
	        break;	        
        }
        return result;
    }
    
    public synchronized void update(Observable o, Object arg) {
        Log.i(TAG, "update(" + arg + ")");
        String qualifier = (String)arg;
        
        if (qualifier.equals(ChatApplication.APPLICATION_QUIT_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_APPLICATION_QUIT_EVENT);
            mHandler.sendMessage(message);
        }
        
        if (qualifier.equals(ChatApplication.HISTORY_CHANGED_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_HISTORY_CHANGED_EVENT);
            mHandler.sendMessage(message);
        }
        
        if (qualifier.equals(ChatApplication.USE_CHANNEL_STATE_CHANGED_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_CHANNEL_STATE_CHANGED_EVENT);
            mHandler.sendMessage(message);
        }
        
        if (qualifier.equals(ChatApplication.ALLJOYN_ERROR_EVENT)) {
            Message message = mHandler.obtainMessage(HANDLE_ALLJOYN_ERROR_EVENT);
            mHandler.sendMessage(message);
        }
    }
    
    private void updateHistory() {
        Log.i(TAG, "updateHistory()");
	    mHistoryList.clear();
	    List<String> messages = mChatApplication.getHistory();
        for (String message : messages) {
            mHistoryList.add(message);
        }
	    mHistoryList.notifyDataSetChanged();
    }
    
    private void updateChannelState() {
        Log.i(TAG, "updateHistory()");
    	AllJoynService.UseChannelState channelState = mChatApplication.useGetChannelState();
    	String name = mChatApplication.useGetChannelName();
    	if (name == null) {
    		name = "Not set";
    	}
        mChannelName.setText(name);
        switch (channelState) {
        case IDLE:
            mChannelStatus.setText("Idle");
            mJoinButton.setEnabled(true);
            mLeaveButton.setEnabled(false);
            mServiceAdvertisement.setEnabled(false);
            break;
        case JOINED:
            mChannelStatus.setText("Joined");
            mJoinButton.setEnabled(false);
            mLeaveButton.setEnabled(true);
            mServiceAdvertisement.setEnabled(true);
            break;	
        }
    }
    
    /**
     * An AllJoyn error has happened.  Since this activity pops up first we
     * handle the general errors.  We also handle our own errors.
     */
    private void alljoynError() {
    	if (mChatApplication.getErrorModule() == ChatApplication.Module.GENERAL ||
    		mChatApplication.getErrorModule() == ChatApplication.Module.USE) {
    		showDialog(DIALOG_ALLJOYN_ERROR_ID);
    	}
    }
    
    private static final int HANDLE_APPLICATION_QUIT_EVENT = 0;
    private static final int HANDLE_HISTORY_CHANGED_EVENT = 1;
    private static final int HANDLE_CHANNEL_STATE_CHANGED_EVENT = 2;
    private static final int HANDLE_ALLJOYN_ERROR_EVENT = 3;
    private static final int HANDLE_SERVICE_ADVERTISEMENT = 4;// For service advertisements
    private static final int time_interval = 10000; // Time interval for service advertisements
    private static final int time_interval2 = 5000;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case HANDLE_APPLICATION_QUIT_EVENT:
	            {
	                Log.i(TAG, "mHandler.handleMessage(): HANDLE_APPLICATION_QUIT_EVENT");
	                finish();
	            }
	            break; 
            case HANDLE_HISTORY_CHANGED_EVENT:
                {
                    Log.i(TAG, "mHandler.handleMessage(): HANDLE_HISTORY_CHANGED_EVENT");
                    updateHistory();
                    break;
                }
            case HANDLE_CHANNEL_STATE_CHANGED_EVENT:
	            {
	                Log.i(TAG, "mHandler.handleMessage(): HANDLE_CHANNEL_STATE_CHANGED_EVENT");
	                updateChannelState();
	                break;
	            }
            case HANDLE_ALLJOYN_ERROR_EVENT:
	            {
	                Log.i(TAG, "mHandler.handleMessage(): HANDLE_ALLJOYN_ERROR_EVENT");
	                alljoynError();
	                break;
	            }
            default:
                break;
            }
        }
    };
    
    public void startAdvertising(){

    	if(mChatApplication.useGetChannelState() == AllJoynService.UseChannelState.JOINED){
    		
            mChatApplication.newLocalUserMessage(services);

        }
        else{
            		//do nothing
        }
    }
    
    Runnable startAdvertising = new Runnable(){

		@Override
		public void run() {
			// TODO Auto-generated method stub
			startAdvertising();
			mHandler.postDelayed(startAdvertising, time_interval);
		}
    	
    };
    
    Runnable getArbitrator = new Runnable(){

		@Override
		public void run() {
			// TODO Auto-generated method stub
			
			if(mChatApplication.getHashtable().isEmpty() == false){
				Log.d(TAG,mChatApplication.getArbitrator());
			}
			else{
				Log.d(TAG,"No entry present in the hashtable");
			}
			mHandler.postDelayed(getArbitrator, time_interval2);
		}
    	
    };
    
    private ChatApplication mChatApplication = null;
    
    private ArrayAdapter<String> mHistoryList;
    
    //Button for service advertisements -- Rohan
    private Button mServiceAdvertisement;
    private Button mJoinButton;
    private Button mLeaveButton;
    
    // String for battery status -- Rohan
    private String batteryStatus;
    
    private TextView mChannelName;
      
    private TextView mChannelStatus;
}

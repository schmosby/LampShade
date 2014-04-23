package com.kuxhausen.huemore.net;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;

import com.kuxhausen.huemore.net.hue.HubConnection;
import com.kuxhausen.huemore.network.OnConnectionStatusChangedListener;
import com.kuxhausen.huemore.persistence.DatabaseDefinitions;
import com.kuxhausen.huemore.state.Group;

public class DeviceManager {
	
	private ArrayList<Connection> mConnections;
	private Context mContext;
	private Group selectedGroup;
	private String selectedGroupName;
	private ArrayList<OnConnectionStatusChangedListener> connectionListeners = new ArrayList<OnConnectionStatusChangedListener>();
	public ArrayList<OnStateChangedListener> brightnessListeners = new ArrayList<OnStateChangedListener>();
	private HashMap<Long, NetworkBulb> bulbMap;
	private MyObserver mConnectionObserver;
	
	public DeviceManager(Context c){
		mContext = c;
		
		mConnectionObserver = new MyObserver(null);
		mContext.getContentResolver().registerContentObserver(DatabaseDefinitions.NetConnectionColumns.URI, true, mConnectionObserver);
		
		loadEverythingFromDatabase();
	}
	
	public void loadEverythingFromDatabase(){
		
		//load all connections from the database 
		mConnections = new ArrayList<Connection>();
		mConnections.addAll(HubConnection.loadHubConnections(mContext, this));
		
		//load all network bulbs from the connections
		bulbMap = new HashMap<Long, NetworkBulb>();
		for(Connection con : mConnections){
			for(NetworkBulb bulb :con.getBulbs()){
				bulbMap.put(bulb.getBaseId(), bulb);
			}
		}
		
		onBulbsListChanged();
	}
	
	public void onDestroy() {
		for(Connection c : mConnections)
			c.onDestroy();
	}
	
	public Group getSelectedGroup(){
		return selectedGroup;
	}
	public String getSelectedGroupName(){
		return selectedGroupName;
	}
	
	public void onGroupSelected(Group group, Integer optionalBri){
		selectedGroup = group;
		selectedGroupName = group.getName();
		
		//TODO
	}
	
	public void addOnConnectionStatusChangedListener(OnConnectionStatusChangedListener l){
		connectionListeners.add(l);
	}
	
	public void removeOnConnectionStatusChangedListener(OnConnectionStatusChangedListener l){
		connectionListeners.remove(l);
	}
	
	public void onConnectionChanged(){
			for(OnConnectionStatusChangedListener l : connectionListeners)
				l.onConnectionStatusChanged();	
	}
	

	public interface OnStateChangedListener {
		public void onStateChanged();
	}
	
	/** announce brightness to any listeners **/
	public void onStateChanged(){
		for(OnStateChangedListener l : brightnessListeners){
			l.onStateChanged();
		}
	}
	public void registerBrightnessListener(OnStateChangedListener l){
		brightnessListeners.add(l);
	}
	
	public void removeBrightnessListener(OnStateChangedListener l){
		brightnessListeners.remove(l);
	}
	
	public Integer getBrightness(Group g){
		//TODO	
		return 100;
	}
	
	public Integer getMaxBrightness(Group g){
		//TODO
		return 100;
	}
	
	/** doesn't notify listeners **/
	public void setBrightness(Group g, int brightness){
		//TODO
	}
	
	public void onBulbsListChanged(){
		for(Connection con : mConnections){
			ArrayList<NetworkBulb> conBulbs = con.getBulbs();
			for(NetworkBulb bulb: conBulbs){
				bulbMap.put(bulb.getBaseId(), bulb);
			}
		}
	}
	
	public NetworkBulb getNetworkBulb(Long bulbDeviceId) {
		return bulbMap.get(bulbDeviceId);
	}
	
	class MyObserver extends ContentObserver {
		public MyObserver(Handler handler) {
			super(handler);
		}
		
		@Override
		public void onChange(boolean selfChange) {
			this.onChange(selfChange, null);
		}
		@Override
		public void onChange(boolean selfChange, Uri uri) {
			loadEverythingFromDatabase();
		}     
	}
}
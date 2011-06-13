package com.dellingertechnologies.javajukebox;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dellingertechnologies.javajukebox.RestClient.RequestMethod;

public class Jukebox extends Activity {

	protected static final long REFRESH_DELAY = 5000;

	private Handler refreshHandler = new Handler();

	private String host;

	private String port;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		ControlListener controlListener = new ControlListener(this);
		findViewById(R.id.volumeButton).setOnClickListener(controlListener);
		findViewById(R.id.restartButton).setOnClickListener(controlListener);
		findViewById(R.id.pauseButton).setOnClickListener(controlListener);
		findViewById(R.id.playButton).setOnClickListener(controlListener);
		findViewById(R.id.skipButton).setOnClickListener(controlListener);
	}

	private void loadPreferences() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		host = prefs.getString("hostPref", "localhost");
		port = prefs.getString("portPref", "9999");
	}

	public String getServiceUrl(){
		return "http://"+host+":"+port+"/service/jukebox";
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		refreshHandler.removeCallbacks(refreshTask);
	}

	@Override
	protected void onResume() {
		super.onResume();
		loadPreferences();
		refreshHandler.removeCallbacks(refreshTask);
		refreshHandler.post(refreshTask);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater menuInflater = getMenuInflater();
    	menuInflater.inflate(R.menu.options, menu);
    	return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
	    case R.id.menu_settings:
	    	Intent settingsActivity = new Intent(getBaseContext(), Preferences.class);
	    	startActivity(settingsActivity);
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}

	private Runnable refreshTask = new Runnable() {
		
		@Override
		public void run() {
			new UpdateNowPlayingTask().execute();
			refreshHandler.postDelayed(this, REFRESH_DELAY);
		}
	};

	private double volume;
	
	public void refreshStatus() {
		new UpdateNowPlayingTask().execute();
	}
	
	private class UpdateNowPlayingTask extends
			AsyncTask<Void, Void, JSONObject> {

		@Override
		protected JSONObject doInBackground(Void... arg0) {
			RestClient client = new RestClient(getServiceUrl() + "/status");

			try {
				Log.d("jukebox", "Calling status service");
				client.execute(RequestMethod.GET);
				return new JSONObject(client.getResponse());
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(JSONObject result) {
			super.onPostExecute(result);
			if (result != null) {
				try {
					if(result.getBoolean("playing")){
						findViewById(R.id.playButton).setVisibility(View.GONE);
						findViewById(R.id.pauseButton).setVisibility(View.VISIBLE);
					}else{
						findViewById(R.id.playButton).setVisibility(View.VISIBLE);
						findViewById(R.id.pauseButton).setVisibility(View.GONE);
					}
					
					JSONObject current = result.getJSONObject("current");
					updateTextView(R.id.titleValue, current, "title");
					updateTextView(R.id.albumValue, current, "album");
					updateTextView(R.id.artistValue, current, "author");
					
					double maxFrames = result.getJSONObject("current").getDouble("mp3.length.frames");
					double curFrame = result.getJSONObject("progress").getDouble("mp3.frame");
					int progress = (int)(curFrame/maxFrames*100);
					ProgressBar currentProgress = (ProgressBar) findViewById(R.id.currentProgress);
					currentProgress.setProgress(progress);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		private void updateTextView(int id, JSONObject json, String key){
			TextView view = (TextView) findViewById(id);
			String text = "";
			try{
				if(json.has(key)){
					text = json.getString(key);
				}
			}catch(Exception e){}
			view.setText(text);
		}

	}

	public double getVolume(){
		return volume;
	}
	public void setVolume(double volume) {
		this.volume = volume;
	}

	public void updateVolume(double d) {
		new AsyncTask<Double, Void, JSONObject>(){

			@Override
			protected JSONObject doInBackground(Double... params) {
				RestClient client = new RestClient(getServiceUrl() + "/volume");
				client.addParam("volume", String.valueOf(params[0]));
				try {
					Log.d("jukebox", "Calling update volume service");
					client.execute(RequestMethod.POST);
					return new JSONObject(client.getResponse());
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}

			@Override
			protected void onPostExecute(JSONObject result) {
				super.onPostExecute(result);
				try{
					setVolume(result.getDouble("volume"));
				}catch(Exception e){
					e.printStackTrace();
				}
			}
			
		}.execute(d);
	}
}
package com.dellingertechnologies.javajukebox;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.dellingertechnologies.javajukebox.RestClient.RequestMethod;
import com.dellingertechnologies.javajukebox.services.GravatarService;

public class Jukebox extends Activity {

	protected static final long REFRESH_DELAY = 5000;
	private Handler refreshHandler = new Handler();
	private double volume;
	protected AlertDialog explicitDialog;
	private String host;
	private String port;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setProgressBarIndeterminate(true);
		setContentView(R.layout.main);
		
		ControlListener controlListener = new ControlListener(this);
		findViewById(R.id.volumeButton).setOnClickListener(controlListener);
		findViewById(R.id.restartButton).setOnClickListener(controlListener);
		findViewById(R.id.pauseButton).setOnClickListener(controlListener);
		findViewById(R.id.playButton).setOnClickListener(controlListener);
		findViewById(R.id.skipButton).setOnClickListener(controlListener);
		findViewById(R.id.likeButton).setOnClickListener(controlListener);
		findViewById(R.id.dislikeButton).setOnClickListener(controlListener);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Do you want to mark this track explicit?")
		       .setCancelable(false)
		       .setPositiveButton("Yes", controlListener)
		       .setNegativeButton("No", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                dialog.cancel();
		           }
		       });
		explicitDialog = builder.create();

		findViewById(R.id.explicitButton).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				explicitDialog.show();
			}
		});
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
		refreshStatus();
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
	    	startActivity(new Intent(getBaseContext(), Preferences.class));
	        return true;
	    case R.id.coming_up:
	    	startActivity(new Intent(getBaseContext(), Queue.class));
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

	public void refreshStatus() {
		new UpdateNowPlayingTask().execute();
	}
	
	private class UpdateNowPlayingTask extends AsyncTask<Void, Void, JSONObject> {

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
					
					JSONObject track = result.getJSONObject("track");
					updateTextView(R.id.titleValue, track, "title");
					updateTextView(R.id.albumValue, track, "album");
					updateTextView(R.id.artistValue, track, "artist");
					updateTextView(R.id.fileValue, track, "file");
					
					double progress = result.getDouble("progress");
					int progressState = (int)(progress*100);
					ProgressBar currentProgress = (ProgressBar) findViewById(R.id.currentProgress);
					currentProgress.setProgress(progressState);
					
					TextView ratingValue = (TextView) findViewById(R.id.ratingValue);
					if(result.has("rating")){
						String rating = result.getString("rating");
						if("LIKE".equals(rating)){
							ratingValue.setText("You JUKED this!");
							ratingValue.setTextColor(Color.GREEN);
							findViewById(R.id.likeButton).setVisibility(View.GONE);
							findViewById(R.id.dislikeButton).setVisibility(View.GONE);
							ratingValue.setVisibility(View.VISIBLE);
						}else if("DISLIKE".equals(rating)){
							ratingValue.setText("You JUNKED this!");
							ratingValue.setTextColor(Color.RED);
							findViewById(R.id.likeButton).setVisibility(View.GONE);
							findViewById(R.id.dislikeButton).setVisibility(View.GONE);
							ratingValue.setVisibility(View.VISIBLE);
						}
					}else{
						findViewById(R.id.likeButton).setVisibility(View.VISIBLE);
						findViewById(R.id.dislikeButton).setVisibility(View.VISIBLE);
						ratingValue.setVisibility(View.GONE);
					}
					
					ImageView explicitImage = (ImageView) findViewById(R.id.explicitImage);
					Button explicitButton = (Button) findViewById(R.id.explicitButton);
					if(track.has("explicit") && track.getBoolean("explicit")){
						explicitImage.setVisibility(View.VISIBLE);
						explicitButton.setVisibility(View.GONE);
					}else{
						explicitImage.setVisibility(View.GONE);
						explicitButton.setVisibility(View.VISIBLE);
					}
					
					ImageView gravatar = (ImageView) findViewById(R.id.main_gravatar);
					JSONObject user = track.getJSONObject("user");
					Drawable d = new GravatarService()
						.getGravatar(user.has("gravatarId") ? user.getString("gravatarId") : null);
					gravatar.setImageDrawable(d);

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
		setProgressBarIndeterminateVisibility(true);
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
					setProgressBarIndeterminateVisibility(false);
				}catch(Exception e){
					e.printStackTrace();
				}
			}
			
		}.execute(d);
	}
}
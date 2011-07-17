package com.dellingertechnologies.javajukebox;

import org.json.JSONObject;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.dellingertechnologies.javajukebox.RestClient.RequestMethod;

public class ControlListener implements OnClickListener, android.content.DialogInterface.OnClickListener{

	Jukebox jukebox = null;
	
	public ControlListener(Jukebox jukebox){
		this.jukebox = jukebox;
	}
	
	@Override
	public void onClick(View v) {
		switch(v.getId()){
			case R.id.restartButton:
				callService("restart");
				break;
			case R.id.pauseButton:
				callService("pause");
				break;
			case R.id.playButton:
				callService("resume");
				break;
			case R.id.skipButton:
				callService("skip");
				break;
			case R.id.volumeButton:
				callVolumeService();
				break;
			case R.id.likeButton:
				callService("like");
				break;
			case R.id.dislikeButton:
				callService("dislike");
				break;
		}
	}

	private void callVolumeService() {
		jukebox.setProgressBarIndeterminateVisibility(true);
		new AsyncTask<Void, Void, JSONObject>(){

			@Override
			protected JSONObject doInBackground(Void... params) {
				try {
					RestClient client = new RestClient(jukebox.getServiceUrl() + "/volume");
					Log.d("jukebox", "Calling service: volume");
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
				if(result != null){
					try{
						jukebox.setProgressBarIndeterminateVisibility(false);
						jukebox.setVolume(result.getDouble("volume"));
						Dialog dialog = new VolumeDialog(jukebox, jukebox);
						dialog.setOwnerActivity(jukebox);
						dialog.show();
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			}
			
		}.execute();
	}

	private void postToService(String command) {
		new AsyncTask<String, Void, Void>(){
			@Override
			protected Void doInBackground(String... params) {
				try {
					RestClient client = new RestClient(jukebox.getServiceUrl() + "/" + params[0]);
					Log.d("jukebox", "Calling POST service: " + params[0]);
					client.execute(RequestMethod.POST);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				jukebox.refreshStatus();
			}
		}.execute(command);
	}
	
	private void callService(String command) {
		jukebox.setProgressBarIndeterminateVisibility(true);
		new AsyncTask<String, Void, Void>(){
			@Override
			protected Void doInBackground(String... params) {
				try {
					RestClient client = new RestClient(jukebox.getServiceUrl() + "/" + params[0]);
					Log.d("jukebox", "Calling GET service: " + params[0]);
					client.execute(RequestMethod.GET);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				jukebox.setProgressBarIndeterminateVisibility(false);
				jukebox.refreshStatus();
			}
		}.execute(command);
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		if(dialog == jukebox.explicitDialog && which == dialog.BUTTON_POSITIVE){
			postToService("explicit");
			dialog.dismiss();
		}
	}

}

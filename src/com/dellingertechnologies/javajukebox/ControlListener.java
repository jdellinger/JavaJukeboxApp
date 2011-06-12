package com.dellingertechnologies.javajukebox;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import com.dellingertechnologies.javajukebox.RestClient.RequestMethod;

public class ControlListener implements OnClickListener{

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
		}
	}

	private void callService(String command) {
		new AsyncTask<String, Void, Void>(){
			@Override
			protected Void doInBackground(String... params) {
				try {
					RestClient client = new RestClient(jukebox.getServiceUrl() + "/" + params[0]);
					Log.d("jukebox", "Calling service: " + params[0]);
					client.execute(RequestMethod.GET);
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

}

package com.dellingertechnologies.javajukebox;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.dellingertechnologies.javajukebox.RestClient.RequestMethod;
import com.dellingertechnologies.javajukebox.model.Track;
import com.dellingertechnologies.javajukebox.model.User;
import com.dellingertechnologies.javajukebox.services.GravatarService;

public class Queue extends ListActivity{

	private String host;
	private String port;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setProgressBarIndeterminate(true);
		setContentView(R.layout.queue);
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		loadPreferences();
		reloadQueue();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		final int trackId = (int) id;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Do you want to delete this track from the queue?")
		       .setCancelable(false)
		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   setProgressBarIndeterminateVisibility(true);
		               new RemoveTrackTask().execute(trackId);
		           }
		       })
		       .setNegativeButton("No", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                dialog.cancel();
		           }
		       });
		AlertDialog alert = builder.create();
		alert.show();
	}

	public void reloadQueue() {
		setProgressBarIndeterminateVisibility(true);
		new ReloadQueueTask().execute();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater menuInflater = getMenuInflater();
    	menuInflater.inflate(R.menu.queue_options, menu);
    	return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
	    case R.id.queue_add_track:
			setProgressBarIndeterminateVisibility(true);
	    	new AddTracksTask().execute(1);
	        return true;
	    case R.id.queue_add_five_tracks:
			setProgressBarIndeterminateVisibility(true);
	    	new AddTracksTask().execute(5);
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}

	private class RemoveTrackTask extends AsyncTask<Integer, Void, Void> {

		@Override
		protected Void doInBackground(Integer... params) {
			RestClient client = new RestClient(getServiceUrl() + "/queue/remove");
			if(params != null && params.length > 0){
				client.addParam("id", String.valueOf(params[0]));
				try {
					Log.d("jukebox", "Calling remove from queue service");
					client.execute(RequestMethod.GET);
				} catch (Exception e) {
					Log.w(Constants.JUKEBOX_TAG, "Exception calling remove from queue service", e);
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			setProgressBarIndeterminateVisibility(false);
			reloadQueue();
		}
		
	}

	private class AddTracksTask extends AsyncTask<Integer, Void, Void> {

		@Override
		protected Void doInBackground(Integer... params) {
			RestClient client = new RestClient(getServiceUrl() + "/queue/add");
			client.addParam("num", String.valueOf(params != null && params.length > 0 ? params[0] : 1));
			try {
				Log.d("jukebox", "Calling add to queue service");
				client.execute(RequestMethod.GET);
			} catch (Exception e) {
				Log.w(Constants.JUKEBOX_TAG, "Exception calling add to queue service", e);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			setProgressBarIndeterminateVisibility(false);
			reloadQueue();
		}
		
	}
	
	private class QueueAdapter implements ListAdapter {

		private Activity context;
		private List<Track> queue;

		public QueueAdapter(Activity context, List<Track> queue){
			this.context = context;
			this.queue = queue;
		}

		@Override
		public boolean areAllItemsEnabled() {
			return true;
		}

		@Override
		public boolean isEnabled(int position) {
			return true;
		}

		@Override
		public int getCount() {
			return queue.size();
		}

		@Override
		public Object getItem(int position) {
			return queue.get(position);
		}

		@Override
		public long getItemId(int position) {
			return queue.get(position).getId();
		}

		@Override
		public int getItemViewType(int position) {
			return R.layout.queue_row;
		}

		@Override
		public View getView(int position, View rowView, ViewGroup parent) {
			RowViewHolder holder;
			if(rowView == null){
				rowView = context.getLayoutInflater().inflate(R.layout.queue_row, null, true);
				
				holder = new RowViewHolder();
				holder.title = (TextView) rowView.findViewById(R.id.row_title);
				holder.album = (TextView) rowView.findViewById(R.id.row_album);
				holder.artist = (TextView) rowView.findViewById(R.id.row_artist);
				holder.gravatar = (ImageView) rowView.findViewById(R.id.row_gravatar);
				
				rowView.setTag(holder);
			}else{
				holder = (RowViewHolder) rowView.getTag();
			}
			Track track = queue.get(position);
			if(track.getTitle() == null || track.getTitle().trim().equals("")){
				holder.title.setText(track.getPath());
			}else{
				holder.title.setText(track.getTitle());
			}
			holder.album.setText(track.getAlbum());
			holder.artist.setText(track.getArtist());
			Drawable d = new GravatarService().getGravatar(track.getUser().getGravatarId());
			holder.gravatar.setImageDrawable(d);
			return rowView;
		}

		@Override
		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isEmpty() {
			return queue.isEmpty();
		}

		@Override
		public void registerDataSetObserver(DataSetObserver observer) {
			
		}

		@Override
		public void unregisterDataSetObserver(DataSetObserver observer) {
			
		}
	}
	
	private class RowViewHolder {
		public TextView title;
		public TextView album;
		public TextView artist;
		public ImageView gravatar;
	}
	
	private class ReloadQueueTask extends AsyncTask<Void, Void, JSONObject> {

		@Override
		protected JSONObject doInBackground(Void... arg0) {
			RestClient client = new RestClient(getServiceUrl() + "/queue");

			try {
				Log.d("jukebox", "Calling queue service");
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
			setProgressBarIndeterminateVisibility(false);
			if (result != null) {
				try {
					List<Track> tracks = buildTracks(result.getJSONArray("queue"));
					setListAdapter(new QueueAdapter(Queue.this, tracks));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private List<Track> buildTracks(JSONArray in){
		List<Track> tracks = new ArrayList<Track>();
		for(int i=0;i<in.length();i++){
			try{
				tracks.add(toTrack(in.getJSONObject(i)));
			}catch(Exception e){
				Log.w("jukebox", "Exception iterating tracks json", e);
			}
		}
		return tracks;
	}
	private Track toTrack(JSONObject json) {
		Track track = new Track();
		try{
			track.setId(json.getInt("id"));
			track.setTitle(json.getString("title"));
			track.setAlbum(json.getString("album"));
			track.setArtist(json.getString("artist"));
			track.setLikes(json.getInt("likes"));
			track.setDislikes(json.getInt("dislikes"));
			track.setSkips(json.getInt("skips"));
			track.setPlays(json.getInt("plays"));
			track.setExplicit(json.getBoolean("explicit"));
			track.setPath(json.getString("file"));
			
			JSONObject jsonUser = json.getJSONObject("user");
			User user = new User(jsonUser.getString("username"));
			if(jsonUser.has("gravatarId")){
				user.setGravatarId(jsonUser.getString("gravatarId"));
			}
			track.setUser(user);
		}catch(Exception e){
			Log.w("jukebox", "Exception parsing track", e);
		}
		return track;
	}

	private void loadPreferences() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		host = prefs.getString("hostPref", "localhost");
		port = prefs.getString("portPref", "9999");
	}

	public String getServiceUrl(){
		return "http://"+host+":"+port+"/service/jukebox";
	}
	

}

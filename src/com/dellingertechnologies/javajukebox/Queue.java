package com.dellingertechnologies.javajukebox;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ListActivity;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.dellingertechnologies.javajukebox.RestClient.RequestMethod;
import com.dellingertechnologies.javajukebox.model.Track;
import com.dellingertechnologies.javajukebox.model.User;

public class Queue extends ListActivity{

	private String host;
	private String port;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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

	public void reloadQueue() {
		new ReloadQueueTask().execute();
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
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = context.getLayoutInflater();
			Track track = queue.get(position);
			View rowView = inflater.inflate(R.layout.queue_row, null, true);
			ImageView gravatar = (ImageView) rowView.findViewById(R.id.row_gravatar);
			TextView titleView = (TextView) rowView.findViewById(R.id.row_title);
			if(track.getTitle() == null || track.getTitle().trim().equals("")){
				titleView.setText(track.getPath());
			}else{
				titleView.setText(track.getTitle());
			}
			TextView albumView = (TextView) rowView.findViewById(R.id.row_album);
			albumView.setText(track.getAlbum());
			TextView artistView = (TextView) rowView.findViewById(R.id.row_artist);
			artistView.setText(track.getArtist());
			try {
				URL url = new URL(getGravatarUrl(track.getUser()
						.getGravatarId()));
				InputStream content = (InputStream) url.getContent();
				Drawable d = Drawable.createFromStream(content, "src");
				gravatar.setImageDrawable(d);
			} catch (Exception e) {
				Log.w("jukebox", "Exception loading gravatar: "
						+ track.getUser().getGravatarId(), e);
			}
			return rowView;
		}

		private String getGravatarUrl(String gravatarId) {
			String id = gravatarId == null ? "" : gravatarId.trim();
			return "http://www.gravatar.com/avatar/"+id+".jpg?d=mm&r=pg&s=80";
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

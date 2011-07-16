package com.dellingertechnologies.javajukebox.services;

import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;

import com.dellingertechnologies.javajukebox.Constants;
import com.dellingertechnologies.javajukebox.util.SimpleCache;

import android.graphics.drawable.Drawable;
import android.util.Log;

public class GravatarService {

	private static SimpleCache<Drawable> cache = new SimpleCache<Drawable>();
	private String defaultCode = "mm";
	private String size = "80";
	private String rating = "pg";
	private MessageFormat urlFormat = new MessageFormat("http://www.gravatar.com/avatar/{0}.jpg?d={1}&r={2}&s={3}");
	
	public Drawable getGravatar(String id){
		String safeId = toSafeId(id);
		Drawable d = cache.get(safeId);
		if(d == null){
			d = retrieveFromRemote(safeId);
			if(d != null){
				cache.put(safeId, d);
			}
		}
		return d;
	}
	
	private String toSafeId(String id){
		return id == null ? "null_id" : id.trim();
	}

	private Drawable retrieveFromRemote(String id){
		Drawable d = null;
		try{
			URL url = new URL(getGravatarUrl(id));
			InputStream content = (InputStream) url.getContent();
			d = Drawable.createFromStream(content, "src");
		}catch(Exception e){
			Log.w(Constants.JUKEBOX_TAG, "Exception retrieving gravatar", e);
		}
		return d;
	}
	
	private String getGravatarUrl(String gravatarId) {
		return urlFormat.format(new String[]{
				gravatarId,
				defaultCode,
				rating,
				size
		});
	}

}

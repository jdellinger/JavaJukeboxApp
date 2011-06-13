package com.dellingertechnologies.javajukebox;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.SeekBar;

public class VolumeDialog extends Dialog implements OnClickListener {

	private Jukebox jukebox;

	public VolumeDialog(Context context, Jukebox jukebox) {
		super(context);
		this.jukebox = jukebox;
		setContentView(R.layout.volume_dialog);
		setTitle("Volume Control");
		setCancelable(true);
		
		SeekBar volumeSlider = (SeekBar) findViewById(R.id.volumeSlider);
		volumeSlider.setProgress((int)(jukebox.getVolume()*100));
		
		findViewById(R.id.volumeOkButton).setOnClickListener(this);
		findViewById(R.id.volumeCancelButton).setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
			case R.id.volumeOkButton:
				SeekBar volumeSlider = (SeekBar) findViewById(R.id.volumeSlider);
				jukebox.updateVolume(volumeSlider.getProgress()/100.0);
				dismiss();
				break;
			case R.id.volumeCancelButton:
				dismiss();
				break;
		}
	}

}

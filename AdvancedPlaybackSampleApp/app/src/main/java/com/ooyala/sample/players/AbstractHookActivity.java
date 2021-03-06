package com.ooyala.sample.players;

import android.app.Activity;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;

import com.ooyala.android.OoyalaNotification;
import com.ooyala.android.OoyalaPlayer;
import com.ooyala.android.OoyalaPlayerLayout;
import com.ooyala.android.configuration.Options;
import com.ooyala.android.ui.OoyalaPlayerLayoutController;
import com.ooyala.android.util.SDCardLogcatOoyalaEventsLogger;

import java.util.Observable;
import java.util.Observer;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/**
 * This class asks permission for WRITE_EXTERNAL_STORAGE. We need it for automation hooks
 * as we need to write into the SD card and automation will parse this file.
 */
public abstract class AbstractHookActivity extends Activity implements Observer {
	private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;

	protected OoyalaPlayer player;
	protected OoyalaPlayerLayout playerLayout;
	protected OoyalaPlayerLayoutController playerLayoutController;

	protected String embedCode;
	protected String pcode;
	protected String domain;
	protected String TAG = this.getClass().toString();

	protected boolean writePermission = false;
	protected boolean asked = false;

	private SDCardLogcatOoyalaEventsLogger log = new SDCardLogcatOoyalaEventsLogger();

	// complete player setup after we asked for permission to write into external storage
	abstract void completePlayerSetup(final boolean asked);

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
		} else {
			writePermission= true;
			asked = true;
		}

		embedCode = getIntent().getExtras().getString("embed_code");
		pcode = getIntent().getExtras().getString("pcode");
		domain = getIntent().getExtras().getString("domain");
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE) {
			asked = true;
			if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
				writePermission = true;
			}
			completePlayerSetup(asked);
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		if (null != player) {
			player.resume();
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		if (player != null) {
			player.suspend();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		destroyPlayer();
	}

	private void destroyPlayer() {
		if (player != null) {
			player.destroy();
			player = null;
		}
		if (playerLayout != null) {
			playerLayout.release();
		}
		if (playerLayoutController != null) {
			playerLayoutController.destroy();
			playerLayoutController = null;
		}
	}

	protected Options createPlayerOptions() {
		return new Options.Builder()
			.setUseExoPlayer(true)
			.build();
	}

	@Override
	public void update(Observable o, Object arg) {
		final String arg1 = OoyalaNotification.getNameOrUnknown(arg);
		if (arg1.equals(OoyalaPlayer.TIME_CHANGED_NOTIFICATION_NAME)) {
			if(TAG.equalsIgnoreCase("class com.ooyala.sample.players.ProgrammaticVolumePlayerActivity"))
			{
				player.setVolume(player.getVolume() + .025f);
				return;
			}
			return;
		}
		String text = "Notification Received: " + arg1 + " - state: " + player.getState();
		Log.d(TAG, text);

		if (writePermission) {
			Log.d(TAG, "Writing log to SD card");
			// Automation Hook: Write the event text along with event count to log file in sdcard if the log file exists
			log.writeToSdcardLog(text);
		}
	}
}
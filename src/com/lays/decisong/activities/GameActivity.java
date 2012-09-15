package com.lays.decisong.activities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.lays.decisong.DecisongApplication;
import com.lays.decisong.R;
import com.lays.decisong.adapters.TracksAdapter;
import com.lays.decisong.models.Album;
import com.lays.decisong.models.Track;
import com.rdio.android.api.Rdio;
import com.rdio.android.api.RdioApiCallback;
import com.rdio.android.api.RdioListener;
import com.rdio.android.api.RdioSubscriptionType;
import com.rdio.android.api.services.RdioAuthorisationException;

public class GameActivity extends ListActivity implements RdioListener {

    /** Activity tag */
    private static final String TAG = GameActivity.class.getSimpleName();

    /** Request code used for startActivityForResult/onActivityResult */
    private static final int REQUEST_AUTHORISE_APP = 100;

    /** Dialog codes */
    private static final int DIALOG_GETTING_USER = 100;
    private static final int DIALOG_GETTING_COLLECTION = 101;
    private static final int DIALOG_GETTING_HEAVY_ROTATION = 102;

    /** Rdio variables */
    private MediaPlayer mMediaPlayer;
    private Queue<Track> mTrackQueue;
    private static Rdio mRdio;
    private static String collectionKey = null;
    
    /** Game variables */
    private ArrayList<String> mAllAlbumKeys;
    private HashMap<String, Album> mAllAlbums;
    private ArrayList<String> mUnchosenAlbums;
    private ArrayList<Track> mChosenTracks;
    private TracksAdapter mAdapter;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_game);
	mTrackQueue = new LinkedList<Track>();
	if (mRdio == null) {
	    mRdio = new Rdio(DecisongApplication.RDIO_API_KEY, DecisongApplication.RDIO_SECRET_KEY, null, null, this, this);
	}
	
	mAllAlbums = new HashMap<String, Album>();
	mChosenTracks = new ArrayList<Track>();
	mAdapter = new TracksAdapter(this, mChosenTracks);
	setListAdapter(mAdapter);
    }

    @Override
    public void onDestroy() {
	// Make sure to call the cleanup method on the API object
	mRdio.cleanup();
	// If we allocated a player, then cleanup after it
	if (mMediaPlayer != null) {
	    mMediaPlayer.reset();
	    mMediaPlayer.release();
	    mMediaPlayer = null;
	}
	super.onDestroy();
    }

    /*
     * Dispatched by the Rdio object once the setTokenAndSecret call has
     * finished, and the credentials are ready to be used to make API calls. The
     * token & token secret are passed in so that you can save/cache them for
     * future re-use.
     * 
     * @see com.rdio.android.api.RdioListener#onRdioAuthorised(java.lang.String,
     * java.lang.String)
     */
    @Override
    public void onRdioAuthorised(String accessToken, String accessTokenSecret) {
	// TODO save accessToken and Secret in user preferences
	playMusic();
    }

    /*
     * Dispatched by the Rdio object when the Rdio object is done initialising,
     * and a connection to the Rdio app service has been established. If
     * authorised is true, then we reused our existing OAuth credentials, and
     * the API is ready for use.
     * 
     * @see com.rdio.android.api.RdioListener#onRdioReady()
     */
    @Override
    public void onRdioReady() {
	Log.i(TAG, "User Subscription State: " + mRdio.getSubscriptionState() + " Fullstream enabled: " + mRdio.canUserPlayFullStreams());
	playMusic();
    }

    /*
     * Dispatched by the Rdio object when app approval is needed. Take the
     * authorisation intent given and invoke the activity for it
     * 
     * @see
     * com.rdio.android.api.RdioListener#onRdioUserAppApprovalNeeded(android
     * .content.Intent)
     */
    @Override
    public void onRdioUserAppApprovalNeeded(Intent authorisationIntent) {
	try {
	    startActivityForResult(authorisationIntent, REQUEST_AUTHORISE_APP);
	} catch (ActivityNotFoundException e) {
	    // Rdio app not found
	    Log.e(TAG, "Rdio app not found, limited to 30s samples.");
	}
    }

    @Override
    public void onRdioUserPlayingElsewhere() {
	Log.w(TAG, "Tell the user the playback is stopping.");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
	switch (requestCode) {
	case REQUEST_AUTHORISE_APP:
	    if (resultCode == Rdio.RESULT_AUTHORISATION_ACCEPTED) {
		Log.i(TAG, "User authorised our app.");
		mRdio.setTokenAndSecret(data);
	    } else if (resultCode == Rdio.RESULT_AUTHORISATION_REJECTED) {
		Log.i(TAG, "User rejected our app.");
	    }
	    break;
	default:
	    break;
	}
    }

    @Override
    protected Dialog onCreateDialog(int id) {
	switch (id) {
	case DIALOG_GETTING_USER:
	    return ProgressDialog.show(this, "", getResources().getString(R.string.getting_user));
	case DIALOG_GETTING_COLLECTION:
	    return ProgressDialog.show(this, "", getResources().getString(R.string.getting_collection));
	case DIALOG_GETTING_HEAVY_ROTATION:
	    return ProgressDialog.show(this, "", getResources().getString(R.string.getting_heavy_rotation));
	}
	return null;
    }

    /**
     * Get the current user, and load their collection to start playback with.
     * Requires authorisation and the Rdio app to be installed.
     */
    private void playMusic() {
	if (mRdio.getSubscriptionState() == RdioSubscriptionType.ANONYMOUS) {
	    playMusicWithoutApp();
	    return;
	}

	showDialog(DIALOG_GETTING_USER);

	// Get the current user so we can find out their user ID and get their
	// collection key
	List<NameValuePair> args = new LinkedList<NameValuePair>();
	args.add(new BasicNameValuePair("extras", "followingCount,followerCount,username,displayName,subscriptionType,trialEndDate,actualSubscriptionType"));
	mRdio.apiCall("currentUser", args, new RdioApiCallback() {
	    @Override
	    public void onApiSuccess(JSONObject result) {
		dismissDialog(DIALOG_GETTING_USER);
		try {
		    result = result.getJSONObject("result");
		    Log.i(TAG, result.toString(2));

		    // c<userid> is the 'collection radio source' key
		    collectionKey = result.getString("key").replace('s', 'c');

		    loadMoreTracks();
		} catch (Exception e) {
		    Log.e(TAG, "Failed to handle JSONObject: ", e);
		}
	    }

	    @Override
	    public void onApiFailure(String methodName, Exception e) {
		dismissDialog(DIALOG_GETTING_USER);
		Log.e(TAG, "getCurrentUser failed. ", e);
		if (e instanceof RdioAuthorisationException) {
		    playMusicWithoutApp();
		}
	    }
	});
    }

    /**
     * Get Rdio's site-wide heavy rotation and play 30s samples. Doesn't require
     * auth or the Rdio app to be installed
     */
    private void playMusicWithoutApp() {

	showDialog(DIALOG_GETTING_HEAVY_ROTATION);

	List<NameValuePair> args = new LinkedList<NameValuePair>();
	args.add(new BasicNameValuePair("type", "albums"));
	mRdio.apiCall("getHeavyRotation", args, getRotationAlbumsCallback);
    }

    private RdioApiCallback getRotationAlbumsCallback = new RdioApiCallback() {
	@Override
	public void onApiSuccess(JSONObject result) {
	    try {
		Log.i(TAG, "Heavy rotation: " + result.toString(2));
		JSONArray albums = result.getJSONArray("result");
		mAllAlbumKeys = new ArrayList<String>(albums.length());
		for (int i = 0; i < albums.length(); i++) {
		    JSONObject album = albums.getJSONObject(i);
		    String albumKey = album.getString("key");
		    mAllAlbumKeys.add(albumKey);
		}

		// Build our argument to pass to the get api
		StringBuffer keyBuffer = new StringBuffer();
		Iterator<String> iter = mAllAlbumKeys.iterator();
		while (iter.hasNext()) {
		    keyBuffer.append(iter.next());
		    if (iter.hasNext()) {
			keyBuffer.append(",");
		    }
		}
		Log.i(TAG, "Album keys to fetch: " + keyBuffer.toString());

		// Get more details (like tracks) for all the albums we parsed
		// out of the heavy rotation
		List<NameValuePair> getArgs = new LinkedList<NameValuePair>();
		getArgs.add(new BasicNameValuePair("keys", keyBuffer.toString()));
		getArgs.add(new BasicNameValuePair("extras", "tracks"));
		mRdio.apiCall("get", getArgs, getAllTracksCallback);
	    } catch (Exception e) {
		Log.e(TAG, "Failed to handle JSONObject: ", e);
	    } finally {
		dismissDialog(DIALOG_GETTING_HEAVY_ROTATION);
	    }
	}

	@Override
	public void onApiFailure(String methodName, Exception e) {
	    dismissDialog(DIALOG_GETTING_HEAVY_ROTATION);
	    Log.e(TAG, "getRotationAlbums failed. ", e);
	    e.printStackTrace();
	}
    };

    private RdioApiCallback getAllTracksCallback = new RdioApiCallback() {

	@Override
	public void onApiSuccess(JSONObject result) {
	    try {
		Log.i(TAG, "Tracks result: " + result.toString(2));
		result = result.getJSONObject("result"); // clever?
		List<Track> trackKeys = new LinkedList<Track>();
		
		// Build our list of tracks to put into the player queue
		for (String albumKey : mAllAlbumKeys) {
		    if (!result.has(albumKey)) {
			Log.w(TAG, "result didn't contain album key: " + albumKey);
			continue;
		    }
		    JSONObject jAlbum = result.getJSONObject(albumKey);
		    JSONArray tracks = jAlbum.getJSONArray("tracks");
		    Log.i(TAG, "Album " + albumKey + " has " + tracks.length() + " tracks");
		    Album album = new Album(albumKey, tracks.length());
		    
		    for (int i = 0; i < tracks.length(); i++) {
			JSONObject trackObject = tracks.getJSONObject(i);
			String key = trackObject.getString("key");
			String name = trackObject.getString("name");
			String artist = trackObject.getString("artist");
			String albumName = trackObject.getString("album");
			String albumArt = trackObject.getString("icon");
			Log.i(TAG, "Found track: " + key + " => " + trackObject.getString("name"));
			Track t = new Track(key, name, artist, albumName, albumArt, albumKey);
			trackKeys.add(t);
			
			album.trackKeys[i] = key;
			album.tracks.put(key, t);
		    }
		    mAllAlbums.put(albumKey, album);
		}

		if (trackKeys.size() > 1) {
		    Collections.shuffle(trackKeys);
		    mTrackQueue.addAll(trackKeys);
		}
		// dismissDialog(DIALOG_GETTING_HEAVY_ROTATION);

		// If we're not playing something, then load something up
		if (mMediaPlayer == null || !mMediaPlayer.isPlaying()) {
		    next(true);
		}
	    } catch (Exception e) {
		Log.e(TAG, "Failed to handle JSONObject: ", e);
	    }
	}

	@Override
	public void onApiFailure(String methodName, Exception e) {
	    Log.e(TAG, "getAllTracks failed. ", e);
	    e.printStackTrace();
	}
    };

    private void next(final boolean manualPlay) {
	if (mMediaPlayer != null) {
	    mMediaPlayer.stop();
	    mMediaPlayer.release();
	    mMediaPlayer = null;
	}

	final Track track = mTrackQueue.poll();
	if (mTrackQueue.size() < 3) {
	    Log.i(TAG, "Track queue depleted, loading more tracks");
	    loadMoreTracks();
	}

	if (track == null) {
	    Log.e(TAG, "Track is null!  Size of queue: " + mTrackQueue.size());
	    return;
	}

	// Load the next track in the background and prep the player (to start buffering)
	// Do in a background thread so it doesn't block the main thread in prepare()
	AsyncTask<Track, Void, Track> loadTrackAsyncTask = new AsyncTask<Track, Void, Track>() {
	    @Override
	    protected Track doInBackground(Track... params) {
		Track track = params[0];
		try {
		    mMediaPlayer = mRdio.getPlayerForTrack(track.key, null, manualPlay);
		    mMediaPlayer.prepare();
		    mMediaPlayer.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
			    next(false);
			}
		    });
		    mMediaPlayer.start();
		} catch (Exception e) {
		    Log.e("Test", "Exception " + e);
		}
		return track;
	    }

	    @Override
	    protected void onPostExecute(Track track) {
		resetUnchosenAlbums(); // reset our previously mUnchosenAlbums
		mChosenTracks.clear(); // clear our previously chosen tracks
		mChosenTracks.add(track); // add track to arraylist of tracks
		mUnchosenAlbums.remove(track.albumKey); // remove the chosen track's album's key first
		addRandomTracks(new Random()); // add our random tracks recursively
		Collections.shuffle(mChosenTracks); // shuffle our mutliple choices
		mAdapter.notifyDataSetChanged(); // notify adapter data set changed
	    }
	};
	loadTrackAsyncTask.execute(track);

	Toast.makeText(this, String.format(getResources().getString(R.string.now_playing), track.trackName, track.albumName, track.artistName), Toast.LENGTH_LONG).show();
    }

    private void resetUnchosenAlbums() {
	mUnchosenAlbums = new ArrayList<String>(mAllAlbumKeys.size());
	for (String key : mAllAlbumKeys) {
	    mUnchosenAlbums.add(key);
	}
    }
    
    private void addRandomTracks(Random r) {
	if (mChosenTracks.size() > 2) {
	    return;
	} else {
	    // find an unchosen random album
	    String key = mUnchosenAlbums.remove(r.nextInt(mUnchosenAlbums.size()));
	    // find a random track on those albums
	    Track randomTrack = mAllAlbums.get(key).getRandomTrack();
	    // add it to Activity's arraylist
	    mChosenTracks.add(randomTrack);
	    addRandomTracks(r);
	}
    }
    
    private void loadMoreTracks() {
	if (mRdio.getSubscriptionState() == RdioSubscriptionType.ANONYMOUS) {
	    Log.i(TAG, "Anonymous user! No more tracks to play.");

	    // Notify the user we're out of tracks
	    Toast.makeText(this, getString(R.string.no_more_tracks), Toast.LENGTH_LONG).show();

	    // Then helpfully point them to the market to go install Rdio ;)
	    Intent installRdioIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:com.rdio.android.ui"));
	    installRdioIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    startActivity(installRdioIntent);

	    finish();
	    return;
	}

	showDialog(DIALOG_GETTING_COLLECTION);
	List<NameValuePair> args = new LinkedList<NameValuePair>();
	args.add(new BasicNameValuePair("keys", collectionKey));
	args.add(new BasicNameValuePair("count", "50"));
	mRdio.apiCall("get", args, new RdioApiCallback() {

	    @Override
	    public void onApiSuccess(JSONObject result) {
		try {
		    result = result.getJSONObject("result");
		    result = result.getJSONObject(collectionKey);
		    List<Track> trackKeys = new LinkedList<Track>();
		    JSONArray tracks = result.getJSONArray("tracks");
		    for (int i = 0; i < tracks.length(); i++) {
			JSONObject trackObject = tracks.getJSONObject(i);
			String key = trackObject.getString("key");
			String name = trackObject.getString("name");
			String artist = trackObject.getString("artist");
			String album = trackObject.getString("album");
			String albumArt = trackObject.getString("icon");
			Log.d(TAG, "Found track: " + key + " => " + trackObject.getString("name"));
			trackKeys.add(new Track(key, name, artist, album, albumArt, null)); // TODO potential danger
		    }
		    if (trackKeys.size() > 1)
			mTrackQueue.addAll(trackKeys);
		    dismissDialog(DIALOG_GETTING_COLLECTION);

		    // If we're not playing something, then load something up
		    if (mMediaPlayer == null || !mMediaPlayer.isPlaying()) {
			next(true);
		    }
		} catch (Exception e) {
		    dismissDialog(DIALOG_GETTING_COLLECTION);
		    Log.e(TAG, "Failed to handle JSONObject: ", e);
		}
	    }

	    @Override
	    public void onApiFailure(String methodName, Exception e) {
		dismissDialog(DIALOG_GETTING_COLLECTION);
		Log.e(TAG, methodName + " failed: ", e);
	    }
	});
    }
}

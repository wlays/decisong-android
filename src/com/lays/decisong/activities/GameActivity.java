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

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.lays.decisong.DecisongApplication;
import com.lays.decisong.R;
import com.lays.decisong.adapters.TracksAdapter;
import com.lays.decisong.models.Album;
import com.lays.decisong.models.Player;
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

    /** ProgressDialogs */
    private static ProgressDialog mGettingCollectionDialog;
    private static ProgressDialog mGettingUserDialog;
    private static ProgressDialog mGettingRotationDialog;

    /** Rdio variables */
    private MediaPlayer mMediaPlayer;
    private Queue<Track> mTrackQueue;
    private static Rdio mRdio;
    private static String collectionKey = null;

    /** Quiz variables */
    private ListView mListView;
    private ArrayList<String> mAllAlbumKeys;
    private HashMap<String, Album> mAllAlbums;
    private ArrayList<String> mUnchosenAlbums;
    private ArrayList<Track> mChosenTracks;
    private TracksAdapter mAdapter;

    /** Players variables */
    private static final int INITIAL_ROUND = 1;
    private static final int INITIAL_PLAYER = 0;
    private static final int LAST_ROUND = 3;
    private TextView mCurrentRoundView;
    private int mCurrentRound;
    private TextView mCurrentPlayerView;
    private int mCurrentPlayer;
    private ArrayList<Player> mPlayers;

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_game);

	// init rdio variables
	mTrackQueue = new LinkedList<Track>();
	if (mRdio == null) {
	    mRdio = new Rdio(DecisongApplication.RDIO_API_KEY,
		    DecisongApplication.RDIO_SECRET_KEY, null, null, this, this);
	}

	// init quiz variables
	mListView = getListView();
	mAllAlbums = new HashMap<String, Album>();
	mChosenTracks = new ArrayList<Track>();
	mAdapter = new TracksAdapter(this, mChosenTracks);
	setListAdapter(mAdapter);

	// init player variables
	mCurrentRoundView = (TextView) findViewById(R.id.current_round);
	mCurrentRound = INITIAL_ROUND;
	mCurrentPlayerView = (TextView) findViewById(R.id.current_player);
	mCurrentPlayer = INITIAL_PLAYER;
	mPlayers = new ArrayList<Player>();
	String[] players = getIntent().getStringArrayExtra(DecisongApplication.PLAYERS_KEY);
	for (String p : players) {
	    mPlayers.add(Player.create(p));
	}
	
	// setup game conditions
	Collections.shuffle(mPlayers);
	mCurrentPlayerView.setText(mPlayers.get(mCurrentPlayer).name);
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
	Log.i(TAG, "User Subscription State: " + mRdio.getSubscriptionState()
		+ " Fullstream enabled: " + mRdio.canUserPlayFullStreams());
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

    /**
     * Get the current user, and load their collection to start playback with.
     * Requires authorisation and the Rdio app to be installed.
     */
    private void playMusic() {
	if (mRdio.getSubscriptionState() == RdioSubscriptionType.ANONYMOUS) {
	    playMusicWithoutApp();
	    return;
	}

	mGettingUserDialog = ProgressDialog.show(this, "",
		getString(R.string.getting_user), true);
	mGettingUserDialog.show();

	// Get the current user so we can find out their user ID and get their
	// collection key
	List<NameValuePair> args = new LinkedList<NameValuePair>();
	args.add(new BasicNameValuePair(
		"extras",
		"followingCount,followerCount,username,displayName,subscriptionType,trialEndDate,actualSubscriptionType"));
	mRdio.apiCall("currentUser", args, new RdioApiCallback() {
	    @Override
	    public void onApiSuccess(JSONObject result) {
		mGettingUserDialog.dismiss();

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
		mGettingUserDialog.dismiss();
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

	mGettingRotationDialog = ProgressDialog.show(this, "",
		getString(R.string.getting_heavy_rotation), true);
	mGettingRotationDialog.show();

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
		mGettingRotationDialog.dismiss();
		Log.e(TAG, "Failed to handle JSONObject: ", e);
	    }
	}

	@Override
	public void onApiFailure(String methodName, Exception e) {
	    mGettingRotationDialog.dismiss();
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
			Log.w(TAG, "result didn't contain album key: "
				+ albumKey);
			continue;
		    }
		    JSONObject jAlbum = result.getJSONObject(albumKey);
		    JSONArray tracks = jAlbum.getJSONArray("tracks");
		    Log.i(TAG, "Album " + albumKey + " has " + tracks.length()
			    + " tracks");
		    Album album = new Album(albumKey, tracks.length());

		    for (int i = 0; i < tracks.length(); i++) {
			JSONObject trackObject = tracks.getJSONObject(i);
			String key = trackObject.getString("key");
			String name = trackObject.getString("name");
			String artist = trackObject.getString("artist");
			String albumName = trackObject.getString("album");
			String albumArt = trackObject.getString("icon");
			Log.i(TAG,
				"Found track: " + key + " => "
					+ trackObject.getString("name"));
			Track t = new Track(key, name, artist, albumName,
				albumArt, albumKey);
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

		// If we're not playing something, then load something up
		if (mMediaPlayer == null || !mMediaPlayer.isPlaying()) {
		    mGettingRotationDialog.dismiss();
		    nextTrack();
		}
	    } catch (Exception e) {
		mGettingRotationDialog.dismiss();
		Log.e(TAG, "Failed to handle JSONObject: ", e);
	    }
	}

	@Override
	public void onApiFailure(String methodName, Exception e) {
	    mGettingRotationDialog.dismiss();
	    Log.e(TAG, "getAllTracks failed. ", e);
	    e.printStackTrace();
	}
    };

    private void nextTrack() {

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

	loadTrackAsyncTask.execute(track);

	Toast.makeText(
		this,
		String.format(getResources().getString(R.string.now_playing),
			track.trackName, track.albumName, track.artistName),
		Toast.LENGTH_LONG).show();
    }

    /**
     * Load the next track in the background and prep the player (to start
     * buffering). Do in a background thread so it doesn't block the main thread
     * in prepare()
     */
    AsyncTask<Track, Void, Track> loadTrackAsyncTask = new AsyncTask<Track, Void, Track>() {

	private ProgressDialog mProgressDialog = ProgressDialog.show(
		GameActivity.this, "", "Loading", true);

	@Override
	protected void onPreExecute() {
	    mProgressDialog.show();
	}

	@Override
	protected Track doInBackground(Track... params) {
	    Track track = params[0];
	    try {
		mMediaPlayer = mRdio.getPlayerForTrack(track.key, null, true);
		mMediaPlayer.prepare();
		mMediaPlayer
			.setOnCompletionListener(new OnCompletionListener() {
			    @Override
			    public void onCompletion(MediaPlayer mp) {
				// time's up for current player, gets no points
				readyNextPlayer();
			    }
			});
	    } catch (Exception e) {
		mProgressDialog.dismiss();
		Log.e(TAG, "Exception: " + e);
	    }
	    return track;
	}

	@Override
	protected void onPostExecute(Track track) {

	    // reset our previously mUnchosenAlbums
	    resetUnchosenAlbums();
	    // clear our previously chosen tracks
	    mChosenTracks.clear();
	    // add chosen track to arraylist of tracks
	    mChosenTracks.add(track);
	    // remove the chosen track's album's key first
	    mUnchosenAlbums.remove(track.albumKey);
	    // add our random tracks recursively
	    addRandomTracks(new Random());
	    // shuffle our mutliple choices
	    Collections.shuffle(mChosenTracks);
	    // notify adapter data set changed
	    mAdapter.notifyDataSetChanged();

	    // set right answer mechanism on list view
	    final int rightAnswer = mChosenTracks.indexOf(track);
	    OnItemClickListener listener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view,
			int position, long id) {
		    if (position == rightAnswer) {
			// right guess, increment current player's score
			mMediaPlayer.pause();
			// TODO stop timer here and give remaining seconds as
			// points
			mPlayers.get(mCurrentPlayer).score++;
			readyNextPlayer();
		    } else {
			// wrong guess, gets no points
			mMediaPlayer.pause();
			readyNextPlayer();
		    }
		}
	    };
	    mListView.setOnItemClickListener(listener);
	    mProgressDialog.dismiss();

	    // show user dialog if they are ready
	    Player p = mPlayers.get(mCurrentPlayer);
	    AlertDialog.Builder builder = new AlertDialog.Builder(
		    GameActivity.this);
	    builder.setTitle("Are you ready?")
		    .setMessage("Current Player: " + p.name)
		    .setCancelable(false)
		    .setPositiveButton("Give it to me!",
			    new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,
					int id) {
				    mMediaPlayer.start();
				    // TODO timer should be reset and started
				    // here too...
				}
			    }).create().show();

	}
    };

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
	    String key = mUnchosenAlbums.remove(r.nextInt(mUnchosenAlbums
		    .size()));
	    // find a random track on those albums
	    Track randomTrack = mAllAlbums.get(key).getRandomTrack();
	    // add it to Activity's arraylist
	    mChosenTracks.add(randomTrack);
	    // add random tracks again
	    addRandomTracks(r);
	}
    }

    private void readyNextPlayer() {

	boolean playerIsLast = mCurrentPlayer == mPlayers.size() - 1;
	boolean roundIsLast = mCurrentRound == LAST_ROUND;

	if (!playerIsLast) {
	    // case 1: player is not last
	    mCurrentPlayer++;
	    mCurrentPlayerView.setText(mPlayers.get(mCurrentPlayer).name);
	    nextTrack();
	} else if (!roundIsLast) {
	    // case 2: player is last but round is not last
	    mCurrentPlayer = INITIAL_PLAYER;
	    mCurrentPlayerView.setText(mPlayers.get(mCurrentPlayer).name);
	    mCurrentRound++;
	    mCurrentRoundView.setText("Round " + mCurrentRound);
	    nextTrack();
	} else {
	    // case 3: player is last and round is last
	    endGame();
	}
    }

    private void endGame() {
	Player winner = null;
	int highestScore = -1;
	for (Player p : mPlayers) {
	    if (p.score > highestScore) {
		highestScore = p.score;
		winner = p;
	    }
	}
	// do dialog here announcing winner
	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	builder.setTitle("Congratualations!")
		.setMessage(
			"Winner is " + winner.name + " Score: " + winner.score)
		.setCancelable(false)
		.setPositiveButton("Okay",
			new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int id) {
				finish();
				// start map activity
			    }
			}).create().show();
    }

    private void loadMoreTracks() {
	if (mRdio.getSubscriptionState() == RdioSubscriptionType.ANONYMOUS) {
	    Log.i(TAG, "Anonymous user! No more tracks to play.");

	    // Notify the user we're out of tracks
	    Toast.makeText(this, getString(R.string.no_more_tracks),
		    Toast.LENGTH_LONG).show();

	    // Then helpfully point them to the market to go install Rdio ;)
	    Intent installRdioIntent = new Intent(Intent.ACTION_VIEW,
		    Uri.parse("market://search?q=pname:com.rdio.android.ui"));
	    installRdioIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    startActivity(installRdioIntent);

	    finish();
	    return;
	}

	mGettingCollectionDialog = ProgressDialog.show(this, "",
		getString(R.string.getting_collection), true);
	mGettingCollectionDialog.show();

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
			Log.d(TAG,
				"Found track: " + key + " => "
					+ trackObject.getString("name"));
			trackKeys.add(new Track(key, name, artist, album,
				albumArt, null)); // TODO potential danger
		    }
		    if (trackKeys.size() > 1)
			mTrackQueue.addAll(trackKeys);
		    mGettingCollectionDialog.dismiss();

		    // If we're not playing something, then load something up
		    if (mMediaPlayer == null || !mMediaPlayer.isPlaying()) {
			nextTrack();
		    }
		} catch (Exception e) {
		    mGettingCollectionDialog.dismiss();
		    Log.e(TAG, "Failed to handle JSONObject: ", e);
		}
	    }

	    @Override
	    public void onApiFailure(String methodName, Exception e) {
		mGettingCollectionDialog.dismiss();
		Log.e(TAG, methodName + " failed: ", e);
	    }
	});
    }
}

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
import android.content.SharedPreferences;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
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
import com.lays.decisong.helpers.TrackJsonHelper;
import com.lays.decisong.models.Album;
import com.lays.decisong.models.Player;
import com.lays.decisong.models.Track;
import com.rdio.android.api.Rdio;
import com.rdio.android.api.RdioApiCallback;
import com.rdio.android.api.RdioSubscriptionType;
import com.rdio.android.api.services.RdioAuthorisationException;

/**
 * Most complicated activity of the application because it holds game logic,
 * manages and controls game flow through different state changes. Game only
 * works when user is in the default anonymous subscription Rdio mode. That
 * means only 30 seconds clips of popular tracks are available in this mode.
 * 
 * @author wlays
 * 
 */
public class GameActivity extends ListActivity {

    /** Activity tag */
    private static final String TAG = GameActivity.class.getSimpleName();

    /** Request code for startActivityForResult/onActivityResult */
    private static final int REQUEST_AUTHORIZE_APP = 100;

    /** ProgressDialogs */
    private ProgressDialog mGettingCollectionDialog;
    private ProgressDialog mGettingRotationDialog;

    /** Rdio variables */
    private RdioApiCallback mRotationAlbumsCallback;
    private RdioApiCallback mAllTracksCallback;
    private MediaPlayer mMediaPlayer;
    private Queue<Track> mTrackQueue;
    private Rdio mRdio;
    private String collectionKey;

    /** Quiz variables */
    private ListView mListView;
    private ArrayList<String> mAllAlbumKeys;
    private HashMap<String, Album> mAllAlbums;
    private ArrayList<String> mUnchosenAlbums;
    private ArrayList<Track> mChosenTracks;
    private TracksAdapter mAdapter;
    private static final int mTimeLimit = 30;
    private int mTimerScore;
    private TextView mTimerView;
    private CountDownTimer mTimer;
    private AsyncTask<Track, Void, Track> mLoadTrackAsyncTask;

    /** Players variables */
    private static final int INITIAL_ROUND = 1;
    private static final int LAST_ROUND = 3;
    private static final int INITIAL_PLAYER_INDEX = 0;
    private TextView mCurrentRoundView;
    private int mCurrentRound;
    private TextView mCurrentPlayerView;
    private int mCurrentPlayerIndex;
    private ArrayList<Player> mPlayers;

    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_game);

	// init rdio variables
	mTrackQueue = new LinkedList<Track>();
	if (mRdio == null) {
	    mRdio = DecisongApplication.getInstance().setupGame(this);
	}

	// init quiz variables
	mListView = getListView();
	mAllAlbums = new HashMap<String, Album>();
	mChosenTracks = new ArrayList<Track>();
	mAdapter = new TracksAdapter(this, mChosenTracks);
	setListAdapter(mAdapter);
	mTimerScore = mTimeLimit;
	mTimerView = (TextView) findViewById(R.id.timer);

	// init player variables
	mCurrentRoundView = (TextView) findViewById(R.id.current_round);
	mCurrentRound = INITIAL_ROUND;
	mCurrentPlayerView = (TextView) findViewById(R.id.current_player);
	mCurrentPlayerIndex = INITIAL_PLAYER_INDEX;
	mPlayers = new ArrayList<Player>();
	if (getIntent().hasExtra(DecisongApplication.PLAYERS_KEY)) {
	    ArrayList<String> players = getIntent().getStringArrayListExtra(
		    DecisongApplication.PLAYERS_KEY);
	    for (String p : players) {
		mPlayers.add(Player.create(p));
	    }
	}

	// setup game conditions
	Collections.shuffle(mPlayers);
	mCurrentPlayerView.setText(mPlayers.get(mCurrentPlayerIndex).getName());

	// if rdio is already initialized before, start game
	SharedPreferences preferences = PreferenceManager
		.getDefaultSharedPreferences(getApplicationContext());
	if (preferences.getBoolean(DecisongApplication.RDIO_INITIALIZED_KEY,
		false)) {
	    onRdioReady();
	}
    }

    public void onDestroy() {
	cleanUpResources();
	super.onDestroy();
    }

    /**
     * Method to prepare for activity's destruction and prevent potential memory
     * leaks by freeing up held resources.
     */
    private void cleanUpResources() {
	if (mMediaPlayer != null) {
	    mMediaPlayer.reset();
	    mMediaPlayer.release();
	    mMediaPlayer = null;
	}
	DecisongApplication.getInstance().cleanUp();
    }

    /**
     * Called by the Rdio object when the Rdio object is done initializing in
     * DecisongApplication and a Rdio app service connection has been
     * established. If authorized before, we can reuse the existing OAuth
     * credentials use the Rdio API but it's of no concern now.
     */
    public void onRdioReady() {
	Log.i(TAG, "User Subscription State: " + mRdio.getSubscriptionState()
		+ " Fullstream Enabled: " + mRdio.canUserPlayFullStreams());
	playMusic();
    }

    /**
     * Called by the Rdio object when Rdio application approval is needed. Takes
     * the authorization intent given and invoke its activity.
     * 
     * @param authorizationIntent
     */
    public void onRdioUserAppApprovalNeeded(Intent authorizationIntent) {
	try {
	    startActivityForResult(authorizationIntent, REQUEST_AUTHORIZE_APP);
	} catch (ActivityNotFoundException e) {
	    Log.w(TAG, "Rdio application not found: Limited to 30s samples.");
	}
    }

    /**
     * Dispatched by the Rdio object once the setTokenAndSecret call has
     * finished, and the credentials are ready to be used to make API calls. The
     * token & token secret are passed in so that we can save/cache them for
     * future re-use.
     * 
     * @param accessToken
     * @param accessTokenSecret
     */
    public void onRdioAuthorised(String accessToken, String accessTokenSecret) {
	// TODO save accessToken and Secret in user preferences here
	playMusic();
    }

    public void onRdioUserPlayingElsewhere() {
	Log.w(TAG, "Tell the user the playback is stopping.");
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
	switch (requestCode) {
	case REQUEST_AUTHORIZE_APP:
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
     * Get the current user, and load their collections to start playback.
     * Requires authorization and the Rdio app to be installed. For now, we only
     * require 30s samples
     */
    private void playMusic() {
	if (mRdio.getSubscriptionState() == RdioSubscriptionType.ANONYMOUS) {
	    // for now, our game will always be call playMusicWithoutApp()
	    playMusicWithoutApp();
	    return;
	}

	// NOT IN USE YET
	final ProgressDialog mGettingUserDialog = ProgressDialog.show(this, "",
		getString(R.string.loading), true);
	mGettingUserDialog.show();

	// Get the current user so we can find out their user ID and get their
	// collection key
	List<NameValuePair> args = new LinkedList<NameValuePair>();
	args.add(new BasicNameValuePair(
		"extras",
		"followingCount,followerCount,username,displayName,subscriptionType,trialEndDate,actualSubscriptionType"));
	mRdio.apiCall("currentUser", args, new RdioApiCallback() {
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
     * auth or the Rdio app to be installed. This is how the game starts.
     */
    private void playMusicWithoutApp() {
	mGettingRotationDialog = ProgressDialog.show(this, "",
		getString(R.string.loading), true);
	mGettingRotationDialog.show();

	List<NameValuePair> args = new LinkedList<NameValuePair>();
	args.add(new BasicNameValuePair("type", "albums"));
	initRotationCallback();
	mRdio.apiCall("getHeavyRotation", args, mRotationAlbumsCallback);
    }

    /**
     * Method to initialize our heavy rotation albums callback.
     */
    private void initRotationCallback() {
	mRotationAlbumsCallback = new RdioApiCallback() {
	    public void onApiSuccess(JSONObject result) {
		try {
		    // Log.i(TAG, "Heavy Rotation: " + result.toString(2));
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
		    // Log.i(TAG, "Album keys to fetch: " +
		    // keyBuffer.toString());

		    // Get tracks for all the albums we got from heavy rotation
		    List<NameValuePair> getArgs = new LinkedList<NameValuePair>();
		    getArgs.add(new BasicNameValuePair("keys", keyBuffer
			    .toString()));
		    getArgs.add(new BasicNameValuePair("extras", "tracks"));
		    initAllTracksCallback();
		    mRdio.apiCall("get", getArgs, mAllTracksCallback);
		} catch (Exception e) {
		    mGettingRotationDialog.dismiss();
		    Log.e(TAG, "Failed to handle JSONObject: ", e);
		}
	    }

	    public void onApiFailure(String methodName, Exception e) {
		mGettingRotationDialog.dismiss();
		Log.e(TAG, "getRotationAlbums failed. ", e);
	    }
	};
    }

    /**
     * Method to initialize our all tracks callback
     */
    private void initAllTracksCallback() {
	mAllTracksCallback = new RdioApiCallback() {
	    public void onApiSuccess(JSONObject result) {
		try {
		    // (TAG, "All Tracks: " + result.toString(2));
		    result = result.getJSONObject("result");
		    List<Track> trackKeys = new LinkedList<Track>();

		    // Build our list of tracks to put into the player queue
		    for (String albumKey : mAllAlbumKeys) {
			if (!result.has(albumKey)) {
			    Log.w(TAG, "Result didn't contain album key: "
				    + albumKey);
			    continue;
			}
			JSONObject jAlbum = result.getJSONObject(albumKey);
			JSONArray tracks = jAlbum.getJSONArray("tracks");
			// Log.i(TAG, "Album " + albumKey + " has " +
			// tracks.length() + " tracks");
			Album album = Album.create(albumKey, tracks.length());

			for (int i = 0; i < tracks.length(); i++) {
			    JSONObject trackObject = tracks.getJSONObject(i);
			    String key = trackObject
				    .getString(TrackJsonHelper.KEY);
			    String name = trackObject
				    .getString(TrackJsonHelper.NAME);
			    String artist = trackObject
				    .getString(TrackJsonHelper.ARTIST);
			    String albumName = trackObject
				    .getString(TrackJsonHelper.ALBUM_NAME);
			    String albumArt = trackObject
				    .getString(TrackJsonHelper.ALBUM_ICON);
			    // Log.i(TAG, "Found track: " + key + " => " +
			    // trackObject.getString("name"));
			    Track t = Track.create(key, name, artist,
				    albumName, albumArt, albumKey);
			    trackKeys.add(t);

			    album.getTrackKeys()[i] = key;
			    album.getTracks().put(key, t);
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

	    public void onApiFailure(String methodName, Exception e) {
		mGettingRotationDialog.dismiss();
		Log.e(TAG, "getAllTracks failed. ", e);
	    }
	};
    }

    /**
     * Method which handles loading and playing of the next track in the song
     * queue.
     */
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
	    finish();
	    return;
	}

	initLoadTrackAsyncTask();
	mLoadTrackAsyncTask.execute(track);

	// Toast.makeText(this,
	// String.format(getResources().getString(R.string.now_playing),
	// track.trackName, track.albumName, track.artistName),
	// Toast.LENGTH_LONG).show();
    }

    /**
     * Load the next track in the background and prepare the player (to start
     * buffering). Do in a background thread so it doesn't block the main thread
     * in prepare() and show an alert dialog to let users acknowledge they're
     * ready.
     */
    private void initLoadTrackAsyncTask() {
	mLoadTrackAsyncTask = new AsyncTask<Track, Void, Track>() {

	    private ProgressDialog mProgressDialog = ProgressDialog.show(
		    GameActivity.this, "", getString(R.string.loading), true);

	    protected void onPreExecute() {
		mProgressDialog.show();
	    }

	    protected Track doInBackground(Track... params) {
		Track track = params[0];
		try {
		    mMediaPlayer = mRdio.getPlayerForTrack(track.getKey(),
			    null, true);
		    mMediaPlayer.prepare();
		    mMediaPlayer
			    .setOnCompletionListener(new OnCompletionListener() {
				public void onCompletion(MediaPlayer mp) {
				    // time's up for current player, gets no
				    // points
				    readyNextPlayer();
				}
			    });
		} catch (Exception e) {
		    mProgressDialog.dismiss();
		    Log.e(TAG, "Exception: " + e);
		}
		return track;
	    }

	    protected void onPostExecute(Track track) {

		// reset our previously mUnchosenAlbums
		resetUnchosenAlbums();
		// clear our previously chosen tracks
		mChosenTracks.clear();
		// first, add the next correct track
		mChosenTracks.add(track);
		// then, remove that correct track's album's key from list of
		// unchosen albums
		mUnchosenAlbums.remove(track.getAlbumKey());
		// add our random tracks recursively
		addRandomTracks(new Random());
		// shuffle our multiple track choices and notify list adapter
		Collections.shuffle(mChosenTracks);
		mAdapter.notifyDataSetChanged();

		// set right answer mechanism on list view
		final int rightAnswer = mChosenTracks.indexOf(track);
		OnItemClickListener listener = new OnItemClickListener() {
		    public void onItemClick(AdapterView<?> parent, View view,
			    int position, long id) {
			if (position == rightAnswer) {
			    // right guess, increment current player's score
			    mMediaPlayer.pause();
			    mTimer.cancel();
			    mPlayers.get(mCurrentPlayerIndex).setScore(
				    mPlayers.get(mCurrentPlayerIndex)
					    .getScore() + mTimerScore);
			    readyNextPlayer();
			} else {
			    // wrong guess, gets no points
			    mMediaPlayer.pause();
			    mTimer.cancel();
			    readyNextPlayer();
			}
		    }
		};
		mListView.setOnItemClickListener(listener);
		restartTimer(mTimeLimit);
		mProgressDialog.dismiss();

		// show user the are-you-ready dialog
		Player p = mPlayers.get(mCurrentPlayerIndex);
		AlertDialog.Builder builder = new AlertDialog.Builder(
			GameActivity.this);
		builder.setTitle("Are you ready?")
			.setMessage("Current Player: " + p.getName())
			.setCancelable(false)
			.setPositiveButton("OK",
				new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog,
					    int id) {
					mMediaPlayer.start();
					mTimer.start();
				    }
				}).create().show();
	    }
	};
    }

    /**
     * Method to reset unchosen albums each turn. This makes sure that the wrong
     * tracks will be chosen differently from the chosen track's album.
     */
    private void resetUnchosenAlbums() {
	mUnchosenAlbums = new ArrayList<String>(mAllAlbumKeys.size());
	for (String key : mAllAlbumKeys) {
	    mUnchosenAlbums.add(key);
	}
    }

    /**
     * Recursive function to add random tracks on the current quiz.
     * 
     * @param r
     */
    private void addRandomTracks(Random r) {
	if (mChosenTracks.size() > 3) {
	    return;
	} else {
	    // find a random unchosen album
	    String key = mUnchosenAlbums.remove(r.nextInt(mUnchosenAlbums
		    .size()));
	    // find a random track on that randomly chosen album
	    Track randomTrack = mAllAlbums.get(key).getRandomTrack();
	    // add it to multiple track choices
	    mChosenTracks.add(randomTrack);
	    // repeat
	    addRandomTracks(r);
	}
    }

    /**
     * Game logic to control flow of game and prepares the next track for the
     * next player.
     */
    private void readyNextPlayer() {

	boolean playerIsLast = mCurrentPlayerIndex == mPlayers.size() - 1;
	boolean roundIsLast = mCurrentRound == LAST_ROUND;

	if (!playerIsLast) {
	    // case 1: player is not last
	    mCurrentPlayerIndex++;
	    mCurrentPlayerView.setText(mPlayers.get(mCurrentPlayerIndex)
		    .getName());
	    nextTrack();
	} else if (!roundIsLast) {
	    // case 2: player is last but round is not last
	    mCurrentPlayerIndex = INITIAL_PLAYER_INDEX;
	    mCurrentPlayerView.setText(mPlayers.get(mCurrentPlayerIndex)
		    .getName());
	    mCurrentRound++;
	    mCurrentRoundView.setText("Round " + mCurrentRound);
	    nextTrack();
	} else {
	    // case 3: player is last and round is last
	    endGame();
	}
    }

    /**
     * Handles the end of game scenario. Announces the winner and re-directs
     * him/her to google maps where they can choose where to eat.
     */
    private void endGame() {
	// get the winner
	Player winner = null;
	int highestScore = -1;
	for (Player p : mPlayers) {
	    if (p.getScore() > highestScore) {
		highestScore = p.getScore();
		winner = p;
	    }
	}

	// alert dialog announcing winner
	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	builder.setTitle("Congratualations!")
		.setMessage(
			"Winner: " + winner.getName() + " Score: "
				+ winner.getScore()).setCancelable(false)
		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int id) {
			cleanUpResources();
			finish();
			Location loc = DecisongApplication.getCurrentLocation();
			if (loc != null) {
			    // re-directs to google maps with current location
			    // and nearby restaurants
			    Uri uri = Uri.parse("geo:" + loc.getLatitude()
				    + "," + loc.getLongitude()
				    + "?z=19&f=l&q=food&mrt=yp");
			    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			    startActivity(intent);
			}
		    }
		}).create().show();
    }

    /**
     * Restarts the timer to the input time. That way we can reset the timer to
     * the beginning or back to somewhere in the middle of the countdown if we
     * were interrupted.
     * 
     * @param time
     */
    private void restartTimer(int time) {
	mTimer = new CountDownTimer(time * 1000, 1000) {
	    public void onTick(long millisUntilFinished) {
		int secondsLeft = (int) (millisUntilFinished / 1000);
		mTimerScore = secondsLeft;
		mTimerView.setText("Time Left: " + secondsLeft);
	    }

	    public void onFinish() {
		mTimerView.setText("Time's up!");
	    }
	};
    }

    /**
     * Method to load more tracks onto the queue but we won't be using it till
     * future support.
     */
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
		getString(R.string.loading), true);
	mGettingCollectionDialog.show();

	List<NameValuePair> args = new LinkedList<NameValuePair>();
	args.add(new BasicNameValuePair("keys", collectionKey));
	args.add(new BasicNameValuePair("count", "50"));
	mRdio.apiCall("get", args, new RdioApiCallback() {

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
			// Log.d(TAG, "Found track: " + key + " => " +
			// trackObject.getString("name"));
			trackKeys.add(Track.create(key, name, artist, album,
				albumArt, null)); // TODO potential future bug
		    }
		    if (trackKeys.size() > 1) {
			mTrackQueue.addAll(trackKeys);
		    }
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

	    public void onApiFailure(String methodName, Exception e) {
		mGettingCollectionDialog.dismiss();
		Log.e(TAG, methodName + " failed: ", e);
	    }
	});
    }

    /**
     * We override the Back button to fashion pause/quit function. Here, we ask
     * the user if they want to quit or resume the game through an AlertDialog
     */
    public void onBackPressed() {
	mTimer.cancel();
	mMediaPlayer.pause();

	new AlertDialog.Builder(this)
		.setTitle("Quit Game")
		.setMessage("Do you really want to quit?")
		.setNegativeButton("Quit",
			new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog,
				    int whichButton) {
				cleanUpResources();
				finish();
				overridePendingTransition(
					R.anim.slide_right_incoming,
					R.anim.slide_right_outgoing);
			    }
			})
		.setPositiveButton("Resume",
			new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog,
				    int whichButton) {
				restartTimer(mTimerScore);
				mTimer.start();
				mMediaPlayer.start();
				return;
			    }
			}).create().show();
    }
}

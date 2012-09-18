package com.lays.decisong.models;

import java.util.HashMap;
import java.util.Random;

public class Album {

    private String key;
    private int tracksNumber;
    private String[] trackKeys;
    private HashMap<String, Track> tracks;

    public static Album create(String k, int number) {
	Album album = new Album();
	album.setKey(k);
	album.setTracksNumber(number);
	album.setTrackKeys(new String[number]);
	album.setTracks(new HashMap<String, Track>());
	return album;
    }

    public Track getRandomTrack() {
	Random r = new Random();
	String key = trackKeys[r.nextInt(tracksNumber)];
	return tracks.get(key);
    }

    public String getKey() {
	return key;
    }

    public void setKey(String key) {
	this.key = key;
    }

    public int getTracksNumber() {
	return tracksNumber;
    }

    public void setTracksNumber(int tracksNumber) {
	this.tracksNumber = tracksNumber;
    }

    public String[] getTrackKeys() {
	return trackKeys;
    }

    public void setTrackKeys(String[] trackKeys) {
	this.trackKeys = trackKeys;
    }

    public HashMap<String, Track> getTracks() {
	return tracks;
    }

    public void setTracks(HashMap<String, Track> tracks) {
	this.tracks = tracks;
    }
}

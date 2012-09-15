package com.lays.decisong.models;

import java.util.HashMap;
import java.util.Random;

public class Album {
    
    public String key;
    public int tracksNumber;
    public String[] trackKeys;
    public HashMap<String, Track> tracks;

    public Album(String k, int number) {
	key = k;
	tracksNumber = number;
	trackKeys = new String[number];
	tracks = new HashMap<String, Track>();
    }
    
    public Track getRandomTrack() {
	Random r = new Random();
	String key = trackKeys[r.nextInt(tracksNumber)];
	return tracks.get(key);
    }
}

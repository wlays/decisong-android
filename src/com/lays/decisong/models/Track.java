package com.lays.decisong.models;

//Our model for the metadata for a track that we care about
public class Track {

    public String key;
    public String trackName;
    public String artistName;
    public String albumName;
    public String albumArt;
    public String albumKey;

    public Track(String k, String name, String artist, String album, String uri, String aKey) {
	key = k;
	trackName = name;
	artistName = artist;
	albumName = album;
	albumArt = uri;
	albumKey = aKey;
    }
}
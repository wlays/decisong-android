package com.lays.decisong.models;

public class Track {

    private String key;
    private String trackName;
    private String artistName;
    private String albumName;
    private String albumArt;
    private String albumKey;

    public static Track create(String k, String name, String artist,
	    String album, String uri, String aKey) {
	Track track = new Track();
	track.setKey(k);
	track.setTrackName(name);
	track.setArtistName(artist);
	track.setAlbumName(album);
	track.setAlbumArt(uri);
	track.setAlbumKey(aKey);
	return track;
    }

    public String getKey() {
	return key;
    }

    public void setKey(String key) {
	this.key = key;
    }

    public String getTrackName() {
	return trackName;
    }

    public void setTrackName(String trackName) {
	this.trackName = trackName;
    }

    public String getArtistName() {
	return artistName;
    }

    public void setArtistName(String artistName) {
	this.artistName = artistName;
    }

    public String getAlbumName() {
	return albumName;
    }

    public void setAlbumName(String albumName) {
	this.albumName = albumName;
    }

    public String getAlbumArt() {
	return albumArt;
    }

    public void setAlbumArt(String albumArt) {
	this.albumArt = albumArt;
    }

    public String getAlbumKey() {
	return albumKey;
    }

    public void setAlbumKey(String albumKey) {
	this.albumKey = albumKey;
    }
}
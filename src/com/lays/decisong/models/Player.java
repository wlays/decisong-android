package com.lays.decisong.models;

/**
 * Our player model class
 * 
 * @author wlays
 * 
 */
public class Player {

    public String name;
    public int score;

    /**
     * Factory method for creating players
     * @param name
     * @return
     */
    public static Player create(String name) {
	Player player = new Player();
	player.name = name;
	player.score = 0;
	return player;
    }
}

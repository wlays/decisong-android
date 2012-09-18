package com.lays.decisong.models;

public class Player {

    private String name;
    private int score;

    public static Player create(String name) {
	Player player = new Player();
	player.setName(name);
	player.setScore(0);
	return player;
    }

    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name;
    }

    public int getScore() {
	return score;
    }

    public void setScore(int score) {
	this.score = score;
    }
}

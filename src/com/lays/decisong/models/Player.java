package com.lays.decisong.models;

import java.util.List;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;

/**
 * Our player model class handles both database saves & queries through ORM
 * 
 * @author wlays
 * 
 */
@Table(name = "Players")
public class Player extends Model {

    @Column(name = "Name")
    public String name;

    /**
     * Factory method for saving players to database.
     * @param name
     * @return
     */
    public static Player create(String name) {
	Player player = new Player();
	player.name = name;
	player.save();
	return player;
    }

    /**
     * Database query for all players in game
     * @return
     */
    public static List<Player> getAll() {
	return new Select().from(Player.class).orderBy("Name ASC").execute();
    }

    // Alternative syntax: Player player = Player.load(Player.class, 1);
    public static Player getPlayer(long id) {
	return new Select().from(Player.class).where("Id = ?", id).executeSingle();
    }
    
    public static void deleteAll() {
	new Delete().from(Player.class).execute();
    }
}

package kr.ac.kumoh.ce.s20120420.JustRun;

import java.io.Serializable;

/**
 * Created by woong on 2017-06-06.
 */
public class Data_ingame implements Serializable {
    int thief_item_num;
    int police_item_num;
    int zone_num;
    int range;
    int room_num;
    int game_time;
    boolean my_role;
    boolean room_master;


    public boolean isMy_role() {
        return my_role;
    }

    public boolean isRoom_master() {
        return room_master;
    }

    public int getThief_item_num() {
        return thief_item_num;
    }

    public int getPolice_item_num() {
        return police_item_num;
    }

    public int getZone_num() {
        return zone_num;
    }

    public int getRange() {
        return range;
    }

    public int getRoom_num() {
        return room_num;
    }

    public int getGame_time() {
        return game_time;
    }

    public Data_ingame(int thief_item_num, int police_item_num, int zone_num, int range, int room_num,int game_time,boolean my_role,boolean room_master) {
        this.thief_item_num = thief_item_num;
        this.police_item_num = police_item_num;
        this.zone_num = zone_num;
        this.range = range;
        this.room_num=room_num;
        this.game_time=game_time;
        this.my_role=my_role;
        this.room_master=room_master;
    }
}

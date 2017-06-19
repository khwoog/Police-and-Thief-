package kr.ac.kumoh.ce.s20120420.JustRun;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by woong on 2017-06-06.
 */
public class Data_Thief implements Serializable {

    int partner_num;
    int room_num;
    int item_num;
    int zone_num;
    int range;
    int my_index;
    int game_time;
    location start;
    location mine;

    List<location> partner ;
    List<location> item = new ArrayList<location>();
    List<location> zone = new ArrayList<location>();
    List<String> partner_nic;

    public Data_Thief(int range, int zone_num, int item_num, int partner_num, int room_num,double la,double lo,double la2,double lo2,int index,int game_time) {

        this.range = range;
        this.zone_num = zone_num;
        this.item_num = item_num;
        this.room_num = room_num;
        this.partner_num = partner_num;
        this.my_index=index;
        partner= new ArrayList<location>();
        item= new ArrayList<location>();
        zone= new ArrayList<location>();
        partner_nic= new ArrayList<String>();
        start=new location(la,lo);
        mine=new location(la2,lo2);
        this.game_time=game_time;
    }

    public int getPartner_num() {
        return partner_num;
    }

    public List<location> getZone() {
        return zone;
    }

    public List<String> getPartner_nic() {
        return partner_nic;
    }
    public location getMine(){
        return mine;
    }
    public List<location> getItem() {
        return item;
    }

    public List<location> getPartner() {
        return partner;
    }

    public location getStart() {
        return start;
    }

    public int getRange() {
        return range;
    }

    public int getZone_num() {
        return zone_num;
    }

    public int getItem_num() {
        return item_num;
    }

    public int getRoom_num() {
        return room_num;
    }

    public void setPartner(double la,double lo) {
        this.partner.add(new location(la,lo));
    }

    public void setItem(double la,double lo) {
        this.item.add(new location(la,lo));
    }

    public void setZone(double la,double lo) {
        this.zone.add(new location(la,lo));
    }

    public void setPartner_nic(String nic) {
        this.partner_nic.add(nic);
    }


    public int getGame_time() {
        return game_time;
    }

    public int getMy_index() {
        return my_index;
    }
}

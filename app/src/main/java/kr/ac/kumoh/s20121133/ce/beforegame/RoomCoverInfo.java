package kr.ac.kumoh.s20121133.ce.beforegame;

/**
 * Created by jang on 2017-06-05.
 */

public class RoomCoverInfo {
    String num;
    String title;
    String total;
    String password;

    public RoomCoverInfo(String num, String title, String total, String password) {
        this.num = num;
        this.title = title;
        this.total = total;
        this.password = password;
    }

    public String getNum() {
        return num;
    }

    public String getTitle() {
        return title;
    }

    public String getTotal() {
        return total;
    }

    public String getPassword() {
        return password;
    }
}
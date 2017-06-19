package kr.ac.kumoh.ce.s20120420.JustRun;

/**
 * Created by woong on 2017-06-07.
 */

/**
 * Created by NYG on 2017-06-02.
 */

public class RoomCoverInfo {
    String num;
    String title;
    String total;
    String password;
    String playinfo;

    public RoomCoverInfo(String num, String title, String total, String password, String playinfo) {
        this.num = num;
        this.title = title;
        this.total = total;
        this.password = password;
        this.playinfo = playinfo;
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

    public String getPlayinfo() {
        return playinfo;
    }
}

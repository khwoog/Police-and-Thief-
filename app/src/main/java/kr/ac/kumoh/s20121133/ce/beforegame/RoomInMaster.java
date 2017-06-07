package kr.ac.kumoh.s20121133.ce.beforegame;

/**
 * Created by jang on 2017-06-05.
 */

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RoomInMaster extends AppCompatActivity {

    TextView mRoomInTitle;
    TextView mRoomInNum;
    private Emitter.Listener roomMaster = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i("된다된다", "된다");
                    JSONObject allData = (JSONObject)args[0];
                    JSONObject data;
                    try{
                        data = allData.getJSONObject("room");
                        String title = data.getString("title");
                        String num = data.getString("num");
                        Log.i("title : ",  title);
                        Log.i("num : ",  num);
                        mRoomInNum.setText(num);
                        mRoomInTitle.setText(title);
                    }catch (JSONException e){
                        Toast.makeText(getApplicationContext(),
                                "Error" + e.toString(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.room_in);
        Intent intent = getIntent();

        mRoomInNum = (TextView)findViewById(R.id.roomInNum);
        mRoomInTitle = (TextView)findViewById(R.id.roomInTitle);

        String title = intent.getExtras().getString("createTitle");
        String password = intent.getExtras().getString("createPwd");
        String radius = intent.getExtras().getString("createRadius");

        RoomList.Socket.emit("room_Master", title, password, radius);
        RoomList.Socket.on("room_Master", roomMaster);
    }
}

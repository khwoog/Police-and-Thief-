package kr.ac.kumoh.s20121133.ce.beforegame;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.engineio.client.Socket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by jang on 2017-05-31.
 */

public class RoomIn extends AppCompatActivity {

    String mCreate; // 방을 생성한 것인지, 들어온 것인지 구별
    String mNum;    // 들어온 방번호
    String mTitle;  // 들어온 방제목

    TextView mRoomInNum;
    TextView mRoomInTitle;
    TextView[] mUserTv = new TextView [12];    // 12명의 텍스트뷰 배열

    Button mPoliceBtn;
    Button mThiefBtn;

    int[] mUsers;  // xml에 있는 12명의 int형 id 저장

    InUserInfo[] mInUser = new InUserInfo[12];  // 12명의 (닉네임, 역할) 클래스 배열

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.room_in);
        Intent intent = getIntent();

        //********** 각 유저 텍스트뷰의 아이디를 저장 **********
        mUsers = new int[]{R.id.user1, R.id.user2, R.id.user3, R.id.user4,
                R.id.user5, R.id.user6, R.id.user7, R.id.user8,
                R.id.user9, R.id.user10, R.id.user11, R.id.user12};

        //********** 각 텍스트뷰 가져오기 **********
        for(int i = 0; i<12; i++){
            mUserTv[i] = (TextView)findViewById(mUsers[i]);
        }

        //********** 유저 클래스 초기화 **********
        for(int i = 0; i<12; i++){
            mInUser[i] = new InUserInfo("user"+(i+1),"");
        }

        mRoomInNum = (TextView)findViewById(R.id.roomInNum);
        mRoomInTitle = (TextView)findViewById(R.id.roomInTitle);

        mCreate = intent.getExtras().getString("create");
        Log.i("들어온 값은 ?", mCreate);
        if (mCreate.equals("yes")) {
            String title = intent.getExtras().getString("createTitle");
            String password = intent.getExtras().getString("createPwd");
            String radius = intent.getExtras().getString("createRadius");
            Log.i("여기로 들어옴", "ㄴㅇㄹ");
            RoomList.mSocket.emit("room_Master", title, password, radius);
            RoomList.mSocket.on("room_Master", roomMaster);

            TextView user = (TextView)findViewById(R.id.user1);
            user.setText(RoomList.mInNicName);
        }// ********** 방 생성하는 경우 **********

        else if(mCreate.equals("no")){
            String num = intent.getExtras().getString("num");
            String title = intent.getExtras().getString("title");
            Log.i("들어온 값은 : ", num);
            Log.i("들어온 값은 : ", title);

            mRoomInNum.setText(num);
            mRoomInTitle.setText(title);
            RoomList.mSocket.emit("notifyAll",num);
        }// ********** 방에 들어온 경우 **********

        RoomList.mSocket.on("notifyAll", notifyAll);

        mPoliceBtn = (Button)findViewById(R.id.polic);
        mPoliceBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                RoomList.mSocket.emit("roleChange", mRoomInNum.getText(), "police");
            }
        });// ********** 경찰하기 **********

        mThiefBtn = (Button)findViewById(R.id.thief);
        mThiefBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RoomList.mSocket.emit("roleChange", mRoomInNum.getText(), "thief");
            }
        });// ********** 도둑하기 **********
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Question");
        dialogBuilder.setMessage("이 방에서 나가시겠습니까?");
        dialogBuilder.setPositiveButton("Yes", yesBtnClickListener);
        dialogBuilder.setNegativeButton("No", noBtnClickListener);
        dialogBuilder.show();
    }
    private DialogInterface.OnClickListener yesBtnClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            Log.i("sdsdfsdf", mRoomInNum.getText().toString());
            RoomList.mSocket.emit("roomPeopleOut", mRoomInNum.getText().toString());
            finish(); // *************** 방안에 있는 사용자가 나가는 경우 서버에게 알린다. ***************
        }
    };
    private DialogInterface.OnClickListener noBtnClickListener = new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.cancel();
        }
    };

    class InUserInfo{
        String nicName;
        String role;
        public InUserInfo(String nicName, String role) {
            this.nicName = nicName;
            this.role = role;
        }

        public String getNicName() {
            return nicName;
        }

        public String getRole() {
            return role;
        }

        public void setNicName(String nicName) {
            this.nicName = nicName;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }

    private Emitter.Listener roomMaster = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject allData = (JSONObject)args[0];
                    JSONObject data;
                    try{
                        data = allData.getJSONObject("room");
                        mNum = data.getString("num");
                        mTitle = data.getString("title");
                        Log.i("title : ",  mTitle);
                        Log.i("num : ",  mNum);
                        mRoomInNum.setText(mNum);
                        mRoomInTitle.setText(mTitle);
                    }catch (JSONException e){
                        Toast.makeText(getApplicationContext(),
                                "Error" + e.toString(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    };
    private Emitter.Listener notifyAll = new Emitter.Listener() {

        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try{
                        JSONObject data = (JSONObject)args[0];
                        JSONArray jsonMainNode = data.getJSONArray("inUser");
                        for(int i = 0; i<12; i++){      // 유저 정보 초기화
                            mInUser[i].setNicName("user"+(i+1));
                            mInUser[i].setRole("");
                            mUserTv[i].setText(mInUser[i].getNicName());
                            mUserTv[i].setBackgroundResource(R.drawable.edge_default);
                        }
                        for(int i = 1; i < jsonMainNode.length(); i++) {
                            JSONObject jsonChildNode = jsonMainNode.getJSONObject(i);
                            String nicName = jsonChildNode.getString("nicName");
                            Log.i("들어온자 ", nicName);
                            String role = jsonChildNode.getString("role");
                            Log.i("들어온자의 역할 ", role);
                            mInUser[i-1].setNicName(nicName);
                            mInUser[i-1].setRole(role);
                        }
                        int j = 0;
                        for(int i = 1; i < jsonMainNode.length(); i++){
                            if(mInUser[i-1].getRole().equals("police")){
                                mUserTv[j].setText(mInUser[i-1].getNicName());
                                mUserTv[j].setBackgroundResource(R.drawable.edge_police);
                                j++;
                            }
                        }
                        for(int i = 1; i < jsonMainNode.length(); i++){
                            if(mInUser[i-1].getRole().equals("thief")){
                                mUserTv[j].setText(mInUser[i-1].getNicName());
                                mUserTv[j].setBackgroundResource(R.drawable.edge_thief);
                                j++;
                            }
                        }
                    }catch (JSONException | NullPointerException e){
                        Toast.makeText(getApplicationContext(),
                                "Error" + e.toString(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    };
}
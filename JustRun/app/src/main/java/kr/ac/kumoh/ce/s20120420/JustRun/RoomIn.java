package kr.ac.kumoh.ce.s20120420.JustRun;

/**
 * Created by woong on 2017-06-07.
 */

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RoomIn extends AppCompatActivity {
    Intent next_intent;
    String mCreate; // 방을 생성한 것인지, 들어온 것인지 구별
    String mNum;    // 들어온 방번호
    String mTitle;  // 들어온 방제목

    TextView mRoomInNum;
    TextView mRoomInTitle;
    TextView[] mUserTv = new TextView [12];    // 12명의 텍스트뷰 배열

    Button mPoliceBtn;
    Button mThiefBtn;
    Button mReadyBtn;
    public int mInUserNum;
    int[] mUsers;  // xml에 있는 12명의 int형 id 저장

    InUserInfo[] mInUser = new InUserInfo[12];  // 12명의 (닉네임, 역할) 클래스 배열


    Data_ingame di;
    int room_num;   //이전 액티비티에서 넘겨받음

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
            mInUser[i] = new InUserInfo("user"+(i+1),"","no","");
        }

        mRoomInNum = (TextView)findViewById(R.id.roomInNum);
        mRoomInTitle = (TextView)findViewById(R.id.roomInTitle);
        mThiefBtn = (Button)findViewById(R.id.thiefBtn);
        mPoliceBtn = (Button)findViewById(R.id.policBtn);
        mReadyBtn = (Button)findViewById(R.id.readyBtn);

        mCreate = intent.getExtras().getString("create");
        Log.i("들어온 값은 ?", mCreate);
        if (mCreate.equals("master")) {
            String title = intent.getExtras().getString("createTitle");
            String password = intent.getExtras().getString("createPwd");
            String radius = intent.getExtras().getString("createRadius");
            Log.i("여기로 들어옴", "ㄴㅇㄹ");
            String time = intent.getExtras().getString("createTime");
            RoomList.mSocket.emit("room_Master", title, password, radius, time);
            RoomList.mSocket.on("room_Master", roomMaster);

            mReadyBtn.setBackgroundResource(R.drawable.button_play);
            TextView user = (TextView)findViewById(R.id.user1);
            user.setText(RoomList.mInNicName);
            user.setBackgroundResource(R.drawable.edge_default_master);

        }// ********** 방 생성하는 경우 **********

        else if(mCreate.equals("nomal")){
            mNum = intent.getExtras().getString("num");
            String title = intent.getExtras().getString("title");
            Log.i("들어온 값은 : ", mNum);
            Log.i("들어온 값은 : ", title);

            mRoomInNum.setText(mNum);
            mRoomInTitle.setText(title);
            RoomList.mSocket.emit("notifyAll",mNum);
        }// ********** 방에 들어온 경우 **********

        RoomList.mSocket.on("notifyAll", notifyAll);
        RoomList.mSocket.on("changeMaster", changeMaster);
        RoomList.mSocket.on("room_basic_infor", room_basic_infor); //방정보 받아오기
        RoomList.mSocket.on("start_game",start_game);



        mPoliceBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                RoomList.mSocket.emit("roleChange", mRoomInNum.getText(), "police");
                mReadyBtn.setEnabled(true);
            }
        });// ********** 경찰하기 **********

        mPoliceBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_UP) {
                    mPoliceBtn.setBackgroundResource(R.drawable.button_police);
                }
                if (action == MotionEvent.ACTION_DOWN) {
                    mPoliceBtn.setBackgroundResource(R.drawable.button_police2);
                }
                return false;
            }
        });
        mThiefBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RoomList.mSocket.emit("roleChange", mRoomInNum.getText(), "thief");
                mReadyBtn.setEnabled(true);
            }
        });// ********** 도둑하기 **********

        mThiefBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_UP) {
                    mThiefBtn.setBackgroundResource(R.drawable.button_thief);
                }
                if (action == MotionEvent.ACTION_DOWN) {
                    mThiefBtn.setBackgroundResource(R.drawable.button_thief2);
                }
                return false;
            }
        });
        mReadyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mCreate.equals("master")){
                    int count = 0;
                    for(int i = 0; i < mInUserNum; i++){
                        if (mInUser[i].getReady().equals("no")) {
                            break;
                        }
                        count++;
                    }
                    if(count == mInUserNum){
                        RoomList.mSocket.emit("playRoom", mNum);
                        GameStartDialog();
                    }else{
                        NotReadyDialog();
                    }

                } else if (mCreate.equals("nomal")){
                    Log.i("방에 들어온자 :" , mCreate);
                    RoomList.mSocket.emit("peopleReady", mRoomInNum.getText());
                }
            }
        });// ********** 준비하기 **********
        mReadyBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_UP) {
                    if(mCreate.equals("master"))
                    {
                        mReadyBtn.setBackgroundResource(R.drawable.button_play);
                    }
                    else{
                        mReadyBtn.setBackgroundResource(R.drawable.button_ready);
                    }

                }
                if (action == MotionEvent.ACTION_DOWN) {
                    if(mCreate.equals("master"))
                    {
                        mReadyBtn.setBackgroundResource(R.drawable.button_play2);
                    }
                    else{
                        mReadyBtn.setBackgroundResource(R.drawable.button_ready2);
                    }
                }
                return false;
            }
        });
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
            RoomList.mSocket.off("notifyAll", notifyAll);
            RoomList.mSocket.off("room_Master", roomMaster);
            RoomList.mSocket.off("changeMaster", changeMaster);
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
        String ready;
        String head;

        public InUserInfo(String nicName, String role, String ready, String head) {
            this.nicName = nicName;
            this.role = role;
            this.ready = ready;
            this.head = head;
        }

        public String getNicName() {
            return nicName;
        }
        public String getRole() {
            return role;
        }
        public String getReady() {
            return ready;
        }
        public String getHead() {
            return head;
        }

        public void setNicName(String nicName) {
            this.nicName = nicName;
        }
        public void setRole(String role) {
            this.role = role;
        }
        public void setReady(String ready) {
            this.ready = ready;
        }
        public void setHead(String ready) {
            this.head = ready;
        }
    }
    private void GameStartDialog(){
        /*new AlertDialog.Builder(this)
                .setTitle("Play in to RunRun")
                .setMessage("곧 게임을 시작합니다.")
                .setPositiveButton("확인", null)
                .show();*/

        RoomList.mSocket.emit("start_game",mNum); //모두 준비 완료

    }

    private void NotReadyDialog(){
        new AlertDialog.Builder(this)
                .setTitle("Not Play")
                .setMessage("준비 하지 않은 인원이 있습니다.")
                .setPositiveButton("확인", null)
                .show();
    }
    private Emitter.Listener changeMaster = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mCreate = "master";
                    Log.i("master", mCreate);
                    mReadyBtn.setBackgroundResource(R.drawable.button_play);
                    mReadyBtn.setEnabled(true);
                }
            });
        }
    };
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
                        Log.i("알리기", "asdasdas");
                        mInUserNum=0;
                        for(int i = 0; i<12; i++){      // 유저 정보 초기화
                            mInUser[i].setNicName("");
                            mInUser[i].setRole("");
                            mInUser[i].setReady("no");
                            mInUser[i].setHead("normal");
                            mUserTv[i].setText(mInUser[i].getNicName());
                            mUserTv[i].setBackgroundResource(R.drawable.edge_default);
                        }
                        for(int i = 1; i < jsonMainNode.length(); i++) {
                            JSONObject jsonChildNode = jsonMainNode.getJSONObject(i);
                            String nicName = jsonChildNode.getString("nicName");
                            Log.i("들어온자 ", nicName);
                            String role = jsonChildNode.getString("role");
                            Log.i("들어온자의 역할 ", role);
                            String ready = jsonChildNode.getString("ready");
                            String head = jsonChildNode.getString("head");
                            mInUser[i-1].setNicName(nicName);
                            mInUser[i-1].setRole(role);
                            mInUser[i-1].setReady(ready);
                            mInUser[i-1].setHead(head);
                            mInUserNum++;
                        }
                        int j = 0;
                        for(int i = 1; i < jsonMainNode.length(); i++){
                            if(mInUser[i-1].getRole().equals("police")){
                                mUserTv[j].setText(mInUser[i-1].getNicName());
                                if(mInUser[i-1].getHead().equals("master")){
                                    mUserTv[j].setBackgroundResource(R.drawable.edge_police_master);
                                    //mReadyBtn.setEnabled(true);
                                }
                                else{
                                    mUserTv[j].setBackgroundResource(R.drawable.edge_police);
                                    if(mInUser[i-1].getReady().equals("ok")){
                                        mUserTv[j].setBackgroundResource(R.drawable.edge_police_ready);
//                                        mPoliceBtn.setEnabled(false);
//                                        mThiefBtn.setEnabled(false);
                                    }
//                                    else{
////                                        mPoliceBtn.setEnabled(true);
////                                        mThiefBtn.setEnabled(true);
////                                        mReadyBtn.setEnabled(true);
//                                    }
                                }
                                Log.i("police 역할 ", mInUser[i-1].getNicName());
                                j++;
                            }
                        }
                        for(int i = 1; i < jsonMainNode.length(); i++){
                            if(mInUser[i-1].getRole().equals("thief")){
                                Log.i("thief 역할 ", mInUser[i-1].getNicName());
                                mUserTv[j].setText(mInUser[i-1].getNicName());
                                if(mInUser[i-1].getHead().equals("master")){
                                    mUserTv[j].setBackgroundResource(R.drawable.edge_thief_master);
                                    //  mReadyBtn.setEnabled(true);
                                }
                                else{
                                    mUserTv[j].setBackgroundResource(R.drawable.edge_thief);
                                    if(mInUser[i-1].getReady().equals("ok")){
                                        mUserTv[j].setBackgroundResource(R.drawable.edge_thief_ready);
//                                        mPoliceBtn.setEnabled(false);
//                                        mThiefBtn.setEnabled(false);
                                    }
//                                    else{//ready가아닐때
//                                        mPoliceBtn.setEnabled(true);
//                                        mThiefBtn.setEnabled(true);
//                                        mReadyBtn.setEnabled(true);
//                                    }
                                }

                                j++;
                            }
                        }
                        for(int i = 1; i < jsonMainNode.length(); i++){
                            if(mInUser[i-1].getRole().equals("default")){
                                Log.i("default 역할 ", "g "+i+" "+j);
                                mUserTv[j].setText(mInUser[i-1].getNicName());

                                if(mInUser[i-1].getHead().equals("master")){
                                    mUserTv[j].setBackgroundResource(R.drawable.edge_default_master);
                                    // mReadyBtn.setEnabled(true);
                                }
                                else{
                                    mUserTv[j].setBackgroundResource(R.drawable.edge_default);
                                    // mReadyBtn.setEnabled(false);//default일때 Ready버튼 비활성화
                                }

                                j++;
                            }
//                            else{//role이 police,thief인 경우 ready 버튼 활성화
//                                mReadyBtn.setEnabled(true);
//                            }

                        }
                    }catch (JSONException | NullPointerException e){
                        Toast.makeText(getApplicationContext(),
                                "Error" + e.toString(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    };

    //서버로부터 게임에 필요한 정보를 가져와서 클래스에 저장하는 코드
    private Emitter.Listener room_basic_infor = new Emitter.Listener() {

        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try{

                        JSONObject data2 = (JSONObject)args[0];
                        JSONObject data=data2.getJSONObject("data");
                        room_num=Integer.valueOf(mNum);
                        di=new Data_ingame(Integer.valueOf(data.getString("thief_item_num")),data.getInt("police_item_num"),data.getInt("zone_num"),data.getInt("range"),
                                room_num,Integer.valueOf(data.getString("game_time")),data.getBoolean("my_role"),data.getBoolean("room_master"));

                        next_intent = new Intent(RoomIn.this, InGameReady.class);
                        next_intent.putExtra("OBJECT", di);
                        startActivity(next_intent);
                        //여기서 다음 시작할 intent에 di putextra 시키고 startactivity(intent)ㄱㄱㄱㄱㄱ


                    }catch (JSONException | NullPointerException e){
                        Toast.makeText(getApplicationContext(),
                                "Error" + e.toString(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    };

    private Emitter.Listener start_game = new Emitter.Listener() {

        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    RoomList.mSocket.emit("room_basic_infor",mNum); //시작하기 직전 게임에 대한 기본정보 달라고 서버에게 요청
                }
            });
        }
    };

}


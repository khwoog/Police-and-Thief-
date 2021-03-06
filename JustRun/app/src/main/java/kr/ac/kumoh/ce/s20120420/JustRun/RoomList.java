package kr.ac.kumoh.ce.s20120420.JustRun;

/**
 * Created by woong on 2017-06-07.
 */
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;

public class RoomList extends AppCompatActivity implements AdapterView.OnItemClickListener{

    public static Socket mSocket;
    {
        try {
            mSocket = IO.socket("http://220.230.113.125:3000");
            //mSocket = IO.socket("http://192.168.168.109:3030");
        } catch (URISyntaxException e) {}
    }
    int f  =0;
    protected ArrayList<RoomCoverInfo> mRoomArray = new ArrayList<RoomCoverInfo>();
    protected JSONObject mResult=null;
    protected Button mExBtn;
    protected static String mInNicName;    // 접속한 사용자의 닉네임 저장
    protected String mRoomNum;      // 클릭한 방의 번호
    protected String mRoomTitle;    // 클릭한 방의 제목
    protected String mRoomPassword; // 클릭한 방의 패스워드
    protected String mRoomPlayInfo;   // 클릭한 방의 플레이 정보
    protected String mRoomTotal;
    protected Button mNewBtn;   // 새로고침 버튼 (방리스트 초기화)
    protected Button mRoomBtn;  // 방생성 버튼

    protected RoomAdapter mAdapter; //각 방을 생성할 어댑터
    protected ListView mList;   // 각 방을 포함할 리스트뷰

    protected EditText mEdPwd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.room_list);

        Intent intent = getIntent();
        mInNicName = intent.getExtras().getString("nicName");
        Log.i("접속한 닉네임 : ", mInNicName);

        mSocket.connect();  // *************** 소켓 연결 ***************
        try{
            Thread.sleep(1000);
            mSocket.emit("enterUser", mInNicName); // *************** 유저 접속 ***************
            mSocket.on("reqRoomList", mListRoom); // *************** 리스트 갱신 ***************
            mSocket.on("requestRoomOK", mActiveResult);
            mSocket.on("pwdResult", mPwdResult);
            Log.i("hi", "sfsdfsdf");
        }catch (Exception e){};

        mExBtn = (Button)findViewById(R.id.explain);
        mExBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ExplainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
            }
        });
        final JSONObject data = new JSONObject();
        try{
            data.put("key1", "asdf");
            data.put("key2", "dfdf");
        }catch(JSONException e){}

        mRoomBtn = (Button)findViewById(R.id.roomBtn);
        mRoomBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), RoomCreate.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
            }
        });// *************** 방 생성하기, 방 설정하는 액티비티를 띄움 ***************

        mNewBtn = (Button)findViewById(R.id.newBtn);
        mNewBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                mSocket.emit("reqRoomList", "jo");
            }
        });// *************** 새로고침 버튼, 방의 목록을 서버에게 요청 ***************

        mAdapter = new RoomAdapter(this, R.layout.room_cover);  // 생성할 방의 레이아웃을 설정
        mList = (ListView)findViewById(R.id.listView);  // 리스트 뷰 가져오기
        mList.setAdapter(mAdapter);                     // 가져온 리스트뷰에 방을 셋팅
        mList.setOnItemClickListener(this);             // 클릭 리스터 셋팅
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //Toast.makeText(this,mRoomArray.get(position).getNum(), Toast.LENGTH_SHORT).show();
        mRoomNum        = mRoomArray.get(position).getNum();
        mRoomTitle      = mRoomArray.get(position).getTitle();
        mRoomPassword   = mRoomArray.get(position).getPassword();
        mRoomPlayInfo   = mRoomArray.get(position).getPlayinfo();
        mRoomTotal      = mRoomArray.get(position).getTotal();

        if(mRoomPlayInfo.equals("true")){
            new AlertDialog.Builder(this)
                    .setTitle("Already Playing..")
                    .setMessage("게임이 시작된 방입니다.")
                    .setPositiveButton("확인", null)
                    .show();
        }else{
            if(mRoomTotal.equals("12")){
                new AlertDialog.Builder(this)
                        .setTitle("full..")
                        .setMessage("방 안에 인원이 꽉 찼습니다.")
                        .setPositiveButton("확인", null)
                        .show();
            }else{
                if(mRoomPassword.equals("")){   // 패스워드가 설정되어있지 않으면 다음 액티비티 실행
                    mSocket.emit("requestRoomOK", mRoomNum);
                }else{  // ********** 패스워드가 설정되어 있다면 비밀번호 입력 다이얼 로그 띄우기 **********
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                    dialogBuilder.setTitle("not open to the public ");
                    dialogBuilder.setMessage("비밀번호를 입력하세요.");
                    mEdPwd = new EditText(this);
                    dialogBuilder.setView(mEdPwd);
                    dialogBuilder.setPositiveButton("Yes", yesBtnResultPwd);
                    dialogBuilder.setNegativeButton("No", noBtnResultPwd );
                    dialogBuilder.show();
                }
            }
        }


    }//*************** 방 목록 클릭 -> 방에 입장 ******************


    public class RoomAdapter extends ArrayAdapter<RoomCoverInfo> {

        private LayoutInflater mInflater = null;

        public RoomAdapter(Context context, int resource) {
            super(context, resource);
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return mRoomArray.size();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            RoomViewHolder viewHolder;

            if(convertView == null){
                convertView = mInflater.inflate( R.layout.room_cover, parent, false); // room_cover각각의 아이템layout 인플레이터
                viewHolder = new RoomViewHolder();
                viewHolder.txNum = (TextView) convertView.findViewById(R.id.roomNum);
                viewHolder.txTitle = (TextView) convertView.findViewById(R.id.roomTitle);
                viewHolder.txTotal = (TextView) convertView.findViewById(R.id.roomTotal);
                convertView.setTag(viewHolder);
            }
            else{
                viewHolder = (RoomViewHolder) convertView.getTag();
            }
            RoomCoverInfo info = mRoomArray.get(position);
            viewHolder.txNum.setText(info.getNum());
            viewHolder.txTitle.setText(info.getTitle());
            viewHolder.txTotal.setText(info.getTotal()+" / 12");
            if(info.getTotal().equals("12")){
                //   convertView.setBackgroundColor(Color.argb(204,255,0,0));
            }
            return convertView;
        }
    }
    static class RoomViewHolder{
        TextView txNum;
        TextView txTitle;
        TextView txTotal;
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Question");
        dialogBuilder.setMessage("닉네임 입력창으로 돌아가시겠습니까?");
        dialogBuilder.setPositiveButton("Yes", yesBtnGoToMaion);
        dialogBuilder.setNegativeButton("No", noBtnGoToMaion );
        dialogBuilder.show();
    }
    private DialogInterface.OnClickListener yesBtnGoToMaion = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            mSocket.disconnect();
            mSocket.off("reqRoomList", mListRoom);
            mSocket.off("pwdResult", mPwdResult);
            mSocket.off("requestRoomOK", mActiveResult);
            dialog.cancel();
            finish();
        }
    };
    private DialogInterface.OnClickListener noBtnGoToMaion = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.cancel();
        }
    };

    private DialogInterface.OnClickListener yesBtnResultPwd = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            String inputPwd = mEdPwd.getText().toString();
            mSocket.emit("requestPwd", mRoomNum, inputPwd);

            dialog.cancel();
        }
    };
    private DialogInterface.OnClickListener noBtnResultPwd = new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.cancel();
        }
    };

    //********** 방의 정보 mRoomArray가져와서 클래스에 넣기 **********
    private Emitter.Listener mListRoom = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mRoomArray.clear();
                    Log.i("방의 리스트를 보여줍니다.", f+"번"); f++;
                    try{
                        JSONObject data = (JSONObject)args[0];
                        JSONArray jsonMainNode = data.getJSONArray("room");

                        for(int i = 1; i < jsonMainNode.length(); i++) {
                            JSONObject jsonChildNode = jsonMainNode.getJSONObject(i);
                            String active = jsonChildNode.getString("active");
                            Log.i(i+"번째 방의 활성화 상태  : ",  active);
                            if(active.equals("false")){
                                Log.i(i+"번째 방이 비활성화입니다.  : ",  active);
                                continue;
                            }
                            String title = jsonChildNode.getString("title");
                            Log.i("RoomList"+i+"번째 방의 제목 : ",  title);
                            String num = jsonChildNode.getString("num");
                            Log.i("RoomList"+i+"번째 방의 번호: ",  num);
                            String total = jsonChildNode.getString("total");
                            Log.i("RoomList"+i+"번째 방의 총원: ",  total);
                            String password = jsonChildNode.getString("pwd");
                            Log.i("RoomList"+i+"번째 방의 비밀번호: ",  password);
                            String playInfo = jsonChildNode.getString("play");
                            Log.i("RoomList"+i+"play 정보: ",  playInfo);

                            mRoomArray.add(new RoomCoverInfo(num, title, total, password, playInfo));
                        }
                    }catch (JSONException | NullPointerException e){
                        Toast.makeText(getApplicationContext(),
                                "Error" + e.toString(), Toast.LENGTH_LONG).show();
                        mResult = null;
                    }
                    mAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    //********** 방의 정보 mRoomArray가져와서 클래스에 넣기 **********
    private Emitter.Listener mPwdResult = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject)args[0];
                    Log.i("클릭한 방의 비밀번호 : ",data.toString());
                    try{
                        String result = data.getString("message");
                        if(result.equals("OK")){
                            Log.i("비밀번호가 일치합니다. ", result);
                            mSocket.emit("requestRoomOK", mRoomNum);
                        }
                        else{
                            FailedPassword();
                        }
                    }catch (JSONException e){
                        Toast.makeText(getApplicationContext(),
                                "Error" + e.toString(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    };

    //********** 방이 존재하는지 확인 **********
    private Emitter.Listener mActiveResult = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject)args[0];
                    Log.i("클릭한 방의 상태 : ",data.toString());
                    try{
                        String result = data.getString("result");
                        if(result.equals("true")){
                            mSocket.emit("roomPeopleAdd", mRoomNum);
                            StartRoomIn();
                        }
                        else if(result.equals("false")){

                            FaileRequestRoom();
                        }
                    }catch (JSONException e){
                        Toast.makeText(getApplicationContext(),
                                "Error" + e.toString(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    };
    protected void StartRoomIn(){
        Intent intent = new Intent(getApplicationContext(), RoomIn.class);
        intent.putExtra("create", "nomal");
        intent.putExtra("num", mRoomNum);
        intent.putExtra("title", mRoomTitle);
        startActivity(intent);
    }
    protected void FailedPassword(){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("비밀번호가 일치하지 않습니다.");
        dialogBuilder.setMessage("");
        dialogBuilder.show();
    }
    protected void FaileRequestRoom(){
        new AlertDialog.Builder(this)
                .setTitle("No Exist")
                .setMessage("존재하지 않는 방입니다.")
                .setPositiveButton("확인", null)
                .show();
    }
    public class To_Server{
        double latitude[];
        double longitude[];
        int index;

        public To_Server (int index) {
            this.latitude = new double[3];
            this.index = index;
            this.longitude = new double[3];
            for(int i=0;i<3;i++)
            {
                latitude[i]=(double)i;
                longitude[i]=(double)i+1;
            }

        }

        public double[] getLatitude() {
            return latitude;
        }

        public double[] getLongitude() {
            return longitude;
        }

        public int getIndex() {
            return index;
        }
    }
}
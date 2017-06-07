package kr.ac.kumoh.s20121133.ce.beforegame;

import android.app.LauncherActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
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
import java.util.List;

/**
 * Created by jang on 2017-05-31.
 */

public class RoomList extends AppCompatActivity implements AdapterView.OnItemClickListener{

    public static Socket Socket;
    {
        try {
            Socket = IO.socket("http://220.230.113.125:3000");
            //Socket = IO.socket("http://192.168.43.196:3030");
        } catch (URISyntaxException e) {}
    }
    private Emitter.Listener test = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i("비밀번호 받습니다.","");
                    JSONObject data = (JSONObject)args[0];
                    Log.i("비밀번호 받습니다.1",data.toString());

                    try{
                        String result = data.getString("message");
                        Log.i("받았다", result) ;
                        if(result.equals("OK")){
                            Socket.emit("roomPeopleAdd", mRoomNum);
                            StartRoomIn();
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
    protected void StartRoomIn(){
        Intent intent = new Intent(getApplicationContext(), RoomIn.class);
        intent.putExtra("create", "no");
        intent.putExtra("num", mRoomNum);
        intent.putExtra("title", mTitle);
        startActivity(intent);
    }
    protected void FailedPassword(){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Failed");
        dialogBuilder.setMessage("");
        dialogBuilder.show();
    }
    private Emitter.Listener objectRoom = new Emitter.Listener() {

        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mRoomArray.clear();
                    Log.i("된다된다", "된다");
                    try{
                        JSONObject data = (JSONObject)args[0];
                        JSONArray jsonMainNode = data.getJSONArray("room");

                        for(int i = 1; i < jsonMainNode.length(); i++) {
                            JSONObject jsonChildNode = jsonMainNode.getJSONObject(i);
                            String active = jsonChildNode.getString("active");
                            Log.i("된다된다3 title : ",  active);
                            if(active.equals("false"))
                                continue;
                            String title = jsonChildNode.getString("title");
                            //  Log.i("된다된다3 title : ",  title);
                            String num = jsonChildNode.getString("num");
                            //  Log.i("된다된다 pwd : ",  pwd);
                            String total = jsonChildNode.getString("total");
                            //  Log.i("된다된다 areaRadius : ",  areaRadius);
                            String password = jsonChildNode.getString("pwd");
                            mRoomArray.add(new RoomCoverInfo(num, title, total, password));
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

    protected ArrayList<RoomCoverInfo> mRoomArray = new ArrayList<RoomCoverInfo>();
    protected JSONObject mResult=null;
    public static String mInNicName;
    protected Button mNewBtn;
    protected Button mRoomBtn;
    protected RoomAdapter mAdapter;
    protected ListView mList;
    protected String mRoomNum;
    protected String mTitle;
    protected String mPassword;
    protected EditText mEdPwd;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.room_list);
        Intent intent = getIntent();
        mInNicName = intent.getExtras().getString("nicName");
        Log.i("들어온 값은 : ", mInNicName);

        Socket.connect();
        Socket.emit("enterUser", mInNicName);
        Socket.on("reqRoomList", objectRoom); // *************** 요청 받고 목록 갱신 ***************
        Socket.on("pwdResult", test);

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
                Socket.emit("reqRoomList", "jo");
            }
        });// *************** 새로고침 버튼, 방의 목록을 서버에게 요청 ***************

        mAdapter = new RoomAdapter(this, R.layout.room_cover);
        mList = (ListView)findViewById(R.id.listView);
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //Toast.makeText(this,mRoomArray.get(position).getNum(), Toast.LENGTH_SHORT).show();

        mRoomNum = mRoomArray.get(position).getNum();
        mTitle = mRoomArray.get(position).getTitle();
        mPassword = mRoomArray.get(position).getPassword();

        if(mPassword.equals("")){
            Socket.emit("roomPeopleAdd", mRoomNum);
            StartRoomIn();
        }else{
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle("not open to the public ");
            dialogBuilder.setMessage("비밀번호를 입력하세요.");
            mEdPwd = new EditText(this);
            dialogBuilder.setView(mEdPwd);
            dialogBuilder.setPositiveButton("Yes", yesBtnClickListener);
            dialogBuilder.setNegativeButton("No", noBtnClickListener );
            dialogBuilder.show();
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
            if(info.getTotal().equals("0")){
                convertView.setBackgroundColor(Color.argb(204,255,0,0));
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
            finish();
        }
    };
    private DialogInterface.OnClickListener noBtnGoToMaion = new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.cancel();
        }
    };

    private DialogInterface.OnClickListener yesBtnClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            String inputPwd = mEdPwd.getText().toString();
            Socket.emit("test", mRoomNum, inputPwd);
        }
    };
    private DialogInterface.OnClickListener noBtnClickListener = new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.cancel();
        }
    };
}
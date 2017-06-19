package kr.ac.kumoh.ce.s20120420.JustRun;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;

import net.daum.mf.map.api.MapCircle;
import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class Police extends AppCompatActivity implements MapView.MapViewEventListener, MapView.POIItemEventListener, MapView.CurrentLocationEventListener {
    int partner_num; //동료 경찰 수
    int enermy_num; //도둑 수
    int item_num;   //아이템 수(모든 경찰 수)
    int zone_num;   //점령존 수(도둑 수)
    int range;       //제한구역범위
    int zone_range;   //점령존 범위
    int roomnum;     //방 번호
    int my_index;    //방 에서의 내 인덱스
    int now_catch; //잡은 도둑 인덱스
    int now_item;//획득한 아이템 인덱스
    int game_time;//게임 시간
    private boolean item_get;//아이템 획득 유무
    boolean first = true;
    boolean change = false;
    boolean out=false;
    boolean initialized=false;
    boolean game_over=false;
    //쓰레드를 위한 핸들러
    Handler Phandler = new Handler();
    Handler Ohandler = new Handler();
    Handler Chandler = new Handler();
    Handler Ihandler = new Handler();
    Handler Ehandler = new Handler();
    Handler Ghandler = new Handler();

    //쓰레드 종료하기 위해 쓰는변수
    private boolean PT_running;
    private boolean OT_running;
    private boolean CT_running;
    private boolean IT_running;
    private boolean ET_running;
    private boolean GT_running;
    boolean visible=false;

    //이동금지
    Animation flow;
    Animation unflow;



    //쓰레드
    PartnerThread p_thread; //동료 위치 변경 쓰레드
    OutcheckThread o_thread;//범위 out check
    CatchThread c_thread;  //자동 검거 쓰레드
    ItemThread i_thread;    //아이템 획득 쓰레드
    EnermyThread e_thread;  //도둑 위치 갱신 쓰레드
    GameThread g_thread;

    Enermy thief[];     //도둑 객체
    Item handcuff[];   //수갑아이템 객체
    Partner police[];  //동료객체
    Zone zone[];        //점령존 객체
    Zone Area;          //제한구역 존
    Zone Prison;        //시작존(감옥존)

    MapPoint tempp = MapPoint.mapPointWithGeoCoord(36.146451, 128.393725);  //현재 내위치 표현하기위한 변수
    MapPoint control_point; //기준점 - 이 점을 기준으로 제한구역 및 시작존(감옥존) 생성
    MapCircle circle1;  //내 위치 주변에 원 생성하기 위한 변수

    Vibrator vibrator;  //진동위한 변수
    MapView mapView;    //맵뷰

    //ui 겹치기 위한변수들
    LayoutInflater inflater,hid_inflater;
    LinearLayout linear;
    FrameLayout hide_linear;
    LinearLayout.LayoutParams paramlinear,hide_paramlinear;

    Button b1, b2;

    Data_Police data; //ingameready 에서 받아온 게임정보(경찰용)
    Intent intent;

    private LocationManager locationManager;
    private LocationListener listener;


    JSONObject out_ob = new JSONObject();
    JSONObject item_ob = new JSONObject();
    JSONObject partner_ob = new JSONObject();
    JSONObject enermy_ob = new JSONObject();
    JSONObject catch_ob = new JSONObject();
    JSONObject mylocation_ob = new JSONObject();

    private static final int MILLISINFUTURE = 45*1000;
    private static final int COUNT_DOWN_INTERVAL = 1000;
    private int count = 44;
    private CountDownTimer countDownTimer;

    TextView game_tv;

    long[] mainpattern={0,1000,500,1000};
    LayoutInflater inflater2;
    View layout;
    Toast toast ;
    TextView text ;

    ImageView death_view;

    LinearLayout death_l1,death_l2;


    @Override
    public void onBackPressed() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.ingame);

        inflater2 = getLayoutInflater();
        toast= new Toast(getApplicationContext());
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        toast.setDuration(Toast.LENGTH_LONG);



        Log.i("1","1");
        //이전 액티비티에서 받아온 초기화 관련된 변수
        intent = getIntent();
        data = (Data_Police) intent.getSerializableExtra("OBJECT");

       ///////////////////////////// //ui 겹치기 코드////////////////////////////
        inflater = (LayoutInflater) getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        linear = (LinearLayout) inflater.inflate(R.layout.basic, null);
        paramlinear = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        addContentView(linear, paramlinear);

        game_tv=(TextView)findViewById(R.id.time_tv);
////////////////////////////////////////////////////////////////////////

        /////////////////////////////////////////이동중화면가림////////////////////////////////////////
        //slier_inflater=(LayoutInflater) getSystemService(
        //      Context.LAYOUT_INFLATER_SERVICE);
        //slidingPage01 = (LinearLayout)slier_inflater.inflate(R.id.slidingPage01,null);
       /* slidingPage01 = (LinearLayout)findViewById(R.id.slidingPage01);
        slidingPage01.setVisibility(View.VISIBLE);*/
        flow = AnimationUtils.loadAnimation(this,R.anim.flow);
        unflow = AnimationUtils.loadAnimation(this,R.anim.unflow);

        FlowAnimationListener animListener = new FlowAnimationListener();
        flow.setAnimationListener(animListener);
        unflow.setAnimationListener(animListener);
        hid_inflater = (LayoutInflater) getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        hide_linear=(FrameLayout) hid_inflater.inflate(R.layout.slider, null);

        hide_paramlinear = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        addContentView(hide_linear, hide_paramlinear);
//////////////////////////////////////////////////////////////////////////////////


        death_view=(ImageView)findViewById(R.id.death_iv);
        death_view.setImageResource(R.drawable.police_dead);



        death_l1=(LinearLayout)findViewById(R.id.death_ll1);
        death_l2=(LinearLayout)findViewById(R.id.death_ll2);

        hide_linear.setVisibility(View.INVISIBLE);
        item_get = false;

        b1 = (Button) findViewById(R.id.button1);  //수갑 아이템
        b2 = (Button) findViewById(R.id.button2);  //도둑 위치 get
        b2.setTextColor(Color.rgb(255,0,0));

        b1.setBackgroundResource(R.drawable.police_button1x);
        b2.setBackgroundResource(R.drawable.police_button2);

        b2.setEnabled(true);




        b1.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {


            }
        });

        //도둑 찾기 클릭시
         b2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Renew_enermy_location();
                count = 44;
                b2.setBackgroundResource(R.drawable.police_button2x);
                b2.setEnabled(false);
               // b2.setBackgroundResource(R.drawable.thiefoff);
                countDownTimer();
                countDownTimer.start();

            }
        });




        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        listener = new LocationListener() {
            double myspeed_double;

            @Override
            public void onLocationChanged(Location location) {
                myspeed_double = location.getSpeed();
                /*if(myspeed_double>=1.0)
                {

                    if(visible==false) {
                        hide_linear.setVisibility(View.VISIBLE);
                        hide_linear.startAnimation(flow);

                    }
                }
                else
                {
                    if(visible==true) {
                        hide_linear.startAnimation(unflow);
                    }

                }*/
                tempp = MapPoint.mapPointWithGeoCoord(location.getLatitude(), location.getLongitude());
/////////////////////////////////////내 위치 보내주기 서버로/////////////////////////////////////////////
                try {
                    mylocation_ob.put("latitude", tempp.getMapPointGeoCoord().latitude);
                    mylocation_ob.put("room_num", roomnum);
                    mylocation_ob.put("index", my_index);
                    mylocation_ob.put("longitude", tempp.getMapPointGeoCoord().longitude);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                RoomList.mSocket.emit("my_location", mylocation_ob);
                /////////////////////////////////////내 위치 보내주기 서버로/////////////////////////////////////////////
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {
                if (change == false) {
                    Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(i);
                    change = true;
                }

            }
        };
        Log.i("2","2");
        mapView = new MapView(this);
        mapView.setDaumMapApiKey("4e6090d28d2a2cc01fea4130929cf630");
        mapView.setMapType(MapView.MapType.Standard);
        ViewGroup mapViewContainer = (ViewGroup) findViewById(R.id.map_view);
        mapViewContainer.addView(mapView);
        mapView.setMapViewEventListener(this);
        mapView.setPOIItemEventListener(this);
        mapView.setCurrentLocationEventListener(this);

        vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);


        RoomList.mSocket.on("request_police_location", request_police_location);
        RoomList.mSocket.on("request_enermy_location", request_enermy_location);
        RoomList.mSocket.on("catch_thief", catch_thief);
        RoomList.mSocket.on("alldeath", alldeath);
        RoomList.mSocket.on("police_item_get", police_item_get);
        RoomList.mSocket.on("police_out", police_out);
        RoomList.mSocket.on("zone_clear", zone_clear);
        RoomList.mSocket.on("allclear", allclear);
        RoomList.mSocket.on("thief_out", thief_out);
        RoomList.mSocket.on("thief_item_use", thief_item_use);
        RoomList.mSocket.on("thief_escape", thief_escape);

        Log.i("3","3");
    }

    @Override
    protected void onDestroy() {
        //종료 될때 트래킹 중지

        /*   mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);
        mapView.setShowCurrentLocationMarker(false);*/
        super.onDestroy();
        if(game_over==false)
        {Game_Over();
            RoomList.mSocket.off("request_police_location", request_police_location);
            RoomList.mSocket.off("request_enermy_location", request_enermy_location);
            RoomList.mSocket.off("catch_thief", catch_thief);
            RoomList.mSocket.off("alldeath", alldeath);
            RoomList.mSocket.off("police_item_get", police_item_get);
            RoomList.mSocket.off("police_out", police_out);
            RoomList.mSocket.off("zone_clear", zone_clear);
            RoomList.mSocket.off("allclear", allclear);
            RoomList.mSocket.off("thief_out", thief_out);
            RoomList.mSocket.off("thief_item_use", thief_item_use);
            RoomList.mSocket.off("thief_escape", thief_escape);


        }

        try{
            countDownTimer.cancel();
        } catch (Exception e) {}
        countDownTimer=null;

    }

    //맵이 처음 시작될때
    @Override
    public void onMapViewInitialized(MapView mapView) {

        Initialize(); //intent로 정보 넘겨 받아 온 것 초기화
        while(!initialized)
        {
            if(initialized)
                break;
        }
        Start_GT();
        mapView.setCurrentLocationRadius(0);
        mapView.setDefaultCurrentLocationMarker();
        //위치 정보 받아오기 시작
        check_location_gps();
        //트래킹 시작
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithHeadingWithoutMapMoving);
        mapView.setMapCenterPointAndZoomLevel(MapPoint.mapPointWithGeoCoord(tempp.getMapPointGeoCoord().latitude, tempp.getMapPointGeoCoord().longitude), 1, false);       //처음 시작될때 내 위치로 맵 이동

        //아이템 좌표 존 좌표 초기시작 위치 동료 위치 다 알고있다고 했을때

        //동료,도둑,점령존,제한구역,시작존,아이템,표시
        Show_Area();
        Show_Enermy();
        Show_item();
        Show_partner();
        Show_zone();
        //쓰레드 시작
        Start_PT();
        Start_ET();
        Start_IT();
        Start_OT();
    }

    //화면 움직이면
    @Override
    public void onMapViewCenterPointMoved(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewZoomLevelChanged(MapView mapView, int i) {

    }

    @Override
    public void onMapViewSingleTapped(MapView mapView, MapPoint mapPoint) {

    }

    @Override   //맵 클릭 두번시
    public void onMapViewDoubleTapped(MapView mapView, MapPoint mapPoint) {

        if(out==false)
        {
            mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);
            mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithHeadingWithoutMapMoving);
        }

    }

    @Override
    public void onMapViewLongPressed(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewDragStarted(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewDragEnded(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewMoveFinished(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onPOIItemSelected(MapView mapView, MapPOIItem mapPOIItem) {
    }

    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem) {

    }

    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem, MapPOIItem.CalloutBalloonButtonType calloutBalloonButtonType) {

    }

    @Override
    public void onDraggablePOIItemMoved(MapView mapView, MapPOIItem mapPOIItem, MapPoint mapPoint) {

    }

    @Override
    public void onCurrentLocationUpdate(MapView mapView, MapPoint mapPoint, float v) {

//  /* */*   tempp = MapPoint.mapPointWithGeoCoord(mapPoint.getMapPointGeoCoord().latitude, mapPoint.getMapPointGeoCoord().longitude);
        /*if (first == true) {
            first = false;
            create_circle(circle1, tempp.getMapPointGeoCoord().latitude, tempp.getMapPointGeoCoord().longitude, 5);
        }
        //내 위치 중심으로 5미터 원 그리기
        delete_circle(circle1);
        circle1.setCenter(MapPoint.mapPointWithGeoCoord(tempp.getMapPointGeoCoord().latitude, tempp.getMapPointGeoCoord().longitude));
        mapView.addCircle(circle1);*/
    }

    @Override
    public void onCurrentLocationDeviceHeadingUpdate(MapView mapView, float v) {

    }

    @Override
    public void onCurrentLocationUpdateFailed(MapView mapView) {
    }

    @Override
    public void onCurrentLocationUpdateCancelled(MapView mapView) {


    }

    //동료 위치 주기적으로 바꾸는 쓰레드 시작
    private void Start_PT() {
        p_thread = new PartnerThread();
        p_thread.start();
    }

    ///동료 위치 주기적으로 바꾸는 쓰레드 종료
    private void Cancle_PT() {
        PT_running = false;
        try {
            p_thread.join();
        } catch (InterruptedException e) {
        }
    }

    private void Start_OT() {
        o_thread = new OutcheckThread();
        o_thread.start();
    }

    ///동료 위치 주기적으로 바꾸는 쓰레드 종료
    private void Cancle_OT() {
        OT_running = false;
        try {
            o_thread.join();
        } catch (InterruptedException e) {
        }
    }

    private void Start_CT() {
        c_thread = new CatchThread();
        c_thread.start();
    }

    ///동료 위치 주기적으로 바꾸는 쓰레드 종료
    private void Cancle_CT() {
        CT_running = false;
        try {
            c_thread.join();
        } catch (InterruptedException e) {
        }
    }

    private void Start_IT() {
        i_thread = new ItemThread();
        i_thread.start();
    }

    ///동료 위치 주기적으로 바꾸는 쓰레드 종료
    private void Cancle_IT() {
        IT_running = false;
        try {
            i_thread.join();
        } catch (InterruptedException e) {
        }
    }

    private void Start_ET() {
        e_thread = new EnermyThread();
        e_thread.start();
    }


    private void Cancle_ET() {
        ET_running = false;
        try {
            e_thread.join();
        } catch (InterruptedException e) {
        }
    }

    private void Start_HT(int index) {
       HideThread h_thread = new HideThread(index);
        h_thread.start();
    }


    private void Start_GT() {
        g_thread = new GameThread();
        g_thread.start();
    }
    private void Cancle_GT() {
        GT_running = false;
        try {
            g_thread.join();
        } catch (InterruptedException e) {
        }
    }
    //제한구역 벗어난거 check 하는 쓰레드
    class OutcheckThread extends Thread {
        int duration = 1000;

        public void run() {
            OT_running = true;
            while (OT_running) {
                synchronized (this) {
                    Ohandler.post(new Runnable() {
                        public void run() {
                            Check_out();
                        }
                    });

                    try {
                        Thread.sleep(duration);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }
    //게임 쓰레드
    class GameThread extends Thread {
        int duration = 1000;

        public void run() {
            GT_running = true;
            while (GT_running) {
                synchronized (this) {
                    Ghandler.post(new Runnable() {
                        public void run() {
                            game_time--;
                            int minute=game_time/60;
                            int second=game_time%60;

                            String now_time=String.valueOf(minute)+":"+String.valueOf(second);

                            game_tv.setText(now_time);
                            if(game_time==0)
                            {
                                Cancle_GT();

                                layout = inflater2.inflate(R.layout.tst_p_lose, //경찰 패배
                                        (ViewGroup) findViewById(R.id.custom_toast_container));

                                toast.setView(layout);
                                toast.show();

                                Game_Over();
                            }
                        }
                    });

                    try {
                        Thread.sleep(duration);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }
    //아이템 자동 획득 쓰레드
    class ItemThread extends Thread {
        int i = 10;
        int duration = 1000;

        public void run() {
            IT_running = true;
            while (IT_running) {
                synchronized (this) {
                    Ihandler.post(new Runnable() {
                        public void run() {

                            //아이템 획득하면 종료
                            if (item_get == true)
                                Cancle_IT();
                            else {
                                //1초마다 아이템 주변에 있는지 확인하고 있으면 획득
                                Item_check_and_get();
                            }
                        }
                    });

                    try {
                        Thread.sleep(duration);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

    //동료 위치 주기적으로 바꾸는 쓰레드
    class PartnerThread extends Thread {

        int duration = 1000;

        public void run() {
            PT_running = true;
            while (PT_running) {
                synchronized (this) {
                    Phandler.post(new Runnable() {
                        public void run() {

                            ///////////////////////////////////////서버에게 동료 위치 달라emit/////////////////////////////////////////

                            try {
                                partner_ob.put("index", my_index);
                                partner_ob.put("room_num", roomnum);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            RoomList.mSocket.emit("request_police_location", roomnum);

                            ///////////////////////////////////////서버에게 동료 위치 달라emit/////////////////////////////////////////

                        }
                    });
                    try {
                        Thread.sleep(duration);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

    //도둑 위치 갱신 쓰레드
    class EnermyThread extends Thread {
        int duration = 500;

        public void run() {
            ET_running = true;
            while (ET_running) {
                synchronized (this) {
                    Ehandler.post(new Runnable() {
                        public void run() {
                            ///////////////////////////////////////서버에게 도둑 위치 달라emit/////////////////////////////////////////

                            try {
                                enermy_ob.put("index", my_index);
                                enermy_ob.put("room_num", roomnum);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            RoomList.mSocket.emit("request_enermy_location", roomnum);

                            ///////////////////////////////////////서버에게 도둑 위치 달라emit/////////////////////////////////////////


                        }
                    });

                    try {
                        Thread.sleep(duration);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

    //자동검거쓰레드
    class CatchThread extends Thread {

        int duration = 3000;

        public void run() {
            CT_running = true;
            while (CT_running) {
                synchronized (this) {
                    Chandler.post(new Runnable() {
                        public void run() {

                            Check_arrest();

                        }
                    });

                    try {
                        Thread.sleep(duration);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }
    //아이템 사용한 도둑 숨겼다가 60초 후에 다시 표시하는 쓰레드
    class HideThread extends Thread implements Runnable {
        int t_index;
        boolean running;
        Handler Hhandler;
            public HideThread(int index) {
            // store parameter for later user
                this.t_index=index;
                this.running=true;
                this.Hhandler= new Handler();
        }

        int duration = 1000;
        int time=0;

        public void run() {

            while (running) {
                synchronized (this) {
                    Hhandler.post(new Runnable() {
                        public void run() {

                            time++;
                            if(time==60)
                            {
                                //thief[t_index].show();
                                thief[t_index].set_hide(false);
                                running=false;
                            }

                        }
                    });

                    try {
                        Thread.sleep(duration);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

    //자동검거 함수
    public void Check_arrest() {

        int i;
        double result = 0;
        for (i = 0; i < thief.length; i++) {
            result = Check_distance(tempp.getMapPointGeoCoord().latitude, tempp.getMapPointGeoCoord().longitude, thief[i].getLatitude(), thief[i].getLongitude());
            if (result < 5 && thief[i].death == false&&thief[i].out==false) {
                now_catch = i;
                thief[i].death = true;
                thief[i].hide();

                vibrator.vibrate(mainpattern, -1);  //진동 처리
                layout = inflater2.inflate(R.layout.tst_p_catch,
                        (ViewGroup) findViewById(R.id.custom_toast_container));
                text= (TextView) layout.findViewById(R.id.tst_id);
                String temp;
               temp=String.valueOf(text.getText());
                temp=String.valueOf(i)+temp;
                text.setText(temp);
                toast.setView(layout);
                toast.show();


                try {
                    catch_ob.put("index", now_catch);
                    catch_ob.put("room_num", roomnum);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                RoomList.mSocket.emit("catch_thief", catch_ob);


                ////////////////////////////////////잡은 도둑 인덱스 보내기//////////////////////////////////////////
            }
        }
    }

    //구역 벗어난거 확인하기 위한 함수
    public void Check_out() {

        Location locationA = new Location("point A");
        locationA.setLatitude(control_point.getMapPointGeoCoord().latitude);
        locationA.setLongitude(control_point.getMapPointGeoCoord().longitude);

        Location locationB = new Location("point B");
        locationB.setLatitude(tempp.getMapPointGeoCoord().latitude);
        locationB.setLongitude(tempp.getMapPointGeoCoord().longitude);
        if (locationB.distanceTo(locationA) > (double) range) {

            layout = inflater2.inflate(R.layout.tst_p_out,
                    (ViewGroup) findViewById(R.id.custom_toast_container));

            toast.setView(layout);
            toast.show();


            Game_out();



        }

    }

    //아이템 획득 함수-경찰 ver
    public void Item_check_and_get() {

        int i;
        double result;
        long[] pattern = {0, 700, 500, 700};

        for (i = 0; i < handcuff.length; i++) {
            result = Check_distance(tempp.getMapPointGeoCoord().latitude, tempp.getMapPointGeoCoord().longitude, handcuff[i].getLatitude(), handcuff[i].getLongitude());
            if (result < 5 && handcuff[i].used == false) {

                //획득한 아이템 획득 처리 및 맵에서 없애기
                handcuff[i].setUsed(true);
                handcuff[i].hide_item();
                now_item = i;
                item_get = true;   //아이템 획득처리
                b1.setBackgroundResource(R.drawable.police_button1);


                layout = inflater2.inflate(R.layout.tst_p_cuff,
                        (ViewGroup) findViewById(R.id.custom_toast_container));
                toast.setView(layout);
                toast.show();





                vibrator.vibrate(pattern, -1);  //진동 처리
                b1.setEnabled(true);
                ///////////////////////////////////////몇 번째 아이템 먹었는지 서버에게 전송/////////////////////////////////////////

                Start_CT();
                try {
                    item_ob.put("room_num", roomnum);
                    item_ob.put("item_index", now_item);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                RoomList.mSocket.emit("police_item_get", item_ob);


                ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

                //자동 검거 쓰레드 시작

            }
        }
    }


    //아이템 클래스
    public class Item {
        MapPOIItem Handcuff;
        boolean used;
        double latitude;
        double longitude;
        int index;
        String name;

        public Item(double latitude_, double longitude_, int index_,String name_) {
            Handcuff = new MapPOIItem();
            this.latitude = latitude_;
            this.longitude = longitude_;
            this.index = index_;
            this.name = name_;
            Handcuff.setItemName(this.name);
            Handcuff.setTag(index);
            Handcuff.setMapPoint(MapPoint.mapPointWithGeoCoord(this.latitude, this.longitude));
            Handcuff.setMarkerType(MapPOIItem.MarkerType.CustomImage);
            Handcuff.setCustomImageResourceId(R.drawable.handcuff);
            Handcuff.setCustomImageAutoscale(false);
            this.used = false;
        }

        public int getIndex() {
            return index;
        }

        public double getLongitude() {
            return longitude;
        }

        public double getLatitude() {
            return latitude;
        }

        public void show_item() {
            mapView.addPOIItem(this.Handcuff);
        }

        public void hide_item() {
            mapView.removePOIItem(this.Handcuff);
        }

        public void setUsed(boolean used) {
            this.used = used;
        }

    }

    //존 클래스
    public class Zone {
        MapCircle Zone_circle;
        double latitude;
        double longitude;
        boolean occupy;
        int radius;
        int index;

        public Zone(double latitude, double longitude, int radius_, int index, int r, int g, int b,int alpha) {
            this.index = index;
            this.radius = radius_;
            this.occupy = false;
            this.longitude = longitude;
            this.latitude = latitude;
            this.Zone_circle = new MapCircle(
                    MapPoint.mapPointWithGeoCoord(latitude, longitude), // center
                    radius_, // radius
                    Color.argb(128, r, g, b), // strokeColor
                    Color.argb(alpha, r, g, b) // fillColor
            );
            Zone_circle.setTag(index);


        }

        public void show_zone() {
            mapView.addCircle(this.Zone_circle);
        }

        public void remove_zone(){
            mapView.removeCircle(this.Zone_circle);
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public boolean isOccupy() {
            return occupy;
        }

        public int getRadius() {
            return radius;
        }

        public void Set_RGB(int r, int g, int b) {
            this.Zone_circle.setFillColor(Color.argb(128, r, g, b));
            this.Zone_circle.setStrokeColor(Color.argb(128, r, g, b));
        }
    }

    //거리 측정 함수
    public double Check_distance(double latitude1, double longitude1, double latitude2, double longitude2) {
        double distance;
        Location locationA = new Location("point A");
        locationA.setLatitude(latitude1);
        locationA.setLongitude(longitude1);

        Location locationB = new Location("point B");
        locationB.setLatitude(latitude2);
        locationB.setLongitude(longitude2);

        distance = locationB.distanceTo(locationA);

        return distance;
    }

    public class Partner {
        MapPOIItem police_marker;
        int index;
        boolean out;
        double latitude;
        double longitude;
        String nic;

        public Partner(String nic, double latitude, double longitude, int index) {
            police_marker=new MapPOIItem();
            this.nic = nic;
            this.longitude = longitude;
            this.latitude = latitude;
            this.index = index;
            this.out = false;
            police_marker.setMapPoint(MapPoint.mapPointWithGeoCoord(this.latitude, this.longitude));
            police_marker.setTag(index);
            police_marker.setItemName(nic);
            police_marker.setMarkerType(MapPOIItem.MarkerType.CustomImage);
            police_marker.setCustomImageResourceId(R.drawable.police);
            police_marker.setCustomImageAutoscale(false);

        }

        public void show() {
            mapView.addPOIItem(this.police_marker);
        }

        public void hide() {
            mapView.removePOIItem(this.police_marker);
        }

        public void change_marker() {
            this.police_marker.setMapPoint(MapPoint.mapPointWithGeoCoord(this.latitude, this.longitude));
        }

        public void change_location(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

    }

    public class Enermy {

        MapPOIItem thief_marker;
        double latitude;
        double longitude;
        boolean death;
        boolean out;
        boolean hide;
        int index;

        public Enermy(double latitude, double longitude, int index) {
            thief_marker=new MapPOIItem();
            this.latitude = latitude;
            this.longitude = longitude;
            this.death = false;
            this.out = false;
            this.index=index;
            this.hide=false;


            thief_marker.setMapPoint(MapPoint.mapPointWithGeoCoord(this.latitude,this.longitude));
            thief_marker.setTag(this.index);
            thief_marker.setItemName("도둑");
            thief_marker.setMarkerType(MapPOIItem.MarkerType.CustomImage);
            thief_marker.setCustomImageResourceId(R.drawable.thief);
            thief_marker.setCustomImageAutoscale(false);
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public boolean isDeath() {
            return death;
        }

        public void show() {
            mapView.addPOIItem(this.thief_marker);
        }

        public void hide() {
            mapView.removePOIItem(thief_marker);
            Log.i("hide","hide");

        }

        public void change_marker() {
            thief_marker.setMapPoint(MapPoint.mapPointWithGeoCoord(this.latitude, this.longitude));
        }

        public void change_location(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
        public void set_hide(boolean hide_)
        {
            this.hide=hide_;
        }

        public int getIndex() {
            return this.index;
        }
    }


    //동료 표시
    public void Show_partner() {
        for (int i = 0; i < partner_num; i++) {
            police[i].show();
        }
    }

    //점령존 표시
    public void Show_zone() {
        for (int i = 0; i < zone_num; i++) {
            zone[i].show_zone();
        }
    }

    //아이템 표시
    public void Show_item() {
        for (int i = 0; i < item_num; i++) {
            handcuff[i].show_item();
        }
    }

    //제한구역 및 시작존 표시
    public void Show_Area() {
        Area.show_zone();
        Prison.show_zone();
    }

    //도둑 표시
    public void Show_Enermy() {
        for (int i = 0; i < enermy_num; i++) {
            thief[i].show();
        }
    }

    //동료 마커 이동
    public void Renew_partner_location() {
        for (int i = 0; i < partner_num; i++) {
            police[i].change_marker();
        }
    }

    //도둑 마커 이동
    public void Renew_enermy_location() {
        for (int i = 0; i < enermy_num; i++) {
            if (thief[i].death == false&&thief[i].hide==false)
                thief[i].change_marker();
        }
    }


    //범위 밖으로 나갔을 때 처리 함수
    public void Game_out() {
        out=true;
        ///////////////////////////////////////누가 나갔는지 서버에게 전송/////////////////////////////////////////
        try {
            out_ob.put("room_num", roomnum);
            out_ob.put("index", my_index);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);
        mapView.setShowCurrentLocationMarker(false);
        cancel();
        Cancle_PT();

        if(item_get==true)
        {
            Cancle_CT();
        }
        Cancle_ET();

        Cancle_OT();

        if(item_get==false)
        {
            Cancle_IT();
        }

        RoomList.mSocket.emit("police_out", out_ob);
        /////////////////////////////////////////////////////////////////////////////////////////////////////////
        //범위 밖으로 나갔을 때 처리(클라이언트 안에서 나갔다고 표시후 관전모드)
    }

    //도둑이 존 점령시
    public void Zone_occupy(int index) {

        zone[index].occupy = true;
        zone[index].Set_RGB(255, 0, 0);
        zone[index].remove_zone();
        zone[index].show_zone();
    }

    //도둑이 존 모두 점령했을때
    public void Zone_all_clear() {


        for(int i=0;i<zone_num;i++)
        {
            zone[i].occupy=false;
            zone[i].Set_RGB(0,0,255);
            zone[i].remove_zone();
            zone[i].show_zone();
        }

    }

    //도둑이 아이템 사용햇을때
    public void Hide_thief(int index) {
        thief[index].set_hide(true);
        Start_HT(index);

    }

    public void Initialize() {
        this.range = data.getRange();
        this.roomnum = data.getRoom_num();
        this.partner_num = data.getPartner_num();
        this.enermy_num = data.getEnermy_num();
        this.item_num = data.getItem_num();
        this.zone_num = data.getZone_num();
        this.control_point = MapPoint.mapPointWithGeoCoord(data.getStart().getLatitude(), data.getStart().getLongitude());
        this.tempp = MapPoint.mapPointWithGeoCoord(data.getMine().getLatitude(), data.getMine().getLongitude());
        this.my_index = data.getMy_index();
        this.game_time=data.getGame_time();
        game_time=game_time*60;
        int minute=game_time/60;
        int second=game_time%60;
        Log.i("range",String.valueOf(range));
        Log.i("roomnum",String.valueOf(roomnum));
        Log.i("partnernum",String.valueOf(partner_num));
        Log.i("enermy_num",String.valueOf(enermy_num));
        Log.i("item_num",String.valueOf(item_num));
        Log.i("zone_num",String.valueOf(zone_num));
        Log.i("control_point_la",String.valueOf(control_point.getMapPointGeoCoord().latitude));
        Log.i("control_point_lo",String.valueOf(control_point.getMapPointGeoCoord().longitude));
        Log.i("my_index",String.valueOf(my_index));

        String now_time=String.valueOf(minute)+":"+String.valueOf(second);

        game_tv.setText(now_time);
        thief = new Enermy[enermy_num];
        handcuff = new Item[item_num];
        zone = new Zone[zone_num];
        police = new Partner[partner_num];
        this.zone_range = 10;
        for (int i = 0; i < item_num; i++) {
            handcuff[i] = new Item(data.getItem().get(i).getLatitude(), data.getItem().get(i).getLongitude(), i,"");
            Log.i("handcuff_la",String.valueOf(handcuff[i].latitude));
            Log.i("handcuff_lo",String.valueOf(handcuff[i].longitude));
        }
        for (int i = 0; i < zone_num; i++) {
            zone[i] = new Zone(data.getZone().get(i).getLatitude(), data.getZone().get(i).getLongitude(), zone_range, i, 0, 0, 255,128);
            Log.i("zone_la",String.valueOf(zone[i].latitude));
            Log.i("zone_lo",String.valueOf(zone[i].longitude));
        }
        for (int i = 0; i < partner_num; i++) {
            police[i] = new Partner(data.getPartner_nic().get(i), data.getPartner().get(i).getLatitude(), data.getPartner().get(i).getLongitude(), i);
            Log.i("police_la",String.valueOf(police[i].latitude));
            Log.i("police_lo",String.valueOf(police[i].longitude));
        }

        for (int i = 0; i < enermy_num; i++) {
            thief[i] = new Enermy(data.getEnermy().get(i).getLatitude(), data.getEnermy().get(i).getLongitude(), i);
            Log.i("thief_la",String.valueOf(thief[i].latitude));
            Log.i("thief_la",String.valueOf(thief[i].longitude));
        }
        this.Area=new Zone(control_point.getMapPointGeoCoord().latitude,control_point.getMapPointGeoCoord().longitude,range,100,255,0,0,0);
        this.Prison=new Zone(control_point.getMapPointGeoCoord().latitude,control_point.getMapPointGeoCoord().longitude,10,101,0,0,0,128);
        Log.i("start_la",String.valueOf(control_point.getMapPointGeoCoord().latitude));
        Log.i("start_lo",String.valueOf(control_point.getMapPointGeoCoord().longitude));

        initialized=true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 10:
                check_location_gps();
                break;
            case 12:
                check_location_network();
                break;
            default:
                break;
        }
    }

    //gps로 위치 측청
    void check_location_gps() {
        // first check for permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET}
                        , 10);
            }
            return;
        }

        //loco,ationManager.requestSingleUpdate("gps", listener, null);
        locationManager.requestLocationUpdates("gps", 100, 0, listener);
    }

    //network로 위치 측정
    void check_location_network() {
        // first check for permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET}
                        , 12);
            }
            return;
        }
        //locationManager.requestSingleUpdate("network", listener, null);
        locationManager.requestLocationUpdates("newtork", 100, 0, listener);
    }

    //위치 측정 취소
    void cancel() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.removeUpdates(listener);
    }
    public void Game_Over(){
        game_over=true;
        vibrator.vibrate(mainpattern,-1);

        if(my_index==0)
        {
            RoomList.mSocket.emit("game_clear",roomnum);
        }

        if(out==false)
        {

            mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);
            mapView.setShowCurrentLocationMarker(false);
            cancel();
            if(item_get==true)
            {
                Cancle_CT();
            }

           Cancle_ET();
           Cancle_OT();
            Cancle_PT();

            if(item_get==false)
            {
                Cancle_IT();
            }
            //승리 패배 표시후 액티비티 전환
        }

        RoomList.mSocket.off("request_police_location", request_police_location);
        RoomList.mSocket.off("request_enermy_location", request_enermy_location);
        RoomList.mSocket.off("catch_thief", catch_thief);
        RoomList.mSocket.off("alldeath", alldeath);
        RoomList.mSocket.off("police_item_get", police_item_get);
        RoomList.mSocket.off("police_out", police_out);
        RoomList.mSocket.off("zone_clear", zone_clear);
        RoomList.mSocket.off("allclear", allclear);
        RoomList.mSocket.off("thief_out", thief_out);
        RoomList.mSocket.off("thief_item_use", thief_item_use);
        RoomList.mSocket.off("thief_escape", thief_escape);

        InGameReady.activity.finish();
        finish();

    }
    public void countDownTimer(){
        countDownTimer = new CountDownTimer(MILLISINFUTURE, COUNT_DOWN_INTERVAL) {
            public void onTick(long millisUntilFinished) {
                b2.setText(String.valueOf(count));
                count --;
            }

            public void onFinish() {
                b2.setEnabled(true);
                b2.setBackgroundResource(R.drawable.police_button2);
                b2.setText(String.valueOf(""));
            }
        };
    }
    //서버로부터 갱신된 위치 받았을 때 경찰 위치바꾸고 마커 다시표시
    private Emitter.Listener request_police_location = new Emitter.Listener() {

        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject origin_data = (JSONObject) args[0];
                        JSONArray data=origin_data.getJSONArray("data");

                        JSONObject p1 = new JSONObject();
                        for (int i = 1; i < data.length(); i++) {
                            p1 = data.getJSONObject(i);
                            police[i - 1].change_location(p1.getDouble("latitude"), p1.getDouble("longitude"));
                           Log.i("p1 la",String.valueOf( police[i-1].latitude));
                            Log.i("p1 lo",String.valueOf( police[i-1].longitude));

                        }

                        Renew_partner_location();

                    } catch (JSONException | NullPointerException e) {
                        Toast.makeText(getApplicationContext(),
                                "Error" + e.toString(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    };
    //서버로부터 도둑 위치 받아와 갱신
    private Emitter.Listener request_enermy_location = new Emitter.Listener() {

        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject origin_data = (JSONObject) args[0];
                        JSONArray data=origin_data.getJSONArray("data");


                        JSONObject p1 = new JSONObject();
                        for (int i = 1; i < data.length(); i++) {
                            p1 = data.getJSONObject(i);
                            thief[i - 1].change_location(p1.getDouble("latitude"), p1.getDouble("longitude"));
                        }


                    } catch (JSONException | NullPointerException e) {
                        Toast.makeText(getApplicationContext(),
                                "Error" + e.toString(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    };
    //서버로부터 검거된 도둑 처리
    private Emitter.Listener catch_thief = new Emitter.Listener() {

        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    JSONObject data = (JSONObject) args[0];
                    try {
                        int d_index = data.getInt("data");
                        if(thief[d_index].death==false)
                        {
                            vibrator.vibrate(mainpattern,-1);

                            thief[d_index].death = true;
                            thief[d_index].hide();

                            layout = inflater2.inflate(R.layout.tst_p_catch,
                                    (ViewGroup) findViewById(R.id.custom_toast_container));
                            text= (TextView) layout.findViewById(R.id.tst_id);
                            String temp;
                            temp=String.valueOf(text.getText());
                            temp=String.valueOf(d_index+1)+temp;
                            text.setText(temp);
                            toast.setView(layout);
                            toast.show();



                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                }
            });
        }
    };
    //서버로부터 먹힌 아이템 처리
    private Emitter.Listener police_item_get = new Emitter.Listener() {

        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {


                    JSONObject data = (JSONObject) args[0];
                    try {

                        int d_index = data.getInt("data");
                        vibrator.vibrate(mainpattern,-1);

                        handcuff[d_index].used = true;
                        handcuff[d_index].hide_item();
                    } catch (JSONException e) {

                        e.printStackTrace();
                    }


                }
            });
        }
    };
    //서버로부터 점령된 점령존 처리
    private Emitter.Listener zone_clear = new Emitter.Listener() {

        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    JSONObject data = (JSONObject) args[0];
                    try {
                        vibrator.vibrate(mainpattern,-1);
                        int d_index = data.getInt("data");


                        layout = inflater2.inflate(R.layout.tst_p_zone,
                                (ViewGroup) findViewById(R.id.custom_toast_container));
                        text= (TextView) layout.findViewById(R.id.tst_id);
                        String temp;
                        temp=String.valueOf(text.getText());
                        temp=String.valueOf(d_index+1)+temp;
                        text.setText(temp);
                        toast.setView(layout);
                        toast.show();
                        Zone_occupy(d_index);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                }
            });
        }
    };
    //점령존이 모두점령 된경우
    private Emitter.Listener allclear = new Emitter.Listener() {

        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {


                    layout = inflater2.inflate(R.layout.tst_p_allzone,
                            (ViewGroup) findViewById(R.id.custom_toast_container));

                    toast.setView(layout);
                    toast.show();

                    Zone_all_clear();
                    vibrator.vibrate(mainpattern,-1);

                }
            });
        }
    };


    //서버로부터 나간 경찰동료 처리
    private Emitter.Listener police_out = new Emitter.Listener() {

        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    JSONObject data = (JSONObject) args[0];
                    try {
                        int d_index = data.getInt("data");
                        if (d_index != my_index) {

                            if (d_index > my_index) {
                                d_index--;
                            }
                            police[d_index].out = true;
                            police[d_index].hide();

                            layout = inflater2.inflate(R.layout.tst_p_out,
                                    (ViewGroup) findViewById(R.id.custom_toast_container));
                            text= (TextView) layout.findViewById(R.id.tst_id);
                            String temp;
                            temp=String.valueOf(text.getText());
                            temp=police[d_index].nic+" "+temp;
                            text.setText(temp);
                            toast.setView(layout);
                            toast.show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                }
            });
        }
    };

    //도둑이 모두 잡혀 게임이 끝난 경우
    private Emitter.Listener alldeath = new Emitter.Listener() {

        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    vibrator.vibrate(mainpattern,-1);
                    Cancle_GT();
                    layout = inflater2.inflate(R.layout.tst_p_win,
                            (ViewGroup) findViewById(R.id.custom_toast_container));
                    toast.setView(layout);
                    toast.show();   //경찰 승리

                    //게임 종료 처리
                    //게임 종료 처리
                    Game_Over();

                }
            });
        }
    };

    //서버로부터 나간 도둑처리
    private Emitter.Listener thief_out = new Emitter.Listener() {

        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    JSONObject data = (JSONObject) args[0];
                    try {
                        int d_index = data.getInt("data");

                        layout = inflater2.inflate(R.layout.tst_p_out,
                                (ViewGroup) findViewById(R.id.custom_toast_container));
                        text= (TextView) layout.findViewById(R.id.tst_id);
                        String temp;
                        temp=String.valueOf(d_index+1)+"번 째 도둑 제한 구역 아웃";
                        text.setText(temp);
                        toast.setView(layout);
                        toast.show();

                        if (d_index != my_index) {
                            if (d_index > my_index) {
                                d_index--;
                            }
                            thief[d_index].out = true;
                            thief[d_index].hide();
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                }
            });
        }
    };

    //서버로부터 아이템 사용한 도둑 처리
    private Emitter.Listener thief_item_use = new Emitter.Listener() {

        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    JSONObject data = (JSONObject) args[0];

                    try {
                        int d_index = data.getInt("data");
                        Hide_thief(d_index);


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                }
            });
        }
    };

    private Emitter.Listener thief_escape = new Emitter.Listener() {

        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    JSONObject data = (JSONObject) args[0];

                    try {
                        int d_index = data.getInt("data");

                            if (thief[d_index].out == false && thief[d_index].death == true) {
                                thief[d_index].death = false;
                                thief[d_index].show();
                            }



                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                }
            });
        }
    };


    private final class FlowAnimationListener implements Animation.AnimationListener{
        public void onAnimationEnd(Animation animation){
            if(visible) {
                hide_linear.setVisibility(View.INVISIBLE);
                Log.i("flow","flow");
                visible = false;
            }else {
                visible = true;
            }
        }

        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }
/*
게임 종료 되었을때의 경우 코드 넣어야댐
쓰레드 종료

*/

}

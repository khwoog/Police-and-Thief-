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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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

public class Thief extends AppCompatActivity implements MapView.MapViewEventListener, MapView.POIItemEventListener, MapView.CurrentLocationEventListener {
    int partner_num; //도둑 수
    int item_num;   //아이템 수(모든 도둑 수)
    int zone_num;   //점령존 수(도둑 수)
    int range;       //제한구역범위
    int roomnum;     //방 번호
    int temp_zone_index;    //현재 위치 범위 안에 든 점령존의 index
    int now_item;    //획득한 아이템의 인덱스
    int zone_range;  //점령존 범위
    int my_index;      //방 에서의 내 인덱스
    int game_time;     //게임 시간

    Data_Thief data;     //ingameready 에서 받아온 게임정보(도둑용)
    Intent intent;

    private boolean isStart;//점령존 버튼 이벤트 관련 변수
    boolean item_end;//남은 아이템이 있는지 판별하기 위한 변수
    boolean game_death;   //죽었는지 안죽었는지 변수
    boolean first = true;
    boolean change=false;
    boolean item_get=false;
    boolean out=false;
    boolean initialized=false;
    boolean game_over=false;
    boolean visible=false;

    //이동금지
    Animation flow;
    Animation unflow;




    //쓰레드를 위한 핸들러
    Handler Phandler = new Handler();
    Handler Ohandler = new Handler();
    Handler Ihandler = new Handler();
    Handler zone_handler = new Handler();   //점령존 점령에 필요한 핸들러
    Handler Ghandler=new Handler();

    //쓰레드 종료하기 위해 쓰는변수
    private boolean PT_running;
    private boolean OT_running;
    private boolean IT_running;
    private boolean GT_running;

    //쓰레드
    PartnerThread p_thread; //동료 위치 변경 쓰레드
    OutcheckThread o_thread;//범위 out check 쓰레드
    ItemThread i_thread;    //아이템 감지 쓰레드
    GameThread g_thread;    //게임 시간 쓰레드


    Item cape[];   //은신 아이템 객체
    Partner thief[];  //동료객체
    Zone zone[];        //점령존 객체
    Zone Area;          //제한구역 존
    Zone Prison;        //시작존(감옥존)

    MapPoint tempp ;  //현재 내위치 표현하기위한 변수
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

    ProgressBar zone_bar;   //점령존 프로그레스바
    int zone_progressBarValue = 0;   //점령존 프로그레스바 상태값
    long[] zone_pattern = {0, 500, 250, 500};
    long[] mainpattern={0,1000,500,1000};

    Location base_point;

    JSONObject out_ob = new JSONObject();
    JSONObject item_ob = new JSONObject();
    JSONObject partner_ob = new JSONObject();
    JSONObject zone_ob = new JSONObject();
    JSONObject mylocation_ob = new JSONObject();
    JSONObject cape_ob=new JSONObject();
    JSONObject escape_ob=new JSONObject();

    private static final int MILLISINFUTURE = 60*1000;
    private static final int COUNT_DOWN_INTERVAL = 1000;
    private int count = 59;
    private CountDownTimer countDownTimer;



    private LocationManager locationManager;
    private LocationListener listener;

    TextView game_tv;

    LayoutInflater inflater2;
    View layout;
    Toast toast;
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



        base_point=new Location("Control_Point");
        //이전 액티비티에서 받아온 초기화 관련된 변수
        intent=getIntent();
        data = (Data_Thief) intent.getSerializableExtra("OBJECT");



        //ui 겹치기 코드
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
        hide_linear.setVisibility(View.INVISIBLE);
        death_view=(ImageView)findViewById(R.id.death_iv);
        death_view.setImageResource(R.drawable.thief_dead);



        death_l1=(LinearLayout)findViewById(R.id.death_ll1);
        death_l2=(LinearLayout)findViewById(R.id.death_ll2);


        item_get = false;
        item_end=false;
        game_death=false;


        b1 = (Button) findViewById(R.id.button1);  //은신 아이템 사용
        b2 = (Button) findViewById(R.id.button2);  //점령 하기
        b1.setEnabled(false);
        b1.setBackgroundResource(R.drawable.thief_button1x);
        b2.setBackgroundResource(R.drawable.thief_button2);
        b1.setTextColor(Color.rgb(255,0,0));




        zone_bar = (ProgressBar) findViewById(R.id.pgbar);
        zone_bar.setVisibility(View.INVISIBLE);
        // 점령존 처리를 위한 핸들러
        zone_handler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (isStart) {
                    zone_progressBarValue++;


                    if (zone_progressBarValue == 100) {
                        zone_progressBarValue = 0;
                        zone_bar.setVisibility(View.INVISIBLE);
                        zone_handler.removeCallbacksAndMessages(null);
                        if (Check_close_zone() == true) {    //위치가 점령존 범위 안에 들떄
                            if (zone[temp_zone_index].isOccupy() == true)  //이미 점령된 존이면
                            {

                                layout = inflater2.inflate(R.layout.tst_t_alreadyzone,
                                        (ViewGroup) findViewById(R.id.custom_toast_container));
                                toast.setView(layout);
                                toast.show();

                            } else {  //처음 점령하는 존일때 처리(점령햇을때)
                                zone[temp_zone_index].occupy = true;
                                vibrator.vibrate(zone_pattern, -1);


                                layout = inflater2.inflate(R.layout.tst_t_zone,
                                        (ViewGroup) findViewById(R.id.custom_toast_container));
                                text= (TextView) layout.findViewById(R.id.tst_id);
                                String temp;
                                temp=String.valueOf(text.getText());
                                temp=String.valueOf(temp_zone_index+1)+temp;
                                text.setText(temp);
                                toast.setView(layout);
                                toast.show();

                                zone[temp_zone_index].Set_RGB(255, 0, 0);
                                zone[temp_zone_index].remove_zone();
                                zone[temp_zone_index].show_zone();

                                //////////////////////서버 쪽에 점령된 존 인덱스 전송////////////////////

                                try {
                                    zone_ob.put("index",temp_zone_index);
                                    zone_ob.put("room_num",roomnum);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                RoomList.mSocket.emit("zone_clear",zone_ob);
                                //////////////////////서버 쪽에 점령된 존 인덱스 전송////////////////////
                            }

                        } else {   //버튼은 끝까지 눌렀는데 범위에서 벗어난상태일때

                            layout = inflater2.inflate(R.layout.tst_t_outzone,
                                    (ViewGroup) findViewById(R.id.custom_toast_container));
                            toast.setView(layout);
                            toast.show();

                        }
                    }
                }
                zone_bar.setProgress(zone_progressBarValue);

                zone_handler.sendEmptyMessageDelayed(0, 30);
            }
        };

//////////////////////////////////////////////

        //은신 아이템 사용시
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Hide_thief();
                //서버로 아이템 쓴 도둑 누군지 보냄
                count = 59;

                b1.setEnabled(false);
                b1.setBackgroundResource(R.drawable.thief_button1x);

                // b2.setBackgroundResource(R.drawable.thiefoff);
                countDownTimer();
                countDownTimer.start();

            }
        });
        //점령하기 버튼 이벤트 리스너 등록
        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        ///꾹 누를때
        b2.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (Check_close_zone() == true) {   //점령존 주변일때만 버튼 프로그레스바 생성
                    zone_bar.setVisibility(View.VISIBLE);
                    isStart = true;
                    zone_handler.sendEmptyMessage(0);
                }
                else if(game_death==true){


                    layout = inflater2.inflate(R.layout.tst_t_outzone,
                            (ViewGroup) findViewById(R.id.custom_toast_container));
                    text= (TextView) layout.findViewById(R.id.tst_id);
                    String temp="검거 되었기 때문에 점령불가";
                    text.setText(temp);

                    toast.setView(layout);
                    toast.show();

                }
                else {   //점령존 주변이아닐떄


                    layout = inflater2.inflate(R.layout.tst_t_outzone,
                            (ViewGroup) findViewById(R.id.custom_toast_container));
                    text= (TextView) layout.findViewById(R.id.tst_id);
                    String temp="점령존 주변이 아님";

                    text.setText(temp);
                    toast.setView(layout);
                    toast.show();

                }


                return false;
            }
        });
        //손가락 뗄때
        b2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                int action = event.getAction();
                if (action == MotionEvent.ACTION_UP) {
                    isStart = false;
                    zone_progressBarValue = 0;
                    zone_bar.setVisibility(View.INVISIBLE);
                    zone_handler.removeCallbacksAndMessages(null);

                }
                return false;
            }
        });








        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        listener = new LocationListener() {
            double myspeed_double;
            @Override
            public void onLocationChanged(Location location) {
                myspeed_double = location.getSpeed();
                if(myspeed_double>=1.0)
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

                }
                tempp = MapPoint.mapPointWithGeoCoord(location.getLatitude(),location.getLongitude());
               /////////////////////////////////////내 위치 보내주기 서버로/////////////////////////////////////////////
                try {
                    mylocation_ob.put("latitude",tempp.getMapPointGeoCoord().latitude);
                    mylocation_ob.put("room_num",roomnum);
                    mylocation_ob.put("index",my_index);
                    mylocation_ob.put("longitude",tempp.getMapPointGeoCoord().longitude);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                RoomList.mSocket.emit("my_location",mylocation_ob);
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
                if(change==false)
                {
                    Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(i);
                    change=true;
                }


            }
        };

        mapView = new MapView(this);
        mapView.setDaumMapApiKey("4e6090d28d2a2cc01fea4130929cf630");
        mapView.setMapType(MapView.MapType.Standard);
        ViewGroup mapViewContainer = (ViewGroup) findViewById(R.id.map_view);
        mapViewContainer.addView(mapView);
        mapView.setMapViewEventListener(this);
        mapView.setPOIItemEventListener(this);
        mapView.setCurrentLocationEventListener(this);

        vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);




        RoomList.mSocket.on("thief_death", thief_death);
        RoomList.mSocket.on("alldeath", alldeath);
        RoomList.mSocket.on("zone_clear", zone_clear);
        RoomList.mSocket.on("allclear", allclear);
        RoomList.mSocket.on("thief_item_get", thief_item_get);
        RoomList.mSocket.on("request_thief_location", request_thief_location);
        RoomList.mSocket.on("thief_out",  thief_out);
        RoomList.mSocket.on("thief_escape",  thief_escape);



    }

    @Override
    protected void onDestroy() {
        //종료 될때 트래킹 중지

        /*   mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);
        mapView.setShowCurrentLocationMarker(false);*/
        super.onDestroy();
        if(game_over==false)
        {
               Game_Over();
            RoomList.mSocket.off("thief_death", thief_death);
            RoomList.mSocket.off("alldeath", alldeath);
            RoomList.mSocket.off("zone_clear", zone_clear);
            RoomList.mSocket.off("allclear", allclear);
            RoomList.mSocket.off("thief_item_get", thief_item_get);
            RoomList.mSocket.off("request_thief_location", request_thief_location);
            RoomList.mSocket.off("thief_out",  thief_out);
            RoomList.mSocket.off("thief_escape",  thief_escape);

        }

        try{
            countDownTimer.cancel();
        } catch (Exception e) {}
        countDownTimer=null;

    }

    //맵이 처음 시작될때
    @Override
    public void onMapViewInitialized(MapView mapView) {
        Initialize();//intent로 정보 넘겨 받아 온 것 초기화
        while(!initialized)
        {
            if(initialized)
                break;
        }
        Start_GT();
        MapPOIItem.ImageOffset trackingImageAnchorPointOffset = new MapPOIItem.ImageOffset(16, 16); // 좌하단(0,0) 기준 앵커포인트 오프셋
        //MapPOIItem.ImageOffset directionImageAnchorPointOffset = new MapPOIItem.ImageOffset(65, 65);
        //MapPOIItem.ImageOffset offImageAnchorPointOffset = new MapPOIItem.ImageOffset(15, 15);
        mapView.setCustomCurrentLocationMarkerTrackingImage(R.drawable.custom_map_present_tracking, trackingImageAnchorPointOffset);
        //mapView.setCustomCurrentLocationMarkerDirectionImage(R.drawable.custom_map_present_direction, directionImageAnchorPointOffset);
        //mapView.setCustomCurrentLocationMarkerImage(R.drawable.custom_map_present, offImageAnchorPointOffset);
        //위치 정보 받아오기 시작
        check_location_gps();
        //트래킹 시작
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithHeadingWithoutMapMoving);
        mapView.setMapCenterPointAndZoomLevel(MapPoint.mapPointWithGeoCoord(tempp.getMapPointGeoCoord().latitude, tempp.getMapPointGeoCoord().longitude), 1, false);       //처음 시작될때 내 위치로 맵 이동

        //아이템 좌표 존 좌표 초기시작 위치 동료 위치 다 알고있다고 했을때

        //동료,도둑,점령존,제한구역,시작존,아이템,표시
        Show_Area();
        Show_partner();
        Show_zone();
        //쓰레드 시작
        Start_PT();
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

        if(out!=true)
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
        int tag;
        String name;
        double result;
        tag = mapPOIItem.getTag();

        name = mapPOIItem.getItemName();


        result = Check_distance(mapPOIItem.getMapPoint().getMapPointGeoCoord().latitude, mapPOIItem.getMapPoint().getMapPointGeoCoord().longitude, tempp.getMapPointGeoCoord().latitude, tempp.getMapPointGeoCoord().longitude);

        if (name == "은신망토" && result < 5&&item_get==false&&game_death==false) {

            b1.setEnabled(true);
            b1.setBackgroundResource(R.drawable.thief_button1);
            vibrator.vibrate(mainpattern,-1);
            item_get=true;
            cape[tag].setUsed(true);    //획득 처리
            cape[tag].hide_item(); //맵에서 지우기


            now_item = tag; //현재 획득한 아이템 인덱스 등록

            layout = inflater2.inflate(R.layout.tst_t_hide,
                    (ViewGroup) findViewById(R.id.custom_toast_container));
            toast.setView(layout);
            toast.show();

///////////////////////////////////////몇 번째 아이템 먹었는지 서버에게 전송/////////////////////////////////////////

            try {
                item_ob.put("room_num",roomnum);
                item_ob.put("item_index",now_item);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            RoomList.mSocket.emit("thief_item_get",item_ob);


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            ////////////////////////////버튼 활성화 코드 넣어야된다~////////////////////////
            //아이템 범위 이탈
        } else if (name == "은신망토" && result >= 5&&item_get==false&&game_death==false) {


            layout = inflater2.inflate(R.layout.tst_t_dthide,
                    (ViewGroup) findViewById(R.id.custom_toast_container));
            toast.setView(layout);
            toast.show();

        }
        else if(item_get==true&&name == "은신망토"&&game_death==false){     //이미 아이템 획득

            layout = inflater2.inflate(R.layout.tst_t_alreadyhide,
                    (ViewGroup) findViewById(R.id.custom_toast_container));
            toast.setView(layout);
            toast.show();
        }
        else if(game_death==true&&name=="은신망토") //검거되었기때문에 획득불가
        {
            layout = inflater2.inflate(R.layout.tst_t_dthide,
                    (ViewGroup) findViewById(R.id.custom_toast_container));
            text= (TextView) layout.findViewById(R.id.tst_id);
            String temp="검거 되었기 때문에 획득 불가";
            text.setText(temp);
            toast.setView(layout);
            toast.show();
        }
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

      /*  //tempp = MapPoint.mapPointWithGeoCoord(mapPoint.getMapPointGeoCoord().latitude, mapPoint.getMapPointGeoCoord().longitude);
        if (first == true) {
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

    private void Cancle_OT() {
        OT_running = false;
        try {
            o_thread.join();
        } catch (InterruptedException e) {
        }
    }

    private void Start_IT() {
        i_thread = new ItemThread();
        i_thread.start();
    }
    private void Cancle_IT() {
        IT_running = false;
        try {
            i_thread.join();
        } catch (InterruptedException e) {
        }
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

                                //도둑 승리
                                layout = inflater2.inflate(R.layout.tst_t_win,
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

                            //아이템 모두 획득하면 종료
                            if (Item_over() == true) {
                                Cancle_IT();
                                item_end = true;
                            } else {
                                //1초마다 아이템 주변에 있는지 확인하고 있으면 표시
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
                                partner_ob.put("index",my_index);
                                partner_ob.put("room_num",roomnum);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            RoomList.mSocket.emit("request_thief_location",roomnum);

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

    //구역 벗어난거 확인하기 위한 함수
    public void Check_out() {

        Location locationA = new Location("point A");
        locationA.setLatitude(control_point.getMapPointGeoCoord().latitude);
        locationA.setLongitude(control_point.getMapPointGeoCoord().longitude);

        Location locationB = new Location("point B");
        locationB.setLatitude(tempp.getMapPointGeoCoord().latitude);
        locationB.setLongitude(tempp.getMapPointGeoCoord().longitude);
        if (locationB.distanceTo(locationA) > (double) range) {
            layout = inflater2.inflate(R.layout.tst_t_out,
                    (ViewGroup) findViewById(R.id.custom_toast_container));
            toast.setView(layout);
            toast.show();

            Game_out();

            death_l1.setVisibility(LinearLayout.INVISIBLE);
            death_l2.setVisibility(LinearLayout.VISIBLE);
            death_view.setVisibility(View.VISIBLE);
        }

    }

    //아이템 획득 함수-도둑 ver
    public void Item_check_and_get() {

        int i;
        double result;
        long[] pattern = {0, 1000, 1000, 2000};


        for (i = 0; i < cape.length; i++) {
            result = Check_distance(tempp.getMapPointGeoCoord().latitude, tempp.getMapPointGeoCoord().longitude, cape[i].getLatitude(), cape[i].getLongitude());
            if (result < 30 && cape[i].used == false) {
                //처음 발견한거라면
                if (cape[i].first == true) {
                    cape[i].first = false;
                    cape[i].show_item();
                    layout = inflater2.inflate(R.layout.tst_t_findhide,
                            (ViewGroup) findViewById(R.id.custom_toast_container));

                    toast.setView(layout);
                    toast.show();

                    vibrator.vibrate(pattern, -1);
                }
                //진동 코드

            }
        }

    }

    //근처에 존이 있는 지 판단하는 함수
    public boolean Check_close_zone() {

        int i;
        boolean bool = false;

        for (i = 0; i < zone.length; i++) {
            if (Check_distance(tempp.getMapPointGeoCoord().latitude, tempp.getMapPointGeoCoord().longitude, zone[i].getLatitude(), zone[i].getLongitude()) <= zone_range) {
                temp_zone_index = i;
                bool = true;
                break;
            }

        }
        return bool;
    }

    public boolean Item_over() {
        boolean over = true;
        int i;
        for (i = 0; i < cape.length; i++) {
            if (cape[i].used == false)
                over = false;
        }
        return over;
    }

    public void delete_circle(MapCircle C) {
        mapView.removeCircle(C);
    }

    public void create_circle(MapCircle c, double latitude, double longitude, int radius) {
        circle1 = new MapCircle(
                MapPoint.mapPointWithGeoCoord(latitude, longitude), // center
                radius, // radius
                Color.argb(128, 255, 0, 0), // strokeColor
                Color.argb(128, 0, 255, 0) // fillColor
        );
    }

    //아이템 클래스
    public class Item {
        MapPOIItem Cape;
        boolean used;   //획득 유무
        double latitude;
        double longitude;
        boolean first; //도둑 아이템에서 처음 발견한 건지 판단 위해쓰일변수
        int index;
        String name;

        public Item(double latitude_, double longitude_, int index_,String name_) {
            Cape = new MapPOIItem();
            this.latitude = latitude_;
            this.longitude = longitude_;
            this.index = index_;
            this.name = name_;
            Cape.setItemName(name);
            Cape.setTag(index);
            Cape.setMapPoint(MapPoint.mapPointWithGeoCoord(this.latitude, this.longitude));
            Cape.setMarkerType(MapPOIItem.MarkerType.CustomImage);
            Cape.setCustomImageResourceId(R.drawable.cape);
            Cape.setCustomImageAutoscale(false);
            this.used = false;
            this.first=true;
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
            mapView.addPOIItem(this.Cape);
        }

        public void hide_item() {
            mapView.removePOIItem(this.Cape);
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
        MapPOIItem thief_marker;
        int index;
        boolean out;
        double latitude;
        double longitude;
        boolean death;
        String nic;

        public Partner(String nic,  double latitude,double longitude, int index) {
            thief_marker=new MapPOIItem();
            this.nic = nic;
            this.longitude = longitude;
            this.latitude = latitude;
            this.index = index;
            this.out = false;
            thief_marker.setMapPoint(MapPoint.mapPointWithGeoCoord(latitude, longitude));
            thief_marker.setTag(index);
            thief_marker.setItemName(nic);
            thief_marker.setMarkerType(MapPOIItem.MarkerType.CustomImage);
            thief_marker.setCustomImageResourceId(R.drawable.thief);
            thief_marker.setCustomImageAutoscale(false);

        }

        public void show() {
            mapView.addPOIItem(thief_marker);
        }

        public void hide() {
            mapView.removePOIItem(thief_marker);
        }

        public void change_marker() {
            thief_marker.setMapPoint(MapPoint.mapPointWithGeoCoord(this.latitude, this.longitude));
        }

        public void change_location(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }


    //동료 표시
    public void Show_partner() {
        for (int i = 0; i < partner_num; i++) {
            thief[i].show();
        }
    }

    //점령존 표시
    public void Show_zone() {
        for (int i = 0; i < zone_num; i++) {
            zone[i].show_zone();
        }
    }

    //제한구역 및 시작존 표시
    public void Show_Area() {
        Area.show_zone();
       Prison.show_zone();
    }

    //동료 마커 이동
    public void Renew_partner_location() {
        for (int i = 0; i < partner_num; i++) {
            thief[i].change_marker();
        }
    }

    //범위 밖으로 나갔을 때 처리 함수
    public void Game_out() {
        out=true;
        ///////////////////////////////////////누가 나갔는지 서버에게 전송/////////////////////////////////////////
        try {
            out_ob.put("room_num",roomnum);
            out_ob.put("index",my_index);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        /////////////////////////////////////////////////////////////////////////////////////////////////////////
        //범위 밖으로 나갔을 때 처리
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);
        mapView.setShowCurrentLocationMarker(false);
        cancel();
        Cancle_OT();
        Cancle_PT();
        if(item_end==false&&game_death==false)
        {
            Cancle_IT();
        }
        RoomList.mSocket.emit("thief_out",out_ob);
        //범위 밖으로 나갔을 때 처리(클라이언트 안에서 나갔다고 표시후 관전모드)
    }

    //도둑이 존 모두 점령했을때
    public void Zone_all_clear() {


        double result = Check_distance(tempp.getMapPointGeoCoord().latitude, tempp.getMapPointGeoCoord().longitude, Prison.getLatitude(), Prison.getLongitude());
        if(game_death==true&&out==false&&result<10)    //내가 죽었었고 감옥존에 있다면
        {


            death_l1.setVisibility(LinearLayout.VISIBLE);
            death_l2.setVisibility(LinearLayout.INVISIBLE);
            death_view.setVisibility(View.INVISIBLE);
            game_death = false;
            //쓰레드 다시 시작

            if (item_end == false)
            {
                Start_IT();
            }

            layout = inflater2.inflate(R.layout.tst_t_escape,
                    (ViewGroup) findViewById(R.id.custom_toast_container));

            toast.setView(layout);
            toast.show();


            try {
                escape_ob.put("index",my_index);
                escape_ob.put("room_num",roomnum);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            RoomList.mSocket.emit("thief_escape",escape_ob);

        }
        else if(game_death==true&&out==false&&result>10)    //내가 죽었었고 감옥존에 없다면
        {
            layout = inflater2.inflate(R.layout.tst_t_notescape,
                    (ViewGroup) findViewById(R.id.custom_toast_container));

            toast.setView(layout);
            toast.show();
        }


        for(int i=0;i<zone_num;i++)
        {
            zone[i].occupy=false;
            zone[i].Set_RGB(0,0,255);
            zone[i].remove_zone();
            zone[i].show_zone();
        }

    }
    //도둑이 존 점령시
    public void Zone_occupy(int index) {

        zone[index].occupy = true;
        zone[index].Set_RGB(255, 0, 0);
        zone[index].remove_zone();
        zone[index].show_zone();
    }
    //도둑이 아이템 사용햇을때
    public void Hide_thief() {
        item_get=false;
        //버튼 비활성화 코드


        //서버로 알림(경찰측에 은신처리 위해서)
        try {
            cape_ob.put("index",my_index);
            cape_ob.put("room_num",roomnum);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RoomList.mSocket.emit("thief_item_use",cape_ob);
    }
//검거 되었을 때
    public void Arrested() {
        death_l1.setVisibility(LinearLayout.INVISIBLE);
        death_l2.setVisibility(LinearLayout.VISIBLE);
        death_view.setVisibility(View.VISIBLE);
        game_death = true;
        if (item_end == false) {
            Cancle_IT();
        }
        //검거 됬을 때 처리(클라이언트 안에서 잡혔다고 표시후 관전모드)
    }

    public void Initialize(){
        this.range=data.getRange();
        this.roomnum=data.getRoom_num();
        this.partner_num=data.getPartner_num();
        this.item_num=data.getItem_num();
        this.zone_num=data.getZone_num();
        this.control_point=MapPoint.mapPointWithGeoCoord(data.getStart().getLatitude(),data.getStart().getLongitude());
        this.base_point.setLatitude(data.getStart().getLatitude());
        this.base_point.setLongitude(data.getStart().getLongitude());
        this.tempp=MapPoint.mapPointWithGeoCoord(data.getMine().getLatitude(),data.getMine().getLongitude());
        this.my_index=data.getMy_index();
        this.game_time=data.getGame_time();
        game_time=game_time*60;
        int minute=game_time/60;
        int second=game_time%60;
        Log.i("range",String.valueOf(range));
        Log.i("roomnum",String.valueOf(roomnum));
        Log.i("partnernum",String.valueOf(partner_num));
        Log.i("item_num",String.valueOf(item_num));
        Log.i("zone_num",String.valueOf(zone_num));
        Log.i("control_point_la",String.valueOf(control_point.getMapPointGeoCoord().latitude));
        Log.i("control_point_lo",String.valueOf(control_point.getMapPointGeoCoord().longitude));
        Log.i("my_index",String.valueOf(my_index));

        String now_time=String.valueOf(minute)+":"+String.valueOf(second);

        game_tv.setText(now_time);
        this.zone_range=12;
        cape = new Item[item_num];
        zone = new Zone[zone_num];
        thief = new Partner[partner_num];
        for(int i=0;i<item_num;i++)
        {
            cape[i]=new Item(data.getItem().get(i).getLatitude(),data.getItem().get(i).getLongitude(),i,"은신망토");
            Log.i("cape_la",String.valueOf(cape[i].latitude));
            Log.i("cape_la",String.valueOf(cape[i].longitude));

        }
        for(int i=0;i<partner_num;i++)
        {
            thief[i]=new Partner(data.getPartner_nic().get(i),data.getPartner().get(i).getLatitude(),data.getPartner().get(i).getLongitude(),i);
            Log.i("thief_la",String.valueOf(thief[i].latitude));
            Log.i("thief_la",String.valueOf(thief[i].longitude));
        }
        for(int i=0;i<zone_num;i++)
        {
            zone[i]=new Zone(data.getZone().get(i).getLatitude(),data.getZone().get(i).getLongitude(),zone_range,i,0,0,255,128);
            Log.i("zone_la",String.valueOf(zone[i].latitude));
            Log.i("zone_lo",String.valueOf(zone[i].longitude));
         }
       Area=new Zone(control_point.getMapPointGeoCoord().latitude,control_point.getMapPointGeoCoord().longitude,range,100,255,0,0,0);
        Prison=new Zone(control_point.getMapPointGeoCoord().latitude,control_point.getMapPointGeoCoord().longitude,10,101,0,0,0,128);
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

        //locationManager.requestSingleUpdate("gps", listener, null);
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
        vibrator.vibrate(mainpattern,-1);

        if(my_index==0)
        {
            RoomList.mSocket.emit("game_clear",roomnum);
        }

        if(out==false&&game_death==false)   //죽은상태가 아니고 out이아니면
        {


            mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);
            mapView.setShowCurrentLocationMarker(false);
            cancel();
           Cancle_OT();
            Cancle_PT();

            if(item_end==false)
            {
                Cancle_IT();
            }
        }
        else if(out==false&&game_death==true)
        {
            //startactivity
        }




        RoomList.mSocket.off("thief_death", thief_death);
        RoomList.mSocket.off("alldeath", alldeath);
        RoomList.mSocket.off("zone_clear", zone_clear);
        RoomList.mSocket.off("allclear", allclear);
        RoomList.mSocket.off("thief_item_get", thief_item_get);
        RoomList.mSocket.off("request_thief_location", request_thief_location);
        RoomList.mSocket.off("thief_out",  thief_out);
        RoomList.mSocket.off("thief_escape",  thief_escape);







        InGameReady.activity.finish();
        finish();
        //승리 패배 표시후 액티비티 전환
    }

    public void countDownTimer(){
        countDownTimer = new CountDownTimer(MILLISINFUTURE, COUNT_DOWN_INTERVAL) {
            public void onTick(long millisUntilFinished) {
                b1.setText(String.valueOf(count));
                count --;
            }

            public void onFinish() {
              //  b1.setEnabled(true);
                // linearLayout.setVisibility(View.INVISIBLE);//
                // btn.setBackgroundResource(R.drawable.thief);
                b1.setText(String.valueOf(""));
            }
        };
    }
    //서버로부터 검거된 도둑 처리
    private Emitter.Listener thief_death = new Emitter.Listener() {

        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    JSONObject data = (JSONObject) args[0];
                    try {
                        int d_index=data.getInt("data");
                        if(d_index==my_index)//내가 죽은거면
                        {
                            vibrator.vibrate(mainpattern,-1);
                           Arrested();

                            layout = inflater2.inflate(R.layout.tst_t_catched,
                                    (ViewGroup) findViewById(R.id.custom_toast_container));
                            text= (TextView) layout.findViewById(R.id.tst_id);
                            String temp;
                            temp="검거됨";
                            text.setText(temp);
                            toast.setView(layout);
                            toast.show();

                            //모든 쓰레드 종료 및 관전모드////////////////////////////////////////
                        }
                        else{   //동료가 죽은거면
                            if(d_index>my_index)
                            {
                               d_index--;
                            }
                            thief[d_index].death=true;
                            thief[d_index].hide();
                            vibrator.vibrate(mainpattern,-1);

                            layout = inflater2.inflate(R.layout.tst_t_catched,
                                    (ViewGroup) findViewById(R.id.custom_toast_container));
                            text= (TextView) layout.findViewById(R.id.tst_id);
                            String temp;
                            temp=String.valueOf(text.getText());

                            temp=thief[d_index].nic+temp;
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
    //서버로부터 점령된 점령존 처리
    private Emitter.Listener zone_clear = new Emitter.Listener() {

        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    JSONObject data = (JSONObject) args[0];
                    try {

                        int d_index = data.getInt("data");


                        vibrator.vibrate(mainpattern,-1);

                        if(zone[d_index].isOccupy()==false)
                        {
                            layout = inflater2.inflate(R.layout.tst_t_zone,
                                    (ViewGroup) findViewById(R.id.custom_toast_container));
                            text= (TextView) layout.findViewById(R.id.tst_id);
                            String temp;
                            temp=String.valueOf(text.getText());
                            temp=String.valueOf(d_index+1)+temp;
                            text.setText(temp);
                            toast.setView(layout);
                            toast.show();
                            Zone_occupy(d_index);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                }
            });
        }
    };

    //점령존이 모두 점령되면 도둑 모두 살리기(out 빼기)
    private Emitter.Listener allclear = new Emitter.Listener() {

        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    vibrator.vibrate(mainpattern,-1);
                    layout = inflater2.inflate(R.layout.tst_t_allzone,
                            (ViewGroup) findViewById(R.id.custom_toast_container));

                    toast.setView(layout);
                    toast.show();

                    Zone_all_clear();


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
                    Cancle_GT();
                    vibrator.vibrate(mainpattern,-1);
                    layout = inflater2.inflate(R.layout.tst_t_lose,
                            (ViewGroup) findViewById(R.id.custom_toast_container));

                    toast.setView(layout);
                    toast.show();   //도둑 패배

                    Game_Over();
                    //게임 종료 처리



                }
            });
        }
    };


    //서버로부터 먹힌 아이템 처리
    private Emitter.Listener thief_item_get = new Emitter.Listener() {

        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    JSONObject data = (JSONObject) args[0];
                    try {

                        int d_index=data.getInt("data");

                        cape[d_index].used=true;
                        if(cape[d_index].first==false)
                        {
                            cape[d_index].hide_item();

//                            cape[d_index].first=true;
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                }
            });
        }
    };


    //서버로부터 갱신된 위치 받았을 때 도둑 위치바꾸고 마커 다시표시
    private Emitter.Listener   request_thief_location = new Emitter.Listener() {

        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try{
                        JSONObject origin_data = (JSONObject) args[0];
                        JSONArray data=origin_data.getJSONArray("data");


                        JSONObject p1=new JSONObject();
                        for(int i = 1; i < data.length(); i++) {
                            p1 = data.getJSONObject(i);
                            thief[i-1].change_location(p1.getDouble("latitude"),p1.getDouble("longitude"));
                        }

                        Renew_partner_location();

                    }catch (JSONException | NullPointerException e){
                        Toast.makeText(getApplicationContext(),
                                "Error" + e.toString(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    };


    //서버로부터 나간 도둑동료 처리
    private Emitter.Listener   thief_out = new Emitter.Listener() {

        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    JSONObject data = (JSONObject) args[0];
                    try {
                        int d_index=data.getInt("data");
                        if(d_index!=my_index)
                        {
                            if(d_index>my_index)
                            {
                                d_index--;
                            }
                            thief[d_index].out=true;
                            thief[d_index].hide();

                            layout = inflater2.inflate(R.layout.tst_t_out,
                                    (ViewGroup) findViewById(R.id.custom_toast_container));
                            text= (TextView) layout.findViewById(R.id.tst_id);
                            String temp;
                            temp=String.valueOf(text.getText());
                            temp=thief[d_index].nic+" "+temp;
                            text.setText(temp);
                            toast.setView(layout);
                            toast.show();
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    vibrator.vibrate(mainpattern,-1);


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


    private Emitter.Listener thief_escape = new Emitter.Listener() {

        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    JSONObject data = (JSONObject) args[0];

                    try {
                        int d_index = data.getInt("data");
                        if(d_index!=my_index)
                        {
                            if(d_index>my_index)
                            {
                                d_index--;
                            }
                            if (thief[d_index].out == false && thief[d_index].death == true) {
                                thief[d_index].death = false;
                                thief[d_index].show();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                }
            });
        }
    };

}

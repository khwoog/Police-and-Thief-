    package kr.ac.kumoh.ce.s20120420.JustRun;

    import android.Manifest;
    import android.app.Activity;
    import android.content.Intent;
    import android.content.pm.PackageManager;
    import android.graphics.Color;
    import android.location.Location;
    import android.location.LocationListener;
    import android.location.LocationManager;
    import android.os.Build;
    import android.os.Bundle;
    import android.os.Handler;
    import android.os.Message;
    import android.provider.Settings;
    import android.support.annotation.NonNull;
    import android.support.annotation.Nullable;
    import android.support.v4.app.ActivityCompat;
    import android.support.v7.app.AppCompatActivity;
    import android.util.Log;
    import android.widget.ImageView;
    import android.widget.LinearLayout;
    import android.widget.TextView;
    import android.widget.Toast;

    import com.github.nkzawa.emitter.Emitter;

    import org.json.JSONArray;
    import org.json.JSONException;
    import org.json.JSONObject;

    import java.util.Random;

    /**
     * Created by woong on 2017-06-06.
     */
    public class InGameReady extends AppCompatActivity {
        public static Activity activity;
        public final static int REPEAT_DELAY = 1000;
        int time;
        int game_time;
        Intent intent;
        int my_index;
        int room_num;
        Intent receive_intent;
        public Handler handler;
        TextView time_tv;
        boolean role;
        private LocationManager locationManager;
        private LocationListener listener;
        Location my_location;
        boolean get = false;
        double latitude;
        double longitude;
        boolean change=false;
        boolean send=false;
        boolean change_tonetwork=false;
        Data_Police dp;
        Data_Thief df;
        Data_ingame di;
        boolean room_master=false;

        //게임 정보 저장 위한 변수들////////////////
        int thief_item_num;
        int police_item_num;
        int zone_num;
        int range;
        int zone_range;
        Location start;
        Location thief_item[];
        Location police_item[];
        Location thief_zone[];

        To_Server infor;
        JSONObject my_ob = new JSONObject();
        JSONObject room_ob=new JSONObject();
        JSONObject ingame_ob=new JSONObject();

        JSONArray Ti = new JSONArray();
        JSONArray Pi = new JSONArray();
        JSONArray Tz = new JSONArray();


        LinearLayout ingame_ll;
        TextView ingame_tv;
        ImageView ingame_iv1;
        ImageView ingame_iv2;

        @Override
        protected void onDestroy() {
            super.onDestroy();
            RoomList.mSocket.off("game_info", game_info);

        }

        @Override
        public void onBackPressed() {

        }

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setContentView(R.layout.ingameready);

            ingame_ll=(LinearLayout)findViewById(R.id.ingame_ll);
            ingame_tv=(TextView)findViewById(R.id.timer);
            ingame_iv1=(ImageView)findViewById(R.id.iv1);
            ingame_iv2=(ImageView)findViewById(R.id.iv2);

            activity=InGameReady.this;
            receive_intent=getIntent();
            di=(Data_ingame) receive_intent.getSerializableExtra("OBJECT");
            room_master=di.isRoom_master();
            if(room_master==true)   //방장이면 방 정보 생성한다.
            {
                set_information();
                thief_item=new Location[thief_item_num];
                police_item=new Location[police_item_num];
                thief_zone=new Location[zone_num];
                zone_range=12;
            }
            else{
                this.room_num=di.getRoom_num();
                this.game_time=di.getGame_time();
                this.role=di.isMy_role();
                this.range=di.getRange();
            }

            if(role==true)
            {
                ingame_ll.setBackgroundResource(R.drawable.info_back_police);
                ingame_tv.setTextColor(Color.rgb(138,172,200));
                ingame_iv1.setImageResource(R.drawable.info_title1_police);
                ingame_iv2.setImageResource(R.drawable.info_title2_police);

            }
            else if(role==false)
            {
                ingame_ll.setBackgroundResource(R.drawable.info_back_thief);
                ingame_tv.setTextColor(Color.rgb(255,82,82));
                ingame_iv1.setImageResource(R.drawable.info_title1_thief);
                ingame_iv2.setImageResource(R.drawable.info_title2_thief);

            }


            my_location=new Location("mine");
            time_tv = (TextView) findViewById(R.id.timer);

            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                listener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {

                        Log.i("get","get");
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        my_location.setLatitude(latitude);
                        my_location.setLongitude(longitude);

                        get = true;
                    }

                @Override
                public void onStatusChanged(String s, int i, Bundle bundle) {

                }

                @Override
                public void onProviderEnabled(String s) {

                }

                @Override
                public void onProviderDisabled(String s) {
                    Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(i);


                    if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        change=true;
                    }

                }
            };
            time = 60;




            handler = new Handler() {

                public void handleMessage(Message msg) {

                    super.handleMessage(msg);

                    this.sendEmptyMessageDelayed(0, REPEAT_DELAY);        // REPEAT_DELAY 간격으로 계속해서 반복하게 만들어준다

                    time--;
                    Log.i("time","time");
                    time_tv.setText(String.valueOf(time));
                    if(change==true&&send==false&&get==false)       //gps안켜져있엇을경우 좌표값 안받아온상태이면 다시 check
                    {

                        change=false;

                        if(time<30)
                        check_location_gps();
                        else
                            check_location_network();
                    }
                    if (time < 30 && get == false&&send==false&&change_tonetwork==false) {       //실내에있어서 안잡히는경우 그냥 네트워크로 좌표정보 받아옴
                        change_tonetwork=true;
                        cancel();
                        check_location_network();

                    }
                    if (get == true) {      //좌표 받아온 경우
                        if(room_master==true)   //방장이면 경찰아이템 도둑아이템 점령존 위치 랜덤으로 생성
                        {
                            Log.i("1","1");
                            start=new Location("start_point");
                            start.setLatitude(my_location.getLatitude());
                            start.setLongitude(my_location.getLongitude());
                            Log.i("2","2");
                            setting_theif_item();

                            Log.i("3","31");
                            setting_police_item();
                            Log.i("4","4");
                            setting_zone();

                        //////////////////////////////방장일 경우 인 게임 정보 생성한거 보내기///////////////////////////////
                            Log.i("5","5");
                            try{
                                room_ob.put("room_num",room_num);
                                room_ob.put("start_latitude",start.getLatitude());
                                room_ob.put("start_longitude",start.getLongitude());
                                for(int i=0;i<police_item_num;i++)
                                {
                                    Pi.put(police_item[i].getLatitude());
                                    Pi.put(police_item[i].getLongitude());
                                    Log.i("police","police");
                                }
                                for(int i=0;i<thief_item_num;i++)
                                {
                                    Ti.put(thief_item[i].getLatitude());
                                    Ti.put(thief_item[i].getLongitude());
                                    Log.i("thief","thief");
                                }
                                for(int i=0;i<zone_num;i++)
                                {
                                    Tz.put(thief_zone[i].getLatitude());
                                    Tz.put(thief_zone[i].getLongitude());
                                    Log.i("zone","zone");
                                }

                                room_ob.put("Pi", Pi);
                                room_ob.put("Ti", Ti);
                                room_ob.put("Tz", Tz);
                            }catch(JSONException e){
                                e.printStackTrace();
                            }
                            Log.i("6","6");
                            RoomList.mSocket.emit("room_infor", room_ob);
                            Log.i("7","7");
                        ////////////////////////////////////////////////////////////////////////////////////////////////////
                        }

                        ///////////////////////////서버로 현재 위치 보내주는거 인덱스랑 같이/////////////////////////
                        infor=new To_Server(my_location.getLatitude(),my_location.getLongitude(),my_index);
                        try {
                            my_ob.put("room_num",room_num);
                            my_ob.put("index",infor.getIndex());
                            my_ob.put("latitude",infor.getLatitude());
                            my_ob.put("longitude",infor.getLongitude());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        RoomList.mSocket.emit("my_location", my_ob);
                        /////////////////////////////////////////////////////////////////////////////////////////////

                        send=true;
                        get = false;
                    }
                    if (time <= 0) {        //타이머 끝나면
                        handler.removeMessages(0);
                        cancel();

                        //////////////////////////////ingame 정보 달라고 요청////////////////////////////////////////
                        try {
                            ingame_ob.put("room_num",room_num);
                            ingame_ob.put("role",role);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        RoomList.mSocket.emit("request_gameinfor",ingame_ob);
                        ////////////////////////////////////////////////////////////////////////////////////////////

                        //가져온 정보를 역할에 따라 Data_Police or Data_Thief로 만든 후 intent 넘길때 putextra로 넘겨준다.!!!!


                    }
                }

            };

            handler.sendEmptyMessage(0);
            check_location_gps();




            RoomList.mSocket.on("game_info", game_info);


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

            locationManager.requestSingleUpdate("gps", listener, null);
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
            locationManager.requestSingleUpdate("network", listener, null);
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
        //경찰 아이템 랜덤으로 위치 생성
        public void setting_police_item(){

            Location ttt;
            boolean reset=false;

            for(int i=0;i<police_item_num;i++)
            {
                police_item[i]=new Location("police");
                ttt=getLocationInLatLngRad(40,start);
                for(int j=0;j<i;j++)
                {
                    if(Check_distance(ttt.getLatitude(),ttt.getLongitude(),police_item[j].getLatitude(),police_item[j].getLongitude())<10)
                    {
                        reset=true;
                        break;
                    }
                }
                if(reset==true)
                {
                    i--;
                    reset=false;
                    continue;
                }
                else{
                    police_item[i].setLatitude(ttt.getLatitude());
                    police_item[i].setLongitude(ttt.getLongitude());
                }
            }
        }
        //도둑 아이템 랜덤으로 위치 생.성
        public void setting_theif_item(){

            boolean reset=false;
            Location ttt;

            for(int i=0;i<thief_item_num;i++)
            {
                thief_item[i]=new Location("thief");
                ttt=getLocationInLatLngRad(range ,start);
                for(int j=0;j<i;j++)
                {
                    if(Check_distance(ttt.getLatitude(),ttt.getLongitude(),thief_item[j].getLatitude(),thief_item[j].getLongitude())<10)
                    {
                        reset=true;
                        break;
                    }
                }
                if(reset==true)
                {
                    i--;
                    reset=false;
                    continue;
                }
                else{
                    thief_item[i].setLatitude(ttt.getLatitude());
                    thief_item[i].setLongitude(ttt.getLongitude());
                }
            }
        }
        //점령존 랜덤으로 위치 생성
        public void setting_zone(){

            boolean reset=false;
            Location ttt;

            for(int i=0;i<zone_num;i++)
            {
                thief_zone[i]=new Location("thief_zone");
                ttt=getLocationInLatLngRad(range-zone_range ,start);

                for(int j=0;j<i;j++)
                {
                    if(Check_distance(ttt.getLatitude(),ttt.getLongitude(),thief_zone[j].getLatitude(),thief_zone[j].getLongitude())<zone_range*2||
                            Check_distance(start.getLatitude(),start.getLongitude(),ttt.getLatitude(),ttt.getLongitude())<zone_range*2)
                    {
                        reset=true;
                        break;
                    }
                }
                if(reset==true)
                {
                    i--;
                    reset=false;
                    continue;
                }
                else{

                    thief_zone[i].setLatitude(ttt.getLatitude());
                    thief_zone[i].setLongitude(ttt.getLongitude());

                }
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




        //랜덤 위치 생성을 위한 함수
        protected static Location getLocationInLatLngRad(double radiusInMeters, Location currentLocation) {
            double x0 = currentLocation.getLongitude();
            double y0 = currentLocation.getLatitude();

            Random random = new Random();

            // Convert radius from meters to degrees.
            double radiusInDegrees = radiusInMeters / 111320f;

            // Get a random distance and a random angle.
            double u = random.nextDouble();
            double v = random.nextDouble();
            double w = radiusInDegrees * Math.sqrt(u);
            double t = 2 * Math.PI * v;
            // Get the x and y delta values.
            double x = w * Math.cos(t);
            double y = w * Math.sin(t);

            // Compensate the x value.
            double new_x = x / Math.cos(Math.toRadians(y0));

            double foundLatitude;
            double foundLongitude;

            foundLatitude = y0 + y;
            foundLongitude = x0 + new_x;

            Location copy = new Location(currentLocation);
            copy.setLatitude(foundLatitude);
            copy.setLongitude(foundLongitude);
            return copy;
        }
        //인게임 정보 객체들 초기화
        public void set_information(){

            this.thief_item_num=di.getThief_item_num();
            this.police_item_num=di.getPolice_item_num();
            this.zone_num=di.getZone_num();
            this.range=di.getRange();
            this.room_num=di.getRoom_num();
            this.game_time=di.getGame_time();
            this.role=di.isMy_role();
            this.room_master=di.isRoom_master();
        }
        public class To_Server{
            double latitude;
            double longitude;
            int index;

            public To_Server(double latitude, double longitude, int index) {
                this.latitude = latitude;
                this.index = index;
                this.longitude = longitude;
            }

            public double getLatitude() {
                return latitude;
            }

            public double getLongitude() {
                return longitude;
            }

            public int getIndex() {
                return index;
            }
        }

        //서버로부터 게임에 필요한 정보를 가져와서 클래스에 저장하는 코드
        private Emitter.Listener game_info = new Emitter.Listener() {

            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            if(role==true)
                            {

                                JSONObject origin_data = (JSONObject)args[0];
                                JSONObject data=  origin_data.getJSONObject("data");
                                //JSONArray jsonMainNode = data.getJSONArray("data");
                                origin_data = (JSONObject)args[1];
                                JSONArray data2 = origin_data.getJSONArray("data2");
                                origin_data = (JSONObject)args[2];
                                JSONArray data3 = origin_data.getJSONArray("data3");
                                origin_data = (JSONObject)args[3];
                                JSONArray data4 = origin_data.getJSONArray("data4");
                                origin_data = (JSONObject)args[4];
                                JSONArray data5 = origin_data.getJSONArray("data5");

                                dp=new Data_Police(range,data.getInt("zonNum"),data.getInt("itemNum"),data.getInt("partnerNum"),
                                        data.getInt("enermyNum"),room_num,Double.valueOf(data.getString("startLatitude")),Double.valueOf(data.getString("startLongitude")),
                                        my_location.getLatitude(),my_location.getLongitude(),data.getInt("my_index"),game_time);


                                int j = 0;
                                JSONObject p1=new JSONObject();
                                for(int i = 1; i < data2.length(); i++) {
                                    p1 = data2.getJSONObject(i);
                                    dp.setItem(p1.getDouble("latitude"), p1.getDouble("longitude"));
                                }
                                for(int i = 1; i < data3.length(); i++){
                                    p1 = data3.getJSONObject(i);
                                    dp.setZone(p1.getDouble("latitude"), p1.getDouble("longitude"));
                                    p1 = data4.getJSONObject(i);
                                    dp.setEnermy(p1.getDouble("latitude"), p1.getDouble("longitude"));
                                }
                                for(int i = 1; i < data5.length(); i++){
                                    p1 = data5.getJSONObject(i);
                                    dp.setPartner(p1.getDouble("latitude"), p1.getDouble("longitude"));
                                    dp.setPartner_nic(p1.getString("nicName"));
                                }

                                    intent = new Intent(InGameReady.this, Police.class);
                                    intent.putExtra("OBJECT", dp);


                                startActivity(intent);
                            }
                            else if(role==false)
                            {
                                JSONObject origin_data = (JSONObject)args[0];
                                JSONObject data=origin_data.getJSONObject("data");
                                //JSONArray jsonMainNode = data.getJSONArray("data");
                               origin_data = (JSONObject)args[1];
                                JSONArray data2 = origin_data.getJSONArray("data2");
                                 origin_data = (JSONObject)args[2];
                                JSONArray data3 = origin_data.getJSONArray("data3");
                                origin_data = (JSONObject)args[3];
                                JSONArray data4 = origin_data.getJSONArray("data4");


                                df=new Data_Thief(range,data.getInt("zonNum"),data.getInt("itemNum"),data.getInt("partnerNum"),
                                        room_num,Double.valueOf(data.getString("startLatitude")),Double.valueOf(data.getString("startLongitude")),
                                        my_location.getLatitude(),my_location.getLongitude(),data.getInt("my_index"),game_time);



                                JSONObject p1=new JSONObject();

                                for(int i = 1; i < data3.length(); i++){
                                    p1 = data2.getJSONObject(i);
                                    df.setItem(p1.getDouble("latitude"), p1.getDouble("longitude"));
                                    p1 = data3.getJSONObject(i);
                                    df.setZone(p1.getDouble("latitude"), p1.getDouble("longitude"));
                                }
                                for(int i = 1; i < data4.length(); i++){
                                    p1 = data4.getJSONObject(i);
                                    df.setPartner(p1.getDouble("latitude"), p1.getDouble("longitude"));
                                    df.setPartner_nic(p1.getString("nicName"));
                                }

                                intent = new Intent(InGameReady.this, Thief.class);
                                intent.putExtra("OBJECT", df);

                                startActivity(intent);
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
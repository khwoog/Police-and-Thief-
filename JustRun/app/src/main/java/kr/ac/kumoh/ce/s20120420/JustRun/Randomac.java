package kr.ac.kumoh.ce.s20120420.JustRun;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import net.daum.mf.map.api.MapCircle;
import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapView;

import java.util.Random;

/**
 * Created by woong on 2017-06-06.
 */
public class Randomac extends AppCompatActivity implements MapView.MapViewEventListener, MapView.POIItemEventListener, MapView.CurrentLocationEventListener {
private boolean role;
        int p_count = 0;
        boolean first = true;
        Handler mHandler = new Handler();
private boolean PT_running; //쓰레드 종료하기 위해 쓰는변수

        String distance;

private MapPOIItem mDefaultMarker;          //기본 마커
private MapPOIItem Handcuff_Marker;         //수갑 아이템
private MapPOIItem Cape_Marker;             //은신 아이템

        MapPoint tempp = MapPoint.mapPointWithGeoCoord(36.146451, 128.393725);  //기본 마커 위치;
        MapPoint start;//맨처음 시작
        MapCircle circle1;

    Location cur;

        Item cape[]=new Item[10];    //은신아이템
    Item fuck;
        Zone zone[];
    Zone zone_two[];
       Zone main;
        location lol[]=new location[10];
        Partner poli[]=new Partner[5];
private static final MapPoint DEFAULT_MARKER_POINT = MapPoint.mapPointWithGeoCoord(36.14501854, 128.39366578);  //기본 마커 위치
private static final MapPoint Handcuff_Marker_POINT = MapPoint.mapPointWithGeoCoord(36.145365, 128.392580); //수갑 아이템 위치
private static final MapPoint Cape_Marker_POINT = MapPoint.mapPointWithGeoCoord(36.145569, 128.392698);      //은신 아이템 위치
private static final MapPoint Partner_Marker_POINT = MapPoint.mapPointWithGeoCoord(36.14501854, 128.39366578); //동료 위치
        MapView mapView;

        //ui 겹치기 위한변수들
        LayoutInflater inflater;
        LinearLayout linear;
        LinearLayout.LayoutParams paramlinear;
    Randomac mRand;
    MapPOIItem tas;

@Override
protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.ingame);
    tas = new MapPOIItem();

zone=new Zone[10];
    zone_two=new Zone[5];
cur=new Location("Point start");

    cur.setLatitude(36.146451);
    cur.setLongitude(128.393725);
        //ui 겹치기 코드
        inflater = (LayoutInflater) getSystemService(

        Context.LAYOUT_INFLATER_SERVICE);

        linear = (LinearLayout) inflater.inflate(R.layout.basic, null);


        paramlinear = new LinearLayout.LayoutParams(

        LinearLayout.LayoutParams.MATCH_PARENT,

        LinearLayout.LayoutParams.MATCH_PARENT);

        addContentView(linear, paramlinear);
////////////////////////////////////////////////////////////////////////
    mRand = new Randomac();
        mapView = new MapView(this);
        mapView.setDaumMapApiKey("4e6090d28d2a2cc01fea4130929cf630");
        mapView.setMapType(MapView.MapType.Standard);
        ViewGroup mapViewContainer = (ViewGroup) findViewById(R.id.map_view);
        mapViewContainer.addView(mapView);
        mapView.setMapViewEventListener(this);
        mapView.setPOIItemEventListener(this);
        mapView.setCurrentLocationEventListener(this);
        MapPoint tempp = MapPoint.mapPointWithGeoCoord(36.146451, 128.393725);  //기본 마커 위치;
        start = MapPoint.mapPointWithGeoCoord(36.146451, 128.393725);  //기본 마커 위치;
        main=new Zone(36.146451, 128.393725,200,0,0,0,255);
    fuck=new Item(36.146451, 128.393725,45,"시발");
    fuck.show_item();
    main.show_zone();
//setting();

    tas.setCustomImageResourceId(R.drawable.cape);
    tas.setCustomImageAutoscale(false);
    tas.setMarkerType(MapPOIItem.MarkerType.BluePin);
    tas.setSelectedMarkerType(MapPOIItem.MarkerType.RedPin);

    tas.setMapPoint(Cape_Marker_POINT);
    mapView.addPOIItem(tas);
    createDefaultMarker(mapView);

        }


@

        Override
protected void onDestroy() {
        //종료 될때 트래킹 중지
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);
        mapView.setShowCurrentLocationMarker(false);
        super.onDestroy();
        }

//맵이 처음 시작될때
@Override
public void onMapViewInitialized(MapView mapView) {

      //  createCapeMarker(mapView);
        // createHandcuffMarker(mapView);
        //create_Partner(mapView);
        createDefaultMarker(mapView);
        if (role == true) {   //경찰인 경우
        mapView.setCurrentLocationRadius(0);
        mapView.setDefaultCurrentLocationMarker();
        } else if (role == false) { //도둑인 경우

        MapPOIItem.ImageOffset trackingImageAnchorPointOffset = new MapPOIItem.ImageOffset(16, 16); // 좌하단(0,0) 기준 앵커포인트 오프셋
        mapView.setCustomCurrentLocationMarkerTrackingImage(R.drawable.custom_map_present_tracking, trackingImageAnchorPointOffset);
           /* mMapView.setCurrentLocationRadius(100); // meter
            mMapView.setCurrentLocationRadiusFillColor(Color.argb(77, 255, 255, 0));
           mMapView.setCurrentLocationRadiusStrokeColor(Color.argb(77, 255, 165, 0));

           mapView.setCustomCurrentLocationMarkerDirectionImage(R.drawable.custom_map_present_direction, directionImageAnchorPointOffset);
            mapView.setCustomCurrentLocationMarkerImage(R.drawable.custom_map_present, offImageAnchorPointOffset);*/

        }

        //트래킹 시작


        mapView.setMapCenterPointAndZoomLevel(MapPoint.mapPointWithGeoCoord(36.1455686, 128.3925396), 1, false);       //처음 시작될때 내 위치로 맵 이동

    setting_police_item();
    setting_theif_item();
    setting_zone();
    setting_police();
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

        if (mapView.getZoomLevel() <= -1) {     //더블 클릭 문제 해결을 위해서
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeadingWithoutMapMoving);

        }

        }

@Override
public void onMapViewLongPressed(MapView mapView, MapPoint mapPoint) {

        }

@Override
public void onMapViewDragStarted(MapView mapView, MapPoint mapPoint) {
        //mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);
        // mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeadingWithoutMapMoving);
        }

@Override
public void onMapViewDragEnded(MapView mapView, MapPoint mapPoint) {
        //mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);
        //mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading);
        }

@Override
public void onMapViewMoveFinished(MapView mapView, MapPoint mapPoint) {

        }

/////////////////////////////////////////////////////////////////////////
//도둑이 아이템 먹을때 없어지게 하기 위한 클릭이벤트
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

//디폴트 마커 생성 (지정한 위치에)
private void createDefaultMarker(MapView mapView) {
        mDefaultMarker = new MapPOIItem();
        String name = "Default Marker";
        mDefaultMarker.setItemName(name);
        mDefaultMarker.setTag(0);
        mDefaultMarker.setMapPoint(DEFAULT_MARKER_POINT);
        mDefaultMarker.setMarkerType(MapPOIItem.MarkerType.BluePin);
        mDefaultMarker.setSelectedMarkerType(MapPOIItem.MarkerType.RedPin);


           mapView.selectPOIItem(mDefaultMarker, true);


        }

//수갑 아이템 생성후 표시 함수
private void createHandcuffMarker(MapView mapView) {
        Handcuff_Marker = new MapPOIItem();
        String name = "Custom Marker";
        Handcuff_Marker.setItemName(name);
        Handcuff_Marker.setTag(1);
        Handcuff_Marker.setMapPoint(Handcuff_Marker_POINT);

        Handcuff_Marker.setMarkerType(MapPOIItem.MarkerType.CustomImage);

        Handcuff_Marker.setCustomImageResourceId(R.drawable.handcuff);
        Handcuff_Marker.setCustomImageAutoscale(false);
        // mCustomMarker.setCustomImageAnchor(0.5f, 1.0f);



        }

//은신 아이템 생성후 표시 함수
private void createCapeMarker(MapView mapView) {
        Cape_Marker = new MapPOIItem();
        String name = "Custom Marker";
        Cape_Marker.setItemName(name);
        Cape_Marker.setTag(2);
        Cape_Marker.setMapPoint(Cape_Marker_POINT);


        Cape_Marker.setMarkerType(MapPOIItem.MarkerType.CustomImage);

        Cape_Marker.setCustomImageResourceId(R.drawable.cape);
        Cape_Marker.setCustomImageAutoscale(false);
        //  mCustomMarker.setCustomImageAnchor(0.5f, 1.0f);

        mapView.addPOIItem(Cape_Marker);
        }



@Override
public void onCurrentLocationUpdate(MapView mapView, MapPoint mapPoint, float v) {

/*String temp;
        temp=String.valueOf(mapPoint.getMapPointGeoCoord().latitude);
        temp+="\n";
        temp+=String.valueOf(mapPoint.getMapPointGeoCoord().longitude);
        Toast.makeText(this,temp,Toast.LENGTH_SHORT).show();*/
        //   Log.i("current position",temp);
        //  mapPoint.getMapPointGeoCoord().latitude
        //mapPoint.getMapPointGeoCoord().longitude
        tempp = MapPoint.mapPointWithGeoCoord(mapPoint.getMapPointGeoCoord().latitude, mapPoint.getMapPointGeoCoord().longitude);
        if (first == true) {
        MapPoint.mapPointWithGeoCoord(36.146451, 128.393725);  //기본 마커 위치;
        first = false;
        create_circle(circle1, tempp.getMapPointGeoCoord().latitude, tempp.getMapPointGeoCoord().longitude, 5);
           /* circle1 = new MapCircle(
                    MapPoint.mapPointWithGeoCoord(tempp.getMapPointGeoCoord().latitude,tempp.getMapPointGeoCoord().longitude), // center
                    5, // radius
                    Color.argb(128, 255, 0, 0), // strokeColor
                    Color.argb(128, 0, 255, 0) // fillColor
            );*/
        start = MapPoint.mapPointWithGeoCoord(tempp.getMapPointGeoCoord().latitude, tempp.getMapPointGeoCoord().longitude);  //기본 마커 위치;
        // handcuff[0].show_item();
        // handcuff[1].show_item();

        }
        //내 위치 중심으로 5미터 원 그리기
        delete_circle(circle1);
        circle1.setCenter(MapPoint.mapPointWithGeoCoord(tempp.getMapPointGeoCoord().latitude, tempp.getMapPointGeoCoord().longitude));
        mapView.addCircle(circle1);


        }

@Override
public void onCurrentLocationDeviceHeadingUpdate(MapView mapView, float v) {

        }

@Override
public void onCurrentLocationUpdateFailed(MapView mapView) {
        //  Log.i("fail","fail");
        }

@Override
public void onCurrentLocationUpdateCancelled(MapView mapView) {


        }



//위치 객체
public class location {
    double latitude;
    double longitude;

    public location(double a, double b) {
        this.latitude = a;
        this.longitude = b;
    }

    public double get_latitude() {
        return this.latitude;
    }

    public double get_longitude() {
        return this.longitude;
    }
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

//존 클래스
public class Zone {
    MapCircle Zone_circle;
    double latitude;
    double longitude;
    boolean occupy;
    int radius;
    int index;

    public Zone(double latitude, double longitude, int radius_, int index,int r,int g,int b) {
        this.index = index;
        this.radius = radius;
        this.occupy = false;
        this.longitude = longitude;
        this.latitude = latitude;
        this.Zone_circle = new MapCircle(
                MapPoint.mapPointWithGeoCoord(latitude, longitude), // center
                radius_, // radius
                Color.argb(128, r, g,b), // strokeColor
                Color.argb(128, r, g, b) // fillColor
        );
        Zone_circle.setTag(index);

    }

    public void show_zone() {
        mapView.addCircle(this.Zone_circle);
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
    public void Set_RGB(int r,int g,int b)
    {
        this.Zone_circle.setFillColor(Color.argb(128,r,g,b));
        this.Zone_circle.setStrokeColor(Color.argb(128,r,g,b));
    }
}



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
    public void setting_police_item(){
        Random r = new Random(); //객체생성
        int length1;
        int length2;
        int direction1;
        int direction2;
        double la=0.0;
        double lo=0.0;
        Location ttt;
        boolean reset=false;



        for(int i=0;i<5;i++)
        {


            ttt=getLocationInLatLngRad(200,cur);
            for(int j=0;j<i;j++)
            {
                if(Check_distance(ttt.getLatitude(),ttt.getLongitude(),cape[j].getLatitude(),cape[j].getLongitude())<10)
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
                cape[i]=new Item(ttt.getLatitude(),ttt.getLongitude(),i,"히히");
                zone[i]=new Zone(ttt.getLatitude(),ttt.getLongitude(),5,i,255,0,0);
                cape[i].Handcuff.setMarkerType(MapPOIItem.MarkerType.CustomImage);

                cape[i].Handcuff.setCustomImageResourceId(R.drawable.cape);
                zone[i].show_zone();
                cape[i].show_item();
                Log.i("laaaa",String.valueOf(ttt.getLatitude()));
                Log.i("loooo",String.valueOf(ttt.getLongitude()));

            }


        }



    }
    public void setting_police(){
        Random r = new Random(); //객체생성
        int length1;
        int length2;
        int direction1;
        int direction2;
        double la=0.0;
        double lo=0.0;
        Location ttt;
        boolean reset=false;



        for(int i=0;i<5;i++)
        {


            ttt=getLocationInLatLngRad(200,cur);
            for(int j=0;j<i;j++)
            {
                if(Check_distance(ttt.getLatitude(),ttt.getLongitude(),poli[j].latitude,poli[j].longitude)<10)
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
                poli[i]=new Partner("동료",ttt.getLatitude(),ttt.getLongitude(),i);

                poli[i].police_marker.setMarkerType(MapPOIItem.MarkerType.CustomImage);

               // poli[i].police_marker.setCustomImageResourceId(R.drawable.police);
                poli[i].show();

                Log.i("laaaa",String.valueOf(ttt.getLatitude()));
                Log.i("loooo",String.valueOf(ttt.getLongitude()));

            }


        }



    }
    public void setting_theif_item(){
        Random r = new Random(); //객체생성
        int length1;
        int length2;
        int direction1;
        int direction2;
        double la=0.0;
        double lo=0.0;
        boolean reset=false;
        Location ttt;



        for(int i=5;i<10;i++)
        {
            ttt=getLocationInLatLngRad(50 ,cur);
            for(int j=5;j<i;j++)
            {
                if(Check_distance(ttt.getLatitude(),ttt.getLongitude(),cape[j].getLatitude(),cape[j].getLongitude())<10)
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
                cape[i]=new Item(ttt.getLatitude(),ttt.getLongitude(),i,"히히");
                cape[i].Handcuff.setMarkerType(MapPOIItem.MarkerType.CustomImage);
                cape[i].Handcuff.setCustomImageResourceId(R.drawable.handcuff);
               cape[i].show_item();

                zone[i]=new Zone(ttt.getLatitude(),ttt.getLongitude(),5,i,255,0,0);
                zone[i].show_zone();
                Log.i("laaaa",String.valueOf(ttt.getLatitude()));
                Log.i("loooo",String.valueOf(ttt.getLongitude()));
            }


        }



    }
    public void setting_zone(){
        Random r = new Random(); //객체생성

        boolean reset=false;
        Location ttt;



        for(int i=0;i<5;i++)
        {
            ttt=getLocationInLatLngRad(190 ,cur);
            for(int j=5;j<i;j++)
            {
                if(Check_distance(ttt.getLatitude(),ttt.getLongitude(),zone_two[j].getLatitude(),zone_two[j].getLongitude())<20||Check_distance(ttt.getLatitude(),ttt.getLongitude(),cur.getLatitude(),cur.getLongitude())<20)
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


                zone_two[i]=new Zone(ttt.getLatitude(),ttt.getLongitude(),10,i,0,255,0);
                zone_two[i].show_zone();
                Log.i("laaaa",String.valueOf(ttt.getLatitude()));
                Log.i("loooo",String.valueOf(ttt.getLongitude()));
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
            police_marker.setCustomImageResourceId(R.drawable.thief);
            police_marker.setCustomImageAutoscale(false);

        }

        public void show() {
            mapView.addPOIItem(this.police_marker);
        }

        public void hide() {
            mapView.removePOIItem(this.police_marker);
        }

        public void change_marker() {
            this.police_marker.setMapPoint(MapPoint.mapPointWithCONGCoord(this.latitude, this.longitude));
        }

        public void change_location(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
    //아이템 클래스
    public class Item {
        MapPOIItem Handcuff;

        boolean used;

        boolean first; //도둑 아이템에서 처음 발견한 건지 판단 위해쓰일변수
        double latitude;
        double longitude;
        int index;
        String name;

        public Item(double latitude_, double longitude_, int index_, String name_) {
            Handcuff = new MapPOIItem();
            this.latitude = latitude_;
            this.longitude = longitude_;
            this.index = index_;
            this.name = name_;
            Handcuff.setItemName(this.name);
            Handcuff.setCustomImageResourceId(R.drawable.cape);
            Handcuff.setCustomImageAutoscale(false);

            Handcuff.setTag(index);
            Handcuff.setMapPoint(MapPoint.mapPointWithGeoCoord(this.latitude, this.longitude));


            this.used = false;
            this.first = false;


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

        public boolean isFirst() {
            return first;
        }
    }
}


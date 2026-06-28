package com.example.cvm;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapOptions;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.CustomRenderer;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.CustomMapStyleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.PolygonOptions;
import com.amap.api.maps.offlinemap.OfflineMapManager;
import com.amap.api.maps.utils.SpatialRelationUtil;
import com.amap.api.maps.utils.overlay.SmoothMoveMarker;
import com.example.cvm.Data.EventInfo;
import com.example.cvm.Data.ObstacleInfo;
import com.example.cvm.Data.RoadInfo;
import com.example.cvm.Data.RscInfo;
import com.example.cvm.Data.VehicleInfo;
import com.example.cvm.Data.WarningInfo;
import com.example.cvm.cube.CubeMapRender;
import com.example.cvm.model.MapRenderer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity {


    public static String STYLE = "style.data";
    public static String STYLE_EXTRA = "style_extra.data";
    public static Integer CURRENT_MAP_SYTLE = 1;

    public static Handler handler;

    private Timer mytimer;
    private TimerTask checkStatus;
    private Integer TTL4 = 15;  //cvm 的预警消息的TTL
    private Integer TTL5 = 15;  //cvm 的红绿灯显示的TTL
    private Integer TTL6 = 15;  //用于调整语音播报间隔时间
    private Integer TTL7 = 15;  //cvm的车辆（包括车速）和行人显示的TTL（显示在地图上）,超时未收到消息则消失
    public Integer TTLSSM = 15;     //从mqtt接收SSM消息的TTL，如果超时则将Obstacles清空
    public Integer TTLBSM = 15;     //从mqtt接收BSM消息的TTL，如果超时则将Vehicles清空
    private Integer refreshmap = 20 ;  //地图随车辆位置移动的TTL
    private Integer refreshspeed = 10;  //当前车速和建议车速的刷新时间间隔
    private boolean isWarning = false; //用于使语音播报有一定间隔时间,为true则表示正在播报或处于两次播报的间隔时间中
    private Integer lastPlayPriority = 0;   //用于存放上一次语音播报信息的优先级，如果本次优先级高于上次，则忽略时间间隔
    private Double HostVehicleHeading = 0.0;    //主车航向角，用于旋转地图方向
    public VehicleInfo hostVehicle = new VehicleInfo(); //用于存储主车信息
    private boolean isInOtherActivity = false;
    private Integer msgCount = 0;

    // UI
    private ImageView road_img;
    private ImageView car_img;
    private ImageView emergency_img;
    private ImageView warning_sign_img;
    private ImageView tip1_img;
    private ImageView tip2_img;
    private ImageView left_light_img;
    private ImageView straight_light_img;
    private ImageView right_light_img;
    private Button background_speed;
    private ImageView logo;
    private ImageView logo_new;
    private ImageView logo_background;
    private TextView host_speed;
    private TextView warning_description;
    private TextView warning_description_new;

    private TextView left_light_time;
    private TextView straight_light_time;
    private TextView right_light_time;
    private TextView advise_speed_left;
    private TextView advise_speed_straight;
    private TextView advise_speed_right;
    private TextView warning_txv;
    private ImageView warning_pic;
    private ImageView warning_pic_new;
    private ImageView warning_background;
    private DecimalFormat format = new DecimalFormat("0");

    private Button locate;
    //private Button mainMenu;
    private MapView mMapView=null;
    private AMap aMap=null;
    private AMapLocationClient mLocationClient = null;
    private AMapLocationClientOption mLocationOption = null;
    private LatLng currenLatLon = null;
    private View infoWindow = null;

    // MediaPlayer
    private MediaPlayer my_MediaPlayer;

    //移植cvm的HMI
    static int leftadvicespeed = 0;
    static int straightadvicespeed = 0;
    static int  rightadvicespeed = 0;
    static int  leftadvice= 0;
    static int  straightadvice= 0;
    static int  rightadvice= 0;
    static int  lefttime= 0;
    static int  straighttime= 0;
    static int  righttime= 0;

    //用于存放车辆和障碍物信息
    static public Vector<VehicleInfo> Vehicles = new Vector<VehicleInfo>(10);
    static public Vector<ObstacleInfo> Obstacles = new Vector<ObstacleInfo>(20);
    //在地图上的标记
    private ArrayList<MarkerOptions> markers = new ArrayList<MarkerOptions>();
    private CustomRenderer renderer = null;

    public static MainActivity instance = null;//便于CarActivity使用MainActivity中的变量

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        instance=this;  //用于CarActivity使用本Activity的变量
        hostVehicle.speed = 0.0;
        hostVehicle.heading = 0.0;
        hostVehicle.latitude = 0.0;
        hostVehicle.longitude = 0.0;
        hostVehicle.id = "";

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); //横屏显示

        //动态申请权限
        List<String> permissionList = new ArrayList<>();
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if(!permissionList.isEmpty()){
            String[] permissions =permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);
        }

        //获取地图控件引用
        mMapView = (MapView) findViewById(R.id.map);
        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        mMapView.onCreate(savedInstanceState);
        //定义了一个地图view
        mMapView = (MapView) findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);// 此方法须覆写，虚拟机需要在很多情况下保存地图绘制的当前状态。
        //初始化地图控制器对象
        aMap = mMapView.getMap();

        aMap.setInfoWindowAdapter(new InfoWindowAdapter());//主要监听
        //aMap.setOnInfoWindowClickListener(listener);//点击监听，自己看

        aMap.getUiSettings().setCompassEnabled(true);
        aMap.moveCamera(CameraUpdateFactory.zoomTo(20));
        setAssetsStyle(aMap,this,STYLE,STYLE_EXTRA);    //个性化地图
        aMap.showBuildings(true);
        aMap.moveCamera(CameraUpdateFactory.changeTilt(20));


        //定位蓝点
        MyLocationStyle myLocationStyle;
        myLocationStyle = new MyLocationStyle();//初始化定位蓝点样式类myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
        myLocationStyle.interval(2000); //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
        aMap.setMyLocationStyle(myLocationStyle);//设置定位蓝点的Style
        aMap.getUiSettings().setMyLocationButtonEnabled(false);//设置默认定位按钮是否显示，非必需设置。
        aMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
        aMap.showIndoorMap(true);     //true：显示室内地图；false：不显示；
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_MAP_ROTATE);//连续定位、且将视角移动到地图中心点，地图依照设备方向旋转，定位点会跟随设备移动。（1秒1次定位）
        myLocationStyle.showMyLocation(false);

        //定位
        mLocationClient = new AMapLocationClient(getApplicationContext());
        mLocationClient.setLocationListener(mLocationListener);
        mLocationOption = new AMapLocationClientOption();
        //设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式。
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置是否返回地址信息（默认返回地址信息）
        mLocationOption.setNeedAddress(true);
        //获取一次定位结果：
        //该方法默认为false。
        mLocationOption.setOnceLocation(true);
        //设置是否允许模拟位置,默认为false，不允许模拟位置
        mLocationOption.setMockEnable(false);
        //给定位客户端对象设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
        //启动定位
        mLocationClient.startLocation();
        Utils.clearMapAndDrawRoad(aMap);
        double zqcz[] = Utils.wgs84togcj02(118.81180046, 31.94904030);
        LatLng zqczLatLng = new LatLng(zqcz[0], zqcz[1]);
        aMap.moveCamera(CameraUpdateFactory.changeLatLng(zqczLatLng));

        //播放音频
        my_MediaPlayer = new MediaPlayer();

        //接收线程，接收cvm发的消息
        new Thread(new Runnable() {
            @Override
            public void run() {
                int port = 4041;
                byte[] buf = new byte[8192];
                DatagramPacket packet = new DatagramPacket(buf,buf.length);
                try{
                    DatagramSocket socket = new DatagramSocket(port);
                    while (true) {
                        socket.receive(packet);
                        final String data=new String(packet.getData() ,0 , packet.getLength());
                        runOnUiThread(new Runnable(){
                            @Override
                            public void run() {
                                try {
                                    decodeCVM(data);
                                    showOnMap();
                                    //showOnMapSmooth();
                                }
                                catch (JSONException e){
                                    Message msg = new Message();
                                    msg.what = 3;//解析出错
                                    handler.sendMessage(msg);
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        initUI(savedInstanceState);

        //定位按钮
        locate.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(currenLatLon!=null)
                    //aMap.moveCamera(CameraUpdateFactory.changeLatLng(currenLatLon));
                    aMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(currenLatLon,20,20,0)));
                //aMap.moveCamera(CameraUpdateFactory.zoomTo(20));
            }
        });

        //主菜单按钮
        background_speed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupMenu(background_speed);
            }
        });

        handler=new Handler(){
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                String str;
                switch (msg.what) {
                    case 6:
                        Toast.makeText(MainActivity.this, "receive udp", Toast.LENGTH_SHORT).show();
                        break;
                    case 3:
                        Toast.makeText(MainActivity.this, "JSON 解析出错", Toast.LENGTH_SHORT).show();
                        break;
                    case 10:
                        Toast.makeText(MainActivity.this, "update map", Toast.LENGTH_SHORT).show();
                        break;
                    case 2:
                        Toast.makeText(MainActivity.this, "mqtt connect failed", Toast.LENGTH_SHORT).show();
                        break;
                    case 21:
                        //Toast.makeText(MainActivity.this, "receive mqtt", Toast.LENGTH_SHORT).show();
//                        str = msg.obj.toString();
//                        try{
//
//                            mc.decodeBSM(str,Vehicles);
//                            showOnMapMqtt();
//                        }catch ( Exception e ){
//                            Toast.makeText(MainActivity.this, " JSON 解析出错", Toast.LENGTH_SHORT).show();
//                        }
                        break;
                    case 22:
                        //Toast.makeText(MainActivity.this, "receive mqtt", Toast.LENGTH_SHORT).show();
//                        str = msg.obj.toString();
//                        try{
//                            if(mc.RSUMessageType(str) == 22){
//                                TTLSSM = 30;
//                                mc.decodeSSM(str,Obstacles);
//                            }
//                            else if(mc.RSUMessageType(str) == 21)
//                            {
//                                RscInfo rscInfo = mc.decodeRSC(str);
//                                dealRscInfo(rscInfo);
//                            }
//
//                        }catch ( Exception e ){
//                            Toast.makeText(MainActivity.this, " JSON 解析出错", Toast.LENGTH_SHORT).show();
//                        }
                        try{
                                dealRscInfo((RscInfo)msg.obj);
                        }catch ( Exception e ){
                            Toast.makeText(MainActivity.this, " JSON 解析出错", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    default:
                        break;
                }
            }
        };

        // timer
        mytimer = new Timer();
        checkStatus = new TimerTask() {
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if( TTL4>0) {
                            TTL4--;
                        }
                        else {
                            //warning_pic.setImageResource(0);
                            warning_pic_new.setImageResource(0);
                            //warning_description.setText("");
                            warning_description_new.setText("");
                            emergency_img.setImageResource(0);
                        }
                        if( TTL5>0) {
                            TTL5--;
                        }
                        else {
                            left_light_img.setImageResource(0);
                            straight_light_img.setImageResource(0);
                            right_light_img.setImageResource(0);
                            left_light_time.setText("");
                            straight_light_time.setText("");
                            right_light_time.setText("");
                            advise_speed_left.setText("");
                            advise_speed_straight.setText("");
                            advise_speed_right.setText("");
                            if(TTL4<=0){
                                //logo.setImageResource(R.drawable.caic);
                                logo.setImageResource(0);
                            }
                        }
                        if( TTL6>0) {
                            TTL6--;
                        }
                        else {
                            isWarning=false;
                        }
                        if( TTL7>0) {
                            TTL7--;
                        }
                        else {
                            Utils.clearMapAndDrawRoad(aMap);     //不平滑版本取消注释
                            host_speed.setText("0\nkm/h");
                            hostVehicle.speed = 0.0;
                            hostVehicle.heading = 0.0;
                            hostVehicle.latitude = 0.0;
                            hostVehicle.longitude = 0.0;
                            hostVehicle.id = "";
//                            for (Vehicle v : vehicleMap.values())  //平滑版本
//                            {
//                                    v.smoothMoveMarker.removeMarker();
//                                    v.smoothMoveMarker.destroy();
//                                    vehicleMap.remove(v);
//                            }
                            vehicleMap.clear();
                        }
                        if( refreshmap>0){
                            refreshmap--;
                        }else{
                            refreshmap=20;
                            //if(renderer!=null)
                                //((MapRenderer)renderer).setVehicles(Vehicles);
                        }
                        if( refreshspeed>0){
                            refreshspeed--;
                        }else{
                            refreshspeed=10;
                        }
                    }
                });
            }};
        mytimer.schedule(checkStatus, 100, 100);

        //aMap.moveCamera(CameraUpdateFactory.changeBearing(90));
        ////初始化renderer
        ////renderer = new CubeMapRender(aMap);
        //VehicleInfo ve = new VehicleInfo();
        //Vehicles.add(ve);
        //renderer = new MapRenderer(aMap,this,Vehicles);

//        //设置renderer
//        aMap.setCustomRenderer(renderer);
//        aMap.moveCamera(CameraUpdateFactory.zoomTo(20));
//        aMap.moveCamera(CameraUpdateFactory.changeTilt(90));
//
//        aMap.clear();
        //aMap.setCustomRenderer(null);





//        VehicleInfo tmpv = new VehicleInfo();
//        tmpv.hostvehicle = true;
//        tmpv.heading = 90.0;
//        tmpv.longitude = 121.205716;
//        tmpv.latitude = 31.291608;
//        Vehicles.add(tmpv);
//        CustomRenderer renderer = new MapRenderer(aMap,this,Vehicles);
//        //设置renderer
//        //aMap.setCustomRenderer(null);
//        aMap.clear();
//        aMap.setCustomRenderer(renderer);
//        aMap.moveCamera(CameraUpdateFactory.zoomTo(20));
//        aMap.moveCamera(CameraUpdateFactory.changeTilt(90));

        //renderer.set
        //try{sleep(6000);}catch (Exception e){}




//        Vehicles.clear();
//        tmpv = new VehicleInfo();
//        tmpv.hostvehicle = true;
//        tmpv.heading = 90.0;
//        tmpv.longitude = 121.206716;
//        tmpv.latitude = 31.291608;
//        Vehicles.add(tmpv);
//        VehicleInfo tmpv2 = new VehicleInfo();
//        tmpv2.hostvehicle = true;
//        tmpv2.heading = 90.0;
//        tmpv2.longitude = 121.205716;
//        tmpv2.latitude = 31.291608;
//        Vehicles.add(tmpv2);
//        renderer = new MapRenderer(aMap,this,Vehicles);
//        //aMap.setCustomRenderer(null);
//        aMap.setCustomRenderer(renderer);
//        //aMap.moveCamera(CameraUpdateFactory.zoomTo(20));
//        aMap.moveCamera(CameraUpdateFactory.changeTilt(90));
//
//        //Log.e("threadID: ", Long.toString(Thread.currentThread().getId()));
//        //Log.e("",((GLSurfaceView.Renderer)renderer).getClass());//setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
//        ((GLSurfaceView.Renderer)renderer);
//        renderer.


//        LatLng ll1 = new LatLng(31.291908,121.205606);
//        LatLng ll2 = new LatLng(31.291808,121.205506);
//        LatLng ll3 = new LatLng(31.291708,121.205406);
//        // 获取轨迹坐标点
////        List<LatLng> points = new ArrayList<LatLng>();//readLatLngs();
////        points.add(ll1);
////        points.add(ll2);
////        points.add(ll3);
//
//        List<LatLng> points = readLatLngs(coords);
//
//        LatLngBounds.Builder b = LatLngBounds.builder();
//        for (int i = 0 ; i < points.size(); i++) {
//            b.include(points.get(i));
//        }
//        LatLngBounds bounds = b.build();
//        aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
//        aMap.animateCamera(CameraUpdateFactory.zoomTo(20));
//        SmoothMoveMarker smoothMarker = new SmoothMoveMarker(aMap);
//// 设置滑动的图标
//        smoothMarker.setDescriptor(BitmapDescriptorFactory.fromBitmap(Utils.zoomimg(BitmapFactory.decodeResource(getResources(), R.drawable.car_white), 44, 100, 0)));
//
//
////        LatLng drivePoint = points.get(0);
////        Pair<Integer, LatLng> pair = SpatialRelationUtil.calShortestDistancePoint(points, drivePoint);
////        points.set(pair.first, drivePoint);
////        List<LatLng> subList = points.subList(pair.first, points.size());
//
//// 设置滑动的轨迹左边点
//        //smoothMarker.setPoints(subList);
//        smoothMarker.setPoints(points);
//// 设置滑动的总时间
//        //smoothMarker.setTotalDuration(1);
//// 开始滑动
//        smoothMarker.startSmoothMove();
//
//        SmoothMoveMarker smoothMarker2 = new SmoothMoveMarker(aMap);
//        smoothMarker2.setDescriptor(BitmapDescriptorFactory.fromBitmap(Utils.zoomimg(BitmapFactory.decodeResource(getResources(), R.drawable.car_blue), 44, 100, 0)));
//        List<LatLng> points2 = readLatLngs(coords2);
//        smoothMarker2.setPoints(points2);
//        //smoothMarker.setTotalDuration(1);
//        smoothMarker2.startSmoothMove();




//        LatLng latLng = new LatLng(39.984059,116.307771);
//        Circle circle = aMap.addCircle(new CircleOptions().
//                center(latLng).
//                radius(1000).
//                fillColor(Color.argb(50, 1, 1, 1)).
//                strokeColor(Color.argb(50, 1, 1, 1)).
//                strokeWidth(15));
    }

//    private List<LatLng> readLatLngs(double[] coords) {
//        List<LatLng> points = new ArrayList<LatLng>();
//        for (int i = 0; i < coords.length; i += 2) {
//            double mpoint[] = Utils.wgs84togcj02(coords[i], coords[i + 1]);
//            //LatLng latlong = new LatLng(mpoint[0], mpoint[1]);
//            points.add(new LatLng(mpoint[0], mpoint[1]));
//        }
//        return points;
//    }

//    private double[] coords = {116.3499049793749, 39.97617053371078,
//            116.34978804908442, 39.97619854213431, 116.349674596623,
//            39.97623045687959, 116.34955525200917, 39.97626931100656,
//            116.34943728748914, 39.976285626595036, 116.34930864705592,
//            39.97628129172198, 116.34918981582413, 39.976260803938594,
//            116.34906721558868, 39.97623535890678, 116.34895185151584,
//            39.976214717128855, 116.34886935936889, 39.976280148755315,
//            116.34873954611332, 39.97628182112874, 116.34860763527448,
//            39.97626038855863, 116.3484658907622, 39.976306080391836,
//            116.34834585430347, 39.976358252119745, 116.34831166130878,
//            39.97645709321835, 116.34827643560175, 39.97655231226543,
//            116.34824186261169, 39.976658372925556, 116.34825080406188,
//            39.9767570732376, 116.34825631960626, 39.976869087779995,
//            116.34822111635201, 39.97698451764595, 116.34822901510276,
//            39.977079745909876, 116.34822234337618, 39.97718701787645,
//            116.34821627457707, 39.97730766147824, 116.34820593515043,
//            39.977417746816776, 116.34821013897107, 39.97753930933358
//            , 116.34821304891533, 39.977652209132174, 116.34820923399242,
//            39.977764016531076, 116.3482045955917, 39.97786190186833,
//            116.34822159449203, 39.977958856930286, 116.3482256370537,
//            39.97807288885813, 116.3482098441266, 39.978170063673524,
//            116.34819564465377, 39.978266951404066, 116.34820541974412,
//            39.978380693859116, 116.34819672351216, 39.97848741209275,
//            116.34816588867105, 39.978593409607825, 116.34818489339459,
//            39.97870216883567, 116.34818473446943, 39.978797222300166,
//            116.34817728972234, 39.978893492422685, 116.34816491505472,
//            39.978997133775266, 116.34815408537773, 39.97911413849568,
//            116.34812908154862, 39.97920553614499, 116.34809495907906,
//            39.979308267469264, 116.34805113358091, 39.97939658036473,
//            116.3480310509613, 39.979491697188685, 116.3480082124968,
//            39.979588529006875, 116.34799530586834, 39.979685789111635,
//            116.34798818413954, 39.979801430587926, 116.3479996420353,
//            39.97990758587515, 116.34798697544538, 39.980000796262615,
//            116.3479912988137, 39.980116318796085, 116.34799204219203,
//            39.98021407403913, 116.34798535084123, 39.980325006125696,
//            116.34797702460183, 39.98042511477518, 116.34796288754136,
//            39.98054129336908, 116.34797509821901, 39.980656820423505,
//            116.34793922017285, 39.98074576792626, 116.34792586413015,
//            39.98085620772756, 116.3478962642899, 39.98098214824056,
//            116.34782449883967, 39.98108306010269, 116.34774758827285,
//            39.98115277119176, 116.34761476652932, 39.98115430642997,
//            116.34749135408349, 39.98114590845294, 116.34734772765582,
//            39.98114337322547, 116.34722082902628, 39.98115066909245,
//            116.34708205250223, 39.98114532232906, 116.346963237696,
//            39.98112245161927, 116.34681500222743, 39.981136637759604,
//            116.34669622104072, 39.981146248090866, 116.34658043260109,
//            39.98112495260716, 116.34643721418927, 39.9811107163792,
//            116.34631638374302, 39.981085081075676, 116.34614782996252,
//            39.98108046779486, 116.3460256053666, 39.981049089345206,
//            116.34588814050122, 39.98104839362087, 116.34575119741586,
//            39.9810544889668, 116.34562885420186, 39.981040940565734,
//            116.34549232235582, 39.98105271658809, 116.34537348820508,
//            39.981052294975264, 116.3453513775533, 39.980956549928244
//    };

//    private double[] coords = {121.205606000,31.291908000,
//                                121.205606000,31.292008000,
//            121.205606000,31.292108000,
//            121.205606000,31.292208000,
//            121.205606000,31.292308000,
//            121.205606000,31.292408000,
//    };

//    private double[] coords = {118.81101467,31.949199140,
//            118.81111467,31.94919914,
//            118.81121467,31.94919914,
//            118.81131467,31.94919914,
//            118.81141467,31.94919914,
//            118.81151467,31.94919914
//    };
//
//    private double[] coords2 = {
//            118.81131467,31.94919914,
//            118.81141467,31.94919914,
//            118.81151467,31.94919914
//    };




    //解析cvm的消息，移植了cvm的场景
    private void decodeCVM(String jsonData)throws JSONException{

        int eventInfoName=0;
        int eventInfoType=0;
        int warningInfoStr=0;
        int eventFlag=0;
        int warningFlag=0;
        int roadFlag=0;
        int rscFlag=0;
        JSONObject json;

        JSONObject roads;
        JSONObject events;
        JSONObject warnings;
        JSONObject vehicles;
        JSONObject obstacles;
        JSONObject rsc;

        json = new JSONObject(jsonData);
        vehicles=json.getJSONObject("Vehicle_Info");

        Vehicles.clear();
        for(int i=0;i<6;i++) {  //i<9
            JSONObject tempObject = vehicles.getJSONObject(Integer.toString(i));
            VehicleInfo tempVehicleInfo = new VehicleInfo();
            tempVehicleInfo.hostvehicle=tempObject.getBoolean("HostVehicle");
            tempVehicleInfo.type=tempObject.getString("Type");
            tempVehicleInfo.id=tempObject.getString("ID");
            tempVehicleInfo.longitude=tempObject.getDouble("longitude");
            tempVehicleInfo.latitude=tempObject.getDouble("latitude");
            tempVehicleInfo.heading=tempObject.getDouble("Angle");
            tempVehicleInfo.speed=tempObject.getDouble("Speed");
            if(!(tempVehicleInfo.id.equals(""))){
                dealVehicleInfo(tempVehicleInfo);
            }
        }

        events=json.getJSONObject("Event_Info");
        for(int i=0;i<3;i++) {      //i<6
            JSONObject tempObject = events.getJSONObject(Integer.toString(i));
            EventInfo tempEventInfo = new EventInfo();
            tempEventInfo.id=tempObject.getInt("EventID");
            tempEventInfo.type=tempObject.getInt("EventType");
            tempEventInfo.name=tempObject.getInt("EventName");
            tempEventInfo.dx1=tempObject.getDouble("dx1");
            tempEventInfo.dx2=tempObject.getDouble("dx2");
            tempEventInfo.dy1=tempObject.getDouble("dy1");
            tempEventInfo.dy2=tempObject.getDouble("dy2");
            if(tempEventInfo.id!=0){
                eventFlag=1;
                eventInfoType=tempEventInfo.type;
                eventInfoName=tempEventInfo.name;
            }
        }

        warnings=json.getJSONObject("Warning_Info");
        for(int i=0;i<1;i++) {      //i<4
            JSONObject tempObject = warnings.getJSONObject(Integer.toString(i));
            //for(int i=0;i<1;i++) {
            //JSONObject tempObject = warnings.getJSONObject(i);
            WarningInfo tempWarningInfo = new WarningInfo();
            tempWarningInfo.priority = tempObject.getInt("WarningPriority");
            tempWarningInfo.id = tempObject.getString("WarningID");
            tempWarningInfo.str = tempObject.getInt("WarningStr");
            tempWarningInfo.level = tempObject.getInt("WarningLevel");
            if (!(tempWarningInfo.id.equals("-1"))) {
                warningFlag = 1;
                warningInfoStr = tempWarningInfo.str;
            }
        }

        roads=json.getJSONObject("Road_Info");
        for(int i=0;i<3;i++) {
            JSONObject tempObject = roads.getJSONObject(Integer.toString(i));
            //for(int i=0;i<1;i++) {
            //JSONObject tempObject = warnings.getJSONObject(i);
            RoadInfo tempRoadInfo = new RoadInfo();
            tempRoadInfo.id = tempObject.getInt("DeviceID");
            tempRoadInfo.distance = tempObject.getInt("Distance");
            tempRoadInfo.movement= tempObject.getInt("Movement");
            tempRoadInfo.signalphase = tempObject.getInt("SignalPhase");
            tempRoadInfo.remaintime = tempObject.getInt("RemainTime");
            tempRoadInfo.advice = tempObject.getInt("Advice");
            tempRoadInfo.advicespeed = tempObject.getInt("AdviceSpeed");
            if (tempRoadInfo.id != 0) {
                roadFlag = 1;
                dealRoadInfo(tempRoadInfo);
            }
        }

        obstacles=json.getJSONObject("Obstacle_Info");
        Obstacles.clear();
        Integer number;
        number=obstacles.getInt("number");
        if(number!=0){
            JSONArray objs = new JSONArray();
            objs = obstacles.getJSONArray("objs");
            for(int i=0;i<number;i++) {
                JSONObject obj=objs.getJSONObject(i);
                ObstacleInfo tempObstacleInfo = new ObstacleInfo();
                tempObstacleInfo.id=obj.getInt("id");
                tempObstacleInfo.type=obj.getInt("type");
                tempObstacleInfo.longitude=obj.getDouble("longitude");
                tempObstacleInfo.latitude=obj.getDouble("latitude");
                //tempObstacleInfo.speed=obj.getDouble("speed");
                tempObstacleInfo.heading=obj.getDouble("heading");
                dealObstacleInfo(tempObstacleInfo);
            }
        }

//        rsc = json.getJSONObject("Rsc_Info");
//        RscInfo rscInfo = new RscInfo();
//        rscInfo.info=rsc.getInt("info");
//        rscInfo.suggestion=rsc.getInt("suggestion");
//        if(rscInfo.suggestion!=14)
//        {
//            rscFlag = 1;
//        }
        if(rscFlag == 1){
            //dealRscInfo(rscInfo);
        }
        else if(eventFlag==1 && warningFlag==0){

            dealEventTips(eventInfoType,eventInfoName);
        }
        else if(eventFlag==0 && warningFlag==1){

            dealWarningTips(warningInfoStr);
        }
        else if(eventFlag==1 && warningFlag==1){
            //dealEventTips(eventInfoType,eventInfoName);
            //delay(500);
            dealWarningTips(warningInfoStr);   //优先处理warning信息
        }
        else if(eventFlag==0 && warningFlag==0 && roadFlag ==0){

        }
    }

    //处理cvm的warning
    private void dealWarningTips(int warningStr){
        TTL4=30;
        logo.setImageResource(0);
        switch (warningStr) {
            case 1:
                emergency_img.setImageResource(R.drawable.emergency);
                warning_pic.setImageResource(R.drawable.sign_up);
                warning_description.setText("前向碰撞！");
                setMediaPlayer(0x0101,30,0);
                break;
            case 2:
                emergency_img.setImageResource(R.drawable.emergency);
                warning_pic.setImageResource(R.drawable.sign__lanechange_a);
                warning_description.setText("变道预警！注意碰撞");//左侧
                setMediaPlayer(0x0102,30,0);
                break;
            case 3:
                emergency_img.setImageResource(R.drawable.emergency);
                warning_pic.setImageResource(R.drawable.sign__lanechange_b);
                warning_description.setText("变道预警！注意碰撞");//右侧
                setMediaPlayer(0x0103,30,0);
                break;
            case 4:
                emergency_img.setImageResource(R.drawable.emergency);
                warning_pic.setImageResource(R.drawable.road_reverseovertaking);
                warning_description.setText("逆向超车预警");
                setMediaPlayer(0x0104,30,0);
                break;
            case 5:
                emergency_img.setImageResource(R.drawable.emergency);
                warning_pic.setImageResource(R.drawable.sign_crossroad_a);
                warning_description.setText("交叉路口，注意碰撞");//左侧
                setMediaPlayer(0x0105,30,0);
                break;
            case 6:
                emergency_img.setImageResource(R.drawable.emergency);
                warning_pic.setImageResource(R.drawable.sign_crossroad_b);
                warning_description.setText("交叉路口，注意碰撞");//右侧
                setMediaPlayer(0x0106,30,0);
                break;
            case 7:
                emergency_img.setImageResource(R.drawable.emergency);
                warning_pic.setImageResource(R.drawable.road_left_turn);
                warning_description.setText("左转时请注意!");
                setMediaPlayer(0x0107,30,0);
                break;
            case 8:
                emergency_img.setImageResource(R.drawable.emergency);
                warning_pic.setImageResource(R.drawable.sign_up);
                warning_description.setText("弱势交通参与者！");
                setMediaPlayer(0x0108,30,0);
                break;
            case 9:
                emergency_img.setImageResource(R.drawable.emergency);
                warning_pic.setImageResource(R.drawable.sign_right_turn);
                warning_description.setText("超车预警！");//右侧车辆向左超车
                setMediaPlayer(0x0109,30,0);
                break;
            case 10:
                emergency_img.setImageResource(R.drawable.emergency);
                warning_pic.setImageResource(R.drawable.sign_left_turn);
                warning_description.setText("超车预警！");//左侧车辆向右超车
                setMediaPlayer(0x010A,30,0);
                break;
            case 11:
                emergency_img.setImageResource(R.drawable.emergency);
                warning_pic.setImageResource(R.drawable.sign_up);
                warning_description.setText("正面来车！");
                setMediaPlayer(0x010B,30,0);
                break;
            case 12:
                emergency_img.setImageResource(R.drawable.emergency);
                warning_pic.setImageResource(R.drawable.sign_up);
                warning_description.setText("前方行人穿行！");
                setMediaPlayer(0x010C,30,0);
                break;
            default:
                //Toast.makeText(MainActivity.this, "unknown warning", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    //处理cvm的event
    private void dealEventTips(int event_type,int event_name){
        TTL4=30;
        logo.setImageResource(0);
        if(event_type==1){
            switch (event_name) {
                case 2:
                    emergency_img.setImageResource(R.drawable.emergency);
                    warning_pic.setImageResource(R.drawable.road_ambulance);
                    warning_description.setText("后方紧急车辆");
                    setMediaPlayer(0x0302,30,0);
                    break;
                case 4:
                    emergency_img.setImageResource(R.drawable.emergency);
                    warning_pic.setImageResource(R.drawable.road_abnormal);
                    warning_description.setText("前方车辆故障");
                    setMediaPlayer(0x0304,30,0);
                    break;
                case 5:
                    break;
                case 6:
                    break;
                default:
                    break;
            }
        }
        else if(event_type==2){
            switch (event_name) {
                case 1:
                    emergency_img.setImageResource(R.drawable.emergency);
                    warning_pic.setImageResource(R.drawable.light_red);
                    warning_description.setText("闯红灯预警");
                    setMediaPlayer(0x0201,30,0);
                    break;
                case 3:
                    emergency_img.setImageResource(R.drawable.emergency);
                    warning_pic.setImageResource(R.drawable.sign_speedlimit30);
                    warning_description.setText("前方限速30");
                    setMediaPlayer(0x0203,30,0);
                    break;
                case 4:
                    emergency_img.setImageResource(R.drawable.emergency);
                    warning_pic.setImageResource(R.drawable.sign_jam);
                    warning_description.setText("前方拥堵");
                    setMediaPlayer(0x0204,30,0);
                    break;
                case 5:
                    warning_pic.setImageResource(R.drawable.sign_up);
                    warning_description.setText(""+"前方道路危险");
                    break;
                case 6:
                    warning_pic.setImageResource(R.drawable.sign_roadwork);
                    warning_description.setText(""+"前方道路施工");
                    break;
                case 9 :
                    emergency_img.setImageResource(R.drawable.emergency);
                    warning_pic.setImageResource(R.drawable.sign_tunnel);
                    warning_description.setText("前方隧道");
                    setMediaPlayer(0x0209,30,0);
                    break;
                case 10 :
                    emergency_img.setImageResource(R.drawable.emergency);
                    warning_pic.setImageResource(R.drawable.sign_noparking);
                    warning_description.setText("禁止停车");
                    setMediaPlayer(0x020A,30,0);
                    break;
                case 11 :
                    emergency_img.setImageResource(R.drawable.emergency);
                    warning_pic.setImageResource(R.drawable.school);
                    warning_description.setText("前方学校");
                    setMediaPlayer(0x020B,30,0);
                    break;
                case 12 :
                    emergency_img.setImageResource(R.drawable.emergency);
                    warning_pic.setImageResource(R.drawable.sign_sharpturn);
                    warning_description.setText("前方急转弯");
                    setMediaPlayer(0x020C,30,0);
                    break;
                case 13 :
                    emergency_img.setImageResource(R.drawable.emergency);
                    warning_pic.setImageResource(R.drawable.nohonking);
                    warning_description.setText("禁止鸣笛");
                    setMediaPlayer(0x020D,30,0);
                    break;
                case 14 :
                    warning_pic.setImageResource(R.drawable.gasstation);
                    warning_description.setText("前方加油站");
                    setMediaPlayer(0x020E,30,0);
                    break;
                case 15 :
                    emergency_img.setImageResource(R.drawable.emergency);
                    warning_pic.setImageResource(R.drawable.bridge);
                    warning_description.setText("前方桥梁");
                    setMediaPlayer(0x020F,30,0);
                    break;
                case 16 :
                    emergency_img.setImageResource(R.drawable.emergency);
                    warning_pic.setImageResource(R.drawable.caution);
                    warning_description.setText("注意前方道路");
                    setMediaPlayer(0x0210,30,0);
                    break;
                default:
                    break;
            }
        }
        else if(event_type==3){
            switch (event_name) {
                case 1:
                    //setMediaPlayer(0x0301);
                    break;
                case 2:
                    //setMediaPlayer(0x0302);
                    break;
                default:
                    break;
            }
        }
        else if(event_type==4){
            //
        }
        else
            Toast.makeText(MainActivity.this, "unknown event", Toast.LENGTH_SHORT).show();
    }

    //处理cvm的road信息
    private void dealRoadInfo(RoadInfo tempRoadInfo){
        TTL5=30;
        logo.setImageResource(0);
        switch (tempRoadInfo.movement)
        {
            case 2:
                switch(tempRoadInfo.signalphase){
                    case 1:
                        left_light_img.setImageResource(R.drawable.green_light);
                        break;
                    case 4:
                        left_light_img.setImageResource(R.drawable.red_light);
                        break;
                    case 2:
                        left_light_img.setImageResource(R.drawable.yellow_light);
                        break;
                    default:
                        left_light_img.setImageResource(0);
                        break;
                }
                //leftadvice=tempRoadInfo.advice;
                leftadvicespeed=tempRoadInfo.advicespeed;
                lefttime=tempRoadInfo.remaintime;
                break;
            case 1:
                switch(tempRoadInfo.signalphase){
                    case 1:
                        straight_light_img.setImageResource(R.drawable.green_light);
                        break;
                    case 4:
                        straight_light_img.setImageResource(R.drawable.red_light);
                        break;
                    case 2:
                        straight_light_img.setImageResource(R.drawable.yellow_light);
                        break;
                    default:
                        straight_light_img.setImageResource(0);
                        break;
                }
                //straightadvice=tempRoadInfo.advice;
                straightadvicespeed=tempRoadInfo.advicespeed;
                straighttime=tempRoadInfo.remaintime;
                break;
            case 3:
                switch(tempRoadInfo.signalphase){
                    case 1:
                        right_light_img.setImageResource(R.drawable.green_light);
                        break;
                    case 4:
                        right_light_img.setImageResource(R.drawable.red_light);
                        break;
                    case 2:
                        right_light_img.setImageResource(R.drawable.yellow_light);
                        break;
                    default:
                        right_light_img.setImageResource(0);
                        break;
                }
                //rightadvice=tempRoadInfo.advice;
                rightadvicespeed=tempRoadInfo.advicespeed;
                righttime=tempRoadInfo.remaintime;
                break;
            default:
                Toast.makeText(MainActivity.this, "unknown road info", Toast.LENGTH_SHORT).show();
                break;
        }

        if (lefttime < 200)
            left_light_time.setText("" + lefttime);
        else
            left_light_time.setText("");
        if (straighttime < 200)
            straight_light_time.setText("" + straighttime);
        else
            straight_light_time.setText("");
        if (righttime < 200)
            right_light_time.setText("" + righttime);
        else
            right_light_time.setText("");

        //if(refreshspeed < 1)
        //DecimalFormat format = new DecimalFormat("0");
        //{
        advise_speed_left.setText("" + format.format(leftadvicespeed) + "km/h\n ");
        advise_speed_straight.setText("" + format.format(straightadvicespeed) + "km/h\n " );
        advise_speed_right.setText("" + format.format(rightadvicespeed) + "km/h\n " );
        //}
    }

    //处理cvm的vehicle信息
    private void dealVehicleInfo(VehicleInfo tempVehicleInfo){
        Vehicles.add(tempVehicleInfo);
    }

    //处理cvm的obstacle信息
    private void dealObstacleInfo(ObstacleInfo tempObstacleInfo){
        Obstacles.add(tempObstacleInfo);
    }

    //处理cvm的rsc信息
    public void dealRscInfo(RscInfo rscInfo){
        TTL4=30;
        logo.setImageResource(0);
        //if(rscInfo.info == 1){
        switch (rscInfo.info){
            case 0:
                {
                    switch (rscInfo.suggestion) {
                        case 0:
                            emergency_img.setImageResource(R.drawable.emergency);
                            warning_pic_new.setImageResource(R.drawable.road_lanechange_a);
                            warning_description_new.setText("左后方有来车");
                            setMediaPlayer(0x0500, 90,0);
                            break;
                        case 1:
                            warning_pic_new.setImageResource(R.drawable.turn_left);
                            warning_description_new.setText("请向左变道");
                            setMediaPlayer(0x0501, 90,0);
                            break;
                        case 2:
                            warning_pic_new.setImageResource(R.drawable.turn_right);
                            warning_description_new.setText("请向右变道");
                            setMediaPlayer(0x0502, 90,0);
                            break;
                            default:
                                break;
                    }
                    break;
                }
            case 1:
                {
                    switch (rscInfo.suggestion) {
                        case 0:
                            setMediaPlayer(0x0401, 90,0);
                            break;
                        case 10:
                            setMediaPlayer(0x040A,90,3);
                            break;
                        case 11:
                            setMediaPlayer(0x040B,90,2);
                            break;
                        case 12:
                            setMediaPlayer(0x040C,90,1);
                            //isWarning = true;
                            break;
                        default:
                            break;
                    }
                    break;
                }
            default:
                break;
        }

        //}
    }

    //在地图上显示车辆和障碍物
    private void showOnMap(){
        TTL7=30;
        msgCount = (msgCount + 1)% 100;
        if( (!Vehicles.isEmpty()) || (!Obstacles.isEmpty()))
        { }
        else
        {
//            Message msg = new Message();
//            msg.what = 11;
//            handler.sendMessage(msg);
            return;
        }
            float currentZoom = aMap.getCameraPosition().zoom;
        //Toast.makeText(MainActivity.this, Float.toString(currentZoom), Toast.LENGTH_SHORT).show();
        //if(CURRENT_MAP_SYTLE == 1) {
            markers.clear();
            for (VehicleInfo vehicle : Vehicles) {
                if (vehicle.type.equals("Car")) {
                    double lat = vehicle.latitude;
                    double lon = vehicle.longitude;
                    double mpoint[] = Utils.wgs84togcj02(lon, lat);
                    LatLng latlong = new LatLng(mpoint[0], mpoint[1]);
                    Marker marker = null;
                    Bitmap bitmapcar = null;
                    if (vehicle.hostvehicle == true) {
                        hostVehicle = vehicle;
                        HostVehicleHeading = vehicle.heading;
                        Integer width = Integer.parseInt(new java.text.DecimalFormat("0").format(20*currentZoom-356));
                        Integer length = Integer.parseInt(new java.text.DecimalFormat("0").format(width*2.27));
                        //随动
                        bitmapcar = Utils.zoomimg(BitmapFactory.decodeResource(getResources(), R.drawable.car_blue), width, length, 0);
                        //不随动
                        //bitmapcar = Utils.zoomimg(BitmapFactory.decodeResource(getResources(), R.drawable.car_blue), width, length, new BigDecimal(HostVehicleHeading % 360.0).floatValue());
                    }//new BigDecimal(tempVehicleInfo.heading % 360.0).floatValue()
                    else {
                        Integer width = Integer.parseInt(new java.text.DecimalFormat("0").format(20*currentZoom-356));
                        Integer length = Integer.parseInt(new java.text.DecimalFormat("0").format(width*2.27));
                        bitmapcar = Utils.zoomimg(BitmapFactory.decodeResource(getResources(), R.drawable.car_red), width, length, new BigDecimal((vehicle.heading - HostVehicleHeading) % 360.0).floatValue());
                    }
                    BitmapDescriptor bitmapDescriptorCar = BitmapDescriptorFactory.fromBitmap(bitmapcar);
                    markers.add(new MarkerOptions().position(latlong).draggable(false)
                            .icon(bitmapDescriptorCar));
                    if (vehicle.hostvehicle == true) {

                        if (refreshspeed < 1) {
                            host_speed.setText("" + format.format(vehicle.speed * 3.6) + "\nkm/h");
                        }

                        //if (msgCount % 20 == 0) {
                        //if (refreshmap == 0) {
                        float rotate = new BigDecimal(Math.floor(vehicle.heading % 360.0)).floatValue();
        //                aMap.moveCamera(CameraUpdateFactory.changeBearing(rotate));
                        //}
                        //if (msgCount == 1) {
                        if (refreshmap == 0) {
                            //aMap.moveCamera(CameraUpdateFactory.changeLatLng(latlong));
                            //随动
                            aMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(latlong,currentZoom,20,rotate)),250,null);
                            //aMap.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.builder().target(latlong).tilt(20).bearing(rotate).zoom(20).build()),250,null);
                        }
                    }
                }
            }

            for (ObstacleInfo obstacle : Obstacles) {
                //ArrayList<Marker> pedestrians = new ArrayList<Marker>();
                double lat = obstacle.latitude;
                double lon = obstacle.longitude;
                //if(lat > 31.94905000)
                    //lat = lat - 0.000032;
                double mpoint[] = Utils.wgs84togcj02(lon, lat);
                int type = obstacle.type;
                LatLng latlong = new LatLng(mpoint[0], mpoint[1]);
                Marker marker = null;
                if (type == 3)     //行人
                {
                    //double heading = (obstacle.heading - HostVehicleHeading + 360.0 ) % 360.0;
                    Bitmap bitmapwarning = null;
                    Integer width = Integer.parseInt(new java.text.DecimalFormat("0").format(20*currentZoom-330));
                    Integer length = Integer.parseInt(new java.text.DecimalFormat("0").format(width*1.43));
//                    if(heading > 0 && heading <180)
//                        bitmapwarning = Utils.zoomimg(BitmapFactory.decodeResource(getResources(), R.drawable.warningpedestrian_right), width, length, 0);
//                    else
//                        bitmapwarning = Utils.zoomimg(BitmapFactory.decodeResource(getResources(), R.drawable.warningpedestrian_left), width, length, 0);
                    bitmapwarning = Utils.zoomimg(BitmapFactory.decodeResource(getResources(), R.drawable.warningpedestrian), width, length, 0);
                    BitmapDescriptor bitmapDescriptorwarning = BitmapDescriptorFactory.fromBitmap(bitmapwarning);
                    markers.add(new MarkerOptions().position(latlong).draggable(false)
                            .icon(bitmapDescriptorwarning).title("p"));
                } else if (type == 1)  //车辆
                {
                    double heading = obstacle.heading;
                    Integer width = Integer.parseInt(new java.text.DecimalFormat("0").format(20*currentZoom-360));
                    Integer length = Integer.parseInt(new java.text.DecimalFormat("0").format(width*2.25));
                    if(lat > 31.94905000)
                    {
                        if(heading > 80 && heading < 100)
                            heading = 87;
                        else if(heading < 280 && heading > 260)
                            heading = 267;
//                        if( heading > 60 && heading < 120)
//                        {
//                            if( lon > 118.81100046 && lon < 118.81230046)
//                                lat = 0.043333333333*lon + 26.8007076134;
//                        }
//                        if(heading > 240 && heading < 300)
//                        {
//                            if( lon > 118.81100046 && lon < 118.81230046)
//                                lat = 0.043333333333*lon + 26.8007336134;
//                        }
                    }
                    //double fixedpoint[] = Utils.wgs84togcj02(lon, lat);
                    //LatLng fixedLatLong = new LatLng(fixedpoint[0], fixedpoint[1]);
                    //随动
                    Bitmap bitmap2 = Utils.zoomimg(BitmapFactory.decodeResource(getResources(), R.drawable.car_white), width, length, new BigDecimal((heading - HostVehicleHeading) % 360.0).floatValue());
                    //不随动
                    //Bitmap bitmap2 = Utils.zoomimg(BitmapFactory.decodeResource(getResources(), R.drawable.car_white), width, length, new BigDecimal(heading % 360.0).floatValue());
                    BitmapDescriptor bitmapDescriptor2 = BitmapDescriptorFactory.fromBitmap(bitmap2);
                    markers.add(new MarkerOptions().position(latlong).draggable(false)
                            .icon(bitmapDescriptor2));
                }
//            //        else if(type==60)  //紧急车辆
//            //        {
//            //            double heading=tempObstacleInfo.heading;
//            //            Bitmap bitmap4 = zoomimg(BitmapFactory.decodeResource(getResources(), R.drawable.car_red),54,100,new BigDecimal(heading % 360.0).floatValue());
//            //            BitmapDescriptor bitmapDescriptor4 = BitmapDescriptorFactory.fromBitmap(bitmap4);
//            //            OverlayOptions ooptions = new MarkerOptions()//
//            //                    .position(latlong)// 设置marker的位置
//            //                    .icon(bitmapDescriptor4)// 设置marker的图标
//            //                    //.rotate(new BigDecimal(-(heading % 360.0)).floatValue())
//            //                    .zIndex(20);// 設置marker的所在層級
//            //            marker = (Marker) mBaiduMap.addOverlay(ooptions);
//            //        }
                else if(type==2)  //自行车
                {
                    //double heading = (obstacle.heading - HostVehicleHeading + 360.0 ) % 360.0;
                    Integer width = Integer.parseInt(new java.text.DecimalFormat("0").format(20*currentZoom-350));
                    Integer length = width;
                    Bitmap bitmap3 = null;
//                    if(heading > 0 && heading <180)
//                        bitmap3 = Utils.zoomimg(BitmapFactory.decodeResource(getResources(), R.drawable.bike_right), width, length, 0);
//                    else
//                        bitmap3 = Utils.zoomimg(BitmapFactory.decodeResource(getResources(), R.drawable.bike_left), width, length, 0);
                    bitmap3 = Utils.zoomimg(BitmapFactory.decodeResource(getResources(), R.drawable.bike), width, length, 0);
                    BitmapDescriptor bitmapDescriptor3 = BitmapDescriptorFactory.fromBitmap(bitmap3);
                    //            OverlayOptions ooptions = new MarkerOptions()//
                    //                    .position(latlong)// 设置marker的位置
                    //                    .icon(bitmapDescriptor3)// 设置marker的图标
                    //                    .zIndex(20);// 設置marker的所在層級
                    //            marker = (Marker) mBaiduMap.addOverlay(ooptions);
                    markers.add(new MarkerOptions().position(latlong).draggable(false)
                            .icon(bitmapDescriptor3));
                }
                else if(type==5) //卡车
                {
                    double heading = obstacle.heading;
                    Integer width = Integer.parseInt(new java.text.DecimalFormat("0").format(20*currentZoom-360));
                    Integer length = Integer.parseInt(new java.text.DecimalFormat("0").format(width*2.4));
                    if(lat > 31.94905000)
                    {
                        if(heading > 80 && heading < 100)
                            heading = 87;
                        else if(heading < 280 && heading > 260)
                            heading = 267;
    //                        if( heading > 60 && heading < 120)
    //                        {
    //                            if( lon > 118.81100046 && lon < 118.81230046)
    //                                lat = 0.043333333333*lon + 26.8007076134;
    //                        }
    //                        if(heading > 240 && heading < 300)
    //                        {
    //                            if( lon > 118.81100046 && lon < 118.81230046)
    //                                lat = 0.043333333333*lon + 26.8007336134;
    //                        }
                    }
                    //double fixedpoint[] = Utils.wgs84togcj02(lon, lat);
                    //LatLng fixedLatLong = new LatLng(fixedpoint[0], fixedpoint[1]);
                    //随动
                    Bitmap bitmap4 = Utils.zoomimg(BitmapFactory.decodeResource(getResources(), R.drawable.track), width, length, new BigDecimal((heading - HostVehicleHeading) % 360.0).floatValue());
                    //不随动
                    //Bitmap bitmap4 = Utils.zoomimg(BitmapFactory.decodeResource(getResources(), R.drawable.track), width, length, new BigDecimal(heading % 360.0).floatValue());
                    BitmapDescriptor bitmapDescriptor4 = BitmapDescriptorFactory.fromBitmap(bitmap4);
                    markers.add(new MarkerOptions().position(latlong).draggable(false)
                            .icon(bitmapDescriptor4));
                }
            }
            if (isInOtherActivity)   //如果正在其他Activity中，mMapView处于pause状态，此时不更新地图
                return;
            //aMap.clear();
            Utils.clearMapAndDrawRoad(aMap);
            ArrayList<Marker> tmpMarkers = aMap.addMarkers(markers, false);

//            for(Marker maker:tmpMarkers){
//                if(maker.getTitle()!= null)
//                    maker.showInfoWindow();
//            }

//        }
//        else if(CURRENT_MAP_SYTLE == 0){
//
//            //设置renderer
//            aMap.setCustomRenderer(null);
//            aMap.clear();
//                    Vehicles.clear();
//
//            VehicleInfo tmpv = new VehicleInfo();
//        tmpv.hostvehicle = true;
//        tmpv.heading = 90.0;
//        tmpv.longitude = 121.206716;
//        tmpv.latitude = 31.291608;
//        Vehicles.add(tmpv);
//           CustomRenderer renderer = new MapRenderer(aMap,getApplicationContext(),Vehicles);
//            aMap.setCustomRenderer(renderer);
//            aMap.moveCamera(CameraUpdateFactory.zoomTo(20));
//            aMap.moveCamera(CameraUpdateFactory.changeTilt(90));
//        }
    }

    List<Vehicle> vehicleList = new ArrayList<Vehicle>();
    Map<Integer,Vehicle> vehicleMap = new HashMap<Integer,Vehicle>();

    //在地图上显示车辆和障碍物（尝试平滑）
    private void showOnMapSmooth(){

        TTL7=30;
        msgCount = (msgCount + 1)% 100;
        for(Vehicle v:vehicleMap.values())
            v.tag = false;
        if( (!Vehicles.isEmpty()) || (!Obstacles.isEmpty()))
        { }
        else
        {
//            Message msg = new Message();
//            msg.what = 11;
//            handler.sendMessage(msg);
            return;
        }
        float currentZoom = aMap.getCameraPosition().zoom;
        //Toast.makeText(MainActivity.this, Float.toString(currentZoom), Toast.LENGTH_SHORT).show();
        //if(CURRENT_MAP_SYTLE == 1) {
        markers.clear();
        for (VehicleInfo vehicle : Vehicles) {
            if (vehicle.type.equals("Car")) {
                Integer id = Integer.parseInt(vehicle.id);
                double lat = vehicle.latitude;
                double lon = vehicle.longitude;
                double mpoint[] = Utils.wgs84togcj02(lon, lat);
                LatLng latlong = new LatLng(mpoint[0], mpoint[1]);
                Marker marker = null;
                Bitmap bitmapcar = null;
                if (vehicle.hostvehicle == true) {
                    Integer width = Integer.parseInt(new java.text.DecimalFormat("0").format(20*currentZoom-356));
                    Integer length = Integer.parseInt(new java.text.DecimalFormat("0").format(width*2.27));
                    bitmapcar = Utils.zoomimg(BitmapFactory.decodeResource(getResources(), R.drawable.car_blue), width, length, 0);
                }//new BigDecimal(tempVehicleInfo.heading % 360.0).floatValue()
                else {
                    Integer width = Integer.parseInt(new java.text.DecimalFormat("0").format(20*currentZoom-356));
                    Integer length = Integer.parseInt(new java.text.DecimalFormat("0").format(width*2.27));
                    bitmapcar = Utils.zoomimg(BitmapFactory.decodeResource(getResources(), R.drawable.car_red), width, length, new BigDecimal((vehicle.heading - HostVehicleHeading) % 360.0).floatValue());
                }
                BitmapDescriptor bitmapDescriptorCar = BitmapDescriptorFactory.fromBitmap(bitmapcar);
                markers.add(new MarkerOptions().position(latlong).draggable(false)
                        .icon(bitmapDescriptorCar));
                if (vehicle.hostvehicle == true) {
                    hostVehicle = vehicle;
                    if (refreshspeed < 1) {
                        host_speed.setText("" + format.format(vehicle.speed * 3.6) + "\nkm/h");
                    }
                    HostVehicleHeading = vehicle.heading;
                    //if (msgCount % 20 == 0) {
                    //if (refreshmap == 0) {
                    float rotate = new BigDecimal(Math.floor(vehicle.heading % 360.0)).floatValue();
                    //                aMap.moveCamera(CameraUpdateFactory.changeBearing(rotate));
                    //}
                    //if (msgCount == 1) {
                    if (refreshmap == 0) {
                        //aMap.moveCamera(CameraUpdateFactory.changeLatLng(latlong));

                        aMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(latlong,currentZoom,20,rotate)),250,null);
                        //aMap.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.builder().target(latlong).tilt(20).bearing(rotate).zoom(20).build()),250,null);
                    }
                    if (vehicleMap.containsKey(id)){
                        Vehicle hv = vehicleMap.get(id);
                        hv.tag = true;
                        hv.points.add(latlong);
                        if(hv.points.size()==3)
                            hv.points.remove(0);
                        hv.smoothMoveMarker.removeMarker();
                        hv.smoothMoveMarker = new SmoothMoveMarker(aMap);
                        hv.smoothMoveMarker.setPoints(hv.points);
                        hv.smoothMoveMarker.setTotalDuration(1);
                        hv.smoothMoveMarker.setDescriptor(BitmapDescriptorFactory.fromBitmap(bitmapcar));
                    }
                    else{
                        Vehicle hv = new Vehicle();
                        hv.tag = true;
                        hv.points = new ArrayList<LatLng>();
                        hv.points.add(latlong);
                        hv.smoothMoveMarker = new SmoothMoveMarker(aMap);
                        hv.smoothMoveMarker.setPoints(hv.points);
                        hv.smoothMoveMarker.setTotalDuration(1);
                        hv.smoothMoveMarker.setDescriptor(BitmapDescriptorFactory.fromBitmap(bitmapcar));
                        vehicleMap.put(id, hv);
                    }
                }
            }
        }

        for (ObstacleInfo obstacle : Obstacles) {
            //ArrayList<Marker> pedestrians = new ArrayList<Marker>();
            Integer id = obstacle.id;
            double lat = obstacle.latitude;
            double lon = obstacle.longitude;
            //if(lat > 31.94905000)
            //lat = lat - 0.000032;
            double mpoint[] = Utils.wgs84togcj02(lon, lat);
            int type = obstacle.type;
            LatLng latlong = new LatLng(mpoint[0], mpoint[1]);
            Marker marker = null;
            if (type == 3)     //行人
            {
                double heading = (obstacle.heading - HostVehicleHeading + 360.0 ) % 360.0;
                Bitmap bitmapwarning = null;
                Integer width = Integer.parseInt(new java.text.DecimalFormat("0").format(20*currentZoom-330));
                Integer length = Integer.parseInt(new java.text.DecimalFormat("0").format(width*1.43));
                if(heading > 0 && heading <180)
                    bitmapwarning = Utils.zoomimg(BitmapFactory.decodeResource(getResources(), R.drawable.warningpedestrian_right), width, length, 0);
                else
                    bitmapwarning = Utils.zoomimg(BitmapFactory.decodeResource(getResources(), R.drawable.warningpedestrian_left), width, length, 0);
                BitmapDescriptor bitmapDescriptorwarning = BitmapDescriptorFactory.fromBitmap(bitmapwarning);
                markers.add(new MarkerOptions().position(latlong).draggable(false)
                        .icon(bitmapDescriptorwarning).title("p"));
            } else if (type == 1)  //车辆
            {
                double heading = obstacle.heading;
                Integer width = Integer.parseInt(new java.text.DecimalFormat("0").format(20*currentZoom-360));
                Integer length = Integer.parseInt(new java.text.DecimalFormat("0").format(width*2.25));
                if(lat > 31.94905000)
                {
                    if(heading > 80 && heading < 100)
                        heading = 87;
                    else if(heading < 280 && heading > 260)
                        heading = 267;
//                        if( heading > 60 && heading < 120)
//                        {
//                            if( lon > 118.81100046 && lon < 118.81230046)
//                                lat = 0.043333333333*lon + 26.8007076134;
//                        }
//                        if(heading > 240 && heading < 300)
//                        {
//                            if( lon > 118.81100046 && lon < 118.81230046)
//                                lat = 0.043333333333*lon + 26.8007336134;
//                        }
                }
                //double fixedpoint[] = Utils.wgs84togcj02(lon, lat);
                //LatLng fixedLatLong = new LatLng(fixedpoint[0], fixedpoint[1]);
                //Bitmap bitmap2 = Utils.zoomimg(BitmapFactory.decodeResource(getResources(), R.drawable.car_white), width, length, new BigDecimal((heading - HostVehicleHeading) % 360.0).floatValue());
                Bitmap bitmap2 = Utils.zoomimg(BitmapFactory.decodeResource(getResources(), R.drawable.car_white), width, length, 0);
                BitmapDescriptor bitmapDescriptor2 = BitmapDescriptorFactory.fromBitmap(bitmap2);
                markers.add(new MarkerOptions().position(latlong).draggable(false)
                        .icon(bitmapDescriptor2));
                if (vehicleMap.containsKey(id)){
                    Vehicle ov = vehicleMap.get(id);
                    ov.tag = true;
                    ov.points.add(latlong);
                    if(ov.points.size()==3)
                        ov.points.remove(0);
                    ov.smoothMoveMarker.removeMarker();
                    ov.smoothMoveMarker = new SmoothMoveMarker(aMap);
                    ov.smoothMoveMarker.setPoints(ov.points);
                    ov.smoothMoveMarker.setTotalDuration(1);
                    ov.smoothMoveMarker.setDescriptor(BitmapDescriptorFactory.fromBitmap(bitmap2));
                }
                else{
                    Vehicle ov = new Vehicle();
                    ov.tag = true;
                    ov.points = new ArrayList<LatLng>();
                    ov.points.add(latlong);
                    ov.smoothMoveMarker = new SmoothMoveMarker(aMap);
                    ov.smoothMoveMarker.setPoints(ov.points);
                    ov.smoothMoveMarker.setTotalDuration(1);
                    ov.smoothMoveMarker.setDescriptor(BitmapDescriptorFactory.fromBitmap(bitmap2));
                    vehicleMap.put(id, ov);
                }
            }
//            //        else if(type==60)  //紧急车辆
//            //        {
//            //            double heading=tempObstacleInfo.heading;
//            //            Bitmap bitmap4 = zoomimg(BitmapFactory.decodeResource(getResources(), R.drawable.car_red),54,100,new BigDecimal(heading % 360.0).floatValue());
//            //            BitmapDescriptor bitmapDescriptor4 = BitmapDescriptorFactory.fromBitmap(bitmap4);
//            //            OverlayOptions ooptions = new MarkerOptions()//
//            //                    .position(latlong)// 设置marker的位置
//            //                    .icon(bitmapDescriptor4)// 设置marker的图标
//            //                    //.rotate(new BigDecimal(-(heading % 360.0)).floatValue())
//            //                    .zIndex(20);// 設置marker的所在層級
//            //            marker = (Marker) mBaiduMap.addOverlay(ooptions);
//            //        }
            else if(type==2)  //自行车
            {
                double heading = (obstacle.heading - HostVehicleHeading + 360.0 ) % 360.0;
                Integer width = Integer.parseInt(new java.text.DecimalFormat("0").format(20*currentZoom-350));
                Integer length = width;
                Bitmap bitmap3 = null;
                if(heading > 0 && heading <180)
                    bitmap3 = Utils.zoomimg(BitmapFactory.decodeResource(getResources(), R.drawable.bike_right), width, length, 0);
                else
                    bitmap3 = Utils.zoomimg(BitmapFactory.decodeResource(getResources(), R.drawable.bike_left), width, length, 0);
                BitmapDescriptor bitmapDescriptor3 = BitmapDescriptorFactory.fromBitmap(bitmap3);
                //            OverlayOptions ooptions = new MarkerOptions()//
                //                    .position(latlong)// 设置marker的位置
                //                    .icon(bitmapDescriptor3)// 设置marker的图标
                //                    .zIndex(20);// 設置marker的所在層級
                //            marker = (Marker) mBaiduMap.addOverlay(ooptions);
                markers.add(new MarkerOptions().position(latlong).draggable(false)
                        .icon(bitmapDescriptor3));
            }
            else if(type==4) //卡车
            {
                double heading = obstacle.heading;
                Integer width = Integer.parseInt(new java.text.DecimalFormat("0").format(20*currentZoom-360));
                Integer length = Integer.parseInt(new java.text.DecimalFormat("0").format(width*2.4));
                if(lat > 31.94905000)
                {
                    if(heading > 80 && heading < 100)
                        heading = 87;
                    else if(heading < 280 && heading > 260)
                        heading = 267;
                    //                        if( heading > 60 && heading < 120)
                    //                        {
                    //                            if( lon > 118.81100046 && lon < 118.81230046)
                    //                                lat = 0.043333333333*lon + 26.8007076134;
                    //                        }
                    //                        if(heading > 240 && heading < 300)
                    //                        {
                    //                            if( lon > 118.81100046 && lon < 118.81230046)
                    //                                lat = 0.043333333333*lon + 26.8007336134;
                    //                        }
                }
                //double fixedpoint[] = Utils.wgs84togcj02(lon, lat);
                //LatLng fixedLatLong = new LatLng(fixedpoint[0], fixedpoint[1]);
                Bitmap bitmap4 = Utils.zoomimg(BitmapFactory.decodeResource(getResources(), R.drawable.track), width, length, new BigDecimal((heading - HostVehicleHeading) % 360.0).floatValue());
                BitmapDescriptor bitmapDescriptor4 = BitmapDescriptorFactory.fromBitmap(bitmap4);
                markers.add(new MarkerOptions().position(latlong).draggable(false)
                        .icon(bitmapDescriptor4));
            }
        }
        for (Vehicle v : vehicleMap.values())
        {
            if(v.tag == false){
                v.smoothMoveMarker.removeMarker();
                v.smoothMoveMarker.destroy();
                vehicleMap.remove(v);
            }
        }
        if (isInOtherActivity)   //如果正在其他Activity中，mMapView处于pause状态，此时不更新地图
            return;
        //Toast.makeText(MainActivity.this, vehicleMap.size()+"", Toast.LENGTH_SHORT).show();
        for(Vehicle v : vehicleMap.values())
            v.smoothMoveMarker.startSmoothMove();
        //aMap.clear();
        //Utils.clearMapAndDrawRoad(aMap);
        //ArrayList<Marker> tmpMarkers = aMap.addMarkers(markers, false);

//            for(Marker maker:tmpMarkers){
//                if(maker.getTitle()!= null)
//                    maker.showInfoWindow();
//            }

//        }
//        else if(CURRENT_MAP_SYTLE == 0){
//
//            //设置renderer
//            aMap.setCustomRenderer(null);
//            aMap.clear();
//                    Vehicles.clear();
//
//            VehicleInfo tmpv = new VehicleInfo();
//        tmpv.hostvehicle = true;
//        tmpv.heading = 90.0;
//        tmpv.longitude = 121.206716;
//        tmpv.latitude = 31.291608;
//        Vehicles.add(tmpv);
//           CustomRenderer renderer = new MapRenderer(aMap,getApplicationContext(),Vehicles);
//            aMap.setCustomRenderer(renderer);
//            aMap.moveCamera(CameraUpdateFactory.zoomTo(20));
//            aMap.moveCamera(CameraUpdateFactory.changeTilt(90));
//        }
    }

    private void initUI(Bundle savedInstanceState){
        road_img = (ImageView)findViewById(R.id.road_img);
        car_img = (ImageView)findViewById(R.id.car_img);
        emergency_img = (ImageView)findViewById(R.id.emergency_img);
        warning_sign_img = (ImageView)findViewById(R.id.warning_sign_img);
        tip1_img=(ImageView)findViewById(R.id.tip1_img);
        tip2_img=(ImageView)findViewById(R.id.tip2_img);
        left_light_img=(ImageView)findViewById(R.id.left_light_img);
        straight_light_img=(ImageView)findViewById(R.id.straight_light_img);
        right_light_img=(ImageView)findViewById(R.id.right_light_img);
        warning_pic=(ImageView)findViewById(R.id.warning_pic);
        warning_pic_new=(ImageView)findViewById(R.id.warning_pic_new);

        left_light_time=(TextView)findViewById(R.id.left_light_time);
        straight_light_time=(TextView)findViewById(R.id.straight_light_time);
        right_light_time=(TextView)findViewById(R.id.right_light_time);
        advise_speed_left=(TextView)findViewById(R.id.advise_speed_left);
        advise_speed_straight=(TextView)findViewById(R.id.advise_speed_straight);
        advise_speed_right=(TextView)findViewById(R.id.advise_speed_right);
        background_speed=(Button)findViewById(R.id.background_speed);
        logo=(ImageView)findViewById(R.id.logo_img);
        logo_new=(ImageView)findViewById(R.id.logo_img_new);
        logo_background=(ImageView)findViewById(R.id.logo_background);


        host_speed = (TextView)findViewById(R.id.host_speed);
        warning_txv = (TextView)findViewById(R.id.warning_txv);
        warning_description = (TextView)findViewById(R.id.warning_description);
        warning_description_new = (TextView)findViewById(R.id.warning_description_new);


        locate = (Button)findViewById(R.id.locate);
        //mainMenu = (Button)findViewById(R.id.mainMenu);
        AssetManager mgr = getAssets();
        //根据路径得到Typeface
        Typeface tf = Typeface.createFromAsset(mgr, "GOTHIC.TTF");
        warning_description.setTypeface(tf);
        advise_speed_left.setTypeface(tf);
        advise_speed_straight.setTypeface(tf);
        advise_speed_right.setTypeface(tf);
        left_light_time.setTypeface(tf);
        straight_light_time.setTypeface(tf);
        right_light_time.setTypeface(tf);
        //host_speed.setTypeface(tf);
//        logo.setImageResource(R.drawable.caic);
//        logo_new.setImageResource(R.drawable.caicblack);
        logo.setImageResource(0);
        logo_new.setImageResource(0);
        //logo_background.setImageResource(R.drawable.white_bkg);
        //logo_background.setAlpha(100);
        //warning_pic_new.setImageResource(R.drawable.car);

        host_speed.setText("0\nkm/h");

        infoWindow = LayoutInflater.from(this).inflate(R.layout.infowindow, null);
    }

    // set Media Player
    private void setMediaPlayer(int type, int duration, int priority){
        if(!isWarning && !my_MediaPlayer.isPlaying())       //如果未处于预警中，且语音未正在播报
        {
            isWarning = true;
            TTL6 = duration;
            lastPlayPriority = priority;
        }else if(isWarning && priority > lastPlayPriority)  //如果处于预警中，且来了优先级更高的预警
        {
            if(my_MediaPlayer.isPlaying())  //如果语音正在播报，则等待该条语音播报完毕
                return;
            else                            //如果语音未正在播报，则忽略预警播报间隔直接播放优先级更高的预警
            {
                isWarning = true;
                TTL6 = duration;
                lastPlayPriority = priority;
            }
        }
        else                                //如果正在预警且没有优先级更高的预警，则以一定的时间间隔播报预警
            return;
        my_MediaPlayer.reset();
        Uri setDataSourceuri = Uri.parse("android.resource://com.example.cvm/"+R.raw.welcome);
        switch(type){
            case 0x0101:
                setDataSourceuri = Uri.parse("android.resource://com.example.cvm/"+R.raw.forward);
                break;
            case 0x0102:
                setDataSourceuri = Uri.parse("android.resource://com.example.cvm/"+R.raw.lanechange_left);
                break;
            case 0x0103:
                setDataSourceuri = Uri.parse("android.resource://com.example.cvm/"+R.raw.lanechange_right);
                break;
            case 0x0104:
                setDataSourceuri = Uri.parse("android.resource://com.example.cvm/"+R.raw.dnpw);
                break;
            case 0x0105:
                setDataSourceuri = Uri.parse("android.resource://com.example.cvm/"+R.raw.icw_left);
                break;
            case 0x0106:
                setDataSourceuri = Uri.parse("android.resource://com.example.cvm/"+R.raw.icw_right);
                break;
            case 0x0107:
                setDataSourceuri = Uri.parse("android.resource://com.example.cvm/"+R.raw.leftturnassistance);
                break;
            case 0x0108:
                setDataSourceuri = Uri.parse("android.resource://com.example.cvm/"+R.raw.vulnerableparticipants);
                break;
            case 0x0109:
                setDataSourceuri = Uri.parse("android.resource://com.example.cvm/"+R.raw.passwarning);
                break;
            case 0x010A:
                setDataSourceuri = Uri.parse("android.resource://com.example.cvm/"+R.raw.passwarning);
                break;
            case 0x010B:
                setDataSourceuri = Uri.parse("android.resource://com.example.cvm/"+R.raw.incoming);
                break;
            case 0x010C:
                setDataSourceuri = Uri.parse("android.resource://com.example.cvm/"+R.raw.pedestrian);
                break;
            case 0x0201:
                setDataSourceuri = Uri.parse("android.resource://com.example.cvm/"+R.raw.redlight);
                break;
            case 0x0203:
                setDataSourceuri = Uri.parse("android.resource://com.example.cvm/"+R.raw.speedlimit);
                break;
            case 0x0204:
                setDataSourceuri = Uri.parse("android.resource://com.example.cvm/"+R.raw.trafficjam);
                break;
            case 0x0209:
                setDataSourceuri = Uri.parse("android.resource://com.example.cvm/"+R.raw.tunnel);
                break;
            case 0x020A:
                setDataSourceuri = Uri.parse("android.resource://com.example.cvm/"+R.raw.noparking);
                break;
            case 0x020B:
                setDataSourceuri = Uri.parse("android.resource://com.example.cvm/"+R.raw.school);
                break;
            case 0x020C:
                setDataSourceuri = Uri.parse("android.resource://com.example.cvm/"+R.raw.sharpturn);
                break;
            case 0x020D:
                setDataSourceuri = Uri.parse("android.resource://com.example.cvm/"+R.raw.nohonking);
                break;
            case 0x020E:
                setDataSourceuri = Uri.parse("android.resource://com.example.cvm/"+R.raw.gasstation);
                break;
            case 0x020F:
                setDataSourceuri = Uri.parse("android.resource://com.example.cvm/"+R.raw.bridge);
                break;
            case 0x0210:
                setDataSourceuri = Uri.parse("android.resource://com.example.cvm/"+R.raw.caution);
                break;
            case 0x0302:
                setDataSourceuri = Uri.parse("android.resource://com.example.cvm/"+R.raw.emergencyvehicle);
                break;
            case 0x0304:
                setDataSourceuri = Uri.parse("android.resource://com.example.cvm/"+R.raw.breakdown);
                break;
            case 0x0401:
                warning_pic_new.setImageResource(R.drawable.green_light);
                warning_description_new.setText("\n请直行汇入");
                setDataSourceuri = Uri.parse("android.resource://com.example.cvm/"+R.raw.gostraight);
                break;
            case 0x040A:
                emergency_img.setImageResource(R.drawable.emergency);
                warning_pic_new.setImageResource(R.drawable.red_light);
                warning_description_new.setText("\n请停车等待");
                setDataSourceuri = Uri.parse("android.resource://com.example.cvm/"+R.raw.stop);
                break;
            case 0x040B:
                emergency_img.setImageResource(R.drawable.emergency);
                warning_pic_new.setImageResource(R.drawable.slow);
                warning_description_new.setText("\n请减速慢行");
                setDataSourceuri = Uri.parse("android.resource://com.example.cvm/"+R.raw.slowdown);
                break;
            case 0x040C:
                emergency_img.setImageResource(R.drawable.emergency);
                warning_pic_new.setImageResource(R.drawable.sign_back_left);
                warning_description_new.setText("\n请加速汇入");
                setDataSourceuri = Uri.parse("android.resource://com.example.cvm/"+R.raw.speedup);
                break;
            case 0x0500:
                setDataSourceuri = Uri.parse("android.resource://com.example.cvm/"+R.raw.donotturnleft);
                break;
            case 0x0501:
                setDataSourceuri = Uri.parse("android.resource://com.example.cvm/"+R.raw.turnleft);
                break;
            case 0x0502:
                setDataSourceuri = Uri.parse("android.resource://com.example.cvm/"+R.raw.turnright);
                break;
            default:
                break;
        }
        try{
            my_MediaPlayer.setDataSource(this,setDataSourceuri);
//            mediaPlayer.prepareAsync();
            my_MediaPlayer.setLooping(false);
            my_MediaPlayer.prepare();
            my_MediaPlayer.start();

        }catch(IOException e){
            Toast.makeText(MainActivity.this, "mediaplayer failed", Toast.LENGTH_LONG).show();
        }
    }

    //声明定位回调监听器
    public AMapLocationListener mLocationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation amapLocation) {
            if (amapLocation !=null ) {
                if (amapLocation.getErrorCode() == 0) {
                    //定位成功
                    currenLatLon = new LatLng(amapLocation.getLatitude(),amapLocation.getLongitude());
                } else { }
            }
        }
    };

    //地图个性化
    public static void setAssetsStyle(AMap aMap, Context context, String style, String style_extra) {
        byte[] buffer1 = null;
        byte[] buffer2 = null;
        InputStream is1 = null;
        InputStream is2 = null;
        if(style != null && style_extra != null) {
            try {
                is1 = context.getAssets().open(style);
                int lenght1 = is1.available();
                buffer1 = new byte[lenght1];
                is1.read(buffer1);
                is2 = context.getAssets().open(style_extra);
                int lenght2 = is2.available();
                buffer2 = new byte[lenght2];
                is2.read(buffer2);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (is1 != null)
                        is1.close();
                    if (is2 != null)
                        is2.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        CustomMapStyleOptions customMapStyleOptions = new CustomMapStyleOptions();
        customMapStyleOptions.setEnable(true);
        customMapStyleOptions.setStyleData(buffer1);
        customMapStyleOptions.setStyleExtraData(buffer2);
        aMap.setCustomMapStyle(customMapStyleOptions);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
        isInOtherActivity = false;
    }
    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
        isInOtherActivity = true;
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
        if(null != mLocationClient){
            mLocationClient.onDestroy();
        }
        finish();
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mMapView.onSaveInstanceState(outState);
    }

    //弹出主目录，可选择进入车辆状态、离线地图的Activity
    private void showPopupMenu(View view) {
        // View当前PopupMenu显示的相对View的位置
        PopupMenu popupMenu = new PopupMenu(this, view);
        // menu布局
        popupMenu.getMenuInflater().inflate(R.menu.menu, popupMenu.getMenu());
        // menu的item点击事件
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                //Toast.makeText(getApplicationContext(), item.getTitle(), Toast.LENGTH_SHORT).show();
                switch (item.getTitle().toString()){
                    case "车辆状态":
                        try
                        {
                            Intent mIntent = new Intent(getApplicationContext(),CarActivity.class);
                            startActivityForResult(mIntent,1);
                            overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
                        }
                        catch (Exception ex)
                        {
                            Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case "离线地图":
                        try
                        {
                            startActivity(new Intent(getApplicationContext(),
                                    com.amap.api.maps.offlinemap.OfflineMapActivity.class));
                            //overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
                        }
                        catch (Exception ex)
                        {
                            Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case "更改地图风格":
                        switch(CURRENT_MAP_SYTLE){
                            case 0:
                                setAssetsStyle(aMap,getApplicationContext(),STYLE,STYLE_EXTRA);
                                CURRENT_MAP_SYTLE = 1;
                                break;
                            case 1:
                                setAssetsStyle(aMap,getApplicationContext(),null,null);
                                CURRENT_MAP_SYTLE = 0;
                                break;
                            default:
                                break;
                        }
                        break;
                        default:
                            break;
                }
                return false;
            }
        });
        // PopupMenu关闭事件
        popupMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {
            }
        });
        popupMenu.show();
    }

    //地图上的infoWindow
    public class InfoWindowAdapter implements AMap.InfoWindowAdapter
    {
        @Override
        public View getInfoWindow(Marker marker) {
            return infoWindow;
        }

        @Override
        public View getInfoContents(Marker marker) {
            return infoWindow;
        }

    }

}

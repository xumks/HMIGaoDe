package com.example.cvm;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.util.Log;

import com.amap.api.maps.AMap;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.GroundOverlayOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.PolygonOptions;
import com.amap.api.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class Utils {

    static double x_PI = 3.14159265358979324 * 3000.0 / 180.0;
    static double PI = 3.1415926535897932384626;
    static double a = 6378245.0;
    static double ee = 0.00669342162296594323;

    //用于缩放图片
    public static Bitmap zoomimg(Bitmap bm, int newWidth, int newHeight, float degrees){
        //获得图片的宽高
        int width = bm.getWidth();
        int height = bm.getHeight();
        //计算缩放比例
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        //取得想要缩放的matrix参数
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        matrix.postRotate(degrees, newWidth/2, newHeight/2);
        //得到新的图片
        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
    }

    //不同坐标系转换
    public static double transformlat(double lng,double lat){
        double ret= -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat + 0.1 * lng * lat + 0.2 * Math.sqrt(Math.abs(lng));
        ret += (20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(lat * PI) + 40.0 * Math.sin(lat / 3.0 * PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(lat / 12.0 * PI) + 320 * Math.sin(lat * PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    public static double transformlng(double lng,double lat){
        double ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng + 0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng));
        ret += (20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(lng * PI) + 40.0 * Math.sin(lng / 3.0 * PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(lng / 12.0 * PI) + 300.0 * Math.sin(lng / 30.0 * PI)) * 2.0 / 3.0;
        return ret;
    }

    public static double[]  wgs84tobd09(double lng, double lat){
        //第一次转换
        double dlat = transformlat(lng - 105.0, lat - 35.0);
        double dlng = transformlng(lng - 105.0, lat - 35.0);
        double radlat = lat / 180.0 * PI;
        double magic = Math.sin(radlat);
        magic = 1 - ee * magic * magic;
        double sqrtmagic = Math.sqrt(magic);
        dlat = (dlat * 180.0) / ((a * (1 - ee)) / (magic * sqrtmagic) * PI);
        dlng = (dlng * 180.0) / (a / sqrtmagic * Math.cos(radlat) * PI);
        double mglat = lat + dlat;
        double mglng = lng + dlng;

        //第二次转换
        double z = Math.sqrt(mglng * mglng + mglat * mglat) + 0.00002 * Math.sin(mglat * x_PI);
        double theta = Math.atan2(mglat, mglng) + 0.000003 * Math.cos(mglng * x_PI);
        double bd_lng = z * Math.cos(theta) + 0.0065;
        double bd_lat = z * Math.sin(theta) + 0.006;
        return new double[]{bd_lat,bd_lng};
    }

    public static double[]  wgs84togcj02(double lng, double lat){
        //第一次转换
        double dlat = transformlat(lng - 105.0, lat - 35.0);
        double dlng = transformlng(lng - 105.0, lat - 35.0);
        double radlat = lat / 180.0 * PI;
        double magic = Math.sin(radlat);
        magic = 1 - ee * magic * magic;
        double sqrtmagic = Math.sqrt(magic);
        dlat = (dlat * 180.0) / ((a * (1 - ee)) / (magic * sqrtmagic) * PI);
        dlng = (dlng * 180.0) / (a / sqrtmagic * Math.cos(radlat) * PI);
        double mglat = lat + dlat;
        double mglng = lng + dlng;
        return new double[]{mglat,mglng};
    }

    public static LatLng wgs84togcJ02latlng(double lng, double lat){
        double point[] = Utils.wgs84togcj02(lng, lat);
        return new LatLng(point[0], point[1]);
    }

    public static void clearMapAndDrawRoad(AMap aMap){
        //Log.e("clear","map");
        //Log.e("Vehicle num",Integer.toString(MainActivity.Vehicles.size()));
        //园区外道路的多边形
        double point1[] = Utils.wgs84togcj02(118.81095046, 31.94929230);
        double point2[] = Utils.wgs84togcj02(118.81195046, 31.94933230);
        double point3[] = Utils.wgs84togcj02(118.81260046, 31.94937230);
        double point4[] = Utils.wgs84togcj02(118.81260046, 31.94923230);
        double point5[] = Utils.wgs84togcj02(118.81190046, 31.94919230);
        double point6[] = Utils.wgs84togcj02(118.81095046, 31.94915230);
        LatLng latLng1 = new LatLng(point1[0], point1[1]);
        LatLng latLng2 = new LatLng(point2[0], point2[1]);
        LatLng latLng3 = new LatLng(point3[0], point3[1]);
        LatLng latLng4 = new LatLng(point4[0], point4[1]);
        LatLng latLng5 = new LatLng(point5[0], point5[1]);
        LatLng latLng6 = new LatLng(point6[0], point6[1]);
        // 声明 多边形参数对象
        PolygonOptions polygonOptions = new PolygonOptions();
        // 添加 多边形的每个顶点（顺序添加）
        polygonOptions.add(latLng1, latLng2, latLng3, latLng4, latLng5, latLng6);
        polygonOptions.strokeWidth(15) // 多边形的边框
                .strokeColor(Color.argb(255, 146, 182, 177)) // 边框颜色
                .fillColor(Color.argb(255, 146, 182, 177));   // 多边形的填充色

        //园区内道路的多边形
        LatLng latLng7 = Utils.wgs84togcJ02latlng(118.81183046, 31.94919230);
        LatLng latLng8 = Utils.wgs84togcJ02latlng(118.81186946, 31.94759230);
        LatLng latLng9 = Utils.wgs84togcJ02latlng(118.81183946, 31.94759230);
        LatLng latLng10 = Utils.wgs84togcJ02latlng(118.81180046, 31.94919230);
        // 声明 多边形参数对象
        PolygonOptions polygonOptions2 = new PolygonOptions();
        // 添加 多边形的每个顶点（顺序添加）
        polygonOptions2.add(latLng7, latLng8, latLng9, latLng10);
        polygonOptions2.strokeWidth(15) // 多边形的边框
                .strokeColor(Color.argb(255, 146, 182, 177)) // 边框颜色
                .fillColor(Color.argb(255, 146, 182, 177));   // 多边形的填充色


        //黄线
        double point7[] = Utils.wgs84togcj02(118.81115046, 31.94921030);
        double point8[] = Utils.wgs84togcj02(118.81160046, 31.94923130);
        double point9[] = Utils.wgs84togcj02(118.81195046, 31.94926230);
        double point10[] = Utils.wgs84togcj02(118.81256046, 31.94930230);
        List<LatLng> latLngs1 = new ArrayList<LatLng>();
        latLngs1.add(new LatLng(point7[0], point7[1]));
        latLngs1.add(new LatLng(point8[0], point8[1]));
        List<LatLng> latLngs2 = new ArrayList<LatLng>();
        latLngs2.add(new LatLng(point8[0], point8[1]));
        latLngs2.add(new LatLng(point9[0], point9[1]));
        latLngs2.add(new LatLng(point10[0], point10[1]));
        PolylineOptions polylineOptions1 = new PolylineOptions();
        polylineOptions1.addAll(latLngs1).width(15).color(Color.argb(255, 255, 255, 0));
        PolylineOptions polylineOptions2 = new PolylineOptions();
        polylineOptions2.addAll(latLngs2).width(15).color(Color.argb(255, 255, 255, 0)).setDottedLine(true);

        //白线
        double point11[] = Utils.wgs84togcj02(118.81115046, 31.94924030);
        double point12[] = Utils.wgs84togcj02(118.81160046, 31.94925930);
        List<LatLng> latLngs3 = new ArrayList<LatLng>();
        latLngs3.add(new LatLng(point11[0], point11[1]));
        latLngs3.add(new LatLng(point12[0], point12[1]));
        PolylineOptions polylineOptions3 = new PolylineOptions();
        polylineOptions3.addAll(latLngs3).width(8).color(Color.argb(255, 255, 255, 255));

        //停止线
        double point13[] = Utils.wgs84togcj02(118.81115046, 31.94921030);
        double point14[] = Utils.wgs84togcj02(118.81115046, 31.94926930);
        List<LatLng> latLngs4 = new ArrayList<LatLng>();
        latLngs4.add(new LatLng(point13[0], point13[1]));
        latLngs4.add(new LatLng(point14[0], point14[1]));
        PolylineOptions polylineOptions4 = new PolylineOptions();
        polylineOptions4.addAll(latLngs4).width(8).color(Color.argb(255, 255, 255, 255));

        //北侧非机动车道线
        double point15[] = Utils.wgs84togcj02(118.81115046, 31.94926930);
        double point16[] = Utils.wgs84togcj02(118.81195046, 31.94930130);
        double point17[] = Utils.wgs84togcj02(118.81256046, 31.94933730);
        List<LatLng> latLngs5 = new ArrayList<LatLng>();
        latLngs5.add(new LatLng(point15[0], point15[1]));
        latLngs5.add(new LatLng(point16[0], point16[1]));
        latLngs5.add(new LatLng(point17[0], point17[1]));
        PolylineOptions polylineOptions5 = new PolylineOptions();
        polylineOptions5.addAll(latLngs5).width(8).color(Color.argb(255, 255, 255, 255));

        //南侧非机动车道线
        double point18[] = Utils.wgs84togcj02(118.81115046, 31.94917930);
        double point19[] = Utils.wgs84togcj02(118.81195046, 31.94921830);
        double point20[] = Utils.wgs84togcj02(118.81256046, 31.94925630);
        List<LatLng> latLngs6 = new ArrayList<LatLng>();
        latLngs6.add(new LatLng(point18[0], point18[1]));
        latLngs6.add(new LatLng(point19[0], point19[1]));
        latLngs6.add(new LatLng(point20[0], point20[1]));
        PolylineOptions polylineOptions6 = new PolylineOptions();
        polylineOptions6.addAll(latLngs6).width(8).color(Color.argb(255, 255, 255, 255));

        //左侧斑马线
        LatLng zebra_crossing = wgs84togcJ02latlng(118.81107046, 31.94922030);
        BitmapDescriptor bitmapZebraCrossing = BitmapDescriptorFactory.fromResource(R.drawable.zebra_crossing);
        GroundOverlayOptions groundOverlayOptions =new GroundOverlayOptions()
                .anchor(0.5f, 0.5f)//设置ground覆盖物的锚点比例，默认为0.5f，水平和垂直方向都居中对齐
                .transparency(0.0f)//设置覆盖物的透明度，范围：0.0~1.0
                //.zIndex(10)//设置覆盖物的层次，zIndex值越大越在上层；
                .image(bitmapZebraCrossing)//覆盖物图片
                .position(zebra_crossing,10,13)
                .bearing(358);
        aMap.clear();


        //路
        aMap.addPolygon(polygonOptions);

        aMap.addPolygon(polygonOptions2);

        //左侧黄实线
        aMap.addPolyline(polylineOptions1);

        //右侧黄虚线
        aMap.addPolyline(polylineOptions2);

        //左侧白实线
        aMap.addPolyline(polylineOptions3);

        //左侧停止线
        aMap.addPolyline(polylineOptions4);

        //北侧非机动车道线
        aMap.addPolyline(polylineOptions5);

        //南侧非机动车道线
        aMap.addPolyline(polylineOptions6);

        //左侧斑马线
        aMap.addGroundOverlay(groundOverlayOptions);

    }

}

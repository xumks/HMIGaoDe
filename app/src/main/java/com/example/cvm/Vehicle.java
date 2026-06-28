package com.example.cvm;

import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.utils.overlay.SmoothMoveMarker;

import java.util.List;

public class Vehicle {
    public Integer id;
    public List<LatLng> points;
    public SmoothMoveMarker smoothMoveMarker;
    public boolean tag;
}

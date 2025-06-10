package com.example.minideliveryapp;

import android.os.Bundle;
import android.os.Handler;

import androidx.core.content.ContextCompat;

import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

public class AnimatedRouteActivity extends BaseMapActivity {

    private GeoPoint startPoint;
    private GeoPoint endPoint;
    private Marker movingMarker;
    private Road road;
    private List<GeoPoint> routePoints;
    private int currentIndex = 0;

    private final long delayMs = 100; // tốc độ di chuyển marker

    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_base);

        map = findViewById(R.id.map);
        initMap();

        // Lấy tọa độ truyền từ MainActivity
        double startLat = getIntent().getDoubleExtra("start_lat", 0);
        double startLon = getIntent().getDoubleExtra("start_lon", 0);
        double endLat = getIntent().getDoubleExtra("end_lat", 0);
        double endLon = getIntent().getDoubleExtra("end_lon", 0);

        startPoint = new GeoPoint(startLat, startLon);
        endPoint = new GeoPoint(endLat, endLon);

        // Lấy đường đi và vẽ route
        getRoute();

        // Khởi tạo marker di chuyển
        movingMarker = new Marker(map);
        movingMarker.setPosition(startPoint);
        movingMarker.setTitle("Moving");
        movingMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.car));
        map.getOverlays().add(movingMarker);
        map.invalidate();
    }

    private void getRoute() {
        OSRMRoadManager roadManager = new OSRMRoadManager(this, "MiniDeliveryApp_Road");
        ArrayList<GeoPoint> waypoints = new ArrayList<>();
        waypoints.add(startPoint);
        waypoints.add(endPoint);
        road = roadManager.getRoad(waypoints);
        routePoints = road.mRouteHigh;

        // Vẽ đường đi
        Polyline roadOverlay = RoadManager.buildRoadOverlay(road);
        map.getOverlayManager().add(roadOverlay);

        // Zoom to bounding box toàn bộ route
        if (routePoints != null && !routePoints.isEmpty()) {
            double minLat = Double.MAX_VALUE, minLon = Double.MAX_VALUE;
            double maxLat = Double.MIN_VALUE, maxLon = Double.MIN_VALUE;

            for (GeoPoint p : routePoints) {
                if (p.getLatitude() < minLat) minLat = p.getLatitude();
                if (p.getLongitude() < minLon) minLon = p.getLongitude();
                if (p.getLatitude() > maxLat) maxLat = p.getLatitude();
                if (p.getLongitude() > maxLon) maxLon = p.getLongitude();
            }

            org.osmdroid.util.BoundingBox boundingBox = new org.osmdroid.util.BoundingBox(maxLat, maxLon, minLat, minLon);
            map.zoomToBoundingBox(boundingBox, true, 50);

            // Bắt đầu animation marker
            animateMarker();
        }
        map.invalidate();
    }

    private void animateMarker() {
        currentIndex = 0;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (currentIndex >= routePoints.size()) {
                    // Kết thúc animation
                    return;
                }
                GeoPoint point = routePoints.get(currentIndex);
                movingMarker.setPosition(point);
                map.invalidate();
                currentIndex++;
                handler.postDelayed(this, delayMs);
            }
        }, delayMs);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null); // Dọn handler tránh leak
    }
}

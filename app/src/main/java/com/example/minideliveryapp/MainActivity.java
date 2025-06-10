package com.example.minideliveryapp;

import android.app.ProgressDialog;
import android.location.Address;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.bonuspack.location.GeocoderNominatim;
import org.osmdroid.bonuspack.location.POI;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.views.overlay.Polyline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private MapView map;
    private AutoCompleteTextView fromInput, toInput;
    private Button showBtn;
    private ProgressDialog progressDialog;

    private GeocoderNominatim geocoder;
    private ArrayAdapter<String> fromAdapter, toAdapter;
    private List<POI> fromResults = new ArrayList<>();
    private List<POI> toResults = new ArrayList<>();
    private final Map<String, Road> roadCache = new HashMap<>();

    // Executor để chạy task background (tối ưu thay vì tạo thread mới mỗi lần)
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    // Hàm tạo key cache
    private String makeCacheKey(POI start, POI end) {
        return start.mLocation.getLatitude() + "," + start.mLocation.getLongitude()
                + "_" + end.mLocation.getLatitude() + "," + end.mLocation.getLongitude();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(this,
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_main);

        initMap();
        initViews();
        initGeocoder();
        initAutocomplete();
        initButton();
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang tính đường, vui lòng chờ...");
        progressDialog.setCancelable(false);

    }

    private void initMap() {
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(14);
        map.getController().setCenter(new GeoPoint(21.0285, 105.8542)); // Hà Nội
    }

    private void initViews() {
        fromInput = findViewById(R.id.fromInput);
        toInput = findViewById(R.id.toInput);
        showBtn = findViewById(R.id.showBtn);

        fromAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line);
        toAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line);

        fromInput.setAdapter(fromAdapter);
        toInput.setAdapter(toAdapter);

        fromInput.setOnItemClickListener((parent, view, position, id) -> {
            // Chọn từ gợi ý -> giữ lại POI đã chọn
            POI poi = fromResults.get(position);
            fromResults.clear();
            fromResults.add(poi);
        });

        toInput.setOnItemClickListener((parent, view, position, id) -> {
            POI poi = toResults.get(position);
            toResults.clear();
            toResults.add(poi);
        });
    }

    private void initGeocoder() {
        geocoder = new GeocoderNominatim("MiniDeliveryApp");
    }

    private void initAutocomplete() {
        fromInput.addTextChangedListener(new AutoCompleteListener(fromInput, fromAdapter, fromResults));
        toInput.addTextChangedListener(new AutoCompleteListener(toInput, toAdapter, toResults));
    }

    private void initButton() {
        showBtn.setOnClickListener(v -> {
            if (fromResults.isEmpty() || toResults.isEmpty()) {
                Toast.makeText(this, "Chọn đúng địa điểm từ gợi ý!", Toast.LENGTH_SHORT).show();
                return;
            }
            drawMarkersAndRoute(fromResults.get(0), toResults.get(0));
        });
    }

    private class AutoCompleteListener implements TextWatcher {
        private final AutoCompleteTextView editText;
        private final ArrayAdapter<String> adapter;
        private final List<POI> results;
        private Runnable delayedSearch;
        private final android.os.Handler handler = new android.os.Handler();

        AutoCompleteListener(AutoCompleteTextView editText, ArrayAdapter<String> adapter, List<POI> results) {
            this.editText = editText;
            this.adapter = adapter;
            this.results = results;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // Huỷ lần tìm kiếm cũ
            if (delayedSearch != null) {
                handler.removeCallbacks(delayedSearch);
            }

            delayedSearch = () -> {
                final String query = editText.getText().toString().trim();
                if (query.isEmpty()) {
                    runOnUiThread(() -> {
                        results.clear();
                        adapter.clear();
                        adapter.notifyDataSetChanged();
                    });
                    return;
                }

                executor.submit(() -> {
                    try {
                        List<Address> addresses = geocoder.getFromLocationName(query, 5);
                        runOnUiThread(() -> {
                            results.clear();
                            adapter.clear();
                            for (Address address : addresses) {
                                String featureName = address.getFeatureName();
                                if (featureName == null || featureName.isEmpty()) {
                                    featureName = address.getAddressLine(0);
                                }
                                POI poi = new POI(POI.POI_SERVICE_NOMINATIM);
                                poi.mLocation = new GeoPoint(address.getLatitude(), address.getLongitude());
                                poi.mDescription = featureName;
                                results.add(poi);
                                adapter.add(poi.mDescription);
                            }
                            adapter.notifyDataSetChanged();
                            editText.showDropDown();
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            };

            // Đợi 400ms sau khi gõ xong mới thực hiện tìm kiếm
            handler.postDelayed(delayedSearch, 400);
        }

        @Override
        public void afterTextChanged(Editable s) {}
    }


    private void drawMarkersAndRoute(POI start, POI end) {
        map.getOverlays().clear();

        Marker startMarker = new Marker(map);
        startMarker.setPosition(start.mLocation);
        startMarker.setTitle("Start: " + start.mDescription);
        map.getOverlays().add(startMarker);

        Marker endMarker = new Marker(map);
        endMarker.setPosition(end.mLocation);
        endMarker.setTitle("End: " + end.mDescription);
        map.getOverlays().add(endMarker);

        String key = makeCacheKey(start, end);
        Road road = roadCache.get(key);

        if (road != null) {
            // Dùng cached road, vẽ luôn
            drawRoadOnMap(road);
        } else {
            // Tính đường mới trên background thread
            showLoading(true);
            executor.submit(() -> {
                RoadManager roadManager = new OSRMRoadManager(this, "MiniDeliveryApp_Road");
                ArrayList<GeoPoint> waypoints = new ArrayList<>();
                waypoints.add(start.mLocation);
                waypoints.add(end.mLocation);

                Road newRoad = roadManager.getRoad(waypoints);

                // Lưu vào cache
                roadCache.put(key, newRoad);

                runOnUiThread(() -> {
                    showLoading(false);
                    drawRoadOnMap(newRoad);
                });
            });
        }

        map.invalidate();
    }

    private void drawRoadOnMap(Road road) {
        Polyline roadOverlay = RoadManager.buildRoadOverlay(road);
        map.getOverlayManager().add(roadOverlay);

        List<GeoPoint> routePoints = road.mRouteHigh;
        if (routePoints != null && !routePoints.isEmpty()) {
            // Tính bounding box chứa toàn bộ route
            double minLat = Double.MAX_VALUE, minLon = Double.MAX_VALUE;
            double maxLat = Double.MIN_VALUE, maxLon = Double.MIN_VALUE;

            for (GeoPoint p : routePoints) {
                if (p.getLatitude() < minLat) minLat = p.getLatitude();
                if (p.getLongitude() < minLon) minLon = p.getLongitude();
                if (p.getLatitude() > maxLat) maxLat = p.getLatitude();
                if (p.getLongitude() > maxLon) maxLon = p.getLongitude();
            }

            // Tạo bounding box từ 2 điểm min/max
            org.osmdroid.util.BoundingBox boundingBox = new org.osmdroid.util.BoundingBox(maxLat, maxLon, minLat, minLon);

            // Zoom sao cho toàn bộ bounding box hiện trên màn hình, có padding 50 pixels
            map.zoomToBoundingBox(boundingBox, true, 50);
        }

        map.invalidate();
    }


    private void showLoading(boolean show) {
        if (show) {
            if (!progressDialog.isShowing()) {
                progressDialog.show();
            }
        } else {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow(); // Dọn dẹp executor khi activity bị destroy
    }
}

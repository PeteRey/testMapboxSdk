package com.example.mapbox;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.offline.OfflineRegion;
import com.mapbox.mapboxsdk.offline.OfflineRegionError;
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus;
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition;
import com.mapbox.mapboxsdk.plugins.offline.model.NotificationOptions;
import com.mapbox.mapboxsdk.plugins.offline.model.OfflineDownloadOptions;
import com.mapbox.mapboxsdk.plugins.offline.offline.OfflinePlugin;
import com.mapbox.mapboxsdk.plugins.offline.utils.OfflineUtils;
import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import timber.log.Timber;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillOpacity;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private MapView mapView;
    private MapboxMap map;
    private ProgressBar progresBar;
    private Button downloadButton;
    private Button listButton;

    private boolean isEndNotified;
    private int regionSelected;

    private OfflineManager offlineManager;
    //private OfflineRegion offlineRegion;

    private static final String TAG = MainActivity.class.toString();

    public static final String JSON_CHARSET = "UTF-8";
    public static final String JSON_FIELD_REGION_NAME = "FIELD_REGION_NAME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_main);
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap){
        map = mapboxMap;
        mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
                @Override
            public void onStyleLoaded(@NonNull Style style) {
                //loadJsonUrl(style);
                //loadOff(style);

                //progresBar = findViewById(R.id.progress_bar);

                offlineManager = OfflineManager.getInstance(MainActivity.this);
                downloadButton = findViewById(R.id.downloadButton);
                downloadButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        downloadRegion();

                    }
                });

                listButton = findViewById(R.id.listButton);
                listButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        downloadedRegionList();
                    }
                });
            }
        });
    }

    public void downloadRegion () {

/*        LatLngBounds latLngBounds = new LatLngBounds.Builder()
                .include(new LatLng(37.7897, -119.5073)) // Northeast
                .include(new LatLng(37.6744, -119.6815)) // Southwest
                .build();

        OfflineTilePyramidRegionDefinition definition = new OfflineTilePyramidRegionDefinition(
                style.getUrl(),
                latLngBounds,
                10,
                20,
                MainActivity.this.getResources().getDisplayMetrics().density);*/

        String styleUrl = map.getStyle().getUrl();
        LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;
        double minZoom = map.getCameraPosition().zoom;
        double maxZoom = map.getMaxZoomLevel();
        float pixelRatio = this.getResources().getDisplayMetrics().density;

        OfflineTilePyramidRegionDefinition definition = new OfflineTilePyramidRegionDefinition(styleUrl, bounds, minZoom, maxZoom, pixelRatio);


        // Implementation that uses JSON to store Yosemite National Park as the offline region name.
        byte[] metadata;
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(JSON_FIELD_REGION_NAME, "Yosemite National Park");
            String json = jsonObject.toString();
            metadata = json.getBytes(JSON_CHARSET);
        } catch (Exception exception) {
            Log.e(TAG, "Failed to encode metadata: " + exception.getMessage());
            metadata = null;
        }


        // Create the region asynchronously
        offlineManager.createOfflineRegion(definition, metadata,
                new OfflineManager.CreateOfflineRegionCallback() {
                    @Override
                    public void onCreate(OfflineRegion offlineRegion) {
                        offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE);

                        // Monitor the download progress using setObserver
                        offlineRegion.setObserver(new OfflineRegion.OfflineRegionObserver() {
                            @Override
                            public void onStatusChanged(OfflineRegionStatus status) {

                                // Calculate the download percentage
                                double percentage = status.getRequiredResourceCount() >= 0
                                        ? (100.0 * status.getCompletedResourceCount() / status.getRequiredResourceCount()) :
                                        0.0;

                                if (status.isComplete()) {
                                    // Download complete
                                    Log.d(TAG, "Region downloaded successfully.");
                                } else if (status.isRequiredResourceCountPrecise()) {
                                    String total = String.valueOf(percentage);
                                    Log.d(TAG, total);
                                }
                            }

                            @Override
                            public void onError(OfflineRegionError error) {
                                // If an error occurs, print to logcat
                                Log.e(TAG, "onError reason: " + error.getReason());
                                Log.e(TAG, "onError message: " + error.getMessage());
                            }

                            @Override
                            public void mapboxTileCountLimitExceeded(long limit) {
                                // Notify if offline region exceeds maximum tile count
                                Log.e(TAG, "Mapbox tile count limit exceeded: " + limit);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error: " + error);
                    }
                });

    }
        private void downloadedRegionList() {
                // Build a region list when the user clicks the list button

                // Reset the region selected int to 0
            regionSelected = 0;

                // Query the DB asynchronously
            offlineManager.listOfflineRegions(new OfflineManager.ListOfflineRegionsCallback() {
                @Override
                public void onList(final OfflineRegion[] offlineRegions) {
                        // Check result. If no regions have been downloaded yet, notify user and return
                    if (offlineRegions == null || offlineRegions.length == 0) {
                        Toast.makeText(getApplicationContext(), getString(R.string.toast_no_regions_yet), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Add all of the region names to a list
                    ArrayList<String> offlineRegionsNames = new ArrayList<>();
                    for (OfflineRegion offlineRegion : offlineRegions) {
                        offlineRegionsNames.add(String.valueOf(1));
                    }
                    final CharSequence[] items = offlineRegionsNames.toArray(new CharSequence[offlineRegionsNames.size()]);

                    // Build a dialog containing the list of regions
                    AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                            .setTitle(getString(R.string.navigate_title))
                            .setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
// Track which region the user selects
                                    regionSelected = which;
                                }
                            })
                            .setPositiveButton(getString(R.string.navigate_positive_button), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {

                                    Toast.makeText(MainActivity.this, items[regionSelected], Toast.LENGTH_LONG).show();

// Get the region bounds and zoom
                                    LatLngBounds bounds = ((OfflineTilePyramidRegionDefinition)
                                            offlineRegions[regionSelected].getDefinition()).getBounds();
                                    double regionZoom = ((OfflineTilePyramidRegionDefinition)
                                            offlineRegions[regionSelected].getDefinition()).getMinZoom();

// Create new camera position
                                    CameraPosition cameraPosition = new CameraPosition.Builder()
                                            .target(bounds.getCenter())
                                            .zoom(regionZoom)
                                            .build();

// Move camera to new position
                                    map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                                }
                            })
                            .setNeutralButton(getString(R.string.navigate_neutral_button_title), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
// Make progressBar indeterminate and
// set it to visible to signal that
// the deletion process has begun
                                /*    progressBar.setIndeterminate(true);
                                    progressBar.setVisibility(View.VISIBLE);*/

// Begin the deletion process
                                    offlineRegions[regionSelected].delete(new OfflineRegion.OfflineRegionDeleteCallback() {
                                        @Override
                                        public void onDelete() {
// Once the region is deleted, remove the
// progressBar and display a toast
                                         /*   progressBar.setVisibility(View.INVISIBLE);
                                            progressBar.setIndeterminate(false);*/
                                            Toast.makeText(getApplicationContext(), getString(R.string.toast_region_deleted),
                                                    Toast.LENGTH_LONG).show();
                                        }

                                        @Override
                                        public void onError(String error) {
                                      /*      progressBar.setVisibility(View.INVISIBLE);
                                            progressBar.setIndeterminate(false);*/
                                            Timber.e( "Error: %s", error);
                                        }
                                    });
                                }
                            })
                            .setNegativeButton(getString(R.string.navigate_negative_button_title), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                // When the user cancels, don't do anything.
                // The dialog will automatically close
                                }
                            }).create();
                    dialog.show();

                }

                @Override
                public void onError(String error) {
                    Timber.e( "Error: %s", error);
                }
            });
        }


/*        offlineManager.createOfflineRegion(definition, metadata, new OfflineManager.CreateOfflineRegionCallback() {
            @Override
            public void onCreate(OfflineRegion offlineRegion) {
                offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE);
                Log.e(TAG, "Creada region offline");
                        offlineRegion = offline;
                launchDownload();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "no creada");
            }
        });*/



/*// Customize the download notification's appearance
        NotificationOptions notificationOptions = NotificationOptions.builder(this)
                .smallIconRes(R.drawable.mapbox_logo_icon)
                .returnActivity(MainActivity.class.getName())
                .build();

// Start downloading the map tiles for offline use
        OfflinePlugin.getInstance(this).startDownload(
                OfflineDownloadOptions.builder()
                        .definition(definition)
                        .metadata(metadata)
                        .notificationOptions(notificationOptions)
                        .build()
        );*/



    private void launchDownload(){

    }
    public void loadJsonUrl(Style style){
        try {
            URL geoJsonUrl = new URL("https://d2ad6b4ur7yvpq.cloudfront.net/naturalearth-3.3.0/ne_50m_urban_areas.geojson");
            GeoJsonSource urbanAreasSource = new GeoJsonSource("urban-areas", geoJsonUrl);
            style.addSource(urbanAreasSource);

            FillLayer urbanArea = new FillLayer("urban-areas-fill", "urban-areas");

            urbanArea.setProperties(
                    fillColor(Color.parseColor("#ff0088")),
                    fillOpacity(0.4f)
            );
            style.addLayerBelow(urbanArea, "water");
        } catch (MalformedURLException malformedUrl){
            malformedUrl.printStackTrace();
        }
    }



    // Add the mapView's own lifecycle methods to the activity's lifecycle methods
    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

}


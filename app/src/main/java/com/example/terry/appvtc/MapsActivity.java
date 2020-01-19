package com.example.terry.appvtc;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.example.terry.appvtc.Adapters.CustomInfoWindowAdapter;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, RoutingListener {

    private GoogleMap mMap;
    protected LatLng startPosition, endPosition;
    private String startAddress, endAddress;

    private Fragment gMapFragment;

    private DrawerLayout mDrawerLayout;
    private BroadcastReceiver mNetworkReceiver;

    ImageButton navButton;
    FloatingActionButton mylocationButton;
    Snackbar internetSnackbar;
    CardView placesCard, localisationCard;

    ConstraintLayout layoutTest;

    private PlaceAutocompleteFragment places;

    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.colorPrimaryDark};
    private boolean hasRoute;


    final int PERMISSION_REQUEST_GPS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        polylines = new ArrayList<>();
        hasRoute = false;

        placesCard = findViewById(R.id.placesCard);
        localisationCard = findViewById(R.id.localisationCard);

        layoutTest = findViewById(R.id.layout_test);
        layoutTest.setVisibility(View.INVISIBLE);

        navButton = findViewById(R.id.navButton);
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!hasRoute) {
                    mDrawerLayout.openDrawer(GravityCompat.START);
                }else {
                    mMap.clear();
                    gMapFragment = getSupportFragmentManager().findFragmentById(R.id.map);
                    if(gMapFragment != null) {
                        ViewGroup.LayoutParams params = gMapFragment.getView().getLayoutParams();
                        params.height = gMapFragment.getView().getHeight() * 2;
                        gMapFragment.getView().setLayoutParams(params);
                    }

                    placesCard.setVisibility(View.VISIBLE);

                    navButton.setImageResource(R.drawable.hamburger_button);
                    setAnimation(navButton);

                    hasRoute = false;
                }
            }
        });

        // Set Hamburger Menu
        setNavigationDrawerMenu();

        mylocationButton = findViewById(R.id.mylocationButton);
        mylocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!hasRoute) {
                    getMyLocation();
                }else {
                    // Zoom Camera to the Route
                    zoomRoute(startPosition, endPosition);
                }
            }
        });

        // Register Receiver for Network Connectivity
        registerReceiverNetwork();

        //*** Permission GPS ***/
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSION_REQUEST_GPS);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
        }

        // Google Places API
        setGooglePlaces();

    }

    private void setNavigationDrawerMenu() {
        // Navigation Drawer
        mDrawerLayout = findViewById(R.id.drawer_layout);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                        // set item as unchecked
                        menuItem.setChecked(false);
                        // close drawer when item is tapped
                        mDrawerLayout.closeDrawers();

                        // Add code here to update the UI based on the item selected
                        switch (menuItem.getItemId()) {
                            case R.id.nav_payout:
                                Intent intent = new Intent(getApplicationContext(), PayoutActivity.class);
                                startActivity(intent);
                                break;
                            case R.id.nav_gallery:
                                Toast.makeText(MapsActivity.this, "2", Toast.LENGTH_SHORT).show();
                                break;
                            case R.id.nav_slideshow:
                                Toast.makeText(MapsActivity.this, "3", Toast.LENGTH_SHORT).show();
                                break;
                            case R.id.nav_manage:
                                Toast.makeText(MapsActivity.this, "4", Toast.LENGTH_SHORT).show();
                                break;
                        }


                        return true;
                    }
                });
    }

    private void setGooglePlaces() {
        places = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        // Customize the Search Bar
        ((EditText)findViewById(R.id.place_autocomplete_search_input)).setTextSize(14.0f);
        ((EditText)findViewById(R.id.place_autocomplete_search_input)).setHint("Quelle est votre destination ?");

        // Google Places Listener Functions
        places.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                //Toast.makeText(MapsActivity.this, place.getName(), Toast.LENGTH_SHORT).show();
                if(place.getAddress() != null) {
                    startAddress = place.getName().toString();
                }
                goToLocation(place.getLatLng());
            }

            @Override
            public void onError(Status status) {
                Toast.makeText(MapsActivity.this, status.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkInternetConnection() {
        ConnectivityManager cm =
                (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        if(cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null &&
                    activeNetwork.isConnectedOrConnecting();

            if (isConnected) {
                Toast.makeText(this, "Connexion réussie", Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(this, "Pas de connexion internet", Toast.LENGTH_SHORT).show();
            }
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // Activate MyLocation cursor & disable MyLocation button
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mMap.getUiSettings().setRotateGesturesEnabled(false);
        } else {
            // Show rationale and request permission.
            Toast.makeText(getApplicationContext(), "Need permission request", Toast.LENGTH_SHORT).show();
        }

        mMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int reason) {
                if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                    if(!mylocationButton.isShown()){
                        mylocationButton.show();
                    }
                }
            }
        });

        // Zoom to GPS Location
        //getMyLocation();

        String title = "3 Villa Saint Charles";
        String subTitle = "4\nmin";

        //Marker
        MarkerOptions markerOpt = new MarkerOptions();
        markerOpt.position(new LatLng(48.845643, 2.284014))
                .title(title)
                .snippet(subTitle);


        MarkerOptions markerOpt2 = new MarkerOptions();
        markerOpt2.position(new LatLng(48.845643, 2.284325))
                .title(title)
                .snippet(subTitle)
                .icon(BitmapDescriptorFactory.fromBitmap(resizeBitmap("mylocation",60,60)));
        markerOpt2.infoWindowAnchor(0,-0.2f);

        //Set Custom InfoWindow Adapter
        CustomInfoWindowAdapter adapter = new CustomInfoWindowAdapter(MapsActivity.this);
        mMap.setInfoWindowAdapter(adapter);

        mMap.addMarker(markerOpt).showInfoWindow();
        mMap.addMarker(markerOpt2).showInfoWindow();



    }


    private void getMyLocation() {
        LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED && lm != null) {
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            double longitude = location.getLongitude();
            double latitude = location.getLatitude();
            LatLng latLng = new LatLng(latitude, longitude);
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 18);
            mMap.animateCamera(cameraUpdate);

            mylocationButton.hide();
        }

        // Test
        gMapFragment = getSupportFragmentManager().findFragmentById(R.id.map);
        if(gMapFragment != null) {
            ViewGroup.LayoutParams params = gMapFragment.getView().getLayoutParams();
            params.height = gMapFragment.getView().getHeight() / 2;
            gMapFragment.getView().setLayoutParams(params);
        }

        layoutTest.setVisibility(View.VISIBLE);

    }

    private void goToLocation(LatLng latLng){
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 18);
        mMap.animateCamera(cameraUpdate);

        startPosition = latLng;
        endPosition = new LatLng(48.841338,2.325480);

        Routing routing = new Routing.Builder()
                .key(getString(R.string.google_maps_key))
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(startPosition, endPosition)
                .build();
        routing.execute();

        mylocationButton.hide();
    }

    private void registerReceiverNetwork() {
        mNetworkReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (intent.getAction() != null && intent.getAction().matches(LocationManager.PROVIDERS_CHANGED_ACTION)) {
                    Toast.makeText(context, getGPSProviderMode(context), Toast.LENGTH_SHORT).show();
                }else {

                    boolean isConnectionUnavailable = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);

                    // Show a Snackbar if internet is unavailable
                    if (isConnectionUnavailable) {
                        View parentLayout = findViewById(android.R.id.content);
                        parentLayout.setBackgroundColor(Color.WHITE);
                        internetSnackbar = Snackbar
                                .make(parentLayout, "Pas de connexion internet", Snackbar.LENGTH_INDEFINITE);
                        View sbView = internetSnackbar.getView();
                        sbView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.red));
                        internetSnackbar.show();

                        // Hide the map
                        gMapFragment = getSupportFragmentManager().findFragmentById(R.id.map);
                        FragmentManager fm = getSupportFragmentManager();
                        fm.beginTransaction()
                                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                                .hide(gMapFragment)
                                .commit();
                        mylocationButton.hide();

                        localisationCard.setVisibility(View.VISIBLE);

                    } else {
                        if (internetSnackbar != null && internetSnackbar.isShown()) {
                            internetSnackbar.dismiss();

                            // Show the map
                            gMapFragment = getSupportFragmentManager().findFragmentById(R.id.map);
                            FragmentManager fm = getSupportFragmentManager();
                            fm.beginTransaction()
                                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                                    .show(gMapFragment)
                                    .commit();

                            if (!mylocationButton.isShown()) {
                                mylocationButton.show();
                            }

                            localisationCard.setVisibility(View.INVISIBLE);
                        }
                    }
                }
            }
        };
        registerReceiver(mNetworkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        registerReceiver(mNetworkReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));

    }

    private String getGPSProviderMode(Context context){
        String locationMode ="";
        ContentResolver contentResolver = context.getContentResolver();
        // Find out what the settings say about which providers are enabled
        int mode = Settings.Secure.getInt(
                contentResolver, Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);

        if (mode != Settings.Secure.LOCATION_MODE_OFF) {
            if (mode == Settings.Secure.LOCATION_MODE_HIGH_ACCURACY) {
                locationMode = "High accuracy. Uses GPS, Wi-Fi, and mobile networks to determine location";
            } else if (mode == Settings.Secure.LOCATION_MODE_SENSORS_ONLY) {
                locationMode = "Device only. Uses GPS to determine location";
            } else if (mode == Settings.Secure.LOCATION_MODE_BATTERY_SAVING) {
                locationMode = "Battery saving. Uses Wi-Fi and mobile networks to determine location";
            }
        }else {
            locationMode = "Location MODE OFF";
        }
        return locationMode;
    }


    protected void unregisterNetworkChanges() {
        try {
            unregisterReceiver(mNetworkReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    public void zoomRoute(LatLng start, LatLng end) {

        if (mMap == null || start == null || end == null) return;

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boundsBuilder.include(start);
        boundsBuilder.include(end);

        int routePadding = 150;
        LatLngBounds latLngBounds = boundsBuilder.build();

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(latLngBounds, routePadding);
        mMap.animateCamera(cameraUpdate);

        mylocationButton.hide();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mNetworkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        registerReceiver(mNetworkReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterNetworkChanges();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterNetworkChanges();
    }


    /** Routing methods Google Directions **/
    @Override
    public void onRoutingFailure(RouteException e) {
        if(e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {

        mMap.clear();
        hasRoute = true;

        // Zoom Camera to the Route
        zoomRoute(startPosition, endPosition);

        if(polylines.size()>0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i <route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            String td = convertDistance(route.get(i).getDistanceValue());
            String ts = convertTime(route.get(i).getDurationValue());

            Toast.makeText(getApplicationContext(),"Route "+ (i+1) +": distance : "+ td+": duration : "+ ts, Toast.LENGTH_LONG).show();
        }

        // Add a marker to the location
        mMap.addMarker(new MarkerOptions().position(startPosition)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .title(startAddress)
                .snippet("4\nmin"))
                .showInfoWindow();
        mMap.addMarker(new MarkerOptions().position(endPosition)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                .title("Edgar Quinet")
                .snippet("12\nmin"))
                .showInfoWindow();


        // Animations and Resizing
        placesCard.setVisibility(View.INVISIBLE);
        navButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        setAnimation(navButton);

        ObjectAnimator animation = ObjectAnimator.ofFloat(mylocationButton, "translationY", -800f);
        animation.setDuration(1000);
        animation.start();

        gMapFragment = getSupportFragmentManager().findFragmentById(R.id.map);
        if(gMapFragment != null) {
            ViewGroup.LayoutParams params = gMapFragment.getView().getLayoutParams();
            params.height = gMapFragment.getView().getHeight() / 2;
            gMapFragment.getView().setLayoutParams(params);
        }

        layoutTest.setVisibility(View.VISIBLE);
    }

    @Override
    public void onRoutingCancelled() {
        Log.i("routing", "Routing was cancelled.");
    }

    private String convertDistance(int distance){
        double convertedDistance;
        String measure = "m";

        if(distance > 1000){
            convertedDistance = distance;
            convertedDistance = convertedDistance / 1000;
            measure = "km";
        }else {
            convertedDistance = distance;
        }
        return convertedDistance + measure;
    }

    private String convertTime(int totalSeconds) {
        int totalMinutes = totalSeconds / 60;
        int minutes = totalMinutes % 60;

        return  minutes + " min";
    }

    private void setAnimation(ImageButton button){
        Animation animation = AnimationUtils.loadAnimation(getBaseContext(), android.R.anim.fade_in);
        animation.setDuration(2000);
        button.startAnimation(animation);
    }

    public Bitmap resizeBitmap(String drawableName, int width, int height){
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(),getResources().getIdentifier(drawableName, "drawable", getPackageName()));
        return Bitmap.createScaledBitmap(imageBitmap, width, height, false);
    }
}

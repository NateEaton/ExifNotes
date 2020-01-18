package com.tommihirvonen.exifnotes.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.tommihirvonen.exifnotes.datastructures.Camera;
import com.tommihirvonen.exifnotes.datastructures.Frame;
import com.tommihirvonen.exifnotes.datastructures.Lens;
import com.tommihirvonen.exifnotes.datastructures.Roll;
import com.tommihirvonen.exifnotes.dialogs.EditFrameDialog;
import com.tommihirvonen.exifnotes.dialogs.EditFrameDialogCallback;
import com.tommihirvonen.exifnotes.utilities.ExtraKeys;
import com.tommihirvonen.exifnotes.utilities.PreferenceConstants;
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.utilities.Utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * MapActivity displays all the frames in the user's database on a map.
 */
public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    /**
     * Reference to the singleton database
     */
    private FilmDbHelper database;

    /**
     * List to hold all the rolls from the database
     */
    private List<Roll> rollList;

    private boolean[] selectedRolls;

    /**
     * GoogleMap object to show the map and to hold all the markers for all frames
     */
    private GoogleMap googleMap_;

    /**
     * Member to indicate whether this activity was continued or not.
     * Some animations will only be activated if this value is false.
     */
    private boolean continueActivity = false;

    /**
     * Holds reference to the GoogleMap map type
     */
    private int mapType;

    private final List<Marker> markerList = new ArrayList<>();

    private BottomSheetBehavior bottomSheetBehavior;

    /**
     * Sets up the activity's layout and view and reads all the rolls from the database.
     *
     * @param savedInstanceState if not null, then the activity is continued
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {

        overridePendingTransition(R.anim.enter_from_right, R.anim.hold);

        super.onCreate(savedInstanceState);

        if (Utilities.isAppThemeDark(getBaseContext())) {
            setTheme(R.style.Theme_AppCompat);
        }

        // In onSaveInstanceState a dummy boolean was put into outState.
        // savedInstanceState is not null if the activity was continued.
        if (savedInstanceState != null) continueActivity = true;

        setContentView(R.layout.activity_map);

        final SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        mapType = sharedPreferences.getInt(PreferenceConstants.KEY_MAP_TYPE, GoogleMap.MAP_TYPE_NORMAL);

        database = FilmDbHelper.getInstance(this);
        rollList = getIntent().getParcelableArrayListExtra(ExtraKeys.ARRAY_LIST_ROLLS);
        if (rollList == null) {
            rollList = new ArrayList<>();
        }
        selectedRolls = new boolean[rollList.size()];
        Arrays.fill(selectedRolls, true);

        Utilities.setUiColor(this, true);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(
                    getIntent().getStringExtra(ExtraKeys.MAPS_ACTIVITY_SUBTITLE)
            );
            getSupportActionBar().setTitle(
                    getIntent().getStringExtra(ExtraKeys.MAPS_ACTIVITY_TITLE)
            );
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        final View bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        final float peekHeightOffset = getResources().getDimensionPixelSize(R.dimen.MapActivityBottomSheetPeekHeight);
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View view, int i) {

            }
            @Override
            public void onSlide(@NonNull View view, float v) {
                final float offset = bottomSheet.getHeight() * v + peekHeightOffset - peekHeightOffset * v;
                switch (bottomSheetBehavior.getState()) {
                    case BottomSheetBehavior.STATE_DRAGGING:
                    case BottomSheetBehavior.STATE_SETTLING:
                        googleMap_.setPadding(0, 0, 0, Math.round(offset));
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED:
                    case BottomSheetBehavior.STATE_EXPANDED:
                    case BottomSheetBehavior.STATE_HALF_EXPANDED:
                    case BottomSheetBehavior.STATE_HIDDEN:
                        break;
                }
            }
        });

        final ListView listView = findViewById(R.id.rolls_list_view);
        final String[] rollNamesArray = new String[rollList.size()];
        for (int i = 0; i < rollList.size(); i++) {
            rollNamesArray[i] = rollList.get(i).getName();
        }
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, rollNamesArray);
        listView.setAdapter(adapter);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.nothing, R.anim.exit_to_right);
    }

    /**
     * Inflate the menu
     *
     * @param menu the menu to be inflated
     * @return super class to execute code for the menu to work properly.
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_map_activity, menu);
        // If only one roll is displayed, hide the filter icon.
        if (rollList.size() == 1) {
            menu.findItem(R.id.menu_item_filter).setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        switch (mapType) {
            case GoogleMap.MAP_TYPE_NORMAL: default:
                menu.findItem(R.id.menu_item_normal).setChecked(true);
                break;
            case GoogleMap.MAP_TYPE_HYBRID:
                menu.findItem(R.id.menu_item_hybrid).setChecked(true);
                break;
            case GoogleMap.MAP_TYPE_SATELLITE:
                menu.findItem(R.id.menu_item_satellite).setChecked(true);
                break;
            case GoogleMap.MAP_TYPE_TERRAIN:
                menu.findItem(R.id.menu_item_terrain).setChecked(true);
                break;
        }
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * Handles the home as up press event.
     *
     * @param item {@inheritDoc}
     * @return call to super
     */
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.menu_item_normal:
                item.setChecked(true);
                setMapType(GoogleMap.MAP_TYPE_NORMAL);
                return true;
            case R.id.menu_item_hybrid:
                item.setChecked(true);
                setMapType(GoogleMap.MAP_TYPE_HYBRID);
                return true;
            case R.id.menu_item_satellite:
                item.setChecked(true);
                setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                return true;
            case R.id.menu_item_terrain:
                item.setChecked(true);
                setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                return true;

            case R.id.menu_item_filter:
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                final String[] rollNames = new String[rollList.size()];
                for (int i = 0; i < rollList.size(); ++i) {
                    rollNames[i] = rollList.get(i).getName();
                }
                final boolean[] selectedRollsTemp = Arrays.copyOf(selectedRolls, selectedRolls.length);
                builder.setMultiChoiceItems(rollNames, selectedRollsTemp, (dialog, which, isChecked) ->
                        selectedRollsTemp[which] = isChecked);
                builder.setNegativeButton(R.string.Cancel, (dialog, which) -> {});
                builder.setPositiveButton(R.string.FilterNoColon, (dialog, which) -> {
                    selectedRolls = selectedRollsTemp;
                    updateMarkers();
                });
                builder.setNeutralButton(R.string.DeselectAll, null);
                final AlertDialog dialog = builder.create();
                dialog.show();
                // Override the neutral button onClick listener after the dialog is shown.
                // This way the dialog isn't dismissed when the button is pressed.
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                    final ListView listView = dialog.getListView();
                    for (int i = 0; i < listView.getCount(); i++) {
                        listView.setItemChecked(i, false);
                    }
                    Arrays.fill(selectedRollsTemp, false);
                });
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Puts a dummy boolean in outState so that it is not null.
     *
     * @param outState used to store the dummy boolean
     */
    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);

        // Insert dummy boolean so that outState is not null.
        outState.putBoolean(ExtraKeys.CONTINUE, true);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     *
     * In this case, draw markers for all the frames in the user's database.
     *
     * @param googleMap {@inheritDoc}
     */
    @Override
    public void onMapReady(final GoogleMap googleMap) {
        googleMap_ = googleMap;
        final int peekHeightOffset = getResources().getDimensionPixelSize(R.dimen.MapActivityBottomSheetPeekHeight);
        googleMap_.setPadding(0, 0, 0, peekHeightOffset);

        // If the app's theme is dark, stylize the map with the custom night mode
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        if (Utilities.isAppThemeDark(getBaseContext())) {
            googleMap_.setMapStyle(new MapStyleOptions(getResources()
                    .getString(R.string.style_json)));
        }

        googleMap_.setMapType(prefs.getInt(PreferenceConstants.KEY_MAP_TYPE, GoogleMap.MAP_TYPE_NORMAL));
        updateMarkers();

        if (rollList.size() == 1) {
            googleMap_.setInfoWindowAdapter(new InfoWindowAdapterSingleRoll());
        } else {
            googleMap_.setInfoWindowAdapter(new InfoWindowAdapterMultipleRolls());
        }
        googleMap_.setOnInfoWindowClickListener(new OnInfoWindowClickListener());

    }

    private void updateMarkers() {
        // Iterator to change marker color
        int i = 0;
        final ArrayList<BitmapDescriptor> markerStyles = new ArrayList<>();

        markerStyles.add(0, BitmapDescriptorFactory.fromBitmap(
                getBitmapFromVectorDrawable(this, R.drawable.ic_marker_red)));
        markerStyles.add(1, BitmapDescriptorFactory.fromBitmap(
                setBitmapHue(getBitmapFromVectorDrawable(this, R.drawable.ic_marker_red),
                        BitmapDescriptorFactory.HUE_AZURE)));
        markerStyles.add(2, BitmapDescriptorFactory.fromBitmap(
                setBitmapHue(getBitmapFromVectorDrawable(this, R.drawable.ic_marker_red),
                        BitmapDescriptorFactory.HUE_GREEN)));
        markerStyles.add(3, BitmapDescriptorFactory.fromBitmap(
                setBitmapHue(getBitmapFromVectorDrawable(this, R.drawable.ic_marker_red),
                        BitmapDescriptorFactory.HUE_ORANGE)));
        markerStyles.add(4, BitmapDescriptorFactory.fromBitmap(
                setBitmapHue(getBitmapFromVectorDrawable(this, R.drawable.ic_marker_red),
                        BitmapDescriptorFactory.HUE_YELLOW)));
        markerStyles.add(5, BitmapDescriptorFactory.fromBitmap(
                setBitmapHue(getBitmapFromVectorDrawable(this, R.drawable.ic_marker_red),
                        BitmapDescriptorFactory.HUE_BLUE)));
        markerStyles.add(6, BitmapDescriptorFactory.fromBitmap(
                setBitmapHue(getBitmapFromVectorDrawable(this, R.drawable.ic_marker_red),
                        BitmapDescriptorFactory.HUE_ROSE)));
        markerStyles.add(7, BitmapDescriptorFactory.fromBitmap(
                setBitmapHue(getBitmapFromVectorDrawable(this, R.drawable.ic_marker_red),
                        BitmapDescriptorFactory.HUE_CYAN)));
        markerStyles.add(8, BitmapDescriptorFactory.fromBitmap(
                setBitmapHue(getBitmapFromVectorDrawable(this, R.drawable.ic_marker_red),
                        BitmapDescriptorFactory.HUE_VIOLET)));
        markerStyles.add(9, BitmapDescriptorFactory.fromBitmap(
                setBitmapHue(getBitmapFromVectorDrawable(this, R.drawable.ic_marker_red),
                        BitmapDescriptorFactory.HUE_MAGENTA)));

        for (Marker marker : markerList) {
            marker.remove();
        }
        markerList.clear();

        for (int rollIterator = 0; rollIterator < rollList.size(); ++rollIterator) {

            if (!selectedRolls[rollIterator]) {
                continue;
            }

            final Roll roll = rollList.get(rollIterator);
            final List<Frame> frameList = database.getAllFramesFromRoll(roll);

            for (final Frame frame : frameList) {

                // Parse the latLngLocation string
                final String location = frame.getLocation();
                if (location != null && location.length() > 0 && !location.equals("null")) {
                    final String latString = location.substring(0, location.indexOf(" "));
                    final String lngString = location.substring(location.indexOf(" ") + 1, location.length() - 1);
                    final double lat = Double.parseDouble(latString.replace(",", "."));
                    final double lng = Double.parseDouble(lngString.replace(",", "."));
                    final LatLng position = new LatLng(lat, lng);
                    final String title = "" + roll.getName();
                    final String snippet = "#" + frame.getCount();
                    final Marker marker = googleMap_.addMarker(new MarkerOptions()
                            .icon(markerStyles.get(i))
                            .position(position)
                            .title(title)
                            .snippet(snippet)
                            .anchor(0.5f, 1.0f)); // Since we use a custom marker icon, set offset.

                    marker.setTag(frame);
                    markerList.add(marker);
                }
            }
            ++i;
            if (i > 9) i = 0;
        }

        if (markerList.size() > 0 && !continueActivity) {

            final LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (final Marker marker : markerList) {
                builder.include(marker.getPosition());
            }
            final LatLngBounds bounds = builder.build();

            final int width = getResources().getDisplayMetrics().widthPixels;
            final int height = getResources().getDisplayMetrics().heightPixels;
            final int padding = (int) (width * 0.12); // offset from edges of the map 12% of screen

            // We use this command where the map's dimensions are specified.
            // This is because on some devices, the map's layout may not have yet occurred
            // (map size is 0).
            final CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);
            googleMap_.moveCamera(cameraUpdate);

        } else {
            Toast.makeText(this, getResources().getString(R.string.NoFramesToShow), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Sets the GoogleMap map type
     *
     * @param mapType One of the map type constants from class GoogleMap
     */
    private void setMapType(final int mapType) {
        this.mapType = mapType;
        googleMap_.setMapType(mapType);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(PreferenceConstants.KEY_MAP_TYPE, mapType);
        editor.apply();
    }

    private class InfoWindowAdapterMultipleRolls implements GoogleMap.InfoWindowAdapter {
        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }
        @Override
        public View getInfoContents(Marker marker) {
            if (marker.getTag() instanceof Frame) {
                final Frame frame = (Frame) marker.getTag();
                final Roll roll = database.getRoll(frame.getRollId());
                final Camera camera = roll.getCameraId() > 0 ?
                        database.getCamera(roll.getCameraId()) :
                        null;
                final Lens lens = frame.getLensId() > 0 ?
                        database.getLens(frame.getLensId()) :
                        null;
                @SuppressLint("InflateParams")
                final View view = getLayoutInflater().inflate(R.layout.info_window_all_frames, null);
                final TextView rollTextView = view.findViewById(R.id.roll_name);
                final TextView cameraTextView = view.findViewById(R.id.camera);
                final TextView frameCountTextView = view.findViewById(R.id.frame_count);
                final TextView dateTimeTextView = view.findViewById(R.id.date_time);
                final TextView lensTextView = view.findViewById(R.id.lens);
                final TextView noteTextView = view.findViewById(R.id.note);
                rollTextView.setText(roll.getName());
                cameraTextView.setText(
                        camera == null ? getString(R.string.NoCamera) : camera.getName()
                );
                final String frameCountText = "#" + frame.getCount();
                frameCountTextView.setText(frameCountText);
                dateTimeTextView.setText(frame.getDate());
                lensTextView.setText(
                        lens == null ? getString(R.string.NoLens) : lens.getName()
                );
                noteTextView.setText(frame.getNote());
                return view;
            } else {
                return null;
            }
        }
    }

    private class InfoWindowAdapterSingleRoll implements GoogleMap.InfoWindowAdapter {
        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }
        @Override
        public View getInfoContents(Marker marker) {
            if (marker.getTag() instanceof Frame) {
                final Frame frame = (Frame) marker.getTag();
                final Lens lens = frame.getLensId() > 0 ?
                        database.getLens(frame.getLensId()) :
                        null;
                @SuppressLint("InflateParams")
                final View view = getLayoutInflater().inflate(R.layout.info_window, null);
                final TextView frameCountTextView = view.findViewById(R.id.frame_count);
                final TextView dateTimeTextView = view.findViewById(R.id.date_time);
                final TextView lensTextView = view.findViewById(R.id.lens);
                final TextView noteTextView = view.findViewById(R.id.note);
                final String frameCountText = "#" + frame.getCount();
                frameCountTextView.setText(frameCountText);
                dateTimeTextView.setText(frame.getDate());

                lensTextView.setText(
                        lens == null ? getString(R.string.NoLens) : lens.getName()
                );
                noteTextView.setText(frame.getNote());
                return view;
            } else {
                return null;
            }
        }
    }

    private class OnInfoWindowClickListener implements GoogleMap.OnInfoWindowClickListener {
        @Override
        public void onInfoWindowClick(Marker marker) {
            if (marker.getTag() instanceof Frame) {
                final Frame frame = (Frame) marker.getTag();
                if (frame != null) {
                    final Bundle arguments = new Bundle();
                    final String title = "" + getResources().getString(R.string.EditFrame) + frame.getCount();
                    final String positiveButton = getResources().getString(R.string.OK);
                    arguments.putString(ExtraKeys.TITLE, title);
                    arguments.putString(ExtraKeys.POSITIVE_BUTTON, positiveButton);
                    arguments.putParcelable(ExtraKeys.FRAME, frame);

                    final EditFrameDialogCallback dialog = new EditFrameDialogCallback();
                    dialog.setArguments(arguments);
                    dialog.setOnPositiveButtonClickedListener(data -> {
                                final Frame editedFrame = data.getParcelableExtra(ExtraKeys.FRAME);
                                if (editedFrame != null) {
                                    database.updateFrame(editedFrame);
                                    marker.setTag(editedFrame);
                                    marker.hideInfoWindow();
                                    marker.showInfoWindow();
                                    setResult(AppCompatActivity.RESULT_OK);
                                }
                            });
                    dialog.show(getSupportFragmentManager().beginTransaction(), EditFrameDialog.TAG);
                }
            }
        }
    }

    private Bitmap getBitmapFromVectorDrawable(final Context context, @DrawableRes final int id) {
        final Drawable drawable = ContextCompat.getDrawable(context, id);
        final Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private Bitmap setBitmapHue(final Bitmap bitmap, final float hue){
        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();
        final float[] hvs = new float[3];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int pixel = bitmap.getPixel(x, y);
                Color.colorToHSV(pixel, hvs);
                hvs[0] = hue;
                bitmap.setPixel(x, y, Color.HSVToColor(Color.alpha(pixel), hvs));
            }
        }
        return bitmap;
    }

}

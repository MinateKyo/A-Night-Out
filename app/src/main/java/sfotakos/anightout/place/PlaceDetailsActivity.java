package sfotakos.anightout.place;

import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import sfotakos.anightout.R;
import sfotakos.anightout.common.Event;
import sfotakos.anightout.common.NetworkUtil;
import sfotakos.anightout.common.data.NightOutContract.EventEntry;
import sfotakos.anightout.common.google_maps_places_photos_api.model.GooglePlacesRequestParams;
import sfotakos.anightout.common.google_maps_places_photos_api.model.Place;
import sfotakos.anightout.databinding.ActivityPlaceDetailsBinding;
import sfotakos.anightout.databinding.LayoutAddEventBinding;
import sfotakos.anightout.eventdetails.PlacePhotosRvAdapter;
import sfotakos.anightout.events.EventsRvAdapter;
import sfotakos.anightout.newevent.NewEventActivity;

//TODO query place details and fill layout with more information
public class PlaceDetailsActivity extends AppCompatActivity {

    // TODO better name
    public final static String PLACE_EXTRA = "PLACEDETAILS_EXTRA";
    public static final String PLACE_DETAILS_ACTIVITY_PARENT = "place-details-activity";

    private ActivityPlaceDetailsBinding mBinding;
    private AlertDialog mEventsDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_place_details);

        setSupportActionBar(mBinding.placeDetailsToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(PLACE_EXTRA)) {
                Place mPlace = (Place) intent.getSerializableExtra(PLACE_EXTRA);
                if (mPlace == null) {
                    throw new RuntimeException("Place data was not recovered properly");
                }

                mBinding.placeDetails.placeNameTextView.setText(mPlace.getName());
                mBinding.placeDetails.placeAddressTextView.setText(mPlace.getVicinity());

                if (mPlace.getPriceLevel() == null) {
                    mBinding.placeDetails.placePriceTextView.setVisibility(View.GONE);
                } else {
                    mBinding.placeDetails.placePriceTextView.setVisibility(View.VISIBLE);
                    mBinding.placeDetails.placePriceTextView.setText(
                            GooglePlacesRequestParams.PlacePrice.getDescriptionByTag(
                                    mPlace.getPriceLevel().toString()));
                }

                // TODO move this to a request class which returns the fully qualified uri
                if (mPlace.getPhotos() != null &&
                        mPlace.getPhotos().size() != 0 &&
                        mPlace.getPhotos().get(0) != null) {

                    List<Uri> photosUri = new ArrayList<>();
                    Uri photoUri = Uri.parse(NetworkUtil.GOOGLE_PLACE_API_BASE_URL).buildUpon()
                            .appendPath("photo")
                            .appendQueryParameter("key", getResources().getString(R.string.google_places_key))
                            .appendQueryParameter("maxheight", "400")
                            .appendQueryParameter("photo_reference", mPlace.getPhotos().get(0).getPhotoReference())
                            .build();

                    photosUri.add(photoUri);

                    mBinding.placeDetails.placePhotosRv.setVisibility(View.VISIBLE);
                    mBinding.placeDetails.placePhotosRv.setAdapter(new PlacePhotosRvAdapter(photosUri));
                    mBinding.placeDetails.placePhotosRv.setLayoutManager(
                            new LinearLayoutManager(this,
                                    LinearLayoutManager.HORIZONTAL, false));
                } else {
                    mBinding.placeDetails.placePhotosRv.setVisibility(View.GONE);
                }

            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.place_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add_to_event) {
            final LayoutAddEventBinding dialogBinding =
                    DataBindingUtil.inflate(getLayoutInflater(),
                            R.layout.layout_add_event, null, false);

            dialogBinding.addEventRootCardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent newEventIntent =
                            new Intent(PlaceDetailsActivity.this, NewEventActivity.class);
                    newEventIntent.setAction(PLACE_DETAILS_ACTIVITY_PARENT);
                    startActivity(newEventIntent);

                    if (mEventsDialog != null){
                        mEventsDialog.dismiss();
                    }
                }
            });

            dialogBinding.addEventEventsRv
                    .setAdapter(new EventsRvAdapter(new EventsRvAdapter.IEventsListener() {
                        @Override
                        public void eventClicked(Event event) {
                            // TODO add to event
                            Toast.makeText(PlaceDetailsActivity.this,
                                    "Add to this event", Toast.LENGTH_LONG).show();
                        }
                    }, queryEvents()));
            dialogBinding.addEventEventsRv.setLayoutManager(new LinearLayoutManager(this));

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            mEventsDialog = builder.setTitle("Add to event").setView(dialogBinding.getRoot()).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // TODO this is duplicated
    private List<Event> queryEvents() {
        List<Event> eventList = new ArrayList<>();
        Cursor cursor = getContentResolver().query(
                EventEntry.CONTENT_URI,
                null,
                null,
                null,
                null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                Event event = new Event();
                int eventIdIndex = cursor.getColumnIndex(EventEntry.EVENT_ID);
                int eventNameIndex = cursor.getColumnIndex(EventEntry.EVENT_NAME);
                int eventDateIndex = cursor.getColumnIndex(EventEntry.EVENT_DATE);
                int eventDescriptionIndex = cursor.getColumnIndex(EventEntry.EVENT_DESCRIPTION);

                int placeNameIndex = cursor.getColumnIndex(EventEntry.RESTAURANT_NAME);
                int placePriceRangeIndex = cursor.getColumnIndex(EventEntry.RESTAURANT_PRICE_RANGE);
                int placeAddressIndex = cursor.getColumnIndex(EventEntry.RESTAURANT_ADDRESS);

                event.setEventId((cursor.getInt(eventIdIndex)));
                event.setEventName(cursor.getString(eventNameIndex));
                event.setEventDate(cursor.getString(eventDateIndex));
                event.setEventDescription(cursor.getString(eventDescriptionIndex));

                Place place = new Place();
                place.setName(cursor.getString(placeNameIndex));
                place.setPriceLevel(cursor.getInt(placePriceRangeIndex));
                place.setVicinity(cursor.getString(placeAddressIndex));

                event.setPlace(place);
                eventList.add(event);
            }
            cursor.close();
        }
        return eventList;
    }
}

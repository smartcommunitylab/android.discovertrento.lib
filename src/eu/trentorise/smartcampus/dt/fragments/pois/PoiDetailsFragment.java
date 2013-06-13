/*******************************************************************************
 * Copyright 2012-2013 Trento RISE
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either   express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package eu.trentorise.smartcampus.dt.fragments.pois;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Address;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.google.android.maps.GeoPoint;

import eu.trentorise.smartcampus.ac.UserRegistration;
import eu.trentorise.smartcampus.ac.authenticator.AMSCAccessProvider;
import eu.trentorise.smartcampus.android.common.SCAsyncTask;
import eu.trentorise.smartcampus.android.common.follow.FollowEntityObject;
import eu.trentorise.smartcampus.android.common.follow.FollowHelper;
import eu.trentorise.smartcampus.android.common.follow.model.Topic;
import eu.trentorise.smartcampus.android.common.navigation.NavigationHelper;
import eu.trentorise.smartcampus.dt.DTParamsHelper;
import eu.trentorise.smartcampus.dt.R;
import eu.trentorise.smartcampus.dt.custom.AbstractAsyncTaskProcessor;
import eu.trentorise.smartcampus.dt.custom.CategoryHelper;
import eu.trentorise.smartcampus.dt.custom.RatingHelper;
import eu.trentorise.smartcampus.dt.custom.RatingHelper.RatingHandler;
import eu.trentorise.smartcampus.dt.custom.data.DTHelper;
import eu.trentorise.smartcampus.dt.custom.data.FollowAsyncTaskProcessor;
import eu.trentorise.smartcampus.dt.custom.data.UnfollowAsyncTaskProcessor;
import eu.trentorise.smartcampus.dt.custom.map.MapManager;
import eu.trentorise.smartcampus.dt.fragments.events.EventDetailsFragment;
import eu.trentorise.smartcampus.dt.fragments.events.EventsListingFragment;
import eu.trentorise.smartcampus.dt.model.BaseDTObject;
import eu.trentorise.smartcampus.dt.model.Concept;
import eu.trentorise.smartcampus.dt.model.DTConstants;
import eu.trentorise.smartcampus.dt.model.POIObject;
import eu.trentorise.smartcampus.dt.model.TmpComment;
import eu.trentorise.smartcampus.dt.notifications.NotificationsSherlockFragmentDT;
import eu.trentorise.smartcampus.protocolcarrier.exceptions.SecurityException;

public class PoiDetailsFragment extends NotificationsSherlockFragmentDT {

	public static final String ARG_POI = "poi_object";
	POIObject poi = null;
	String poiId;
	private TmpComment tmp_comments[];
	private boolean mFollowByIntent;

	private Fragment mFragment = this;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setHasOptionsMenu(true);
		if (getArguments() != null)
			poi = (POIObject) getArguments().getSerializable(ARG_POI);

		tmp_comments = new TmpComment[0];
		// tmp_comments = new TmpComment[5];
		for (int i = 0; i < tmp_comments.length; i++)
			tmp_comments[i] = new TmpComment("This is a comment about the POI", "student", new Date());

		setFollowByIntent();
	}

	private POIObject getPOI() {
		if (poi == null) {
			poi = (POIObject) getArguments().getSerializable(ARG_POI);
			poiId = poi.getId();
		}
		poi = DTHelper.findPOIById(poiId);
		return poi;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.poidetails, container, false);
	}

	private void updateRating(Integer result) {
		getPOI().getCommunityData().setAverageRating(result);
		RatingBar rating = (RatingBar) getView().findViewById(R.id.poi_rating);
		rating.setRating(getPOI().getCommunityData().getAverageRating());
	}

	@Override
	public void onStart() {
		super.onStart();

		if (getPOI() != null) {
			ImageView certifiedBanner = (ImageView) this.getView().findViewById(R.id.banner_certified);
			if (CategoryHelper.FAMILY_CATEGORY_POI.equals(getPOI().getType()) && isCertified(getPOI())) {
				certifiedBanner.setVisibility(View.VISIBLE);
			} else {
				certifiedBanner.setVisibility(View.GONE);
			}
			// title
			TextView tv = (TextView) this.getView().findViewById(R.id.poi_details_title);
			tv.setText(getPOI().getTitle());

			// BUTTONS
			ToggleButton followTbtn = (ToggleButton) this.getView().findViewById(R.id.poidetails_follow_tbtn);
			if (getPOI().getCommunityData().getFollowing().containsKey(DTHelper.getUserId())) {
				followTbtn.setBackgroundResource(R.drawable.ic_btn_monitor_on);
				followTbtn.setChecked(true);
			} else {
				followTbtn.setBackgroundResource(R.drawable.ic_btn_monitor_off);
				followTbtn.setChecked(false);
			}

			// follow/unfollow
			followTbtn.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (isChecked) {
						// FOLLOW
						FollowEntityObject obj = new FollowEntityObject(poi.getEntityId(), poi.getTitle(),
								DTConstants.ENTITY_TYPE_POI);
						if (mFollowByIntent) {
							FollowHelper.follow(mFragment, obj, 3000);
						} else {

							SCAsyncTask<Object, Void, Topic> followTask = new SCAsyncTask<Object, Void, Topic>(
									getSherlockActivity(), new FollowAsyncTaskProcessor(getSherlockActivity()));
							followTask.execute(getSherlockActivity().getApplicationContext(), DTParamsHelper.getAppToken(),
									DTHelper.getAuthToken(), obj);

						}
					} else {
						buttonView.setBackgroundResource(R.drawable.ic_btn_monitor_off);
						// UNFOLLOW
						BaseDTObject obj;
						try {
							obj = DTHelper.findPOIByEntityId(getPOI().getEntityId());
							if (obj != null) {
								SCAsyncTask<BaseDTObject, Void, BaseDTObject> unfollowTask = new SCAsyncTask<BaseDTObject, Void, BaseDTObject>(
										getSherlockActivity(), new UnfollowAsyncTaskProcessor(getSherlockActivity()));
								unfollowTask.execute(obj);

							}
						} catch (Exception e) {
							Log.e(EventDetailsFragment.class.getName(),
									String.format("Error unfollowing event %s", getPOI().getEntityId()));
						}
					}
				}
			});

			// map
			ImageButton mapBtn = (ImageButton) getView().findViewById(R.id.poidetails_map);
			mapBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					ArrayList<BaseDTObject> list = new ArrayList<BaseDTObject>();
					list.add(poi);
					MapManager.switchToMapView(list, mFragment);
				}
			});

			// directions
			ImageButton directionsBtn = (ImageButton) getView().findViewById(R.id.poidetails_directions);
			directionsBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Address to = poi.asGoogleAddress();
					Address from = null;
					GeoPoint mylocation = MapManager.requestMyLocation(getActivity());
					if (mylocation != null) {
						from = new Address(Locale.getDefault());
						from.setLatitude(mylocation.getLatitudeE6() / 1E6);
						from.setLongitude(mylocation.getLongitudeE6() / 1E6);
					}
					NavigationHelper.bringMeThere(getActivity(), from, to);
				}
			});

			// description, optional
			tv = (TextView) this.getView().findViewById(R.id.poi_details_descr);
			if (poi.getDescription() != null && poi.getDescription().length() > 0) {
				tv.setText(poi.getDescription());
			} else {
				((LinearLayout) this.getView().findViewById(R.id.poidetails)).removeView(tv);
			}

			// notes
			tv = (TextView) this.getView().findViewById(R.id.poi_details_notes);
			if (poi.getCommunityData() != null && poi.getCommunityData().getNotes() != null
					&& poi.getCommunityData().getNotes().length() > 0) {
				tv.setText(poi.getCommunityData().getNotes());
			} else {
				((LinearLayout) this.getView().findViewById(R.id.poidetails)).removeView(tv);
			}

			// location
			tv = (TextView) this.getView().findViewById(R.id.poi_details_loc);
			tv.setText(Html.fromHtml("<a href=\"\">" + poi.shortAddress() + "</a> "));
			tv.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					ArrayList<BaseDTObject> list = new ArrayList<BaseDTObject>();
					list.add(poi);
					MapManager.switchToMapView(list, PoiDetailsFragment.this);
				}
			});

			// tags
			tv = (TextView) this.getView().findViewById(R.id.poi_details_tags);
			if (poi.getCommunityData() != null && poi.getCommunityData().getTags() != null
					&& poi.getCommunityData().getTags().size() > 0) {
				tv.setText(Concept.toSimpleString(poi.getCommunityData().getTags()));
			} else {
				((LinearLayout) this.getView().findViewById(R.id.poidetails)).removeView(tv);
			}

			// multimedia
			((LinearLayout) getView().findViewById(R.id.multimedia_source))
					.removeView(getView().findViewById(R.id.gallery_btn));

			/*
			 * ImageButton b = (ImageButton) getView().findViewById(
			 * R.id.gallery_btn); if (hasMultimediaAttached())
			 * b.setOnClickListener(new OnClickListener() {
			 * 
			 * @Override public void onClick(View v) { FragmentTransaction
			 * fragmentTransaction = getSherlockActivity()
			 * .getSupportFragmentManager().beginTransaction(); GalleryFragment
			 * fragment = new GalleryFragment(); Bundle args = new Bundle(); //
			 * add args args.putString("title", poi.getTitle());
			 * fragment.setArguments(args); fragmentTransaction
			 * .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
			 * fragmentTransaction.replace(android.R.id.content, fragment,
			 * "gallery");
			 * fragmentTransaction.addToBackStack(fragment.getTag());
			 * fragmentTransaction.commit(); } }); else ((LinearLayout)
			 * this.getView().findViewById(R.id.tablerow)) .removeView(b);
			 */
			// source
			tv = (TextView) this.getView().findViewById(R.id.poi_details_source);
			if (poi.getSource() != null && poi.getSource().length() > 0) {
				/* Source is "ou" sometimes O_o */
				tv.setText(poi.getSource());
			} else if (poi.createdByUser()) {
				tv.setText(getString(R.string.source_smartcampus));
			} else {
				((LinearLayout) this.getView().findViewById(R.id.poidetails)).removeView(tv);
			}

			// rating
			RatingBar rating = (RatingBar) getView().findViewById(R.id.poi_rating);
			rating.setOnTouchListener(new View.OnTouchListener() {

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if (event.getAction() == MotionEvent.ACTION_UP) {
						if (new AMSCAccessProvider().isUserAnonymous(getSherlockActivity())) {
							// show dialog box
							UserRegistration.upgradeuser(getSherlockActivity());
							return false;
						} else {
							ratingDialog();
						}
					}
					return true;
				}
			});
			if (poi.getCommunityData() != null) {
				rating.setRating(poi.getCommunityData().getAverageRating());
			}

			if (tmp_comments.length > 0) {
				// Comments
				LinearLayout commentsList = (LinearLayout) getView().findViewById(R.id.comments_list);
				for (int i = 0; i < tmp_comments.length; i++) {
					View entry = getLayoutInflater(this.getArguments()).inflate(R.layout.comment_row, null);

					TextView tmp = (TextView) entry.findViewById(R.id.comment_text);
					tmp.setText(tmp_comments[i].getText());
					tmp = (TextView) entry.findViewById(R.id.comment_author);
					tmp.setText(tmp_comments[i].getAuthor());
					tmp = (TextView) entry.findViewById(R.id.comment_date);
					tmp.setText(tmp_comments[i].getDate());
					commentsList.addView(entry);
				}
			} else {
				((LinearLayout) getView().findViewById(R.id.poidetails)).removeView(getView().findViewById(R.id.poi_comments));
				((LinearLayout) getView().findViewById(R.id.poidetails)).removeView(getView().findViewById(R.id.comments_list));
				((LinearLayout) getView().findViewById(R.id.poidetails)).removeView(getView().findViewById(
						R.id.poi_comments_separator));
			}

		}
	}

	private boolean isCertified(POIObject poi) {
		String status = (String) poi.getCustomData().get("status");
		if (("Certificato finale").equals(status) || ("Certificato base").equals(status)) {
			return true;
		} else {
			return false;
		}
	}

	private boolean hasMultimediaAttached() {
		return true;
	}

	private void setFollowByIntent() {
		try {
			ApplicationInfo ai = getSherlockActivity().getPackageManager().getApplicationInfo(
					getSherlockActivity().getPackageName(), PackageManager.GET_META_DATA);
			Bundle aBundle = ai.metaData;
			mFollowByIntent = aBundle.getBoolean("follow-by-intent");
		} catch (NameNotFoundException e) {
			mFollowByIntent = false;
			Log.e(PoiDetailsFragment.class.getName(), "you should set the follow-by-intent metadata in app manifest");
		}

	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		getSherlockActivity().getSupportMenuInflater().inflate(R.menu.gripmenu, menu);

		String userId = DTHelper.getUserId();

		SubMenu submenu = menu.getItem(0).getSubMenu();
		submenu.clear();

		// submenu.add(Menu.CATEGORY_SYSTEM, R.id.submenu_rate, Menu.NONE,
		// R.string.rate);
		// submenu.add(Menu.NONE, R.id.submenu_get_dir, Menu.NONE,
		// R.string.getdir);
		// submenu.add(Menu.NONE, R.id.submenu_see_on_map, Menu.NONE,
		// R.string.onmap);

		submenu.add(Menu.NONE, R.id.submenu_show_related_events, Menu.NONE, R.string.submenu_related_events);
		submenu.add(Menu.CATEGORY_SYSTEM, R.id.submenu_tag, Menu.NONE, R.string.submenu_tag);

		// ONLY THE OWNER CAN EDIT AND DELETE OBJECTS
		if (DTHelper.isOwnedObject(getPOI())) {
			submenu.add(Menu.CATEGORY_SYSTEM, R.id.submenu_edit, Menu.NONE, R.string.edit);
			submenu.add(Menu.CATEGORY_SYSTEM, R.id.submenu_delete, Menu.NONE, R.string.delete);
		}

		super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.submenu_show_related_events) {
			/*
			 * It would be great if we could already know whether there are any
			 * events related to this POI: in case there were not, this entry in
			 * the menu could be omitted
			 */
			FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
			EventsListingFragment fragment = new EventsListingFragment();
			Bundle args = new Bundle();
			args.putString(EventsListingFragment.ARG_POI, poi.getId());
			args.putString(EventsListingFragment.ARG_POI_NAME, poi.getTitle());
			fragment.setArguments(args);
			fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
			// fragmentTransaction.detach(this);
			fragmentTransaction.replace(android.R.id.content, fragment, "pois");
			fragmentTransaction.addToBackStack(fragment.getTag());
			fragmentTransaction.commit();
			return true;
		} else if (item.getItemId() == R.id.submenu_edit) {
			if (new AMSCAccessProvider().isUserAnonymous(getSherlockActivity())) {
				// show dialog box
				UserRegistration.upgradeuser(getSherlockActivity());
				return false;
			} else {
				FragmentTransaction fragmentTransaction = getSherlockActivity().getSupportFragmentManager().beginTransaction();
				Fragment fragment = new CreatePoiFragment();
				Bundle args = new Bundle();
				args.putSerializable(CreatePoiFragment.ARG_POI, poi);
				fragment.setArguments(args);
				fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
				// fragmentTransaction.detach(this);
				fragmentTransaction.replace(android.R.id.content, fragment, "pois");
				fragmentTransaction.addToBackStack(fragment.getTag());
				fragmentTransaction.commit();
				return true;
			}
		} else if (item.getItemId() == R.id.submenu_delete) {
			if (new AMSCAccessProvider().isUserAnonymous(getSherlockActivity())) {
				// show dialog box
				UserRegistration.upgradeuser(getSherlockActivity());
				return false;
			} else {
				new SCAsyncTask<POIObject, Void, Boolean>(getActivity(), new POIDeleteProcessor(getActivity())).execute(poi);
				return true;
			}
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 3000) {
			if (resultCode == Activity.RESULT_OK) {
				eu.trentorise.smartcampus.cm.model.Topic topic = (eu.trentorise.smartcampus.cm.model.Topic) data
						.getSerializableExtra("topic");
				new FollowAsyncTask().execute(topic.getId());
				// fix to avoid onActivityResult DiscoverTrentoActivity failure
				data.putExtra(AccountManager.KEY_AUTHTOKEN, DTHelper.getAuthToken());
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void ratingDialog() {
		float rating = (poi != null && poi.getCommunityData() != null && poi.getCommunityData().getAverageRating() > 0) ? poi
				.getCommunityData().getAverageRating() : 2.5f;
		RatingHelper
				.ratingDialog(getActivity(), rating, new RatingProcessor(getActivity()), R.string.rating_place_dialog_title);
	}

	/*
	 * CLASSES
	 */
	private class RatingProcessor extends AbstractAsyncTaskProcessor<Integer, Integer> implements RatingHandler {
		public RatingProcessor(Activity activity) {
			super(activity);
		}

		@Override
		public Integer performAction(Integer... params) throws SecurityException, Exception {
			return DTHelper.rate(poi, params[0]);
		}

		@Override
		public void handleResult(Integer result) {
			updateRating(result);
			Toast.makeText(getSherlockActivity(), R.string.rating_success, Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onRatingChanged(float rating) {
			new SCAsyncTask<Integer, Void, Integer>(getActivity(), this).execute((int) rating);
		}
	}

	private class POIDeleteProcessor extends AbstractAsyncTaskProcessor<POIObject, Boolean> {
		public POIDeleteProcessor(Activity activity) {
			super(activity);
		}

		@Override
		public Boolean performAction(POIObject... params) throws SecurityException, Exception {
			return DTHelper.deletePOI(params[0]);
		}

		@Override
		public void handleResult(Boolean result) {
			if (result) {
				getSherlockActivity().getSupportFragmentManager().popBackStack();
			} else {
				Toast.makeText(getActivity(), getActivity().getString(R.string.app_failure_cannot_delete), Toast.LENGTH_LONG)
						.show();
			}
		}

	}

	class FollowAsyncTask extends AsyncTask<String, Void, Void> {
		@Override
		protected Void doInBackground(String... params) {
			String topicId = params[0];
			try {
				DTHelper.follow(DTHelper.findPOIById(poiId), topicId);
			} catch (Exception e) {
				Log.e(FollowAsyncTask.class.getName(), String.format("Exception following event %s", poiId));
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			getSherlockActivity().invalidateOptionsMenu();
		}

	}

}

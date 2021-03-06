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
package eu.trentorise.smartcampus.dt.fragments.stories;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import eu.trentorise.smartcampus.android.common.tagging.SemanticSuggestion;
import eu.trentorise.smartcampus.android.common.tagging.TaggingDialog.OnTagsSelectedListener;
import eu.trentorise.smartcampus.android.common.tagging.TaggingDialog.TagProvider;
import eu.trentorise.smartcampus.android.common.validation.ValidatorHelper;
import eu.trentorise.smartcampus.android.feedback.fragment.FeedbackFragment;
import eu.trentorise.smartcampus.dt.DiscoverTrentoActivity;
import eu.trentorise.smartcampus.dt.R;
import eu.trentorise.smartcampus.dt.custom.Utils;
import eu.trentorise.smartcampus.dt.custom.ViewHelper;
import eu.trentorise.smartcampus.dt.custom.data.DTHelper;
import eu.trentorise.smartcampus.dt.fragments.events.POISelectActivity;
import eu.trentorise.smartcampus.dt.model.LocalStepObject;
import eu.trentorise.smartcampus.territoryservice.model.POIObject;
import eu.trentorise.smartcampus.territoryservice.model.StoryObject;

/*
 * Fragment that manages a single step of a story with the POI and the note
 */

public class AddStepToStoryFragment extends FeedbackFragment implements OnTagsSelectedListener, TagProvider {

	public static String ARG_STEP_HANDLER = "handler";
	public static String ARG_STORY_OBJECT = "story";
	public static String ARG_STEP_POSITION = "position";
	private View view = null;
	private POIObject poi = null;
	private LocalStepObject step = null;
	private StepHandler stepHandler = null;
	private StoryObject storyObject = null;
	private Integer position = null;
	private static final String TAG = "AddStepToStoryFragment";

	@Override
	public void onTagsSelected(Collection<SemanticSuggestion> suggestions) {

	}

	@Override
	public void onSaveInstanceState(Bundle arg0) {
		super.onSaveInstanceState(arg0);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		

        
		if (Log.isLoggable(TAG, Log.VERBOSE)) {
			Log.v(TAG, "eu.trentorise.smartcampus.dt.fragments.stories.AddStepToStoryFragment.onCreate ");
		}

		setHasOptionsMenu(false);

		if (getArguments() != null && getArguments().containsKey(ARG_STEP_HANDLER)
				&& getArguments().getParcelable(ARG_STEP_HANDLER) != null) {
			stepHandler = (StepHandler) getArguments().getParcelable(ARG_STEP_HANDLER);
		}
		if (getArguments() != null && getArguments().containsKey(ARG_STORY_OBJECT)
				&& getArguments().getSerializable(ARG_STORY_OBJECT) != null) {
			storyObject = (StoryObject) getArguments().getSerializable(ARG_STORY_OBJECT);

		}
		if (getArguments() != null && getArguments().containsKey(ARG_STEP_POSITION)
				&& getArguments().getSerializable(ARG_STEP_POSITION) != null) {
			position = getArguments().getInt(ARG_STEP_POSITION);

		}

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		if (Log.isLoggable(TAG, Log.VERBOSE)) {
			Log.v(TAG, "eu.trentorise.smartcampus.dt.fragments.stories.AddStepToStoryFragment.onCreateView ");
		}
		view = inflater.inflate(R.layout.createstepform, container, false);

		// title
		AutoCompleteTextView poiField = (AutoCompleteTextView) view.findViewById(R.id.step_place);
		ArrayAdapter<String> stepAdapter = new ArrayAdapter<String>(getSherlockActivity(), R.layout.dd_list, R.id.dd_textview,
				DTHelper.getAllPOITitles());
		poiField.setAdapter(stepAdapter);

		// load the poi's title
		if (poi != null) {
			poiField.setText(poi.getTitle());
		} else if ((storyObject != null) && (position != null)) {
			step = Utils.getLocalStepFromStep(storyObject.getSteps().get(position));
			poi = step.assignedPoi();
			if (poi != null)
				poiField.setText(poi.getTitle());

		}

		// button linked to the map where choose the POIs
		ImageButton locationBtn = (ImageButton) view.findViewById(R.id.btn_step_locate);
		locationBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivityForResult(new Intent(getActivity(), POISelectActivity.class), POISelectActivity.RESULT_SELECTED);
			}
		});

		// note
		TextView noteField = (TextView) view.findViewById(R.id.step_tags);
		if (step != null) {
			noteField.setText(step.getNote());
		}

		// cancel
		Button cancel = (Button) view.findViewById(R.id.btn_createstep_cancel);
		cancel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getSherlockActivity().getSupportFragmentManager().popBackStack();
			}

		});

		// save
		Button save = (Button) view.findViewById(R.id.btn_createstep_ok);
		save.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// get the data from the user interface and call the correct
				// method of the handler
				// if the step is new or an update
				AutoCompleteTextView stepPlace = (AutoCompleteTextView) view.findViewById(R.id.step_place);
				TextView notePlace = (TextView) view.findViewById(R.id.step_tags);

				if (stepPlace.getText() != null && stepPlace.getText().length() > 0) {
					if (poi == null || !poi.getTitle().equals(stepPlace.getText())) {
						 poi = DTHelper.findPOIByTitle(stepPlace.getText().toString());
					}
					if (poi == null) {
						ValidatorHelper.highlight(
								getActivity(), 
								view.findViewById(R.id.poi_title), 
								getString(R.string.poi_step_not_exist));
						return;
					}
					step = new LocalStepObject(poi, notePlace.getText().toString());
					if (stepHandler != null) {
						if ((storyObject != null) && (position != null))
							stepHandler.updateStep(step, position);
						else
							stepHandler.addStep(step);

					}

				} else {
					ValidatorHelper.highlight(
							getActivity(), 
							view.findViewById(R.id.step_place), 
							getString(R.string.toast_is_required_p, getString(R.string.create_place)));
				}
			}
		});

		return view;
	}

	@Override
	public void onStart() {
		// TODO Auto-generated me
		DiscoverTrentoActivity.mDrawerToggle.setDrawerIndicatorEnabled(false);
    	DiscoverTrentoActivity.drawerState = "off";
        getSherlockActivity().getSupportActionBar().setHomeButtonEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayShowTitleEnabled(true);
        super.onStart();
	}
	@Override
	public void onPause() {
		super.onPause();
		// hide the keyboard
		TextView noteField = (TextView) view.findViewById(R.id.step_tags);
		ViewHelper.hideKeyboard(getSherlockActivity(), noteField);

	}

	// get the poi from the choosing POIs activity
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent result) {
		super.onActivityResult(requestCode, resultCode, result);

		if (Log.isLoggable(TAG, Log.VERBOSE)) {
			Log.v(TAG, "eu.trentorise.smartcampus.dt.fragments.stories.AddStepToStoryFragment.onActivityResult ");
		}

		if (resultCode == POISelectActivity.RESULT_SELECTED) {
			poi = (POIObject) result.getSerializableExtra("poi");
			AutoCompleteTextView text = (AutoCompleteTextView) view.findViewById(R.id.step_place);
			text.setText(poi.getTitle());
			for (int i = 0; i < text.getAdapter().getCount(); i++) {
				if (poi.getTitle().equals((text.getAdapter().getItem(i)))) {
					text.setListSelection(i);
				}
			}
		}
	}

	@Override
	public List<SemanticSuggestion> getTags(CharSequence text) {

		if (Log.isLoggable(TAG, Log.VERBOSE)) {
			Log.v(TAG, "eu.trentorise.smartcampus.dt.fragments.stories.AddStepToStoryFragment.getTags ");
		}

		try {
			return DTHelper.getSuggestions(text);
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}

	public interface StepHandler {
		public void addStep(LocalStepObject step);

		public void updateStep(LocalStepObject step, Integer position);
	}

}

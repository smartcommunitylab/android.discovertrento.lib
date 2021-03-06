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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;

import eu.trentorise.smartcampus.android.common.SCAsyncTask;
import eu.trentorise.smartcampus.android.common.SCAsyncTask.SCAsyncTaskProcessor;
import eu.trentorise.smartcampus.android.common.listing.AbstractLstingFragment;
import eu.trentorise.smartcampus.android.common.tagging.SemanticSuggestion;
import eu.trentorise.smartcampus.android.common.tagging.TaggingDialog;
import eu.trentorise.smartcampus.android.common.tagging.TaggingDialog.TagProvider;
import eu.trentorise.smartcampus.android.feedback.utils.FeedbackFragmentInflater;
import eu.trentorise.smartcampus.dt.DTParamsHelper;
import eu.trentorise.smartcampus.dt.DiscoverTrentoActivity;
import eu.trentorise.smartcampus.dt.R;
import eu.trentorise.smartcampus.dt.custom.AbstractAsyncTaskProcessor;
import eu.trentorise.smartcampus.dt.custom.CategoryHelper;
import eu.trentorise.smartcampus.dt.custom.CategoryHelper.CategoryDescriptor;
import eu.trentorise.smartcampus.dt.custom.StoryAdapter;
import eu.trentorise.smartcampus.dt.custom.StoryPlaceholder;
import eu.trentorise.smartcampus.dt.custom.Utils;
import eu.trentorise.smartcampus.dt.custom.data.DTHelper;
import eu.trentorise.smartcampus.dt.custom.data.FollowAsyncTaskProcessor;
import eu.trentorise.smartcampus.dt.fragments.search.SearchFragment;
import eu.trentorise.smartcampus.dt.fragments.search.WhenForSearch;
import eu.trentorise.smartcampus.dt.fragments.search.WhereForSearch;
import eu.trentorise.smartcampus.dt.model.StoryObjectForBean;
import eu.trentorise.smartcampus.protocolcarrier.exceptions.SecurityException;
import eu.trentorise.smartcampus.social.model.Concept;
import eu.trentorise.smartcampus.territoryservice.model.BaseDTObject;
import eu.trentorise.smartcampus.territoryservice.model.StoryObject;

/*
 * Fragment lists the stories of a category, my stories or the searched ones
 */
public class StoriesListingFragment extends AbstractLstingFragment<StoryObject> implements TagProvider {

	private String category;

	private ListView list;
	private Context context;
	private View clickedElement;
	public static final String ARG_ID = "id_story";
	public static final String ARG_INDEX = "index_adapter";
	private String idStory = "";
	private Integer indexAdapter;
	private Boolean reload = false;
	private Integer postitionSelected = 0;

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(ARG_ID, idStory);
		if (indexAdapter != null)
			outState.putInt(ARG_INDEX, indexAdapter);

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.context = this.getSherlockActivity();
		setHasOptionsMenu(true);
	}

	@Override
	public void onActivityCreated(Bundle arg0) {
		super.onActivityCreated(arg0);
		list = (ListView) getSherlockActivity().findViewById(R.id.stories_list);
		if (arg0 != null) {
			// Restore last state for checked position.
			idStory = arg0.getString(ARG_ID);
			indexAdapter = arg0.getInt(ARG_INDEX);

		}
		if (getStoriesAdapter() == null) {
			setAdapter(new StoryAdapter(context, R.layout.stories_row));
		} else {
			setAdapter(getStoriesAdapter());
		}

	}

	/**
	 * @return
	 */
	private StoryAdapter getStoriesAdapter() {
		return (StoryAdapter) getAdapter();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.stories_list, container, false);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!idStory.equals("")) {
			// get info of the event
			StoryObject story = DTHelper.findStoryById(idStory);

			if (story == null) {
				// cancellazione
				removeStory(indexAdapter);

			} else {
				// modifica se numero della versione e' diverso
				 if (story.getUpdateTime() != getStoriesAdapter().getItem(indexAdapter).getUpdateTime()) {
					removeStory(indexAdapter);
					insertStory(story);
				}
			}
			// notify
			getStoriesAdapter().notifyDataSetChanged();
			updateList(getAdapter().getCount() == 0);
			idStory = "";
			indexAdapter = 0;
		}
	}

	/*
	 * insert in the same adapter the new item
	 */
	private void insertStory(StoryObject story) {
		StoryAdapter storiesAdapter = getStoriesAdapter();
		// add in the right place
		int i = 0;
		boolean insert = false;
		while (i < storiesAdapter.getCount()) {
			if (storiesAdapter.getItem(i).getTitle() != null) {
				if (storiesAdapter.getItem(i).getTitle().toLowerCase().compareTo(story.getTitle().toLowerCase()) <= 0)
					i++;
				else {
					storiesAdapter.insert(story, i);
					insert = true;
					break;
				}
			}
		}

		if (!insert) {
			storiesAdapter.insert(story, storiesAdapter.getCount());
		}
	}

	/* clean the adapter from the items modified or erased */
	private void removeStory(Integer indexAdapter) {
		StoryAdapter storiesAdapter = getStoriesAdapter();
		StoryObject objectToRemove = storiesAdapter.getItem(indexAdapter);
		int i = 0;
		while (i < storiesAdapter.getCount()) {
			if (storiesAdapter.getItem(i).getEntityId() == objectToRemove.getEntityId())
				storiesAdapter.remove(storiesAdapter.getItem(i));
			else
				i++;
		}
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		getSherlockActivity().getSupportMenuInflater().inflate(R.menu.gripmenu, menu);

		SubMenu submenu = menu.getItem(0).getSubMenu();
		submenu.clear();

		if (getArguments() == null || !getArguments().containsKey(SearchFragment.ARG_QUERY)) {
			submenu.add(Menu.CATEGORY_SYSTEM, R.id.submenu_search, Menu.NONE, R.string.search_txt);
		}

		if (category == null)
			category = (getArguments() != null) ? getArguments().getString(SearchFragment.ARG_CATEGORY) : null;
		if (category != null) {
			String addString = getString(R.string.add_story_in_cat, getString(CategoryHelper.getCategoryDescriptorByCategoryFiltered(
					CategoryHelper.CATEGORY_TYPE_STORIES, category).description));

			submenu.add(Menu.CATEGORY_SYSTEM, R.id.menu_item_addstory, Menu.NONE, addString);
		}

		//NotificationsSherlockFragmentDT.onPrepareOptionsMenuNotifications(menu);

		super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {


		if (item.getItemId() == R.id.menu_item_addstory) {
			// if (new
			// AMSCAccessProvider().isUserAnonymous(getSherlockActivity())) {
			// // show dialog box
			// UserRegistration.upgradeuser(getSherlockActivity());
			// return false;
			// } else
			{
				FragmentTransaction fragmentTransaction = getSherlockActivity().getSupportFragmentManager()
						.beginTransaction();
				Fragment fragment = new CreateStoryFragment();
				Bundle args = new Bundle();
				args.putString(SearchFragment.ARG_CATEGORY, category);
				fragment.setArguments(args);
				fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
				// fragmentTransaction.detach(this);
				fragmentTransaction.replace(R.id.fragment_container, fragment, "stories");
				fragmentTransaction.addToBackStack(fragment.getTag());
				fragmentTransaction.commit();
				reload = true;
				return true;
			}

		} else if (item.getItemId() == R.id.submenu_search) {
			FragmentTransaction fragmentTransaction;
			Fragment fragment;
			fragmentTransaction = getSherlockActivity().getSupportFragmentManager().beginTransaction();
			fragment = new SearchFragment();
			Bundle args = new Bundle();
			args.putString(SearchFragment.ARG_CATEGORY, category);
			args.putString(CategoryHelper.CATEGORY_TYPE_STORIES, CategoryHelper.CATEGORY_TYPE_STORIES);
			if (getArguments() != null && getArguments().containsKey(SearchFragment.ARG_MY)
					&& getArguments().getBoolean(SearchFragment.ARG_MY))
				args.putBoolean(SearchFragment.ARG_MY, getArguments().getBoolean(SearchFragment.ARG_MY));
			fragment.setArguments(args);
			fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
			fragmentTransaction.replace(R.id.fragment_container, fragment, "stories");
			fragmentTransaction.addToBackStack(fragment.getTag());
			fragmentTransaction.commit();
			/* add category to bundle */
			return true;

		} else
			return super.onOptionsItemSelected(item);
	}

	/*
	 * try to load the arguments, change the story for every case
	 */
	@Override
	public void onStart() {
		if (reload) {
			getStoriesAdapter().clear();
			reload = false;
		}
		DiscoverTrentoActivity.mDrawerToggle.setDrawerIndicatorEnabled(false);
    	DiscoverTrentoActivity.drawerState = "off";
        getSherlockActivity().getSupportActionBar().setHomeButtonEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayShowTitleEnabled(true);
        
		Bundle bundle = this.getArguments();
		String category = (bundle != null) ? bundle.getString(SearchFragment.ARG_CATEGORY) : null;
		CategoryDescriptor catDescriptor = CategoryHelper.getCategoryDescriptorByCategoryFiltered("stories", category);
		String categoryString = (catDescriptor != null) ? context.getResources().getString(catDescriptor.description)
				: null;

		TextView title = (TextView) getView().findViewById(R.id.list_title);
		if (category != null && categoryString != null) {
			title.setText(categoryString);
		} else if (bundle != null && bundle.containsKey(SearchFragment.ARG_MY)) {
			title.setText(R.string.mystory);
		} else if (bundle != null && bundle.containsKey(SearchFragment.ARG_QUERY)) {
			String query = bundle.getString(SearchFragment.ARG_QUERY);
			title.setText(context.getResources().getString(R.string.search_for) + " '" + query + "'");
			if (bundle.containsKey(SearchFragment.ARG_CATEGORY_SEARCH)) {
				category = bundle.getString(SearchFragment.ARG_CATEGORY_SEARCH);
				if (category != null)
					title.append(context.getResources().getString(R.string.search_in_category) + " " + category);
			}

		}

		// close items menus if open
		((View) list.getParent()).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				hideListItemsMenu(v, false);
			}
		});
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				hideListItemsMenu(view, false);
				setStorePoiId(view, position);
			}
		});

		FeedbackFragmentInflater.inflateHandleButton(getSherlockActivity(), getView());
		super.onStart();
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		super.onScrollStateChanged(view, scrollState);
		if ((postitionSelected != -1) && (scrollState == SCROLL_STATE_TOUCH_SCROLL)) {
			hideListItemsMenu(view, true);

		}
	}

	/*
	 * the contextual menu for every item in the list
	 */
	private void setStorePoiId(View v, int position) {
		final StoryObject story = ((StoryPlaceholder) v.getTag()).story;
		idStory = story.getId();
		indexAdapter = position;
	}

	protected void setupOptionsListeners(ViewSwitcher vs, final int position) {
		final StoryObject story = ((StoryPlaceholder) vs.getTag()).story;

		ImageButton b = (ImageButton) vs.findViewById(R.id.story_delete_btn);
		if (DTHelper.isOwnedObject(story)) {
			b.setVisibility(View.VISIBLE);
			b.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					{
						new SCAsyncTask<StoryObject, Void, Boolean>(getActivity(), new StoryDeleteProcessor(
								getActivity())).execute(story);
					}
				}
			});
		} else {
			b.setVisibility(View.GONE);
		}

		b = (ImageButton) vs.findViewById(R.id.story_edit_btn);
		b.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				{
					// load pois of the story
					FragmentTransaction fragmentTransaction = getSherlockActivity().getSupportFragmentManager()
							.beginTransaction();
					Fragment fragment = new CreateStoryFragment();
					Bundle args = new Bundle();
					args.putSerializable(CreateStoryFragment.ARG_STORY, story);
					fragment.setArguments(args);
					fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
					fragmentTransaction.replace(R.id.fragment_container, fragment, "stories");
					fragmentTransaction.addToBackStack(fragment.getTag());
					fragmentTransaction.commit();
				}
			}
		});

		b = (ImageButton) vs.findViewById(R.id.story_tag_btn);
		b.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				{
					TaggingDialog taggingDialog = new TaggingDialog(getActivity(),
							new TaggingDialog.OnTagsSelectedListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onTagsSelected(Collection<SemanticSuggestion> suggestions) {
									new TaggingAsyncTask(story).execute(Utils.conceptConvertSS(suggestions));
								}
							}, StoriesListingFragment.this, Utils
									.conceptConvertToSS(story.getCommunityData().getTags()));
					taggingDialog.show();
				}

			}
		});
		b = (ImageButton) vs.findViewById(R.id.story_follow_btn);
		b.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				SCAsyncTask<Object, Void, BaseDTObject> followTask = new SCAsyncTask<Object, Void, BaseDTObject>(
						getSherlockActivity(), new FollowAsyncTaskProcessor(getSherlockActivity(), null));
				followTask.execute(getSherlockActivity().getApplicationContext(), DTParamsHelper.getAppToken(),
						DTHelper.getAuthToken(), story);
			}
		});
	}

	private void hideListItemsMenu(View v, boolean close) {
		boolean toBeHidden = false;
		for (int index = 0; index < list.getChildCount(); index++) {
			View view = list.getChildAt(index);
			if (view instanceof ViewSwitcher && ((ViewSwitcher) view).getDisplayedChild() == 1) {
				((ViewSwitcher) view).showPrevious();
				toBeHidden = true;
				getStoriesAdapter().setElementSelected(-1);
				postitionSelected = -1;
			}
		}
		if (!toBeHidden && v != null && v.getTag() != null && !close) {
			// no items needed to be flipped, fill and open details page
			FragmentTransaction fragmentTransaction = getSherlockActivity().getSupportFragmentManager()
					.beginTransaction();
			StoryDetailsFragment fragment = new StoryDetailsFragment();

			Bundle args = new Bundle();
			args.putString(StoryDetailsFragment.ARG_STORY_ID, ((StoryPlaceholder) v.getTag()).story.getId());
			fragment.setArguments(args);

			fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
			fragmentTransaction.replace(R.id.fragment_container, fragment, "stories");
			fragmentTransaction.addToBackStack(fragment.getTag());
			fragmentTransaction.commit();
		}
	}

	/*
	 * get all the stories of a category. Used in combination with a asynch task
	 */
	private List<StoryObject> getStories(AbstractLstingFragment.ListingRequest... params) {
		try {
			Collection<StoryObjectForBean> result = null;
			List<StoryObject> returnArray = new ArrayList<StoryObject>();

			Bundle bundle = getArguments();
			boolean my = false;
			if (bundle.getBoolean(SearchFragment.ARG_MY))
				my = true;
			String categories = bundle.getString(SearchFragment.ARG_CATEGORY);
			SortedMap<String, Integer> sort = new TreeMap<String, Integer>();
			sort.put("title", 1);
			if (bundle.containsKey(SearchFragment.ARG_CATEGORY)
					&& (bundle.getString(SearchFragment.ARG_CATEGORY) != null)) {

				result = DTHelper.searchInGeneral(params[0].position, params[0].size,
						bundle.getString(SearchFragment.ARG_QUERY),
						(WhereForSearch) bundle.getParcelable(SearchFragment.ARG_WHERE_SEARCH),
						(WhenForSearch) bundle.getParcelable(SearchFragment.ARG_WHEN_SEARCH), my,
						StoryObjectForBean.class, sort, categories);

			} else if (bundle.containsKey(SearchFragment.ARG_MY) && (bundle.getBoolean(SearchFragment.ARG_MY))) {

				result = DTHelper.searchInGeneral(params[0].position, params[0].size,
						bundle.getString(SearchFragment.ARG_QUERY),
						(WhereForSearch) bundle.getParcelable(SearchFragment.ARG_WHERE_SEARCH),
						(WhenForSearch) bundle.getParcelable(SearchFragment.ARG_WHEN_SEARCH), my,
						StoryObjectForBean.class, sort, categories);

			} else if (bundle.containsKey(SearchFragment.ARG_QUERY)) {

				result = DTHelper.searchInGeneral(params[0].position, params[0].size,
						bundle.getString(SearchFragment.ARG_QUERY),
						(WhereForSearch) bundle.getParcelable(SearchFragment.ARG_WHERE_SEARCH),
						(WhenForSearch) bundle.getParcelable(SearchFragment.ARG_WHEN_SEARCH), my,
						StoryObjectForBean.class, sort, categories);

			} else if (bundle.containsKey(SearchFragment.ARG_LIST)) {
				List<StoryObjectForBean> results = (List<StoryObjectForBean>) bundle.get(SearchFragment.ARG_LIST);
				for (StoryObjectForBean storyBean : results) {
					returnArray.add(storyBean.getObjectForBean());
				}
				return returnArray;
			} else {
				return Collections.emptyList();
			}

			for (StoryObjectForBean storyBean : result) {
				returnArray.add(storyBean.getObjectForBean());
			}
			return returnArray;

		} catch (Exception e) {
			Log.e(StoriesListingFragment.class.getName(), e.getMessage());
			e.printStackTrace();
			return Collections.emptyList();
		}
	}

	/*
	 * Asynchtask that get all the stories
	 */
	private class StoryLoader extends
			AbstractAsyncTaskProcessor<AbstractLstingFragment.ListingRequest, List<StoryObject>> {

		public StoryLoader(Activity activity) {
			super(activity);
		}

		@Override
		public List<StoryObject> performAction(AbstractLstingFragment.ListingRequest... params)
				throws SecurityException, Exception {
			return getStories(params);
		}

		@Override
		public void handleResult(List<StoryObject> result) {
			updateList(result == null || result.isEmpty());
		}

	}

	@Override
	public List<SemanticSuggestion> getTags(CharSequence text) {
		try {
			return DTHelper.getSuggestions(text);
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}

	private class TaggingAsyncTask extends SCAsyncTask<List<Concept>, Void, Void> {

		public TaggingAsyncTask(final StoryObject s) {
			super(getSherlockActivity(), new AbstractAsyncTaskProcessor<List<Concept>, Void>(getSherlockActivity()) {
				@Override
				public Void performAction(List<Concept>... params) throws SecurityException, Exception {
					s.getCommunityData().setTags(params[0]);
					DTHelper.saveStory(s);
					return null;
				}

				@Override
				public void handleResult(Void result) {
					Toast.makeText(getSherlockActivity(), R.string.tags_successfully_added, Toast.LENGTH_SHORT).show();
				}
			});
		}
	}

	private class StoryDeleteProcessor extends AbstractAsyncTaskProcessor<StoryObject, Boolean> {
		private StoryObject object = null;

		public StoryDeleteProcessor(Activity activity) {
			super(activity);
		}

		@Override
		public Boolean performAction(StoryObject... params) throws SecurityException, Exception {
			object = params[0];
			return DTHelper.deleteStory(params[0]);
		}

		@Override
		public void handleResult(Boolean result) {
			if (result) {
				((StoryAdapter) list.getAdapter()).remove(object);
				((StoryAdapter) list.getAdapter()).notifyDataSetChanged();
				hideListItemsMenu(clickedElement, false);
				updateList(((StoryAdapter) list.getAdapter()).isEmpty());
			} else {
				Toast.makeText(getActivity(), R.string.app_failure_cannot_delete, Toast.LENGTH_LONG).show();
			}
		}

	}

	@Override
	protected SCAsyncTaskProcessor<AbstractLstingFragment.ListingRequest, List<StoryObject>> getLoader() {
		return new StoryLoader(getActivity());
	}

	@Override
	protected ListView getListView() {
		return list;
	}

	private void updateList(boolean empty) {
		if (getView() != null) {

			eu.trentorise.smartcampus.dt.custom.ViewHelper.removeEmptyListView((LinearLayout) getView().findViewById(
					R.id.storylistcontainer));
			if (empty) {
				eu.trentorise.smartcampus.dt.custom.ViewHelper.addEmptyListView((LinearLayout) getView().findViewById(
						R.id.storylistcontainer));
			}
			hideListItemsMenu(null, false);
		}
	}
}

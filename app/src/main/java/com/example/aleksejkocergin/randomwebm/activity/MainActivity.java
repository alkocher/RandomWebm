package com.example.aleksejkocergin.randomwebm.activity;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v7.widget.SearchView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.apollographql.apollo.ApolloCallback;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.exception.ApolloException;
import com.example.aleksejkocergin.myapplication.TagsQuery;
import com.example.aleksejkocergin.randomwebm.RandomWebmApplication;
import com.example.aleksejkocergin.randomwebm.fragments.WebmListFragment;
import com.example.aleksejkocergin.randomwebm.fragments.RandomFragment;
import com.example.aleksejkocergin.randomwebm.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;



public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static long backPressed;
    private String createdAt = "createdAt";
    private RandomWebmApplication application;
    private Handler uiHandler = new Handler(Looper.getMainLooper());
    private ApolloCall<TagsQuery.Data> tagsCall;
    private ArrayAdapter<String> arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        application = (RandomWebmApplication) getApplicationContext();
        arrayAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_dropdown_item_1line);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.container, new RandomFragment()).commit();
        }
        loadTags();
    }

    private List<TagsQuery.GetTag> responseTag(Response<TagsQuery.Data> response) {
        List<TagsQuery.GetTag> tagList = new ArrayList<>();

        final TagsQuery.Data responseData = response.data();
        if (responseData == null) {
            return Collections.emptyList();
        }
        final List<TagsQuery.GetTag> tags = responseData.getTags();
        if (tags != null) {
            if (tags.size() > 0) {
                tagList.addAll(tags);
            }
        }
        return tagList;
    }

    private void loadTags() {

        final TagsQuery tagsQuery = TagsQuery.builder()
                .build();
        tagsCall = application.apolloClient()
                .query(tagsQuery);
        tagsCall.enqueue(tagsDataCallback);
    }

    private ApolloCall.Callback<TagsQuery.Data> tagsDataCallback = new ApolloCallback<>(new ApolloCall.Callback<TagsQuery.Data>() {
        @Override
        public void onResponse(@Nonnull Response<TagsQuery.Data> response) {
            if (responseTag(response) != null) {
                for (int i = 0; i < responseTag(response).size(); ++i) {
                    arrayAdapter.add(responseTag(response).get(i).name());
                }
            }
        }
        @Override
        public void onFailure(@Nonnull ApolloException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }, uiHandler);

    @Override
    public void onBackPressed() {

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (backPressed + 2000 > System.currentTimeMillis()) {
                super.onBackPressed();
            } else {
                Toast.makeText(getBaseContext(), R.string.exit, Toast.LENGTH_SHORT).show();
            }
            backPressed = System.currentTimeMillis();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        final SearchView.SearchAutoComplete searchAutoComplete = searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        if (searchManager != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        }
        searchAutoComplete.setAdapter(arrayAdapter);
        searchAutoComplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String queryString = (String) adapterView.getItemAtPosition(i);
                searchAutoComplete.setText("" + queryString);
                getIntent().putExtra("order", createdAt);
                getIntent().putExtra("tagName", queryString.toLowerCase());
                getSupportFragmentManager().beginTransaction().replace(R.id.container, new WebmListFragment()).commit();
                setTitle(queryString);
                // Call collapse action view on 'MenuItem'
                (menu.findItem(R.id.action_search)).collapseActionView();
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                getIntent().putExtra("order", createdAt);
                getIntent().putExtra("tagName", query.toLowerCase());
                getSupportFragmentManager().beginTransaction().replace(R.id.container, new WebmListFragment()).commit();
                (menu.findItem(R.id.action_search)).collapseActionView();
                setTitle(query);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        // Handle navigation view item clicks here.
        int id = item.getItemId();
        String tagName = "";
        WebmListFragment orderFragment = new WebmListFragment();

        if (id == R.id.nav_home) {
            getSupportFragmentManager().beginTransaction().replace(R.id.container, new RandomFragment()).commit();
        } else if (id == R.id.nav_recent) {
            String createdAt = "createdAt";
            getIntent().putExtra("order", createdAt);
            getIntent().putExtra("tagName", tagName);
            getSupportFragmentManager().beginTransaction().replace(R.id.container, orderFragment).commit();
        } else if (id == R.id.nav_top_rated) {
            String likes = "likes";
            getIntent().putExtra("order", likes);
            getIntent().putExtra("tagName", tagName);
            getSupportFragmentManager().beginTransaction().replace(R.id.container, orderFragment).commit();
        } else if (id == R.id.nav_most_viewed) {
            String views = "views";
            getIntent().putExtra("order", views);
            getIntent().putExtra("tagName", tagName);
            getSupportFragmentManager().beginTransaction().replace(R.id.container, orderFragment).commit();
        } else if (id == R.id.nav_favorite) {

        }
        item.setChecked(true);
        setTitle(item.getTitle());
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}

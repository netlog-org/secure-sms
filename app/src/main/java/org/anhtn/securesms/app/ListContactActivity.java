package org.anhtn.securesms.app;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import org.anhtn.securesms.R;

public class ListContactActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_contact);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_base);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_list_contact, menu);

        final SearchManager searchManager = (SearchManager)
                getSystemService(Context.SEARCH_SERVICE);
        final MenuItem item = menu.findItem(R.id.action_search);

        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false);
//        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
//
//            @Override
//            public boolean onQueryTextSubmit(String s) {
////                Intent i = new Intent(RealTimeActivity.this, RealTimeSearchActivity.class);
////                i.putExtra(SearchManager.QUERY, s);
////                i.setAction(Intent.ACTION_SEARCH);
////                startActivity(i);
//                return true;
//            }
//
//            @Override
//            public boolean onQueryTextChange(String s) {
//                return false;
//            }
//        });

        return true;
    }
}

package org.anhtn.securesms;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.PhoneLookup;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class MainActivity extends ActionBarActivity {

    private SmsListAdapter mAdapter;
    private ProgressBar pb;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_base);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.app_name);

        pb = (ProgressBar) findViewById(R.id.progress);

        mAdapter = new SmsListAdapter(this, R.layout.view_list_sms_item_1);
        listView = (ListView) findViewById(R.id.list);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(MainActivity.this, SmsContentActivity.class);
                final SmsObject smsObject = mAdapter.getItem(position);
                i.putExtra("address", smsObject.From);
                i.putExtra("addressInContact", smsObject.FromDisplayName);
                startActivity(i);
            }
        });
        listView.setAdapter(mAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        pb.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);
        mAdapter.clear();

        new Thread(new Runnable() {
            @Override
            public void run() {
                loadData();
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sms_content, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadData() {
        Uri inboxUri = Uri.parse("content://sms/");
        String[] reqCols = new String[] {"address, body"};
        final List<SmsObject> results = new ArrayList<>();

        Cursor c = getContentResolver().query(inboxUri, reqCols, null, null, "date DESC");
        Set<String> addressSet = new HashSet<>();
        if (c.moveToFirst()) {
            do {
                String address = c.getString(c.getColumnIndex("address"));
                if (address.startsWith("+84")) {
                    address = address.replace("+84", "0");
                }

                final boolean ok = addressSet.add(address);
                if (ok) {
                    SmsObject smsObject = new SmsObject();
                    smsObject.From = address;
                    smsObject.Content = c.getString(c.getColumnIndex("body"));
                    try {
                        String phoneNumber = String.valueOf(Long.parseLong(address));
                        smsObject.FromDisplayName = phoneLookup(phoneNumber);
                    } catch (NumberFormatException ignored) {}
                    results.add(smsObject);
                }
            } while (c.moveToNext());
        }
        c.close();

        pb.post(new Runnable() {
            @Override
            public void run() {
                for (SmsObject smsObject : results) {
                    mAdapter.add(smsObject);
                }
                pb.setVisibility(View.GONE);
                listView.setVisibility(View.VISIBLE);
            }
        });
    }

    private String phoneLookup(String phoneNumber) {
        Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber));
        Cursor c = getContentResolver().query(uri, new String[]{PhoneLookup.DISPLAY_NAME},
                null, null, null);
        if (c.moveToFirst()) {
            final String result = c.getString(c.getColumnIndex(PhoneLookup.DISPLAY_NAME));
            c.close();
            return result;
        }
        return null;
    }


    private static class SmsObject {
        public String From;
        public String FromDisplayName;
        public String Content;
    }


    private static class SmsListAdapter extends ArrayAdapter<SmsObject> {

        public SmsListAdapter(Context context, int resource) {
            super(context, resource);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater)
                        getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.view_list_sms_item_1, parent, false);
            }
            final SmsObject smsObject = getItem(position);
            final TextView textFrom = (TextView) view.findViewById(R.id.text_sms_from);
            final TextView textContent = (TextView) view.findViewById(R.id.text_sms_content);

            if (smsObject.FromDisplayName != null) {
                textFrom.setText(smsObject.FromDisplayName);
            } else {
                textFrom.setText(smsObject.From);
            }
            textContent.setText(smsObject.Content);

            return view;
        }
    }
}

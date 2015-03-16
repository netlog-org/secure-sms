package org.anhtn.securesms;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class SmsContentActivity extends ActionBarActivity {

    private SmsListAdapter mAdapter;
    private ProgressBar pb;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_content);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_base);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final String address = getIntent().getStringExtra("address");
        getSupportActionBar().setTitle(address);

        pb = (ProgressBar) findViewById(R.id.progress);

        mAdapter = new SmsListAdapter(this, R.layout.view_list_sms_item_1);
        listView = (ListView) findViewById(R.id.list);
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
        String[] reqCols = new String[] {"body", "date", "type"};
        final List<SmsObject> results = new ArrayList<>();

        CharSequence address = getSupportActionBar().getTitle();
        if (address == null) return;
        String selection;
        try {
            address = String.valueOf(Long.parseLong(address.toString()));
            selection = "address like '%" + address + "'";
        } catch (NumberFormatException ex) {
            selection = "address='" + address + "'";
        }

        Cursor c = getContentResolver().query(inboxUri, reqCols, selection,
                null, "date ASC");
        if (c.moveToFirst()) {
            do {
                SmsObject smsObject = new SmsObject();
                smsObject.Type = c.getInt(c.getColumnIndex("type"));
                if (smsObject.Type != SmsObject.TYPE_INBOX
                        && smsObject.Type != SmsObject.TYPE_SENT) {

                    Log.e("SecureSMS", "Ignore sms type: " + smsObject.Type);
                    continue;
                }
                Date date = new Date(Long.parseLong(c.getString(c.getColumnIndex("date"))));
                smsObject.Date = DateFormat.getInstance().format(date);
                smsObject.Content = c.getString(c.getColumnIndex("body"));
                results.add(smsObject);
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
                listView.setSelection(mAdapter.getCount() - 1);
            }
        });
    }


    private static class SmsObject {
        public static final int TYPE_INBOX = 1;
        public static final int TYPE_SENT = 2;

        public int Type;
        public String Content;
        public String Date;
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
                view = inflater.inflate(R.layout.view_list_sms_item_2, parent, false);
            }

            final SmsObject smsObject = getItem(position);
            LinearLayout container = (LinearLayout) view.findViewById(R.id.container);
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)
                    container.getLayoutParams();

            if (smsObject.Type == SmsObject.TYPE_INBOX) {
                params.leftMargin = getContext().getResources()
                        .getDimensionPixelSize(R.dimen.message_item_small_margin);
                params.rightMargin = getContext().getResources()
                        .getDimensionPixelSize(R.dimen.message_item_large_margin);
                container.setBackgroundResource(R.drawable.message_inbox_bg);
            } else {
                params.leftMargin = getContext().getResources()
                        .getDimensionPixelSize(R.dimen.message_item_large_margin);
                params.rightMargin = getContext().getResources()
                        .getDimensionPixelSize(R.dimen.message_item_small_margin);
                container.setBackgroundResource(R.drawable.message_sent_bg);
            }
            container.setLayoutParams(params);

            final TextView textContent = (TextView) view.findViewById(R.id.text_sms_content);
            textContent.setText(smsObject.Content);

            final TextView textDate = (TextView) view.findViewById(R.id.text_sms_date);
            textDate.setText(smsObject.Date);

            return view;
        }
    }
}

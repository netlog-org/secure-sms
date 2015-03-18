package org.anhtn.securesms.app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.anhtn.securesms.R;
import org.anhtn.securesms.utils.Global;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class MainActivity extends ActionBarActivity {

    public static boolean sLeaveFromChild = false;

    private SmsListAdapter mAdapter;
    private ProgressBar pb;
    private ListView listView;
    private HashMap<String, String> mContactData = new HashMap<>();

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
    protected void onStart() {
        super.onStart();
//        if (!sLeaveFromChild) {
//            showInputPasswordDialog();
//        } else sLeaveFromChild = false;

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

    @SuppressLint("InflateParams")
    private void showInputPasswordDialog() {
        View container = getLayoutInflater().inflate(R.layout.view_dialog_password, null);
        final EditText input = (EditText) container.findViewById(android.R.id.edit);
        input.setBackgroundResource(android.R.color.transparent);
        new AlertDialog.Builder(this)
                .setMessage("Input password to continue")
                .setView(container)
                .setCancelable(false)
                .setInverseBackgroundForced(true)
                .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!Global.DEFAULT_PASSWORD.equals(input.getText().toString())) {
                            showInputPasswordDialog();
                        }
                    }
                }).show();
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
                        if (mContactData.containsKey(phoneNumber)) {
                            smsObject.FromDisplayName = mContactData.get(phoneNumber);
                        } else {
                            smsObject.FromDisplayName = phoneLookup(phoneNumber);
                            mContactData.put(phoneNumber, smsObject.FromDisplayName);
                        }
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
            List<String> results = new ArrayList<>();
            do {
                results.add(c.getString(c.getColumnIndex(PhoneLookup.DISPLAY_NAME)));
            } while (c.moveToNext());
            c.close();

            if (results.isEmpty()) return null;
            else if (results.size() == 1) return results.get(0);
            else {
                StringBuilder builder = new StringBuilder(results.get(0));
                for (int i = 1; i < results.size(); i++) {
                    builder.append(results.get(i));
                    builder.append(", ");
                }
                return builder.toString();
            }
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
            final TextView textFrom = (TextView) view.findViewById(android.R.id.text1);
            final TextView textContent = (TextView) view.findViewById(android.R.id.text2);

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

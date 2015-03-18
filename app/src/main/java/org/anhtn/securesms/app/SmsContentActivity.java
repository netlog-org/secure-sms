package org.anhtn.securesms.app;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.RawContacts;
import android.provider.Telephony;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.anhtn.securesms.R;
import org.anhtn.securesms.crypto.AESHelper;
import org.anhtn.securesms.model.SmsObject;
import org.anhtn.securesms.utils.CacheHelper;
import org.anhtn.securesms.utils.Global;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class SmsContentActivity extends ActionBarActivity {

    private static final String INTENT_SMS_SENT = "org.anhtn.securesms.INTENT_SMS_SENT";

    private static final int MENU_COPY_ID = 123;
    private static final int MENU_FORWARD_ID = 456;
    private static final int MENU_DELETE_ID = 789;

    private SmsListAdapter mAdapter;
    private ProgressBar pb;
    private ListView listView;
    private TextView txtNewSms;
    private String mAddress, mCurrentMsgToSent;
    private int mCurrentPosLongClick = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_content);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_base);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mAddress = getIntent().getStringExtra("address");
        String addressInContact = getIntent().getStringExtra("addressInContact");
        if (addressInContact == null) {
            addressInContact = mAddress;
        }
        getSupportActionBar().setTitle(addressInContact);

        pb = (ProgressBar) findViewById(R.id.progress);
        txtNewSms = (TextView) findViewById(R.id.text);
        final String content = getIntent().getStringExtra("content");
        if (content != null) {
            txtNewSms.setText(content);
        }

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurrentMsgToSent = txtNewSms.getText().toString();
                if (mCurrentMsgToSent.length() > 0) {
                    txtNewSms.setText("");
                    sendSms(Global.ALGORITHM + AESHelper.encryptToBase64(
                            Global.DEFAULT_PASSWORD, mCurrentMsgToSent));
                }
            }
        });

        mAdapter = new SmsListAdapter(this, R.layout.view_list_sms_item_1);
        listView = (ListView) findViewById(R.id.list);
        listView.setAdapter(mAdapter);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                                           int position, long id) {
                mCurrentPosLongClick = position;
                return false;
            }
        });
        registerForContextMenu(listView);
    }

    @Override
    protected void onStart() {
        super.onStart();

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
    protected void onResume() {
        super.onResume();
        registerReceiver(mSmsSentReceiver, new IntentFilter(INTENT_SMS_SENT));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mSmsSentReceiver);
        MainActivity.sLeaveFromChild = true;
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
        if (id == R.id.action_call) {
            String uri = "tel:" + mAddress;
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse(uri));
            startActivity(intent);
            return true;
        }
        else if (id == R.id.action_delete) {
            return true;
        }
        else if (id == R.id.action_add_contact) {
            Intent intent = new Intent(Intents.Insert.ACTION);
            intent.setType(RawContacts.CONTENT_TYPE);
            intent.putExtra(Intents.Insert.PHONE, mAddress);
            intent.putExtra(Intents.Insert.PHONE_TYPE,
                    CommonDataKinds.Phone.TYPE_MOBILE);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == listView.getId()) {
            menu.setHeaderTitle(R.string.message_options);
            String[] menuItems = getResources().getStringArray(R.array.message_options);
            final int[] itemIds = new int[]{
                    MENU_COPY_ID,
                    MENU_FORWARD_ID,
                    MENU_DELETE_ID
            };
            for (int i = 0; i<menuItems.length; i++) {
                menu.add(Menu.NONE, itemIds[i], i, menuItems[i]);
            }
            return;
        }
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_COPY_ID:
                copyToClipboard();
                break;

            case MENU_FORWARD_ID:
                forwardMessage();
                break;

            case MENU_DELETE_ID:
                deleteMessage();
                break;

            default:
                return super.onContextItemSelected(item);
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private void showChooseContactDialog() {
        final List<SmsObject> list = (List<SmsObject>)
                CacheHelper.getInstance().get("sms");
        final String[] items = new String[list.size()];
        int i = 0;
        for (SmsObject obj : list) {
            items[i++] = (obj.AddressInContact != null)
                    ? obj.AddressInContact : obj.Address;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, items);

        new AlertDialog.Builder(this)
                .setTitle(R.string.action_add)
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, null)
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SmsMessage sms = mAdapter.getItem(mCurrentPosLongClick);
                        Intent i = new Intent(SmsContentActivity.this,
                                SmsContentActivity.class);
                        i.putExtra("content", sms.Content);
                        i.putExtra("address", list.get(which).Address);
                        i.putExtra("addressInContact", list.get(which).AddressInContact);
                        startActivity(i);
                    }
                }).show();
    }

    private void copyToClipboard() {
        if (mCurrentPosLongClick < 0) return;
        String text = mAdapter.getItem(mCurrentPosLongClick).Content;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager)
                    getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(text);
        } else {
            ClipboardManager clipboard = (ClipboardManager)
                    getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("secure-sms-content", text);
            clipboard.setPrimaryClip(clip);
        }
    }

    @SuppressWarnings("unchecked")
    private void forwardMessage() {
        final String[] items = new String[]{
                getResources().getString(R.string.action_add),
                getResources().getString(R.string.recent_list)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, items);
        final List<SmsObject> list = (List<SmsObject>)
                CacheHelper.getInstance().get("sms");

        new AlertDialog.Builder(this)
                .setTitle(R.string.action_add)
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, null)
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            Intent i = new Intent(SmsContentActivity.this,
                                    ListContactActivity.class);
                            i.putExtra("content",
                                    mAdapter.getItem(mCurrentPosLongClick).Content);
                            i.putExtra("address", list.get(which).Address);
                            startActivity(i);
                        } else {
                            showChooseContactDialog();
                        }
                    }
                }).show();
    }

    private void deleteMessage() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.delete_message)
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SmsMessage sms = mAdapter.getItem(mCurrentPosLongClick);
                        Uri uri = Uri.parse("content://sms/");
                        int ret = getContentResolver().delete(uri, "_id= ?",
                                new String[]{String.valueOf(sms.Id)});
                        if (ret > 0) {
                            deleteListViewItem(mCurrentPosLongClick);
                        }
                    }
                }).show();
    }

    private void deleteListViewItem(int position) {
        mAdapter.remove(mAdapter.getItem(position));
    }

    private void scrollListViewToBottom() {
        listView.setSelection(mAdapter.getCount() - 1);
    }

    private void loadData() {
        Uri uri = Uri.parse("content://sms/");
        String[] reqCols = new String[] {"_id", "body", "date", "type"};
        final List<SmsMessage> results = new ArrayList<>();

        CharSequence address = mAddress;
        if (address == null) return;
        String selection;
        try {
            address = String.valueOf(Long.parseLong(address.toString()));
            selection = "address like '%" + address + "'";
        } catch (NumberFormatException ex) {
            selection = "address='" + address + "'";
        }

        Cursor c = getContentResolver().query(uri, reqCols, selection,
                null, "date ASC");
        if (c.moveToFirst()) {
            do {
                SmsMessage sms = new SmsMessage();
                sms.Type = c.getInt(c.getColumnIndex("type"));
                sms.Id = c.getInt(c.getColumnIndex("_id"));
                if (sms.Type != SmsMessage.TYPE_INBOX
                        && sms.Type != SmsMessage.TYPE_SENT) {

                    Global.log("Ignore sms type: " + sms.Type);
                    continue;
                }
                try {
                    Date date = new Date(Long.parseLong(c.getString(c.getColumnIndex("date"))));
                    sms.Date = DateFormat.getInstance().format(date);
                    String content = c.getString(c.getColumnIndex("body"));
                    if (content.startsWith(Global.ALGORITHM)) {
                        content = content.replace(Global.ALGORITHM, "");
                        content = AESHelper.decryptFromBase64(Global.DEFAULT_PASSWORD, content);
                    }
                    sms.Content = content;
                    results.add(sms);
                } catch (NumberFormatException ignored) { }
            } while (c.moveToNext());
        }
        c.close();

        pb.post(new Runnable() {
            @Override
            public void run() {
                for (SmsMessage sms : results) {
                    mAdapter.add(sms);
                }
                pb.setVisibility(View.GONE);
                listView.setVisibility(View.VISIBLE);
                scrollListViewToBottom();
            }
        });
    }

    private void sendSms(String msg) {
        try {
            if (!PhoneNumberUtils.isWellFormedSmsAddress(mAddress)) {
                throw new RuntimeException("Not have well formed sms address");
            }
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(INTENT_SMS_SENT), 0);
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(mAddress, null, msg, pi, null);
            sendBroadcast(new Intent(INTENT_SMS_SENT));
        } catch (Exception ex) {
            Global.error("Failed to send sms: " + ex.getMessage());
        }
    }


    private BroadcastReceiver mSmsSentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mCurrentMsgToSent == null) return;

            SmsMessage sms = new SmsMessage();
            sms.Content = mCurrentMsgToSent;
            sms.Type = SmsMessage.TYPE_SENT;

            String sOk = DateFormat.getInstance().format(new Date(System.currentTimeMillis()));
            String sFail = "Send message failed";
            sms.Date = (getResultCode() == RESULT_OK) ? sOk : sFail;

            mAdapter.add(sms);
            scrollListViewToBottom();
            mCurrentMsgToSent = null;
        }
    };


    private static class SmsMessage {
        public static final int TYPE_INBOX = 1;
        public static final int TYPE_SENT = 2;

        public int Id;
        public int Type;
        public String Content;
        public String Date;
    }


    private static class SmsListAdapter extends ArrayAdapter<SmsMessage> {

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

            final SmsMessage sms = getItem(position);
            LinearLayout container = (LinearLayout) view.findViewById(R.id.container);
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)
                    container.getLayoutParams();

            if (sms.Type == SmsMessage.TYPE_INBOX) {
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

            final TextView textContent = (TextView) view.findViewById(android.R.id.text1);
            textContent.setText(sms.Content);

            final TextView textDate = (TextView) view.findViewById(android.R.id.text2);
            textDate.setText(sms.Date);

            return view;
        }
    }
}

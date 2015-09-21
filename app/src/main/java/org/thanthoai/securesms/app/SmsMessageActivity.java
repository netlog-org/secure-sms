package org.thanthoai.securesms.app;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.RawContacts;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
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
import android.widget.Toast;

import org.thanthoai.securesms.BuildConfig;
import org.thanthoai.securesms.R;
import org.thanthoai.securesms.crypto.AESHelper;
import org.thanthoai.securesms.loaders.SmsMessageLoader;
import org.thanthoai.securesms.model.SentMessageModel;
import org.thanthoai.securesms.model.SmsConversation;
import org.thanthoai.securesms.model.SmsMessage;
import org.thanthoai.securesms.services.DeleteMessageService;
import org.thanthoai.securesms.utils.CacheHelper;
import org.thanthoai.securesms.utils.Global;
import org.thanthoai.securesms.utils.Keys;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;


public class SmsMessageActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<List<SmsMessage>> {

    private static final String INTENT_SMS_SENT = "org.thanthoai.securesms.INTENT_SMS_SENT";
    private static final int UPDATE_PASSPHRASE_REQUEST_CODE = 0xaecf;

    private static final int MENU_COPY_ID = 123;
    private static final int MENU_FORWARD_ID = 456;
    private static final int MENU_DELETE_ID = 789;

    private SmsListAdapter mAdapter;
    private ProgressBar pb;
    private ListView listView;
    private TextView txtNewSms;
    private String mAddress;
    private String mPassphrase, mAppPassphrase;
    private int mCurrentPosLongClick = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_message);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_base);
        setSupportActionBar(toolbar);
        final ActionBar ab = getSupportActionBar();
        if (ab != null) ab.setDisplayHomeAsUpEnabled(true);

        final Intent i = getIntent();
        mPassphrase = i.getStringExtra("passphrase");
        mAppPassphrase = i.getStringExtra(Keys.APP_PASSPHRASE);
        if (BuildConfig.DEBUG && mPassphrase == null || mAppPassphrase == null) {
            throw new AssertionError("Not found encrypt passphrase or app passphrase");
        }

        mAddress = i.getStringExtra("address");
        String addressInContact = i.getStringExtra("addressInContact");
        if (addressInContact == null) {
            addressInContact = mAddress;
        }
        if (ab != null) ab.setTitle(addressInContact);

        pb = (ProgressBar) findViewById(R.id.progress);
        txtNewSms = (TextView) findViewById(R.id.text);
        txtNewSms.setHint(getString(R.string.type_message_hint, mAddress));
        final String content = i.getStringExtra("content");
        if (content != null) {
            txtNewSms.setText(content);
        }

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String text = txtNewSms.getText().toString();
                if (text.length() > 0) {
                    txtNewSms.setText("");
                    sendSms(text);
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

        setListViewVisible(false);
        mAdapter.clear();

        Bundle args = new Bundle();
        args.putString("address", mAddress);
        if (getSupportLoaderManager().getLoader(0) == null) {
            getSupportLoaderManager().initLoader(0, args, this);
        } else {
            getSupportLoaderManager().restartLoader(0, args, this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mSmsSentReceiver, new IntentFilter(INTENT_SMS_SENT));
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mDeleteMessageDoneReceiver,
                new IntentFilter(DeleteMessageService.DELETE_MESSAGE_DONE));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mSmsSentReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(
                mDeleteMessageDoneReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UPDATE_PASSPHRASE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                mPassphrase = data.getStringExtra("passphrase");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sms_message, menu);
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
            final ActionBar ab = getSupportActionBar();
            final CharSequence title = ab != null ? ab.getTitle() : "";
            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.delete_all_messages, title))
                    .setCancelable(true)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deleteAllMessageOfAddress();
                        }
                    }).show();
            return true;
        }
        else if (id == R.id.action_add_contact) {
            Intent intent = new Intent(Intents.Insert.ACTION);
            intent.setType(RawContacts.CONTENT_TYPE);
            intent.putExtra(Intents.Insert.PHONE, mAddress);
            intent.putExtra(Intents.Insert.PHONE_TYPE, CommonDataKinds.Phone.TYPE_MOBILE);
            startActivity(intent);
            return true;
        }
        else if (id == R.id.action_change_passphrase) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.change_passphrase_warning)
                    .setCancelable(false)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent i = new Intent(SmsMessageActivity.this, ChangeAesPassActivity.class);
                            i.putExtra("app_passphrase", mAppPassphrase);
                            i.putExtra("address", mAddress);
                            startActivityForResult(i, UPDATE_PASSPHRASE_REQUEST_CODE);
                        }
                    }).show();
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
                new AlertDialog.Builder(this)
                        .setMessage(R.string.delete_message)
                        .setCancelable(true)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteMessage();
                            }
                        }).show();
                break;

            default:
                return super.onContextItemSelected(item);
        }
        return true;
    }

    @Override
    public Loader<List<SmsMessage>> onCreateLoader(int id, Bundle args) {
        return new SmsMessageLoader(this, args.getString("address"), mPassphrase);
    }

    @Override
    public void onLoadFinished(Loader<List<SmsMessage>> loader,
                               List<SmsMessage> data) {
        for (SmsMessage sms : data) {
            mAdapter.add(sms);
        }
        setListViewVisible(true);
        scrollListViewToBottom();
    }

    @Override
    public void onLoaderReset(Loader<List<SmsMessage>> loader) {
        Global.log("Sms content loader reset");
    }

    @SuppressWarnings("deprecation")
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
        final List<SmsConversation> list = (List<SmsConversation>)
                CacheHelper.getInstance().get("sms");
        if (list == null) return;
        final String[] items = new String[list.size()];
        int i = 0;
        for (SmsConversation obj : list) {
            items[i++] = (obj.AddressInContact != null)
                    ? obj.AddressInContact : obj.Address;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, items);

        new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setCancelable(true)
                .setNegativeButton(R.string.new_sms, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent i = new Intent(SmsMessageActivity.this, ContactActivity.class);
                        i.putExtra(Keys.APP_PASSPHRASE, mAppPassphrase);
                        i.putExtra("content",mAdapter.getItem(mCurrentPosLongClick).Content);
                        startActivity(i);
                    }
                })
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SmsMessage sms = mAdapter.getItem(mCurrentPosLongClick);
                        Intent i = new Intent(SmsMessageActivity.this, SmsMessageActivity.class);
                        i.putExtra("content", sms.Content);
                        i.putExtra("address", list.get(which).Address);
                        i.putExtra("addressInContact", list.get(which).AddressInContact);
                        i.putExtra("passphrase", mPassphrase);
                        i.putExtra(Keys.APP_PASSPHRASE, mAppPassphrase);
                        startActivity(i);
                    }
                }).show();
    }

    private void deleteMessage() {
        boolean deleteSuccess;
        SmsMessage sms = mAdapter.getItem(mCurrentPosLongClick);
        if (sms.Type == SmsMessage.TYPE_ENCRYPTED) {
            SentMessageModel model = new SentMessageModel();
            model._Id = sms.Id;
            deleteSuccess = model.delete(this);
        } else {
            Uri uri = Uri.parse("content://sms/");
            deleteSuccess = getContentResolver().delete(uri, "_id= ?",
                    new String[]{String.valueOf(sms.Id)}) != -1;
        }
        if (deleteSuccess) {
            deleteListViewItem(mCurrentPosLongClick);
        }
    }

    private void deleteAllMessageOfAddress() {
        Intent i = new Intent(this, DeleteMessageService.class);
        i.setAction(Intent.ACTION_DELETE);
        i.putExtra("address", mAddress);
        startService(i);
    }

    private void deleteListViewItem(int position) {
        mAdapter.remove(mAdapter.getItem(position));
    }

    private void scrollListViewToBottom() {
        listView.setSelection(mAdapter.getCount() - 1);
    }

    private void setListViewVisible(boolean isVisible) {
        if (isVisible) {
            listView.setVisibility(View.VISIBLE);
            pb.setVisibility(View.GONE);
        } else {
            listView.setVisibility(View.INVISIBLE);
            pb.setVisibility(View.VISIBLE);
        }
    }

    private void sendSms(String msg) {
        String cipherText = AESHelper.encryptToBase64(mPassphrase, msg);
        try {
            if (cipherText == null)
                throw new NullPointerException();
            if (!PhoneNumberUtils.isWellFormedSmsAddress(mAddress))
                throw new RuntimeException("Not have well formed sms address");

            cipherText = Global.AES_PREFIX + cipherText;
            Intent i = new Intent(INTENT_SMS_SENT);
            i.putExtra("raw", msg);
            i.putExtra("encrypted", cipherText);
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
            SmsManager.getDefault().sendTextMessage(mAddress, null, cipherText, pi, null);
            sendBroadcast(new Intent(INTENT_SMS_SENT));
        } catch (Exception ex) {
            Global.error("Failed to send sms: " + ex.getMessage());
            Toast.makeText(this, R.string.send_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private final BroadcastReceiver mSmsSentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String rawMsg = intent.getStringExtra("raw");
            final String encryptedMsg = intent.getStringExtra("encrypted");
            if (rawMsg == null || encryptedMsg == null) return;

            final long currentTimeMillis = System.currentTimeMillis();
            SmsMessage sms = new SmsMessage();
            sms.Content = rawMsg;
            sms.Type = SmsMessage.TYPE_SENT;
            sms.Date = (getResultCode() == RESULT_OK)
                    ? DateFormat.getInstance().format(new Date(currentTimeMillis))
                    : getString(R.string.send_failed);
            mAdapter.add(sms);
            scrollListViewToBottom();

            SharedPreferences pref = PreferenceManager
                    .getDefaultSharedPreferences(SmsMessageActivity.this);
            if (pref.getBoolean("save_sent_message", false)) {
                SentMessageModel model = new SentMessageModel();
                model.Status = (getResultCode() == RESULT_OK)
                        ? SentMessageModel.STATUS_SENT_SUCCESS
                        : SentMessageModel.STATUS_SENT_FAIL;
                model.Date = String.valueOf(currentTimeMillis);
                model.Body = encryptedMsg;
                model.Address = mAddress;
                if (model.insert(SmsMessageActivity.this) == -1) {
                    Global.error("Save sent message to database failed");
                }
            }
        }
    };

    private final BroadcastReceiver mDeleteMessageDoneReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.getBooleanExtra("result", false)) {
                Toast.makeText(SmsMessageActivity.this, R.string.delete_fail,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            finish();
        }
    };


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

            final TextView textContent = (TextView) view.findViewById(R.id.text1);
            textContent.setText(sms.Content);

            final TextView textDate = (TextView) view.findViewById(R.id.text2);
            try {
                textDate.setText(DateFormat.getInstance().format(
                        new Date(Long.parseLong(sms.Date))));
            } catch (Exception ex) {
                textDate.setText(sms.Date);
            }

            return view;
        }
    }
}

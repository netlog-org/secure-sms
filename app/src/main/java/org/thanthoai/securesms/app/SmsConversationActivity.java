package org.thanthoai.securesms.app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
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

import com.melnykov.fab.FloatingActionButton;

import org.thanthoai.securesms.R;
import org.thanthoai.securesms.crypto.AESHelper;
import org.thanthoai.securesms.loaders.SmsConversationLoader;
import org.thanthoai.securesms.model.PassphraseModel;
import org.thanthoai.securesms.model.SmsConversation;
import org.thanthoai.securesms.utils.cache.CacheHelper;
import org.thanthoai.securesms.utils.Global;
import org.thanthoai.securesms.utils.Keys;

import java.util.List;


public class SmsConversationActivity extends BaseProtectedActivity
        implements LoaderManager.LoaderCallbacks<List<SmsConversation>>{

    private SmsListAdapter mAdapter;
    private ProgressBar pb;
    private View viewListContainer;

    private final CacheHelper mCache = CacheHelper.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_conversation);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_base);
        setSupportActionBar(toolbar);
        final ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle(R.string.app_name);
            ab.setLogo(R.drawable.action_logo);
        }

        pb = (ProgressBar) findViewById(R.id.progress);
        viewListContainer = findViewById(R.id.list_container);

        mAdapter = new SmsListAdapter(this, R.layout.view_list_sms_item_1);

        final ListView listView = (ListView) findViewById(R.id.list);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final SmsConversation conversation = mAdapter.getItem(position);

                final PassphraseModel model = PassphraseModel.findByAddress(
                        SmsConversationActivity.this, conversation.Address);
                final String passphrase = (model != null)
                        ? AESHelper.decryptFromBase64(getAppPassphrase(), model.Passphrase)
                        : Global.DEFAULT_PASSPHRASE;

                Intent i = new Intent(SmsConversationActivity.this, SmsMessageActivity.class);
                i.putExtra("address", conversation.Address);
                i.putExtra("addressInContact", conversation.AddressInContact);
                i.putExtra("passphrase", passphrase);
                i.putExtra(Keys.APP_PASSPHRASE, getAppPassphrase());
                startActivity(i);
            }
        });
        listView.setEmptyView(findViewById(R.id.text_list_empty));
        listView.setAdapter(mAdapter);

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setShadow(true);
        fab.show(true);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(SmsConversationActivity.this, ContactActivity.class);
                i.putExtra(Keys.APP_PASSPHRASE, getAppPassphrase());
                startActivity(i);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        setListViewVisible(false);
        mAdapter.clear();

        if (getSupportLoaderManager().getLoader(0) == null) {
            getSupportLoaderManager().initLoader(0, null, this);
        } else {
            getSupportLoaderManager().restartLoader(0, null, this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (Intent.ACTION_SHUTDOWN.equals(intent.getAction())) {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sms_conversation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_setting) {
            Intent i = new Intent(this, SettingActivity.class);
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<List<SmsConversation>> onCreateLoader(int id, Bundle args) {
        return new SmsConversationLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<List<SmsConversation>> loader,
                               List<SmsConversation> data) {
        for (SmsConversation object : data) {
            mAdapter.add(object);
        }
        setListViewVisible(true);
        mCache.put("sms", data);
    }

    @Override
    public void onLoaderReset(Loader<List<SmsConversation>> loader) {
        Global.log("Sms loader reset");
    }

    @SuppressLint("InflateParams")
    @SuppressWarnings("unused")
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
                        if (!Global.DEFAULT_PASSPHRASE.equals(input.getText().toString())) {
                            showInputPasswordDialog();
                        }
                    }
                }).show();
    }

    private void setListViewVisible(boolean isVisible) {
        if (isVisible) {
            pb.setVisibility(View.GONE);
            viewListContainer.setVisibility(View.VISIBLE);
        } else {
            pb.setVisibility(View.VISIBLE);
            viewListContainer.setVisibility(View.INVISIBLE);
        }
    }


    private static class SmsListAdapter extends ArrayAdapter<SmsConversation> {

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
            final SmsConversation conversation = getItem(position);
            final TextView textFrom = (TextView) view.findViewById(R.id.text1);
            final TextView textContent = (TextView) view.findViewById(R.id.text2);

            if (conversation.AddressInContact != null) {
                textFrom.setText(conversation.AddressInContact);
            } else {
                textFrom.setText(conversation.Address);
            }
            textContent.setText(conversation.Content);

            return view;
        }
    }
}

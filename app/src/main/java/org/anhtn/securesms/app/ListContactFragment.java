package org.anhtn.securesms.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.anhtn.securesms.R;
import org.anhtn.securesms.loaders.ContactLoader;
import org.anhtn.securesms.utils.CacheHelper;
import org.anhtn.securesms.model.ContactObject;
import org.anhtn.securesms.utils.Global;

import java.util.List;


public class ListContactFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<List<ContactObject>> {

    private ProgressBar pb;
    private ListContactAdapter mAdapter;
    private LoaderManager mLoaderManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new ListContactAdapter(getActivity(), R.layout.view_list_sms_item_1);
        setListAdapter(mAdapter);
        mLoaderManager = getActivity().getSupportLoaderManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v =  inflater.inflate(R.layout.fragment_list_contact, container, false);
        pb = (ProgressBar) v.findViewById(android.R.id.progress);
        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final ContactObject object = mAdapter.getItem(position);
                if (object.PhoneNumbers.size() > 1) {
                    final String[] numbers = new String[object.PhoneNumbers.keySet().size()];
                    int i = 0;
                    for (String s : object.PhoneNumbers.keySet()) {
                        numbers[i++] = s;
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            getActivity(), android.R.layout.simple_list_item_1,
                            numbers
                    );

                    new AlertDialog.Builder(getActivity())
                            .setTitle(getString(R.string.choose_phone_number, object.DisplayName))
                            .setCancelable(true)
                            .setNegativeButton(android.R.string.cancel, null)
                            .setAdapter(adapter, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    onContactSelected(numbers[which], object.DisplayName);
                                }
                            }).show();
                } else {
                    onContactSelected(object.PrimaryNumber, object.DisplayName);
                }
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (CacheHelper.getInstance().contains("contact")) {
            mAdapter.clear();
            List<ContactObject> contactObjects = (List<ContactObject>)
                    CacheHelper.getInstance().get("contact");
            for (ContactObject object : contactObjects) {
                mAdapter.add(object);
            }
            getListView().setVisibility(View.VISIBLE);
            pb.setVisibility(View.INVISIBLE);
        }
        mLoaderManager.initLoader(0, null, this);
    }

    @Override
    public Loader<List<ContactObject>> onCreateLoader(int id, Bundle args) {
        return new ContactLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<List<ContactObject>> loader, List<ContactObject> data) {
        if (mAdapter.isEmpty()) {
            getListView().setVisibility(View.VISIBLE);
            pb.setVisibility(View.INVISIBLE);
            CacheHelper.getInstance().put("contact", data);
        } else  {
            mAdapter.clear();
        }
        for (ContactObject object : data) {
            mAdapter.add(object);
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {
        Global.log("Contact loader reset");
    }

    private void onContactSelected(String phoneNumber, String contactName) {
        Intent i = new Intent(getActivity(), SmsContentActivity.class);
        i.putExtra("address", phoneNumber);
        i.putExtra("addressInContact", contactName);
        i.putExtra("content", getActivity().getIntent().getStringExtra("content"));
        startActivity(i);
    }


    private static class ListContactAdapter extends ArrayAdapter<ContactObject> {

        public ListContactAdapter(Context context, int resource) {
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

            final ContactObject object = getItem(position);
            final TextView txtName = (TextView) view.findViewById(android.R.id.text1);
            final TextView txtPhone = (TextView) view.findViewById(android.R.id.text2);
            txtName.setText(object.DisplayName);
            txtPhone.setText(object.PrimaryNumber + " "
                    + object.PhoneNumbers.get(object.PrimaryNumber));

            return view;
        }
    }
}

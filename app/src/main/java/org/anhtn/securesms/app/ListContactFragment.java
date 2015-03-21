package org.anhtn.securesms.app;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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

import java.util.ArrayList;
import java.util.List;


public class ListContactFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<List<ContactObject>>,
        SearchView.OnQueryTextListener {

    private ProgressBar pb;
    private View viewListContainer;
    private ListContactAdapter mAdapter;
    private List<ContactObject> mContactList = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new ListContactAdapter(getActivity(), R.layout.view_list_sms_item_1);
        setListAdapter(mAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v =  inflater.inflate(R.layout.fragment_list_contact, container, false);
        pb = (ProgressBar) v.findViewById(R.id.progress);
        viewListContainer = v.findViewById(R.id.list_container);
        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final ContactObject c = mAdapter.getItem(position);
                if (c.PhoneNumbers.size() > 1) {
                    final String[] numbers = new String[c.PhoneNumbers.keySet().size()];
                    int i = 0;
                    for (String s : c.PhoneNumbers.keySet()) {
                        numbers[i++] = s;
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            getActivity(), android.R.layout.simple_list_item_1,
                            numbers
                    );

                    new AlertDialog.Builder(getActivity())
                            .setTitle(getString(R.string.choose_phone_number, c.DisplayName))
                            .setCancelable(true)
                            .setNegativeButton(android.R.string.cancel, null)
                            .setAdapter(adapter, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    goToSendMessageActivity(numbers[which], c.DisplayName);
                                }
                            }).show();
                } else {
                    goToSendMessageActivity(c.PrimaryNumber, c.DisplayName);
                }
            }
        });
        getListView().setEmptyView(view.findViewById(R.id.text_list_empty));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        if (CacheHelper.getInstance().contains("contact")) {
            mAdapter.clear();
            List<ContactObject> contactObjects = (List<ContactObject>)
                    CacheHelper.getInstance().get("contact");
            for (ContactObject object : contactObjects) {
                mAdapter.add(object);
            }
            setListViewVisible();
        }
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_list_contact, menu);

        final SearchManager searchManager = (SearchManager)
                getActivity().getSystemService(Context.SEARCH_SERVICE);
        final MenuItem item = menu.findItem(R.id.action_search);

        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(
                getActivity().getComponentName()));
        searchView.setIconifiedByDefault(false);
        searchView.setOnQueryTextListener(this);
    }

    @Override
    public Loader<List<ContactObject>> onCreateLoader(int id, Bundle args) {
        return new ContactLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<List<ContactObject>> loader, List<ContactObject> data) {
        if (mAdapter.isEmpty()) {
            setListViewVisible();
            CacheHelper.getInstance().put("contact", data);
        } else  {
            mAdapter.clear();
        }
        for (ContactObject object : data) {
            mAdapter.add(object);
        }
        mContactList = data;
    }

    @Override
    public void onLoaderReset(Loader loader) {
        Global.log("Contact loader reset");
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return true;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        mAdapter.clear();
        if (s.length() == 0) {
            for (ContactObject contact : mContactList) {
                mAdapter.add(contact);
            }
            return false;
        }
        try {
            String keyword = String.valueOf(Long.parseLong(s));
            for (ContactObject contact : mContactList) {
                for (String number : contact.PhoneNumbers.keySet()) {
                    List<Integer> matchedPos = new ArrayList<>();
                    if (Global.smartContains(number, keyword, matchedPos)) {
                        if (number.equals(contact.PrimaryNumber)) {
                            Integer[] arr = new Integer[matchedPos.size()];
                            ContactObject clone = new ContactObject(contact);
                            clone.SpannablePrimaryNumber = getSpannableWithBoldInSomeParts(
                                    number, matchedPos.toArray(arr));
                            mAdapter.add(clone);
                        } else {
                            mAdapter.add(contact);
                        }
                        break;
                    }
                }
            }
            if (mAdapter.isEmpty()) {
                final String text = getResources().getString(R.string.new_sms_to, s);
                final int index = text.indexOf(s);
                final SpannableStringBuilder builder = new SpannableStringBuilder(text);
                applySpannableBlackColorWithBold(builder, index, index + s.length());

                ContactObject contact = new ContactObject();
                contact.SpannablePrimaryNumber = builder;
                contact.PrimaryNumber = s;
                mAdapter.add(contact);
            }
        } catch (NumberFormatException ex) {
            for (ContactObject contact : mContactList) {
                if (Global.smartContains(contact.DisplayName.toLowerCase(),
                        s.toLowerCase(), null)) {
                    mAdapter.add(contact);
                }
            }
        }
        return false;
    }

    private void setListViewVisible() {
        viewListContainer.setVisibility(View.VISIBLE);
        pb.setVisibility(View.INVISIBLE);
    }

    private void goToSendMessageActivity(String phoneNumber, String contactName) {
        Intent i = new Intent(getActivity(), SmsContentActivity.class);
        i.putExtra("address", phoneNumber);
        i.putExtra("addressInContact", contactName);
        i.putExtra("content", getActivity().getIntent().getStringExtra("content"));
        startActivity(i);
    }

    private CharSequence getSpannableWithBoldInSomeParts(String text, Integer[] boldPos) {
        final SpannableStringBuilder builder = new SpannableStringBuilder(text);
        if (boldPos.length == 1) {
            applySpannableBlackColorWithBold(builder, boldPos[0], boldPos[0] + 1);
        } else {
            int i = 0;
            for (int j = 1; j < boldPos.length; j++) {
                if (boldPos[j] - boldPos[j-1] == 1) {
                    continue;
                }
                applySpannableBlackColorWithBold(builder, boldPos[i], boldPos[j-1] + 1);
                i = j;
            }
            applySpannableBlackColorWithBold(builder, boldPos[i],
                    boldPos[boldPos.length-1] + 1);
        }
        return builder;
    }

    private void applySpannableBlackColorWithBold(SpannableStringBuilder builder,
                                                  int start, int end) {
        builder.setSpan(new StyleSpan(Typeface.BOLD), start, end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new ForegroundColorSpan(Color.BLACK), start, end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }


    private static class ListContactAdapter extends ArrayAdapter<ContactObject> {

        private static final int TYPE_NORMAL = 1;
        private static final int TYPE_EMPTY_VIEW = 2;

        public ListContactAdapter(Context context, int resource) {
            super(context, resource);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            final ContactObject object = getItem(position);

            if (getItemViewType(position) == TYPE_EMPTY_VIEW) {
                if (view == null) {
                    LayoutInflater inflater = (LayoutInflater)
                            getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = inflater.inflate(android.R.layout.simple_list_item_1,
                            parent, false);
                }
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                textView.setText(object.SpannablePrimaryNumber);
            } else {
                if (view == null) {
                    LayoutInflater inflater = (LayoutInflater)
                            getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = inflater.inflate(R.layout.view_list_sms_item_1, parent, false);
                }

                final TextView txtName = (TextView) view.findViewById(R.id.text1);
                final TextView txtPhone = (TextView) view.findViewById(R.id.text2);
                txtName.setText(object.DisplayName);

                String type = object.PhoneNumbers.get(object.PrimaryNumber);
                if (type == null) type = "";
                if (object.SpannablePrimaryNumber != null) {
                    SpannableStringBuilder builder = (SpannableStringBuilder)
                            object.SpannablePrimaryNumber;
                    builder.append(" ");
                    builder.append(type);
                    txtPhone.setText(builder);
                } else {
                    txtPhone.setText(object.PrimaryNumber + " " + type);
                }
            }

            return view;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            final ContactObject c = getItem(position);
            if (c.DisplayName == null)
                return TYPE_EMPTY_VIEW;
            else
                return TYPE_NORMAL;
        }
    }
}

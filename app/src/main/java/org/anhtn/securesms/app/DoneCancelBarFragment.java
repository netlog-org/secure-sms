package org.anhtn.securesms.app;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import org.anhtn.securesms.R;

public class DoneCancelBarFragment extends Fragment {

    public static final int MODE_NORMAL = 0;
    public static final int MODE_NAVIGATION = 1;

    private ArrayAdapter<String> mAdapter;
    private Spinner spinner;
    private TextView textView;

    @Override
    @SuppressWarnings("unchecked")
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_done_cancel, container, false);
        spinner = (Spinner) v.findViewById(R.id.spinner);
        textView = (TextView) v.findViewById(R.id.text);
        mAdapter = new ArrayAdapter<>(getActivity(), R.layout.view_spinner_item);
        mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(mAdapter);

        return v;
    }

    public void setMode(int mode) {
        switch (mode) {
            case MODE_NORMAL:
                textView.setVisibility(View.VISIBLE);
                spinner.setVisibility(View.GONE);
                break;

            case MODE_NAVIGATION:
                textView.setVisibility(View.GONE);
                spinner.setVisibility(View.VISIBLE);
                break;

            default: throw new IllegalArgumentException();
        }
    }

    public void setDropdownItems(String[] items) {
        mAdapter.clear();
        for (String item : items) {
            mAdapter.add(item);
        }
    }

    public void setCancelIcon(int resId) {
        final View view = getView();
        if (view != null) {
            ImageButton imgButton = (ImageButton) view.findViewById(R.id.button_cancel);
            imgButton.setImageResource(resId);
        }
    }
}

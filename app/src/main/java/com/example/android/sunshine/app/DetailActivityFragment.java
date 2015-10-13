package com.example.android.sunshine.app;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;


/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment {

    public DetailActivityFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.fragment_detail, container, false);

        Intent intent = getActivity().getIntent();
        String message = intent.getStringExtra(DetailActivity.WEATHERDATA);
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
        TextView msgTextView = (TextView) rootView.findViewById(R.id.msgTextView);
        msgTextView.setText(message);

        return rootView;
    }
}

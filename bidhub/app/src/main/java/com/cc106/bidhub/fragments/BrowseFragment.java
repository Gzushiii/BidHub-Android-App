package com.cc106.bidhub.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.cc106.bidhub.toast.ToastHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cc106.bidhub.R;

public class BrowseFragment extends Fragment {

    private String loggedInUserEmail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_browse, container, false);
        
        // Get the logged-in user's email from arguments
        if (getArguments() != null) {
            loggedInUserEmail = getArguments().getString("USER_EMAIL");
        }
        
        // TODO: Implement browse functionality
        ToastHelper.showInfo(getContext(), "Browse Items - Coming Soon!");
        
        return view;
    }
    
    public void updateUserEmail(String email) {
        this.loggedInUserEmail = email;
    }
}

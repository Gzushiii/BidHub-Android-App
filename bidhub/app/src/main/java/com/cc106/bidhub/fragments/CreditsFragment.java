package com.cc106.bidhub.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cc106.bidhub.R;

public class CreditsFragment extends Fragment {

    private String loggedInUserEmail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_credits, container, false);
        
        // Get the logged-in user's email from arguments
        if (getArguments() != null) {
            loggedInUserEmail = getArguments().getString("USER_EMAIL");
        }
        
        // TODO: Implement credits functionality
        Toast.makeText(getContext(), "Credits Management - Coming Soon!", Toast.LENGTH_SHORT).show();
        
        return view;
    }
    
    public void updateUserEmail(String email) {
        this.loggedInUserEmail = email;
    }
}

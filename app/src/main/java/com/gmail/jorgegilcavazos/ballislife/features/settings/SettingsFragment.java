package com.gmail.jorgegilcavazos.ballislife.features.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;

import com.gmail.jorgegilcavazos.ballislife.features.login.LoginActivity;
import com.gmail.jorgegilcavazos.ballislife.network.RedditAuthentication;
import com.gmail.jorgegilcavazos.ballislife.util.TeamName;
import com.gmail.jorgegilcavazos.ballislife.R;

public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener{
    private static final String TAG = "SettingsFragment";

    private final RedditAuthentication.DeAuthTask.OnDeAuthTaskCompleted deAuthListener =
            new RedditAuthentication.DeAuthTask.OnDeAuthTaskCompleted() {
        @Override
        public void onSuccess() {
            RedditAuthentication.getInstance().clearRefreshTokenInPrefs(getActivity());
            initLogInStatusText();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_general);

        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            pickPreferenceObject(getPreferenceScreen().getPreference(i));
        }

        initListeners();
    }

    private void pickPreferenceObject(Preference preference) {
        if (preference instanceof PreferenceCategory) {
            PreferenceCategory category = (PreferenceCategory) preference;
            for (int i = 0; i < category.getPreferenceCount(); i++) {
                pickPreferenceObject(category.getPreference(i));
            }
        } else {
            initSummary(preference);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference preference = findPreference(key);
        switch (key) {
            case "teams_list":
                String abbrev = sharedPreferences.getString(key, null);
                preference.setSummary(getTeamName(abbrev));
                break;
            case "log_out_pref":
                preference.setTitle("Log in");
                break;
        }
    }

    private String getTeamName(String abbreviation) {
        if (abbreviation != null) {
            if (abbreviation.equals("noteam")) {
                return "No team selected";
            }

            abbreviation = abbreviation.toUpperCase();
            for (TeamName teamName : TeamName.values()) {
                if (teamName.toString().equals(abbreviation)) {
                    return teamName.getTeamName();
                }
            }
        }
        return "No team selected";
    }

    private void initSummary(Preference preference) {
        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            preference.setSummary(getTeamName(listPreference.getValue()));
        }
    }

    private void initLogInStatusText() {
        Preference logInStatusPref = findPreference("log_in_status_pref");
        RedditAuthentication reddit = RedditAuthentication.getInstance();
        if (reddit.isUserLoggedIn()) {
            logInStatusPref.setTitle(R.string.log_out);
            logInStatusPref.setSummary(String.format(getString(R.string.logged_as_user),
                    reddit.getRedditClient().getAuthenticatedUser()));

        } else {
            logInStatusPref.setTitle(R.string.log_in);
            logInStatusPref.setSummary(R.string.click_login);
        }
    }

    private void initListeners() {
        Preference logInStatusPref = findPreference("log_in_status_pref");
        logInStatusPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                RedditAuthentication reddit = RedditAuthentication.getInstance();
                if (reddit.isUserLoggedIn()) {
                    reddit.deAuthenticateUser(getActivity(), deAuthListener);
                } else {
                    Intent loginIntent = new Intent(getActivity(), LoginActivity.class);
                    startActivity(loginIntent);
                }
                return false;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);

        initLogInStatusText();
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }
}
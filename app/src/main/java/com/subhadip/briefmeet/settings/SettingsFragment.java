package com.subhadip.briefmeet.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.subhadip.briefmeet.BuildConfig;
import com.subhadip.briefmeet.MainActivity;
import com.subhadip.briefmeet.R;
import com.subhadip.briefmeet.WebActivity;
import com.subhadip.briefmeet.databinding.FragmentSettingsBinding;
import com.subhadip.briefmeet.profile.ProfileActivity;
import com.subhadip.briefmeet.utils.AppConstants;
import com.subhadip.briefmeet.utils.SharedObjects;


public class SettingsFragment extends Fragment implements View.OnClickListener {

    SharedObjects sharedObjects;

    FragmentSettingsBinding binding;

    public SettingsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container,false);
        sharedObjects = new SharedObjects(getActivity());

        binding.llProfile.setOnClickListener(this);
        binding.llLogout.setOnClickListener(this);
        binding.llRateUs.setOnClickListener(this);
        binding.llShare.setOnClickListener(this);
        binding.llHelpTuts.setOnClickListener(this);
        binding.llPrivacyPolicy.setOnClickListener(this);
        binding.llTermsAndCondition.setOnClickListener(this);
        return binding.getRoot();
    }


    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
            case R.id.llProfile:
                startActivity(new Intent(getActivity(), ProfileActivity.class));
                break;
            case R.id.llLogout:
                ((MainActivity) getActivity()).removeAllPreferenceOnLogout();
                break;
            case R.id.llRateUs:
                if (SharedObjects.isNetworkConnected(getActivity())) {
                    final String appPackageName = getActivity().getPackageName(); // getPackageName() from Context or Activity object
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                    } catch (android.content.ActivityNotFoundException anfe) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                    }
                } else {
                    AppConstants.showAlertDialog(getString(R.string.err_internet),getActivity());
                }
                break;
            case R.id.llShare:
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT,
                        getString(R.string.share_msg) + BuildConfig.APPLICATION_ID);
                sendIntent.setType("text/plain");
                startActivity(sendIntent);
                break;
            case R.id.llTermsAndCondition:
                Intent termIntent = new Intent(getActivity(), WebActivity.class);
                termIntent.putExtra("type", AppConstants.TERMS_PAGE);
                startActivity(termIntent);
                break;
            case R.id.llPrivacyPolicy:
                Intent policyIntent = new Intent(getActivity(), WebActivity.class);
                policyIntent.putExtra("type", AppConstants.PRIVACY_PAGE);
                startActivity(policyIntent);
                break;
            case R.id.llHelpTuts:
                Intent helpIntent = new Intent(getActivity(), WebActivity.class);
                helpIntent.putExtra("type", AppConstants.HELP_PAGE);
                startActivity(helpIntent);
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case android.R.id.home: {
                ((MainActivity) getActivity()).onBackPressed();
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}

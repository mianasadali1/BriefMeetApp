package com.subhadip.briefmeet;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.subhadip.briefmeet.databinding.ActivityWebBinding;
import com.subhadip.briefmeet.utils.AppConstants;

public class WebActivity extends AppCompatActivity {

    ActivityWebBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWebBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding.webview.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        int type = getIntent().getIntExtra("type", 0);

        switch (type) {
            case AppConstants.TERMS_PAGE:
                binding.webview.loadUrl(getResources().getString(R.string.term_url));
                getSupportActionBar().setTitle(R.string.terms_condition);
                break;
            case AppConstants.PRIVACY_PAGE:
                binding.webview.loadUrl(getResources().getString(R.string.privacy_url));
                getSupportActionBar().setTitle(R.string.privacy_policy);
                break;
            case AppConstants.HELP_PAGE:
                binding.webview.loadUrl(getResources().getString(R.string.help_url));
                getSupportActionBar().setTitle(R.string.help_tutorial);
                break;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}
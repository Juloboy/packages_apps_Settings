/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.wifi.dpp;

import android.app.ActionBar;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.settings.R;
import com.android.settings.wifi.qrcode.QrCodeGenerator;

import com.google.zxing.WriterException;

/**
 * After sharing a saved Wi-Fi network, {@code WifiDppConfiguratorActivity} start with this fragment
 * to generate a Wi-Fi DPP QR code for other device to initiate as an enrollee.
 */
public class WifiDppQrCodeGeneratorFragment extends WifiDppQrCodeBaseFragment {
    private static final String TAG = "WifiDppQrCodeGeneratorFragment";

    private ImageView mQrCodeView;
    private String mQrCode;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_WIFI_DPP_CONFIGURATOR;
    }

    // Container Activity must implement this interface
    public interface OnQrCodeGeneratorFragmentAddButtonClickedListener {
        public void onQrCodeGeneratorFragmentAddButtonClicked();
    }
    OnQrCodeGeneratorFragmentAddButtonClickedListener mListener;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);
        final ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.show();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mListener = (OnQrCodeGeneratorFragmentAddButtonClickedListener) context;
    }

    @Override
    public void onDetach() {
        mListener = null;

        super.onDetach();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        final WifiNetworkConfig wifiNetworkConfig = getWifiNetworkConfigFromHostActivity();
        MenuItem menuItem;
        if (wifiNetworkConfig.isSupportWifiDpp(getActivity())) {
            menuItem = menu.add(0, Menu.FIRST, 0, R.string.next_label);
            menuItem.setIcon(R.drawable.ic_scan_24dp);
            menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        } else {
            menuItem = menu.findItem(Menu.FIRST);
            if (menuItem != null) {
                menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            }
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case Menu.FIRST:
                mListener.onQrCodeGeneratorFragmentAddButtonClicked();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override
    public final View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.wifi_dpp_qrcode_generator_fragment, container,
                /* attachToRoot */ false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mQrCodeView = view.findViewById(R.id.qrcode_view);

        setHeaderIconImageResource(R.drawable.ic_qrcode_24dp);

        final WifiNetworkConfig wifiNetworkConfig = getWifiNetworkConfigFromHostActivity();
        mTitle.setText(R.string.wifi_dpp_share_wifi);
        mSummary.setText(getString(R.string.wifi_dpp_scan_qr_code_with_another_device,
                wifiNetworkConfig.getSsid()));

        mQrCode = wifiNetworkConfig.getQrCode();
        setQrCode();
    }

    private void setQrCode() {
        try {
            final int qrcodeSize = getContext().getResources().getDimensionPixelSize(
                    R.dimen.qrcode_size);
            final Bitmap bmp = QrCodeGenerator.encodeQrCode(mQrCode, qrcodeSize);
            mQrCodeView.setImageBitmap(bmp);
        } catch (WriterException e) {
            Log.e(TAG, "Error generatting QR code bitmap " + e);
        }
    }

    WifiNetworkConfig getWifiNetworkConfigFromHostActivity() {
        final WifiNetworkConfig wifiNetworkConfig = ((WifiNetworkConfig.Retriever) getActivity())
                .getWifiNetworkConfig();
        if (!WifiNetworkConfig.isValidConfig(wifiNetworkConfig)) {
            throw new IllegalStateException("Invalid Wi-Fi network for configuring");
        }

        return wifiNetworkConfig;
    }
}
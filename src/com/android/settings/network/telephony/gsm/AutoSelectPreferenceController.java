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

package com.android.settings.network.telephony.gsm;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settings.network.telephony.NetworkSelectSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * Preference controller for "Auto Select Network"
 */
public class AutoSelectPreferenceController extends TogglePreferenceController {

    private int mSubId;
    private TelephonyManager mTelephonyManager;
    private boolean mOnlyAutoSelectInHome;
    private List<OnNetworkSelectModeListener> mListeners;

    public AutoSelectPreferenceController(Context context, String key) {
        super(context, key);
        mTelephonyManager = context.getSystemService(TelephonyManager.class);
        mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        mListeners = new ArrayList<>();
    }

    @Override
    public int getAvailabilityStatus() {
        return MobileNetworkUtils.shouldDisplayNetworkSelectOptions(mContext,mSubId)
                ? AVAILABLE
                : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return mTelephonyManager.getNetworkSelectionMode()
                == TelephonyManager.NETWORK_SELECTION_MODE_AUTO;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        preference.setSummary(null);
        if (mTelephonyManager.getServiceState().getRoaming()) {
            preference.setEnabled(true);
        } else {
            preference.setEnabled(!mOnlyAutoSelectInHome);
            if (mOnlyAutoSelectInHome) {
                preference.setSummary(mContext.getString(
                        R.string.manual_mode_disallowed_summary,
                        mTelephonyManager.getSimOperatorName()));
            }
        }
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (isChecked) {
            mTelephonyManager.setNetworkSelectionModeAutomatic();

            for (OnNetworkSelectModeListener lsn : mListeners) {
                lsn.onNetworkSelectModeChanged();
            }
            // Manually check whether it is successfully
            return mTelephonyManager.getNetworkSelectionMode()
                    == TelephonyManager.NETWORK_SELECTION_MODE_AUTO;
        } else {
            final Bundle bundle = new Bundle();
            bundle.putInt(Settings.EXTRA_SUB_ID, mSubId);
            new SubSettingLauncher(mContext)
                    .setDestination(NetworkSelectSettings.class.getName())
                    .setSourceMetricsCategory(SettingsEnums.MOBILE_NETWORK_SELECT)
                    .setTitleRes(R.string.choose_network_title)
                    .setArguments(bundle)
                    .launch();
            return false;
        }
    }

    public AutoSelectPreferenceController init(int subId) {
        mSubId = subId;
        mTelephonyManager = TelephonyManager.from(mContext).createForSubscriptionId(mSubId);
        final PersistableBundle carrierConfig = mContext.getSystemService(
                CarrierConfigManager.class).getConfigForSubId(mSubId);
        mOnlyAutoSelectInHome = carrierConfig.getBoolean(
                CarrierConfigManager.KEY_ONLY_AUTO_SELECT_IN_HOME_NETWORK_BOOL);

        return this;
    }

    public AutoSelectPreferenceController addListener(OnNetworkSelectModeListener lsn) {
        mListeners.add(lsn);

        return this;
    }

    /**
     * Callback when network select mode is changed
     *
     * @see TelephonyManager#getNetworkSelectionMode()
     */
    public interface OnNetworkSelectModeListener {
        void onNetworkSelectModeChanged();
    }
}
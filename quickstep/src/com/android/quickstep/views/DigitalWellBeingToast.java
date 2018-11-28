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

package com.android.quickstep.views;

import android.app.ActivityOptions;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.userevent.nano.LauncherLogProto;
import com.android.systemui.shared.recents.model.Task;

public final class DigitalWellBeingToast extends LinearLayout {
    public interface InitializeCallback {
        void call(long t, boolean b);
    }

    private static final String TAG = DigitalWellBeingToast.class.getSimpleName();

    private Task mTask;

    public DigitalWellBeingToast(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutDirection(Utilities.isRtl(getResources()) ?
                View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        setOnClickListener((view) -> openAppUsageSettings());

    }

    private void setRemainingTime(long remainingTime, boolean isGroupLimit) {
        final TextView remainingTimeText = findViewById(R.id.remaining_time);
        if (remainingTime <= 0) {
            setVisibility(GONE);
        } else {
            setVisibility(VISIBLE);
            remainingTimeText.setText(getText(remainingTime, isGroupLimit));
        }
    }

    public void initialize(Task task, InitializeCallback callback) {
        mTask = task;
        Utilities.THREAD_POOL_EXECUTOR.execute(() -> {
            final long appRemainingTimeMs = -1;
            final boolean isGroupLimit = true;
            post(() -> {
                setRemainingTime(appRemainingTimeMs, isGroupLimit);
                callback.call(appRemainingTimeMs, isGroupLimit);
            });
        });
    }

    public static String getText(long remainingTime, boolean isGroupLimit) {
        return "Remaining time:" + (remainingTime + 119999) / 120000
                + " min " + (isGroupLimit ? "for group" : "for the app");
    }

    public void openAppUsageSettings() {
        final Intent intent = new Intent(TaskView.SEE_TIME_IN_APP_TEMPLATE)
                .putExtra(Intent.EXTRA_PACKAGE_NAME,
                        mTask.getTopComponent().getPackageName()).addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        try {
            final Launcher launcher = Launcher.getLauncher(getContext());
            final ActivityOptions options = ActivityOptions.makeScaleUpAnimation(
                    this, 0, 0,
                    getWidth(), getHeight());
            launcher.startActivity(intent, options.toBundle());
            launcher.getUserEventDispatcher().logActionOnControl(LauncherLogProto.Action.Touch.TAP,
                    LauncherLogProto.ControlType.APP_USAGE_SETTINGS, this);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Failed to open app usage settings for task "
                    + mTask.getTopComponent().getPackageName(), e);
        }
    }
}

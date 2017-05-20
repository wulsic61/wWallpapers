package com.dm.wallpaper.board.fragments.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.danimahardhika.android.helpers.core.ColorHelper;
import com.danimahardhika.android.helpers.core.DrawableHelper;
import com.danimahardhika.android.helpers.permission.PermissionHelper;
import com.danimahardhika.cafebar.CafeBar;
import com.danimahardhika.cafebar.CafeBarTheme;
import com.dm.wallpaper.board.R;
import com.dm.wallpaper.board.R2;
import com.dm.wallpaper.board.helpers.WallpaperHelper;
import com.dm.wallpaper.board.utils.Extras;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;

/*
 * Wallpaper Board
 *
 * Copyright (c) 2017 Dani Mahardhika
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

public class WallpaperOptionsFragment extends DialogFragment implements View.OnClickListener {

    @BindView(R2.id.apply)
    LinearLayout mApply;
    @BindView(R2.id.save)
    TextView mSave;
    @BindView(R2.id.apply_text)
    TextView mApplyText;
    @BindView(R2.id.divider)
    View mDivider;

    private String mName;
    private String mUrl;

    private static final String TAG = "com.dm.wallpaper.board.dialog.wallpaper.options";

    private static WallpaperOptionsFragment newInstance(String url, String name) {
        WallpaperOptionsFragment fragment = new WallpaperOptionsFragment();
        Bundle bundle = new Bundle();
        bundle.putString(Extras.EXTRA_URL, url);
        bundle.putString(Extras.EXTRA_NAME, name);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static void showWallpaperOptionsDialog(FragmentManager fm, String url, String name) {
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag(TAG);
        if (prev != null) {
            ft.remove(prev);
        }

        try {
            DialogFragment dialog = WallpaperOptionsFragment.newInstance(url, name);
            dialog.show(ft, TAG);
        } catch (IllegalArgumentException | IllegalStateException ignored) {}
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
        builder.customView(R.layout.fragment_wallpaper_options, false);
        builder.typeface("Font-Medium.ttf", "Font-Regular.ttf");
        MaterialDialog dialog = builder.build();
        dialog.show();

        ButterKnife.bind(this, dialog);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mName = getArguments().getString(Extras.EXTRA_NAME);
        mUrl = getArguments().getString(Extras.EXTRA_URL);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mUrl = savedInstanceState.getString(Extras.EXTRA_URL);
            mName = savedInstanceState.getString(Extras.EXTRA_NAME);
        }

        int color = ColorHelper.getAttributeColor(getActivity(), android.R.attr.textColorPrimary);
        mApplyText.setCompoundDrawablesWithIntrinsicBounds(DrawableHelper.getTintedDrawable(
                getActivity(), R.drawable.ic_toolbar_apply, color), null, null, null);
        mSave.setCompoundDrawablesWithIntrinsicBounds(DrawableHelper.getTintedDrawable(
                getActivity(), R.drawable.ic_toolbar_download, color), null, null, null);

        mApply.setOnClickListener(this);
        if (getActivity().getResources().getBoolean(R.bool.enable_wallpaper_download)) {
            mSave.setOnClickListener(this);
            return;
        }

        mDivider.setVisibility(View.GONE);
        mSave.setVisibility(View.GONE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(Extras.EXTRA_URL, mUrl);
        outState.putString(Extras.EXTRA_NAME, mName);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        int color = ColorHelper.getAttributeColor(getActivity(), R.attr.colorAccent);
        if (id == R.id.apply) {
            WallpaperHelper.applyWallpaper(getActivity(), null, color, mUrl, mName);
        } else if (id == R.id.save) {
            if (PermissionHelper.isStorageGranted(getActivity())) {
                File target = new File(WallpaperHelper.getDefaultWallpapersDirectory(getActivity()).toString()
                        + File.separator + mName + WallpaperHelper.IMAGE_EXTENSION);

                if (target.exists()) {
                    Context context = getActivity();
                    CafeBar.builder(getActivity())
                            .theme(new CafeBarTheme.Custom(ColorHelper.getAttributeColor(getActivity(), R.attr.card_background)))
                            .autoDismiss(false)
                            .fitSystemWindow()
                            .maxLines(4)
                            .typeface("Font-Regular.ttf", "Font-Bold.ttf")
                            .content(String.format(getResources().getString(R.string.wallpaper_download_exist),
                                    ("\"" +mName + WallpaperHelper.IMAGE_EXTENSION+ "\"")))
                            .icon(R.drawable.ic_toolbar_download)
                            .positiveText(R.string.wallpaper_download_exist_replace)
                            .positiveColor(color)
                            .onPositive(cafeBar -> {
                                if (context == null) {
                                    cafeBar.dismiss();
                                    return;
                                }

                                WallpaperHelper.downloadWallpaper(context, color, mUrl, mName);
                                cafeBar.dismiss();
                            })
                            .negativeText(R.string.wallpaper_download_exist_new)
                            .onNegative(cafeBar -> {
                                if (context == null) {
                                    cafeBar.dismiss();
                                    return;
                                }

                                WallpaperHelper.downloadWallpaper(context, color, mUrl, mName +"_"+ System.currentTimeMillis());
                                cafeBar.dismiss();
                            })
                            .show();
                    dismiss();
                    return;
                }

                WallpaperHelper.downloadWallpaper(getActivity(), color, mUrl, mName);
                dismiss();
                return;
            }
            PermissionHelper.requestStorage(getActivity());
        }
        dismiss();
    }
}

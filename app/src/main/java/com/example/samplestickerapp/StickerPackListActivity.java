/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.example.samplestickerapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class StickerPackListActivity extends AddStickerPackActivity {
    public static final String EXTRA_STICKER_PACK_LIST_DATA = "sticker_pack_list";
    private static final int STICKER_PREVIEW_DISPLAY_LIMIT = 5;
    private LinearLayoutManager packLayoutManager;
    private RecyclerView packRecyclerView;
    private StickerPackListAdapter allStickerPacksListAdapter;
    private WhiteListCheckAsyncTask whiteListCheckAsyncTask;
    private ArrayList<StickerPack> stickerPackList;

    private InterstitialAd plusButtonAd;
    private InterstitialAd backPressAd;

    private DatabaseReference mDatabase;
    private String version;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sticker_pack_list);
        packRecyclerView = findViewById(R.id.sticker_pack_list);
        stickerPackList = getIntent().getParcelableArrayListExtra(EXTRA_STICKER_PACK_LIST_DATA);
        showStickerPackList(stickerPackList);


        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
            checkUpdate(version,false);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        //Initialization of Mobile ads
        //TODO: Change AdUnit Id and AppId
        MobileAds.initialize(this, "ca-app-pub-9415187538460886~1114124599");

        //Banner ad
        AdView mAdView = findViewById(R.id.adView);
        //add test device for testing
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        plusButtonAd = new InterstitialAd(this);
        plusButtonAd.setAdUnitId("ca-app-pub-9415187538460886/5073373295");
        plusButtonAd.loadAd(new AdRequest.Builder().build());
        backPressAd = new InterstitialAd(this);
        backPressAd.setAdUnitId("ca-app-pub-9415187538460886/7512011989");
        backPressAd.loadAd(new AdRequest.Builder().build());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.app_update:
                checkUpdate(version,true);
                return true;

            case R.id.visit_website:
                Uri uri = Uri.parse("http://skycomp.me/stickersapp/");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                return true;

            case android.R.id.home:
                // Navigate back to parent activity (CatalogActivity)
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        backPressAd.show();
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        whiteListCheckAsyncTask = new WhiteListCheckAsyncTask(this);
        whiteListCheckAsyncTask.execute(stickerPackList.toArray(new StickerPack[stickerPackList.size()]));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (whiteListCheckAsyncTask != null && !whiteListCheckAsyncTask.isCancelled()) {
            whiteListCheckAsyncTask.cancel(true);
        }
    }

    private void showStickerPackList(List<StickerPack> stickerPackList) {
        allStickerPacksListAdapter = new StickerPackListAdapter(stickerPackList, onAddButtonClickedListener);
        packRecyclerView.setAdapter(allStickerPacksListAdapter);
        packLayoutManager = new LinearLayoutManager(this);
        packLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                packRecyclerView.getContext(),
                packLayoutManager.getOrientation()
        );
        packRecyclerView.addItemDecoration(dividerItemDecoration);
        packRecyclerView.setLayoutManager(packLayoutManager);
        packRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(this::recalculateColumnCount);
    }

    public void checkUpdate(String verName, boolean status)  {
        mDatabase = FirebaseDatabase.getInstance().getReference("Version/NewVersion");

        double currentVersion = Double.valueOf(verName);

        mDatabase.addValueEventListener(new ValueEventListener(){

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Double value = dataSnapshot.getValue(Double.class);
                Log.e("Firebase Version:","Value: " + value);
                if (currentVersion != value) {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(StickerPackListActivity.this);
                    alertDialogBuilder.setTitle("Update").setMessage("Your app is out of date").setPositiveButton("Update", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Uri uri = Uri.parse("http://skycomp.me/stickersapp/");
                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                            startActivity(intent);
                        }
                    }).setNegativeButton("Later", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).show();
                } else if(status){
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(StickerPackListActivity.this);
                    alertDialogBuilder.setTitle("Update").setMessage("Your app is updated with latest Version.").setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }


    private final StickerPackListAdapter.OnAddButtonClickedListener onAddButtonClickedListener = pack -> {
        //TODO:Plus Button Adunit change
        if (plusButtonAd.isLoaded()) {
            plusButtonAd.show();
            //Toast.makeText(this, "Ad Show", Toast.LENGTH_SHORT).show();
            plusButtonAd = new InterstitialAd(this);
            plusButtonAd.setAdUnitId("ca-app-pub-9415187538460886/5073373295");
            plusButtonAd.loadAd(new AdRequest.Builder().build());
            addStickerPackToWhatsApp(pack.identifier, pack.name);
        } else {
            //Toast.makeText(this, "Ad not Loaded", Toast.LENGTH_SHORT).show();
            plusButtonAd = new InterstitialAd(this);
            plusButtonAd.setAdUnitId("ca-app-pub-9415187538460886/5073373295");
            plusButtonAd.loadAd(new AdRequest.Builder().build());
            addStickerPackToWhatsApp(pack.identifier, pack.name);

        }
    };

    private void recalculateColumnCount() {
        final int previewSize = getResources().getDimensionPixelSize(R.dimen.sticker_pack_list_item_preview_image_size);
        int firstVisibleItemPosition = packLayoutManager.findFirstVisibleItemPosition();
        StickerPackListItemViewHolder viewHolder = (StickerPackListItemViewHolder) packRecyclerView.findViewHolderForAdapterPosition(firstVisibleItemPosition);
        if (viewHolder != null) {
            final int max = Math.max(viewHolder.imageRowView.getMeasuredWidth() / previewSize, 1);
            int numColumns = Math.min(STICKER_PREVIEW_DISPLAY_LIMIT, max);
            allStickerPacksListAdapter.setMaxNumberOfStickersInARow(numColumns);
        }
    }


    static class WhiteListCheckAsyncTask extends AsyncTask<StickerPack, Void, List<StickerPack>> {
        private final WeakReference<StickerPackListActivity> stickerPackListActivityWeakReference;

        WhiteListCheckAsyncTask(StickerPackListActivity stickerPackListActivity) {
            this.stickerPackListActivityWeakReference = new WeakReference<>(stickerPackListActivity);
        }

        @Override
        protected final List<StickerPack> doInBackground(StickerPack... stickerPackArray) {
            final StickerPackListActivity stickerPackListActivity = stickerPackListActivityWeakReference.get();
            if (stickerPackListActivity == null) {
                return Arrays.asList(stickerPackArray);
            }
            for (StickerPack stickerPack : stickerPackArray) {
                stickerPack.setIsWhitelisted(WhitelistCheck.isWhitelisted(stickerPackListActivity, stickerPack.identifier));
            }
            return Arrays.asList(stickerPackArray);
        }

        @Override
        protected void onPostExecute(List<StickerPack> stickerPackList) {
            final StickerPackListActivity stickerPackListActivity = stickerPackListActivityWeakReference.get();
            if (stickerPackListActivity != null) {
                stickerPackListActivity.allStickerPacksListAdapter.setStickerPackList(stickerPackList);
                stickerPackListActivity.allStickerPacksListAdapter.notifyDataSetChanged();
            }
        }
    }
}

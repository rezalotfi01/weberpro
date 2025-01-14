package com.github.rezalotfi01.weberpro.Activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.github.rezalotfi01.weberpro.BaseClasses.WeberActivity;
import com.github.rezalotfi01.weberpro.Database.Record;
import com.github.rezalotfi01.weberpro.R;
import com.github.rezalotfi01.weberpro.Service.HolderService;
import com.github.rezalotfi01.weberpro.Utils.BrowserUtils;
import com.github.rezalotfi01.weberpro.Utils.IntentUtils;
import com.github.rezalotfi01.weberpro.Utils.RecordUtils;
import com.github.rezalotfi01.weberpro.View.DialogAdapter;
import com.github.rezalotfi01.weberpro.View.WeberContextWrapper;
import com.github.rezalotfi01.weberpro.View.WeberToast;
import com.sdsmdg.tastytoast.TastyToast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class HolderActivity extends WeberActivity {
    private static final int TIMER_SCHEDULE_DEFAULT = 512;

    private Record first = null;
    private Record second = null;
    private Timer timer = null;
    private boolean background = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() == null || getIntent().getData() == null) {
            finish();
            return;
        }

        first = new Record();
        first.setTitle(getString(R.string.album_untitled));
        first.setURL(getIntent().getData().toString());
        first.setTime(System.currentTimeMillis());

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (first != null && second == null) {
                    Intent toService = new Intent(HolderActivity.this, HolderService.class);
                    RecordUtils.INSTANCE.setHolder(first);
                    startService(toService);
                    background = true;
                }
                HolderActivity.this.finish();
            }
        };
        timer = new Timer();
        timer.schedule(task, TIMER_SCHEDULE_DEFAULT);
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (intent == null || intent.getData() == null || first == null) {
            finish();
            return;
        }

        if (timer != null) {
            timer.cancel();
        }

        second = new Record();
        second.setTitle(getString(R.string.album_untitled));
        second.setURL(intent.getData().toString());
        second.setTime(System.currentTimeMillis());

        if (first.getURL().equals(second.getURL())) {
            showHolderDialog();
        } else {
            Intent toService = new Intent(HolderActivity.this, HolderService.class);
            RecordUtils.INSTANCE.setHolder(second);
            startService(toService);
            background = true;
            finish();
        }
    }

    @Override
    public void onDestroy() {
        if (timer != null) {
            timer.cancel();
        }

        if (background) {
            WeberToast.Companion.showPrettyToast(this, R.string.toast_load_in_background, TastyToast.LENGTH_LONG,TastyToast.INFO);
            WeberToast.Companion.showPrettyToast(this, R.string.toast_load_in_background, TastyToast.LENGTH_SHORT,TastyToast.INFO);
        }

        first = null;
        second = null;
        timer = null;
        background = false;
        super.onDestroy();
    }

    private void showHolderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(new WeberContextWrapper(this));
        builder.setCancelable(true);

        FrameLayout linearLayout = (FrameLayout) getLayoutInflater().inflate(R.layout.dialog_list, null, false);
        builder.setView(linearLayout);

        String[] strings = getResources().getStringArray(R.array.holder_menu);
        List<String> list = new ArrayList<>(Arrays.asList(strings));

        ListView listView = linearLayout.findViewById(R.id.dialog_list);
        DialogAdapter adapter = new DialogAdapter(this, R.layout.dialog_text_item, list);
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        final AlertDialog dialog = builder.create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                HolderActivity.this.finish();
            }
        });
        dialog.show();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        Intent toActivity = new Intent(HolderActivity.this, BrowserActivity.class);
                        toActivity.putExtra(IntentUtils.OPEN, first.getURL());
                        startActivity(toActivity);
                        break;
                    case 1:
                        BrowserUtils.copyURL(HolderActivity.this, first.getURL());
                        break;
                    case 2:
                        IntentUtils.share(HolderActivity.this, first.getTitle(), first.getURL());
                        break;
                    default:
                        break;
                }
                dialog.hide();
                dialog.dismiss();
                finish();
            }
        });
    }
}

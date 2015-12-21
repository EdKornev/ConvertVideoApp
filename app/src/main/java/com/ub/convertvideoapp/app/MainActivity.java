package com.ub.convertvideoapp.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import net.ypresto.androidtranscoder.MediaTranscoder;
import net.ypresto.androidtranscoder.format.MediaFormatPresets;
import net.ypresto.androidtranscoder.format.MediaFormatStrategy;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK = 1;

    private EditText mETWidth, mETHeight;
    private Spinner spinner;

    private ProgressDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if (mETHeight.getText().toString().isEmpty() || mETWidth.getText().toString().isEmpty()) {
//                    Toast.makeText(MainActivity.this, "Fill height and width", Toast.LENGTH_SHORT).show();
//                    return;
//                }
                startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("video/*"), REQUEST_CODE_PICK);
            }
        });

        mETHeight = (EditText) findViewById(R.id.et_height);
        mETWidth = (EditText) findViewById(R.id.et_width);

        List<String> results = new ArrayList<>();

        int size = MediaCodecList.getCodecCount();
        for (int i = 0; i < size; i++) {
            MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);
            if (info.isEncoder()) {
                for (String type : info.getSupportedTypes()) {
                    if (type.startsWith("video/")) {
                        results.add(type);
                    }
                }
            }
        }

        spinner = (Spinner) findViewById(R.id.s_type);
        spinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, results));

        mDialog = new ProgressDialog(this);
        mDialog.setTitle("Decoding video ...");
        mDialog.setMessage("Decoding in progress ...");
        mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mDialog.setProgress(0);
        mDialog.setMax(100);
        mDialog.setCancelable(false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_PICK) {
            final File file;
            try {
                file = File.createTempFile("transcode_test_" + Calendar.getInstance().getTimeInMillis(), ".mp4", getExternalFilesDir(null));
            } catch (IOException e) {
                Toast.makeText(this, "Failed to create temporary file.", Toast.LENGTH_LONG).show();
                return;
            }

            ContentResolver resolver = getContentResolver();
            ParcelFileDescriptor parcelFileDescriptor = null;
            try {
                parcelFileDescriptor = resolver.openFileDescriptor(data.getData(), "r");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Coder.Listener listener = new Coder.Listener() {
                @Override
                public void onTranscodeProgress(double progress) {
                    Log.e("Progress", String.valueOf(progress));

                    mDialog.setProgress(Double.valueOf(progress*100).intValue());
                }

                @Override
                public void onTranscodeCompleted() {
                    mDialog.cancel();
                    startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(file), "video/*"));
                }

                @Override
                public void onTranscodeFailed(Exception exception) {
                    Log.e("Some", exception.getMessage(), exception);
                }
            };

            CustomFormatStrategy strategy = new CustomFormatStrategy();
            strategy.setType(spinner.getSelectedItem().toString());

            mDialog.show();

            Coder.getInstance().transcodeVideo(fileDescriptor, file.getAbsolutePath(),
                    strategy, listener);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

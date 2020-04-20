package com.example.moneyrecordv3;

import android.app.ProgressDialog;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager viewPager;
    private ViewPagerAdapter adapter;

    myDbAdapter helper;
    ProgressBar progressBar;
    Vibrator vibrator;
    int VIBRATION_TIME = 200;

    private SharedViewModel sharedVM;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        adapter = new ViewPagerAdapter(getSupportFragmentManager());

        adapter.AddFragment(new FragmentSubmit(), "");
        adapter.AddFragment(new FragmentHistory(), "");
        adapter.AddFragment(new FragmentSummary(), "");

        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(3); // prevent fragments to be 'recreated' when switching between tabs
        tabLayout.setupWithViewPager(viewPager);

        tabLayout.getTabAt(0).setIcon(R.drawable.ic_submit);
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_history);
        tabLayout.getTabAt(2).setIcon(R.drawable.ic_summary);

        helper = new myDbAdapter(this);
        vibrator = (Vibrator) MainActivity.this.getSystemService(Context.VIBRATOR_SERVICE);

        sharedVM = ViewModelProviders.of(this).get(SharedViewModel.class);
        sharedVM.getFragment().observe(this, new Observer<String[]>() {
            @Override
            public void onChanged(@Nullable String[] payload) {
                viewPager.setCurrentItem(Integer.valueOf(payload[0]));
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_backup:
                backupDatabase();
                break;
            case R.id.menu_item_cloud_download:
                downloadDatabase();
                break;
            case R.id.menu_item_renumber:
                renumberDatabase();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void backupDatabase() {
        progressBar = findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE); // show ProgressBar

        final String database = helper.getDataOrderedByDate();

        StringRequest request = new StringRequest(Request.Method.POST, getResources().getString(R.string.gsheet_url),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        progressBar.setVisibility(View.INVISIBLE); // hide ProgressBar
                        vibrator.vibrate(VibrationEffect.createOneShot(VIBRATION_TIME, VibrationEffect.DEFAULT_AMPLITUDE));
                        if (response.equals("SUCCESS")) {
                            Toast.makeText(MainActivity.this, "DB Backup successful!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(MainActivity.this, "DB Backup failed!", Toast.LENGTH_LONG).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressBar.setVisibility(View.INVISIBLE); // hide ProgressBar
                        vibrator.vibrate(VibrationEffect.createOneShot(VIBRATION_TIME, VibrationEffect.DEFAULT_AMPLITUDE));
                        Toast.makeText(MainActivity.this, "DB Backup failed!", Toast.LENGTH_LONG).show();
                    }
                }
        ) {
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("action", "backupDB");
                params.put("database", database);
                return params;
            }
        };

        int socketTimeOut = 60000; // milliseconds

        RetryPolicy retryPolicy = new DefaultRetryPolicy(socketTimeOut, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        request.setRetryPolicy(retryPolicy);

        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);

        queue.add(request);
    }

    private void downloadDatabase() {
        progressBar = findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE); // show ProgressBar

        StringRequest request = new StringRequest(Request.Method.POST, getResources().getString(R.string.gsheet_url),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        progressBar.setVisibility(View.INVISIBLE); // hide ProgressBar
                        vibrator.vibrate(VibrationEffect.createOneShot(VIBRATION_TIME, VibrationEffect.DEFAULT_AMPLITUDE));
                        if (!response.isEmpty()) {
                            String[] lines = response.split(Pattern.quote("\\n"));

                            for (int i = lines.length-2; i >= 0; i--) {
                                String[] data = lines[i].split(" ");
                                String date = data[1];
                                Float amount = Float.valueOf(data[2]);
                                String method = data[3];
                                String category = data[4];
                                String comments = "";
                                if (data.length == 6) {
                                    comments = data[5];
                                }
                                helper.insertData(date, amount, method, category, comments);
                            }

                            Toast.makeText(MainActivity.this, "DB Download successful!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(MainActivity.this, "DB Download failed!", Toast.LENGTH_LONG).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressBar.setVisibility(View.INVISIBLE); // hide ProgressBar
                        vibrator.vibrate(VibrationEffect.createOneShot(VIBRATION_TIME, VibrationEffect.DEFAULT_AMPLITUDE));
                        Toast.makeText(MainActivity.this, "DB Download failed!", Toast.LENGTH_LONG).show();
                    }
                }
        ) {
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("action", "downloadDB");
                return params;
            }
        };

        int socketTimeOut = 60000; // milliseconds

        RetryPolicy retryPolicy = new DefaultRetryPolicy(socketTimeOut, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        request.setRetryPolicy(retryPolicy);

        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);

        queue.add(request);
    }

    private void renumberDatabase() {
        Toast.makeText(this, "Renumbering DB - WIP", Toast.LENGTH_SHORT).show();
    }
}

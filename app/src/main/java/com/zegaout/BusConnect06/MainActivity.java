package com.zegaout.BusConnect06;

import android.app.SearchManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

    SearchAdapter   _adapter;
    BusStop         _last;
    Tracker         _tracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        _adapter = new SearchAdapter(this, getBusStops());
        ListView listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(_adapter);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        _tracker = getDefaultTracker();
        SharedPreferences history = getSharedPreferences("history", MODE_PRIVATE);
        Integer lastStop = history.getInt("lastStop", -1);
        if (lastStop != -1) {
            _last = new BusStop("lastStop", lastStop, "lastStop");
            makeRequest(_last);
        } else {
            _last = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        final ListView listView = (ListView) findViewById(R.id.listView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                MenuItemCompat.collapseActionView(menu.getItem(0));
                _last = (BusStop) listView.getAdapter().getItem(i);
                SharedPreferences userDetails = getSharedPreferences("history", MODE_PRIVATE);
                SharedPreferences.Editor edit = userDetails.edit();
                edit.clear();
                edit.putInt("lastStop", _last._num);
                edit.commit();
                makeRequest(_last);
            }
        });
        MenuItemCompat.setOnActionExpandListener(menu.getItem(0), new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                listView.setVisibility(View.VISIBLE);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                listView.setVisibility(View.GONE);
                return true;
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                ((ArrayAdapter<BusStop>) listView.getAdapter()).getFilter().filter(newText);
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            makeRequest(_last);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private ArrayList<BusStop> getBusStops()
    {
        ArrayList<BusStop> rtn = new ArrayList<BusStop>();
        Scanner s = null;

        try {
            s = new Scanner(getResources().getAssets().open("busstops.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        };
        while (s.hasNextLine()){
            String str = s.nextLine();
            String[] data = str.split(";");
            rtn.add(new BusStop(data[0], Integer.parseInt(data[2]), data[1]));
        }
        s.close();
        return (rtn);
    }

    private TimetablesModel getTimetables(BusStop busStop)
    {
        TimetablesModel timetables = new TimetablesModel();
        timetables.getFromHTML(busStop._num);
        return timetables;

    }

    private void setTimetableView(TimetablesModel timetables)
    {
        findViewById(R.id.progressBar).setVisibility(View.GONE);
        findViewById(R.id.scrollView).setVisibility(View.VISIBLE);
        ((TextView)findViewById(R.id.busstop)).setText(timetables._stop);

        if (timetables._timetables == null) {
            ((TextView)findViewById(R.id.refresh_time)).setText(R.string.no_schedule);
            return;
        } else if (timetables._getTime == null) {
            Toast.makeText(this, "Une erreur est survenue", Toast.LENGTH_SHORT).show();
            return;
        }
        ((TextView)findViewById(R.id.refresh_time)).setText(timetables._getTime);
        LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        for (TimetablesModel.LineTimetables linetimetables : timetables._timetables) {
            View v = vi.inflate(R.layout.bus_layout, null);
            ((TextView)v.findViewById(R.id.line_text)).setText(linetimetables._line);
            for(TimetablesModel.Timetable timetable : linetimetables._timetables) {
                View v2 = vi.inflate(R.layout.bus_schedule, null);
                if (timetable._isVirtual)
                    timetable._time += "*";
                ((TextView)v2.findViewById(R.id.schedule_time)).setText(timetable._time);
                ((TextView)v2.findViewById(R.id.schedule_direction)).setText(timetable._direction);

                ViewGroup insertPoint2 = (ViewGroup) v.findViewById(R.id.line_container);
                insertPoint2.addView(v2);
            }
            ViewGroup insertPoint = (ViewGroup) findViewById(R.id.result_container);
            insertPoint.addView(v);
        }
    }

    private void makeRequest(BusStop busstop)
    {
        if (busstop == null) {
            Toast.makeText(this, "Rien à rafraîchir :/", Toast.LENGTH_SHORT).show();
            return;
        }
        _tracker.send(new HitBuilders.EventBuilder()
                .setCategory("Action")
                .setAction("Get")
                .build());
        ((LinearLayout)findViewById(R.id.result_container)).removeAllViews();
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        findViewById(R.id.scrollView).setVisibility(View.GONE);
        new APIRequester().execute(busstop);
    }

    private class APIRequester extends AsyncTask<BusStop, Void, TimetablesModel>
    {
        @Override
        protected TimetablesModel doInBackground(BusStop... busStops) {
            return getTimetables(busStops[0]);
        }

        @Override
        protected void onPostExecute(TimetablesModel result) {
            setTimetableView(result);
        }
    }

    synchronized public Tracker getDefaultTracker() {
        if (_tracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
            _tracker = analytics.newTracker("UA-55879257-2");
        }
        return _tracker;
    }
}

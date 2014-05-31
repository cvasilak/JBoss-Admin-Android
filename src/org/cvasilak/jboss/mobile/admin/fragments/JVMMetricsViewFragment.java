/*
 * JBoss Admin
 * Copyright 2013, Christos Vasilakis, and individual contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cvasilak.jboss.mobile.admin.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.cvasilak.jboss.mobile.admin.JBossAdminApplication;
import org.cvasilak.jboss.mobile.admin.R;
import org.cvasilak.jboss.mobile.admin.fragments.dialogs.ErrorDialogFragment;
import org.cvasilak.jboss.mobile.admin.fragments.dialogs.ProgressDialogFragment;
import org.cvasilak.jboss.mobile.admin.model.Metric;
import org.cvasilak.jboss.mobile.admin.net.Callback;
import org.cvasilak.jboss.mobile.admin.util.commonsware.MergeAdapter;
import org.cvasilak.jboss.mobile.admin.util.listview.adapters.MetricsAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JVMMetricsViewFragment extends ListFragment {

    private static final String TAG = JVMMetricsViewFragment.class.getSimpleName();

    private JBossAdminApplication application;

    private List<Metric> osMetrics;
    private List<Metric> heapMetrics;
    private List<Metric> nonHeapMetrics;
    private List<Metric> threadUsageMetrics;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        Log.d(TAG, "@onCreate()");

        application = (JBossAdminApplication) getActivity().getApplication();

        osMetrics = new ArrayList<Metric>();

        osMetrics.add(new Metric("Name", "name"));
        osMetrics.add(new Metric("Version", "version"));
        osMetrics.add(new Metric("Processors", "available-processors"));

        heapMetrics = new ArrayList<Metric>();

        heapMetrics.add(new Metric("Max", "max"));
        heapMetrics.add(new Metric("Used", "used"));
        heapMetrics.add(new Metric("Committed", "committed"));
        heapMetrics.add(new Metric("Init", "init"));

        nonHeapMetrics = new ArrayList<Metric>();

        nonHeapMetrics.add(new Metric("Max", "max"));
        nonHeapMetrics.add(new Metric("Used", "used"));
        nonHeapMetrics.add(new Metric("Committed", "committed"));
        nonHeapMetrics.add(new Metric("Init", "init"));

        threadUsageMetrics = new ArrayList<Metric>();

        threadUsageMetrics.add(new Metric("Live", "thread-count"));
        threadUsageMetrics.add(new Metric("Daemon", "daemon"));

        setHasOptionsMenu(true);

        refresh();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ActionBar bar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        bar.setTitle(getString(R.string.jvm));

        MergeAdapter adapter = new MergeAdapter();

        TextView sectionHeader;

        // Section: Operating System Information
        sectionHeader = new TextView(getActivity());
        sectionHeader.setBackgroundColor(Color.DKGRAY);
        sectionHeader.setPadding(15, 10, 0, 10);
        sectionHeader.setText("Operating System");
        adapter.addView(sectionHeader);

        MetricsAdapter osMetricsAdapter = new MetricsAdapter(getActivity(), osMetrics);
        adapter.addAdapter(osMetricsAdapter);

        // Section: Heap Usage
        sectionHeader = new TextView(getActivity());
        sectionHeader.setBackgroundColor(Color.DKGRAY);
        sectionHeader.setPadding(15, 10, 0, 10);
        sectionHeader.setText("Heap Usage");
        adapter.addView(sectionHeader);

        MetricsAdapter heapMetricsAdapter = new MetricsAdapter(getActivity(), heapMetrics);
        adapter.addAdapter(heapMetricsAdapter);

        // Section: Non Heap Usage
        sectionHeader = new TextView(getActivity());
        sectionHeader.setBackgroundColor(Color.DKGRAY);
        sectionHeader.setPadding(15, 10, 0, 10);
        sectionHeader.setText("Non Heap Usage");
        adapter.addView(sectionHeader);

        MetricsAdapter nonHeapMetricsAdapter = new MetricsAdapter(getActivity(), nonHeapMetrics);
        adapter.addAdapter(nonHeapMetricsAdapter);

        // Section: Thread Usage
        sectionHeader = new TextView(getActivity());
        sectionHeader.setBackgroundColor(Color.DKGRAY);
        sectionHeader.setPadding(15, 10, 0, 10);
        sectionHeader.setText("Thread Usage");
        adapter.addView(sectionHeader);

        MetricsAdapter threadUsageMetricsAdapter = new MetricsAdapter(getActivity(), threadUsageMetrics);
        adapter.addAdapter(threadUsageMetricsAdapter);

        setListAdapter(adapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_refresh, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.refresh) {
            refresh();

            return (true);
        }

        return (super.onOptionsItemSelected(item));
    }

    public void refresh() {
        ProgressDialogFragment.showDialog(getActivity(), R.string.queryingServer);

        application.getOperationsManager().fetchJVMMetrics(new Callback() {
            @Override
            public void onSuccess(JsonElement reply) {
                ProgressDialogFragment.dismissDialog(getActivity());

                JsonObject jsonObj = reply.getAsJsonObject();

                JsonObject step1 = jsonObj.getAsJsonObject("step-1").getAsJsonObject("result");

                /* Memory */
                long max, committed, init, used;
                float usedPerc, committedPerc, initPerc;

                // Heap Usage
                JsonObject jsonHeapUsage = step1.getAsJsonObject("heap-memory-usage");
                Map<String, String> heapUsage = new HashMap<String, String>();

                max = jsonHeapUsage.getAsJsonPrimitive("max").getAsLong() / 1024 / 1024;
                committed = jsonHeapUsage.getAsJsonPrimitive("committed").getAsLong() / 1024 / 1024;
                committedPerc = (max != 0 ? ((float) committed / max) * 100 : 0);
                init = jsonHeapUsage.getAsJsonPrimitive("init").getAsLong() / 1024 / 1024;
                initPerc = (max != 0 ? ((float) init / max) * 100 : 0);
                used = jsonHeapUsage.getAsJsonPrimitive("used").getAsLong() / 1024 / 1024;
                usedPerc = (max != 0 ? ((float) used / max) * 100 : 0);

                heapUsage.put("max", String.format("%d MB", max));
                heapUsage.put("used", String.format("%d MB (%.0f%%)", used, usedPerc));
                heapUsage.put("committed", String.format("%d MB (%.0f%%)", committed, committedPerc));
                heapUsage.put("init", String.format("%d MB (%.0f%%)", init, initPerc));

                for (Metric metric : heapMetrics) {
                    metric.setValue(heapUsage.get(metric.getKey()));
                }

                // Non Heap Usage
                JsonObject jsonNonHeapUsage = step1.getAsJsonObject("non-heap-memory-usage");
                Map<String, String> nonHeapUsage = new HashMap<String, String>();

                max = jsonNonHeapUsage.getAsJsonPrimitive("max").getAsLong() / 1024 / 1024;
                committed = jsonNonHeapUsage.getAsJsonPrimitive("committed").getAsLong() / 1024 / 1024;
                committedPerc = (max != 0 ? ((float) committed / max) * 100 : 0);
                init = jsonNonHeapUsage.getAsJsonPrimitive("init").getAsLong() / 1024 / 1024;
                initPerc = (max != 0 ? ((float) init / max) * 100 : 0);
                used = jsonNonHeapUsage.getAsJsonPrimitive("used").getAsLong() / 1024 / 1024;
                usedPerc = (max != 0 ? ((float) used / max) * 100 : 0);

                nonHeapUsage.put("max", String.format("%d MB", max));
                nonHeapUsage.put("used", String.format("%d MB (%.0f%%)", used, usedPerc));
                nonHeapUsage.put("committed", String.format("%d MB (%.0f%%)", committed, committedPerc));
                nonHeapUsage.put("init", String.format("%d MB (%.0f%%)", init, initPerc));

                for (Metric metric : nonHeapMetrics) {
                    metric.setValue(nonHeapUsage.get(metric.getKey()));
                }

                // Threading
                JsonObject jsonThreading = jsonObj.getAsJsonObject("step-2").getAsJsonObject("result");
                Map<String, String> threading = new HashMap<String, String>();

                int threadCount = jsonThreading.getAsJsonPrimitive("thread-count").getAsInt();
                int daemonThreadCount = jsonThreading.getAsJsonPrimitive("daemon-thread-count").getAsInt();
                float daemonUsedPerc = (threadCount != 0 ? ((float) daemonThreadCount / threadCount) * 100 : 0);

                threading.put("thread-count", String.format("%d", threadCount));
                threading.put("daemon", String.format("%d (%.0f%%)", daemonThreadCount, daemonUsedPerc));

                for (Metric metric : threadUsageMetrics) {
                    metric.setValue(threading.get(metric.getKey()));
                }

                // OS
                JsonObject jsonOS = jsonObj.getAsJsonObject("step-3").getAsJsonObject("result");
                Map<String, String> os = new HashMap<String, String>();

                os.put("name", jsonOS.get("name").getAsString());
                os.put("version", jsonOS.get("version").getAsString());
                os.put("available-processors", jsonOS.get("available-processors").getAsString());

                for (Metric metric : osMetrics) {
                    metric.setValue(os.get(metric.getKey()));
                }

                ((MergeAdapter) getListAdapter()).notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception e) {
                ProgressDialogFragment.dismissDialog(getActivity());

                ErrorDialogFragment.showDialog(getActivity(), e.getMessage());
            }
        });
    }
}
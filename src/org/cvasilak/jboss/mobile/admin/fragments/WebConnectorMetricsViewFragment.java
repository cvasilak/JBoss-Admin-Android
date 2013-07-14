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
import android.widget.TextView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
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

public class WebConnectorMetricsViewFragment extends SherlockListFragment {

    private static final String TAG = WebConnectorMetricsViewFragment.class.getSimpleName();

    private JBossAdminApplication application;

    private List<Metric> generalMetrics;
    private List<Metric> reqPerConnectorMetrics;

    private String connectorName;

    public static WebConnectorMetricsViewFragment newInstance(String name) {
        WebConnectorMetricsViewFragment f = new WebConnectorMetricsViewFragment();

        Bundle args = new Bundle();
        args.putString("connectorName", name);

        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        this.connectorName = getArguments().getString("connectorName");

        application = (JBossAdminApplication) getSherlockActivity().getApplication();

        generalMetrics = new ArrayList<Metric>();

        generalMetrics.add(new Metric("Protocol", "protocol"));
        generalMetrics.add(new Metric("Bytes Sent", "bytesSent"));
        generalMetrics.add(new Metric("Bytes Received", "bytesReceived"));

        reqPerConnectorMetrics = new ArrayList<Metric>();

        reqPerConnectorMetrics.add(new Metric("Request Count", "requestCount"));
        reqPerConnectorMetrics.add(new Metric("Error Count", "errorCount"));
        reqPerConnectorMetrics.add(new Metric("Processing Time (ms)", "processingTime"));
        reqPerConnectorMetrics.add(new Metric("Max Time (ms)", "maxTime"));

        setHasOptionsMenu(true);

        refresh();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ActionBar bar = getSherlockActivity().getSupportActionBar();
        bar.setTitle(connectorName);

        MergeAdapter adapter = new MergeAdapter();

        TextView sectionHeader;

        // Section: General
        sectionHeader = new TextView(getSherlockActivity());
        sectionHeader.setBackgroundColor(Color.DKGRAY);
        sectionHeader.setPadding(15, 10, 0, 10);
        sectionHeader.setText("General");
        adapter.addView(sectionHeader);

        MetricsAdapter generalMetricsAdapter = new MetricsAdapter(getSherlockActivity(), generalMetrics);
        adapter.addAdapter(generalMetricsAdapter);

        // Section: Request per Connector
        sectionHeader = new TextView(getSherlockActivity());
        sectionHeader.setBackgroundColor(Color.DKGRAY);
        sectionHeader.setPadding(15, 10, 0, 10);
        sectionHeader.setText("Request per Connector");
        adapter.addView(sectionHeader);

        MetricsAdapter reqPerConnectorMetricsAdapter = new MetricsAdapter(getSherlockActivity(), reqPerConnectorMetrics);
        adapter.addAdapter(reqPerConnectorMetricsAdapter);

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
        ProgressDialogFragment.showDialog(getSherlockActivity(), R.string.queryingServer);

        application.getOperationsManager().fetchWebConnectorMetrics(connectorName, new Callback() {
            @Override
            public void onSuccess(JsonElement reply) {
                ProgressDialogFragment.dismissDialog(getSherlockActivity());

                JsonObject jsonObj = reply.getAsJsonObject();

                Map<String, String> info = new HashMap<String, String>();

                long bytesSent = jsonObj.getAsJsonPrimitive("bytesSent").getAsLong();
                long bytesReceived = jsonObj.getAsJsonPrimitive("bytesReceived").getAsLong();

                info.put("bytesSent", String.format("%d", bytesSent));
                info.put("bytesReceived", String.format("%d", bytesReceived));
                info.put("protocol", jsonObj.get("protocol").getAsString());

                for (Metric metric : generalMetrics) {
                    metric.setValue(info.get(metric.getKey()));
                }

                int requestCount = jsonObj.getAsJsonPrimitive("requestCount").getAsInt();
                int errorCount = jsonObj.getAsJsonPrimitive("errorCount").getAsInt();
                float errorPerc = (requestCount != 0 ? ((float) errorCount / requestCount) * 100 : 0);
                int processingTime = jsonObj.getAsJsonPrimitive("processingTime").getAsInt();
                int maxTime = jsonObj.getAsJsonPrimitive("maxTime").getAsInt();

                info.put("requestCount", String.format("%d", requestCount));
                info.put("errorCount", String.format("%d (%.0f%%)", errorCount, errorPerc));
                info.put("processingTime", String.format("%d", processingTime));
                info.put("maxTime", String.format("%d", maxTime));

                for (Metric metric : reqPerConnectorMetrics) {
                    metric.setValue(info.get(metric.getKey()));
                }

                // refresh table
                ((MergeAdapter) getListAdapter()).notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception e) {
                ProgressDialogFragment.dismissDialog(getSherlockActivity());

                ErrorDialogFragment.showDialog(getSherlockActivity(), e.getMessage());
            }
        });
    }
}
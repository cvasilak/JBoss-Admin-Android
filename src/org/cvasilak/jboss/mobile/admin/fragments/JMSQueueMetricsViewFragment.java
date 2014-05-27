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
import org.cvasilak.jboss.mobile.admin.net.JBossOperationsManager.JMSType;
import org.cvasilak.jboss.mobile.admin.util.commonsware.MergeAdapter;
import org.cvasilak.jboss.mobile.admin.util.listview.adapters.MetricsAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JMSQueueMetricsViewFragment extends ListFragment {

    private static final String TAG = JMSQueueMetricsViewFragment.class.getSimpleName();

    private JBossAdminApplication application;

    private List<Metric> inFlightMetrics;
    private List<Metric> msgProcessedMetrics;
    private List<Metric> consumerMetrics;

    private String queueName;

    public static JMSQueueMetricsViewFragment newInstance(String name) {
        JMSQueueMetricsViewFragment f = new JMSQueueMetricsViewFragment();

        Bundle args = new Bundle();
        args.putString("queueName", name);

        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        this.queueName = getArguments().getString("queueName");

        application = (JBossAdminApplication) getActivity().getApplication();

        inFlightMetrics = new ArrayList<Metric>();

        inFlightMetrics.add(new Metric("Messages In Queue", "message-count"));
        inFlightMetrics.add(new Metric("In Delivery", "delivering-count"));

        msgProcessedMetrics = new ArrayList<Metric>();

        msgProcessedMetrics.add(new Metric("Messages Added", "messages-added"));
        msgProcessedMetrics.add(new Metric("Messages Scheduled", "scheduled-count"));

        consumerMetrics = new ArrayList<Metric>();

        consumerMetrics.add(new Metric("Number of Consumer", "consumer-count"));

        setHasOptionsMenu(true);

        refresh();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ActionBar bar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        bar.setTitle(queueName);

        MergeAdapter adapter = new MergeAdapter();

        TextView sectionHeader;

        // Section: In-Flight Messages
        sectionHeader = new TextView(getActivity());
        sectionHeader.setBackgroundColor(Color.DKGRAY);
        sectionHeader.setPadding(15, 10, 0, 10);
        sectionHeader.setText("In-Flight Messages");
        adapter.addView(sectionHeader);

        MetricsAdapter inFlightMetricsAdapter = new MetricsAdapter(getActivity(), inFlightMetrics);
        adapter.addAdapter(inFlightMetricsAdapter);

        // Section: Messages Processed
        sectionHeader = new TextView(getActivity());
        sectionHeader.setBackgroundColor(Color.DKGRAY);
        sectionHeader.setPadding(15, 10, 0, 10);
        sectionHeader.setText("Messages Processed");
        adapter.addView(sectionHeader);

        MetricsAdapter msgProcessedMetricsAdapter = new MetricsAdapter(getActivity(), msgProcessedMetrics);
        adapter.addAdapter(msgProcessedMetricsAdapter);

        // Section: Consumer
        sectionHeader = new TextView(getActivity());
        sectionHeader.setBackgroundColor(Color.DKGRAY);
        sectionHeader.setPadding(15, 10, 0, 10);
        sectionHeader.setText("Consumer");
        adapter.addView(sectionHeader);

        MetricsAdapter consumerMetricsAdapter = new MetricsAdapter(getActivity(), consumerMetrics);
        adapter.addAdapter(consumerMetricsAdapter);

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

        application.getOperationsManager().fetchJMSQueueMetrics(queueName, JMSType.QUEUE, new Callback() {
            @Override
            public void onSuccess(JsonElement reply) {
                ProgressDialogFragment.dismissDialog(getActivity());

                JsonObject jsonObj = reply.getAsJsonObject();

                Map<String, String> info = new HashMap<String, String>();

                int msgCount = jsonObj.getAsJsonPrimitive("message-count").getAsInt();
                int delivCount = jsonObj.getAsJsonPrimitive("delivering-count").getAsInt();
                float delivPerc = (msgCount != 0 ? ((float) delivCount / msgCount) * 100 : 0);
                int msgAdded = jsonObj.getAsJsonPrimitive("messages-added").getAsInt();

                info.put("message-count", String.format("%d", msgCount));
                info.put("delivering-count", String.format("%d (%.0f%%)", delivCount, delivPerc));
                for (Metric metric : inFlightMetrics) {
                    metric.setValue(info.get(metric.getKey()));
                }

                int schCount = jsonObj.getAsJsonPrimitive("scheduled-count").getAsInt();
                info.put("messages-added", String.format("%d", msgAdded));
                info.put("scheduled-count", String.format("%d", schCount));

                for (Metric metric : msgProcessedMetrics) {
                    metric.setValue(info.get(metric.getKey()));
                }

                int consCount = jsonObj.getAsJsonPrimitive("consumer-count").getAsInt();
                info.put("consumer-count", String.format("%d", consCount));

                for (Metric metric : consumerMetrics) {
                    metric.setValue(info.get(metric.getKey()));
                }

                // refresh table
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
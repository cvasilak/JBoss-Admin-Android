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

public class TransactionMetricsViewFragment extends SherlockListFragment {

    private static final String TAG = TransactionMetricsViewFragment.class.getSimpleName();

    private JBossAdminApplication application;

    private List<Metric> sucFailMetrics;
    private List<Metric> failOriginMetrics;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        application = (JBossAdminApplication) getSherlockActivity().getApplication();

        sucFailMetrics = new ArrayList<Metric>();

        sucFailMetrics.add(new Metric("Total", "number-of-transactions"));
        sucFailMetrics.add(new Metric("Commited", "number-of-committed-transactions"));
        sucFailMetrics.add(new Metric("Aborted", "number-of-aborted-transactions"));
        sucFailMetrics.add(new Metric("Timed Out", "number-of-timed-out-transactions"));

        failOriginMetrics = new ArrayList<Metric>();

        failOriginMetrics.add(new Metric("Applications", "number-of-application-rollbacks"));
        failOriginMetrics.add(new Metric("Resources", "number-of-resource-rollbacks"));

        setHasOptionsMenu(true);

        refresh();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ActionBar bar = getSherlockActivity().getSupportActionBar();
        bar.setTitle(getString(R.string.transactions));

        MergeAdapter adapter = new MergeAdapter();

        TextView sectionHeader;

        // Section: Success/Failure Ratio
        sectionHeader = new TextView(getSherlockActivity());
        sectionHeader.setBackgroundColor(Color.DKGRAY);
        sectionHeader.setPadding(15, 10, 0, 10);
        sectionHeader.setText("Success/Failure Ratio");
        adapter.addView(sectionHeader);

        MetricsAdapter sucFailMetricsAdapter = new MetricsAdapter(getSherlockActivity(), sucFailMetrics);
        adapter.addAdapter(sucFailMetricsAdapter);

        // Section: Failure Origin
        sectionHeader = new TextView(getSherlockActivity());
        sectionHeader.setBackgroundColor(Color.DKGRAY);
        sectionHeader.setPadding(15, 10, 0, 10);
        sectionHeader.setText("Failure Origin");
        adapter.addView(sectionHeader);

        MetricsAdapter failOriginMetricsAdapter = new MetricsAdapter(getSherlockActivity(), failOriginMetrics);
        adapter.addAdapter(failOriginMetricsAdapter);

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

        application.getOperationsManager().fetchTranscationMetrics(new Callback() {
            @Override
            public void onSuccess(JsonElement reply) {
                ProgressDialogFragment.dismissDialog(getSherlockActivity());

                JsonObject jsonObj = reply.getAsJsonObject();

                Map<String, String> info = new HashMap<String, String>();

                int total = jsonObj.getAsJsonPrimitive("number-of-transactions").getAsInt();
                int committed = jsonObj.getAsJsonPrimitive("number-of-committed-transactions").getAsInt();
                float committedPerc = (total != 0 ? ((float) committed / total) * 100 : 0);

                int aborted = jsonObj.getAsJsonPrimitive("number-of-aborted-transactions").getAsInt();
                float abortedPerc = (total != 0 ? ((float) aborted / total) * 100 : 0);

                int timedOut = jsonObj.getAsJsonPrimitive("number-of-timed-out-transactions").getAsInt();
                float timedOutPerc = (total != 0 ? ((float) timedOut / total) * 100 : 0);

                int appRollbacks = jsonObj.getAsJsonPrimitive("number-of-application-rollbacks").getAsInt();
                int resRollbacks = jsonObj.getAsJsonPrimitive("number-of-resource-rollbacks").getAsInt();

                info.put("number-of-transactions", String.format("%d", total));
                info.put("number-of-committed-transactions", String.format("%d (%.0f%%)", committed, committedPerc));
                info.put("number-of-aborted-transactions", String.format("%d (%.0f%%)", aborted, abortedPerc));
                info.put("number-of-timed-out-transactions", String.format("%d (%.0f%%)", timedOut, timedOutPerc));
                info.put("number-of-application-rollbacks", String.format("%d", appRollbacks));
                info.put("number-of-resource-rollbacks", String.format("%d", resRollbacks));

                for (Metric metric : sucFailMetrics) {
                    metric.setValue(info.get(metric.getKey()));
                }

                for (Metric metric : failOriginMetrics) {
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
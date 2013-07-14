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
import org.cvasilak.jboss.mobile.admin.net.JBossOperationsManager.DataSourceType;
import org.cvasilak.jboss.mobile.admin.util.commonsware.MergeAdapter;
import org.cvasilak.jboss.mobile.admin.util.listview.adapters.MetricsAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataSourceMetricsViewFragment extends SherlockListFragment {

    private static final String TAG = DataSourceMetricsViewFragment.class.getSimpleName();

    private JBossAdminApplication application;

    private List<Metric> poolMetrics;
    private List<Metric> prepStatementMetrics;

    private String dsName;
    private DataSourceType dsType;

    public static DataSourceMetricsViewFragment newInstance(String name, DataSourceType type) {
        DataSourceMetricsViewFragment f = new DataSourceMetricsViewFragment();

        Bundle args = new Bundle();
        args.putString("dsName", name);
        args.putString("dsType", type.name());
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        this.dsName = getArguments().getString("dsName");
        this.dsType = DataSourceType.valueOf(getArguments().getString("dsType"));

        application = (JBossAdminApplication) getSherlockActivity().getApplication();

        poolMetrics = new ArrayList<Metric>();

        poolMetrics.add(new Metric("Available", "AvailableCount"));
        poolMetrics.add(new Metric("Active Count", "ActiveCount"));
        poolMetrics.add(new Metric("Max Used", "MaxUsedCount"));

        prepStatementMetrics = new ArrayList<Metric>();

        prepStatementMetrics.add(new Metric("Current Size", "PreparedStatementCacheCurrentSize"));
        prepStatementMetrics.add(new Metric("Hit Count", "PreparedStatementCacheHitCount"));
        prepStatementMetrics.add(new Metric("Miss Used", "PreparedStatementCacheMissCount"));

        setHasOptionsMenu(true);

        refresh();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ActionBar bar = getSherlockActivity().getSupportActionBar();
        bar.setTitle(dsName);

        MergeAdapter adapter = new MergeAdapter();

        TextView sectionHeader;

        // Section: Pool Usage
        sectionHeader = new TextView(getSherlockActivity());
        sectionHeader.setBackgroundColor(Color.DKGRAY);
        sectionHeader.setPadding(15, 10, 0, 10);
        sectionHeader.setText("Pool Usage");
        adapter.addView(sectionHeader);

        MetricsAdapter poolMetricsAdapter = new MetricsAdapter(getSherlockActivity(), poolMetrics);
        adapter.addAdapter(poolMetricsAdapter);

        // Section: Prepared Statement Pool Usage
        sectionHeader = new TextView(getSherlockActivity());
        sectionHeader.setBackgroundColor(Color.DKGRAY);
        sectionHeader.setPadding(15, 10, 0, 10);
        sectionHeader.setText("Prepared Statement Pool Usage");
        adapter.addView(sectionHeader);

        MetricsAdapter prepStatementMetricsAdapter = new MetricsAdapter(getSherlockActivity(), prepStatementMetrics);
        adapter.addAdapter(prepStatementMetricsAdapter);

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

        application.getOperationsManager().fetchDataSourceMetrics(this.dsName, this.dsType, new Callback() {
            @Override
            public void onSuccess(JsonElement reply) {
                ProgressDialogFragment.dismissDialog(getSherlockActivity());

                JsonObject jsonObj = reply.getAsJsonObject();

                Map<String, String> info = new HashMap<String, String>();

                JsonObject jsonPool = jsonObj.getAsJsonObject("step-1").getAsJsonObject("result");
                int availCount = jsonPool.getAsJsonPrimitive("AvailableCount").getAsInt();
                int activeCount = jsonPool.getAsJsonPrimitive("ActiveCount").getAsInt();
                int maxUsedCount = jsonPool.getAsJsonPrimitive("MaxUsedCount").getAsInt();

                float usedPerc = (availCount != 0 ? ((float) activeCount / availCount) * 100 : 0);

                info.put("AvailableCount", String.format("%d", availCount));
                info.put("ActiveCount", String.format("%d (%.0f%%)", activeCount, usedPerc));
                info.put("MaxUsedCount", String.format("%d", maxUsedCount));

                for (Metric metric : poolMetrics) {
                    metric.setValue(info.get(metric.getKey()));
                }

                JsonObject jsonJDBC = jsonObj.getAsJsonObject("step-2").getAsJsonObject("result");
                int curSize = jsonJDBC.getAsJsonPrimitive("PreparedStatementCacheCurrentSize").getAsInt();
                int hitCount = jsonJDBC.getAsJsonPrimitive("PreparedStatementCacheHitCount").getAsInt();
                float hitPerc = (curSize != 0 ? ((float) hitCount / curSize) * 100 : 0);

                int misUsed = jsonJDBC.getAsJsonPrimitive("PreparedStatementCacheMissCount").getAsInt();
                float misPerc = (curSize != 0 ? ((float) misUsed / curSize) * 100 : 0);

                info.put("PreparedStatementCacheCurrentSize", String.format("%d", curSize));
                info.put("PreparedStatementCacheHitCount", String.format("%d (%.0f%%)", hitCount, hitPerc));
                info.put("PreparedStatementCacheMissCount", String.format("%d (%.0f%%)", misUsed, misPerc));

                for (Metric metric : prepStatementMetrics) {
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
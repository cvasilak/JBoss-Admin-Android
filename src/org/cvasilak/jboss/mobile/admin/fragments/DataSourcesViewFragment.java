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

import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBarActivity;
import android.view.*;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.cvasilak.jboss.mobile.admin.JBossAdminApplication;
import org.cvasilak.jboss.mobile.admin.R;
import org.cvasilak.jboss.mobile.admin.activities.JBossServerRootActivity;
import org.cvasilak.jboss.mobile.admin.fragments.dialogs.ErrorDialogFragment;
import org.cvasilak.jboss.mobile.admin.fragments.dialogs.ProgressDialogFragment;
import org.cvasilak.jboss.mobile.admin.net.Callback;
import org.cvasilak.jboss.mobile.admin.net.JBossOperationsManager.DataSourceType;

import java.util.ArrayList;
import java.util.List;

public class DataSourcesViewFragment extends ListFragment {

    private static final String TAG = DataSourcesViewFragment.class.getSimpleName();

    private JBossAdminApplication application;

    private List<DataSource> datasources;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        application = (JBossAdminApplication) getActivity().getApplication();

        datasources = new ArrayList<DataSource>();

        setHasOptionsMenu(true);

        refresh();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ActionBar bar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        bar.setTitle(getString(R.string.data_sources));

        setListAdapter(new DataSourceAdapter(datasources));
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

    @Override
    public void onListItemClick(ListView list, View view, int position, long id) {
        DataSource selectedDS = datasources.get(position);
        DataSourceMetricsViewFragment fragment = DataSourceMetricsViewFragment.newInstance(selectedDS.name, selectedDS.type);

        ((JBossServerRootActivity) getActivity()).addFragment(fragment);
    }

    public void refresh() {
        //progress = ProgressDialog.show(getActivity(), "", getString(R.string.queryingServer));
        ProgressDialogFragment.showDialog(getActivity(), R.string.queryingServer);

        application.getOperationsManager().fetchDataSourceList(new Callback() {
            @Override
            public void onSuccess(JsonElement reply) {
                ProgressDialogFragment.dismissDialog(getActivity());

                datasources.clear();

                JsonObject jsonObj = reply.getAsJsonObject();

                JsonArray jsonDsList = jsonObj.getAsJsonObject("step-1").getAsJsonArray("result");
                for (JsonElement ds : jsonDsList) {
                    datasources.add(new DataSource(ds.getAsString(), DataSourceType.StandardDataSource));
                }

                JsonArray jsonXADsList = jsonObj.getAsJsonObject("step-2").getAsJsonArray("result");
                for (JsonElement ds : jsonXADsList) {
                    datasources.add(new DataSource(ds.getAsString(), DataSourceType.XADataSource));
                }

                ((ArrayAdapter) getListAdapter()).notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception e) {
                ProgressDialogFragment.dismissDialog(getActivity());

                ErrorDialogFragment.showDialog(getActivity(), e.getMessage());
            }
        });
    }

    class DataSourceAdapter extends ArrayAdapter<DataSource> {
        DataSourceAdapter(List<DataSource> list) {
            super(getActivity(), R.layout.icon_text_row, R.id.row_name, list);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = super.getView(position, convertView, parent);
            ViewHolder holder = (ViewHolder) row.getTag();

            if (holder == null) {
                holder = new ViewHolder(row);
                row.setTag(holder);
            }

            holder.populateFrom(getItem(position));

            return (row);
        }
    }

    static class ViewHolder {
        ImageView icon = null;
        TextView name = null;

        ViewHolder(View row) {
            this.icon = (ImageView) row.findViewById(R.id.row_icon);
            this.name = (TextView) row.findViewById(R.id.row_name);
        }

        void populateFrom(DataSource ds) {
            name.setText(ds.name);
            // check and set the XA icon to distinqush XA Data sources
            icon.setImageResource((ds.type == DataSourceType.XADataSource ? R.drawable.ic_xa_ds : 0));
        }
    }

    class DataSource {
        String name;
        DataSourceType type;

        DataSource(String name, DataSourceType type) {
            this.name = name;
            this.type = type;
        }
    }
}
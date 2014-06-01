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

package org.cvasilak.jboss.mobile.app.fragments;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.*;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.cvasilak.jboss.mobile.app.JBossAdminApplication;
import org.cvasilak.jboss.mobile.app.R;
import org.cvasilak.jboss.mobile.app.activities.JBossServerRootActivity;
import org.cvasilak.jboss.mobile.app.fragments.dialogs.ErrorDialogFragment;
import org.cvasilak.jboss.mobile.app.fragments.dialogs.ProgressDialogFragment;
import org.cvasilak.jboss.mobile.app.net.Callback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DomainServerGroupsViewFragment extends ListFragment {

    private static final String TAG = JMSQueuesViewFragment.class.getSimpleName();

    private JBossAdminApplication application;

    private List<Group> groups;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        Log.d(TAG, "@onCreate()");

        application = (JBossAdminApplication) getActivity().getApplication();

        groups = new ArrayList<Group>();

        setHasOptionsMenu(true);

        refresh();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ActionBar bar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        bar.setTitle(getString(R.string.server_groups));

        setListAdapter(new GroupAdapter(groups));
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
        Group group = groups.get(position);

        DeploymentsViewFragment fragment = DeploymentsViewFragment.newInstance(group.name, DeploymentsViewFragment.Mode.SERVER_MODE);

        ((JBossServerRootActivity) getActivity()).addFragment(fragment);
    }

    public void refresh() {
        ProgressDialogFragment.showDialog(getActivity(), R.string.queryingServer);

        application.getOperationsManager().fetchDomainGroups(new Callback() {
            @Override
            public void onSuccess(JsonElement reply) {
                ProgressDialogFragment.dismissDialog(getActivity());

                groups.clear();

                JsonObject jsonObj = reply.getAsJsonObject();

                String name, profile;
                boolean hasDeployments = false;

                for (Map.Entry<String, JsonElement> e : jsonObj.entrySet()) {
                    name = e.getKey();

                    JsonObject detailsJsonObj = e.getValue().getAsJsonObject();

                    if (detailsJsonObj.get("deployment") != null)
                        hasDeployments = true;

                    profile = detailsJsonObj.get("profile").getAsString();

                    groups.add(new Group(name, profile, hasDeployments));
                }

                ((GroupAdapter) getListAdapter()).notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception e) {
                ProgressDialogFragment.dismissDialog(getActivity());

                ErrorDialogFragment.showDialog(getActivity(), e.getMessage());
            }
        });
    }

    class GroupAdapter extends ArrayAdapter<Group> {
        GroupAdapter(List<Group> groups) {
            super(getActivity(), R.layout.twoline_list_item, R.id.text1, groups);
        }

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

        class ViewHolder {
            TextView text1;
            TextView text2;

            ViewHolder(View row) {
                this.text1 = (TextView) row.findViewById(R.id.text1);
                this.text2 = (TextView) row.findViewById(R.id.text2);
            }

            void populateFrom(Group group) {
                text1.setText(group.name);
                text2.setText(group.profile);
            }
        }
    }

    class Group {
        String name;
        String profile;
        boolean hasDeployments;

        Group(String name, String profile, boolean hasDeployments) {
            this.name = name;
            this.profile = profile;
            this.hasDeployments = hasDeployments;
        }
    }

}
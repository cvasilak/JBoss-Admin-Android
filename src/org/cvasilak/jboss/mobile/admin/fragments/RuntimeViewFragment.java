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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.cvasilak.jboss.mobile.admin.JBossAdminApplication;
import org.cvasilak.jboss.mobile.admin.R;
import org.cvasilak.jboss.mobile.admin.activities.JBossServerRootActivity;
import org.cvasilak.jboss.mobile.admin.fragments.dialogs.ErrorDialogFragment;
import org.cvasilak.jboss.mobile.admin.fragments.dialogs.ParameterizedDialogFragment;
import org.cvasilak.jboss.mobile.admin.fragments.dialogs.ProgressDialogFragment;
import org.cvasilak.jboss.mobile.admin.net.Callback;
import org.cvasilak.jboss.mobile.admin.net.JBossOperationsManager.HostStatus;
import org.cvasilak.jboss.mobile.admin.util.commonsware.MergeAdapter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.cvasilak.jboss.mobile.admin.net.JBossOperationsManager.HostStatus.*;

public class RuntimeViewFragment extends ListFragment {

    private static final String TAG = RuntimeViewFragment.class.getSimpleName();

    private JBossAdminApplication application;

    // the Choose Server button on top of the list
    private Button chooseServer;

    // the adapter that holds the hosts listview
    private HostAdapter hostAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "@onCreate()");

        setRetainInstance(true);

        application = (JBossAdminApplication) getActivity().getApplication();

        // inform runtime that we have action buttons
        setHasOptionsMenu(true);

        // determine whether we are running in STANDALONE or DOMAIN mode
        // and update table view accordingly
        if (application.getOperationsManager().getDomainHost() == null) {
            ProgressDialogFragment.showDialog(getActivity(), R.string.fetchingDomainInfo);

            application.getOperationsManager().fetchActiveServerInformation(new Callback() {
                @Override
                public void onSuccess(JsonElement reply) {
                    ProgressDialogFragment.dismissDialog(getActivity());

                    JsonObject hosts = reply.getAsJsonObject();

                    String host = null;
                    String server = null;

                    for (Map.Entry<String, JsonElement> e : hosts.entrySet()) {
                        host = e.getKey();

                        JsonObject hostInfo = e.getValue().getAsJsonObject();
                        JsonObject servers = hostInfo.getAsJsonObject("server");

                        for (Map.Entry<String, JsonElement> p : servers.entrySet()) {
                            server = p.getKey();
                            break;
                        }

                        if (server != null)
                            break;
                    }

                    application.getOperationsManager().changeActiveMonitoringServer(host, server);

                    // update table to reflect mode
                    buildTable();
                }

                @Override
                public void onFailure(Exception e) {
                    ProgressDialogFragment.dismissDialog(getActivity());

                    // HTTP/1.1 500 Internal Server Error
                    // occurred doing :read-children-resources(child-type=host)
                    // the server is running in standalone mode, we can live with that
                    buildTable();
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        ActionBar bar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        bar.setTitle(application.getOperationsManager().getServer().getName());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_logout, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.logout) {
            ((JBossServerRootActivity) getActivity()).onBackPressed();
            return (true);
        }

        return (super.onOptionsItemSelected(item));
    }

    private void buildTable() {
        Map<String, List<String>> table = new HashMap<String, List<String>>();

        table.put("Server Status", Arrays.asList("Configuration", "JVM"));
        table.put("Subsystem Metrics", Arrays.asList("Data Sources", "JMS Destinations", "Transactions", "Web"));

        if (application.getOperationsManager().isDomainController()) {
            table.put("Deployments", Arrays.asList("Deployment Content", "Server Groups"));
        } else {
            table.put("Deployments", Arrays.asList("Manage Deployments"));
        }

        MergeAdapter adapter = new MergeAdapter();

        // cater for domain mode and display a button
        // for the user to change the active server
        if (application.getOperationsManager().isDomainController()) {
            chooseServer = new Button(getActivity());
            chooseServer.setText(application.getOperationsManager().getDomainHost() + ":" + application.getOperationsManager().getDomainServer());
            chooseServer.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_servers_selection, 0);
            chooseServer.setTypeface(null, Typeface.ITALIC);
            chooseServer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDomainHostChooser();
                }
            });

            adapter.addView(chooseServer);
        }

        for (Map.Entry<String, List<String>> entry : table.entrySet()) {
            // add section header
            TextView section = new TextView(getActivity());
            section.setBackgroundColor(Color.DKGRAY);
            section.setPadding(15, 10, 0, 10);
            section.setText(entry.getKey());
            adapter.addView(section);

            // add section data
            adapter.addAdapter(new ArrayAdapter<String>(
                    getActivity(),
                    android.R.layout.simple_list_item_1,
                    entry.getValue()));
        }

        setListAdapter(adapter);
    }

    @Override
    public void onListItemClick(ListView list, View view, int position, long id) {
        String value = (String) list.getItemAtPosition(position);

        Fragment fragment = null;

        if (value.equals("Configuration")) {
            fragment = new ConfigurationViewFragment();
        } else if (value.equals("JVM")) {
            fragment = new JVMMetricsViewFragment();
        } else if (value.equals("Data Sources")) {
            fragment = new DataSourcesViewFragment();
        } else if (value.equals("JMS Destinations")) {
            fragment = new JMSTypeSelectorViewFragment();
        } else if (value.equals("Transactions")) {
            fragment = new TransactionMetricsViewFragment();
        } else if (value.equals("Web")) {
            fragment = new WebConnectorTypeSelectorViewFragment();
        } else if (value.equals("Deployment Content")) {
            fragment = DeploymentsViewFragment.newInstance(null, DeploymentsViewFragment.Mode.DOMAIN_MODE);
        } else if (value.equals("Server Groups")) {
            fragment = new DomainServerGroupsViewFragment();
        } else if (value.equals("Manage Deployments")) {
            fragment = DeploymentsViewFragment.newInstance(null, DeploymentsViewFragment.Mode.STANDALONE_MODE);
        }

        ((JBossServerRootActivity) getActivity()).addFragment(fragment);
    }

    public void showDomainHostChooser() {
        ProgressDialogFragment.showDialog(getActivity(), R.string.queryingServer);

        application.getOperationsManager().fetchDomainHostInfoInformation(new Callback() {
            @Override
            public void onSuccess(JsonElement reply) {
                JsonArray jsonArr = reply.getAsJsonArray();

                // if only one host exists in the domain,
                // no need to display the host chooser dialog
                // immediately show the servers dialog for this host
                if (jsonArr.size() == 1) {
                    showDomainServersDialog(jsonArr.get(0).getAsString(), false);
                    return;
                }

                ProgressDialogFragment.dismissDialog(getActivity());

                final ArrayAdapter<String> hosts = new ArrayAdapter<String>(
                        getActivity(),
                        android.R.layout.simple_list_item_single_choice);

                // will hold the default selection host position
                // when the list is displayed
                int selPos = -1;

                String activeHost = application.getOperationsManager().getDomainHost();

                for (int i = 0; i < jsonArr.size(); i++) {
                    String host = jsonArr.get(i).getAsString();

                    if (host.equals(activeHost))
                        selPos = i;

                    hosts.add(host);
                }

                AlertDialog.Builder hostsDialog = new AlertDialog.Builder(
                        getActivity());

                hostsDialog.setTitle(R.string.select_host);
                hostsDialog.setSingleChoiceItems(hosts, selPos, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showDomainServersDialog(hosts.getItem(which), true);
                    }
                });

                hostsDialog.setPositiveButton(R.string.next, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        int selectedPosition = ((AlertDialog) dialogInterface).getListView().getCheckedItemPosition();

                        showDomainServersDialog(hosts.getItem(selectedPosition), true);
                    }
                });

                hostsDialog.setCancelable(false);

                ParameterizedDialogFragment dialog = new ParameterizedDialogFragment(hostsDialog);
                dialog.show(getActivity().getSupportFragmentManager(), null);
            }

            @Override
            public void onFailure(Exception e) {
                ProgressDialogFragment.dismissDialog(getActivity());

                ErrorDialogFragment.showDialog(getActivity(), e.getMessage());
            }
        });
    }

    public void showDomainServersDialog(final String host, boolean showProgress) {
        if (showProgress)
            ProgressDialogFragment.showDialog(getActivity(), R.string.queryingServer);

        application.getOperationsManager().fetchServersInformation(host, new Callback() {
            @Override
            public void onSuccess(JsonElement reply) {
                ProgressDialogFragment.dismissDialog(getActivity());

                hostAdapter = new HostAdapter();

                JsonObject jsonObj = reply.getAsJsonObject();

                for (Map.Entry<String, JsonElement> e : jsonObj.entrySet()) {
                    String name = e.getKey();

                    JsonObject info = e.getValue().getAsJsonObject();

                    HostStatus status = HostStatus.valueOf(info.get("status").getAsString());
                    String group = info.get("group").getAsString();

                    hostAdapter.add(new Server(name, host, group, status));
                }

                AlertDialog.Builder serversDialog = new AlertDialog.Builder(
                        getActivity());

                serversDialog.setTitle(R.string.select_server);
                serversDialog.setSingleChoiceItems(hostAdapter, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Server server = hostAdapter.getItem(which);

                        if (server.status == STARTED) {
                            // update operations manager
                            application.getOperationsManager().changeActiveMonitoringServer(host, server.name);

                            // update button view
                            chooseServer.setText(application.getOperationsManager().getDomainHost() + ":" + application.getOperationsManager().getDomainServer());

                            dialog.dismiss();
                        }
                    }
                });

                serversDialog.setCancelable(false);

                ParameterizedDialogFragment dialog = new ParameterizedDialogFragment(serversDialog);
                dialog.show(getActivity().getSupportFragmentManager(), null);
            }

            @Override
            public void onFailure(Exception e) {
                ProgressDialogFragment.dismissDialog(getActivity());

                ErrorDialogFragment.showDialog(getActivity(), e.getMessage());
            }
        });
    }

    public void toggleServerStatus(final Server server) {
        final String actionStr = (server.status == STARTED ? "Stop" : "Start");

        final boolean start = actionStr.equals("Start");

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                getActivity());

        alertDialog
                .setTitle(String.format(getString(R.string.dialog_confirm_action_title), actionStr))
                .setMessage(String.format(getString(R.string.dialog_confirm_action_body),
                        actionStr, server.name))

                .setNegativeButton(getString(R.string.dialog_button_NO),
                        null)
                .setPositiveButton(getString(R.string.dialog_button_YES),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int which) {

                                ProgressDialogFragment.showDialog(getActivity(), R.string.applyingAction);

                                application.getOperationsManager().changeStatus(server.belongingHost, server.name, start, new Callback() {
                                    @Override
                                    public void onSuccess(JsonElement reply) {
                                        ProgressDialogFragment.dismissDialog(getActivity());

                                        String status = reply.getAsString();

                                        boolean error = false;

                                        if (start && !status.equals("STARTED"))
                                            error = true;

                                        if (!start && !status.equals("STOPPED"))
                                            error = true;

                                        Toast.makeText(getActivity(), (error ?
                                                getString(R.string.failed)
                                                : getString(R.string.success)),
                                                Toast.LENGTH_SHORT)
                                                .show();

                                        // update model
                                        if (!error) {
                                            server.status = start ? STARTED : STOPPED;
                                        }

                                        // refresh view
                                        hostAdapter.notifyDataSetChanged();
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        ProgressDialogFragment.dismissDialog(getActivity());

                                        ErrorDialogFragment.showDialog(getActivity(), e.getMessage());
                                    }
                                });
                            }
                        });

        ParameterizedDialogFragment dialog = new ParameterizedDialogFragment(alertDialog);
        dialog.show(getActivity().getSupportFragmentManager(), null);
    }

    class HostAdapter extends ArrayAdapter<Server> {
        HostAdapter() {
            super(getActivity(), R.layout.server_row, R.id.server_name);
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

            // only enable toggleStatus for status's other than pending
            if (getItem(position).status != STARTING
                    && getItem(position).status != STOPPING) {
                holder.startStop.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toggleServerStatus((Server) v.getTag());
                    }
                });
            }
            return (row);
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            // disable item click if the server is not yet started
            return getItem(position).status == STARTED;
        }
    }

    class ViewHolder {
        ImageView status = null;
        TextView name = null;
        TextView group = null;
        ImageView startStop = null;

        ViewHolder(View row) {
            this.status = (ImageView) row.findViewById(R.id.server_status_icon);
            this.name = (TextView) row.findViewById(R.id.server_name);
            this.group = (TextView) row.findViewById(R.id.server_group_name);
            this.startStop = (ImageView) row.findViewById(R.id.server_start_stop);
        }

        void populateFrom(Server server) {
            name.setText(server.name);

            // handle text color becoming invisible in 'Gingerbread'
            // see https://code.google.com/p/android/issues/detail?id=922
            if (Build.VERSION.SDK_INT == 10) {
                name.setTextColor(Color.BLACK);
            }

            group.setText(server.group);

            switch (server.status) {
                case STARTED:
                    status.setImageResource(R.drawable.ic_element_up);
                    startStop.setImageResource(R.drawable.ic_stop);
                    startStop.setTag(server);
                    break;
                case DISABLED:
                case STOPPED:
                case FAILED:
                    status.setImageResource(R.drawable.ic_element_down);
                    startStop.setImageResource(R.drawable.ic_start);
                    startStop.setTag(server);
                    break;
                case STARTING:
                case STOPPING:
                    status.setImageResource(R.drawable.ic_element_down);
                    startStop.setImageResource(R.drawable.ic_clear);
                    startStop.setTag(null);
                    break;
            }
        }
    }

    class Server {
        String name;
        String group;
        String belongingHost;
        HostStatus status;

        Server(String name, String belongingHost, String group, HostStatus status) {
            this.name = name;
            this.belongingHost = belongingHost;
            this.group = group;
            this.status = status;
        }
    }
}
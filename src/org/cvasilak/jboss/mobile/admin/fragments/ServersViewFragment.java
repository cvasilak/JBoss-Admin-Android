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
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.gson.JsonElement;
import org.cvasilak.jboss.mobile.admin.JBossAdminApplication;
import org.cvasilak.jboss.mobile.admin.R;
import org.cvasilak.jboss.mobile.admin.activities.JBossServerRootActivity;
import org.cvasilak.jboss.mobile.admin.fragments.dialogs.ErrorDialogFragment;
import org.cvasilak.jboss.mobile.admin.fragments.dialogs.ParameterizedDialogFragment;
import org.cvasilak.jboss.mobile.admin.fragments.dialogs.ProgressDialogFragment;
import org.cvasilak.jboss.mobile.admin.model.Server;
import org.cvasilak.jboss.mobile.admin.model.ServersManager;
import org.cvasilak.jboss.mobile.admin.net.Callback;

import java.util.List;

public class ServersViewFragment extends SherlockListFragment {

    private static final String TAG = ServersViewFragment.class.getSimpleName();

    private JBossAdminApplication application;

    private ServersManager serversManager;

    private ServerAdapter adapter;

    private int selectedItemPos = -1;

    private ActionMode mActionMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        Log.d(TAG, "@onCreate()");

        application = ((JBossAdminApplication) getSherlockActivity().getApplication());
        serversManager = application.getServersManager();

        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        adapter = new ServerAdapter(serversManager.getServers());
        setListAdapter(adapter);

        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        getListView().setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                                           int position, long id) {

                if (mActionMode != null) {
                    return false;
                }

                selectedItemPos = position;
                getListView().setItemChecked(position, true);

                // Start the CAB using the ActionMode.Callback defined above
                mActionMode = getSherlockActivity().startActionMode(new ActionModeCallback());

                return true;
            }
        });

        getSherlockActivity().
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        getSherlockActivity().
                getSupportActionBar().setTitle("JBoss Admin");
    }

    @Override
    public void onResume() {
        super.onResume();

        adapter.notifyDataSetChanged();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_add, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.add) {

            showEditorForPosition(-1);

            return (true);
        }

        return (super.onOptionsItemSelected(item));
    }

    private final class ActionModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            getSherlockActivity().getSupportMenuInflater().inflate(R.menu.context_menu_servers_list,
                    menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.servers_context_edit:

                    showEditorForPosition(selectedItemPos);

                    mActionMode.finish();

                    return true;

                case R.id.servers_context_delete:
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                            getSherlockActivity());

                    alertDialog
                            .setTitle(String.format(getString(R.string.dialog_confirm_action_title), getString(R.string.action_delete)))
                            .setMessage(String.format(getString(R.string.dialog_confirm_action_body),
                                    getString(R.string.action_delete), serversManager.serverAtIndex(selectedItemPos).getName()))

                            .setIcon(R.drawable.ic_action_delete)
                            .setNegativeButton(getString(R.string.dialog_button_NO),
                                    null)
                            .setPositiveButton(getString(R.string.dialog_button_YES),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog,
                                                            int which) {
                                            // Delete the server that the context menu is for
                                            serversManager.removeServerAtIndex(selectedItemPos);
                                            try {
                                                serversManager.save();
                                            } catch (Exception e) {
                                                Log.d(TAG, "exception on save", e);

                                                ErrorDialogFragment.
                                                        showDialog(getSherlockActivity(), getString(R.string.error_on_save));
                                            }

                                            adapter.notifyDataSetChanged();

                                        }
                                    });

                    ParameterizedDialogFragment dialog = new ParameterizedDialogFragment(alertDialog);
                    dialog.show(getSherlockActivity().getSupportFragmentManager(), null);

                    mActionMode.finish();

                    return true;

                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
        }
    }

    @Override
    public void onListItemClick(ListView list, View view, int position, long id) {
        application.setCurrentActiveServer(serversManager.serverAtIndex(position));

        ProgressDialogFragment.showDialog(getSherlockActivity(), R.string.queryingServer);

        application.getOperationsManager().fetchJBossVersion(new Callback() {
            @Override
            public void onSuccess(JsonElement reply) {
                ProgressDialogFragment.dismissDialog(getSherlockActivity());

                Intent i = new Intent(getSherlockActivity(),
                        JBossServerRootActivity.class);

                startActivity(i);
            }

            @Override
            public void onFailure(Exception e) {
                ProgressDialogFragment.dismissDialog(getSherlockActivity());

                ErrorDialogFragment.showDialog(getSherlockActivity(), e.getMessage());
            }
        });
    }

    private void showEditorForPosition(int pos) {
        FragmentManager fragmentManager = getSherlockActivity().getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                .addToBackStack(null)
                .replace(android.R.id.content, ServerEditFragment.newInstance(pos))
                .commit();
    }

    class ServerAdapter extends ArrayAdapter<Server> {
        ServerAdapter(List<Server> servers) {
            super(getSherlockActivity(), R.layout.twoline_list_item, R.id.text1, servers);
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

        class ViewHolder {
            TextView text1;
            TextView text2;

            ViewHolder(View row) {
                this.text1 = (TextView) row.findViewById(R.id.text1);
                this.text2 = (TextView) row.findViewById(R.id.text2);
            }

            void populateFrom(Server server) {
                text1.setText(server.getName());
                text2.setText(server.getHostname() + ":" + server.getPort());
            }
        }
    }
}
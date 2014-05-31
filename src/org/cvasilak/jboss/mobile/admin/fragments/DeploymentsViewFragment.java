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
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.cvasilak.jboss.mobile.admin.JBossAdminApplication;
import org.cvasilak.jboss.mobile.admin.R;
import org.cvasilak.jboss.mobile.admin.fragments.dialogs.ErrorDialogFragment;
import org.cvasilak.jboss.mobile.admin.fragments.dialogs.InfoDialogFragment;
import org.cvasilak.jboss.mobile.admin.fragments.dialogs.ParameterizedDialogFragment;
import org.cvasilak.jboss.mobile.admin.fragments.dialogs.ProgressDialogFragment;
import org.cvasilak.jboss.mobile.admin.model.Deployment;
import org.cvasilak.jboss.mobile.admin.net.Callback;
import org.cvasilak.jboss.mobile.admin.service.UploadToJBossServerService;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DeploymentsViewFragment extends ListFragment {

    private static final String TAG = DeploymentsViewFragment.class.getSimpleName();

    private JBossAdminApplication application;

    private List<Deployment> deployments;

    private String group;

    private int selectedItemPos = -1;
    private ActionMode mActionMode;

    public static enum Mode {
        STANDALONE_MODE,
        DOMAIN_MODE,
        SERVER_MODE
    }

    private Mode mode;

    // variable used in AlertDialog to retain the selected Item Pos
    // so upon reorientation we reselect the item
    // Note: required because for some weird reason, setSingleChoiceItems() in AlertDialog
    // doesn't retain the selected pos. In contrast, setMultiChoiceItems() does (?)
    private static int selectedAlertDialogItemPos = -1;

    public static DeploymentsViewFragment newInstance(String group, Mode mode) {
        DeploymentsViewFragment f = new DeploymentsViewFragment();

        Bundle args = new Bundle();
        args.putString("group", group);
        args.putString("mode", mode.name());

        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        if (getArguments() != null) {
            this.group = getArguments().getString("group");
            this.mode = Mode.valueOf(getArguments().getString("mode"));
        }

        application = (JBossAdminApplication) getActivity().getApplication();

        deployments = new ArrayList<Deployment>();

        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Log.d(TAG, "@onActivityCreated()");

        ActionBar bar = ((ActionBarActivity) getActivity()).getSupportActionBar();

        bar.setTitle((mode == Mode.DOMAIN_MODE ?
                getString(R.string.repository) :
                getString(R.string.deployments)));

        // Define the contextual action mode
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                                           int position, long id) {

                if (mActionMode != null) {
                    return false;
                }

                selectedItemPos = position;
                getListView().setItemChecked(position, true);

                // Start the CAB using the ActionMode.Callback defined above
                mActionMode = ((ActionBarActivity) getActivity()).startSupportActionMode(new ActionModeCallback());

                return true;
            }
        });

        setListAdapter(new DeploymentAdapter(deployments));

        refresh();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_refresh_add, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.refresh) {
            refresh();

            return (true);

        } else if (item.getItemId() == R.id.add) {

            if (mode == Mode.SERVER_MODE)
                showRepositoryDeploymentsOptionsMenu();
            else if (mode == Mode.DOMAIN_MODE || mode == Mode.STANDALONE_MODE) {
                showLocalDeploymentsOptionsMenu();
            }

            return (true);
        }

        return (super.onOptionsItemSelected(item));
    }

    private final class ActionModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            getActivity().getMenuInflater().inflate(R.menu.context_menu_deployments_list,
                    menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mod, Menu menu) {
            MenuItem item = menu.findItem(R.id.deployments_context_action);

            Deployment deployment = deployments.get(selectedItemPos);

            if (mode == Mode.DOMAIN_MODE)
                item.setTitle(R.string.action_add_to_group);
            else
                item.setTitle(deployment.isEnabled() ? R.string.action_disable : R.string.action_enable);

            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mod, MenuItem item) {
            Deployment deployment = deployments.get(selectedItemPos);

            switch (item.getItemId()) {
                case R.id.deployments_context_delete:
                    deleteDeployment(deployment);

                    mActionMode.finish();
                    return true;

                case R.id.deployments_context_action:
                    if (mode == Mode.DOMAIN_MODE) { // Add Deployment to Group
                        addDeploymentToGroup(deployment);
                    } else {
                        toggleDeploymentStatus(deployment); // Enable/Disable Deployment
                    }

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

    public void showLocalDeploymentsOptionsMenu() {
        File root = application.getLocalDeploymentsDirectory();

        final String[] files = root.list();

        if (files == null || files.length == 0) { // if no deployments are present
            // inform user the path to where deployments should reside
            // for the app to pick them up
            InfoDialogFragment infoDialog = InfoDialogFragment.
                    newInstance(getString(R.string.directory_empty_title), String.format(getString(R.string.directory_empty_msg), root.getAbsolutePath()));
            infoDialog.show(getActivity().getSupportFragmentManager(), InfoDialogFragment.TAG);

            return;
        }

        // sort by filename
        Arrays.sort(files);

        selectedAlertDialogItemPos = -1; // reset any previous selected value

        // time to display the list of files
        AlertDialog.Builder filesDialog = new AlertDialog.Builder(
                getActivity());

        filesDialog.setTitle(R.string.upload_deployment_step1);

        filesDialog.setSingleChoiceItems(files, selectedAlertDialogItemPos, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                selectedAlertDialogItemPos = ((AlertDialog) dialogInterface).getListView().getCheckedItemPosition();
                // enable button if a deployment is clicked
                ((AlertDialog) dialogInterface).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            }
        });

        // Cancel Button
        filesDialog.setNegativeButton(R.string.cancel, null);

        // Upload Button
        filesDialog.setPositiveButton(R.string.action_upload, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                uploadDeployment(files[selectedAlertDialogItemPos]);
            }
        });
        filesDialog.setCancelable(true);

        ParameterizedDialogFragment dialog = new ParameterizedDialogFragment(filesDialog, new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                ((AlertDialog) dialogInterface).getListView().setItemChecked(selectedAlertDialogItemPos, true);

                ((AlertDialog) dialogInterface).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(selectedAlertDialogItemPos != -1);
            }
        });

        dialog.show(getActivity().getSupportFragmentManager(), null);
    }

    public void showRepositoryDeploymentsOptionsMenu() {
        ProgressDialogFragment.showDialog(getActivity(), R.string.queryingServer);

        application.getOperationsManager().fetchDeployments(null, new Callback() {
            @Override
            public void onSuccess(JsonElement reply) {
                ProgressDialogFragment.dismissDialog(getActivity());

                String name, runtimeName, BYTES_VALUE;
                boolean enabled = false;

                JsonObject jsonObj = reply.getAsJsonObject();

                final ArrayAdapter<Deployment> repoAdapter = new ArrayAdapter<Deployment>(
                        getActivity(),
                        android.R.layout.select_dialog_singlechoice);

                for (Map.Entry<String, JsonElement> e : jsonObj.entrySet()) {
                    name = e.getKey();

                    JsonObject detailsJsonObj = e.getValue().getAsJsonObject();

                    if (detailsJsonObj.get("enabled") != null)
                        enabled = detailsJsonObj.get("enabled").getAsBoolean();

                    runtimeName = detailsJsonObj.get("runtime-name").getAsString();

                    // "content" : [{"hash" : { "BYTES_VALUE" : "Pb4xyzgJmsxruKEf5eGOLu6lBjw="}}],
                    BYTES_VALUE = detailsJsonObj.get("content").getAsJsonArray().get(0).getAsJsonObject().
                            get("hash").getAsJsonObject().
                            get("BYTES_VALUE").getAsString();

                    repoAdapter.add(new Deployment(name, runtimeName, enabled, BYTES_VALUE));
                }

                selectedAlertDialogItemPos = -1; // reset any previous selected value

                // time to display content repository
                AlertDialog.Builder filesDialog = new AlertDialog.Builder(
                        getActivity());

                filesDialog.setTitle(R.string.add_deployment_from_repository);
                filesDialog.setSingleChoiceItems(repoAdapter, selectedAlertDialogItemPos, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        selectedAlertDialogItemPos = ((AlertDialog) dialogInterface).getListView().getCheckedItemPosition();

                        // enable buttons if a deployment is clicked
                        ((AlertDialog) dialogInterface).getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(true);
                        ((AlertDialog) dialogInterface).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    }
                });

                // Cancel Button
                filesDialog.setNegativeButton(R.string.cancel, null);

                // Add to Group Button
                filesDialog.setNeutralButton(R.string.add_to_group, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        addDeployment(repoAdapter.getItem(selectedAlertDialogItemPos), false, Arrays.asList(group));
                    }
                });

                // Add to Group and Enable Button
                filesDialog.setPositiveButton(R.string.add_to_group_enable, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        addDeployment(repoAdapter.getItem(selectedAlertDialogItemPos), true, Arrays.asList(group));
                    }
                });
                filesDialog.setCancelable(true);

                ParameterizedDialogFragment dialog = new ParameterizedDialogFragment(filesDialog, new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialogInterface) {
                        ((AlertDialog) dialogInterface).getListView().setItemChecked(selectedAlertDialogItemPos, true);

                        ((AlertDialog) dialogInterface).getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(selectedAlertDialogItemPos != -1);
                        ((AlertDialog) dialogInterface).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(selectedAlertDialogItemPos != -1);
                    }
                });

                dialog.show(getActivity().getSupportFragmentManager(), null);
            }

            @Override
            public void onFailure(Exception e) {
                ProgressDialogFragment.dismissDialog(getActivity());

                ErrorDialogFragment.showDialog(getActivity(), e.getMessage());
            }
        });
    }

    public void addDeploymentToGroup(final Deployment deployment) {
        ProgressDialogFragment.showDialog(getActivity(), R.string.queryingServer);

        application.getOperationsManager().fetchDomainGroups(new Callback() {
            @Override
            public void onSuccess(JsonElement reply) {
                ProgressDialogFragment.dismissDialog(getActivity());

                JsonObject jsonObj = reply.getAsJsonObject();

                final List<String> groups = new ArrayList<String>();
                for (Map.Entry<String, JsonElement> e : jsonObj.entrySet()) {
                    groups.add(e.getKey());
                }

                // time to display the list
                AlertDialog.Builder groupsDialog = new AlertDialog.Builder(
                        getActivity());

                groupsDialog.setTitle(R.string.add_deployment_to_group);
                groupsDialog.setMultiChoiceItems(groups.toArray(new String[groups.size()]), new boolean[groups.size()], new DialogInterface.OnMultiChoiceClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                        int selected = getSelectedItems(dialogInterface).size();

                        // disable or enable buttons depending on the
                        // number of items checked.
                        ((AlertDialog) dialogInterface).getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(selected != 0);
                        ((AlertDialog) dialogInterface).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(selected != 0);
                    }

                });

                // Cancel Button
                groupsDialog.setNegativeButton(R.string.cancel, null);

                // Add to Group Button
                groupsDialog.setNeutralButton(R.string.add_to_group, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int k) {
                        addDeployment(deployment, false, getSelectedItems(dialogInterface));
                    }
                });

                // Add to Group and Enable Button
                groupsDialog.setPositiveButton(R.string.add_to_group_enable, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        addDeployment(deployment, true, getSelectedItems(dialogInterface));
                    }
                });
                groupsDialog.setCancelable(true);

                ParameterizedDialogFragment dialog = new ParameterizedDialogFragment(groupsDialog, new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialogInterface) {
                        int selected = getSelectedItems(dialogInterface).size();

                        // disable or enable buttons depending on the
                        // number of items checked.
                        ((AlertDialog) dialogInterface).getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(selected != 0);
                        ((AlertDialog) dialogInterface).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(selected != 0);
                    }
                });

                dialog.show(getActivity().getSupportFragmentManager(), null);
            }

            @Override
            public void onFailure(Exception e) {
                ProgressDialogFragment.dismissDialog(getActivity());

                ErrorDialogFragment.showDialog(getActivity(), e.getMessage());
            }
        });
    }

    public void uploadDeployment(final String filename) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                getActivity());

        alertDialog
                .setTitle(String.format(getString(R.string.dialog_confirm_action_title), getString(R.string.action_upload)))
                .setMessage(String.format(getString(R.string.dialog_confirm_action_body),
                        getString(R.string.action_upload), filename))
                .setNegativeButton(getString(R.string.dialog_button_NO),
                        null)
                .setPositiveButton(getString(R.string.dialog_button_YES),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int which) {

                                Intent i = new Intent(getActivity(), UploadToJBossServerService.class);

                                // put the details for the service to pickup
                                i.putExtra("server", application.getOperationsManager().getServer());
                                i.putExtra("directory", application.getLocalDeploymentsDirectory().toString());
                                i.putExtra("filename", filename);

                                // start service to upload file
                                getActivity().startService(i);

                                Toast.makeText(getActivity(), getString(R.string.deployment_schedule_to_upload), Toast.LENGTH_SHORT).show();
                            }
                        });

        ParameterizedDialogFragment dialog = new ParameterizedDialogFragment(alertDialog);
        dialog.show(getActivity().getSupportFragmentManager(), null);
    }

    public void addDeployment(final Deployment deployment, final boolean enable, final List<String> groups) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                getActivity());

        alertDialog
                .setTitle(String.format(getString(R.string.dialog_confirm_action_title), ""))
                .setMessage(String.format(getString(R.string.dialog_confirm_action_body),
                        enable ? getString(R.string.add_to_group_enable) : getString(R.string.add_to_group), ""))
                .setNegativeButton(getString(R.string.dialog_button_NO),
                        null)
                .setPositiveButton(getString(R.string.dialog_button_YES),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int which) {

                                ProgressDialogFragment.showDialog(getActivity(), R.string.applyingAction);

                                application.getOperationsManager().addDeploymentContent(deployment.getBYTES_VALUE(), deployment.getName(), groups, enable, new Callback() {
                                    @Override
                                    public void onSuccess(JsonElement reply) {
                                        ProgressDialogFragment.dismissDialog(getActivity());

                                        Toast.makeText(getActivity(), getString(R.string.deployment_added), Toast.LENGTH_SHORT).show();

                                        // reflect enable in our model
                                        deployment.setEnabled(enable);

                                        // add to list
                                        if (mode != Mode.DOMAIN_MODE)
                                            deployments.add(deployment);

                                        ((DeploymentAdapter) getListAdapter()).notifyDataSetChanged();
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

    public void toggleDeploymentStatus(final Deployment deployment) {
        String action = deployment.isEnabled() ? getString(R.string.action_disable) : getString(R.string.action_enable);

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                getActivity());

        alertDialog
                .setTitle(String.format(getString(R.string.dialog_confirm_action_title), action))
                .setMessage(String.format(getString(R.string.dialog_confirm_action_body),
                        action, deployment.getName()))

                .setNegativeButton(getString(R.string.dialog_button_NO),
                        null)
                .setPositiveButton(getString(R.string.dialog_button_YES),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                ProgressDialogFragment.showDialog(getActivity(), R.string.applyingAction);

                                application.getOperationsManager().changeDeploymentStatus(deployment.getName(), group, !deployment.isEnabled(), new Callback() {
                                    @Override
                                    public void onSuccess(JsonElement reply) {
                                        ProgressDialogFragment.dismissDialog(getActivity());

                                        deployment.setEnabled(!deployment.isEnabled());

                                        ((DeploymentAdapter) getListAdapter()).notifyDataSetChanged();

                                        Toast.makeText(getActivity(), getString(deployment.isEnabled() ?
                                                R.string.deployment_enabled :
                                                R.string.deployment_disabled), Toast.LENGTH_SHORT).show();
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

    public void deleteDeployment(final Deployment deployment) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                getActivity());

        alertDialog
                .setTitle(String.format(getString(R.string.dialog_confirm_action_title), getString(R.string.action_delete)))
                .setMessage(String.format(getString(R.string.dialog_confirm_action_body),
                        getString(R.string.action_delete), deployment.getName()))
                .setIcon(R.drawable.ic_action_delete)
                .setNegativeButton(getString(R.string.dialog_button_NO),
                        null)
                .setPositiveButton(getString(R.string.dialog_button_YES),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                ProgressDialogFragment.showDialog(getActivity(), R.string.queryingServer);

                                application.getOperationsManager().removeDeployment(deployment.getName(), group, new Callback() {
                                    @Override
                                    public void onSuccess(JsonElement reply) {
                                        ProgressDialogFragment.dismissDialog(getActivity());

                                        deployments.remove(deployment);

                                        Toast.makeText(getActivity(), getString(R.string.deployment_deleted), Toast.LENGTH_SHORT).show();

                                        ((DeploymentAdapter) getListAdapter()).notifyDataSetChanged();
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

    public void refresh() {
        ProgressDialogFragment.showDialog(getActivity(), R.string.queryingServer);

        application.getOperationsManager().fetchDeployments(group, new Callback() {
            @Override
            public void onSuccess(JsonElement reply) {
                ProgressDialogFragment.dismissDialog(getActivity());

                deployments.clear();

                String name, runtimeName;
                boolean enabled = false;

                JsonObject jsonObj = reply.getAsJsonObject();

                for (Map.Entry<String, JsonElement> e : jsonObj.entrySet()) {
                    name = e.getKey();

                    JsonObject detailsJsonObj = e.getValue().getAsJsonObject();

                    if (detailsJsonObj.get("enabled") != null)
                        enabled = detailsJsonObj.get("enabled").getAsBoolean();

                    runtimeName = detailsJsonObj.get("runtime-name").getAsString();

                    deployments.add(new Deployment(name, runtimeName, enabled, null /* BYTES_VALUE is null */));
                }

                ((DeploymentAdapter) getListAdapter()).notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception e) {
                ProgressDialogFragment.dismissDialog(getActivity());

                ErrorDialogFragment.showDialog(getActivity(), e.getMessage());
            }
        });
    }

    //@Override
    public void onDeploymentAdded(String name, String runtimeName, String key) {
        deployments.add(new Deployment(name, runtimeName, false, key));

        ((DeploymentAdapter) getListAdapter()).notifyDataSetChanged();
    }


    // util method to get the selections from a listview
    public List<String> getSelectedItems(DialogInterface dialogInterface) {
        ListView list = ((AlertDialog) dialogInterface).getListView();

        List<String> selectedGroups = new ArrayList<String>();
        for (int i = 0; i < list.getCount(); i++) {
            if (list.isItemChecked(i))
                selectedGroups.add((String) list.getItemAtPosition(i));
        }

        return selectedGroups;
    }

    class DeploymentAdapter extends ArrayAdapter<Deployment> {
        DeploymentAdapter(List<Deployment> list) {
            super(getActivity(), R.layout.deployment_row, R.id.deployment_name, list);
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

    class ViewHolder {
        ImageView icon = null;
        TextView runtimeName = null;

        ViewHolder(View row) {
            this.icon = (ImageView) row.findViewById(R.id.deployment_icon);
            this.runtimeName = (TextView) row.findViewById(R.id.deployment_runtime_name);
        }

        void populateFrom(Deployment deployment) {
            // set runtime-name only if it differs from name
            if (!deployment.getName().equals(deployment.getRuntimeName())) {
                runtimeName.setText(deployment.getRuntimeName());
            } else {
                runtimeName.setText("");
            }

            // on domain mode, we only display the content
            // repository "status" icon is unusable
            if (mode == Mode.STANDALONE_MODE || mode == Mode.SERVER_MODE) {
                // check and set correct icon for the deployment
                icon.setImageResource((deployment.isEnabled() ?
                        R.drawable.ic_element_up :
                        R.drawable.ic_element_down));
            }
        }
    }
}
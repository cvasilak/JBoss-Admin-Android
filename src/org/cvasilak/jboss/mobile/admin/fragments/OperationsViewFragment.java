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

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.cvasilak.jboss.mobile.admin.JBossAdminApplication;
import org.cvasilak.jboss.mobile.admin.R;
import org.cvasilak.jboss.mobile.admin.activities.JBossServerRootActivity;
import org.cvasilak.jboss.mobile.admin.fragments.dialogs.ErrorDialogFragment;
import org.cvasilak.jboss.mobile.admin.fragments.dialogs.ProgressDialogFragment;
import org.cvasilak.jboss.mobile.admin.model.ManagementModelBase;
import org.cvasilak.jboss.mobile.admin.model.Operation;
import org.cvasilak.jboss.mobile.admin.model.OperationParameter;
import org.cvasilak.jboss.mobile.admin.net.Callback;
import org.cvasilak.jboss.mobile.admin.util.ParametersMap;

import java.util.*;

public class OperationsViewFragment extends ListFragment {

    private static final String TAG = OperationsViewFragment.class.getSimpleName();

    private JBossAdminApplication application;

    private ArrayList<String> path;

    private OperationAdapter adapter;

    private static final List<String> GENERIC_OPS =
            Arrays.asList("add", "read-operation-description", "read-resource-description", "read-operation-names");

    public static OperationsViewFragment newInstance(ArrayList<String> path) {
        OperationsViewFragment f = new OperationsViewFragment();

        Bundle args = new Bundle();
        args.putStringArrayList("path", path);

        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "@onCreate()");

        setRetainInstance(true);

        application = (JBossAdminApplication) getActivity().getApplication();

        if (getArguments() != null) {
            this.path = getArguments().getStringArrayList("path");
        }

        adapter = new OperationAdapter(getActivity());

        setListAdapter(adapter);

        refresh();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // adjust the title
        ActionBar bar = ((ActionBarActivity) getActivity()).getSupportActionBar();

        boolean filterGeneric = (path != null && path.get(path.size() - 1).equals("*"));

        if (filterGeneric)
            bar.setTitle(getString(R.string.generic_operations));
        else
            bar.setTitle(getString(R.string.operations));
    }

    @Override
    public void onListItemClick(ListView list, View view, int position, long id) {
        final Operation operation = adapter.getItem(position);

        if (operation.getDescr() == null) {
            // first time clicked, need to retrieve operation info

            ProgressDialogFragment.showDialog(getActivity(), R.string.queryingServer);

            ParametersMap params = ParametersMap.newMap()
                    .add("operation", "read-operation-description")
                    .add("address", (path == null ? Arrays.asList("/") : this.path))
                    .add("name", operation.getName());

            application.getOperationsManager().genericRequest(params, true, new Callback() {
                @Override
                public void onSuccess(JsonElement reply) {
                    ProgressDialogFragment.dismissDialog(getActivity());

                    JsonObject jsonObj = reply.getAsJsonObject();

                    operation.setDescr(jsonObj.get("description").getAsString());

                    List<OperationParameter> params = new ArrayList<OperationParameter>();

                    // if the operation has no parameters
                    // simple set an empty list and display
                    // the operation execute dialog.
                    // Note: JBoss EAP returns the key even if empty
                    //       while JBoss 7.x does not thus the extra
                    //       handling.
                    if (!jsonObj.has("request-properties")) {
                        // set an empty list
                        operation.setParameters(params);
                        showFragment(OperationExecViewFragment.newInstance(operation));

                        return;
                    }

                    JsonObject reqParams = jsonObj.get("request-properties").getAsJsonObject();

                    // Initialize operation parameters
                    // see: https://docs.jboss.org/author/display/AS71/Description+of+the+Management+Model
                    for (Map.Entry<String, JsonElement> elem : reqParams.entrySet()) {

                        OperationParameter param = new OperationParameter();

                        param.setName(elem.getKey());

                        JsonObject jsonElem = elem.getValue().getAsJsonObject();

                        param.setTypeFromString(jsonElem.getAsJsonObject("type").getAsJsonPrimitive("TYPE_MODEL_VALUE")
                                .getAsString());

                        // for LIST type extract the type of object the list holds
                        if (param.getType() == ManagementModelBase.Type.LIST) {
                            // TODO: currently we only support single type LISTS
                            // not a collection of various objects (see 'add' deployment)
                            JsonElement typeModelValue = jsonElem.getAsJsonObject("value-type").getAsJsonPrimitive("TYPE_MODEL_VALUE");

                            if (typeModelValue != null)
                                param.setValueTypeFromString(typeModelValue.getAsString());
                        }

                        param.setDescr(jsonElem.get("description").getAsString());

                        // true if null is a valid value. If not present, false is the default.
                        param.setNillable(jsonElem.get("nillable") != null && jsonElem.get("nillable").getAsBoolean());

                        // required – boolean
                        // Only relevant to parameters. true if the parameter must be present in the request object used to invoke
                        // the operation; false if it can omitted. If not present, true is the default
                        param.setRequired(jsonElem.get("required") != null && jsonElem.get("required").getAsBoolean());

                        param.setDefaultValue(jsonElem.get("default"));

                        // default false for boolean values when defaultValue is nil
                        if (param.getType() == ManagementModelBase.Type.BOOLEAN) {
                            if (param.getDefaultValue() == null) {
                                param.setValue(Boolean.FALSE);
                            } else {
                                param.setValue(param.getDefaultValue());
                            }
                        } else {
                            param.setValue(param.getDefaultValue());
                        }

                        params.add(param);
                    }

                    // sort by name
                    Collections.sort(params);
                    // 'required' attributes on top
                    Collections.sort(params, OperationParameter.REQUIRED_COMPARATOR);

                    // for "add" operation insert a fake parameter that denotes the resource path
                    if (operation.getName().equals("add")) {
                        OperationParameter param = new OperationParameter();

                        // extract the child type from path (e.g. "/deployment=" )
                        String basePath = path.get(path.size() - 2);

                        param.setName(String.format("%s=<name>/", basePath));
                        param.setType(ManagementModelBase.Type.STRING);
                        param.setDescr(String.format("Resource name for the new %s", basePath));
                        param.setRequired(true);
                        param.setAddParameter(true);
                        param.setDefaultValue(null);

                        // insert it at the top of the list
                        params.add(0, param);
                    }

                    operation.setParameters(params);

                    showFragment(OperationExecViewFragment.newInstance(operation));
                }

                @Override
                public void onFailure(Exception e) {
                    ProgressDialogFragment.dismissDialog(getActivity());

                    ErrorDialogFragment.showDialog(getActivity(), e.getMessage());
                }
            }
            );
        } else {
            showFragment(OperationExecViewFragment.newInstance(operation));
        }
    }

    public void refresh() {
        ProgressDialogFragment.showDialog(getActivity(), R.string.queryingServer);

        ParametersMap childParams = ParametersMap.newMap()
                .add("operation", "read-operation-names")
                .add("address", this.path == null ? Arrays.asList("/") : this.path);

        application.getOperationsManager().genericRequest(childParams, true, new Callback() {
            @Override
            public void onSuccess(JsonElement reply) {
                ProgressDialogFragment.dismissDialog(getActivity());

                // Check if user requested to see generic operations.
                // If so raise a flag so the list of operation names received from
                // the server is filtered by generic op names
                boolean filterGeneric = (path != null && path.get(path.size() - 1).equals("*"));

                for (JsonElement elem : reply.getAsJsonArray()) {
                    String name = elem.getAsString();

                    if (filterGeneric && !GENERIC_OPS.contains(name))
                        continue;

                    if (!filterGeneric && name.equals("add"))
                        continue;

                    // ok add the operation to the list
                    Operation oper = new Operation();
                    oper.setName(name);
                    oper.setPath(OperationsViewFragment.this.path);

                    adapter.add(oper);
                }
            }

            @Override
            public void onFailure(Exception e) {
                ProgressDialogFragment.dismissDialog(getActivity());

                ErrorDialogFragment.showDialog(getActivity(), e.getMessage());
            }
        }
        );
    }

    private void showFragment(Fragment fragment) {
        ((JBossServerRootActivity) getActivity()).addFragment(fragment);
    }

    class OperationAdapter extends ArrayAdapter<Operation> {
        public OperationAdapter(Context context) {
            super(context, R.layout.icon_text_row, R.id.row_name);
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

        void populateFrom(Operation operation) {
            name.setText(operation.getName());
            icon.setImageResource(R.drawable.ic_operations);
        }
    }
}
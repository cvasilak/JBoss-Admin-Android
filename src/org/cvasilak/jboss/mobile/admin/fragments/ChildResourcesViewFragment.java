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
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.google.gson.JsonElement;
import org.cvasilak.jboss.mobile.admin.JBossAdminApplication;
import org.cvasilak.jboss.mobile.admin.R;
import org.cvasilak.jboss.mobile.admin.activities.JBossServerRootActivity;
import org.cvasilak.jboss.mobile.admin.net.Callback;
import org.cvasilak.jboss.mobile.admin.util.ParametersMap;
import org.cvasilak.jboss.mobile.admin.util.commonsware.MergeAdapter;
import org.cvasilak.jboss.mobile.admin.util.listview.adapters.IconTextRowAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChildResourcesViewFragment extends ListFragment {

    private static final String TAG = ChildResourcesViewFragment.class.getSimpleName();

    private JBossAdminApplication application;

    private ArrayList<String> path;
    private String childTypeName;

    private List<String> childResources;

    public static ChildResourcesViewFragment newInstance(ArrayList<String> path, String childTypeName) {
        ChildResourcesViewFragment f = new ChildResourcesViewFragment();

        Bundle args = new Bundle();
        args.putStringArrayList("path", path);
        args.putString("childTypeName", childTypeName);

        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "@onCreate()");

        setRetainInstance(true);

        application = (JBossAdminApplication) getActivity().getApplication();

        childResources = new ArrayList<String>();

        if (getArguments() != null) {
            this.path = getArguments().getStringArrayList("path");
            this.childTypeName = getArguments().getString("childTypeName");
        }

        // inform runtime that we have an action button (refresh)
        setHasOptionsMenu(true);

        refresh();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ActionBar bar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        bar.setTitle(childTypeName);
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
        String selection = (String) list.getAdapter().getItem(position);

        if (selection.equals("<undefined>"))
            return;

        Fragment fragment;

        ArrayList<String> next = new ArrayList<String>();

        if (this.path != null)
            next.addAll(this.path);

        next.add(childTypeName);

        if (selection.equals(getString(R.string.generic_operations))) {
            next.add("*");

            fragment = OperationsViewFragment.newInstance(next);
        } else {
            next.add(childResources.get(position - 1));

            fragment = ProfileViewFragment.newInstance(next);
        }

        ((JBossServerRootActivity) getActivity()).addFragment(fragment);
    }

    public void refresh() {
        // will identify the child resources of this child type
        // Note: we reload here in case an "add" happened down the road
        ParametersMap childParams = ParametersMap.newMap()
                .add("operation", "read-children-names")
                .add("address", this.path == null ? Arrays.asList("/") : this.path)
                .add("child-type", this.childTypeName);

        application.getOperationsManager().genericRequest(childParams, true, new Callback() {
            @Override
            public void onSuccess(JsonElement reply) {
                childResources = new ArrayList<String>();

                for (JsonElement elem : reply.getAsJsonArray()) {
                    childResources.add(elem.getAsString());
                }

                // append child-type name and the star for "read-operation-names"
                List<String> genericOpsPath = new ArrayList<String>();

                if (path != null)
                    genericOpsPath.addAll(path);

                genericOpsPath.add(childTypeName);
                genericOpsPath.add("*");

                ParametersMap operationsParams = ParametersMap.newMap()
                        .add("operation", "read-operation-names")
                        .add("address", genericOpsPath);

                application.getOperationsManager().genericRequest(operationsParams, true, new Callback() {
                    @Override
                    public void onSuccess(JsonElement reply) {
                        // operations found, check if they contain
                        // generic add
                        boolean hasGenericOps = false;

                        for (JsonElement elem : reply.getAsJsonArray()) {
                            if (elem.getAsString().equals("add")) {
                                hasGenericOps = true;
                                break;
                            }
                        }

                        // if no generic operations found and children resources
                        // are empty inform as "undefined"
                        if (childResources.size() == 0 && !hasGenericOps) {
                            childResources.add("<undefined>");
                        }

                        buildTable(hasGenericOps);
                    }

                    @Override
                    public void onFailure(Exception e) {

                        buildTable(false);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                        getActivity());

                alertDialog
                        .setTitle(R.string.dialog_error_title)
                        .setMessage(e.getMessage())
                        .setPositiveButton(R.string.dialog_button_Bummer, null)
                        .setCancelable(false)
                        .setIcon(android.R.drawable.ic_dialog_alert).show();

            }
        });
    }

    private void buildTable(boolean hasGenericOps) {
        MergeAdapter adapter = new MergeAdapter();

        TextView sectionHeader;

        if (childResources.size() != 0) {
            // Section: Child Resources
            sectionHeader = new TextView(getActivity());
            sectionHeader.setBackgroundColor(Color.DKGRAY);
            sectionHeader.setPadding(15, 10, 0, 10);
            sectionHeader.setText(R.string.child_resources);
            adapter.addView(sectionHeader);

            adapter.addAdapter(new ArrayAdapter<String>(
                    getActivity(),
                    android.R.layout.simple_list_item_2, android.R.id.text1, childResources));
        }

        if (hasGenericOps) {
            if (childResources.size() != 0) { // if the are previous items, add the splitter
                sectionHeader = new TextView(getActivity());
                sectionHeader.setBackgroundColor(Color.DKGRAY);
                sectionHeader.setHeight(20);
                sectionHeader.setPadding(15, 10, 0, 10);
                adapter.addView(sectionHeader);
            }

            adapter.addAdapter(new IconTextRowAdapter(getActivity(), Arrays.asList(getString(R.string.generic_operations)), R.drawable.ic_operations));
        }

        setListAdapter(adapter);
    }
}
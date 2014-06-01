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

import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.cvasilak.jboss.mobile.app.JBossAdminApplication;
import org.cvasilak.jboss.mobile.app.R;
import org.cvasilak.jboss.mobile.app.fragments.dialogs.ErrorDialogFragment;
import org.cvasilak.jboss.mobile.app.fragments.dialogs.ProgressDialogFragment;
import org.cvasilak.jboss.mobile.app.net.Callback;

import java.util.ArrayList;
import java.util.List;

public class ExtensionsViewFragment extends ListFragment {

    private static final String TAG = ExtensionsViewFragment.class.getSimpleName();

    private JBossAdminApplication application;

    private List<String> extensions;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        application = (JBossAdminApplication) getActivity().getApplication();

        extensions = new ArrayList<String>();

        setHasOptionsMenu(true);

        refresh();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ActionBar bar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        bar.setTitle(getString(R.string.extensions));

        setListAdapter(new ArrayAdapter<String>(
                getActivity(),
                android.R.layout.simple_list_item_1, extensions));
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

        application.getOperationsManager().fetchExtensionsInformation(new Callback() {
            @Override
            public void onSuccess(JsonElement reply) {
                ProgressDialogFragment.dismissDialog(getActivity());

                extensions.clear();

                JsonArray jsonArray = reply.getAsJsonArray();

                for (JsonElement entry : jsonArray) {
                    extensions.add(entry.getAsString());
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
}
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
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import org.cvasilak.jboss.mobile.app.R;
import org.cvasilak.jboss.mobile.app.activities.JBossServerRootActivity;

public class JMSTypeSelectorViewFragment extends ListFragment {

    private static final String TAG = JMSTypeSelectorViewFragment.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ActionBar bar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        bar.setTitle(getString(R.string.jms_destinations));

        setListAdapter(new ArrayAdapter<String>(
                getActivity(),
                android.R.layout.simple_list_item_1, new String[]{"Queues", "Topics"}));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_refresh, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onListItemClick(ListView list, View view, int position, long id) {
        String value = (String) list.getItemAtPosition(position);

        Fragment fragment = null;

        if (value.equals("Queues")) {
            fragment = new JMSQueuesViewFragment();
        } else if (value.equals("Topics")) {
            fragment = new JMSTopicsViewFragment();
        }

        ((JBossServerRootActivity) getActivity()).addFragment(fragment);
    }
}
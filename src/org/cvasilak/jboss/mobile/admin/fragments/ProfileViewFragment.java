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
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.cvasilak.jboss.mobile.admin.JBossAdminApplication;
import org.cvasilak.jboss.mobile.admin.R;
import org.cvasilak.jboss.mobile.admin.activities.JBossServerRootActivity;
import org.cvasilak.jboss.mobile.admin.fragments.dialogs.ErrorDialogFragment;
import org.cvasilak.jboss.mobile.admin.fragments.dialogs.ProgressDialogFragment;
import org.cvasilak.jboss.mobile.admin.model.Attribute;
import org.cvasilak.jboss.mobile.admin.model.ChildType;
import org.cvasilak.jboss.mobile.admin.model.ManagementModelBase;
import org.cvasilak.jboss.mobile.admin.net.Callback;
import org.cvasilak.jboss.mobile.admin.util.ParametersMap;
import org.cvasilak.jboss.mobile.admin.util.commonsware.MergeAdapter;
import org.cvasilak.jboss.mobile.admin.util.listview.adapters.IconTextRowAdapter;

import java.util.*;

public class ProfileViewFragment extends SherlockListFragment {

    private static final String TAG = ProfileViewFragment.class.getSimpleName();

    private JBossAdminApplication application;

    private ArrayList<Attribute> attributes;
    private ArrayList<ChildType> childTypes;

    private ArrayList<String> path;

    public static ProfileViewFragment newInstance(ArrayList<String> path) {
        ProfileViewFragment f = new ProfileViewFragment();

        Bundle args = new Bundle();
        args.putStringArrayList("path", path);

        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Log.d(TAG, "@onCreate()");

        setRetainInstance(true);

        application = (JBossAdminApplication) getSherlockActivity().getApplication();

        if (getArguments() != null) {
            this.path = getArguments().getStringArrayList("path");
        }

        attributes = new ArrayList<Attribute>();
        childTypes = new ArrayList<ChildType>();

        // inform runtime that we have action buttons
        setHasOptionsMenu(true);

        refresh();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // adjust the title
        ActionBar bar = getSherlockActivity().getSupportActionBar();

        if (path == null)
            bar.setTitle(application.getOperationsManager().
                    getServer().getName());
        else
            bar.setTitle(path.get(path.size() - 1));

        MergeAdapter adapter = new MergeAdapter();

        TextView sectionHeader;

        // Section: Attributes
        sectionHeader = new TextView(getSherlockActivity());
        sectionHeader.setBackgroundColor(Color.DKGRAY);
        sectionHeader.setPadding(15, 10, 0, 10);
        sectionHeader.setText(R.string.attributes);
        adapter.addView(sectionHeader);

        adapter.addAdapter(new AttributeAdapter(attributes));

        // Section ChildTypes
        sectionHeader = new TextView(getSherlockActivity());
        sectionHeader.setBackgroundColor(Color.DKGRAY);
        sectionHeader.setPadding(15, 10, 0, 10);
        sectionHeader.setText(R.string.child_types);
        adapter.addView(sectionHeader);

        adapter.addAdapter(new ChildTypeAdapter(childTypes));

        // Section Operations
        sectionHeader = new TextView(getSherlockActivity());
        sectionHeader.setBackgroundColor(Color.DKGRAY);
        sectionHeader.setHeight(20);
        sectionHeader.setPadding(15, 10, 0, 10);
        adapter.addView(sectionHeader);

        adapter.addAdapter(new IconTextRowAdapter(getSherlockActivity(), Arrays.asList(getString(R.string.operations)), R.drawable.ic_operations));

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

    @Override
    public void onListItemClick(ListView list, View view, int position, long id) {
        if (position - 1 <= attributes.size()) { // Attribute selection
            showAttributeEditor(attributes.get(position - 1));

        } else if (position - 2 <= (attributes.size() + childTypes.size())) { // ChildType selection
            showChildType(childTypes.get(position - 2 - attributes.size()));

        } else { // Operation selection
            showOperations();
        }
    }

    public void refresh() {
        ProgressDialogFragment.showDialog(getSherlockActivity(), R.string.queryingServer);

        ParametersMap step1 = ParametersMap.newMap()
                .add("operation", "read-resource")
                .add("address", (path == null ? Arrays.asList("/") : this.path));

        ParametersMap step2 = ParametersMap.newMap()
                .add("operation", "read-children-types")
                .add("address", (path == null ? Arrays.asList("/") : this.path));

        ParametersMap params = ParametersMap.newMap()
                .add("operation", "composite")
                .add("steps", Arrays.asList(step1, step2));

        application.getOperationsManager().genericRequest(params, true, new Callback() {
            @Override
            public void onSuccess(JsonElement reply) {
                ProgressDialogFragment.dismissDialog(getSherlockActivity());

                // clear existing data
                attributes.clear();
                childTypes.clear();

                JsonObject jsonObj = reply.getAsJsonObject();

                // attributes
                JsonObject attrs = jsonObj.getAsJsonObject("step-1").getAsJsonObject("result");
                // children types
                JsonArray childs = jsonObj.getAsJsonObject("step-2").getAsJsonArray("result");

                // iterate attributes
                for (Map.Entry<String, JsonElement> e : attrs.entrySet()) {
                    String name = e.getKey();

                    boolean found = false;
                    // check if it exists in child types
                    for (JsonElement elem : childs) {
                        if (elem.getAsString().equals(name)) {
                            found = true;
                        }
                    }

                    if (!found) { // its an attribute
                        Attribute attr = new Attribute();

                        attr.setName(name);
                        attr.setValue(e.getValue());
                        attr.setPath(path);

                        attributes.add(attr);

                    } else { // its a child type
                        ChildType type = new ChildType();
                        type.setName(name);
                        type.setValue(e.getValue());

                        childTypes.add(type);
                    }
                }

                // if empty add dummies to fill the UI
                // add a dummy one to fill the space
                if (childTypes.size() == 0) {
                    childTypes.add(new ChildType("-empty list-"));
                }

                if (attributes.size() == 0)
                    attributes.add(new Attribute("-empty list-"));

                // sort by name
                Collections.sort(attributes);
                Collections.sort(childTypes);

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

    public void showAttributeEditor(final Attribute selectedAttr) {
        // we must do read-resource-description to initialize info if
        // this attribute 'Type' is empty.
        // NOTE: since the methods retrieves information for all
        // attributes we update all of our attributes in our local model
        // to avoid contacting the server again in case a user
        // clicks another attribute in the same resource.
        if (selectedAttr.getType() == null) {
            ProgressDialogFragment.showDialog(getSherlockActivity(), R.string.queryingServer);

            ParametersMap params = ParametersMap.newMap()
                    .add("operation", "read-resource-description")
                    .add("address", (path == null ? Arrays.asList("/") : this.path));

            application.getOperationsManager().genericRequest(params, true, new Callback() {
                @Override
                public void onSuccess(JsonElement reply) {
                    ProgressDialogFragment.dismissDialog(getSherlockActivity());

                    JsonObject jsonObj = reply.getAsJsonObject();

                    JsonObject jsonAttrs = jsonObj.getAsJsonObject("attributes");

                    for (Attribute attr : attributes) {
                        JsonObject info = jsonAttrs.getAsJsonObject(attr.getName());

                        if (info == null) {
                            attr.setType(ManagementModelBase.Type.UNDEFINED);
                            continue;
                        }

                        attr.setTypeFromString(info.getAsJsonObject("type").getAsJsonPrimitive("TYPE_MODEL_VALUE")
                                .getAsString());

                        // for LIST type extract the type of object the list holds
                        if (attr.getType() == ManagementModelBase.Type.LIST) {
                            // TODO: currently we only support single type LISTS
                            // not a collection of various objects (see 'add' deployment)
                            JsonElement typeModelValue = info.getAsJsonObject("value-type").getAsJsonPrimitive("TYPE_MODEL_VALUE");

                            if (typeModelValue != null)
                                attr.setValueTypeFromString(typeModelValue.getAsString());
                        }

                        attr.setDescr(info.get("description").getAsString());

                        if (info.get("access-type").getAsString().equals("read-only")
                                || info.get("access-type").getAsString().equals("metric"))
                            attr.setReadOnly(true);
                    }

                    showFragment(AttributeEditorFragment.newInstance(selectedAttr));
                }

                @Override
                public void onFailure(Exception e) {
                    ProgressDialogFragment.dismissDialog(getSherlockActivity());

                    ErrorDialogFragment.showDialog(getSherlockActivity(), e.getMessage());
                }
            }
            );
        } else {
            showFragment(AttributeEditorFragment.newInstance(selectedAttr));
        }
    }

    public void showChildType(ChildType type) {
        showFragment(ChildResourcesViewFragment.newInstance(this.path, type.getName()));
    }

    public void showOperations() {
        showFragment(OperationsViewFragment.newInstance(this.path));
    }

    private void showFragment(Fragment fragment) {
        ((JBossServerRootActivity) getSherlockActivity()).addFragment(fragment);
    }

    class AttributeAdapter extends ArrayAdapter<Attribute> {
        AttributeAdapter(List<Attribute> attributes) {
            super(getSherlockActivity(), R.layout.twoline_list_item, R.id.text1, attributes);
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

        @Override
        public boolean isEnabled(int position) {
            return (!getItem(position).getName().equals("-empty list-"));
        }

        class ViewHolder {
            TextView text1;
            TextView text2;

            ViewHolder(View row) {
                this.text1 = (TextView) row.findViewById(R.id.text1);
                this.text2 = (TextView) row.findViewById(R.id.text2);
            }

            void populateFrom(Attribute attr) {
                text1.setText(attr.getName());

                if (attr.getName().equals("-empty list-")) {
                    text1.setTextColor(Color.GRAY);
                    text1.setTextSize(14);
                }

                if (attr.getValue() != null)
                    text2.setText(attr.getValue().toString());
                else
                    text2.setText("");

            }
        }
    }

    class ChildTypeAdapter extends ArrayAdapter<ChildType> {
        ChildTypeAdapter(List<ChildType> childTypes) {
            super(getSherlockActivity(), R.layout.icon_text_row, R.id.row_name, childTypes);
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

        @Override
        public boolean isEnabled(int position) {
            return (!getItem(position).getName().equals("-empty list-"));
        }

        class ViewHolder {
            ImageView icon = null;
            TextView name = null;

            ViewHolder(View row) {
                this.icon = (ImageView) row.findViewById(R.id.row_icon);
                this.name = (TextView) row.findViewById(R.id.row_name);
            }

            void populateFrom(ChildType childType) {
                name.setText(childType.getName());

                if (childType.getName().equals("-empty list-")) {
                    name.setTextColor(Color.GRAY);
                    name.setTextSize(14);
                } else {
                    icon.setImageResource(R.drawable.ic_folder);
                }
            }
        }
    }
}
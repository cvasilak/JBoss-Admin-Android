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
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import com.google.gson.JsonElement;
import org.cvasilak.jboss.mobile.app.JBossAdminApplication;
import org.cvasilak.jboss.mobile.app.R;
import org.cvasilak.jboss.mobile.app.fragments.dialogs.ErrorDialogFragment;
import org.cvasilak.jboss.mobile.app.fragments.dialogs.InfoDialogFragment;
import org.cvasilak.jboss.mobile.app.fragments.dialogs.ProgressDialogFragment;
import org.cvasilak.jboss.mobile.app.model.Attribute;
import org.cvasilak.jboss.mobile.app.net.Callback;
import org.cvasilak.jboss.mobile.app.util.ParametersMap;
import org.cvasilak.jboss.mobile.app.util.commonsware.MergeAdapter;
import org.cvasilak.jboss.mobile.app.util.listview.adapters.ManagementModelRowAdapter;
import org.cvasilak.jboss.mobile.app.util.listview.adapters.ValueChangedListener;

import java.util.Arrays;

public class AttributeEditorFragment extends ListFragment implements ValueChangedListener {

    private static final String TAG = AttributeEditorFragment.class.getSimpleName();

    private JBossAdminApplication application;

    private Attribute attr;

    private Attribute clonedAttr;
    private static final String PARCELABLE_KEY = "ATTR_KEY";

    public static AttributeEditorFragment newInstance(Attribute attr) {
        AttributeEditorFragment f = new AttributeEditorFragment();

        Bundle args = new Bundle();
        args.putParcelable(PARCELABLE_KEY, attr);

        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        application = (JBossAdminApplication) getActivity().getApplication();

        this.attr = getArguments().getParcelable(PARCELABLE_KEY);
        // edit operation will be done one a cloned 'attribute'
        // if the user performs 'saves' and its successfully,
        // then we will modify the original attribute.
        this.clonedAttr = new Attribute(attr);

        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ActionBar bar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        bar.setTitle(attr.getName());

        MergeAdapter adapter = new MergeAdapter();

        // Section: Attributes
        adapter.addAdapter(new ManagementModelRowAdapter(getActivity(), Arrays.asList(clonedAttr), this));

        // Section: Description
        TextView descrHeader = new TextView(getActivity());
        descrHeader.setBackgroundColor(Color.DKGRAY);
        descrHeader.setPadding(15, 10, 0, 10);
        descrHeader.setText(R.string.description);
        adapter.addView(descrHeader);

        TextView description = new TextView(getActivity());
        description.setText(attr.getDescr());

        adapter.addView(description);

        setListAdapter(adapter);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // disable 'Save' button if the attribute is 'read-only'
        if (attr.isReadOnly())
            menu.getItem(0).setEnabled(false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_save, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.save) {

            View v = getActivity().getCurrentFocus();

            // handle the case where an editext has focus
            // and user clicks 'Save'.
            // update local model and hide the soft keyboard
            if (v != null && (v instanceof EditText)) {
                EditText text = (EditText) v;

                onValueChanged(text.getTag().toString(), text.getText().toString());

                // hide the keyboard now
                InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }

            save();

            return (true);
        }

        return (super.onOptionsItemSelected(item));
    }

    public void save() {
        ProgressDialogFragment.showDialog(getActivity(), R.string.applyingAction);

        ParametersMap params = ParametersMap.newMap()
                .add("operation", "write-attribute")
                .add("address", (attr.getPath() == null ? Arrays.asList("/") : attr.getPath()))
                .add("name", attr.getName())
                .add("value", clonedAttr.getValue());

        application.getOperationsManager().genericRequest(params, false, new Callback() {
            @Override
            public void onSuccess(JsonElement reply) {
                ProgressDialogFragment.dismissDialog(getActivity());

                if (reply.getAsJsonObject().get("outcome").getAsString().equals("success"))
                    attr.setValue(clonedAttr.getValue());

                // show the reply from server
                InfoDialogFragment infoDialog = InfoDialogFragment.
                        newInstance(getString(R.string.dialog_server_reply_title), reply.toString());
                infoDialog.show(getActivity().getSupportFragmentManager(), InfoDialogFragment.TAG);
            }

            @Override
            public void onFailure(Exception e) {
                ProgressDialogFragment.dismissDialog(getActivity());
                ErrorDialogFragment.showDialog(getActivity(), e.getMessage());
            }
        });
    }

    @Override
    public void onValueChanged(final String tag, final Object value) {
        clonedAttr.setValue(value);
    }
}
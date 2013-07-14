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
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockDialogFragment;
import com.google.gson.JsonElement;
import org.cvasilak.jboss.mobile.admin.JBossAdminApplication;
import org.cvasilak.jboss.mobile.admin.R;
import org.cvasilak.jboss.mobile.admin.fragments.dialogs.ErrorDialogFragment;
import org.cvasilak.jboss.mobile.admin.fragments.dialogs.ProgressDialogFragment;
import org.cvasilak.jboss.mobile.admin.model.Server;
import org.cvasilak.jboss.mobile.admin.net.Callback;
import org.cvasilak.jboss.mobile.admin.net.TalkToJBossServerTask;
import org.cvasilak.jboss.mobile.admin.util.ParametersMap;

import java.util.*;

public class DeploymentDetailsDialogFragment extends SherlockDialogFragment {

    private static final String TAG = DeploymentDetailsDialogFragment.class.getSimpleName();

    private JBossAdminApplication application;

    private EditText key;
    private EditText name;
    private EditText runtimeName;

    private Server server;
    private String BYTES_VALUE;
    private String NAME;

    public static DeploymentDetailsDialogFragment newInstance(Server server, String BYTES_VALUE, String name) {
        DeploymentDetailsDialogFragment f = new DeploymentDetailsDialogFragment();

        Bundle args = new Bundle();

        args.putParcelable("server", server);
        args.putString("BYTES_VALUE", BYTES_VALUE);
        args.putString("name", name);

        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        Log.d(TAG, "@onCreate()");

        application = (JBossAdminApplication) getSherlockActivity().getApplication();

        // extract params
        server = getArguments().getParcelable("server");
        BYTES_VALUE = getArguments().getString("BYTES_VALUE");
        NAME = getArguments().getString("name");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.deploymentdetail_form, container, false);

        key = (EditText) view.findViewById(R.id.key);
        key.setText(BYTES_VALUE);
        key.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        name = (EditText) view.findViewById(R.id.name);
        name.setText(NAME);
        name.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        runtimeName = (EditText) view.findViewById(R.id.runtimeName);
        runtimeName.setText(NAME);
        runtimeName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        Button save = (Button) view.findViewById(R.id.done);

        save.setOnClickListener(onSave);

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private View.OnClickListener onSave = new View.OnClickListener() {
        public void onClick(View v) {

            if (name.getText().toString().equals("")
                    || runtimeName.getText().toString().equals("")) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                        getSherlockActivity());

                alertDialog
                        .setTitle(R.string.dialog_error_title)
                        .setMessage(R.string.missing_params)
                        .setPositiveButton(R.string.dialog_button_Bummer, null)
                        .setCancelable(false)
                        .setIcon(android.R.drawable.ic_dialog_alert).show();

                return;
            }

            // set up 'add-content' params
            Map<String, String> bytes = new HashMap<String, String>();
            bytes.put("BYTES_VALUE", BYTES_VALUE);

            Map<String, Map<String, String>> HASH = new HashMap<String, Map<String, String>>();
            HASH.put("hash", bytes);

            ParametersMap params = ParametersMap.newMap()
                    .add("operation", "add")
                    .add("address", Arrays.asList("deployment", name.getText().toString()))
                    .add("name", name.getText().toString())
                    .add("runtime-name", runtimeName.getText().toString())
                    .add("content", wrapToList(HASH));

            ProgressDialogFragment.showDialog(getSherlockActivity(), R.string.enablingDeployment);

            TalkToJBossServerTask task = new TalkToJBossServerTask(getSherlockActivity(), server, new Callback() {
                @Override
                public void onSuccess(JsonElement reply) {
                    ProgressDialogFragment.dismissDialog(getSherlockActivity());

                    Toast.makeText(getSherlockActivity(), getString(R.string.deployment_added), Toast.LENGTH_SHORT).show();

                    getSherlockActivity().finish();
                }

                @Override
                public void onFailure(Exception e) {
                    ProgressDialogFragment.dismissDialog(getSherlockActivity());

                    ErrorDialogFragment.showDialog(getSherlockActivity(), e.getMessage());
                }
            });

            task.execute(params);
        }
    };

    private List<Map<String, Map<String, String>>> wrapToList(Map<String, Map<String, String>> map) {
        List<Map<String, Map<String, String>>> list = new ArrayList<Map<String, Map<String, String>>>();
        list.add(map);

        return list;
    }

    @Override
    public void onDestroyView() {
        // Work around bug:
        // http://code.google.com/p/android/issues/detail?id=17423
        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
        super.onDestroyView();
    }
}

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
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import org.cvasilak.jboss.mobile.admin.JBossAdminApplication;
import org.cvasilak.jboss.mobile.admin.R;
import org.cvasilak.jboss.mobile.admin.fragments.dialogs.ErrorDialogFragment;
import org.cvasilak.jboss.mobile.admin.model.Server;
import org.cvasilak.jboss.mobile.admin.model.ServersManager;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class ServerEditFragment extends Fragment {

    private static final String TAG = ServerEditFragment.class.getSimpleName();

    private EditText name;
    private EditText hostname;
    private EditText port;
    private CheckBox isSSLSecured;
    private EditText username;
    private EditText password;

    private ServersManager serversManager;
    private Server server;

    public static ServerEditFragment newInstance(int serverIndex) {
        ServerEditFragment f = new ServerEditFragment();

        Bundle args = new Bundle();

        args.putInt("index", serverIndex);

        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        Log.d(TAG, "@onCreate()");

        serversManager = ((JBossAdminApplication) getActivity().getApplication()).getServersManager();

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.serverdetail_form, null);

        name = (EditText) view.findViewById(R.id.name);
        hostname = (EditText) view.findViewById(R.id.hostname);
        port = (EditText) view.findViewById(R.id.port);
        isSSLSecured = (CheckBox) view.findViewById(R.id.isSSLSecured);
        username = (EditText) view.findViewById(R.id.username);
        password = (EditText) view.findViewById(R.id.password);

        int serverIndex = getArguments().getInt("index");

        if (serverIndex != -1) {  // we are asked to edit an existing server
            server = serversManager.serverAtIndex(serverIndex);

            name.setText(server.getName());
            hostname.setText(server.getHostname());
            port.setText(String.valueOf(server.getPort()));
            isSSLSecured.setChecked(server.isSSLSecured());
            username.setText(server.getUsername());
            password.setText(server.getPassword());
        }

        ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();

        actionBar.setTitle(serverIndex != -1 ? server.getName() : getString(R.string.new_server));
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_save, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getActivity().onBackPressed();
        } else {
            if (item.getItemId() == R.id.save) {
                if (name.getText().toString().equals("")
                        || hostname.getText().toString().equals("")
                        || port.getText().toString().equals("")) {

                    ErrorDialogFragment.showDialog(getActivity(), getString(R.string.not_enough_params));

                    return false;
                }

                if (server == null) {
                    server = new Server();

                    serversManager.addServer(server);
                }

                server.setName(name.getText().toString());
                server.setHostname(hostname.getText().toString());
                server.setPort(Integer.parseInt(port.getText().toString()));
                server.setSSLSecured(isSSLSecured.isChecked());
                server.setUsername(username.getText().toString());

                // try to urlencode password
                try {
                    String encodedPassword = URLEncoder.encode(password.getText().toString(), "UTF-8");
                    server.setPassword(encodedPassword);
                } catch (UnsupportedEncodingException e) {
                    ErrorDialogFragment.showDialog(getActivity(), getString(R.string.error_on_password_save));
                }

                try {
                    serversManager.save();

                    View v = getActivity().getCurrentFocus();

                    // hide the soft keyboard if open
                    if (v != null && (v instanceof EditText)) {
                        // hide the keyboard now
                        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }

                    getActivity().getSupportFragmentManager().popBackStack();

                } catch (Exception e) { // error occurred during save
                    ErrorDialogFragment.showDialog(getActivity(), getString(R.string.error_on_save));
                }
            }
        }

        return true;
    }
}

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

package org.cvasilak.jboss.mobile.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import org.cvasilak.jboss.mobile.app.R;
import org.cvasilak.jboss.mobile.app.fragments.DeploymentDetailsDialogFragment;
import org.cvasilak.jboss.mobile.app.model.Server;

public class UploadCompletedActivity extends ActionBarActivity {

    private static final String TAG = UploadCompletedActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
    }

    @Override
    public void onResume() {
        super.onResume();

        setTitle(R.string.upload_deployment_step2);

        // extract params passed from the service
        Bundle extras = getIntent().getExtras();

        Server server = extras.getParcelable("server");
        String BYTES_VALUE = extras.getString("BYTES_VALUE");
        String filename = extras.getString("filename");

        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG);
        if (fragment != null)
            getSupportFragmentManager().beginTransaction().remove(fragment).commit();

        getSupportFragmentManager().beginTransaction()
                .add(R.id.app_container,
                        DeploymentDetailsDialogFragment.newInstance(server, BYTES_VALUE, filename), TAG)
                .commit();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
    }
}
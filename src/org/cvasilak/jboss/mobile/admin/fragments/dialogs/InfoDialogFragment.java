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

package org.cvasilak.jboss.mobile.admin.fragments.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import org.cvasilak.jboss.mobile.admin.R;

public class InfoDialogFragment extends DialogFragment {

    public static final String TAG = InfoDialogFragment.class.getSimpleName();

    public static InfoDialogFragment newInstance(String title, String message) {
        InfoDialogFragment f = new InfoDialogFragment();

        Bundle args = new Bundle();

        args.putString("title", title);
        args.putString("message", message);

        f.setArguments(args);

        return f;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                getActivity());

        builder
                .setTitle(getArguments().getString("title"))
                .setMessage(getArguments().getString("message"))
                .setPositiveButton(R.string.dialog_button_OK, null)
                .setCancelable(false)
                .setIcon(android.R.drawable.ic_dialog_info);

        return builder.create();
    }

    public static void showDialog(FragmentActivity activity, String title, String message) {
        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();

        InfoDialogFragment dialog = InfoDialogFragment.newInstance(title, message);
        dialog.show(ft, TAG);
    }
}

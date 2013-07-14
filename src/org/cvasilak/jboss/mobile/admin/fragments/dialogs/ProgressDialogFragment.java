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

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class ProgressDialogFragment extends SherlockDialogFragment {

    public static final String TAG = ProgressDialogFragment.class.getSimpleName();

    public static ProgressDialogFragment newInstance(int msgId) {
        ProgressDialogFragment f = new ProgressDialogFragment();

        Bundle args = new Bundle();

        args.putInt("message", msgId);

        f.setArguments(args);

        return f;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog dialog = new ProgressDialog(getSherlockActivity());
        dialog.setMessage(getSherlockActivity().getString(getArguments().getInt("message")));
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);

        // Disable the back button
        DialogInterface.OnKeyListener keyListener = new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode,
                                 KeyEvent event) {
                return (keyCode == KeyEvent.KEYCODE_BACK);
            }
        };

        dialog.setOnKeyListener(keyListener);
        return dialog;
    }

    public static void showDialog(SherlockFragmentActivity activity, int msgId) {
        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();

        ProgressDialogFragment dialog = ProgressDialogFragment.newInstance(msgId);
        dialog.show(ft, TAG);
    }

    public static void dismissDialog(SherlockFragmentActivity activity) {
        ProgressDialogFragment dialog =
                (ProgressDialogFragment) activity.getSupportFragmentManager().findFragmentByTag(TAG);

        if (dialog != null) {
            FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
            ft.remove(dialog);
            ft.commitAllowingStateLoss();
        }
    }
}

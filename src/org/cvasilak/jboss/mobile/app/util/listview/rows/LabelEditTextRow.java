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

package org.cvasilak.jboss.mobile.app.util.listview.rows;

import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import org.cvasilak.jboss.mobile.app.R;
import org.cvasilak.jboss.mobile.app.activities.JBossServerRootActivity;
import org.cvasilak.jboss.mobile.app.fragments.dialogs.InfoDialogFragment;
import org.cvasilak.jboss.mobile.app.util.listview.adapters.ValueChangedListener;

public class LabelEditTextRow implements RowView {

    private final Context context;
    private final LayoutInflater inflater;

    private final boolean isRequired;
    private final String label;
    private final String descr;

    private Object value;

    private final String hint;
    private final boolean isNumber;

    private ValueChangedListener listener;

    public LabelEditTextRow(Context context, boolean isRequired, String label,
                            String descr, String hint, boolean isNumber, Object value, ValueChangedListener listener) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);

        this.isRequired = isRequired;
        this.label = label;
        this.descr = descr;
        this.hint = hint;
        this.isNumber = isNumber;

        this.listener = listener;

        // for 'undefined' values default to an empty string
        if (value == null || (value instanceof String && value.equals("undefined")))
            this.value = "";
        else
            this.value = value;
    }

    public View getView(View convertView) {
        View row = convertView;
        ViewHolder holder;

        if (row == null) {
            row = inflater.inflate(R.layout.label_edittext, null);
            holder = new ViewHolder(row);

            row.setTag(holder);

        } else {
            holder = (ViewHolder) row.getTag();
        }

        holder.starIcon.setVisibility(isRequired ? View.VISIBLE : View.INVISIBLE);

        holder.label.setText(label);
        holder.value.setHint(hint);
        holder.value.setTag(label);
        holder.value.setText(value.toString());

        holder.infoIcon.setVisibility(View.VISIBLE);
        holder.infoIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InfoDialogFragment infoDialog = InfoDialogFragment.
                        newInstance(label, descr);

                infoDialog.show(((JBossServerRootActivity) context).getSupportFragmentManager(), InfoDialogFragment.TAG);
            }
        });

        // adjust soft keyboard based on type
        if (isNumber)
            holder.value.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        else
            holder.value.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        holder.value.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (listener == null)
                    return;

                EditText text = ((EditText) v);

                String value = text.getText().toString();

                LabelEditTextRow.this.value = value;

                listener.onValueChanged(label, value);
            }
        });

        return row;
    }

    public int getViewType() {
        return RowType.LABEL_EDIT_TEXT.ordinal();
    }

    class ViewHolder {
        ImageView starIcon = null;
        TextView label = null;
        EditText value = null;
        ImageView infoIcon = null;

        ViewHolder(View row) {
            this.starIcon = (ImageView) row.findViewById(R.id.row_star_icon);
            this.label = (TextView) row.findViewById(R.id.label);
            this.value = (EditText) row.findViewById(R.id.value);
            this.infoIcon = (ImageView) row.findViewById(R.id.row_info_icon);
        }
    }
}

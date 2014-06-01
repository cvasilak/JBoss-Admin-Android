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

package org.cvasilak.jboss.mobile.app.fragments.util;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.InputType;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import com.mobeta.android.dslv.DragSortListView;
import org.cvasilak.jboss.mobile.app.R;
import org.cvasilak.jboss.mobile.app.fragments.dialogs.ErrorDialogFragment;

import java.util.ArrayList;
import java.util.List;


public class ListEditorFragment extends ListFragment {

    ArrayAdapter<RowModel> adapter;

    private ListEditorListener listener;

    public static ListEditorFragment newInstance(ArrayList<String> list, boolean isNumber, String placeHolder) {
        ListEditorFragment f = new ListEditorFragment();

        Bundle args = new Bundle();
        args.putStringArrayList("list", list);
        args.putBoolean("isNumber", isNumber);
        args.putString("placeHolder", placeHolder);

        f.setArguments(args);

        return f;
    }

    private DragSortListView.DropListener onDrop =
            new DragSortListView.DropListener() {
                @Override
                public void drop(int from, int to) {
                    RowModel item = adapter.getItem(from);

                    adapter.remove(item);
                    adapter.insert(item, to);
                }
            };

    private DragSortListView.RemoveListener onRemove =
            new DragSortListView.RemoveListener() {
                @Override
                public void remove(int which) {
                    adapter.remove(adapter.getItem(which));
                }
            };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        adapter = new RowAdapter(getArguments().getBoolean("isNumber"), getArguments().getString("placeHolder"));

        // initialize existing list (if any)
        ArrayList<String> list = getArguments().getStringArrayList("list");

        if (list != null) {
            for (String elem : list) {
                adapter.add(new RowModel(elem));
            }
        }

        setListAdapter(adapter);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        View view = inflater.inflate(R.layout.editor_listview, container, false);

        DragSortListView lv = (DragSortListView) view.findViewById(android.R.id.list);

        lv.setDropListener(onDrop);
        lv.setRemoveListener(onRemove);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_add_done, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.add) {
            // add an empty row
            adapter.add(new RowModel(""));

            return true;

        } else if (item.getItemId() == R.id.done) {
            RowAdapter adapter = (RowAdapter) getListAdapter();

            View v = getActivity().getCurrentFocus();

            // handle the case where an editext has focus
            // and user clicks 'Save'.
            // update local model and hide the soft keyboard
            if (v != null && (v instanceof EditText)) {
                EditText text = (EditText) v;

                RowModel model = adapter.getItem((Integer) text.getTag());
                model.value = ((EditText) v).getText().toString();

                // hide the keyboard now
                InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }

            // quickly scan for unedited textboxes
            // if so exit
            for (int i = 0; i < adapter.getCount(); i++) {
                if (adapter.getItem(i).value.equals("")) {

                    ErrorDialogFragment errorDialog = ErrorDialogFragment.newInstance(getString(R.string.unedited_field));
                    errorDialog.show(getActivity().getSupportFragmentManager(), ErrorDialogFragment.TAG);

                    return false;
                }
            }

            if (listener != null) {
                ArrayList<String> list = new ArrayList<String>();

                for (int i = 0; i < adapter.getCount(); i++) {
                    list.add(adapter.getItem(i).value);
                }

                // inform our listener for the list
                listener.onListEditorFinished(list);

                // close listeditor screen by simulating back button pressed.
                // this will allow the root activity to remove the fragment
                // from the back stack
                getActivity().onBackPressed();
            }
        }

        return (super.onOptionsItemSelected(item));
    }

    public ListEditorListener getListener() {
        return listener;
    }

    public void setListener(ListEditorListener listener) {
        this.listener = listener;
    }

    class RowAdapter extends ArrayAdapter<RowModel> {
        boolean isNumber;
        String placeHolder;

        RowAdapter(boolean isNumber, String placeHolder) {
            super(getActivity(), R.layout.editor_listview_row, R.id.value);

            this.isNumber = isNumber;
            this.placeHolder = placeHolder;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = super.getView(position, convertView, parent);

            EditText text = (EditText) row.getTag();

            if (text == null) {
                text = (EditText) row.findViewById(R.id.value);
                row.setTag(text);

                text.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        Integer position = (Integer) v.getTag();

                        RowModel model = getItem(position);

                        model.value = ((EditText) v).getText().toString();
                    }
                });
            }

            text.setTag(position);
            text.setText(getItem(position).value);

            // placeholder for the type
            text.setHint(placeHolder);

            // adjust keyboard based on type
            if (isNumber)
                text.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            else
                text.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

            return row;
        }
    }

    class RowModel {
        String value;

        RowModel(String value) {
            this.value = value;
        }

        public String toString() {
            return value;
        }
    }

    public interface ListEditorListener {
        public void onListEditorFinished(final List<String> list);
    }
}

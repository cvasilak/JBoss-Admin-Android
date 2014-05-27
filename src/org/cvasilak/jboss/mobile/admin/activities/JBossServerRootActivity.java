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

package org.cvasilak.jboss.mobile.admin.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import org.cvasilak.jboss.mobile.admin.JBossAdminApplication;
import org.cvasilak.jboss.mobile.admin.R;
import org.cvasilak.jboss.mobile.admin.fragments.ProfileViewFragment;
import org.cvasilak.jboss.mobile.admin.fragments.RuntimeViewFragment;

import java.util.HashMap;
import java.util.Stack;
import java.util.UUID;

/*
 * Activity that supports tabs with fragments where each tab can have separate back stack.
 * Extracted from Andrey Novikov post found in https://gist.github.com/andreynovikov/4619215
 * and modified a bit to suit our needs.
 */
public class JBossServerRootActivity extends ActionBarActivity implements ActionBar.TabListener {

    private static final String TAG = JBossServerRootActivity.class.getSimpleName();

    private JBossAdminApplication application;

    private enum TabType {
        RUNTIME, PROFILE
    }

    private HashMap<TabType, Stack<String>> backStacks;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "@onCreate()");

        application = (JBossAdminApplication) getApplication();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Set back stacks
        if (savedInstanceState != null) {
            // Read back stacks after orientation change

            @SuppressWarnings("unchecked")
            HashMap<TabType, Stack<String>> restore = (HashMap<TabType, Stack<String>>)
                    savedInstanceState.getSerializable("stacks");

            backStacks = restore;
        } else {
            // Initialize back stacks on first run
            backStacks = new HashMap<TabType, Stack<String>>();
            backStacks.put(TabType.RUNTIME, new Stack<String>());
            backStacks.put(TabType.PROFILE, new Stack<String>());
        }

        actionBar.addTab(actionBar.newTab()
                .setIcon(R.drawable.ic_summary)
                .setTag(TabType.RUNTIME)
                .setTabListener(this));

        actionBar.addTab(actionBar.newTab()
                .setIcon(R.drawable.ic_profile)
                .setTag(TabType.PROFILE)
                .setTabListener(this));

        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Select proper stack
        ActionBar.Tab tab = getSupportActionBar().getSelectedTab();
        Stack<String> backStack = backStacks.get(tab.getTag());

        if (!backStack.isEmpty()) {
            // Restore topmost fragment (e.g. after application switch)
            String tag = backStack.peek();
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
            if (fragment.isDetached()) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.attach(fragment);
                ft.commit();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Select proper stack
        ActionBar.Tab tab = getSupportActionBar().getSelectedTab();
        Stack<String> backStack = backStacks.get(tab.getTag());
        if (!backStack.isEmpty()) {
            // Detach topmost fragment otherwise it will not be correctly displayed
            // after orientation change
            String tag = backStack.peek();
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
            ft.detach(fragment);
            ft.commit();
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // Restore selected tab
        int saved = savedInstanceState.getInt("tab", 0);
        if (saved != getSupportActionBar().getSelectedNavigationIndex())
            getSupportActionBar().setSelectedNavigationItem(saved);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save selected tab and all back stacks
        outState.putInt("tab", getSupportActionBar().getSelectedNavigationIndex());
        outState.putSerializable("stacks", backStacks);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        // Select proper stack
        ActionBar.Tab tab = getSupportActionBar().getSelectedTab();
        Stack<String> backStack = backStacks.get(tab.getTag());
        String tag = backStack.pop();
        if (backStack.isEmpty()) {
            // Let application finish
            super.onBackPressed();
        } else {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
            // Animate return to previous fragment
            ft.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            // Remove topmost fragment from back stack and forget it
            ft.remove(fragment);
            showFragment(backStack, ft);
            ft.commit();
        }
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        // Select proper stack
        Stack<String> backStack = backStacks.get(tab.getTag());

        if (backStack.isEmpty()) {
            // If it is empty instantiate and add initial tab fragments
            Fragment fragment;
            switch ((TabType) tab.getTag()) {
                case RUNTIME:
                    fragment = Fragment.instantiate(this, RuntimeViewFragment.class.getName());
                    break;
                case PROFILE:
                    fragment = Fragment.instantiate(this, ProfileViewFragment.class.getName());
                    break;
                default:
                    throw new java.lang.IllegalArgumentException("Unknown tab");
            }
            addFragment(fragment, backStack, ft);
        } else {
            // Show topmost fragment
            showFragment(backStack, ft);

        }
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
        // Select proper stack
        Stack<String> backStack = backStacks.get(tab.getTag());
        // Get topmost fragment
        String tag = backStack.peek();
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
        // Detach it
        ft.detach(fragment);
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
        // Select proper stack
        Stack<String> backStack = backStacks.get(tab.getTag());

        if (backStack.size() > 1)
            ft.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right);

        // Clean the stack leaving only initial fragment
        while (backStack.size() > 1) {
            // Pop topmost fragment
            String tag = backStack.pop();
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
            // Remove it
            ft.remove(fragment);
        }

        showFragment(backStack, ft);
    }

    public void addFragment(Fragment fragment) {
        // Select proper stack
        ActionBar.Tab tab = getSupportActionBar().getSelectedTab();
        Stack<String> backStack = backStacks.get(tab.getTag());

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        // Animate transfer to new fragment
        ft.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        // Get topmost fragment
        String tag = backStack.peek();
        Fragment top = getSupportFragmentManager().findFragmentByTag(tag);
        ft.detach(top);
        // Add new fragment
        addFragment(fragment, backStack, ft);
        ft.commit();
    }

    private void addFragment(Fragment fragment, Stack<String> backStack, FragmentTransaction ft) {
        // Add fragment to back stack with unique tag
        String tag = UUID.randomUUID().toString();
        ft.add(android.R.id.content, fragment, tag);
        backStack.push(tag);
    }

    private void showFragment(Stack<String> backStack, FragmentTransaction ft) {
        // Peek topmost fragment from the stack
        String tag = backStack.peek();
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
        // and attach it
        ft.attach(fragment);
    }
}
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

package org.cvasilak.jboss.mobile.admin.model;

public class Deployment {

    private String name;
    private String runtimeName;
    private boolean enabled;
    private String BYTES_VALUE;

    public Deployment() {
    }

    public Deployment(String name, String runtimeName, boolean enabled, String BYTES_VALUE) {
        this.name = name;
        this.runtimeName = runtimeName;
        this.enabled = enabled;
        this.BYTES_VALUE = BYTES_VALUE;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRuntimeName() {
        return runtimeName;
    }

    public void setRuntimeName(String runtimeName) {
        this.runtimeName = runtimeName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBYTES_VALUE() {
        return BYTES_VALUE;
    }

    public void setBYTES_VALUE(String BYTES_VALUE) {
        this.BYTES_VALUE = BYTES_VALUE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Deployment that = (Deployment) o;

        if (enabled != that.enabled) return false;
        if (BYTES_VALUE != null ? !BYTES_VALUE.equals(that.BYTES_VALUE) : that.BYTES_VALUE != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (runtimeName != null ? !runtimeName.equals(that.runtimeName) : that.runtimeName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (runtimeName != null ? runtimeName.hashCode() : 0);
        result = 31 * result + (enabled ? 1 : 0);
        result = 31 * result + (BYTES_VALUE != null ? BYTES_VALUE.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return name;
    }
}
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

package org.cvasilak.jboss.mobile.admin.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ParametersMap implements Map<String, Object> {

    private final Map<String, Object> params = new HashMap<String, Object>();

    @Override
    public void clear() {
        params.clear();
    }

    @Override
    public boolean containsKey(final Object o) {
        return params.containsKey(o);
    }

    @Override
    public boolean containsValue(final Object o) {
        return params.containsValue(o);
    }

    @Override
    public Set<java.util.Map.Entry<String, Object>> entrySet() {
        return params.entrySet();
    }

    @Override
    public Object get(final Object o) {
        return params.get(o);
    }

    @Override
    public boolean isEmpty() {
        return params.isEmpty();
    }

    @Override
    public Set<String> keySet() {
        return params.keySet();
    }

    @Override
    public Object put(final String s, final Object o) {
        return params.put(s, o);
    }

    @Override
    public void putAll(final Map<? extends String, ? extends Object> map) {
        params.putAll(map);
    }

    @Override
    public Object remove(final Object o) {
        return params.remove(o);
    }

    @Override
    public int size() {
        return params.size();
    }

    @Override
    public Collection<Object> values() {
        return params.values();
    }

    public static ParametersMap newMap() {
        return new ParametersMap();
    }

    public ParametersMap add(final String name, final Object value) {
        params.put(name, value);
        return this;
    }
}

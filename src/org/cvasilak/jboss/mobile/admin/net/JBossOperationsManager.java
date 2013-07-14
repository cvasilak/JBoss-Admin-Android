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

package org.cvasilak.jboss.mobile.admin.net;

import android.content.Context;
import org.cvasilak.jboss.mobile.admin.model.Server;
import org.cvasilak.jboss.mobile.admin.util.ParametersMap;

import java.util.*;

public class JBossOperationsManager {

    private static final String TAG = JBossOperationsManager.class.getSimpleName();

    private final Context context;
    private final Server server;

    private String domainHost;
    private String domainServer;

    private boolean isDomainController;

    public static enum DataSourceType {
        StandardDataSource,
        XADataSource
    }

    public static enum HostStatus {
        STARTED,
        STARTING,
        STOPPING,
        STOPPED,
        DISABLED,
        FAILED
    }

    public static enum JMSType {
        QUEUE,
        TOPIC
    }

    public JBossOperationsManager(Context context, Server server) {
        this.context = context;
        this.server = server;
    }

    public Server getServer() {
        return server;
    }

    public String getDomainHost() {
        return domainHost;
    }

    public String getDomainServer() {
        return domainServer;
    }

    public boolean isDomainController() {
        return isDomainController;
    }

    public void changeActiveMonitoringServer(final String host, final String server) {
        isDomainController = true;

        this.domainHost = host;
        this.domainServer = server;
    }

    private List<String> prefixAddressWithDomainServer(final List<String> address) {
        if (isDomainController) {
            List<String> convAddress = new ArrayList<String>();
            convAddress.add("host");
            convAddress.add(domainHost);
            convAddress.add("server");
            convAddress.add(domainServer);
            convAddress.addAll(address);

            return convAddress;
        }

        return address;
    }

    private List<String> prefixAddressWithDomainGroup(final String group, final List<String> address) {
        if (isDomainController) {
            List<String> convAddress = new ArrayList<String>();

            if (group != null) {
                convAddress.add("server-group");
                convAddress.add(group);
            }

            if (!address.get(0).equals("/")) {
                convAddress.addAll(address);
            }

            return convAddress;
        }

        return address;
    }

    public void fetchJBossVersion(final Callback callback) {
        ParametersMap params = ParametersMap.newMap()
                .add("operation", "read-attribute")
                .add("name", "release-version");

        TalkToJBossServerTask task = new TalkToJBossServerTask(context, server, callback);
        task.execute(params);
    }

    public void fetchActiveServerInformation(final Callback callback) {
        ParametersMap params = ParametersMap.newMap()
                .add("operation", "read-children-resources")
                .add("child-type", "host");

        TalkToJBossServerTask task = new TalkToJBossServerTask(context, server, callback);
        task.execute(params);
    }

    public void fetchDomainHostInfoInformation(final Callback callback) {
        ParametersMap params = ParametersMap.newMap()
                .add("operation", "read-children-names")
                .add("child-type", "host");

        TalkToJBossServerTask task = new TalkToJBossServerTask(context, server, callback);
        task.execute(params);
    }

    public void fetchServersInformation(final String host, final Callback callback) {
        ParametersMap params = ParametersMap.newMap()
                .add("operation", "read-children-resources")
                .add("address", Arrays.asList("host", host))
                .add("child-type", "server-config")
                .add("include-runtime", Boolean.TRUE);

        TalkToJBossServerTask task = new TalkToJBossServerTask(context, server, callback);
        task.execute(params);
    }

    public void fetchConfigurationInformation(final Callback callback) {
        ParametersMap params = ParametersMap.newMap()
                .add("operation", "read-resource")
                .add("address", prefixAddressWithDomainServer(Arrays.asList("/")))
                .add("include-runtime", Boolean.TRUE);

        TalkToJBossServerTask task = new TalkToJBossServerTask(context, server, callback);
        task.execute(params);
    }

    public void fetchExtensionsInformation(final Callback callback) {
        ParametersMap params = ParametersMap.newMap()
                .add("operation", "read-children-names")
                .add("address", prefixAddressWithDomainServer(Arrays.asList("/")))
                .add("child-type", "extension");

        TalkToJBossServerTask task = new TalkToJBossServerTask(context, server, callback);
        task.execute(params);
    }

    public void fetchPropertiesInformation(final Callback callback) {
        ParametersMap params = ParametersMap.newMap()
                .add("operation", "read-attribute")
                .add("address", prefixAddressWithDomainServer(Arrays.asList("core-service", "platform-mbean", "type", "runtime")))
                .add("name", "system-properties");

        TalkToJBossServerTask task = new TalkToJBossServerTask(context, server, callback);
        task.execute(params);
    }

    public void fetchJVMMetrics(final Callback callback) {
        ParametersMap memoryParams = ParametersMap.newMap()
                .add("operation", "read-resource")
                .add("address", prefixAddressWithDomainServer(Arrays.asList("core-service", "platform-mbean", "type", "memory")))
                .add("include-runtime", Boolean.TRUE);

        ParametersMap threadingParams = ParametersMap.newMap()
                .add("operation", "read-resource")
                .add("address", prefixAddressWithDomainServer(Arrays.asList("core-service", "platform-mbean", "type", "threading")))
                .add("include-runtime", Boolean.TRUE);

        ParametersMap os = ParametersMap.newMap()
                .add("operation", "read-resource")
                .add("address", prefixAddressWithDomainServer(Arrays.asList("core-service", "platform-mbean", "type", "operating-system")))
                .add("include-runtime", Boolean.TRUE);

        ParametersMap params = ParametersMap.newMap()
                .add("operation", "composite")
                .add("steps", Arrays.asList(memoryParams, threadingParams, os));

        TalkToJBossServerTask task = new TalkToJBossServerTask(context, server, callback);
        task.execute(params);
    }

    public void fetchDataSourceList(final Callback callback) {
        ParametersMap dsParams = ParametersMap.newMap()
                .add("operation", "read-children-names")
                .add("address", prefixAddressWithDomainServer(Arrays.asList("subsystem", "datasources")))
                .add("child-type", "data-source");

        ParametersMap xadsParams = ParametersMap.newMap()
                .add("operation", "read-children-names")
                .add("address", prefixAddressWithDomainServer(Arrays.asList("subsystem", "datasources")))
                .add("child-type", "xa-data-source");

        ParametersMap params = ParametersMap.newMap()
                .add("operation", "composite")
                .add("steps", Arrays.asList(dsParams, xadsParams));

        TalkToJBossServerTask task = new TalkToJBossServerTask(context, server, callback);
        task.execute(params);
    }

    public void fetchDataSourceMetrics(final String dsName, final DataSourceType dsType, final Callback callback) {
        ParametersMap poolParams = ParametersMap.newMap()
                .add("operation", "read-resource")
                .add("address", prefixAddressWithDomainServer(Arrays.asList("subsystem", "datasources",
                        (dsType == DataSourceType.XADataSource ? "xa-data-source" : "data-source"),
                        dsName,
                        "statistics", "pool")))
                .add("include-runtime", Boolean.TRUE);

        ParametersMap jdbcParams = ParametersMap.newMap()
                .add("operation", "read-resource")
                .add("address", prefixAddressWithDomainServer(Arrays.asList("subsystem", "datasources",
                        (dsType == DataSourceType.XADataSource ? "xa-data-source" : "data-source"),
                        dsName,
                        "statistics", "jdbc")))
                .add("include-runtime", Boolean.TRUE);

        ParametersMap params = ParametersMap.newMap()
                .add("operation", "composite")
                .add("steps", Arrays.asList(poolParams, jdbcParams));

        TalkToJBossServerTask task = new TalkToJBossServerTask(context, server, callback);
        task.execute(params);
    }

    public void fetchJMSMessagingModelList(final JMSType type, final Callback callback) {
        ParametersMap params = ParametersMap.newMap()
                .add("operation", "read-children-names")
                .add("address", prefixAddressWithDomainServer(Arrays.asList("subsystem", "messaging",
                        "hornetq-server", "default")))
                .add("child-type", (type == JMSType.QUEUE ? "jms-queue" : "jms-topic"));

        TalkToJBossServerTask task = new TalkToJBossServerTask(context, server, callback);
        task.execute(params);
    }

    public void fetchJMSQueueMetrics(final String name, final JMSType type, final Callback callback) {
        ParametersMap params = ParametersMap.newMap()
                .add("operation", "read-resource")
                .add("address", prefixAddressWithDomainServer(Arrays.asList("subsystem", "messaging",
                        "hornetq-server", "default",
                        (type == JMSType.QUEUE ? "jms-queue" : "jms-topic"),
                        name)))
                .add("include-runtime", Boolean.TRUE);

        TalkToJBossServerTask task = new TalkToJBossServerTask(context, server, callback);
        task.execute(params);
    }

    public void fetchTranscationMetrics(final Callback callback) {
        ParametersMap params = ParametersMap.newMap()
                .add("operation", "read-resource")
                .add("address", prefixAddressWithDomainServer(Arrays.asList("subsystem", "transactions")))
                .add("include-runtime", Boolean.TRUE);

        TalkToJBossServerTask task = new TalkToJBossServerTask(context, server, callback);
        task.execute(params);
    }

    public void fetchWebConnectorsList(final Callback callback) {
        ParametersMap params = ParametersMap.newMap()
                .add("operation", "read-children-names")
                .add("address", prefixAddressWithDomainServer(Arrays.asList("subsystem", "web")))
                .add("child-type", "connector");

        TalkToJBossServerTask task = new TalkToJBossServerTask(context, server, callback);
        task.execute(params);
    }

    public void fetchWebConnectorMetrics(final String name, final Callback callback) {
        ParametersMap params = ParametersMap.newMap()
                .add("operation", "read-resource")
                .add("address", prefixAddressWithDomainServer(Arrays.asList("subsystem", "web", "connector", name)))
                .add("include-runtime", Boolean.TRUE);

        TalkToJBossServerTask task = new TalkToJBossServerTask(context, server, callback);
        task.execute(params);
    }

    public void fetchDeployments(final String group, final Callback callback) {
        ParametersMap params = ParametersMap.newMap()
                .add("operation", "read-children-resources")
                .add("address", prefixAddressWithDomainGroup(group, Arrays.asList("/")))
                .add("child-type", "deployment");

        TalkToJBossServerTask task = new TalkToJBossServerTask(context, server, callback);
        task.execute(params);
    }

    public void fetchDomainGroups(final Callback callback) {
        ParametersMap params = ParametersMap.newMap()
                .add("operation", "read-children-resources")
                .add("child-type", "server-group");

        TalkToJBossServerTask task = new TalkToJBossServerTask(context, server, callback);
        task.execute(params);
    }

    public void changeDeploymentStatus(final String name, final String group, final boolean enable, final Callback callback) {
        ParametersMap params = ParametersMap.newMap()
                .add("operation", enable ? "deploy" : "undeploy")
                .add("address", prefixAddressWithDomainGroup(group, Arrays.asList("deployment", name)));

        TalkToJBossServerTask task = new TalkToJBossServerTask(context, server, callback);
        task.execute(params);
    }

    public void removeDeployment(final String name, final String group, final Callback callback) {
        ParametersMap params = ParametersMap.newMap()
                .add("operation", "remove")
                .add("address", prefixAddressWithDomainGroup(group, Arrays.asList("deployment", name)));

        TalkToJBossServerTask task = new TalkToJBossServerTask(context, server, callback);
        task.execute(params);
    }

    public void addDeploymentContent(final String hash, final String name, final List<String> groups, final boolean enable, Callback callback) {
        ParametersMap params;

        ArrayList<ParametersMap> steps = new ArrayList<ParametersMap>();

        for (String group : groups) {

            Map<String, String> BYTES_VALUE = new HashMap<String, String>();
            BYTES_VALUE.put("BYTES_VALUE", hash);

            Map<String, Map<String, String>> HASH = new HashMap<String, Map<String, String>>();
            HASH.put("hash", BYTES_VALUE);

            params = ParametersMap.newMap()
                    .add("operation", "add")
                    .add("address", prefixAddressWithDomainGroup(group, Arrays.asList("deployment", name)))
                    .add("name", name)
                    .add("content", wrapToList(HASH));

            steps.add(params);

            if (enable) {
                params = ParametersMap.newMap()
                        .add("operation", "deploy")
                        .add("address", prefixAddressWithDomainGroup(group, Arrays.asList("deployment", name)));

                steps.add(params);
            }
        }

        params = ParametersMap.newMap()
                .add("operation", "composite")
                .add("steps", steps);

        TalkToJBossServerTask task = new TalkToJBossServerTask(context, server, callback);
        task.execute(params);
    }

    public void changeStatus(final String host, final String serverName, final boolean start, final Callback callback) {
        ParametersMap params = ParametersMap.newMap()
                .add("operation", (start ? "start" : "stop"))
                .add("address", Arrays.asList("host", host, "server-config", serverName))
                .add("blocking", Boolean.TRUE);

        TalkToJBossServerTask task = new TalkToJBossServerTask(context, server, callback);
        task.execute(params);

    }

    public void genericRequest(final ParametersMap params, final boolean processRequest, final Callback callback) {
        TalkToJBossServerTask task = new TalkToJBossServerTask(context, server, callback, processRequest);
        task.execute(params);
    }

    private List<Map<String, Map<String, String>>> wrapToList(Map<String, Map<String, String>> map) {
        List<Map<String, Map<String, String>>> list = new ArrayList<Map<String, Map<String, String>>>();
        list.add(map);

        return list;
    }
}

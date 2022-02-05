/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.infinity.luix.core.listener;

import org.infinity.luix.core.url.Url;

import java.util.List;


/**
 * Listener used to handle the subscribed event to one consumer.
 */
public interface ProviderDiscoveryListener {

    /**
     * Called by the event which is subscribed.
     *
     * @param registryUrl   registry url
     * @param interfaceName interface name
     * @param providerUrls  provider urls
     */
    void onNotify(Url registryUrl, String interfaceName, List<Url> providerUrls);

}

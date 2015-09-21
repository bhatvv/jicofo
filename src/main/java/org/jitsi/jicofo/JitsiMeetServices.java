/*
 * Jicofo, the Jitsi Conference Focus.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
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
package org.jitsi.jicofo;

import net.java.sip.communicator.impl.protocol.jabber.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.colibri.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jirecon.*;
import net.java.sip.communicator.util.*;

import org.jitsi.jicofo.util.*;
import org.jitsi.protocol.xmpp.*;

import java.util.*;

/**
 * Class manages discovered components discovery of Jitsi Meet application
 * services like bridge, recording, SIP gateway and so on...
 *
 * @author Pawel Domas
 */
public class JitsiMeetServices
{
    /**
     * The logger
     */
    private final static Logger logger
        = Logger.getLogger(JitsiMeetServices.class);

    public static final String BRIDGE_TO_PUBSUB_PNAME
        = "org.jitsi.focus.PUBSUB_STATS_NODE";

    /**
     * Feature set advertised by videobridge.
     */
    public static final String[] VIDEOBRIDGE_FEATURES = new String[]
        {
            ColibriConferenceIQ.NAMESPACE,
            ProtocolProviderServiceJabberImpl
                .URN_XMPP_JINGLE_DTLS_SRTP,
            ProtocolProviderServiceJabberImpl
                .URN_XMPP_JINGLE_ICE_UDP_1,
            ProtocolProviderServiceJabberImpl
                .URN_XMPP_JINGLE_RAW_UDP_0
        };

    /**
     * Features advertised by Jirecon recorder container.
     */
    private static final String[] JIRECON_RECORDER_FEATURES = new String[]
        {
            JireconIqProvider.NAMESPACE
        };

    /**
     * Features advertised by SIP gateway component.
     */
    private static final String[] SIP_GW_FEATURES = new String[]
        {
            "http://jitsi.org/protocol/jigasi",
            "urn:xmpp:rayo:0"
        };

    /**
     * Features used to recognize pub-sub service.
     */
    /*private static final String[] PUBSUB_FEATURES = new String[]
        {
            "http://jabber.org/protocol/pubsub",
            "http://jabber.org/protocol/pubsub#subscribe"
        };*/

    /**
     * Manages Jitsi Videobridge component XMPP addresses.
     */
    private final BridgeSelector bridgeSelector;

    /**
     * Jirecon recorder component XMPP address.
     */
    private String jireconRecorder;

    /**
     * SIP gateway component XMPP address.
     */
    private String sipGateway;

    /**
     * Returns <tt>true</tt> if given list of features complies with JVB feature
     * list.
     * @param features the list of feature to be checked.
     */
    static public boolean isJitsiVideobridge(List<String> features)
    {
        return DiscoveryUtil.checkFeatureSupport(VIDEOBRIDGE_FEATURES, features);
    }

    /**
     * Creates new instance of <tt>JitsiMeetServices</tt>
     *
     * @param operationSet subscription operation set to be used for watching
     *                     JVB stats sent over pub-sub.
     */
    public JitsiMeetServices(OperationSetSubscription operationSet)
    {
        this.bridgeSelector = new BridgeSelector(operationSet);
    }

    /**
     * Called by other classes when they detect JVB instance.
     * @param bridgeJid the JID of discovered JVB component.
     */
    void newBridgeDiscovered(String bridgeJid)
    {
        bridgeSelector.addJvbAddress(bridgeJid);
    }

    /**
     * Call when new component becomes available.
     *
     * @param node component XMPP address
     * @param features list of features supported by <tt>node</tt>
     */
    void newNodeDiscovered(String node, List<String> features)
    {
        if (isJitsiVideobridge(features))
        {
            newBridgeDiscovered(node);
        }
        else if (
            jireconRecorder == null
                && DiscoveryUtil.checkFeatureSupport(
                        JIRECON_RECORDER_FEATURES, features))
        {
            logger.info("Discovered Jirecon recorder: " + node);

            setJireconRecorder(node);
        }
        else if (sipGateway == null
            && DiscoveryUtil.checkFeatureSupport(SIP_GW_FEATURES, features))
        {
            logger.info("Discovered SIP gateway: " + node);

            setSipGateway(node);
        }
        /*
        FIXME: pub-sub service auto-detect ?
        else if (capsOpSet.hasFeatureSupport(item, PUBSUB_FEATURES))
        {
            // Potential PUBSUB service
            logger.info("Potential PUBSUB service:" + item);
            List<String> subItems = capsOpSet.getItems(item);
            for (String subItem: subItems)
            {
                logger.info("Subnode " + subItem + " of " + item);
                capsOpSet.hasFeatureSupport(
                    item, subItem, VIDEOBRIDGE_FEATURES);
            }
        }*/
    }

    /**
     * Call when components goes offline.
     *
     * @param node XMPP address of disconnected XMPP component.
     */
    void nodeNoLongerAvailable(String node)
    {
        if (bridgeSelector.isJvbOnTheList(node))
        {
            bridgeSelector.removeJvbAddress(node);
        }
        else if (node.equals(jireconRecorder))
        {
            logger.warn("Jirecon recorder went offline: " + node);

            jireconRecorder = null;
        }
        else if (node.equals(sipGateway))
        {
            logger.warn("SIP gateway went offline: " + node);

            sipGateway = null;
        }
    }

    /**
     * Sets new XMPP address of the SIP gateway component.
     * @param sipGateway the XMPP address to be set as SIP gateway component
     *                   address.
     */
    void setSipGateway(String sipGateway)
    {
        this.sipGateway = sipGateway;
    }

    /**
     * Returns XMPP address of SIP gateway component.
     */
    public String getSipGateway()
    {
        return sipGateway;
    }

    /**
     * Sets new XMPP address of the Jirecon jireconRecorder component.
     * @param jireconRecorder the XMPP address to be set as Jirecon recorder
     *                        component address.
     */
    public void setJireconRecorder(String jireconRecorder)
    {
        this.jireconRecorder = jireconRecorder;
    }

    /**
     * Returns the XMPP address of Jirecon recorder component.
     */
    public String getJireconRecorder()
    {
        return jireconRecorder;
    }

    /**
     * Returns {@link BridgeSelector} bound to this instance that can be used to
     * select the videobridge on the xmppDomain handled by this instance.
     */
    public BridgeSelector getBridgeSelector()
    {
        return bridgeSelector;
    }
}

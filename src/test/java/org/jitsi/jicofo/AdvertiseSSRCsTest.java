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

import mock.*;
import mock.muc.*;
import mock.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.colibri.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jitsimeet.*;

import net.java.sip.communicator.util.*;
import org.jitsi.jicofo.osgi.*;

import org.jitsi.protocol.xmpp.util.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 */
@RunWith(JUnit4.class)
public class AdvertiseSSRCsTest
{
    private static final Logger logger
        = Logger.getLogger(AdvertiseSSRCsTest.class);

    static OSGiHandler osgi = new OSGiHandler();

    @BeforeClass
    public static void setUpClass()
        throws InterruptedException
    {
        OSGi.setUseMockProtocols(true);

        osgi.init();
    }

    @AfterClass
    public static void tearDownClass()
    {
        osgi.shutdown();
    }

    @Test
    public void testOneToOneConference()
        throws Exception
    {
        //FIXME: test when there is participant without contents

        String roomName = "testSSRCs@conference.pawel.jitsi.net";
        String serverName = "test-server";

        TestConference testConf = new TestConference();
        testConf.allocateMockConference(osgi, serverName, roomName);

        MockProtocolProvider pps
            = testConf.getFocusProtocolProvider();

        MockMultiUserChatOpSet mucOpSet = pps.getMockChatOpSet();

        MockMultiUserChat chat
            = (MockMultiUserChat) mucOpSet.findRoom(roomName);

        // Join with all users
        MockParticipant user1 = new MockParticipant("User1");
        user1.setSsrcVideoType(SSRCInfoPacketExtension.CAMERA_VIDEO_TYPE);
        user1.join(chat);

        MockParticipant user2 = new MockParticipant("User2");
        user2.setSsrcVideoType(SSRCInfoPacketExtension.SCREEN_VIDEO_TYPE);
        user2.join(chat);

        // Accept invite with all users
        assertNotNull(user1.acceptInvite(4000));
        assertNotNull(user2.acceptInvite(4000));

        user1.waitForAddSource(2000);
        user2.waitForAddSource(2000);

        assertEquals(1, user1.getRemoteSSRCs("audio").size());
        // No groups
        assertEquals(0, user1.getRemoteSSRCGroups("audio").size());

        // Verify SSRC owners and video types
        // From user 1 perspective
        assertEquals(
            user2.getMyJid(),
            SSRCSignaling.getSSRCOwner(user1.getRemoteSSRCs("audio").get(0)));
        assertEquals(
            user2.getMyJid(),
            SSRCSignaling.getSSRCOwner(user1.getRemoteSSRCs("video").get(0)));
        assertEquals(
            user2.getSsrcVideoType(),
            SSRCSignaling.getVideoType(user1.getRemoteSSRCs("video").get(0)));
        // From user 2 perspective
        assertEquals(
            user1.getMyJid(),
            SSRCSignaling.getSSRCOwner(user2.getRemoteSSRCs("audio").get(0)));
        assertEquals(
            user1.getMyJid(),
            SSRCSignaling.getSSRCOwner(user2.getRemoteSSRCs("video").get(0)));
        assertEquals(
            user1.getSsrcVideoType(),
            SSRCSignaling.getVideoType(user2.getRemoteSSRCs("video").get(0)));

        user2.leave();

        assertEquals(0, user1.getRemoteSSRCs("audio").size());
        // No groups
        assertEquals(0, user1.getRemoteSSRCGroups("audio").size());

        MockParticipant user3 = new MockParticipant("User3");
        user3.join(chat);
        assertNotNull(user3.acceptInvite(4000));

        user1.waitForAddSource(2000);

        assertEquals(1, user1.getRemoteSSRCs("audio").size());
        // No groups
        assertEquals(0, user1.getRemoteSSRCGroups("audio").size());

        user3.leave();
        user1.leave();
    }

    @Test
    public void testOneToOneSSRCGroupsConference()
        throws Exception
    {
        String roomName = "testSSRCs@conference.pawel.jitsi.net";
        String serverName = "test-server";

        TestConference testConf = new TestConference();
        testConf.allocateMockConference(osgi, serverName, roomName);

        MockProtocolProvider pps
            = testConf.getFocusProtocolProvider();

        MockMultiUserChatOpSet mucOpSet = pps.getMockChatOpSet();

        MockMultiUserChat chat
            = (MockMultiUserChat) mucOpSet.findRoom(roomName);

        // Join with all users
        MockParticipant user1 = new MockParticipant("User1");
        user1.setUseSsrcGroups(true);

        MockParticipant user2 = new MockParticipant("User2");
        user2.setUseSsrcGroups(true);

        user1.join(chat);
        user2.join(chat);

        // Accept invite with all users
        assertNotNull(user1.acceptInvite(4000));
        assertNotNull(user2.acceptInvite(4000));

        user1.waitForAddSource(2000);

        assertEquals(1, user1.getRemoteSSRCs("audio").size());
        assertEquals(2, user1.getRemoteSSRCs("video").size());
        // groups
        assertEquals(0, user1.getRemoteSSRCGroups("audio").size());
        assertEquals(1, user1.getRemoteSSRCGroups("video").size());

        // Check if layers are up-to-date on the bridge
        verifySimulcastLayersOnTheBridge(testConf, user1);
        verifySimulcastLayersOnTheBridge(testConf, user2);

        logger.info("Switching to desktop stream");

        // Test video stream switch(for desktop sharing)
        long [] desktopSSRC = new long[1];
        desktopSSRC[0] = MockParticipant.nextSSRC();
        user2.switchVideoSSRCs(desktopSSRC, false);
        // Wait for update
        user1.waitForAddSource(5000);
        // Check one SSRC is received and no groups
        // FIXME: fix tests failing randomly at this point with 3 SSRCs
        // looks like some issue when sending source-remove
        assertEquals(1, user1.getRemoteSSRCs("video").size());
        assertEquals(0, user1.getRemoteSSRCGroups("video").size());
        // Verify on the bridge
        verifyNOSimulcastLayersOnTheBridge(testConf, user2);

        logger.info("Switching back to camera stream");

        // Restore video stream
        long[] videoSSRCs = new long[2];
        videoSSRCs[0] = MockParticipant.nextSSRC();
        videoSSRCs[1] = MockParticipant.nextSSRC();
        user2.switchVideoSSRCs(videoSSRCs, true);
        // Wait for update
        user1.waitForAddSource(2000);
        // Check 2 SSRCs are received and 1 group
        assertEquals(2, user1.getRemoteSSRCs("video").size());
        assertEquals(1, user1.getRemoteSSRCGroups("video").size());
        // Verify on the bridge
        verifySimulcastLayersOnTheBridge(testConf, user2);

        // User2 - quit
        user2.leave();

        assertEquals(0, user1.getRemoteSSRCs("audio").size());
        // No groups
        assertEquals(0, user1.getRemoteSSRCGroups("audio").size());

        // This one has no groups
        MockParticipant user3 = new MockParticipant("User3");
        user3.join(chat);
        assertNotNull(user3.acceptInvite(4000));

        user1.waitForAddSource(2000);

        assertEquals(1, user1.getRemoteSSRCs("audio").size());
        assertEquals(1, user1.getRemoteSSRCs("video").size());
        // No groups
        assertEquals(0, user1.getRemoteSSRCGroups("audio").size());
        assertEquals(0, user1.getRemoteSSRCGroups("video").size());

        user3.leave();
        user1.leave();
    }

    /**
     * Verifies if number of simulcast layers on the bridge matches the SSRCs
     * count in local video group. Also checks if primary SSRCs of particular
     * layers do match local video SSRCs.
     * @param testConference instance of <tt>TestConference</tt> that will be
     *                       used for obtaining videobridge backend.
     * @param peer the <tt>MockParticipant</tt> for which simulcast layers will
     *             be verified.
     */
    private void verifySimulcastLayersOnTheBridge(TestConference testConference,
                                                  MockParticipant peer)
    {
        long[] simulcastLayersSSRCs
            = testConference.getSimulcastLayersSSRCs(peer.getMyJid());
        List<SourcePacketExtension> videoSSRCs = peer.getVideoSSRCS();

        assertEquals(videoSSRCs.size(), simulcastLayersSSRCs.length);

        for (int i=0; i<videoSSRCs.size(); i++)
        {
            assertEquals(
                "idx: " + i,
                videoSSRCs.get(i).getSSRC(),
                simulcastLayersSSRCs[i]);
        }
    }

    /**
     * Verifies if the are 0 simulcast layers on the bridge for given
     * <tt>peer</tt>.
     * @param testConference instance of <tt>TestConference</tt> that will be
     *                       used for obtaining videobridge backend.
     * @param peer the <tt>MockParticipant</tt> for which simulcast layers will
     *             be verified.
     */
    private void verifyNOSimulcastLayersOnTheBridge(
            TestConference testConference, MockParticipant peer)
    {
        long[] simulcastLayersSSRCs
            = testConference.getSimulcastLayersSSRCs(peer.getMyJid());

        assertEquals(0, simulcastLayersSSRCs.length);
    }
}

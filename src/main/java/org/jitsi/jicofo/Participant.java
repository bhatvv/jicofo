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

import net.java.sip.communicator.impl.protocol.jabber.extensions.colibri.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.service.protocol.*;

import org.jitsi.jicofo.util.*;
import org.jitsi.protocol.xmpp.*;
import org.jitsi.protocol.xmpp.util.*;

import java.util.*;

/**
 * Class represent Jitsi Meet conference participant. Stores information about
 * Colibri channels allocated, Jingle session and media SSRCs.
 *
 * @author Pawel Domas
 */
public class Participant
{
    /**
     * MUC chat member of this participant.
     */
    private final XmppChatMember roomMember;

    /**
     * Jingle session(if any) established with this peer.
     */
    private JingleSession jingleSession;

    /**
     * Information about Colibri channels allocated for this peer(if any).
     */
    private ColibriConferenceIQ colibriChannelsInfo;

    /**
     * Peer's media SSRCs.
     */
    private MediaSSRCMap ssrcs = new MediaSSRCMap();

    /**
     * Peer's media SSRC groups.
     */
    private MediaSSRCGroupMap ssrcGroups = new MediaSSRCGroupMap();

    /**
     * SSRCs received from other peers scheduled for later addition, because
     * of the Jingle session not being ready at the point when SSRCs appeared in
     * the conference.
     */
    private MediaSSRCMap ssrcsToAdd = new MediaSSRCMap();

    /**
     * SSRC groups received from other peers scheduled for later addition.
     * @see #ssrcsToAdd
     */
    private MediaSSRCGroupMap ssrcGroupsToAdd = new MediaSSRCGroupMap();

    /**
     * SSRCs received from other peers scheduled for later removal, because
     * of the Jingle session not being ready at the point when SSRCs appeared in
     * the conference.
     * FIXME: do we need that since these were never added ? - check
     */
    private MediaSSRCMap ssrcsToRemove = new MediaSSRCMap();

    /**
     * SSRC groups received from other peers scheduled for later removal.
     * @see #ssrcsToRemove
     */
    private MediaSSRCGroupMap ssrcGroupsToRemove = new MediaSSRCGroupMap();

    /**
     * The list of XMPP features supported by this participant. 
     */
    private List<String> supportedFeatures = new ArrayList<String>();

    /**
     * Remembers participant's muted status.
     */
    private boolean mutedStatus;
    
    
    /**
     * Remembers participant's hold status.
     */
    private boolean holdStatus;
    

    /**
     *
     */
    private String displayName = null;

    /** The participant jabberid. */
    private String jabberid;

    /**
     * Returns the endpoint ID for a participant in the videobridge(Colibri)
     * context. This method can be used before <tt>Participant</tt> instance is
     * created for the <tt>ChatRoomMember</tt>.
     *
     * @param chatRoomMember XMPP MUC chat room member which represent a
     *                       <tt>Participant</tt>.
     */
    static public String getEndpointId(ChatRoomMember chatRoomMember)
    {
        return chatRoomMember.getName(); // XMPP MUC Nickname
    }

    /**
     * Creates new {@link Participant} for given chat room member.
     *
     * @param roomMember the {@link XmppChatMember} that represent this
     *                   participant in MUC conference room.
     */
    public Participant(XmppChatMember roomMember)
    {
        if (roomMember == null)
        {
            throw new NullPointerException("roomMember");
        }
        this.roomMember = roomMember;
    }

    /**
     * Returns {@link JingleSession} established with this conference
     * participant or <tt>null</tt> if there is no session yet.
     */
    public JingleSession getJingleSession()
    {
        return jingleSession;
    }

    /**
     * Sets {@link JingleSession} established with this peer.
     * @param jingleSession the new Jingle session to be assigned to this peer.
     */
    public void setJingleSession(JingleSession jingleSession)
    {
        this.jingleSession = jingleSession;
    }

    /**
     * Returns {@link XmppChatMember} that represents this participant in
     * conference multi-user chat room.
     */
    public XmppChatMember getChatMember()
    {
        return roomMember;
    }

    /**
     * Imports media SSRCs from given list of <tt>ContentPacketExtension</tt>.
     * @param answer the list that contains peer's media contents.
     */
    public void addSSRCsFromContent(List<ContentPacketExtension> answer)
    {
        // Configure SSRC owner in 'ssrc-info' with user's MUC Jid
        MediaSSRCMap peerSSRCs = MediaSSRCMap.getSSRCsFromContent(answer);
        for (String mediaType : peerSSRCs.getMediaTypes())
        {
            List<SourcePacketExtension> mediaSsrcs
                = peerSSRCs.getSSRCsForMedia(mediaType);

            for (SourcePacketExtension ssrcPe : mediaSsrcs)
            {
                SSRCSignaling.setSSRCOwner(
                    ssrcPe, roomMember.getContactAddress());
            }
        }
        // Store SSRCs
        ssrcs.add(peerSSRCs);
    }

    /**
     * Removes given media SSRCs from this peer state.
     * @param ssrcMap the SSRC map that contains the SSRCs to be removed.
     */
    public void removeSSRCs(MediaSSRCMap ssrcMap)
    {
        this.ssrcs.remove(ssrcMap);
    }

    /**
     * Returns the {@link MediaSSRCMap} which contains this peer's media SSRCs.
     */
    public MediaSSRCMap getSSRCS()
    {
        return ssrcs;
    }

    /**
     * Returns shallow copy of this peer's media SSRC map.
     */
    public MediaSSRCMap getSSRCsCopy()
    {
        return ssrcs.copyShallow();
    }

    /**
     * Returns deep copy of this peer's media SSRC group map.
     */
    public MediaSSRCGroupMap getSSRCGroupsCopy()
    {
        return ssrcGroups.copy();
    }

    /**
     * Returns <tt>true</tt> if this peer has any not synchronized SSRCs
     * scheduled for addition.
     */
    public boolean hasSsrcsToAdd()
    {
        return !ssrcsToAdd.isEmpty() || !ssrcGroupsToAdd.isEmpty();
    }

    /**
     * Reset the queue that holds not synchronized SSRCs scheduled for future
     * addition.
     */
    public void clearSsrcsToAdd()
    {
        ssrcsToAdd = new MediaSSRCMap();
        ssrcGroupsToAdd = new MediaSSRCGroupMap();
    }

    /**
     * Reset the queue that holds not synchronized SSRCs scheduled for future
     * removal.
     */
    public void clearSsrcsToRemove()
    {
        ssrcsToRemove = new MediaSSRCMap();
        ssrcGroupsToRemove = new MediaSSRCGroupMap();
    }

    /**
     * Returns <tt>true</tt> if this peer has any not synchronized SSRCs
     * scheduled for removal.
     */
    public boolean hasSsrcsToRemove()
    {
        return !ssrcsToRemove.isEmpty() || !ssrcGroupsToRemove.isEmpty();
    }

    /**
     * Returns <tt>true</tt> if this peer has any not synchronized SSRCs
     * scheduled for addition.
     */
    public MediaSSRCMap getSsrcsToAdd()
    {
        return ssrcsToAdd;
    }

    /**
     * Returns <tt>true</tt> if this peer has any not synchronized SSRCs
     * scheduled for removal.
     */
    public MediaSSRCMap getSsrcsToRemove()
    {
        return ssrcsToRemove;
    }

    /**
     * Schedules SSRCs received from other peer for future 'source-add' update.
     *
     * @param ssrcMap the media SSRC map that contains SSRCs for future updates.
     */
    public void scheduleSSRCsToAdd(MediaSSRCMap ssrcMap)
    {
        ssrcsToAdd.add(ssrcMap);
    }

    /**
     * Schedules SSRCs received from other peer for future 'source-remove'
     * update.
     *
     * @param ssrcMap the media SSRC map that contains SSRCs for future updates.
     */
    public void scheduleSSRCsToRemove(MediaSSRCMap ssrcMap)
    {
        ssrcsToRemove.add(ssrcMap);
    }

    /**
     * Sets information about Colibri channels allocated for this participant.
     *
     * @param colibriChannelsInfo the IQ that holds colibri channels state.
     */
    public void setColibriChannelsInfo(ColibriConferenceIQ colibriChannelsInfo)
    {
        this.colibriChannelsInfo = colibriChannelsInfo;
    }

    /**
     * Returns {@link ColibriConferenceIQ} that describes Colibri channels
     * allocated for this participant.
     */
    public ColibriConferenceIQ getColibriChannelsInfo()
    {
        return colibriChannelsInfo;
    }

    /**
     * Returns <tt>true</tt> if this participant supports RTP bundle and RTCP
     * mux.
     */
    public boolean hasBundleSupport()
    {
        return supportedFeatures.contains(DiscoveryUtil.FEATURE_RTCP_MUX)
                && supportedFeatures.contains(DiscoveryUtil.FEATURE_RTP_BUNDLE);
    }

    /**
     * Returns <tt>true</tt> if this participant supports DTLS.
     */
    public boolean hasDtlsSupport()
    {
        return supportedFeatures.contains(DiscoveryUtil.FEATURE_DTLS);
    }

    /**
     * FIXME: we need to remove "is SIP gateway code", but there are still 
     * situations where we need to know whether given peer is a human or not.
     * For example when we close browser window and only SIP gateway stays
     * we should destroy the conference and close SIP connection.
     *  
     * Returns <tt>true</tt> if this participant belongs to SIP gateway service.
     */
    public boolean isSipGateway()
    {
        return supportedFeatures.contains(
                "http://jitsi.org/protocol/jigasi");
    }

    /**
     * Returns <tt>true</tt> if RTP audio is supported by this peer.
     */
    public boolean hasAudioSupport()
    {
        return supportedFeatures.contains(DiscoveryUtil.FEATURE_AUDIO);
    }

    /**
     * Returns <tt>true</tt> if RTP video is supported by this peer.
     */
    public boolean hasVideoSupport()
    {
        return supportedFeatures.contains(DiscoveryUtil.FEATURE_VIDEO);
    }

    /**
     * Returns <tt>true</tt> if this peer supports ICE transport.
     */
    public boolean hasIceSupport()
    {
        return supportedFeatures.contains(DiscoveryUtil.FEATURE_ICE);
    }

    /**
     * Returns <tt>true</tt> if this peer supports DTLS/SCTP. 
     */
    public boolean hasSctpSupport()
    {
        return supportedFeatures.contains(DiscoveryUtil.FEATURE_SCTP);
    }

    /**
     * Sets the list of features supported by this participant.
     * @see DiscoveryUtil for the list of predefined feature constants. 
     * @param supportedFeatures the list of features to set.
     */
    public void setSupportedFeatures(List<String> supportedFeatures)
    {
        if (supportedFeatures == null)
        {
            throw new NullPointerException("supportedFeatures");
        }

        this.supportedFeatures = supportedFeatures;
    }

    /**
     * Sets muted status of this participant.
     * @param mutedStatus new muted status to set.
     */
    public void setMuted(boolean mutedStatus)
    {
        this.mutedStatus = mutedStatus;
    }

    /**
     * Sets hold status of this participant.
     * @param holdStatus new hold status to set.
     */
    public void setHold(boolean holdStatus)
    {
        this.holdStatus = holdStatus;
    }
    
    
    /**
     * Returns <tt>true</tt> if this participant is muted or <tt>false</tt>
     * otherwise.
     */
    public boolean isMuted()
    {
        return mutedStatus;
    }
    
    /**
     * Returns <tt>true</tt> if this participant is hold or <tt>false</tt>
     * otherwise.
     */
    public boolean isHold()
    {
        return holdStatus;
    }

    /**
     * Returns the list of SSRC groups of given media type that belong ot this
     * participant.
     * @param media the name of media type("audio","video", ...)
     * @return the list of {@link SSRCGroup} for given media type.
     */
    public List<SSRCGroup> getSSRCGroupsForMedia(String media)
    {
        return ssrcGroups.getSSRCGroupsForMedia(media);
    }

    /**
     * Returns <tt>MediaSSRCGroupMap</tt> that contains the mapping of media
     * SSRC groups that describe media of this participant.
     */
    public MediaSSRCGroupMap getSSRCGroups()
    {
        return ssrcGroups;
    }

    /**
     * Adds SSRC groups for media described in given Jiongle content list.
     * @param contents the list of <tt>ContentPacketExtension</tt> that
     *                 describes media SSRC groups.
     */
    public void addSSRCGroupsFromContent(List<ContentPacketExtension> contents)
    {
        for (ContentPacketExtension content : contents)
        {
            List<SSRCGroup> groups
                = SSRCGroup.getSSRCGroupsForContent(content);

            ssrcGroups.addSSRCGroups(content.getName(), groups);
        }
    }

    /**
     * Schedules given media SSRC groups for later addition.
     * @param ssrcGroups the <tt>MediaSSRCGroupMap</tt> to be scheduled for
     *                   later addition.
     */
    public void scheduleSSRCGroupsToAdd(MediaSSRCGroupMap ssrcGroups)
    {
        ssrcGroupsToAdd.add(ssrcGroups);
    }

    /**
     * Schedules given media SSRC groups for later removal.
     * @param ssrcGroups the <tt>MediaSSRCGroupMap</tt> to be scheduled for
     *                   later removal.
     */
    public void scheduleSSRCGroupsToRemove(MediaSSRCGroupMap ssrcGroups)
    {
        ssrcGroupsToRemove.add(ssrcGroups);
    }

    /**
     * Returns the map of SSRC groups that are waiting for synchronization.
     */
    public MediaSSRCGroupMap getSSRCGroupsToAdd()
    {
        return ssrcGroupsToAdd;
    }

    /**
     * Returns the map of SSRC groups that are waiting for being removed from
     * peer session.
     */
    public MediaSSRCGroupMap getSsrcGroupsToRemove()
    {
        return ssrcGroupsToRemove;
    }

    /**
     * Removes SSRC groups from this participant state.
     * @param ssrcGroupsToRemove the map of SSRC groups that will be removed
     *                           from this participant media state description.
     */
    public void removeSSRCGroups(MediaSSRCGroupMap ssrcGroupsToRemove)
    {
        this.ssrcGroups.remove(ssrcGroupsToRemove);
    }

    /**
     * Returns the endpoint ID for this participant in the videobridge(Colibri)
     * context.
     */
    public String getEndpointId()
    {
        return getEndpointId(roomMember);
    }

    /**
     * Returns the display name of the participant.
     * @return the display name of the participant.
     */
    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * Sets the display name of the participant.
     * @param displayName the display name to set.
     */
    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    /**
     * Gets the participant jabberid.
     *
     * @return the jabberid
     */
    public String getJabberid() 
    {
	return jabberid;
    }

    /**
     * Sets the participantjabberid.
     *
     * @param jabberid
     *            the new jabberid
     */
    public void setJabberid(String jabberid) 
    {
	this.jabberid = jabberid;
    }

}

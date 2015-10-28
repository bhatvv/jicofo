package org.jitsi.impl.protocol.xmpp.extensions;

import org.jitsi.util.*;

import net.java.sip.communicator.util.Logger;

/**
 * The Class PrivateIQ.
 */
public class PrivateIQ extends AbstractIQ {

	/** The logger. */
	public static Logger logger = Logger.getLogger(PrivateIQ.class);
	/**
	 * Name space of private packet extension.
	 */
	public static final String NAMESPACE = "jabber:iq:private";

	/** XML element name of private packet extension. */
	public static final String QUERY_ELEMENT_NAME = "query";

	/** XML element name of query. */
	public static final String MEDIA_ELEMENT_NAME = "media";

	/** XML element name of media. */
	public static final String INFO_ELEMENT_NAME = "info";
	
	/** XML element name of data. */
	public static final String DATA_ELEMENT_NAME = "data";

	/** Attribute name jid. */
	public static final String JID_ATTR_NAME = "jid";

	/** Attribute name room. */
	public static final String ROOM_ATTR_NAME = "room";

	/** Attribute name jabberid. */
	public static final String ROUTING_ATTR_NAME = "jabberid";

	/** Attribute name action. */
	public static final String ACTION_ATTR_NAME = "action";

	/** Attribute name media. */
	public static final String MEDIA_ATTR_NAME = "media";
	
	/** Attribute name destJid. */
	public static final String DESTJID_ATTR_NAME = "destJid";
	
	/** Attribute name holdUser. */
	public static final String HOLDUSER_ATTR_NAME = "holdUser";
	
	/** Attribute name value of data. */
	public static final String VALUE_ATTR_NAME = "value";
	
	/** The jid. */
	private String jid;

	/** The room. */
	private String room;

	/** The jabberid. */
	private String jabberid;

	/** The action. */
	private String action;

	/** The media. */
	private String media;

	/** The audio support. */
	private boolean audioSupport;

	/** The video support. */
	private boolean videoSupport;

	/** The connected. */
	private boolean connected;
	
	/** Indicates if the participant is sip. */
	private boolean sipCall;
	
	/**  Destination jid sent to client for sip calls. */
	private String destJid;
	
	/**  Jid of the user thats going on hold. */
	private String holdUser;
	
	/**  Statistics data value parameter. */
	private String value;
	
	/**  Indicates if the participant is on hold. */
	private Boolean isOnHold;
	
	/**
	 * The Enum mediaValues.
	 */
	private enum mediaValues {

		/** Only Audio. */
		Audio,
		/** Only Video. */
		Video,
		/** Both Audio video. */
		AudioVideo,
		/** None. */
		None
	};

	/**
	 * Instantiates a new private iq.
	 */
	public PrivateIQ() {
		super(NAMESPACE, QUERY_ELEMENT_NAME);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jitsi.impl.protocol.xmpp.extensions.AbstractIQ#getChildElementXML()
	 */
	@Override
	public String getChildElementXML() {
		if (isConnected()) {
			setAction("connected");
		} else {
			setAction("disconnected");
		}

		if (hasAudioSupport() && hasVideoSupport()) {
			setMedia(mediaValues.AudioVideo.toString());
		} else if (hasAudioSupport() && !hasVideoSupport()) {
			setMedia(mediaValues.Audio.toString());
		} else if (!hasAudioSupport() && hasVideoSupport()) {
			setMedia(mediaValues.Video.toString());
		} else {
			setMedia(mediaValues.None.toString());
		}

		StringBuilder output = new StringBuilder();
		output.append("<" + QUERY_ELEMENT_NAME + " xmlns='jabber:iq:private'").append(">")
		    .append("<" + MEDIA_ELEMENT_NAME + " xmlns='media:prefs'").append(">")
		    .append("<" + INFO_ELEMENT_NAME + " " + ROUTING_ATTR_NAME + "='").append(getJabberid())
		    .append("' " + MEDIA_ATTR_NAME + "='").append(getMedia())
		    .append("' " + ACTION_ATTR_NAME + "='").append(getAction())
		    .append(isSipCall() ? "' " + DESTJID_ATTR_NAME + "='" + getDestjid() : "")
		    .append(isOnHold() != null ? "' " + HOLDUSER_ATTR_NAME + "='" + getHoldUser() : "")
		    .append("' />").append("</media>")
		    .append(StringUtils.isNullOrEmpty(value) ? 
		    		"" : "<" + DATA_ELEMENT_NAME + " " + VALUE_ATTR_NAME + "='" + getValue() + "' />")
		    .append("</" + QUERY_ELEMENT_NAME + ">");

		logger.info("PrivateIQ Child XML: " + output.toString());

		return output.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jitsi.impl.protocol.xmpp.extensions.AbstractIQ#printAttributes(java
	 * .lang.StringBuilder)
	 */
	@Override
	protected void printAttributes(StringBuilder out) {
		printStrAttr(out, ROOM_ATTR_NAME, room);
	}

	/**
	 * Gets the jid.
	 *
	 * @return the jid
	 */
	public String getJid() {
		return jid;
	}

	/**
	 * Sets the jid.
	 *
	 * @param jid
	 *            the new jid
	 */
	public void setJid(String jid) {
		this.jid = jid;
	}

	/**
	 * Gets the room.
	 *
	 * @return the room
	 */
	public String getRoom() {
		return room;
	}

	/**
	 * Sets the room.
	 *
	 * @param room
	 *            the new room
	 */
	public void setRoom(String room) {
		this.room = room;
	}

	/**
	 * Gets the jabberid.
	 *
	 * @return the jabberid
	 */
	public String getJabberid() {
		return jabberid;
	}

	/**
	 * Sets the jabberid.
	 *
	 * @param jabberid
	 *            the new jabberid
	 */
	public void setJabberid(String jabberid) {
		this.jabberid = jabberid;
	}

	/**
	 * Gets the action.
	 *
	 * @return the action
	 */
	public String getAction() {
		return action;
	}

	/**
	 * Sets the action.
	 *
	 * @param action
	 *            the new action
	 */
	public void setAction(String action) {
		this.action = action;
	}

	/**
	 * Gets the media.
	 *
	 * @return the media
	 */
	public String getMedia() {
		return media;
	}

	/**
	 * Sets the media.
	 *
	 * @param media
	 *            the new media
	 */
	public void setMedia(String media) {
		this.media = media;
	}

	/**
	 * Checks for audio support.
	 *
	 * @return true, if successful
	 */
	public boolean hasAudioSupport() {
		return audioSupport;
	}

	/**
	 * Sets the audio support.
	 *
	 * @param audioSupport
	 *            the new audio support
	 */
	public void setAudioSupport(boolean audioSupport) {
		this.audioSupport = audioSupport;
	}

	/**
	 * Checks for video support.
	 *
	 * @return true, if successful
	 */
	public boolean hasVideoSupport() {
		return videoSupport;
	}

	/**
	 * Sets the video support.
	 *
	 * @param videoSupport
	 *            the new video support
	 */
	public void setVideoSupport(boolean videoSupport) {
		this.videoSupport = videoSupport;
	}

	/**
	 * Checks if is connected.
	 *
	 * @return true, if is connected
	 */
	public boolean isConnected() {
		return connected;
	}

	/**
	 * Sets the connected.
	 *
	 * @param connected
	 *            the new connected
	 */
	public void setConnected(boolean connected) {
		this.connected = connected;
	}
	
	/**
	 * Checks if is sip call.
	 *
	 * @return true, if is sip call
	 */
	public boolean isSipCall() {
		return sipCall;
	}

	/**
	 * Sets the sip call.
	 *
	 * @param sipCall the new sip call
	 */
	public void setSipCall(boolean sipCall) {
		this.sipCall = sipCall;
	}

	/**
	 * Gets the destjid.
	 *
	 * @return the destjid
	 */
	public String getDestjid() {
		return destJid;
	}

	/**
	 * Sets the destjid.
	 *
	 * @param destjid the new destjid
	 */
	public void setDestjid(String destjid) {
		this.destJid = destjid;
	}

	/**
	 * @return the holdUser
	 */
	public String getHoldUser() {
		return holdUser;
	}

	/**
	 * @param holdUser the holdUser to set
	 */
	public void setHoldUser(String holdUser) {
		this.holdUser = holdUser;
	}

	/**
	 * Gets the value.
	 *
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Sets the value.
	 *
	 * @param value the new value
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * Checks if is on hold.
	 *
	 * @return the boolean
	 */
	public Boolean isOnHold() {
		return isOnHold;
	}

	/**
	 * Sets the on hold.
	 *
	 * @param isOnHold the new on hold
	 */
	public void setOnHold(Boolean isOnHold) {
		this.isOnHold = isOnHold;
	}

}

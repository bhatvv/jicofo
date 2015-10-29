package org.jitsi.impl.protocol.xmpp.extensions;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jitsi.util.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmpp.packet.IQ;

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
		output.append("<" + QUERY_ELEMENT_NAME + " xmlns=\"jabber:iq:private\"").append(">")
		    .append("<" + MEDIA_ELEMENT_NAME + " xmlns=\"media:prefs\"").append(">")
		    .append("<" + INFO_ELEMENT_NAME + " " + ROUTING_ATTR_NAME + "=\"").append(getJabberid())
		    .append("\" " + MEDIA_ATTR_NAME + "=\"").append(getMedia())
		    .append("\" " + ACTION_ATTR_NAME + "=\"").append(getAction())
		    .append(isSipCall() ? "\" " + DESTJID_ATTR_NAME + "=\"" + getDestjid() : "")
		    .append(isOnHold() != null ? "\" " + HOLDUSER_ATTR_NAME + "=\"" + getHoldUser() : "")
		    .append(StringUtils.isNullOrEmpty(value) ? 
		    		"\">" : "\"><" + DATA_ELEMENT_NAME + ">" + getValue() + "</" + DATA_ELEMENT_NAME + ">")
		    .append("</" + INFO_ELEMENT_NAME + ">").append("</media>")
		    .append("</" + QUERY_ELEMENT_NAME + ">");

		return output.toString();
	}
	
	/**
	 * Convert org.xmpp.packet.IQ to PrivateIQ.
	 *
	 * @param iq the org.xmpp.packet.IQ
	 * @return the private iq
	 */
	public static PrivateIQ convert(IQ iq)
	{
		PrivateIQ privateIQ = new PrivateIQ();
		String iqXml = iq.toXML();
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();

			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(iqXml));
			Document doc = dBuilder.parse(is);
			doc.getDocumentElement().normalize();

			Node iqNode = doc.getElementsByTagName("iq").item(0);
			Element iqElement = (Element) iqNode;
			privateIQ.setTo(iqElement.getAttribute("to"));
			privateIQ.setFrom(iqElement.getAttribute("from"));
			privateIQ.setType(iqElement.getAttribute("type").equalsIgnoreCase("set")
					? org.jivesoftware.smack.packet.IQ.Type.SET : org.jivesoftware.smack.packet.IQ.Type.GET);

			Node queryNode = iqElement.getElementsByTagName(QUERY_ELEMENT_NAME).item(0);
			Element queryElement = (Element) queryNode;
			Node mediaNode = queryElement.getElementsByTagName(MEDIA_ELEMENT_NAME).item(0);
			Element mediaElement = (Element) mediaNode;

			Node infoNode = mediaElement.getElementsByTagName(INFO_ELEMENT_NAME).item(0);
			Element infoElement = (Element) infoNode;
			privateIQ.setJabberid(infoElement.getAttribute(ROUTING_ATTR_NAME));

			String media = infoElement.getAttribute(MEDIA_ATTR_NAME);
			if (media.equals(mediaValues.AudioVideo.toString()))
			{
				privateIQ.setAudioSupport(true);
				privateIQ.setVideoSupport(true);
			}
			else if (media.equals(mediaValues.Audio.toString()))
			{
				privateIQ.setAudioSupport(true);
				privateIQ.setVideoSupport(false);
			}
			else if (media.equals(mediaValues.Video.toString()))
			{
				privateIQ.setAudioSupport(false);
				privateIQ.setVideoSupport(true);
			}
			else
			{
				privateIQ.setAudioSupport(false);
				privateIQ.setVideoSupport(false);
			}

			privateIQ.setConnected(infoElement.getAttribute(ACTION_ATTR_NAME)
				.equals("connected") ? true : false);

			Node dataNode = mediaElement.getElementsByTagName(DATA_ELEMENT_NAME).item(0);
			privateIQ.setValue(dataNode.getTextContent());
		}
		catch (ParserConfigurationException e)
		{
			logger.error("Error parsing private iq xml : ", e);
		}
		catch (SAXException e)
		{
			logger.error("Error parsing xml : ", e);
		}
		catch (IOException e)
		{
			logger.error("Error reading string input : ", e);
		}
		
		return privateIQ;
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

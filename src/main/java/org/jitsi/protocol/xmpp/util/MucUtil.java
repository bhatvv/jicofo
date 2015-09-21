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
package org.jitsi.protocol.xmpp.util;

/**
 * Utility class for XMPP MUC operations.
 *
 * @author Pawel Domas
 */
public class MucUtil
{
    /**
     * Extracts room name from the full MUC address. If given room name is
     * already in the simple form then it will be returned unmodified.
     *
     * @param roomName room name in the form of {room_name}@{muc.server.net}.
     *
     * @return room name extracted from full address: {room_name}@muc.server
     *         .net
     */
    public static String extractName(String roomName)
    {
        int atIdx = roomName.indexOf("@");
        if (atIdx != -1)
        {
            roomName = roomName.substring(0, atIdx);
        }
        return roomName;
    }
}

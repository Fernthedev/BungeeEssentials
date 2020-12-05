/*
 * BungeeEssentials: Full customization of a few necessary features for your server!
 * Copyright (C) 2016 David Shen (PantherMan594)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.pantherman594.gssentials.announcement;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.Collection;

public class Announcement {
    private final Integer delay;
    private final Integer interval;
    private final String msg;
    private final String server;

    /**
     * Sets up an announcement
     *
     * @param delay    The delay (seconds) before the announcement starts.
     * @param interval The interval (seconds) between announcements.
     * @param msg      The message in the announcement.
     * @param server   The server to broadcast the announcement.
     */
    public Announcement(Integer delay, Integer interval, String msg, String server) {
        this.delay = delay;
        this.interval = interval;
        this.msg = msg;
        this.server = server;
    }

    Integer getDelay() {
        return delay;
    }

    Integer getInterval() {
        return interval;
    }

    public String getMsg() {
        return msg;
    }

    /**
     * @return A list of players to announce to.
     */
    public Collection<ProxiedPlayer> getPlayers() {
        if (server.equals("ALL")) {
            return ProxyServer.getInstance().getPlayers();
        } else if (ProxyServer.getInstance().getServerInfo(server) != null) {
            return ProxyServer.getInstance().getServerInfo(server).getPlayers();
        }
        return new ArrayList<>();
    }
}
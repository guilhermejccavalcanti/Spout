/*
 * This file is part of Spout.
 *
 * Copyright (c) 2011 Spout LLC <http://www.spout.org/>
 * Spout is licensed under the Spout License Version 1.
 *
 * Spout is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * In addition, 180 days after any changes are published, you can use the
 * software, incorporating those changes, under the terms of the MIT license,
 * as described in the Spout License Version 1.
 *
 * Spout is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Lesser General Public License,
 * the MIT license and the Spout License Version 1 along with this program.
 * If not, see <http://www.gnu.org/licenses/> for the GNU Lesser General Public
 * License and see <http://spout.in/licensev1> for the full license, including
 * the MIT license.
 */
package org.spout.api.protocol;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.spout.api.Client;
import org.spout.api.Engine;
import org.spout.api.Platform;
import org.spout.api.Server;
import org.spout.api.Spout;

/**
 * A {@link SimpleChannelUpstreamHandler} which processes incoming network events.
 */
public class CommonHandler extends SimpleChannelInboundHandler<Message> {

    /**
	 * The server.
	 */
    private final Engine engine;

    /**
	 * The associated session
	 */
    private final AtomicReference<Session> session = new AtomicReference<>(null);

    /**
	 * Indicates if it is an upstream channel pipeline
	 */
    private final boolean onClient;

    private final CommonDecoder decoder;

    private final CommonEncoder encoder;

    public CommonHandler(CommonEncoder encoder, CommonDecoder decoder) {
        if (Spout.getPlatform() == Platform.CLIENT) {
            this.engine = (Client) Spout.getEngine();
            this.onClient = true;
        } else {
            this.engine = (Server) Spout.getEngine();
            this.onClient = false;
        }
        this.encoder = encoder;
        this.decoder = decoder;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        final Channel c = ctx.channel();
        if (onClient) {
            engine.getLogger().info("Upstream channel connected: " + c + ".");
        } else {
            try {
                Server server = (Server) engine;
                server.getChannelGroup().add(c);
                Session session = server.newSession(c);
                server.getSessionRegistry().add(session);
                setSession(session);
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new RuntimeException("Exception thrown when connecting", ex);
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        try {
            Channel c = ctx.channel();
            Session session = this.session.get();
            if (!onClient) {
                Server server = (Server) engine;
                server.getChannelGroup().remove(c);
                server.getSessionRegistry().remove(session);
            }
            if (session.isPrimary(c)) {
                session.dispose();
            }
        } catch (Exception ex) {
            throw new RuntimeException("Exception thrown when disconnecting", ex);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message i) {
        Session session = this.session.get();
        if (session.isPrimary(ctx.channel())) {
            session.messageReceived(i);
        } else {
            session.messageReceivedOnAuxChannel(ctx.channel(), i);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Channel c = ctx.channel();
        if (c.isActive()) {
            engine.getLogger().log(Level.WARNING, "Exception caught, closing channel: " + c + "...", cause);
            c.close();
        }
    }

    public Session getSession() {
        return this.session.get();
    }

    public void setSession(Session session) {
        if (!this.session.compareAndSet(null, session)) {
            throw new IllegalStateException("Session may not be set more than once");
        }
        decoder.setProtocol(session.getProtocol());
        encoder.setProtocol(session.getProtocol());
    }
}

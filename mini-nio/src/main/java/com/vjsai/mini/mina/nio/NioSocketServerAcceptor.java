package com.vjsai.mini.mina.nio;

import com.vjsai.mini.mina.filters.DefaultTcpFilter;
import com.vjsai.mini.mina.polling.AbstractPollingIoAcceptor;
import com.vjsai.mini.mina.session.SocketSessionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.*;
import java.util.Iterator;

/**
 * NioSocketServerAcceptor to accept client connections
 *
 */
public final class NioSocketServerAcceptor extends AbstractPollingIoAcceptor {

	private final static Logger LOGGER = LoggerFactory.getLogger(NioSocketServerAcceptor.class);
    /**
     * channel selector
     */
	private Selector selector;

	private SelectionKey selectionKey = null;

	public NioSocketServerAcceptor() throws Exception {
		super(NioProcessor.class, null);
		//add default TCP filter
		this.getFilterChain().addFilter("DEFAULT", new DefaultTcpFilter());
	}

	/**
	 * bind the server socket to local address
	 * @param socketAddress
	 * @throws IOException
	 */
	@Override
	protected void bindLocal(SocketAddress socketAddress) throws IOException {
	    //open and bind
		ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.socket().bind(socketAddress);
		//configure as non blocking
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.socket().setReuseAddress(true);
		selectionKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT, null);
		LOGGER.debug(" Listening to : " + socketAddress.toString() );
	}

	@Override
	protected int select(int selectMs) throws ClosedSelectorException, IOException {
		return selector.select(selectMs);
	}

	@Override
	protected void init() throws IOException {
		selector = Selector.open();
	}

    /**
     * check if we can accept connections and return the session state
     * @param selectionKey
     * @return
     * @throws IOException
     */
	@Override
	protected SocketSessionState accept(SelectionKey selectionKey) throws IOException {

		if (selectionKey == null || !selectionKey.isValid() || !selectionKey.isAcceptable()) {
			return null;
		}

		SocketChannel socketChannel = ((ServerSocketChannel) selectionKey.channel()).accept();
		if (socketChannel == null) {
			return null;
		}
        //create new socket session for this channel
		SocketSessionState sessionState = new SocketSessionState(socketChannel, this.filterChain,this.getHandler(),
				this.getSessionBufferSize(),this.getMaxSessionIdleTimeoutInMS());
		LOGGER.debug("Server channel created session : "+ sessionState);
		return sessionState;
	}

	@Override
	public void shutdown() {
		if (selectionKey == null || selector == null) {
			return;
		}
		//invoke shutdown
		super.shutdown();
		//close the selected channel
		try {
			selectionKey.channel().close();
			selectionKey.channel();
		} catch (IOException e) {
			LOGGER.warn("server shutdown because of : " + e.toString(), e);
		}
		//close the selected selector
		try {
			selector.wakeup();
			selector.close();
		} catch (Exception e) {
			LOGGER.warn("server shutdown because of : " + e.toString(), e);
		}
	}

    /**
     * iterator of selectkeys
     * @return
     */
	@Override
	protected Iterator<SelectionKey> selectKeys() {
		return selector.selectedKeys().iterator();
	}
}
package com.vjsai.mini.mina.nio;

import com.vjsai.mini.mina.api.IoFilterChain;
import com.vjsai.mini.mina.api.IoHandler;
import com.vjsai.mini.mina.exceptions.HandlerNotDefinedException;
import com.vjsai.mini.mina.filters.BaseFilterChain;
import com.vjsai.mini.mina.filters.DefaultTcpFilter;
import com.vjsai.mini.mina.polling.AbstractPollingIoProcessor;
import com.vjsai.mini.mina.session.SocketSessionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class deals woth socket connecting
 */
public final class NioSocketConnector extends AbstractPollingIoProcessor {

	private final Logger LOGGER = LoggerFactory.getLogger(NioSocketConnector.class);
	//current channel selector
	private Selector selector = null;
	//max idle session timeout
	private long maxSessionIdleTimeoutinMS = 0;
	//size of session buffer
	private int sessionBufferSize = 0;
	//handler
	private IoHandler handler = null;
	//filter chain
	private IoFilterChain filterChain;
	//selectionKey
	private SelectionKey selectionKey = null;

	public NioSocketConnector() throws IOException {
		//create a new cached threadpool
		this(Executors.newCachedThreadPool());
	}

	private NioSocketConnector(Executor executor) throws IOException {
		super(executor);
		this.selector = Selector.open();
		this.filterChain = new BaseFilterChain();
		this.filterChain.addFilter("DEFAULT", new DefaultTcpFilter());
	}

	/**
	 * client main
	 * 
	 * @param socketAddress
	 * @throws com.vjsai.mini.mina.exceptions.HandlerNotDefinedException
	 * @throws IOException
	 */
	public void connect(SocketAddress socketAddress) throws HandlerNotDefinedException, IOException {
		if (handler == null) {
			throw new HandlerNotDefinedException("Handler No Defined");
		}
		try {
			SocketSessionState session = initLocal(socketAddress);
			if (init(session)) {
				add(session);
			}

		} catch (IOException e) {
			LOGGER.warn("connect: ", e);
			throw e;
		}
	}

	/**
	 * Intialize the local socket state
	 * @param socketAddress
	 * @return
	 * @throws IOException
	 */
	private SocketSessionState initLocal(SocketAddress socketAddress) throws IOException {
		SocketChannel socketChannel = SocketChannel.open(socketAddress);
		SocketSessionState sessionState = new SocketSessionState(socketChannel, this.filterChain,this.getHandler(),
				this.getSessionBufferSize(),this.getMaxSessionIdleTimeoutinMS());
		return sessionState;
	}

	/**
	 * add session and start processing
	 * @param session
	 */
	@Override
	public void add(SocketSessionState session) {
		session.setRunnableProcessor(this);
		super.add(session);
	}

	/**
	 * tries to check if connectio n happened successfully if not return false
	 * @param session
	 * @return
	 * @throws IOException
	 */
	@Override
	protected boolean init(SocketSessionState session) throws IOException {
		SocketChannel socketChannel = session.getSocketChannel();
		if (socketChannel.finishConnect()) {
			socketChannel.configureBlocking(false);
			socketChannel.socket().setReuseAddress(true);
			selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
			session.setSelectionKey(selectionKey);
			selectionKey.attach(session);
			return true;
		}
		return false;
	}

	/**
	 *
	 * selects keys which channels are ready for I/O operations
	 * @param selectMs
	 * @return
	 * @throws ClosedSelectorException
	 * @throws IOException
	 */
	@Override
	protected int select(int selectMs) throws ClosedSelectorException, IOException {
		return selector.select(selectMs);
	}

	/**
	 * wakes up the selector thread
	 */
	@Override
	protected void wakeup() {
		selector.wakeup();
	}

	@Override
	protected Iterator<SelectionKey> selectKeys() {
		return selector.selectedKeys().iterator();
	}

	/***
	 * read control from session in this PROCESSOR
	 * @throws IOException
	 */

	@Override
	protected byte[] read(SocketSessionState session) throws IOException {
		byte[] receivedBytes = null;
		receivedBytes = session.read();
		return receivedBytes;
	}

	/**
	 * write to session
	 * @param session
	 */
	@Override
	protected void write(SocketSessionState session) {
		session.writeLocal();
	}

	public void close() {
		this.stopRun = true;
		//check if both selectionkey and selector are not null
		if (selectionKey == null || selector == null) {
			return;
		}
		//close the channel
		try {
			selectionKey.channel().close();
			selectionKey.cancel();
		} catch (IOException e) {
			LOGGER.error("client shutdown:" + e.toString(), e);
		}
		//wake up the thread and close
		try {
			this.selector.wakeup();
			this.selector.close();
		} catch (IOException e) {
			LOGGER.error("client shutdown:" + e.toString(), e);
		}
		//stop the executor
		ExecutorService es = (ExecutorService) executor;
		try {
			es.shutdownNow();
		} catch (Exception e) {
		}

	}

	/**
	 * Method to get maxSessionIdleTimeout in milliseconds
	 * @return
	 */
	public long getMaxSessionIdleTimeoutinMS() {
		return maxSessionIdleTimeoutinMS;
	}

	/**
	 * set socket state session's max idle time span(millisecond)
	 * 
	 * @param maxSessionIdleTimeoutinMS
	 */
	public void setMaxSessionIdleTimeoutinMS(long maxSessionIdleTimeoutinMS) {
		this.maxSessionIdleTimeoutinMS = maxSessionIdleTimeoutinMS;
	}

	/**
	 * to get current session's buffer size
	 * @return
	 */
	public int getSessionBufferSize() {
		return sessionBufferSize;
	}

	/**
	 * set socket session's channel buffer size (in bytes)
	 * 
	 * @param sessionBufferSize
	 */
	public void setSessionBufferSize(int sessionBufferSize) {
		this.sessionBufferSize = sessionBufferSize;
	}

	/**
	 * to get the current handler
	 * @return
	 */
	public IoHandler getHandler() {
		return handler;
	}

	/**
	 * method to set IoHandler
	 * @param handler
	 */
	public void setHandler(IoHandler handler) {
		this.handler = handler;
	}
}
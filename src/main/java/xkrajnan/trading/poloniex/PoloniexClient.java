/**
 * 
 */
package xkrajnan.trading.poloniex;

import java.util.concurrent.TimeUnit;

import rx.functions.Action1;
import ws.wamp.jawampa.PubSubData;
import ws.wamp.jawampa.WampClient;
import ws.wamp.jawampa.WampClient.ConnectedState;
import ws.wamp.jawampa.WampClient.ConnectingState;
import ws.wamp.jawampa.WampClient.DisconnectedState;
import ws.wamp.jawampa.WampClient.State;
import ws.wamp.jawampa.WampClientBuilder;
import ws.wamp.jawampa.transport.netty.NettyWampClientConnectorProvider;

/**
 * @author xkrajnan
 *
 */
public class PoloniexClient
{
	private static final String POLONIEX_API_URL = "wss://api.poloniex.com";
	private static final String REALM = "realm1";
	private static final int RECONNECT_INTERVAL_SEC = 5;

	private final WampClient client;

	private final Action1<State> logStatusChanged = new Action1<State>() {
		@Override
		public void call(State status)
		{
			System.err.println("Status: " + status);

			if (status instanceof ConnectedState) {
				// client.makeSubscription("ticker").subscribe(printTickerData);
				client.makeSubscription("BTC_ETH").subscribe(printOrder);

			} else if (status instanceof ConnectingState) {

			} else if (status instanceof DisconnectedState) {

			} else {
				System.err.println("Invalid client state!");
			}
		}
	};

	private final Action1<PubSubData> printTickerData = new Action1<PubSubData>() {
		@Override
		public void call(PubSubData data)
		{
			TickerRecord record = new TickerRecord(data);
			System.out.println(record);
		}
	};

	private final Action1<PubSubData> printOrder = new Action1<PubSubData>() {
		@Override
		public void call(PubSubData data)
		{
			System.out.println(data.arguments());

			try {
				// TODO: this cannot work like this because there can be
				// multiple simultaneous updates:
				// [{"type":"orderBookRemove","data":{"type":"ask","rate":"0.02485989"}},{"type":"orderBookModify","data":{"type":"ask","rate":"0.02488640","amount":"241.65200000"}}]
				OrderBookModify orderBookData = new OrderBookModify(data);
				System.out.println(orderBookData);

			} catch (Exception e) {
				System.out.println(e);
			}
		}
	};

	public PoloniexClient() throws Exception
	{
		NettyWampClientConnectorProvider connectorProvider = new NettyWampClientConnectorProvider();

		WampClientBuilder builder = new WampClientBuilder();

		builder.withConnectorProvider(connectorProvider);
		builder.withUri(POLONIEX_API_URL);
		builder.withRealm(REALM);
		builder.withInfiniteReconnects();
		builder.withReconnectInterval(RECONNECT_INTERVAL_SEC, TimeUnit.SECONDS);

		client = builder.build();

		client.statusChanged().subscribe(logStatusChanged);
	}

	public void open()
	{
		client.open();
	}

	public void close()
	{
		client.close().toBlocking().last();
	}

}

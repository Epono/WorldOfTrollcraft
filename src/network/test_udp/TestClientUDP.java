package network.test_udp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.apache.log4j.Logger;

import network.test_udp.listening_delegate.impl.ClientListeningDelegate;

public class TestClientUDP {

	public static void main(String argv[]) throws Exception {
		InetAddress server = InetAddress.getByName(NetworkUtils.SERVER_ADDRESS);
		DatagramSocket datagramSocket = new DatagramSocket();
		datagramSocket.setSoTimeout(5000);

		System.out.println("Client startup");

		/**
		 * Network listening thread
		 */
		ClientListeningDelegate clientListeningDelegate = new ClientListeningDelegate();
		new ListeningThread(datagramSocket, clientListeningDelegate).start();

		// TODO: Merge with client ?
		/**
		 * Command listening thread
		 */
		new Thread(new Runnable() {
			@Override
			public void run() {
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

				while (true) {
					try {
						String command = br.readLine();
						switch (command) {
						case "help":
							// TODO: Displays available commands
							System.out.println("Available commands : TODO");
							break;
						case "list":
							// TODO: lists all connected players
							System.out.println("Connected players : TODO");
							break;
						case "exit":
							// TODO: properly close server
							System.out.println("Closing server (not clean)");
							datagramSocket.close();
							System.exit(0);
							break;
						default:
							// If none of the above, send packet
							NetworkUtils.sendPacket(datagramSocket, server, NetworkUtils.SERVER_PORT,
									new Packet(0, 0, (byte) 255, (short) command.getBytes().length, command.getBytes()));
							break;
						}
					} catch (IOException e) {
						Logger.getLogger(TestServerUDP.class).error("IO error command");
					}
				}
			}
		}).start();
	}
}

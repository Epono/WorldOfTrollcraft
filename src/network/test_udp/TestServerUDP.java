package network.test_udp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.log4j.Logger;

import network.test_udp.listening_delegate.impl.ServerListeningDelegate;

public class TestServerUDP {

	public static void main(String argv[]) throws Exception {

		// TODO: Put in a try/catch
		DatagramSocket datagramSocket = new DatagramSocket(NetworkUtils.SERVER_PORT);

		System.out.println("Server startup");

		/**
		 * Network listening thread
		 */
		ServerListeningDelegate serverListeningDelegate = new ServerListeningDelegate();
		new ListeningThread(datagramSocket, serverListeningDelegate).start();

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
						String commandLine = br.readLine();

						String command = commandLine.split(" ")[0];
						ArrayList<String> commandArguments = new ArrayList<String>(
								Arrays.asList(commandLine.split(" ")));
						commandArguments.remove(0);
						
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
						}
					} catch (IOException e) {
						Logger.getLogger(TestServerUDP.class).error("IO error command");
					}
				}
			}
		}).start();
	}
}
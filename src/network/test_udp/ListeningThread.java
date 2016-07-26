package network.test_udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.PortUnreachableException;
import java.net.SocketTimeoutException;
import java.nio.channels.IllegalBlockingModeException;
import java.util.Arrays;

import org.apache.log4j.Logger;

import network.test_udp.listening_delegate.ListeningDelegate;

/**
 * Custom thread used to listen to the network
 */
public class ListeningThread extends Thread {

	/**
	 * @param datagramSocket
	 *            socket used to send packets
	 * @param delegate
	 *            method to call to process the packet (one for the server, one
	 *            for the client)
	 */
	public ListeningThread(DatagramSocket datagramSocket, ListeningDelegate delegate) {
		super(new Runnable() {
			private byte buffer[] = new byte[NetworkUtils.MAX_PACKET_SIZE];

			@Override
			public void run() {
				while (true) {
					try {
						DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
						datagramSocket.receive(datagramPacket);
						Packet packet = new Packet(datagramPacket.getData());

						System.out.println("Received packet : ");
						System.out.println(String.format("DatagramPacket : address = %s - length = %d - data = %s",
								datagramPacket.getSocketAddress(), datagramPacket.getLength(),
								Arrays.toString(datagramPacket.getData())));
						System.out.println(String.format("Packet : %s", packet));
						System.out.println();

						// Do something with the received packet
						delegate.processPacket(datagramSocket, datagramPacket);

					} catch (PortUnreachableException pue) {
						Logger.getLogger(TestServerUDP.class).error("Port Unreachable");
					} catch (SocketTimeoutException e) {
						Logger.getLogger(TestServerUDP.class).error("Socket Timeout");
					} catch (IllegalBlockingModeException e) {
						Logger.getLogger(TestServerUDP.class).error("Illegal Blocking Mode");
					} catch (IOException e) {
						Logger.getLogger(TestServerUDP.class).error("IO error");
					}
				}
			}
		});
	}

}

package network.test_udp.listening_delegate;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Command pattern (kinda) in order to have a more modular code.<br/>
 */
public interface ListeningDelegate {

	/**
	 * Method to process received packets
	 * 
	 * @param datagramSocket
	 *            socket used to send packets
	 * @param datagramPacket
	 *            packet received
	 */
	public void processPacket(DatagramSocket datagramSocket, DatagramPacket datagramPacket);
}
package network.test_udp.listening_delegate.impl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import network.test_udp.NetworkUtils;
import network.test_udp.Packet;
import network.test_udp.listening_delegate.ListeningDelegate;

/**
 * 
 */
public class ServerListeningDelegate implements ListeningDelegate {

	/*
	 * (non-Javadoc)
	 * 
	 * @see network.test_udp.listening_delegate.ListeningDelegate#call(java.net.
	 * DatagramSocket, java.net.DatagramPacket)
	 */
	@Override
	public void processPacket(DatagramSocket datagramSocket, DatagramPacket datagramPacket) {
		// For now, we just respond with a default packet
		try {
			NetworkUtils.sendPacket(datagramSocket, datagramPacket.getAddress(), datagramPacket.getPort(),
					new Packet(0, 0, (byte) 0, (short) 0, new byte[] {}));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

package network.test_udp.listening_delegate.impl;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import network.test_udp.listening_delegate.ListeningDelegate;

/**
 * 
 */
public class ClientListeningDelegate implements ListeningDelegate {

	/*
	 * (non-Javadoc)
	 * 
	 * @see network.test_udp.listening_delegate.ListeningDelegate#call(java.net.
	 * DatagramSocket, java.net.DatagramPacket)
	 */
	@Override
	public void processPacket(DatagramSocket datagramSocket, DatagramPacket datagramPacket) {
		// For now, we don't do anything
	}
}

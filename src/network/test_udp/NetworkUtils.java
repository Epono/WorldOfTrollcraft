package network.test_udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

public class NetworkUtils {

	public static final String SERVER_ADDRESS = "127.0.0.1";
	public static final int SERVER_PORT = 6798;
	public static final int MAX_PACKET_SIZE = 256;

	/**
	 * Method to send a custom packet
	 * 
	 * @param socket
	 *            : socket used to send the packet
	 * @param destinationIpAddress
	 *            : IP address of the receiver
	 * @param destinationPort
	 *            : port of the receiver
	 * @param packet
	 *            : custom packet to send
	 * @throws IOException
	 */
	public static void sendPacket(DatagramSocket socket, InetAddress destinationIpAddress, int destinationPort,
			Packet packet) throws IOException {
		ByteBuffer bf = ByteBuffer.wrap(new byte[Packet.HEADER_LENGTH + packet.getData().length]);

		bf.putInt(packet.getCrc());
		bf.putInt(packet.getId());
		bf.put(packet.getType());
		bf.putShort((short) packet.getData().length);
		bf.put(packet.getData());

		byte buffer[] = bf.array();

		DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length, destinationIpAddress,
				destinationPort);
		try {
			socket.send(datagramPacket);
			// System.out.println(String.format("Packet : %s", packet));
			// System.out.println(String.format("DatagramPacket : length = %d -
			// data = %s", datagramPacket.getLength(),
			// Arrays.toString(datagramPacket.getData())));
		} catch (SocketTimeoutException e) {
			System.err.println("Timeout");
		}

		// TODO: système de ack (si on recoit pas de "ok" dans les 5-10sec, on
		// essaye de reenvoyer, au bout de quelques essais, )
	}

}

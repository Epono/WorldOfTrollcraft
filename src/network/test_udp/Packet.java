package network.test_udp;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class Packet {
	// length = 4 (crc) + 4 (id) + 1 (type) + 2 (length) + data.length
	public static final int HEADER_LENGTH = 11;

	// CRC of the packet
	private int crc;

	// Id of the packet (incremental)
	private int id;

	// Type of action (login request, etc)
	private byte type;

	// Length of data
	private short length;

	// Actual data (direction of displacement, etc) (can be empty)
	private byte data[];

	/**
	 * Constructor for sending packets
	 * 
	 * @param crc
	 * @param id
	 * @param type
	 * @param length
	 * @param data
	 */
	public Packet(int crc, int id, byte type, short length, byte[] data) {
		super();
		this.crc = crc;
		this.id = id;
		this.type = type;
		this.length = length;
		this.data = data;
	}

	/**
	 * Constructor for received packets
	 * 
	 * @param data
	 */
	public Packet(byte[] buffer) {
		ByteBuffer bf = ByteBuffer.wrap(buffer);
		this.crc = bf.getInt();
		this.id = bf.getInt();
		this.type = bf.get();
		this.length = bf.getShort();
		this.data = new byte[this.length];
		bf.get(this.data, 0, this.length);
	}

	public int getCrc() {
		return crc;
	}

	public int getId() {
		return id;
	}

	public byte getType() {
		return type;
	}

	public short getLength() {
		return length;
	}

	public byte[] getData() {
		return data;
	}

	@Override
	public String toString() {
		return "Packet [crc=" + crc + ", id=" + id + ", type=" + type + ", length=" + length + ", data="
				+ Arrays.toString(data) + "]";
	}
}
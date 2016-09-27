import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Slave {
	private static long timeOffset = 0;

	public static long currentTime() {
		return System.currentTimeMillis() + timeOffset;
	}

	public static void main(String... args) throws Exception {
		System.out.println("Starting slave process...");
		new Thread(new Listener()).start();

		while (true) {
			System.out.printf("Current time is: %10.3f [+%d]\n", (double) currentTime() / 1000, timeOffset);
			Thread.sleep(3000);
		}
	}

	private static class Listener implements Runnable {
		@Override
		public void run() {
			try {
				InetAddress group = InetAddress.getByName("225.5.5.5");
				MulticastSocket s = new MulticastSocket(5555);
				s.joinGroup(group);

				byte[] buffer = new byte[1024];

				while (true) {
					DatagramPacket message = new DatagramPacket(buffer, buffer.length);
					s.receive(message);

					DataInputStream in = new DataInputStream(new ByteArrayInputStream(buffer));
					switch (in.readByte()) {
						case 0:
							System.out.println("Received request from master");
							long masterTime = in.readLong();

							ByteArrayOutputStream baos = new ByteArrayOutputStream(8);
							DataOutputStream out = new DataOutputStream(baos);
							out.writeLong(currentTime() - masterTime);

							s.send(new DatagramPacket(baos.toByteArray(), 0, 8, message.getAddress(), 5555));
							break;

						case 1:
							System.out.println("Received sync from master");
							timeOffset = Math.max(timeOffset, in.readLong());
							break;

						default:
							System.out.print("Unknown message type");
					}
				}
			} catch (IOException exception) {
				exception.printStackTrace();
				System.exit(1);
			}
		}
	}
}

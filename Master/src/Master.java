import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;

/**
 * Created by matthieu.villard on 20.09.2016.
 */
public class Master {

	public Master(int timeOut, long period){
		System.out.println("Starting master process...");
		Thread sender = new Thread(new Sender(period));
		sender.start();
		new Thread(new Listener(timeOut, sender)).start();
	}

	public static long currentTime() {
		return System.currentTimeMillis();
	}

	private static class Sender implements Runnable {

		private long period;

		public Sender(long period){
			this.period = period;
			System.out.printf("Starting sender, processing every %d ms\n", period);
		}

		@Override
		public void run() {
			try {
				InetAddress group = InetAddress.getByName("225.5.5.5");
				MulticastSocket s = new MulticastSocket(5554);

				while (true) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream(9);
					DataOutputStream out = new DataOutputStream(baos);
					out.writeByte(0);
					out.writeLong(currentTime());

					System.out.println("Sending request to slaves");
					s.send(new DatagramPacket(baos.toByteArray(), 0, 9, group, 5555));
					Thread.sleep(period);
				}
			} catch (IOException exception) {
				exception.printStackTrace();
				System.exit(1);
			} catch (InterruptedException exception) {
				exception.printStackTrace();
				System.exit(1);
			}
		}
	}

	private static class Listener implements Runnable {

		private int timeOut;
		private long time;
		private long maxOffset;
		private Thread sender;

		public Listener(int timeOut, Thread sender){
			this.timeOut = timeOut;
			this.sender = sender;
			System.out.printf("Starting listener, waiting up to %d ms\n", timeOut);
		}

		@Override
		public void run() {
			try {
				InetAddress group = InetAddress.getByName("225.5.5.5");
				MulticastSocket s = new MulticastSocket(5553);
				s.setSoTimeout(timeOut);
				s.joinGroup(group);

				byte[] buffer = new byte[1024];

				while (true) {
					time = currentTime();
					while (currentTime() - time < timeOut) {
						DatagramPacket message = new DatagramPacket(buffer, buffer.length);
						try {
							s.receive(message);
							DataInputStream in = new DataInputStream(new ByteArrayInputStream(buffer));
							long offset = in.readLong();
							System.out.printf("Received response from slave : %d\n", offset);

							maxOffset = Math.max(maxOffset, offset);
						} catch (SocketTimeoutException exception) {
							System.out.println("End of timeout");

							ByteArrayOutputStream baos = new ByteArrayOutputStream(9);
							DataOutputStream out = new DataOutputStream(baos);
							out.writeByte(1);
							out.writeLong(maxOffset);

							System.out.printf("Sending max offset to slaves : %d\n", maxOffset);
							s.send(new DatagramPacket(baos.toByteArray(), 0, 9, group, 5555));
						}
					}
				}
			} catch (IOException exception) {
				exception.printStackTrace();
				System.exit(1);
			}
		}
	}

	public static void main(String... args) throws Exception {
		new Master(3000, 3000);
	}

}
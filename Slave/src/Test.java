import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Test {
	public static void main(String... args) {
		try {
			InetAddress group = InetAddress.getByName("225.5.5.5");
			MulticastSocket s = new MulticastSocket(5554);
			System.out.println(s.getNetworkInterface().toString());
			s.joinGroup(group);

			DatagramPacket pkg = new DatagramPacket(new byte[] {0, 0, 0}, 3, group, 5555);
			s.send(pkg);
		} catch (java.io.IOException e) {
			e.printStackTrace();
		}
	}
}

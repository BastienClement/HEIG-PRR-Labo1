import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * File     : Slave.java
 * Project  : PRR - labo 01
 * Author   : Matthieu Villard & Batien Clément
 * Date     : 18 octobre 2016
 *
 * Cette classe a pour but de fournir les fonctionnalités nécessaire pour synchroniser des horloges. Ainsi, cette classe
 * représente un esclave qui peut recevoir des requête de synchronisation ou des réponses de synchronisation. Dans le
 * cas d'une requête, il envoit son décalage par rapport à l'heure fournie par le maître. Dans le cas d'une réponse,
 * il met à jour son décalage en utilisant le décalage maximum envoyé par le maître.
 *
 */
public class Slave {
	private static volatile long timeOffset = 0;

	/**
	 * Obtient l'heure actuelle, correspond à l'heure du system additionnée au décalage enregistré
	 *
	 * @return system time
	 */
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

	/**
	 * Cette classe interne a pour but d'écouter les requêtes et les réponses envoyées par le maître
	 *
	 */
	private static class Listener implements Runnable {

		/**
		 * Méthode exécutée par le Thread. Cette méthode reçoit les requêtes et les réponses du maître
		 * synchronisation.
		 *
		 */
		@Override
		public void run() {
			try {
				InetAddress group = InetAddress.getByName("225.5.5.5");
				// Utilisation d'une liste de diffusion pour recevoir les messages du maître
				MulticastSocket listener = new MulticastSocket(5555);
				// Rejoint le groupe de diffusion
				listener.joinGroup(group);

				byte[] buffer = new byte[9];

				while (true) {
					DatagramPacket message = new DatagramPacket(buffer, buffer.length);
					// Récéption d'un message
					listener.receive(message);

					DataInputStream in = new DataInputStream(new ByteArrayInputStream(buffer));
					// Actions en fonction du premier byte
					switch (in.readByte()) {
						case 0:
							// Réception d'une requête de synchronisation
							System.out.println("Received request from master");
							long masterTime = in.readLong();

							ByteArrayOutputStream baos = new ByteArrayOutputStream(8);
							DataOutputStream out = new DataOutputStream(baos);
							// Calcul de la différence
							out.writeLong(currentTime() - masterTime);

							// Utilisation d'un datagramme UDP point-à-point pour retourner la réponse
							DatagramSocket sender = new DatagramSocket();

							// Envoit de la réponse
							sender.send(new DatagramPacket(baos.toByteArray(), 0, 8, message.getAddress(), 5553));
							sender.close();
							break;

						case 1:
							// Récéption du résultat de synchronisation
							System.out.println("Received sync from master");
							// Mise à jour de l'offset
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

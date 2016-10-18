/**
 * File     : Master.java
 * Project  : PRR - labo 01
 * Author   : Matthieu Villard & Batien Clément
 * Date     : 18 octobre 2016
 *
 * Cette classe a pour but de fournir les fonctionnalités nécessaire pour synchroniser des horloges. Ainsi, cette classe
 * représente le maître qui peut envoyer des requêtes de synchronisation à tous ses esclaves. Il peut ensuite écouter
 * les réponses envoyées par les esclaves et finalement envoyer le décalage maximum à tous les esclaves.
 *
 */
import java.io.*;
import java.net.*;

/**
 * Created by matthieu.villard on 20.09.2016.
 */
public class Master {

	/**
	 * Constructeur de la classe Master
	 *
	 * @param timeOut le temps d'attente maximum pour recevoir toutes les réponses des esclave
	 * @param period le temps d'attente entre chaque requête de synchronisation
	 */
	public Master(int timeOut, long period){
		System.out.println("Starting master process...");

		// Création des threads d'envoi des requêtes (sender) et d'écoute des réponse (listener)
		Thread listener = new Thread(new Listener(timeOut));
		Thread sender = new Thread(new Sender(period, listener));
		listener.start();
		sender.start();
	}

	/**
	 * Obtient l'heure actuelle du maître
	 *
	 * @return system time
	 */
	public static long currentTime() {
		return System.currentTimeMillis();
	}

	/**
	 * Cette classe interne a pour but d'envoyer des requêtes de synchronisation à intervalle régulier.
	 *
	 */
	private static class Sender implements Runnable {

		private long period; // l'intervalle entre chaque requête
		private Thread listener; // Le thread qui écoute les réponses

		/**
		 * Constructeur de la classe Sender
		 *
		 * @param period l'intervalle entre chaque requête
		 * @param listener le thread qui écoute les réponses
		 */
		public Sender(long period, Thread listener){
			this.period = period;
			this.listener = listener;
			System.out.printf("Starting sender, processing every %d ms\n", period);
		}

		/**
		 * Méthode exécutée par le Thread. Cette méthode envoit régulièrement des requêtes de synchronisation.
		 *
		 */
		@Override
		public void run() {
			synchronized (listener) {
				try {
					InetAddress group = InetAddress.getByName("225.5.5.5"); // Addresse du groupe
					MulticastSocket sender = new MulticastSocket(); // Socket multicast pour joindre tous les esclaves

					while (true) {
						// Tableau avec 1er byte égal à 0 (requête) ou 1 (réponse) et 8 byte pour formet un long
						ByteArrayOutputStream baos = new ByteArrayOutputStream(9);
						DataOutputStream out = new DataOutputStream(baos);
						out.writeByte(0);
						out.writeLong(currentTime());

						System.out.println("Sending request to slaves");
						// Envoit de la requête
						sender.send(new DatagramPacket(baos.toByteArray(), 0, 9, group, 5555));
						// Attente que le listener ait reçu toutes les réponses
						listener.wait();
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
	}

	/**
	 * Cette classe interne a pour but d'écouter les réponses envoyées par les esclaves
	 *
	 */
	private static class Listener implements Runnable {

		private int timeOut; // Temps d'attente maximum
		private long time; // Temps écoulé
		private long maxOffset; // Décalage maximum reçu des esclaves

		/**
		 * Constructeur de la classe Sender
		 *
		 * @param timeOut Le temps maximum d'attente pour recevoir toutes les réponses
		 */
		public Listener(int timeOut){
			this.timeOut = timeOut;
			System.out.printf("Starting listener, waiting up to %d ms\n", timeOut);
		}

		/**
		 * Méthode exécutée par le Thread. Cette méthode reçoit les réponses suite à une demande de synchronisation.
		 *
		 */
		@Override
		public void run() {
			synchronized (this) {
				try {
					// Utilisation d'un datagramme UDP point-à-point
					DatagramSocket listener = new DatagramSocket(5553);
					// On utilise un timeout pour écouter toutes les réponses. Cela permet d'avoir un nombre d'esclaves
					// allant de 1 à n.
					listener.setSoTimeout(timeOut);
					byte[] buffer = new byte[8];

					while (true) {
						// Début d'une série de réception
						time = currentTime();
						while (currentTime() - time < timeOut) {
							DatagramPacket message = new DatagramPacket(buffer, buffer.length);
							try {
								// Réceéption d'une réponse
								listener.receive(message);
								DataInputStream in = new DataInputStream(new ByteArrayInputStream(buffer));
								// Récupère la valeur de l'offset
								long offset = in.readLong();
								System.out.printf("Received response from slave : %d\n", offset);

								// Calcul le décalage maximum
								maxOffset = Math.max(maxOffset, offset);
							} catch (SocketTimeoutException exception) {
								// Le temps maximum est écoulé
								System.out.println("End of timeout");

								ByteArrayOutputStream baos = new ByteArrayOutputStream(9);
								DataOutputStream out = new DataOutputStream(baos);
								// Utilisé pour envoyer le décalage maximum
								out.writeByte(1);
								out.writeLong(maxOffset);

								System.out.printf("Sending max offset to slaves : %d\n", maxOffset);
								// Utilisation d'une liste de diffusion pour renvoyer le décalage
								MulticastSocket sender = new MulticastSocket();
								InetAddress group = InetAddress.getByName("225.5.5.5");
								sender.send(new DatagramPacket(baos.toByteArray(), 0, 9, group, 5555));
								sender.close();
								// Notification de la fin au Thread d'envoit des requêtes de synchronisation
								notify();
							}
						}
					}
				} catch (IOException exception) {
					exception.printStackTrace();
					System.exit(1);
				}
			}
		}
	}

	public static void main(String... args) throws Exception {
		new Master(3000, 3000);
	}
}
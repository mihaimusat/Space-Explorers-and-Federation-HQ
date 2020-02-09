import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.nio.charset.StandardCharsets;

/**
 * @author mihaimusat
 * Musat Mihai-Robert
 * Grupa 332CB
 *
 * Class for a space explorer.
 */

public class SpaceExplorer extends Thread {

	/**
	 * Creates a {@code SpaceExplorer} object.
	 *
	 * @param hashCount
	 *            number of times that a space explorer repeats the hash operation
	 *            when decoding
	 * @param discovered
	 *            set containing the IDs of the discovered solar systems
	 * @param channel
	 *            communication channel between the space explorers and the
	 *            headquarters
	 */

	// definire constante
	private static final String SENT_ALL_SIGNAL = "END";
	private static final String STOP_EXPLORE_SIGNAL = "EXIT";

	// declarare proprietati pentru un Space Explorer
	private int numHash;
	private volatile Set<Integer> seenSystems;
	private CommunicationChannel usedChannel;

	// initializare obiect de tip Space Explorer
	public SpaceExplorer(Integer hashCount, Set<Integer> discovered, CommunicationChannel channel) {
		this.numHash = hashCount;
		this.seenSystems = discovered;
		this.usedChannel = channel;
	}

	@Override
	public void run() {

		while(true) {
			// un space explorer va primi mesaje intr-o bucla infinita
			Message receivedMessage = usedChannel.getMessageHeadQuarterChannel();

			// obtine campul data al mesajului
			String sentData = receivedMessage.getData();

			// daca mesajul este de tip EXIT, termina executia thread-ului
			if(receivedMessage == null ||
					sentData == null ||
					sentData.equals(STOP_EXPLORE_SIGNAL)) {
				return;
			}

			// obtine sistemul solar curent
			int crtSystem = receivedMessage.getCurrentSolarSystem();

			// sistemul solar curent a fost descoperit ?
			boolean checkCurrent = false;

			// verific daca acesta exista in set-ul de sisteme solare descoperite
			// si daca nu exista, il adaug
			synchronized(seenSystems) {
				if(!seenSystems.contains(crtSystem)) {
					checkCurrent = true;
					seenSystems.add(crtSystem);
				}
			}

			// obtine sistemul solar parinte
			int parentSystem = receivedMessage.getParentSolarSystem();

			// daca am in set toate sistemele solare adiacente
			// unui sistem solar parinte, inseamna ca pot sa incep transmisia altui mesaj
			if(checkCurrent) {
				// aplica functia de criptare pe campul data al mesajului
				// folosind functia encryptMultipleTimes
				String nextData = encryptMultipleTimes(sentData, numHash);

				// construieste un nou mesaj cu campurile obtinute
				Message nextMessage = new Message(parentSystem, crtSystem, nextData);

				// la final, pune mesajul pe canalul corespunzator Space Explorer
				usedChannel.putMessageSpaceExplorerChannel(nextMessage);
			}
		}

	}

	/**
	 * Applies a hash function to a string for a given number of times (i.e.,
	 * decodes a frequency).
	 *
	 * @param input
	 *            string to he hashed multiple times
	 * @param count
	 *            number of times that the string is hashed
	 * @return hashed string (i.e., decoded frequency)
	 */
	private String encryptMultipleTimes(String input, Integer count) {
		String hashed = input;
		for (int i = 0; i < count; ++i) {
			hashed = encryptThisString(hashed);
		}

		return hashed;
	}

	/**
	 * Applies a hash function to a string (to be used multiple times when decoding
	 * a frequency).
	 *
	 * @param input
	 *            string to be hashed
	 * @return hashed string
	 */
	private static String encryptThisString(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] messageDigest = md.digest(input.getBytes(StandardCharsets.UTF_8));

			// convert to string
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < messageDigest.length; i++) {
				String hex = Integer.toHexString(0xff & messageDigest[i]);
				if (hex.length() == 1)
					hexString.append('0');
				hexString.append(hex);
			}
			return hexString.toString();

		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
}
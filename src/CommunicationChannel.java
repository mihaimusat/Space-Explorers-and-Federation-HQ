import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * @author mihaimusat
 * Musat Mihai-Robert
 * Grupa 332CB
 *
 * Class that implements the channel used by headquarters and space explorers to communicate.
 */

public class CommunicationChannel {

    // definire constante
    private static final String SENT_ALL_SIGNAL = "END";
    private static final String STOP_EXPLORE_SIGNAL = "EXIT";
    private static final int CAPACITY = 100000;

    // folosesc doua buffere pentru canalele Space Explorer si HQ
    private ArrayBlockingQueue<Message> bufferSpaceExplorer;
    private ArrayBlockingQueue<Message> bufferHQ;

    // retin in set nodurile trimise spre Space Explorer
    private Set<Integer> sentNodes;

    // retin in hashmap perechi de forma (thread_id, mesaj) pentru a
    // vedea ce thread-uri au trimis parintele sistemului solar curent
    private Map<Long, Message> parentSource;

    // thread_id pentru Space Explorer/HQ care are voie sa citeasca/scrie la un moment dat
    private long crtHQ;

    // contor pentru numarul de mesaje scrise de HQ la un moment dat
    private int count;

    /**
     * Creates a {@code CommunicationChannel} object.
     */

    // initializare obiect de tip CommunicationChannel
    public CommunicationChannel() {
        this.bufferSpaceExplorer = new ArrayBlockingQueue<Message>(CAPACITY);
        this.bufferHQ = new ArrayBlockingQueue<Message>(CAPACITY);
        this.sentNodes = new HashSet<Integer>(CAPACITY);
        this.parentSource = new HashMap<Long, Message>();
        this.crtHQ = -1;
        this.count = 0;
    }

    /**
     * Puts a message on the space explorer channel (i.e., where space explorers write to and
     * headquarters read from).
     *
     * @param message
     *            message to be put on the channel
     */
    public void putMessageSpaceExplorerChannel(Message message) {

        // incearca sa puna mesajul in buffer-ul pentru Space Explorer
        try {
            bufferSpaceExplorer.put(message);
        }
        catch(InterruptedException e) {
        }
    }

    /**
     * Gets a message from the space explorer channel (i.e., where space explorers write to and
     * headquarters read from).
     *
     * @return message from the space explorer channel
     */
    public Message getMessageSpaceExplorerChannel() {

        Message getMessage = null;

        // incearca sa ia mesajul din buffer-ul pentru Space Explorer
        try {
            getMessage = bufferSpaceExplorer.take();
        }
        catch(InterruptedException e) {
        }

        return getMessage;
    }

    /**
     * Puts a message on the headquarters channel (i.e., where headquarters write to and
     * space explorers read from).
     *
     * @param message
     *            message to be put on the channel
     */

    public void putMessageHeadQuarterChannel(Message message) {

        // obtine campul data al mesajului
        String sendData = message.getData();

        // daca am un mesaj de tip EXIT sau END, pot sa trec mai departe
        if(sendData.equals(SENT_ALL_SIGNAL) || sendData.equals(STOP_EXPLORE_SIGNAL)) {
            return;
        }

        // obtine sistemul solar curent
        int crtSystem = message.getCurrentSolarSystem();

        // mesajul care urmeaza sa fie trimis de HQ
        Message nextMessage = null;

        // obtine thread-ul curent
        long threadId = Thread.currentThread().getId();

        if(crtHQ != threadId) {
            if(count == 0) {
                count = 1;
            }
            else {
                count = 0;
            }
        }

        // verifica daca thread-ul curent a adaugat in map
        // parintele sistemului solar
        boolean checkThread = parentSource.containsKey(threadId);

        if(!checkThread) {
            // in acest caz, dupa ce imi initializez
            // campurile mesajului care trebuie trimis
            // actualizez count si thread-ul care
            // scrie este thread-ul curent
            int parentSystem = crtSystem;
            crtSystem = -1;
            count = 1;
            crtHQ = threadId;
            nextMessage = new Message(parentSystem, crtSystem, null);
            parentSource.put(threadId, nextMessage);
        }

        if(checkThread) {
            // in acest caz, cum sistemul solar parinte a fost
            // deja pus, noul mesaj de trimis este cel pus de thread-ul
            // care a adaugat parintele si apoi actualizez
            // sistemul solar curent si campul data
            nextMessage = parentSource.remove(threadId);
            count = 0;
            crtHQ = -1;
            nextMessage.setCurrentSolarSystem(crtSystem);
            nextMessage.setData(sendData);

            // verifica daca sistemul solar curent a fost deja pus
            // in set-ul de sisteme solare trimise spre Space Explorer
            boolean checkSent = sentNodes.contains(crtSystem);
            if (checkSent) {
                return;
            }
            else {
                sentNodes.add(crtSystem);
            }
        }

        // incearca sa puna mesajul in buffer-ul pentru HQ
        try {
            bufferHQ.put(nextMessage);
        }
        catch(InterruptedException e) {
        }

    }


    /**
     * Gets a message from the headquarters channel (i.e., where headquarters write to and
     * space explorer read from).
     *
     * @return message from the header quarter channel
     */
    public Message getMessageHeadQuarterChannel() {

        Message getMessage = null;

        // incearca sa ia mesajul din buffer-ul pentru HQ
        try {
            getMessage = bufferHQ.take();
        }
        catch(InterruptedException e) {
        }

        return getMessage;
    }
}
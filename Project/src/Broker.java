import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/*
 * Broker is the server for the Consumer. Each one of the three is assigned to different artists depending on their (ip+port hashkey).
 * Broker pulls chunks from Publisher (as a client), and pushes said chunks to the Consumer (as a server).
 */
public class Broker implements Node{
	private List<Consumer> registeredUsers = new ArrayList<>(); //List of registered Consumers.
	private List<Publisher> registeredPublisher = new ArrayList<>(); //All the publishers that have artists that should be saved in this broker.
	private List<ArtistName> registeredArtists = new ArrayList<>(); //All the artists that are saved in this broker.
	private Info brokerInfo = new Info(); //Contains this broker's information.
	private ExecutorService pool = Executors.newFixedThreadPool(100); //Broker thread pool.
	private ServerSocket providerSocket; //Broker's server socket, this accepts Consumer queries.
	private int[] ipPort; //Hashed ip+Port of all the brokers.
	private int brokerId;
	
	//Broker as a client for publisher
	InputStreamReader inputPublisher = null;
    BufferedReader outPublisher = null;
	
	public static void main(String[]args) throws IOException{
		Broker b = new Broker();
		int port;
		BufferedReader reader = new BufferedReader(new FileReader("./src/initBroker.txt")); //Reading init file to initialize this broker's port correctly.
		
		
		String line;	
		line = reader.readLine();
		int brokerNumber = Integer.parseInt(line);
		b.setBrokerId(brokerNumber); //Setting broker's id, based on the initBroker.txt file.
		reader.close();
		
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("./src/initBroker.txt")));
		if(brokerNumber == 0) { //This means that this is the first broker to be initialized.
			port = FIRSTBROKER;
			brokerNumber++; //Increasing the counter of running brokers.
			writer.write(String.valueOf(brokerNumber)); //Refreshing init file.
		}else if(brokerNumber == 1) {
			port = SECONDBROKER;
			brokerNumber++;
			writer.write(String.valueOf(brokerNumber));
		}else {
			port = THIRDBROKER;
			brokerNumber = 0; //When we initialize the third broker we reset the counter to 0.
			writer.write(String.valueOf(brokerNumber));
		}	
		
		//Setting Info ip, port and brokerId.
		b.getBrokerInfo().setIp(ip);
		b.getBrokerInfo().setPort(String.valueOf(port));	
		b.getBrokerInfo().setBrokerId(b.getBrokerId());
		
		writer.close();
		
		b.calculateKeys(); //Check method.
		b.init(port);
	}
	
	public void init(int port) throws UnknownHostException, IOException{
		providerSocket = new ServerSocket(port);
		System.out.println("[BROKER] Initializing data.");
		int publisherId = 0;
		Socket initializeSocket = null;
		while(publisherId<3) {
			if(publisherId == 0) {
				initializeSocket = new Socket(ip, FIRSTPUBLISHER); //This socket will be used to initialize this broker. (Retrieving data from publishers)
			}else if(publisherId == 1) {
				initializeSocket = new Socket(ip, SECONDPUBLISHER);
			}else {
				initializeSocket = new Socket(ip, THIRDPUBLISHER);
			}
			
			PrintWriter initializeQuery = new PrintWriter(initializeSocket.getOutputStream(), true); 
			ObjectInputStream initStream = new ObjectInputStream(initializeSocket.getInputStream());
	        
	        initializeQuery.println(getIpPort()[getBrokerId()]); //Sending hashed ip+port key to publisher.
	        try {
	        	Publisher p = new Publisher();
	        	Boolean publisherExists = false;
	        	p.setPublisherId(publisherId);
	        	ArtistName topic = (ArtistName) initStream.readObject(); //Reading the first artistName that the Publisher sent.

	        	
	        	while(!topic.getArtistName().equalsIgnoreCase("")) { //Initializing topicList and registeredPublishers. (String "" is a terminal message sent by the publisher).
					for(Publisher registeredP:registeredPublisher) {
						if(p.getPublisherId() == registeredP.getPublisherId()) {
							publisherExists = true;
						}
					}
					if(!publisherExists) {
						getRegisteredPublisher().add(p);
					}
	        		System.out.println(topic.getArtistName());
					p.getArtists().add(topic);
					getRegisteredArtists().add(topic);
					topic = (ArtistName) initStream.readObject(); //Reading the next artistName that the Publisher sent.
	        	}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
	        publisherId++;
	        initializeSocket.close();
		}
		//Initializing info list  Info {ListOfBrokers {IP,Port} , < BrokerId, ArtistName>}
		getBrokerInfo().setRegisteredArtists(getRegisteredArtists());
		//Creating file Broker(brokerId).txt which contains the Broker's Info and registeredArtists.
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("./src/Broker" + getBrokerId() + ".txt")));
		writer.write(getBrokerInfo().getIp());
		writer.write("\n" + getBrokerInfo().getPort());
		writer.write("\n" + getBrokerInfo().getBrokerId());
		for(ArtistName artist:getRegisteredArtists()) {
			writer.write("\n" + artist.getArtistName());
		}
		writer.close();
		
		while(true) {//Accepting Consumer queries.
	       	System.out.println("[BROKER] Waiting for consumer connection.");
	       	Socket client = providerSocket.accept();
	       	
	       	System.out.println("[BROKER] Connected to a consumer!");
	       	ActionsForConsumers consumerThread = new ActionsForConsumers(client);
	       	pool.execute(consumerThread);
		}
	}

    private class ActionsForConsumers extends Thread {
    	//Sockets for consumer and publisher, I/O streams, reader/writers.
    	private Socket connection;
    	private Socket requestSocket = null;
        private PrintWriter printOut;
        private BufferedReader out;
    	private InputStreamReader in;
    	private BufferedReader publisherReader;
		private ObjectInputStream inP;
		private ObjectOutputStream outC;
    	private PrintWriter publisherWriter;
 
        public ActionsForConsumers(Socket socket) {
            this.connection = socket;
        }
 
        public void run() {
    		try {
    	        //I/O streams for the consumer
    	        in = new InputStreamReader(connection.getInputStream());
    	        out = new BufferedReader(in);
    	        printOut = new PrintWriter(connection.getOutputStream(), true);
    			outC = new ObjectOutputStream(connection.getOutputStream());
    			
    			String consumerQuery = out.readLine();
    			if(consumerQuery.equalsIgnoreCase("Initialize broker list.")) { //The Consumer sends this query only the first time they connect to any server.
    				consumerQuery = out.readLine(); //reading consumer id
    				Consumer c = new Consumer();
    				c.setConsumerId(Integer.parseInt(consumerQuery));
    				for(Consumer registeredConsumer:getRegisteredUsers()) {
    					if(!(registeredConsumer.getConsumerId() == c.getConsumerId())) { //adding new customer (if he does not already exist in registeredUser list.
    						getRegisteredUsers().add(c);
    					}
    				}
    				
        			
        			for(int i=0; i<3; i++) { //reading broker info files.
            			BufferedReader readBrokerInfo = new BufferedReader(new FileReader("./src/Broker" + i + ".txt")); //Broker file reader.
            			String line;
            			Info brokerInfo = new Info();
            			
            			line = readBrokerInfo.readLine(); //reading ip
            			brokerInfo.setIp(line); 
            			line = readBrokerInfo.readLine(); //reading port
            			brokerInfo.setPort(line); 
            			line = readBrokerInfo.readLine(); //reading brokerId
            			brokerInfo.setBrokerId(Integer.parseInt(line));
            			line = readBrokerInfo.readLine(); 
            			
            			while(line != null) {//reading artistNames
            				brokerInfo.getRegisteredArtists().add(new ArtistName(line));
            				line = readBrokerInfo.readLine(); 
            			}
            			readBrokerInfo.close();
            			outC.writeObject(brokerInfo); //sending info to consumer
            			outC.flush();            			
        			}
        			
        			outC.writeObject(new Info("", "", -1, null));
    			}else {//reading consumer id
    				Consumer c = new Consumer();
    				c.setConsumerId(Integer.parseInt(consumerQuery));
    				for(Consumer registeredConsumer:getRegisteredUsers()) {
    					if(!(registeredConsumer.getConsumerId() == c.getConsumerId())) { //adding new customer (if he does not already exist in registeredUser list.
    						getRegisteredUsers().add(c);
    					}
    				}
    			}


    	        while(true) { //This is where the Broker pulls from the Publisher and pushes to the Consumer the mp3 that's been asked from the query.
    	        	String artistName = out.readLine(); //Consumer artistName query.
    	        	
        			for(Publisher p:getRegisteredPublisher()) { //Looking for the artistName in all the registeredPublishers.
        				for(ArtistName artist:p.getArtists()) {
    	    				if(artist.getArtistName().equalsIgnoreCase(artistName)) {
    	    					if(p.getPublisherId() == 0) { //Connecting to the correct Publisher.
    	    						requestSocket = new Socket(ip, FIRSTPUBLISHER);
    	    					}else if(p.getPublisherId() == 1) {
    	    						requestSocket = new Socket(ip, SECONDPUBLISHER);
    	    					}else {
    	    						requestSocket = new Socket(ip, THIRDPUBLISHER);
    	    					}
    	    					//Initializing reader/writer and stream.
    	    					publisherWriter = new PrintWriter(requestSocket.getOutputStream(), true);
    			    			inP = new ObjectInputStream(requestSocket.getInputStream());
    			    	        publisherReader = new BufferedReader(new InputStreamReader(requestSocket.getInputStream()));
    	    				}
        				}
        			}
        			
        			if(requestSocket == null) {
        				break;
        			}
        			publisherWriter.println(artistName);
    	        	
    	        	if(artistName.equals("quit")) break; //Terminal message.
    	        	
	    	        while(true) { //This is where the Consumer sends song queries.
	    	        	String inputLine = out.readLine(); //This inputLinee is the song query.
	    	        	System.out.println("Client query: " + artistName + " - " + inputLine);
	    	        	
		    	        if(!inputLine.equalsIgnoreCase("back")) { 
		    	        	publisherWriter.println(inputLine);
			    	        Value publisherResponse = (Value) inP.readObject(); //Pulling first object from publisher.
			    	        if(publisherResponse.getMusicFile().getMusicFileExtract() != null){ //If the first Value contains a chunk.
				    	        while(publisherResponse.getMusicFile().getMusicFileExtract() != null) {
		        					outC.writeObject(publisherResponse); //Pushing Value object to the Consumer.
		        					outC.flush();
				    	        	publisherResponse = (Value) inP.readObject(); //Pulling next Value from Publisher.
				    	        	if(publisherResponse.getMusicFile().getMusicFileExtract() == null) {
			        					outC.writeObject(publisherResponse);
			        					outC.flush();
				    	        		break;
				    	        	}
				    	        }
			    	        }else { //If the first chunk is null, that means that the song doesn't exist.
		    					outC.writeObject(publisherResponse); //Pushes null chunk to Consumer (to stay in sync).
		    					outC.flush();
		    					String availableSongs = publisherReader.readLine(); //Pulls from Publisher the available song list for the artist.
		    					printOut.println(availableSongs); //Pushes song list metadata to Consumer.
			    	        }
		    	        }else { //Breaks this while ("back" is a terminal message for this loop), goes back to ArtistName loop.
		    	        	publisherWriter.println(inputLine); //Sends back command to the Publisher so that they don't get out of sync.
		    	        	break;
		    	        }
	    	        }
    	        }

    			
                	
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} finally {
				try {
					//Closing sockets, I/O streams, writers/readers.
					printOut.close();
					in.close();
					out.close();
					connection.close();
					if(requestSocket != null) {
						requestSocket.close();
						publisherReader.close();
					}
				} catch (IOException ioException) {
					ioException.printStackTrace();
				}
			}
        }
    }
	
	public List<Broker> getBrokers(){
		return brokers;
	}
	
	public void connect() {}
	
	public void disconnect() {}
	
	public void updateNodes() {}
	public void calculateKeys() { //Setting the hashkeys(ip+port) of all the brokers.
		int[] hashedKeys = new int[3];
		String hash;
		MessageDigest md;
		
		try {
			for(int i=0; i<3; i++) {
				if(i==0) {//Setting the key for the first broker.
					hash = ip + FIRSTBROKER;
				}else if(i==1) {
					hash = ip + SECONDBROKER;
				}else {
					hash = ip + THIRDBROKER;
				}
				md = MessageDigest.getInstance("MD5");
			    md.update(hash.getBytes());
			    byte[] digest = md.digest();
			    BigInteger no = new BigInteger(1, digest); 
			    String hashtext = no.toString(16); 
			    while (hashtext.length() < 32) { 
			    	hashtext = "0" + hashtext; 
			    } 
			    int hashCode = no.hashCode()%59;
			    hashedKeys[i] = hashCode;
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}	
		setIpPort(hashedKeys);
	}
	
	public void notifyPublisher(String str) {}
	public void pull(ArtistName artistName) {}

	public int[] getIpPort() {
		return ipPort;
	}

	public void setIpPort(int[] ipPort) {
		this.ipPort = ipPort;
	}

	public List<ArtistName> getRegisteredArtists() {
		return this.registeredArtists;
	}

	public void setRegisteredArtists(List<ArtistName> registeredArtists) {
		this.registeredArtists = registeredArtists;
	}

	public int getBrokerId() {
		return brokerId;
	}

	public void setBrokerId(int brokerId) {
		this.brokerId = brokerId;
	}

	public List<Publisher> getRegisteredPublisher() {
		return registeredPublisher;
	}

	public void setRegisteredPublisher(List<Publisher> registeredPublisher) {
		this.registeredPublisher = registeredPublisher;
	}

	public List<Consumer> getRegisteredUsers() {
		return registeredUsers;
	}

	public void setRegisteredUsers(List<Consumer> registeredUsers) {
		this.registeredUsers = registeredUsers;
	}

	public Info getBrokerInfo() {
		return brokerInfo;
	}

	public void setBrokerInfo(Info brokerInfo) {
		this.brokerInfo = brokerInfo;
	}
}

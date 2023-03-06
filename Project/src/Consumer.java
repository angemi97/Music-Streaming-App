import java.util.*;
import java.net.*;
import java.io.*;

/*
 * Consumer is the user(client) that asks for artists and songs.
 * He receives the chunks from the Broker(server) and creates the chunk files inside the Downloaded Chunks folder.
 */
public class Consumer implements Node {
	private int consumerId;
	private Socket requestSocket = null;
	private PrintWriter initializeQuery;
    private BufferedReader out;
    private BufferedReader keyboard;
	private InputStreamReader input;
	private ObjectInputStream inB;
	private String consumerName;
	private List<Info> brokerInfo = new ArrayList<>();

	public static void main(String[]args) throws IOException{
		Consumer c1 = new Consumer();
		
		
		c1.connect(); //Connecting to a random Broker.
		System.out.println("Login name: ");
		String consumerName = c1.getKeyboard().readLine();
		c1.setConsumerName(consumerName);
		c1.init(c1.getSocket().getPort()); //Initializes the Consumer.
		System.out.println("Bye!");
	}
	
	public void init(int port){
		try {
			Boolean artistFound; 
	        initializeQuery.println("Initialize broker list.");
	        initializeQuery.println(String.valueOf(getConsumerId())); //sending consumerId.
	        
	        Info info = (Info) inB.readObject();//getting broker info
	        
	        while(!info.getIp().equalsIgnoreCase("")) {
	        	getBrokerInfo().add(info);
	        	info = (Info) inB.readObject();
	        }
	        System.out.println("Available artists: ");
	        for(Info i:getBrokerInfo()) {
	        	for(ArtistName artist:i.getRegisteredArtists()) {
	        		System.out.println(artist.getArtistName());
	        	}
	        }
	        
	        while(true) {
	        	artistFound = false; //This is set to true if the artist from the query exists.
	        	System.out.println("Type the name of an available artist (type 'quit' to disconnect): ");
	        	String artist = keyboard.readLine().trim();
	        		
	        	if (artist.equals("quit")) { //Terminal message.
	        		initializeQuery.println(artist); //Sends terminal message to Broker so that he can disconnect and terminate the Thread.
	        		disconnect(); //Disconnecting from the Broker.	        		
	        		break;
	        	}
	        	
	        	for(Info i:getBrokerInfo()) {
	        		for(ArtistName registeredArtist:i.getRegisteredArtists()) {
	        			if(artist.equalsIgnoreCase(registeredArtist.getArtistName())) {
	        				artistFound = true;
	        				if(!(requestSocket.getPort() == Integer.parseInt(i.getPort()))){ //if consumer isn't already connected to the correct broker.
	        					initializeQuery.println("quit"); //Sending terminal message to the Broker so that he can disconnect and  terminate the Thread.
	        					disconnect();
	        					connect(Integer.parseInt(i.getPort())); //connecting to a new broker.
	        					initializeQuery.println(String.valueOf(getConsumerId())); //sending consumerId to the new broker.
	        				}
	        				initializeQuery.println(artist); //Sending query to the Broker.
	        			}
	        		}
	        	}
	        	
	        	//Consumer chooses song.
	        	if(artistFound) {
	        		Boolean listedSongs = false; // This is set to true after listing the songs of the artist that has been chosen.
		        	while(true) {
		        		String consumerQuery;
		        		if(!listedSongs) { //The first query is automatic and will list all of the artists available songs in a string. (The consumer won't receive the actual chunks, just a string with the titles.
		        			consumerQuery = "List songs!!!";
		        		}else {
		        			System.out.println("Type the name of the song you'd like to listen to: (type 'back' to choose a different artist): ");
		        			consumerQuery = keyboard.readLine().trim();
		        		}
			        	
			        				
			        	if (consumerQuery.equals("back")) {     		
				        	initializeQuery.println(consumerQuery);
			        		break;
			        	}
			        				
			        	initializeQuery.println(consumerQuery);
			        				
			        	Value brokerResponse = (Value) inB.readObject();
			            if(brokerResponse.getMusicFile().getMusicFileExtract() != null){ //Receiving chunks from broker.
			                int c = 0;
			                System.out.println("Downloading song: " + brokerResponse.getMusicFile().getArtistName() + " - " + brokerResponse.getMusicFile().getTrackName() + "...");
			        	   	while(brokerResponse.getMusicFile().getMusicFileExtract() != null) { //Writing chunk mp3 files in the Downloaded Chunks folder.
			        	   		FileOutputStream mp3 = new FileOutputStream("./src/Downloaded Chunks" + "/" + brokerResponse.getMusicFile().getTrackName() + c + ".mp3");
			        	   	    mp3.write(brokerResponse.getMusicFile().getMusicFileExtract());
			        	   	    mp3.close();
			        	   	    c++;
			        	        brokerResponse = (Value) inB.readObject();
			        	        if(brokerResponse.getMusicFile().getMusicFileExtract() == null) {
			        	   	    	break;
			        	   	    }
			        	   	} 
			        	   	System.out.println("Your song has been downloaded successfully! Check your Downloaded Chunks folder.");
			           	}else { //If the first chunk is null the song doesn't exist.
			           		if(!listedSongs) {
			           			listedSongs = true;
			           		}else {
			           			System.out.println("The requested song does not exist.");
			           		}
			           		System.out.println(artist + "'s available song list.");
			           		String availableSongs = out.readLine();
			           		for(String songName:availableSongs.split("NEXT")) {//Printing existing songlist of the artist.
			           			System.out.println(songName);
			           		}
			           	}	
		        	}
	        	}else {
	        		System.out.println("The artist " + artist + " is not available!");
	        	}
	        } 

		} catch (UnknownHostException unknownHost) {
			System.err.println("You are trying to connect to an unknown host!");
		} catch (IOException ioException) {
			ioException.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public List<Broker> getBrokers(){
		return brokers;
	}
	
	public void connect() { //Connects to a random Broker and nitializes sockets, readers/writers and I/O streams.
		try {
			if(requestSocket == null) {
				Random randGen = new Random(); 				
				int random = randGen.nextInt(3);
				if(random == 0) {
					requestSocket = new Socket(ip, FIRSTBROKER);
				}else if(random == 1) {
					requestSocket = new Socket(ip, SECONDBROKER);
				}else {
					requestSocket = new Socket(ip, THIRDBROKER);
				}
			}
			
			
			keyboard = new BufferedReader(new InputStreamReader(System.in));
			input = new InputStreamReader(requestSocket.getInputStream());
	        out = new BufferedReader(input);
	        initializeQuery = new PrintWriter(requestSocket.getOutputStream(), true);
	        inB = new ObjectInputStream(requestSocket.getInputStream());
	        
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void connect(int port) { //Connects to a Broker and initializes sockets, readers/writers and I/O streams.
		try {
			requestSocket = new Socket(ip, port);			
			input = new InputStreamReader(requestSocket.getInputStream());
	        out = new BufferedReader(input);
	        initializeQuery = new PrintWriter(requestSocket.getOutputStream(), true);
	        inB = new ObjectInputStream(requestSocket.getInputStream());
	        
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	public void disconnect(Broker broker, ArtistName artistName) {}
	  
	public void register(Broker broker, ArtistName artistName) {}
	public void disconnect() {//Disconnects from a Broker and closes sockets, readers/writers and I/O streams.
		try {
			out.close();
			input.close();
			initializeQuery.close();
			requestSocket.close();
			inB.close();
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	public void playData(ArtistName artistName, Value value) {}

	@Override
	public void updateNodes() {}
	public Socket getSocket() {
		return requestSocket;
	}
	
	public void setConsumerName(String name) {
		this.consumerName = name;
	}
	
	public String getConsumerName() {
		return this.consumerName;
	}
   
	public BufferedReader getKeyboard(){
		return keyboard;
	}

	public List<Info> getBrokerInfo() {
		return brokerInfo;
	}

	public void setBrokerInfo(List<Info> brokerInfo) {
		this.brokerInfo = brokerInfo;
	}

	public int getConsumerId() {
		return consumerId;
	}

	public void setConsumerId(int consumerId) {
		this.consumerId = consumerId;
	}
}

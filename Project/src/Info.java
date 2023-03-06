import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/*
 * Info objects are used when initializing the Brokers.
 * This class is used when initializing a Broker
 * to store their ip, port, id and the artists
 * that have been assigned to said Broker.
 * 
 * It implements Serializable so that each broker can send their info
 * to the Consumer that is connected to them.
 */

public class Info implements Serializable{
	private static final long serialVersionUID = 1L;
	private String ip;
	private String port;
	private int brokerId;
	private List<ArtistName> registeredArtists = new ArrayList<>();
	
	//Class Constructors.
	
	public Info() {}
	
	public Info(String ip, String port, int brokerId, List<ArtistName> registeredArtists) {
		this.ip = ip;
		this.port = port;
		this.brokerId = brokerId;
		this.registeredArtists = registeredArtists;
	}
	
	//Setters and getters of this class.
	
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public int getBrokerId() {
		return brokerId;
	}
	public void setBrokerId(int brokerId) {
		this.brokerId = brokerId;
	}
	public String getPort() {
		return port;
	}
	public void setPort(String port) {
		this.port = port;
	}
	public List<ArtistName> getRegisteredArtists() {
		return registeredArtists;
	}
	public void setRegisteredArtists(List<ArtistName> registeredArtists) {
		this.registeredArtists = registeredArtists;
	}
}
